/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2022 by the members listed in the COPYING,        *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.freight.logistics.resourceImplementations;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.freight.logistics.LSPCarrierResource;
import org.matsim.freight.logistics.LSPResource;
import org.matsim.freight.logistics.resourceImplementations.ResourceImplementationUtils.TranshipmentHubSchedulerBuilder;
import org.matsim.freight.logistics.resourceImplementations.ResourceImplementationUtils.TransshipmentHubBuilder;

public class SecondReloadResourceTest {

	private TransshipmentHubResource transshipmentHubResource;
	private Id<Link> reloadingLinkId;

	@BeforeEach
	public void initialize() {
		TranshipmentHubSchedulerBuilder schedulerBuilder = ResourceImplementationUtils.TranshipmentHubSchedulerBuilder.newInstance();
		schedulerBuilder.setCapacityNeedFixed(10);
		schedulerBuilder.setCapacityNeedLinear(1);

		Id<LSPResource> reloadingId = Id.create("TranshipmentHub2", LSPResource.class);
		reloadingLinkId = Id.createLinkId("(14 2) (14 3)");

		TransshipmentHubBuilder transshipmentHubBuilder = ResourceImplementationUtils.TransshipmentHubBuilder.newInstance(reloadingId, reloadingLinkId, null);
		transshipmentHubBuilder.setTransshipmentHubScheduler(schedulerBuilder.build());
		transshipmentHubResource = transshipmentHubBuilder.build();

	}

	@Test
	public void TranshipmentHubTest() {
		assertEquals(10, transshipmentHubResource.getCapacityNeedFixed(), 0.0);
		assertEquals(1, transshipmentHubResource.getCapacityNeedLinear(), 0.0);
		assertFalse(LSPCarrierResource.class.isAssignableFrom(transshipmentHubResource.getClass()));
//		assertSame(TranshipmentHub.getClassOfResource(), TranshipmentHub.class);
		assertNotNull(transshipmentHubResource.getClientElements());
		assertTrue(transshipmentHubResource.getClientElements().isEmpty());
		assertSame(transshipmentHubResource.getEndLinkId(), reloadingLinkId);
		assertSame(transshipmentHubResource.getStartLinkId(), reloadingLinkId);
		assertNotNull(transshipmentHubResource.getSimulationTrackers());
		assertFalse(transshipmentHubResource.getSimulationTrackers().isEmpty());
		assertEquals(1, transshipmentHubResource.getSimulationTrackers().size());
		assertNotNull(transshipmentHubResource.getAttributes());
		assertTrue(transshipmentHubResource.getAttributes().isEmpty());
	}

}
