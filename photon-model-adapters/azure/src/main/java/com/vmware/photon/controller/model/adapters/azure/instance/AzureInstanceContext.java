/*
 * Copyright (c) 2015-2016 VMware, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, without warranties or
 * conditions of any kind, EITHER EXPRESS OR IMPLIED.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.vmware.photon.controller.model.adapters.azure.instance;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.management.compute.ComputeManagementClient;
import com.microsoft.azure.management.compute.models.ImageReference;
import com.microsoft.azure.management.network.NetworkManagementClient;
import com.microsoft.azure.management.network.NetworkSecurityGroupsOperations;
import com.microsoft.azure.management.network.SubnetsOperations;
import com.microsoft.azure.management.network.models.NetworkInterface;
import com.microsoft.azure.management.network.models.NetworkSecurityGroup;
import com.microsoft.azure.management.network.models.PublicIPAddress;
import com.microsoft.azure.management.network.models.Subnet;
import com.microsoft.azure.management.resources.ResourceManagementClient;
import com.microsoft.azure.management.resources.models.ResourceGroup;
import com.microsoft.azure.management.storage.StorageManagementClient;
import com.microsoft.azure.management.storage.models.StorageAccount;

import okhttp3.OkHttpClient;

import com.vmware.photon.controller.model.adapterapi.ComputeInstanceRequest;
import com.vmware.photon.controller.model.adapters.azure.utils.AzureDeferredResultServiceCallback;
import com.vmware.photon.controller.model.adapters.util.instance.BaseComputeInstanceContext;
import com.vmware.photon.controller.model.resources.DiskService.DiskState;
import com.vmware.photon.controller.model.resources.ResourceGroupService.ResourceGroupState;
import com.vmware.photon.controller.model.resources.SecurityGroupService.SecurityGroupState;
import com.vmware.photon.controller.model.resources.StorageDescriptionService.StorageDescription;
import com.vmware.xenon.common.DeferredResult;
import com.vmware.xenon.common.Operation;
import com.vmware.xenon.services.common.AuthCredentialsService.AuthCredentialsServiceState;

/**
 * Context object to store relevant information during different stages.
 */
public class AzureInstanceContext extends
        BaseComputeInstanceContext<AzureInstanceContext, AzureInstanceContext.AzureNicContext> {

    /**
     * The class encapsulates NIC related data (both Photon Model and Azure model) used during
     * provisioning.
     */
    public static class AzureNicContext extends BaseComputeInstanceContext.BaseNicContext {

        /**
         * The Azure subnet this NIC is associated to. It is either looked up from Azure or created
         * by this service.
         */
        public Subnet subnet;

        /**
         * The actual NIC object in Azure. It is created by this service.
         */
        public NetworkInterface nic;

        /**
         * The public IP assigned to the NIC. It is created by this service.
         */
        public PublicIPAddress publicIP;

        /**
         * The security group this NIC is assigned to. It is created by this service.
         */
        public NetworkSecurityGroup securityGroup;

        /**
         * The resource group state the security group is member of. Optional.
         */
        public ResourceGroupState securityGroupRGState;

        /**
         * A shortcut method to {@code this.securityGroupStates.get(0)}.
         *
         * @return {@code null} is returned if security group is not specified.
         */
        public SecurityGroupState securityGroupState() {
            return this.securityGroupStates != null && !this.securityGroupStates.isEmpty()
                    ? this.securityGroupStates.get(0) : null;
        }
    }

    public AzureInstanceStage stage;

    public AuthCredentialsServiceState childAuth;

    public StorageDescription storageDescription;
    public DiskState bootDisk;
    public List<DiskState> childDisks;
    public String vmName;
    public String vmId;

    // Azure specific context
    public ApplicationTokenCredentials credentials;
    public ResourceGroup resourceGroup;
    public StorageAccount storage;

    public String storageAccountName;
    public ImageReference imageReference;
    public String operatingSystemFamily;

    public ResourceManagementClient resourceManagementClient;
    public NetworkManagementClient networkManagementClient;
    public StorageManagementClient storageManagementClient;
    public ComputeManagementClient computeManagementClient;
    public OkHttpClient.Builder clientBuilder;
    public OkHttpClient httpClient;

    public AzureInstanceContext(AzureInstanceService service,
            ComputeInstanceRequest computeRequest) {
        super(service, computeRequest, AzureNicContext::new);
    }

    /**
     * Hook into parent populate behavior.
     */
    @Override
    protected DeferredResult<AzureInstanceContext> getVMDescription(AzureInstanceContext context) {
        return super.getVMDescription(context)
                // Populate vm name
                .thenApply(ctx -> {
                    ctx.vmName = ctx.child.name != null ? ctx.child.name : ctx.child.id;
                    return ctx;
                });
    }

    @Override
    protected DeferredResult<AzureInstanceContext> customizeContext(AzureInstanceContext context) {
        return DeferredResult.completed(context)
                .thenCompose(this::getNicSecurityGroupResourceGroupStates)
                .thenApply(log("getNicSecurityGroupResourceGroupStates"))
                .thenCompose(this::getNetworks).thenApply(log("getNetworks"))
                .thenCompose(this::getSecurityGroups).thenApply(log("getSecurityGroups"));
    }

    /**
     * @return type safe reference to the service using this context.
     */
    private AzureInstanceService service() {
        return (AzureInstanceService) this.service;
    }

    /**
     * For every NIC lookup associated Azure Subnets as specified by
     * {@code AzureNicContext.networkState.name} and {@code AzureNicContext.subnetState.name}. If
     * any of the subnets is not found leave the {@link AzureNicContext#subnet} as null and proceed
     * without an exception.
     */
    private DeferredResult<AzureInstanceContext> getNetworks(AzureInstanceContext context) {
        if (context.nics.isEmpty()) {
            return DeferredResult.completed(context);
        }

        SubnetsOperations azureClient = service()
                .getNetworkManagementClient(context)
                .getSubnetsOperations();

        List<DeferredResult<Subnet>> getSubnetDRs = context.nics
                .stream()
                // Filter only vNet-Subnet with existing RG state
                .filter(nicCtx -> nicCtx.networkRGState != null)
                .map(nicCtx -> {
                    String msg = "Getting Azure Subnet ["
                            + nicCtx.networkRGState.name + "/"
                            + nicCtx.networkState.name + "/"
                            + nicCtx.subnetState.name
                            + "] for [" + nicCtx.nicStateWithDesc.name + "] NIC for ["
                            + context.vmName
                            + "] VM";

                    AzureDeferredResultServiceCallback<Subnet> handler = new AzureDeferredResultServiceCallback<Subnet>(
                            service(), msg) {
                        @Override
                        protected DeferredResult<Subnet> consumeSuccess(Subnet subnet) {
                            nicCtx.subnet = subnet;
                            return DeferredResult.completed(subnet);
                        }
                    };
                    azureClient.getAsync(
                            nicCtx.networkRGState.name,
                            nicCtx.networkState.name,
                            nicCtx.subnetState.name,
                            null /* expand */,
                            handler);

                    return handler.toDeferredResult();
                })
                .collect(Collectors.toList());

        return DeferredResult.allOf(getSubnetDRs)
                .handle((all, exc) -> {
                    if (exc != null) {
                        String msg = String.format(
                                "Error getting Subnets from Azure for [%s] VM.",
                                context.child.name);
                        throw new IllegalStateException(msg, exc);
                    }
                    return context;
                });
    }

    /**
     * For every NIC lookup associated Azure Security Groups as specified by
     * {@code AzureNicContext.securityGroupState.name}. If any of the security groups is not found
     * leave the {@code AzureNicContext.securityGroup} as null and proceed without an exception.
     */
    private DeferredResult<AzureInstanceContext> getSecurityGroups(AzureInstanceContext context) {
        if (context.nics.isEmpty()) {
            return DeferredResult.completed(context);
        }

        NetworkSecurityGroupsOperations azureClient = service()
                .getNetworkManagementClient(context)
                .getNetworkSecurityGroupsOperations();

        List<DeferredResult<NetworkSecurityGroup>> getSecurityGroupDRs = context.nics
                .stream()
                // Filter only SGs with existing RG state
                .filter(nicCtx -> nicCtx.securityGroupState() != null
                        && nicCtx.securityGroupRGState != null)
                .map(nicCtx -> {
                    String sgName = nicCtx.securityGroupState().name;

                    String msg = "Getting Azure Security Group["
                            + nicCtx.securityGroupRGState.name + "/" + sgName
                            + "] for [" + nicCtx.nicStateWithDesc.name + "] NIC for ["
                            + context.vmName
                            + "] VM";

                    AzureDeferredResultServiceCallback<NetworkSecurityGroup> handler = new AzureDeferredResultServiceCallback<NetworkSecurityGroup>(
                            service(), msg) {
                        @Override
                        protected DeferredResult<NetworkSecurityGroup> consumeSuccess(
                                NetworkSecurityGroup securityGroup) {
                            nicCtx.securityGroup = securityGroup;
                            return DeferredResult.completed(securityGroup);
                        }
                    };
                    azureClient.getAsync(
                            nicCtx.securityGroupRGState.name,
                            sgName,
                            null /* expand */,
                            handler);

                    return handler.toDeferredResult();
                })
                .collect(Collectors.toList());

        return DeferredResult.allOf(getSecurityGroupDRs)
                .handle((all, exc) -> {
                    if (exc != null) {
                        String msg = String.format(
                                "Error getting Security Group from Azure for [%s] VM.",
                                context.child.name);
                        throw new IllegalStateException(msg, exc);
                    }
                    return context;
                });
    }

    /**
     * Get {@link ResourceGroupState}s of the {@link SecurityGroupState}s the NICs are assigned to.
     * If any of the RGs is not specified or not found leave the
     * {@link AzureNicContext#securityGroupRGState} as null and proceed without an exception.
     */
    private DeferredResult<AzureInstanceContext> getNicSecurityGroupResourceGroupStates(
            AzureInstanceContext context) {

        if (context.nics.isEmpty()) {
            return DeferredResult.completed(context);
        }

        List<DeferredResult<Void>> getStatesDR = context.nics
                .stream()
                // Filter only SGs with existing RG links
                .filter(nicCtx -> nicCtx.securityGroupState() != null
                        && nicCtx.securityGroupState().groupLinks != null
                        && !nicCtx.securityGroupState().groupLinks.isEmpty())
                .map(nicCtx -> {
                    Set<String> groupLinks = nicCtx.securityGroupState().groupLinks;

                    // NOTE: Get first RG Link! If there are more than one link log a warning.
                    String rgLink = groupLinks.iterator().next();

                    if (groupLinks.size() > 1) {
                        context.service.logSevere(
                                "More than one resource group links are assigned to [%s] NIC's"
                                        + " Security Group state. Get: %s",
                                nicCtx.securityGroupState().name, rgLink);
                    }

                    Operation getRGOp = Operation.createGet(context.service.getHost(), rgLink);

                    // Get the RG by link
                    return context.service
                            .sendWithDeferredResult(getRGOp, ResourceGroupState.class)
                            .thenAccept(rgState -> nicCtx.securityGroupRGState = rgState);
                })
                .collect(Collectors.toList());

        return DeferredResult.allOf(getStatesDR).handle((all, exc) -> {
            if (exc != null) {
                String msg = String.format(
                        "Error getting ResourceGroup states of NIC Security Group states for "
                                + "[%s] VM.",
                        context.child.name);
                throw new IllegalStateException(msg, exc);
            }
            return context;
        });
    }

}
