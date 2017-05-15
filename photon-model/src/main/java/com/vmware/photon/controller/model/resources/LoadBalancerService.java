/*
 * Copyright (c) 2015-2017 VMware, Inc. All Rights Reserved.
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

package com.vmware.photon.controller.model.resources;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.vmware.photon.controller.model.ServiceUtils;
import com.vmware.photon.controller.model.UriPaths;
import com.vmware.photon.controller.model.constants.PhotonModelConstants;
import com.vmware.photon.controller.model.resources.ComputeService.ComputeState;
import com.vmware.photon.controller.model.resources.EndpointService.EndpointState;
import com.vmware.photon.controller.model.resources.LoadBalancerDescriptionService.LoadBalancerDescription;
import com.vmware.photon.controller.model.resources.SubnetService.SubnetState;
import com.vmware.xenon.common.DeferredResult;
import com.vmware.xenon.common.Operation;
import com.vmware.xenon.common.ServiceDocument;
import com.vmware.xenon.common.ServiceDocumentDescription.PropertyUsageOption;
import com.vmware.xenon.common.StatefulService;
import com.vmware.xenon.common.UriUtils;
import com.vmware.xenon.common.Utils;

/**
 * Represents the actual state of a load balancer.
 */
public class LoadBalancerService extends StatefulService {

    public static final String FACTORY_LINK = UriPaths.RESOURCES_LOAD_BALANCERS;

    public static final String FIELD_NAME_DESCRIPTION_LINKS = "descriptionLink";
    public static final String FIELD_NAME_ENDPOINT_LINK = PhotonModelConstants.FIELD_NAME_ENDPOINT_LINK;
    public static final String FIELD_NAME_REGION_ID = "regionId";
    public static final String FIELD_NAME_COMPUTE_LINKS = "computeLinks";
    public static final String FIELD_NAME_SUBNET_LINKS = "subnetLinks";
    public static final String FIELD_NAME_PROTOCOL = "protocol";
    public static final String FIELD_NAME_PORT = "port";
    public static final String FIELD_NAME_INSTANCE_PROTOCOL = "instanceProtocol";
    public static final String FIELD_NAME_INSTANCE_PORT = "instancePort";

    public static final int MIN_PORT_NUMBER = 1;
    public static final int MAX_PORT_NUMBER = 65535;

    /**
     * Represents the state of a load balancer.
     */
    public static class LoadBalancerState extends ResourceState {
        /**
         * Link to the desired state of the load balancer, if any.
         */
        @UsageOption(option = PropertyUsageOption.OPTIONAL)
        public String descriptionLink;

        /**
         * Link to the cloud account endpoint the load balancer belongs to.
         */
        @UsageOption(option = PropertyUsageOption.REQUIRED)
        public String endpointLink;

        /**
         * Region identifier of this service instance.
         */
        @UsageOption(option = PropertyUsageOption.REQUIRED)
        @UsageOption(option = PropertyUsageOption.AUTO_MERGE_IF_NOT_NULL)
        public String regionId;

        /**
         * Links to the load balanced instances.
         */
        @UsageOption(option = PropertyUsageOption.REQUIRED)
        @UsageOption(option = PropertyUsageOption.AUTO_MERGE_IF_NOT_NULL)
        public Set<String> computeLinks;

        /**
         * List of subnets the load balancer is attached to. Typically these must be in different
         * availability zones, and have nothing to do with the subnets the cluster instances are
         * attached to.
         */
        @UsageOption(option = PropertyUsageOption.REQUIRED)
        @UsageOption(option = PropertyUsageOption.AUTO_MERGE_IF_NOT_NULL)
        public Set<String> subnetLinks;

        /**
         * Load balancer protocol.
         */
        @UsageOption(option = PropertyUsageOption.REQUIRED)
        @UsageOption(option = PropertyUsageOption.AUTO_MERGE_IF_NOT_NULL)
        public String protocol;

        /**
         * The port the load balancer is listening on.
         */
        @UsageOption(option = PropertyUsageOption.REQUIRED)
        @UsageOption(option = PropertyUsageOption.AUTO_MERGE_IF_NOT_NULL)
        public Integer port;

        /**
         * The protocol to use for routing traffic to instances.
         */
        @UsageOption(option = PropertyUsageOption.REQUIRED)
        @UsageOption(option = PropertyUsageOption.AUTO_MERGE_IF_NOT_NULL)
        public String instanceProtocol;

        /**
         * The port on which the instances are listening.
         */
        @UsageOption(option = PropertyUsageOption.REQUIRED)
        @UsageOption(option = PropertyUsageOption.AUTO_MERGE_IF_NOT_NULL)
        public Integer instancePort;

        /**
         * The adapter to use to create the load balancer instance.
         */
        @UsageOption(option = PropertyUsageOption.AUTO_MERGE_IF_NOT_NULL)
        public URI instanceAdapterReference;

        @Override
        public void copyTo(ResourceState target) {
            super.copyTo(target);
            if (target instanceof LoadBalancerState) {
                LoadBalancerState targetState = (LoadBalancerState) target;
                targetState.descriptionLink = this.descriptionLink;
                targetState.endpointLink = this.endpointLink;
                targetState.regionId = this.regionId;
                targetState.computeLinks = this.computeLinks;
                targetState.subnetLinks = this.subnetLinks;
                targetState.protocol = this.protocol;
                targetState.port = this.port;
                targetState.instanceProtocol = this.instanceProtocol;
                targetState.instancePort = this.instancePort;
                targetState.instanceAdapterReference = this.instanceAdapterReference;
            }
        }
    }

    /**
     * Load balancer state with all links expanded.
     */
    public static class LoadBalancerStateExpanded extends LoadBalancerState {
        public LoadBalancerDescription description;
        public EndpointState endpointState;
        public Set<ComputeState> computes;
        public Set<SubnetState> subnets;

        public static URI buildUri(URI loadBalancerStateUri) {
            return UriUtils.buildExpandLinksQueryUri(loadBalancerStateUri);
        }
    }

    public LoadBalancerService() {
        super(LoadBalancerState.class);
        super.toggleOption(ServiceOption.PERSISTENCE, true);
        super.toggleOption(ServiceOption.REPLICATION, true);
        super.toggleOption(ServiceOption.OWNER_SELECTION, true);
        super.toggleOption(ServiceOption.IDEMPOTENT_POST, true);
    }

    @Override
    public void handleGet(Operation get) {
        LoadBalancerState currentState = getState(get);
        boolean doExpand = get.getUri().getQuery() != null &&
                UriUtils.hasODataExpandParamValue(get.getUri());

        if (!doExpand) {
            get.setBody(currentState).complete();
            return;
        }

        LoadBalancerStateExpanded expanded = new LoadBalancerStateExpanded();
        currentState.copyTo(expanded);

        DeferredResult
                .allOf(
                        getDr(currentState.descriptionLink, LoadBalancerDescription.class)
                                .thenAccept(description -> expanded.description = description),
                        getDr(currentState.endpointLink, EndpointState.class)
                                .thenAccept(endpointState -> expanded.endpointState = endpointState),
                        getDr(currentState.computeLinks, ComputeState.class, HashSet::new)
                                .thenAccept(computes -> expanded.computes = computes),
                        getDr(currentState.subnetLinks, SubnetState.class, HashSet::new)
                                .thenAccept(subnets -> expanded.subnets = subnets))
                .whenComplete((ignore, e) -> {
                    if (e != null) {
                        get.fail(e);
                    } else {
                        get.setBody(expanded).complete();
                    }
                });
    }

    @Override
    public void handleStart(Operation start) {
        processInput(start);
        start.complete();
    }

    @Override
    public void handlePut(Operation put) {
        LoadBalancerState returnState = processInput(put);
        setState(put, returnState);
        put.complete();
    }

    private LoadBalancerState processInput(Operation op) {
        if (!op.hasBody()) {
            throw (new IllegalArgumentException("body is required"));
        }
        LoadBalancerState state = op.getBody(LoadBalancerState.class);
        validateState(state);
        return state;
    }

    @Override
    public void handlePatch(Operation patch) {
        LoadBalancerState currentState = getState(patch);
        ResourceUtils.handlePatch(patch, currentState, getStateDescription(),
                LoadBalancerState.class, null);
    }

    private void validateState(LoadBalancerState state) {
        Utils.validateState(getStateDescription(), state);
        if (state.port < MIN_PORT_NUMBER || state.port > MAX_PORT_NUMBER) {
            throw new IllegalArgumentException("Invalid load balancer port number.");
        }
        if (state.instancePort < MIN_PORT_NUMBER || state.instancePort > MAX_PORT_NUMBER) {
            throw new IllegalArgumentException("Invalid instance port number.");
        }
    }

    @Override
    public ServiceDocument getDocumentTemplate() {
        ServiceDocument td = super.getDocumentTemplate();
        ServiceUtils.setRetentionLimit(td);
        LoadBalancerState template = (LoadBalancerState) td;

        template.id = UUID.randomUUID().toString();
        template.descriptionLink = "lb-description-link";
        template.name = "load-balancer";
        template.endpointLink = UriUtils.buildUriPath(EndpointService.FACTORY_LINK,
                "my-endpoint");
        template.protocol = "HTTP";
        template.port = 80;
        template.instanceProtocol = "HTTP";
        template.instancePort = 80;

        return template;
    }

    private <T> DeferredResult<T> getDr(String link, Class<T> type) {
        if (link == null) {
            return DeferredResult.completed(null);
        }
        return sendWithDeferredResult(Operation.createGet(this, link), type);
    }

    private <T, C extends Collection<T>> DeferredResult<C> getDr(Collection<String> links,
            Class<T> type, Supplier<C> collectionFactory) {
        if (links == null) {
            return DeferredResult.completed(null);
        }
        return DeferredResult
                .allOf(links.stream().map(link -> getDr(link, type)).collect(Collectors.toList()))
                .thenApply(items -> items.stream()
                        .collect(Collectors.toCollection(collectionFactory)));
    }
}