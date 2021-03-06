/*
 * Copyright (c) 2017 VMware, Inc. All Rights Reserved.
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

package com.vmware.photon.controller.model.tasks;

import static java.util.stream.Collectors.toList;

import static com.vmware.xenon.common.ServiceDocumentDescription.PropertyIndexingOption.STORE_ONLY;
import static com.vmware.xenon.common.ServiceDocumentDescription.PropertyUsageOption.SERVICE_USE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.vmware.photon.controller.model.UriPaths;
import com.vmware.photon.controller.model.query.QueryUtils;
import com.vmware.photon.controller.model.resources.IPAddressService;
import com.vmware.photon.controller.model.resources.IPAddressService.IPAddressState;
import com.vmware.photon.controller.model.resources.IPAddressService.IPAddressState.IPAddressStatus;
import com.vmware.photon.controller.model.resources.SubnetRangeService.SubnetRangeState;
import com.vmware.photon.controller.model.resources.SubnetService.SubnetState;
import com.vmware.photon.controller.model.support.IPVersion;
import com.vmware.photon.controller.model.tasks.ServiceTaskCallback.ServiceTaskCallbackResponse;
import com.vmware.photon.controller.model.util.ClusterUtil.ServiceTypeCluster;
import com.vmware.photon.controller.model.util.IpHelper;
import com.vmware.xenon.common.DeferredResult;
import com.vmware.xenon.common.Operation;
import com.vmware.xenon.common.Service;
import com.vmware.xenon.common.ServiceDocument;
import com.vmware.xenon.common.ServiceDocumentDescription;
import com.vmware.xenon.common.ServiceErrorResponse;
import com.vmware.xenon.common.TaskState;
import com.vmware.xenon.common.UriUtils;
import com.vmware.xenon.common.Utils;
import com.vmware.xenon.services.common.QueryTask;
import com.vmware.xenon.services.common.TaskService;

/**
 * Task implementing the allocation of an IP address.
 */
public class IPAddressAllocationTaskService extends
        TaskService<IPAddressAllocationTaskService.IPAddressAllocationTaskState> {

    public static final String FACTORY_LINK = UriPaths.TASKS + "/ip-address-allocation-tasks";
    public static final String ID_SEPARATOR = "_";

    public static final String CUSTOM_PROPERTY_SUFFIX_IP_ADDRESS_LINK = "-IPAddressLink";

    /**
     * Service context that is created for passing intermediate data between async calls.
     * Used only during allocation.
     */
    public static class IPAddressAllocationContext {
        /*Subnet state document*/
        public SubnetState subnetState;

        /* Enumeration of subnet range states*/
        public List<SubnetRangeState> subnetRangeStates;

        /*Resource requesting IP address*/
        @Deprecated
        public String connectedResourceLink;

        /*A reference to state that gets passed in to the request*/
        public IPAddressAllocationTaskState serviceState;

        /*All the ips associated with the subnet state*/
        public List<IPAddressState> ipAddressStates;

        /*Existing IP addresses in the subnet that are not available*/
        public Set<Long> unavailableIpAddresses;

        /*The string is the resource link. And the list is the list of all ip addresses assigned
          to that resource.*/
        public Map<String, List<IPAddressState>> connectedResourceToAllocatedIpsMap = new ConcurrentHashMap<>();

        /*This field is a summation of all the IPs required by all the resources*/
        public int requestedIpCount;

        /*The total possible IP addresses in all the subnet ranges*/
        public int maxPossibleIpsCount;

        public DeferredResult<IPAddressAllocationContext> populate(
                IPAddressAllocationTaskService ipAddressAllocationTaskService,
                IPAddressAllocationTaskState state) {

            this.serviceState = state;
            this.connectedResourceLink = state.connectedResourceLink;
            return this.populateContextWithSubnet(ipAddressAllocationTaskService, state.subnetLink)
                    .thenCompose(ctxt -> populateContextWithExistingSubnetRanges
                            (ipAddressAllocationTaskService))
                    .thenCompose(ctxt -> populateContextWithExistingIpAddresses
                            (ipAddressAllocationTaskService))
                    .thenCompose(ctxt -> this.setUnAvailableIps())
                    .thenCompose(ctxt -> this.setRequiredIpCounts());
        }

        /**
         * Retrieve the subnet state by its self link and add it to the context.
         *
         * @param subnetResourceLink Document self link of the subnet document to be retrieved.
         *                           Example: /resources/sub-networks/<some GUID>
         * @return Subnet document.
         */
        private DeferredResult<IPAddressAllocationContext> populateContextWithSubnet(
                IPAddressAllocationTaskService ipAddressAllocationTaskService,
                String subnetResourceLink) {
            return ipAddressAllocationTaskService.sendWithDeferredResult(
                    Operation.createGet(ipAddressAllocationTaskService, subnetResourceLink))
                    .thenApply(o -> {
                        this.subnetState = o.getBody(SubnetState.class);
                        return this;
                    });
        }

        /**
         * Retrieves subnet ranges, that belong to a subnet and adds it to the context.
         *
         * @return Sets list of subnet range states for the subnet resource link and returns the context.
         */
        private DeferredResult<IPAddressAllocationContext> populateContextWithExistingSubnetRanges(
                IPAddressAllocationTaskService ipAddressAllocationTaskService) {
            QueryTask.Query.Builder builder = QueryTask.Query.Builder.create()
                    .addKindFieldClause(SubnetRangeState.class)
                    .addFieldClause(SubnetRangeState.FIELD_NAME_SUBNET_LINK,
                            this.subnetState.documentSelfLink);

            QueryUtils.QueryByPages<SubnetRangeState> query = new QueryUtils.QueryByPages<>(ipAddressAllocationTaskService.getHost(),
                            builder.build(),
                            SubnetRangeState.class,
                            null);
            //todo: add tenantLinks

            query.setClusterType(ServiceTypeCluster.INVENTORY_SERVICE);

            return query.collectDocuments(toList())
                    .thenApply(subnetRangeStatesRet -> {
                        this.subnetRangeStates = subnetRangeStatesRet;
                        return this;
                    });
        }

        /**
         * This method finds all the IP address documents in the subnet and adds it to the context.
         *
         * The way this is done is, we iterate thru all the subnet ranges in the subnet. We get the
         * subnet range document self link. Then we query IP address docs that hold a reference to this
         * subnet range document self link.
         * @return
         */
        private DeferredResult<IPAddressAllocationContext> populateContextWithExistingIpAddresses(
                IPAddressAllocationTaskService ipAddressAllocationTaskService) {

            this.ipAddressStates = new ArrayList<>();

            List<DeferredResult<List<IPAddressState>>> results = new ArrayList<>();

            for (SubnetRangeState subnetRangeState : this.subnetRangeStates) {
                results.add(retrieveExistingIpAddressesFromRange(ipAddressAllocationTaskService,
                        subnetRangeState));
            }

            return DeferredResult.allOf(results).thenApply((lstIps) -> {
                lstIps.forEach((item) -> this.ipAddressStates.addAll(item));

                return this;
            });
        }

        private DeferredResult<IPAddressAllocationContext> setUnAvailableIps() {

            this.unavailableIpAddresses = new HashSet<>();
            DeferredResult<IPAddressAllocationContext> deferredContext =
                    new DeferredResult<>();

            List<IPAddressState> existingIpStates = this.ipAddressStates;

            //Add ips that are not available
            for (IPAddressState ipState : existingIpStates) {
                if (ipState.ipAddressStatus != IPAddressState.IPAddressStatus.AVAILABLE) {
                    this.unavailableIpAddresses
                            .add(IpHelper.ipStringToLong(ipState.ipAddress));
                }
            }
            return deferredContext.completed(this);
        }

        /*Every connected resource has a count that tells you how many IPs are needed */
        private DeferredResult<IPAddressAllocationContext> setRequiredIpCounts() {

            DeferredResult<IPAddressAllocationContext> deferredResult = new DeferredResult<>();
            this.requestedIpCount = this.serviceState.connectedResourceToRequiredIpCountMap
                    .values().stream().mapToInt(i -> i).sum();
            return deferredResult.completed(this);
        }

        /**
         * Retrieves existing IP address resources created with different status within a specific range
         *
         * @param rangeState Subnet range information.
         * @return List of IP Addresses created within that range.
         */
        private DeferredResult<List<IPAddressState>> retrieveExistingIpAddressesFromRange(
                IPAddressAllocationTaskService ipAddressAllocationTaskService,
                SubnetRangeState rangeState) {
            QueryTask.Query getIpAddressQuery = QueryTask.Query.Builder.create()
                    .addFieldClause(IPAddressState.FIELD_NAME_SUBNET_RANGE_LINK,
                            rangeState.documentSelfLink)
                    .build();

            QueryUtils.QueryByPages<IPAddressState> queryByPages = new QueryUtils.QueryByPages<>(
                    ipAddressAllocationTaskService.getHost(),
                    getIpAddressQuery,
                    IPAddressState.class, null);
            queryByPages.setClusterType(ServiceTypeCluster.INVENTORY_SERVICE);

            return queryByPages.collectDocuments(toList());
        }

    }

    /**
     * Class that represents the result of IP address allocation.
     */
    public static class IPAddressAllocationTaskResult
            extends ServiceTaskCallbackResponse<IPAddressAllocationTaskState.SubStage> {
        public IPAddressAllocationTaskResult(TaskState.TaskStage taskStage, Object taskSubStage,
                ServiceErrorResponse failure) {
            super(taskStage, taskSubStage, new HashMap<>(), failure);
        }

        /*Resource link of the IP address being allocated.*/
        public List<String> ipAddressLinks;

        /*IP addresses being allocated.*/
        @Deprecated
        public List<String> ipAddresses;

        /*IP ranges from which IP address is allocated.*/
        public List<String> subnetRangeLinks;

       /*Connected resource that is associated with IP address(es) */
        @Deprecated
        public String connectedResourceLink;

        /*Maps connected resource link to ip address link*/
        public Map<String, List<String>> resourceToAllocatedIPsMap;
    }

    /**
     * Represents the state of IP allocation task.
     */
    public static class IPAddressAllocationTaskState extends TaskService.TaskServiceState {

        /**
         * SubStage.
         */
        public enum SubStage {
            CREATED, ALLOCATE_IP_ADDRESS, DEALLOCATE_IP_ADDRESS, FINISHED, FAILED
        }

        public enum RequestType {
            ALLOCATE,
            DEALLOCATE
        }

        /**
         * (Internal) Describes task sub-stage.
         */
        @ServiceDocument.Documentation(description = "Describes task sub-stage.")
        @PropertyOptions(usage = { SERVICE_USE }, indexing = STORE_ONLY)
        public SubStage taskSubStage;

        /**
         * The type of the request (allocate or deallocate).
         */
        @ServiceDocument.Documentation(
                description = "Request type -whether to allocate or de-allocate.")
        @ServiceDocument.PropertyOptions(usage = {
                ServiceDocumentDescription.PropertyUsageOption.REQUIRED,
                ServiceDocumentDescription.PropertyUsageOption.SINGLE_ASSIGNMENT })
        public RequestType requestType;

        /**
         * In case of allocation request: id of the subnet for which IP address will be allocated.
         * Not used for de-allocation.
         */
        @ServiceDocument.Documentation(
                description = "The subnet from which IP address needs to be allocated.")
        @ServiceDocument.PropertyOptions(usage = {
                ServiceDocumentDescription.PropertyUsageOption.SINGLE_ASSIGNMENT })
        public String subnetLink;

        /**
         * For allocation, set by the task with the resource links of IP address being allocated.
         * Required for de-allocation.
         */
        @ServiceDocument.Documentation(
                description = "Resource links of the IP address being allocated.")
        @ServiceDocument.PropertyOptions(usage = {
                ServiceDocumentDescription.PropertyUsageOption.AUTO_MERGE_IF_NOT_NULL })
        @Deprecated
        public List<String> ipAddressLinks;

        /**
         * Connected resource that is associated with IP address(es)
         */
        @ServiceDocument.Documentation(
                description = "The connected resource associated with IP address(es).")
        @ServiceDocument.PropertyOptions(usage = {
                ServiceDocumentDescription.PropertyUsageOption.SINGLE_ASSIGNMENT })
        @Deprecated
        public String connectedResourceLink;

        /**
         * Connected resources that have IP addresses
         * The string is the link of the connected resource
         * The int is the number of IPs requested for that resource
         */
        @ServiceDocument.Documentation(
                description = "A map where the key is the connected resource link and the value "
                        + "is the number of ip addresses required.")
        @ServiceDocument.PropertyOptions(usage = {
                ServiceDocumentDescription.PropertyUsageOption.SINGLE_ASSIGNMENT })
        public Map<String, Integer> connectedResourceToRequiredIpCountMap;

        /**
         * For allocation, set by the task with the IP addresses being allocated.
         * Not used for de-allocation.
         */
        @ServiceDocument.Documentation(description = "The IP address being allocated.")
        @ServiceDocument.PropertyOptions(usage = {
                ServiceDocumentDescription.PropertyUsageOption.AUTO_MERGE_IF_NOT_NULL })
        @Deprecated
        public List<String> ipAddresses;

        /**
         * For allocation, number of IP addresses to allocate.
         * Not used for de-allocation.
         */
        @Deprecated
        public int allocationCount;

        /**
         * For allocation, set by the task with the ip range from which IP address is allocated.
         * Not used for de-allocation.
         */
        @ServiceDocument.Documentation(
                description = "Resource link of the IP range from which IP address is being "
                        + "allocated.")
        @ServiceDocument.PropertyOptions(usage = {
                ServiceDocumentDescription.PropertyUsageOption.AUTO_MERGE_IF_NOT_NULL })
        public List<String> subnetRangeLinks;

        /**
         * A callback to the initiating task.
         */
        @ServiceDocument.Documentation(description = "A callback to the initiating task.")
        @ServiceDocument.PropertyOptions(usage = {
                ServiceDocumentDescription.PropertyUsageOption.SINGLE_ASSIGNMENT })
        public ServiceTaskCallback<?> serviceTaskCallback;

        @ServiceDocument.Documentation(
                description = "The IP addresses allocated to connected resources.")
        @ServiceDocument.PropertyOptions(usage = {
                ServiceDocumentDescription.PropertyUsageOption.AUTO_MERGE_IF_NOT_NULL })
        public Map<String, List<String>> rsrcToAllocatedIpsMap;

        public IPAddressAllocationTaskState() {
            this.ipAddresses = new ArrayList<>();
            this.ipAddressLinks = new ArrayList<>();
            this.subnetRangeLinks = new ArrayList<>();
            this.rsrcToAllocatedIpsMap = new ConcurrentHashMap<>();
        }

        @Override
        public String toString() {
            String lineSeparator = System.getProperty("line.separator");
            StringBuilder sb = new StringBuilder();
            sb.append(lineSeparator);
            sb.append("request type: ").append(this.requestType).append(lineSeparator);
            sb.append("task stage: ").append(this.taskSubStage).append(lineSeparator);
            sb.append("subnet link: ").append(this.subnetLink).append(lineSeparator);
            sb.append("subnet range links: ");
            sb.append(this.subnetRangeLinks.stream().collect(Collectors.joining(",")))
                    .append(lineSeparator);

            Map<String, List<String>> allocatedMap = this.rsrcToAllocatedIpsMap;

            String allocatedMapStr = allocatedMap.keySet().stream().map(key -> String.format
                    ("Resource: %s  "
                                    + "Allocated IP Addresses: %s", key,
                            String.join(",", allocatedMap.get(key)))).collect
                    (Collectors.joining(lineSeparator));

            sb.append(allocatedMapStr).append(lineSeparator);

            return sb.toString();
        }
    }

    /*
    We throw this exception when an attempted modification of IP address returns a 304 not
    modified. This typically happens when two threads try to acquire the same IP simultaneously.
    */
    public static class ConcurrentRequestException extends CompletionException {
        public ConcurrentRequestException(String message) {
            super(message);
        }
    }

    public IPAddressAllocationTaskService() {
        super(IPAddressAllocationTaskState.class);
        super.toggleOption(Service.ServiceOption.PERSISTENCE, true);
        super.toggleOption(Service.ServiceOption.REPLICATION, true);
        super.toggleOption(Service.ServiceOption.OWNER_SELECTION, true);
        super.toggleOption(Service.ServiceOption.INSTRUMENTATION, true);
    }

    @Override
    public void handlePatch(Operation patch) {
        IPAddressAllocationTaskState body = getBody(patch);
        IPAddressAllocationTaskState currentState = getState(patch);

        if (validateTransitionAndUpdateState(patch, body, currentState)) {
            return;
        }

        patch.complete();

        switch (currentState.taskInfo.stage) {
        case CREATED:
            break;
        case STARTED:
            handleSubStagePatch(currentState);
            break;
        case FINISHED:
            logInfo(() -> "Task is complete");
            sendCallbackResponse(currentState);
            break;
        case FAILED:
            logInfo(() -> "Task failed");
            sendCallbackResponse(currentState);
            break;
        default:
            break;
        }
    }

    protected void handleSubStagePatch(IPAddressAllocationTaskState state) {
        switch (state.taskSubStage) {
        case CREATED:
            if (state.requestType == IPAddressAllocationTaskState.RequestType.ALLOCATE) {
                proceedTo(IPAddressAllocationTaskState.SubStage.ALLOCATE_IP_ADDRESS,
                        s -> s.subnetLink = state.subnetLink);
            } else {
                proceedTo(IPAddressAllocationTaskState.SubStage.DEALLOCATE_IP_ADDRESS,
                        s -> s.ipAddressLinks = state.ipAddressLinks);
            }
            break;

        case ALLOCATE_IP_ADDRESS:
            allocateIpAddress(state);
            break;

        case DEALLOCATE_IP_ADDRESS:
            deallocateIpAddress(state);
            break;

        case FAILED:
            logWarning(() -> String.format("Task failed with %s",
                    Utils.toJsonHtml(state.taskInfo.failure)));
            sendSelfPatch(state, TaskState.TaskStage.FAILED, null);
            break;

        case FINISHED:
            sendSelfPatch(state, TaskState.TaskStage.FINISHED, null);
            break;

        default:
            break;
        }
    }

    @Override
    public void handleStart(Operation startOp) {
        try {
            IPAddressAllocationTaskState taskState = validateStartPost(startOp);
            if (startOp == null) {
                return;
            }

            initializeState(taskState, startOp);

            // Send completion to the caller (with a CREATED state)
            startOp.setBody(taskState).complete();

            // And then start internal state machine
            sendSelfPatch(taskState, TaskState.TaskStage.STARTED, null);
        } catch (Throwable e) {
            logSevere(e);
            startOp.fail(e);
        }
    }

    /**
     * Customize the validation logic that's part of initial {@code POST} creating the task service.
     *
     * @see #handleStart(Operation)
     */
    @Override
    protected IPAddressAllocationTaskState validateStartPost(Operation startOp) {
        try {
            IPAddressAllocationTaskState state = startOp
                    .getBody(IPAddressAllocationTaskState.class);
            if (state == null) {
                throw new IllegalArgumentException("IPAddressAllocationTaskState is required");
            }

            if (state.requestType == null) {
                throw new IllegalArgumentException("state.requestType is required");
            }

            if (state.requestType == IPAddressAllocationTaskState.RequestType.ALLOCATE) {

                if (state.subnetLink == null) {
                    throw new IllegalArgumentException("state.subnetLink is required");
                }

                if (StringUtils.isEmpty(state.connectedResourceLink) &&
                        (state.connectedResourceToRequiredIpCountMap == null
                                || state.connectedResourceToRequiredIpCountMap.size() == 0)) {
                    throw new IllegalArgumentException(
                            "state.connectedResourceToReqIpCountMap is required");
                }
            } else if (state.ipAddressLinks == null || state.ipAddressLinks.isEmpty()) {
                throw new IllegalArgumentException("state.ipAddressLinks is required");
            }

            return state;
        } catch (Throwable e) {
            logSevere(e);
            startOp.fail(e);
            return null;
        }
    }

    /**
     * Customize the initialization logic (set the task with default values) that's part of initial
     * {@code POST} creating the task service.
     *
     * @see #handleStart(Operation)
     */
    @Override
    protected void initializeState(IPAddressAllocationTaskState startState, Operation startOp) {
        if (startState.taskInfo == null || startState.taskInfo.stage == null) {
            startState.taskInfo = new TaskState();
            startState.taskInfo.stage = TaskState.TaskStage.CREATED;
        }

        startState.taskSubStage = IPAddressAllocationTaskState.SubStage.CREATED;
    }

    /**
     * De-allocates an IP address, based on IP address resource link.
     *
     * @param state IP Address allocation task state.
     */
    private void deallocateIpAddress(IPAddressAllocationTaskState state) {
        IPAddressState addressState = new IPAddressState();
        addressState.connectedResourceLink = state.connectedResourceLink;
        addressState.ipAddressStatus = IPAddressState.IPAddressStatus.RELEASED;

        List<DeferredResult<Operation>> deferredResults = new ArrayList<>();
        for (int i = 0; i < state.ipAddressLinks.size(); i++) {
            String ipAddressResourceLink = state.ipAddressLinks.get(i);
            Operation patchOp = Operation.createPatch(this, ipAddressResourceLink)
                    .setBody(addressState);
            deferredResults.add(this.sendWithDeferredResult(patchOp));
        }

        DeferredResult.allOf(deferredResults).thenAccept(
                dr -> proceedTo(IPAddressAllocationTaskState.SubStage.FINISHED, null))
                .exceptionally(e -> {
                    if (e != null) {
                        failTask(e, "Failed to de-allocate IP addresses due to failure %s",
                                e.getMessage());
                    }
                    return null;
                });
    }

    /**
     * Allocates IP Address for a subnet
     *
     * @param state IP Address allocation task state.
     */
    private void allocateIpAddress(IPAddressAllocationTaskState state) {

        //If the newly added connectedResourceToReqIpCountMap is not set,
        // then pick value from the old deprecated field to maintain backward compatibility
        if (state.connectedResourceToRequiredIpCountMap == null
                || state.connectedResourceToRequiredIpCountMap.size() == 0) {
            state.connectedResourceToRequiredIpCountMap = new ConcurrentHashMap<>();
            state.connectedResourceToRequiredIpCountMap.put(state.connectedResourceLink, 1);
        }

        IPAddressAllocationContext ipAddressAllocationContext = new IPAddressAllocationContext();

        ipAddressAllocationContext.populate(this, state)
                .thenAccept(ctxt ->
                {
                    if (ctxt.subnetRangeStates == null || ctxt.subnetRangeStates.size() == 0) {
                        // ignore this particular error for now and complete task, as certain
                        // providers (e.g., AWS, Azure) do not have address ranges defined.
                        // TODO: this task would eventually need to delegate IP address allocation to an
                        // adapter, which will take the appropriate action based on the adapter type
                        logWarning(() -> String.format("No IP addresses can be allocated for "
                                + "subnet %s. There are no address ranges available for this "
                                + "subnet.", ctxt.subnetState.documentSelfLink));
                        proceedTo(IPAddressAllocationTaskState.SubStage.FINISHED, null);
                        return;
                    }
                    checkEnoughIPsAvailableOrFail(ctxt);
                    int requiredIpCount = ctxt.requestedIpCount;

                    allocateFromExistingIPswithRetry(ctxt)
                            .thenCompose((outCtxt) ->
                            {
                                int allocatedIpCount = getAllocatedIpsCount(outCtxt);

                                if (allocatedIpCount < requiredIpCount) {
                                    //When we allocate IP addresses we first try to pick from existing IP
                                    // address documents, that are in available state. Usually, these are
                                    // IP addresses that became available from a previous de-allocation.
                                    // If we can't fulfill out IP address requirements by these pre existing
                                    // IP addresses, then we create new IP address documents in lucene
                                    logInfo("Could not allocate from existing IP address documents. "
                                            + "Hence creating new IPs.");
                                    return createNewIpsWithRetry(outCtxt);
                                } else {
                                    //else pass through
                                    return DeferredResult.completed(outCtxt);
                                }
                            })
                            .thenAccept((context) -> proceedTo(
                                    IPAddressAllocationTaskState.SubStage.FINISHED, s -> addResultToState(context, s)))
                            .exceptionally((e) -> {
                                if (e != null) {
                                    failTask(e, "Failed to allocate IP addresses due to failure %s",
                                            e.getMessage());
                                }
                                return null;
                            });
                })
                .exceptionally((e) -> {
                    if (e != null) {
                        failTask(e, "Failed to allocate IP addresses due to failure %s",
                                e.getMessage());
                    }
                    return null;
                });
    }

    /**
     * Here we try to allocate IPs from existing IP address docs by finding docs
     * whose status is set to available.
     *
     * We iterate thru every connected resource that had an IP address requested for.
     * If that resource had lesser IPs than it needed, then we try to find if there are any
     * existing available IPs in the subnet, that can be assigned to these resources.
     *
     * @param context Allocation context information.
     */
    private DeferredResult<IPAddressAllocationContext> allocateFromExistingIPs(
            IPAddressAllocationContext context) {

        try {
            Map<String, Integer> connectedResourceToReqIpCountMap =
                    context.serviceState.connectedResourceToRequiredIpCountMap;
            List<DeferredResult<Void>> results = new ArrayList<>();

            for (String connectedResourceLink : connectedResourceToReqIpCountMap.keySet()) {
                //Try allocating from existing IPs

                int ipsRequiredForResCount = connectedResourceToReqIpCountMap
                        .get(connectedResourceLink);

                int allocatedIpCountForRes = 0;

                if (context.connectedResourceToAllocatedIpsMap.get
                        (connectedResourceLink) != null) {
                    //Check how many IPs have already been allocated to this resource
                    allocatedIpCountForRes = context.connectedResourceToAllocatedIpsMap.get
                            (connectedResourceLink).size();
                }

                for (int i = allocatedIpCountForRes; i < ipsRequiredForResCount; i++) {
                    results.add(assignExistingIpToResource(context, connectedResourceLink)
                            .thenAccept((ipAddressState) -> {
                                if (ipAddressState != null) {
                                    String ipAddress = ipAddressState.ipAddress;
                                    logInfo("Successfully allocated IP %s for resource %s",
                                            ipAddress, connectedResourceLink);
                                    long longIpAddr = IpHelper.ipStringToLong(ipAddress);
                                    context.unavailableIpAddresses.add(longIpAddr);
                                    addIpToContext(context, connectedResourceLink,
                                            ipAddressState);
                                }
                            }));
                }

            }

            return DeferredResult.allOf(results).thenApply((ignore) -> context);
        } catch (Exception ex) {
            return new DeferredResult<>().failed(ex);
        }

    }

    /**
     * The context has all the existing IP address documents owned by the subnet.
     * We go through each of those IP address docs and check if it's status is available.
     * If it is, we allocate it to the passed in, connected resource.
     *
     * @param context IPAddressAllocationContext
     * @param connectedResourceLink The resource that needs an IP address
     * @return A DeferredResult of an IPAddressState
     *
     */
    private DeferredResult<IPAddressState> assignExistingIpToResource(IPAddressAllocationContext context,
            String connectedResourceLink) {

        List<IPAddressState> existingIpAddressStates = context.ipAddressStates;
        DeferredResult<IPAddressState> returnIp = new DeferredResult<>();

        for (IPAddressState addressState : existingIpAddressStates) {
            Long longIp = IpHelper.ipStringToLong(addressState.ipAddress);
            if (addressState.ipAddressStatus == IPAddressState.IPAddressStatus.AVAILABLE
                    && (!context.unavailableIpAddresses.contains(longIp))) {
                context.unavailableIpAddresses.add(longIp);
                addressState.ipAddressStatus = IPAddressState.IPAddressStatus.ALLOCATED;
                addressState.connectedResourceLink = connectedResourceLink;
                logInfo("Picking an existing IP %s for resource %s", addressState.ipAddress,
                        connectedResourceLink);
                return updateExistingIpAddressResource(addressState, context);
            } else {
                context.unavailableIpAddresses.add(longIp);
            }
        }

        returnIp.complete(null);
        return returnIp;
    }

    /**
     * We go thru all the resources and check if they got enough IPs allocated to them.
     * If not we create new IPs (within the subnet ranges) for them.
     *
     * @param context IPAddressAllocationContext
     * @return a deferred result of IPAddressAllocationContext
     */
    private DeferredResult<IPAddressAllocationContext> createNewIps(
            IPAddressAllocationContext context) {
        try {
            Map<String, Integer> connectedResourceLinks = context.serviceState.connectedResourceToRequiredIpCountMap;

            List<DeferredResult<Void>> results = new ArrayList<>();

            for (String connectedResourceLink : connectedResourceLinks.keySet()) {

                int ipsRequiredForResCount = connectedResourceLinks.get(connectedResourceLink);
                int allocatedIpCountForRes = 0;

                if (context.connectedResourceToAllocatedIpsMap.get(connectedResourceLink) != null) {
                   //Check how many IPs have already been allocated to this resource
                    allocatedIpCountForRes = context.connectedResourceToAllocatedIpsMap.get
                            (connectedResourceLink).size();
                }

                for (int i = allocatedIpCountForRes; i < ipsRequiredForResCount; i++) {

                    results.add(
                            createNewIpForSingleResource(context, connectedResourceLink).thenAccept(
                                    (ipAddressState) ->
                                    {
                                        if (ipAddressState != null) {
                                            String ipAddress = ipAddressState.ipAddress;
                                            logInfo("Successfully created IP %s for resource %s",
                                                    ipAddress, connectedResourceLink);
                                            long longIpAddr = IpHelper.ipStringToLong(ipAddress);
                                            context.unavailableIpAddresses.add(longIpAddr);
                                            addIpToContext(context, connectedResourceLink,
                                                    ipAddressState);
                                        }
                                    }
                            ));

                }

            }

            return DeferredResult.allOf(results).thenApply((ignore) -> context);

        } catch (Exception ex) {
            return new DeferredResult<>().failed(ex);
        }

    }

    private DeferredResult<IPAddressState> createNewIpForSingleResource(IPAddressAllocationContext context,
            String connectedResourceLink) {

        List<SubnetRangeState> subnetRangeStates = context.subnetRangeStates;
        DeferredResult<IPAddressState> deferredIpAddr;

        for (SubnetRangeState subnetRangeState : subnetRangeStates) {

            if (!subnetRangeState.ipVersion.equals(IPVersion.IPv4)) {
                //If IPV6 then skip to next IP range.
                logWarning(
                        "Skipping allocation from IP address range %s. Currently, only IPV4 is supported",
                        subnetRangeState.documentSelfLink);
                continue;
            }

            deferredIpAddr = createIpInRange(context, connectedResourceLink, subnetRangeState);

            if (deferredIpAddr != null) {
                return deferredIpAddr;
            }

        }

        String msg = String.format("Couldn't allocate an IP address for resource %s. No IP "
                + "addresses were remaining", connectedResourceLink);

        return (new DeferredResult<>()).failed(new Exception(msg));
    }

    /**
     * This method goes thru all the IP addresses in the subnet range. If that IP is not in the
     * unavailable IP list, then a new IP address doc for that IP gets created.
     * @param context
     * @param connectedResourceLink
     * @param subnetRangeState
     * @return
     */
    private DeferredResult<IPAddressState> createIpInRange(IPAddressAllocationContext context,
            String connectedResourceLink, SubnetRangeState subnetRangeState) {
        DeferredResult<IPAddressState> returnIp = null;
        long beginAddress = IpHelper.ipStringToLong(subnetRangeState.startIPAddress);
        long endAddress = IpHelper.ipStringToLong(subnetRangeState.endIPAddress);

        for (long address = beginAddress; address <= endAddress; address++) {
            if (!context.unavailableIpAddresses.contains(address)) {
                context.unavailableIpAddresses.add(address);
                String ipAddress = IpHelper.longToIpString(address);
                returnIp = createNewIpAddressResource(ipAddress,
                        subnetRangeState.documentSelfLink,
                        connectedResourceLink, context);
                break;
            }
        }
        return returnIp;
    }

    /**
     * Creates new IP address resource with the specified IP address and moves the task state to completed,
     * once it is done. Then it creates IP Address resource, it first creates it with AVAILABLE state. It then
     * changes the state to ALLOCATED. This is done in two steps, due to concurrency issues. When multiple allocation
     * requests are invoked at the same time, they end up creating single IP Address resource. Only one of them
     * succeeds in their PATCH and the other one will retry the allocation operation.
     *
     * @param ipAddress               IP address to use for the new IP address resource.
     * @param subnetRangeResourceLink Subnet range resource link to use for the new IP address resource.
     * @param connectedResourceLink   Link to the resource this IP is assigned to.
     */
    private DeferredResult<IPAddressState> createNewIpAddressResource(String ipAddress,
            String subnetRangeResourceLink,
            String connectedResourceLink,
            IPAddressAllocationContext context) {
        IPAddressState ipAddressState = new IPAddressState();
        ipAddressState.ipAddressStatus = IPAddressState.IPAddressStatus.AVAILABLE;
        ipAddressState.ipAddress = ipAddress;
        ipAddressState.subnetRangeLink = subnetRangeResourceLink;
        ipAddressState.documentSelfLink = generateIPAddressDocumentSelfLink(subnetRangeResourceLink,
                ipAddress);
        logInfo("Creating IPAddressState with IP %s, subnet %s, for connected resource "
                        + "%s", ipAddress,
                subnetRangeResourceLink, connectedResourceLink);
        return sendWithDeferredResult(Operation.createPost(this, IPAddressService.FACTORY_LINK)
                .setBody(ipAddressState))
                .thenApply((out) ->
                {
                    IPAddressState availableIPAddress = out.getBody(IPAddressState.class);
                    availableIPAddress.ipAddressStatus = IPAddressState.IPAddressStatus.ALLOCATED;
                    availableIPAddress.connectedResourceLink = connectedResourceLink;
                    return availableIPAddress;
                })
                .thenCompose(
                        (availableIPAddress) -> (updateExistingIpAddressResource
                                (availableIPAddress, context)
                        ));

    }

    /**
     * Updates an existing IP address resource and moves the task state to completed, once it is done.
     *
     * @param addressState New IP address state.
     */
    private DeferredResult<IPAddressState> updateExistingIpAddressResource(
            IPAddressState addressState, IPAddressAllocationContext context) {
        DeferredResult<IPAddressState> ipAddressStateDeferredResult = new DeferredResult<>();

        sendRequest(Operation.createPatch(this, addressState.documentSelfLink)
                .setBody(addressState)
                .setCompletion((o, e) -> {
                    if (e != null) {
                        logSevere("Error updating IP %s: Error: %s ", addressState.ipAddress,
                                e.getMessage());
                        ipAddressStateDeferredResult.fail(e);
                    } else if (o.getStatusCode() == Operation.STATUS_CODE_NOT_MODIFIED) {
                        // Another concurrent request obtained the IP Address.
                        String msg = String
                                .format("IP Address %s is already allocated. Will re-attempt allocation with a different IP Address.",
                                        addressState.ipAddress);
                        long longIpAddr = IpHelper.ipStringToLong(addressState.ipAddress);
                        context.unavailableIpAddresses.add(longIpAddr);
                        ipAddressStateDeferredResult
                                .fail(new ConcurrentRequestException(msg));
                    } else {
                        long longIpAddr = IpHelper.ipStringToLong(addressState.ipAddress);
                        context.unavailableIpAddresses.add(longIpAddr);
                        ipAddressStateDeferredResult.complete(addressState);
                    }
                }));

        return ipAddressStateDeferredResult;
    }

    /**
     * Validates patch transition and updates it to the requested state
     *
     * @param patch        Patch operation
     * @param body         Body of the patch request
     * @param currentState Current state of patch request
     * @return True if transition is invalid. False otherwise.
     */
    private boolean validateTransitionAndUpdateState(Operation patch,
            IPAddressAllocationTaskState body,
            IPAddressAllocationTaskState currentState) {

        TaskState.TaskStage currentStage = currentState.taskInfo.stage;
        IPAddressAllocationTaskState.SubStage currentSubStage = currentState.taskSubStage;
        boolean isUpdate = false;

        if (body.ipAddressLinks != null) {
            currentState.ipAddressLinks = body.ipAddressLinks;
            isUpdate = true;
        }

        if (body.ipAddresses != null) {
            currentState.ipAddresses = body.ipAddresses;
            isUpdate = true;
        }

        if (body.subnetRangeLinks != null) {
            currentState.subnetRangeLinks = body.subnetRangeLinks;
            isUpdate = true;
        }

        if (body.rsrcToAllocatedIpsMap != null) {
            currentState.rsrcToAllocatedIpsMap = body.rsrcToAllocatedIpsMap;
            isUpdate = true;
        }

        if (body.taskInfo == null || body.taskInfo.stage == null) {
            if (isUpdate) {
                patch.complete();
                return true;
            }
            patch.fail(new IllegalArgumentException(
                    "taskInfo and stage are required"));
            return true;
        }

        if (currentStage.ordinal() > body.taskInfo.stage.ordinal()) {
            patch.fail(new IllegalArgumentException(
                    "stage can not move backwards:" + body.taskInfo.stage));
            return true;
        }

        if (body.taskInfo.failure != null) {
            logWarning(() -> String.format("Referrer %s is patching us to failure: %s",
                    patch.getReferer(), Utils.toJsonHtml(body.taskInfo.failure)));
            currentState.taskInfo.failure = body.taskInfo.failure;
            currentState.taskInfo.stage = body.taskInfo.stage;
            currentState.taskSubStage = IPAddressAllocationTaskState.SubStage.FAILED;
            return false;
        }

        if (TaskState.isFinished(body.taskInfo)) {
            currentState.taskInfo.stage = body.taskInfo.stage;
            currentState.taskSubStage = IPAddressAllocationTaskState.SubStage.FINISHED;
            return false;
        }

        if (currentSubStage != null && body.taskSubStage != null
                && currentSubStage.ordinal() > body.taskSubStage.ordinal()) {
            patch.fail(new IllegalArgumentException(
                    "subStage can not move backwards:" + body.taskSubStage));
            return true;
        }

        currentState.taskInfo.stage = body.taskInfo.stage;
        currentState.taskSubStage = body.taskSubStage;

        logFine(() -> String.format("Moving from %s(%s) to %s(%s)", currentSubStage, currentStage,
                body.taskSubStage, body.taskInfo.stage));

        return false;
    }

    private void failTask(Throwable e, String messageFormat, Object... args) {
        String message = String.format(messageFormat, args);
        logWarning(() -> message);

        IPAddressAllocationTaskState body = new IPAddressAllocationTaskState();
        body.taskInfo = new TaskState();
        body.taskInfo.stage = TaskState.TaskStage.FAILED;
        body.taskInfo.failure = Utils.toServiceErrorResponse(e);

        sendSelfPatch(body);
    }

    private void proceedTo(IPAddressAllocationTaskState.SubStage nextSubstage,
            Consumer<IPAddressAllocationTaskState> patchBodyConfigurator) {
        IPAddressAllocationTaskState state = new IPAddressAllocationTaskState();
        state.taskInfo = new TaskState();
        state.taskSubStage = nextSubstage;
        sendSelfPatch(state, TaskState.TaskStage.STARTED, patchBodyConfigurator);
    }

    private static String generateIPAddressDocumentSelfLink(String subnetRangeSelfLink,
            String ipAddress) {
        if (subnetRangeSelfLink == null || subnetRangeSelfLink.isEmpty()) {
            return ipAddress;
        }

        return UriUtils.getLastPathSegment(subnetRangeSelfLink) + ID_SEPARATOR + ipAddress;
    }

    private void sendCallbackResponse(IPAddressAllocationTaskState state) {
        IPAddressAllocationTaskResult result;
        if (state.taskInfo.stage == TaskState.TaskStage.FAILED) {
            result = new IPAddressAllocationTaskResult(state.serviceTaskCallback
                    .getFailedResponse(state.taskInfo.failure).taskInfo.stage,
                    state.serviceTaskCallback
                            .getFailedResponse(state.taskInfo.failure).taskSubStage,
                    state.taskInfo.failure);
        } else {
            result = new IPAddressAllocationTaskResult(
                    state.serviceTaskCallback.getFinishedResponse().taskInfo.stage,
                    state.serviceTaskCallback.getFinishedResponse().taskSubStage, null);
        }

        result.ipAddresses = state.ipAddresses;
        result.ipAddressLinks = state.ipAddressLinks;
        result.subnetRangeLinks = state.subnetRangeLinks;
        result.connectedResourceLink = state.connectedResourceLink;
        result.resourceToAllocatedIPsMap = state.rsrcToAllocatedIpsMap;
        if (state.ipAddresses != null && state.ipAddresses.size() > 0) {
            result.customProperties.put(state.connectedResourceLink, state.ipAddresses.get(0));
            result.customProperties
                    .put(state.connectedResourceLink + CUSTOM_PROPERTY_SUFFIX_IP_ADDRESS_LINK,
                            state.ipAddressLinks.get(0));
        }

        logInfo(state.toString());


        sendRequest(Operation.createPatch(state.serviceTaskCallback.serviceURI).setBody(result));
    }

    private void checkEnoughIPsAvailableOrFail(IPAddressAllocationContext context) {
        int requiredIpCounts = context.requestedIpCount;
        setMaxPossibleIpCountInSubnet(context);
        int countOfTotalPossibleIPs = context.maxPossibleIpsCount;
        int availableIpsCount =
                countOfTotalPossibleIPs - context.unavailableIpAddresses.size();

        if (requiredIpCounts > availableIpsCount) {
            String message = String.format("%d IPs are required to complete the task" +
                            " but only %d IPs were available in the subnet %s",
                    requiredIpCounts, availableIpsCount, context.subnetState.documentSelfLink);
            throw new CompletionException(new Exception(message));
        }

    }

    private int getAllocatedIpsCount(IPAddressAllocationContext context) {
        Map<String, List<IPAddressState>> allocatedMap = context
                .connectedResourceToAllocatedIpsMap;
        int allocatedIpCount = 0;

        for (String resource : allocatedMap.keySet()) {
            allocatedIpCount = allocatedIpCount + allocatedMap.get(resource).size();
        }

        return allocatedIpCount;
    }

    private void setMaxPossibleIpCountInSubnet(IPAddressAllocationContext context) {
        List<SubnetRangeState> subnetRangeStates = context.subnetRangeStates;
        int maxIps = 0;

        for (SubnetRangeState subnetRangeState : subnetRangeStates) {
            Long startIP = IpHelper.ipStringToLong(subnetRangeState.startIPAddress);
            Long endIP = IpHelper.ipStringToLong(subnetRangeState.endIPAddress);
            maxIps = maxIps + (int) (endIP - startIP) + 1;
        }
        context.maxPossibleIpsCount = maxIps;
    }


    private void addIpToContext(IPAddressAllocationContext context, String connectedResourceLink,
            IPAddressState ipAddressState) {
        context.connectedResourceToAllocatedIpsMap
                .computeIfAbsent(connectedResourceLink, k -> new ArrayList());

        List<IPAddressState> ipStatesForResource = context.connectedResourceToAllocatedIpsMap.get
                (connectedResourceLink);

        logInfo("Adding to context, resource %s mapping to IP %s",connectedResourceLink,
                ipAddressState.ipAddress);
        ipStatesForResource.add(ipAddressState);
    }

    /**
     * We try to allocate existing IP address docs to the connected resources that requested an IP.
     *
     * We perform retries on this method because multiple threads could see the same IP document
     * as available and request for it. Only one thread will be granted this IP and others will
     * fail. In that case we want to retry and look for another IP address doc that may be available.
     * We keep retrying as long as there are existing IP address docs with their state as available.
     *
     * @param context
     * @return
     */
    private DeferredResult<IPAddressAllocationContext> allocateFromExistingIPswithRetry(
            IPAddressAllocationContext context) {
        int requiredIpCount = context.requestedIpCount;

        DeferredResult<IPAddressAllocationContext> deferredResult = new DeferredResult<>();

        allocateFromExistingIPs(context).whenComplete((result, exception) -> {
            if (exception == null) {
                deferredResult.complete(context);
            } else if (exception instanceof ConcurrentRequestException
                    || exception instanceof java.util.concurrent.CompletionException) {
                //Comes in here when there is a ConcurrentRequestException exception
                //If there are IP address docs with state as available try again.
                long availableStatusCount = context.ipAddressStates.stream().filter(state -> state
                        .ipAddressStatus == IPAddressStatus.AVAILABLE).count();

                if (availableStatusCount > 0) {

                    if (getAllocatedIpsCount(context) < requiredIpCount) {

                        logInfo("Retrying to allocate from existing IPs.");

                        //Before we retry, we query to find all the existing IP address documents
                        //And then we use that list to figure out the unavailable IPs
                        context.populateContextWithExistingIpAddresses(this)
                                .thenCompose(IPAddressAllocationContext::setUnAvailableIps)
                                .thenCompose((refreshedIPsCtxt) ->
                                        allocateFromExistingIPswithRetry(refreshedIPsCtxt))
                                .whenComplete((completedContext, e) -> {
                                    if (e != null) {
                                        deferredResult.fail(e);
                                    } else {
                                        deferredResult.complete(completedContext);
                                    }
                                });
                    } else {
                        //There is an exception but you still have enough IPs
                        //Should not come in here
                        logWarning("Unexpected code path for ip allocation for %s", context
                                .subnetState.documentSelfLink);
                        deferredResult.complete(context);
                    }
                } else {
                    //No more available IPs left left.
                    logInfo("Could not allocate from existing IPs. %s", exception.getMessage());
                    deferredResult.complete(context);
                }
            } else {
                // For all other exceptions
                logInfo("Could not allocate from existing IPs. %s", exception.getMessage());
                deferredResult.complete(context);
            }
        });
        return deferredResult;

    }

    /**
     *
     * @param context
     * @return
     */
    private DeferredResult<IPAddressAllocationContext> createNewIpsWithRetry(
            IPAddressAllocationContext context) {
        int requiredIpCount = context.requestedIpCount;

        DeferredResult<IPAddressAllocationContext> deferredResult = new DeferredResult<>();

        createNewIps(context).whenComplete((result, exception) -> {
            if (exception == null) {
                deferredResult.complete(context);
            } else if (exception instanceof ConcurrentRequestException
                    || exception instanceof java.util.concurrent.CompletionException) {
                //Comes in here when there is an exception
                if (context.maxPossibleIpsCount > context.unavailableIpAddresses.size()) {
                    logInfo("Retrying to create new IPs.");

                    if (getAllocatedIpsCount(context) < requiredIpCount) {

                        context.populateContextWithExistingIpAddresses(this)
                                .thenCompose(IPAddressAllocationContext::setUnAvailableIps)
                                .thenCompose((refreshedIPsCtxt) ->
                                        createNewIpsWithRetry(refreshedIPsCtxt))
                                .whenComplete((completedContext, e) -> {
                                    if (e != null) {
                                        deferredResult.fail(e);
                                    } else {
                                        deferredResult.complete(completedContext);
                                    }
                                });
                    } else {
                        //There is an exception but you still have enough IPs
                        //Should not come in here
                        logWarning("Unexpected code path for ip allocation for %s", context
                                .subnetState.documentSelfLink);
                        deferredResult.complete(context);
                    }

                } else {
                    //No more IPs left in ranges left. Fail method call.
                    logSevere("No more IPs available for allocation. Failing task.");
                    deferredResult.fail(exception);
                }

            } else {
                // For all other exceptions, fail request
                deferredResult.fail(exception);
            }
        });
        return deferredResult;

    }

    private void addResultToState(IPAddressAllocationContext context, IPAddressAllocationTaskState
            state) {

        state.requestType = IPAddressAllocationTaskState.RequestType.ALLOCATE;

        state.subnetLink = context.subnetState.documentSelfLink;

        List<String> ctxtSubnetRangeLinks = context.subnetRangeStates.stream().map(s -> s
                .documentSelfLink).collect(toList());

        state.subnetRangeLinks.addAll(ctxtSubnetRangeLinks);

        for (String connectedResource : context.connectedResourceToAllocatedIpsMap
                .keySet()) {
            //Multiple IPs per connected resource
            List<IPAddressState> addressStates = context.connectedResourceToAllocatedIpsMap
                    .get(connectedResource);
            List<String> ipAddressSelfLinks = new ArrayList<>();

            for (IPAddressState ipAddressState : addressStates) {
                ipAddressSelfLinks
                        .add(ipAddressState.documentSelfLink);
                state.ipAddresses.add(ipAddressState.ipAddress);
                state.ipAddressLinks
                        .add(ipAddressState.documentSelfLink);
            }

            state.rsrcToAllocatedIpsMap
                    .put(connectedResource, ipAddressSelfLinks);


        }

    }

}

