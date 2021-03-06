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

package com.vmware.photon.controller.model.adapters.awsadapter;

import static org.junit.Assert.assertTrue;

import static com.vmware.photon.controller.model.adapters.awsadapter.TestAWSSetupUtils.AWS_DEFAULT_SUBNET_CIDR;
import static com.vmware.photon.controller.model.adapters.awsadapter.TestAWSSetupUtils.regionId;

import java.util.List;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.model.InternetGateway;
import com.amazonaws.services.ec2.model.InternetGatewayAttachment;
import com.amazonaws.services.ec2.model.RouteTable;
import com.amazonaws.services.ec2.model.Vpc;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.vmware.photon.controller.model.PhotonModelMetricServices;
import com.vmware.photon.controller.model.PhotonModelServices;
import com.vmware.photon.controller.model.adapters.awsadapter.util.AWSNetworkClient;
import com.vmware.photon.controller.model.tasks.PhotonModelTaskServices;
import com.vmware.photon.controller.model.tasks.TaskUtils;
import com.vmware.xenon.common.CommandLineArgumentParser;
import com.vmware.xenon.common.Operation;
import com.vmware.xenon.common.UriUtils;
import com.vmware.xenon.common.test.VerificationHost;

public class TestAWSNetworkService {
    /*
    * This test requires the following four command line variables.
    * If they are not present the tests will be ignored.
    * Pass them into the test with the -Dxenon.variable=value syntax
    * i.e -Dxenon.subnet="10.1.0.0/16"
    *
    * privateKey & privateKeyId are credentials to an AWS VPC account
    * region is the ec2 region where the tests should be run (us-east-1)
    * subnet is the RFC-1918 subnet of the default VPC
    */
    public String secretKey;
    public String accessKey;

    VerificationHost host;

    AWSNetworkClient netClient;

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        CommandLineArgumentParser.parseFromProperties(this);

        // ignore if any of the required properties are missing
        org.junit.Assume.assumeTrue(TestUtils.isNull(this.secretKey, this.accessKey));

        this.host = VerificationHost.create(0);
        try {
            this.host.start();
            PhotonModelServices.startServices(this.host);
            PhotonModelMetricServices.startServices(this.host);
            PhotonModelTaskServices.startServices(this.host);

            AWSNetworkService netSvc = new AWSNetworkService();
            this.host.startService(
                    Operation.createPost(UriUtils.buildUri(this.host,
                            AWSNetworkService.class)),
                    netSvc);

            this.netClient = new AWSNetworkClient(
                    TestUtils.getClient(this.accessKey, this.secretKey, regionId, false));
        } catch (Throwable e) {
            throw new Exception(e);
        }
    }

    @After
    public void tearDown() throws InterruptedException {
        if (this.host == null) {
            return;
        }
        this.host.tearDownInProcessPeers();
        this.host.toggleNegativeTestMode(false);
        this.host.tearDown();
    }

    @Test
    public void testGetDefaultVPCSubnet() throws Throwable {
        String sub = this.netClient.getDefaultVPC().getCidrBlock();
        // should always return an RFC1918 address
        TaskUtils.isRFC1918(sub);
    }

    /*
     * Test will first create a VPC then create a subnet associated to that VPC
     * Once complete it will delete the VPC and associated subnet
     */
    @Test
    public void testVPCAndSubnet() throws Throwable {
        String vpcID = this.netClient.createVPC(AWS_DEFAULT_SUBNET_CIDR);
        assertTrue(vpcID != null);

        String subnetID = this.netClient.createSubnet(AWS_DEFAULT_SUBNET_CIDR, vpcID).getSubnetId();
        assertTrue(subnetID != null);

        // ensure getters works..
        assertTrue(this.netClient.getVPC(vpcID).getVpcId()
                .equalsIgnoreCase(vpcID));
        assertTrue(
                this.netClient.getSubnet(subnetID).getSubnetId()
                        .equalsIgnoreCase(subnetID));

        // delete subnet / vpc
        this.netClient.deleteSubnet(subnetID);
        this.netClient.deleteVPC(vpcID);

        // verify vpc deletion
        // Since only one exception can be thrown in a test we will only verify the removal
        // of the VPC.  VPC removal requires all related objects be removed, so if the VPC
        // is gone then it's safe to say the subnet is as well
        this.expectedEx.expect(AmazonServiceException.class);
        this.expectedEx.expectMessage("InvalidVpcID.NotFound");
        this.netClient.getVPC(vpcID);
    }

    @Test
    public void testCreateInternetGateway() throws Throwable {
        String gatewayID = this.netClient.createInternetGateway();
        assertTrue(gatewayID != null);
        assertTrue(this.netClient.getInternetGateway(gatewayID)
                .getInternetGatewayId()
                .equalsIgnoreCase(gatewayID));
        this.netClient.deleteInternetGateway(gatewayID);
    }

    @Test
    public void testGetMainRouteTable() throws Throwable {
        Vpc defVPC = this.netClient.getDefaultVPC();
        assertTrue(defVPC != null);
        RouteTable routeTable = this.netClient.getMainRouteTable(defVPC.getVpcId());
        assertTrue(routeTable != null);
    }

    /*
     * Test covers the necessary elements for a successful environment creation
     * These environmental elements are necessary before any VM instances can be
     * created
     *
     * - Internet Gateway
     * - VPC
     * - Subnet
     * - Route to IG
     *
     */
    @Test
    public void testEnvironmentCreation() throws Throwable {
        boolean attached = false;

        String gatewayID = this.netClient.createInternetGateway();
        assertTrue(gatewayID != null);
        String vpcID = this.netClient.createVPC(AWS_DEFAULT_SUBNET_CIDR);
        assertTrue(vpcID != null);
        String subnetID = this.netClient.createSubnet(AWS_DEFAULT_SUBNET_CIDR, vpcID).getSubnetId();

        this.netClient.attachInternetGateway(vpcID, gatewayID);
        InternetGateway gw = this.netClient.getInternetGateway(gatewayID);
        List<InternetGatewayAttachment> attachments = gw.getAttachments();
        // ensure we are attached to newly created vpc
        for (InternetGatewayAttachment attachment : attachments) {
            if (attachment.getVpcId().equalsIgnoreCase(vpcID)) {
                attached = true;
                break;
            }
        }
        assertTrue(attached);
        RouteTable routeTable = this.netClient.getMainRouteTable(vpcID);
        this.netClient.createInternetRoute(gatewayID, routeTable.getRouteTableId(), "0.0.0.0/0");

        //remove resources
        this.netClient.detachInternetGateway(vpcID, gatewayID);
        this.netClient.deleteInternetGateway(gatewayID);
        this.netClient.deleteSubnet(subnetID);
        this.netClient.deleteVPC(vpcID);
    }

    @Test
    public void TestGetInvalidVPC() throws Throwable {
        this.expectedEx.expect(AmazonServiceException.class);
        this.expectedEx.expectMessage("InvalidVpcID.NotFound");
        this.netClient.getVPC("1234");
    }

    @Test
    public void testGetInvalidSubnet() throws Throwable {
        this.expectedEx.expect(AmazonServiceException.class);
        this.expectedEx.expectMessage("InvalidSubnetID.NotFound");
        this.netClient.getSubnet("1234");
    }
}
