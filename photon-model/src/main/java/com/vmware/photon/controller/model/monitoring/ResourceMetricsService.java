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

package com.vmware.photon.controller.model.monitoring;

import java.util.Map;

import com.esotericsoftware.kryo.serializers.VersionFieldSerializer.Since;

import com.vmware.photon.controller.model.UriPaths;
import com.vmware.photon.controller.model.constants.ReleaseConstants;
import com.vmware.photon.controller.model.resources.util.PhotonModelUtils;

import com.vmware.xenon.common.FactoryService;
import com.vmware.xenon.common.Operation;
import com.vmware.xenon.common.ServiceDocument;
import com.vmware.xenon.common.ServiceDocumentDescription.PropertyIndexingOption;
import com.vmware.xenon.common.ServiceDocumentDescription.PropertyUsageOption;
import com.vmware.xenon.common.StatefulService;
import com.vmware.xenon.common.Utils;

public class ResourceMetricsService extends StatefulService {

    public static final String FACTORY_LINK = UriPaths.MONITORING + "/resource-metrics";

    public static FactoryService createFactory() {
        return FactoryService.createIdempotent(ResourceMetricsService.class);
    }

    public ResourceMetricsService() {
        super(ResourceMetrics.class);
        super.toggleOption(ServiceOption.IMMUTABLE, true);
        super.toggleOption(ServiceOption.PERSISTENCE, true);
        super.toggleOption(ServiceOption.ON_DEMAND_LOAD, true);
        super.toggleOption(ServiceOption.REPLICATION, true);
        super.toggleOption(ServiceOption.OWNER_SELECTION, true);
    }

    public static class ResourceMetrics extends ServiceDocument {
        public static final String FIELD_NAME_ENTRIES = "entries";
        public static final String FIELD_NAME_TIMESTAMP = "timestampMicrosUtc";
        public static final String FIELD_NAME_CUSTOM_PROPERTIES = "customProperties";
        public static final String PROPERTY_RESOURCE_LINK = "resourceLink";

        @Documentation(description = "Map of datapoints. The key represents the metric name and the value"
                + "represents the the metric value")
        @UsageOption(option = PropertyUsageOption.REQUIRED)
        @PropertyOptions(indexing = PropertyIndexingOption.EXPAND)
        public Map<String, Double> entries;
        @Documentation(description = "timestamp associated with this metric entry")
        @UsageOption(option = PropertyUsageOption.REQUIRED)
        public Long timestampMicrosUtc;

        @PropertyOptions(indexing = {
                PropertyIndexingOption.CASE_INSENSITIVE,
                PropertyIndexingOption.EXPAND,
                PropertyIndexingOption.FIXED_ITEM_NAME })
        @Since(ReleaseConstants.RELEASE_VERSION_0_6_15)
        public Map<String, String> customProperties;
    }

    @Override
    public void handleStart(Operation start) {
        try {
            processInput(start);
            start.complete();
        } catch (Throwable t) {
            start.fail(t);
        }
    }

    @Override
    public void handleDelete(Operation delete) {
        if (delete.hasBody()) {
            ServiceDocument state = delete.getBody(ServiceDocument.class);
            if (state != null && state.documentExpirationTimeMicros != 0) {
                ResourceMetrics body = getState(delete);
                body.documentExpirationTimeMicros = state.documentExpirationTimeMicros;
            }
        }
        super.handleDelete(delete);
    }

    @Override
    public void handlePut(Operation put) {
        PhotonModelUtils.handleIdempotentPut(this, put);
    }

    private ResourceMetrics processInput(Operation op) {
        if (!op.hasBody()) {
            throw (new IllegalArgumentException("body is required"));
        }
        ResourceMetrics state = op.getBody(ResourceMetrics.class);
        Utils.validateState(getStateDescription(), state);
        return state;
    }
}
