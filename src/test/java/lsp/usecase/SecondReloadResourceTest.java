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

package lsp.usecase;

import lsp.LSPCarrierResource;
import lsp.LSPResource;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import static org.junit.Assert.*;


public class SecondReloadResourceTest {

	private TransshipmentHub transshipmentHub;
	private Id<Link> reloadingLinkId;

	@Before
	public void initialize() {
		UsecaseUtils.TranshipmentHubSchedulerBuilder schedulerBuilder = UsecaseUtils.TranshipmentHubSchedulerBuilder.newInstance();
		schedulerBuilder.setCapacityNeedFixed(10);
		schedulerBuilder.setCapacityNeedLinear(1);

		Id<LSPResource> reloadingId = Id.create("TranshipmentHub2", LSPResource.class);
		reloadingLinkId = Id.createLinkId("(14 2) (14 3)");

		UsecaseUtils.TransshipmentHubBuilder transshipmentHubBuilder = UsecaseUtils.TransshipmentHubBuilder.newInstance(reloadingId, reloadingLinkId, null);
		transshipmentHubBuilder.setTransshipmentHubScheduler(schedulerBuilder.build());
		transshipmentHub = transshipmentHubBuilder.build();

	}

	@Test
	public void TranshipmentHubTest() {
		assertEquals(10, transshipmentHub.getCapacityNeedFixed(), 0.0);
		assertEquals(1, transshipmentHub.getCapacityNeedLinear(), 0.0);
		assertFalse(LSPCarrierResource.class.isAssignableFrom(transshipmentHub.getClass()));
//		assertSame(TranshipmentHub.getClassOfResource(), TranshipmentHub.class);
		assertNotNull(transshipmentHub.getClientElements());
		assertTrue(transshipmentHub.getClientElements().isEmpty());
		assertSame(transshipmentHub.getEndLinkId(), reloadingLinkId);
		assertSame(transshipmentHub.getStartLinkId(), reloadingLinkId);
		assertNotNull(transshipmentHub.getSimulationTrackers());
		assertFalse(transshipmentHub.getSimulationTrackers().isEmpty());
		assertEquals(1, transshipmentHub.getSimulationTrackers().size());
		assertNotNull(transshipmentHub.getAttributes());
		assertTrue(transshipmentHub.getAttributes().isEmpty());
	}

}
