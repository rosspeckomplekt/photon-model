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

package com.vmware.photon.controller.model.resources.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.vmware.photon.controller.model.helpers.BaseModelTest;
import com.vmware.photon.controller.model.resources.ComputeDescriptionService.ComputeDescription;
import com.vmware.photon.controller.model.resources.ComputeDescriptionServiceTest;
import com.vmware.photon.controller.model.resources.ComputeService;
import com.vmware.photon.controller.model.resources.ComputeService.ComputeState;
import com.vmware.photon.controller.model.resources.ComputeServiceTest;
import com.vmware.photon.controller.model.resources.ResourcePoolService;
import com.vmware.photon.controller.model.resources.ResourcePoolService.ResourcePoolState;
import com.vmware.photon.controller.model.resources.ResourcePoolServiceTest;
import com.vmware.xenon.common.ServiceDocument;
import com.vmware.xenon.common.test.TestContext;

/**
 * Tests for the {@link ResourcePoolQueryHelper} class.
 */
public class ResourcePoolQueryHelperTest extends BaseModelTest {
    // references to the test model:
    //     rp1   rp2   rp3    (none)
    //     /\     |     |       |
    //    /  \    |     |       |
    //   c1  c2   c3  (none)    c4
    private ResourcePoolState rp1;
    private ResourcePoolState rp2;
    private ResourcePoolState rp3;
    private ComputeDescription cd1;
    private ComputeState c1;
    private ComputeState c2;
    private ComputeState c3;
    private ComputeState c4;

    @Before
    /**
     * Creates the test model.
     */
    public void createResources() throws Throwable {
        this.rp1 = createRp();
        this.rp2 = createRp();
        this.rp3 = createRp();

        this.cd1 = ComputeDescriptionServiceTest.createComputeDescription(this);

        this.c1 = createCompute(this.cd1, this.rp1);
        this.c2 = createCompute(this.cd1, this.rp1);
        this.c3 = createCompute(this.cd1, this.rp2);
        this.c4 = createCompute(this.cd1, null);
    }

    @After
    /**
     * Deletes the created test model resources.
     */
    public void deleteResources() throws Throwable {
        for (ServiceDocument resource : Arrays.asList(this.rp1, this.rp2, this.rp3, this.cd1,
                this.c1, this.c2, this.c3, this.c4)) {
            deleteServiceSynchronously(resource.documentSelfLink);
        }
    }

    @Test
    public void testAllResourcePools() throws Throwable {
        ResourcePoolQueryHelper helper = ResourcePoolQueryHelper.create(getHost());
        ResourcePoolQueryHelper.QueryResult qr = runHelperSynchronously(helper);

        assertThat(qr.resourcesPools.size(), is(3));
        assertThat(qr.resourcesPools.get(this.rp1.documentSelfLink).computeStates, hasSize(2));
        assertThat(qr.resourcesPools.get(this.rp2.documentSelfLink).computeStates, hasSize(1));
        assertThat(qr.resourcesPools.get(this.rp3.documentSelfLink).computeStates, is(empty()));

        assertThat(qr.computesByLink.size(), is(4));

        assertThat(qr.rpLinksByComputeLink.size(), is(4));
        assertThat(qr.rpLinksByComputeLink.get(this.c1.documentSelfLink),
                contains(this.rp1.documentSelfLink));
        assertThat(qr.rpLinksByComputeLink.get(this.c2.documentSelfLink),
                contains(this.rp1.documentSelfLink));
        assertThat(qr.rpLinksByComputeLink.get(this.c3.documentSelfLink),
                contains(this.rp2.documentSelfLink));
        assertThat(qr.rpLinksByComputeLink.get(this.c4.documentSelfLink), is(empty()));
    }

    @Test
    public void testForResourcePool() throws Throwable {
        ResourcePoolQueryHelper helper = ResourcePoolQueryHelper.createForResourcePool(getHost(),
                this.rp2.documentSelfLink);
        ResourcePoolQueryHelper.QueryResult qr = runHelperSynchronously(helper);

        assertThat(qr.resourcesPools.size(), is(1));
        assertThat(qr.resourcesPools.get(this.rp2.documentSelfLink).computeStates, hasSize(1));

        assertThat(qr.computesByLink.size(), is(1));

        assertThat(qr.rpLinksByComputeLink.size(), is(1));
        assertThat(qr.rpLinksByComputeLink.get(this.c3.documentSelfLink),
                contains(this.rp2.documentSelfLink));
    }

    @Test
    public void testForComputes() throws Throwable {
        ResourcePoolQueryHelper helper = ResourcePoolQueryHelper.createForComputes(getHost(),
                Arrays.asList(this.c1.documentSelfLink, this.c4.documentSelfLink));
        ResourcePoolQueryHelper.QueryResult qr = runHelperSynchronously(helper);

        assertThat(qr.resourcesPools.size(), is(1));
        assertThat(qr.resourcesPools.get(this.rp1.documentSelfLink).computeStates, hasSize(1));

        assertThat(qr.computesByLink.size(), is(2));

        assertThat(qr.rpLinksByComputeLink.size(), is(2));
        assertThat(qr.rpLinksByComputeLink.get(this.c1.documentSelfLink),
                contains(this.rp1.documentSelfLink));
        assertThat(qr.rpLinksByComputeLink.get(this.c4.documentSelfLink), is(empty()));
    }

    @Test
    public void testWithAdditionalQuery() throws Throwable {
        ResourcePoolQueryHelper helper = ResourcePoolQueryHelper.create(getHost());
        helper.setAdditionalQueryClausesProvider(qb -> {
            qb.addInClause(ServiceDocument.FIELD_NAME_SELF_LINK,
                    Arrays.asList(this.c1.documentSelfLink, this.c4.documentSelfLink));
        });
        ResourcePoolQueryHelper.QueryResult qr = runHelperSynchronously(helper);

        assertThat(qr.resourcesPools.size(), is(3));
        assertThat(qr.resourcesPools.get(this.rp1.documentSelfLink).computeStates, hasSize(1));
        assertThat(qr.resourcesPools.get(this.rp2.documentSelfLink).computeStates, is(empty()));
        assertThat(qr.resourcesPools.get(this.rp3.documentSelfLink).computeStates, is(empty()));

        assertThat(qr.computesByLink.size(), is(2));

        assertThat(qr.rpLinksByComputeLink.size(), is(2));
        assertThat(qr.rpLinksByComputeLink.get(this.c1.documentSelfLink),
                contains(this.rp1.documentSelfLink));
        assertThat(qr.rpLinksByComputeLink.get(this.c4.documentSelfLink), is(empty()));
    }

    /**
     * Creates a RP resource.
     */
    private ResourcePoolState createRp() throws Throwable {
        ResourcePoolState rp = ResourcePoolServiceTest.buildValidStartState();
        return postServiceSynchronously(ResourcePoolService.FACTORY_LINK, rp,
                ResourcePoolState.class);
    }

    /**
     * Creates a compute resource.
     */
    private ComputeState createCompute(ComputeDescription cd, ResourcePoolState rp)
            throws Throwable {
        ComputeState compute = ComputeServiceTest.buildValidStartState(cd);
        compute.resourcePoolLink = rp != null ? rp.documentSelfLink : null;
        return postServiceSynchronously(ComputeService.FACTORY_LINK, compute, ComputeState.class);
    }

    /**
     * Synchronously executes the given ResourcePoolQueryHelper instance.
     */
    private ResourcePoolQueryHelper.QueryResult runHelperSynchronously(
            ResourcePoolQueryHelper helper) {
        ResourcePoolQueryHelper.QueryResult[] resultHolder = { null };
        TestContext ctx = this.host.testCreate(1);
        helper.query(qr -> {
            if (qr.error != null) {
                ctx.failIteration(qr.error);
                return;
            }
            resultHolder[0] = qr;
            ctx.completeIteration();
        });
        this.host.testWait(ctx);

        return resultHolder[0];
    }
}