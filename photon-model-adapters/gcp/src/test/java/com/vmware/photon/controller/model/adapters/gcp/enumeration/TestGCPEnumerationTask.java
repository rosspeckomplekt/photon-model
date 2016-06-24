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

package com.vmware.photon.controller.model.adapters.gcp.enumeration;

import static com.vmware.photon.controller.model.adapters.gcp.GCPTestUtil.createDefaultComputeHost;
import static com.vmware.photon.controller.model.adapters.gcp.GCPTestUtil.createDefaultResourcePool;
import static com.vmware.photon.controller.model.adapters.gcp.GCPTestUtil.createDefaultVMResource;
import static com.vmware.photon.controller.model.adapters.gcp.GCPTestUtil.deleteDocument;
import static com.vmware.photon.controller.model.adapters.gcp.GCPTestUtil.deleteInstances;
import static com.vmware.photon.controller.model.adapters.gcp.GCPTestUtil.generateRandomName;
import static com.vmware.photon.controller.model.adapters.gcp.GCPTestUtil.provisionInstances;
import static com.vmware.photon.controller.model.adapters.gcp.GCPTestUtil.stopInstances;
import static com.vmware.photon.controller.model.adapters.gcp.GCPTestUtil.syncQueryComputeStatesWithPowerState;
import static com.vmware.photon.controller.model.adapters.gcp.utils.GCPUtils.privateKeyFromPkcs8;

import java.util.Collections;
import java.util.logging.Level;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.ComputeScopes;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.vmware.photon.controller.model.PhotonModelServices;
import com.vmware.photon.controller.model.adapterapi.EnumerationAction;
import com.vmware.photon.controller.model.adapters.gcp.GCPAdapters;
import com.vmware.photon.controller.model.resources.ComputeService;
import com.vmware.photon.controller.model.resources.ComputeService.PowerState;
import com.vmware.photon.controller.model.resources.ResourcePoolService.ResourcePoolState;
import com.vmware.photon.controller.model.tasks.PhotonModelTaskServices;
import com.vmware.photon.controller.model.tasks.ProvisioningUtils;
import com.vmware.photon.controller.model.tasks.ResourceEnumerationTaskService;
import com.vmware.photon.controller.model.tasks.ResourceEnumerationTaskService.ResourceEnumerationTaskState;
import com.vmware.photon.controller.model.tasks.TestUtils;
import com.vmware.xenon.common.BasicReusableHostTestCase;
import com.vmware.xenon.common.CommandLineArgumentParser;
import com.vmware.xenon.common.UriUtils;

/**
 * Test to enumerate instances on GCP and tear it down. The test creates instances on GCP using
 * the GCE Java SDK. It then invokes the GCP enumeration adapter to enumerate all the resources
 * on the GCP endpoint and validates that all the updates to the local state are as expected.
 * The test will by default run in mock mode.
 *
 * To actually run the test, please change the field isMock to false and specify the credentials
 * in system properties in command line. Here is an example:
 * mvn test -pl photon-model-adapters/gcp -Dtest=TestGCPEnumerationTask -Dxenon.userEmail="xxx"
 * -Dxenon.privateKey="yyy" -Dxenon.projectID="zzz" -Dxenon.zoneID="ooo"
 */
public class TestGCPEnumerationTask extends BasicReusableHostTestCase {
    private static final String APPLICATION_NAME = "enumeration-test";
    private static final String GCP_VM_NAME_PREFIX = "enumtest-";
    private static final int TIME_OUT_SECONDS = 1200;
    private static final int NUMBER_OF_COMPUTE_HOST = 1;
    private static final int INITIAL_NUMBER_OF_VMS = 1;
    private static final int PROVISION_NUMBER_OF_VMS = 2;
    private static final int RESULT_NUMBER_OF_VMS = INITIAL_NUMBER_OF_VMS + PROVISION_NUMBER_OF_VMS;
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    public boolean isMock = true;
    public String userEmail = "userEmail";
    public String privateKey = "privateKey";
    public String projectID = "projectID";
    public String zoneID = "zoneID";
    public String gcpVMName;

    // Fields that are used across method calls, stash them as private fields
    private ResourcePoolState outPool;
    private ComputeService.ComputeState computeHost;
    private ComputeService.ComputeState vmState;
    private Compute compute;

    /**
     * Do some preparation before running enumeration test. It will generate
     * a random default VM name, provision a host service, set the timeout of
     * the host service, create a default resource pool, compute host and vm.
     * Then it will wait until every service is ready to start.
     * @throws Throwable Exception during preparation.
     */
    @Before
    public void setUp() throws Throwable {
        CommandLineArgumentParser.parseFromProperties(this.host);
        this.gcpVMName = this.gcpVMName == null ? generateRandomName(GCP_VM_NAME_PREFIX) : this.gcpVMName;
        PhotonModelServices.startServices(this.host);
        PhotonModelTaskServices.startServices(this.host);
        GCPAdapters.startServices(this.host);
        this.host.setTimeoutSeconds(TIME_OUT_SECONDS);
        this.host.waitForServiceAvailable(PhotonModelServices.LINKS);
        this.host.waitForServiceAvailable(PhotonModelTaskServices.LINKS);
        this.host.waitForServiceAvailable(GCPAdapters.LINKS);

        if (!this.isMock) {
            // If you copy and paste private key to the system properties, the new line
            // character will remain plain text.
            this.privateKey = this.privateKey.replaceAll("\\\\n", "\n");
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            GoogleCredential credential = new GoogleCredential.Builder()
                    .setTransport(httpTransport)
                    .setJsonFactory(JSON_FACTORY)
                    .setServiceAccountId(this.userEmail)
                    .setServiceAccountScopes(Collections.singletonList(ComputeScopes.CLOUD_PLATFORM))
                    .setServiceAccountPrivateKey(privateKeyFromPkcs8(this.privateKey))
                    .build();
            this.compute = new Compute.Builder(httpTransport, JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        }

        // Create the compute host, resource pool and the VM state to be used in the test.
        createResourcePoolComputeHostAndVMState();
    }

    /**
     * Do some clean up after enumeration test. It will delete the default VM
     * as well as compute host. It will also delete all VMs of the specified
     * project and zone.
     * @throws Throwable Exception during clean up.
     */
    @After
    public void tearDown() throws Throwable {
        // try to delete the VM
        if (this.vmState != null) {
            try {
                deleteDocument(this.host, this.vmState.documentSelfLink);
            } catch (Throwable deleteEx) {
                // just log and move on
                host.log(Level.WARNING, "Exception deleting VM - %s", deleteEx.getMessage());
            }
        }
        if (this.computeHost != null) {
            try {
                deleteDocument(this.host, this.computeHost.documentSelfLink);
            } catch (Throwable deleteEx) {
                host.log(Level.WARNING, "Exception deleting VM - %s", deleteEx.getMessage());
            }
        }
        if (!this.isMock) {
            deleteInstances(this.compute, this.projectID, this.zoneID, this.host);
        }
    }

    /**
     * The main flow of the test case. It will run the enumeration twice. One is before
     * provisioning any vms on GCP. The other is after provisioning some vms.
     * @throws Throwable Exception during running test case.
     */
    @Test
    public void testEnumeration() throws Throwable {
        // There are one compute host and one stale compute state.
        ProvisioningUtils.queryComputeInstances(host, NUMBER_OF_COMPUTE_HOST + INITIAL_NUMBER_OF_VMS);
        runEnumeration();

        if (this.isMock) {
            return;
        }

        // There should be only one compute host left.
        // Test deletes.
        ProvisioningUtils.queryComputeInstances(host, NUMBER_OF_COMPUTE_HOST);
        // Provision several vms on the cloud and run enumeration again.
        host.log(Level.INFO, "Provisioning instances...");
        provisionInstances(this.compute, this.userEmail, this.projectID, this.zoneID,
                PROVISION_NUMBER_OF_VMS, this.host);
        runEnumeration();
        // There should be one compute host with several synchronized vms.
        // Test creates.
        ProvisioningUtils.queryComputeInstances(host, RESULT_NUMBER_OF_VMS);
        // Make sure that all power states are on.
        syncQueryComputeStatesWithPowerState(this.host, this.outPool, this.computeHost,
                PowerState.ON, PROVISION_NUMBER_OF_VMS);
        // Stop all vms on the cloud and run enumeration again.
        host.log(Level.INFO, "Stopping instances...");
        stopInstances(this.compute, this.projectID, this.zoneID, this.host);
        runEnumeration();
        // Check the number of local vms and their power states, which should be OFF.
        // Test updates.
        syncQueryComputeStatesWithPowerState(this.host, this.outPool, this.computeHost,
                PowerState.OFF, PROVISION_NUMBER_OF_VMS);
        // Delete all vms on the cloud and run enumeration again.
        host.log(Level.INFO, "Deleting instances...");
        deleteInstances(this.compute, this.projectID, this.zoneID, this.host);
        runEnumeration();
        // There should be only one compute host left.
        // Test deletes.
        ProvisioningUtils.queryComputeInstances(host, NUMBER_OF_COMPUTE_HOST);
    }

    /**
     * Run the enumeration and wait until it ends.
     * @throws Throwable Exception during running and waiting enumeration.
     */
    private void runEnumeration() throws Throwable {
        ResourceEnumerationTaskState enumerationTaskState = new ResourceEnumerationTaskState();

        enumerationTaskState.resourcePoolLink = this.outPool.documentSelfLink;
        enumerationTaskState.computeDescriptionLink = this.computeHost.descriptionLink;
        enumerationTaskState.parentComputeLink = this.computeHost.documentSelfLink;
        enumerationTaskState.enumerationAction = EnumerationAction.START;
        enumerationTaskState.adapterManagementReference = UriUtils
                .buildUri(GCPEnumerationAdapterService.SELF_LINK);
        enumerationTaskState.isMockRequest = this.isMock;

        ResourceEnumerationTaskState enumTask = TestUtils.doPost(host, enumerationTaskState,
                ResourceEnumerationTaskState.class, UriUtils.buildUri(host,
                        ResourceEnumerationTaskService.FACTORY_LINK));

        this.host.waitFor("Error waiting for enumeration task", () -> {
            try {
                if (this.host.waitForFinishedTask(ResourceEnumerationTaskState.class, enumTask
                        .documentSelfLink) != null) {
                    return true;
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
            return false;
        });
    }

    /**
     * Creates the state associated with the resource pool, compute host and the VM to be created.
     * @throws Throwable Exception during creating default resource pool, compute host or VM.
     */
    private void createResourcePoolComputeHostAndVMState() throws Throwable {
        // Create a resource pool where the VM will be housed.
        this.outPool = createDefaultResourcePool(this.host, this.projectID);

        // Create a compute host for the GCP GCE VM.
        this.computeHost = createDefaultComputeHost(this.host, this.userEmail, this.privateKey, this.zoneID,
                this.outPool.documentSelfLink);

        // Create a GCP VM compute resource.
        // This vm is stale and should be deleted after the first enumeration.
        this.vmState = createDefaultVMResource(this.host, this.userEmail, this.privateKey, this.zoneID,
                this.gcpVMName, this.computeHost.documentSelfLink, this.outPool.documentSelfLink);
    }
}
