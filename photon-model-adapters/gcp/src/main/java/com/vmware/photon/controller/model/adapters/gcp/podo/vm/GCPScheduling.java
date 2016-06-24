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

package com.vmware.photon.controller.model.adapters.gcp.podo.vm;

/**
 * Scheduling options for this instance.
 * For more information, see the following link:
 * https://cloud.google.com/compute/docs/instances/setting-instance-scheduling-options
 */
public class GCPScheduling {
    public String onHostMaintenance;
    public Boolean automaticRestart;
    public Boolean preemptible;
}
