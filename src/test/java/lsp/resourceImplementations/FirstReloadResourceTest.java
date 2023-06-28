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

package lsp.resourceImplementations;

import lsp.LSPCarrierResource;
import lsp.LSPResource;
import lsp.resourceImplementations.transshipmentHub.TranshipmentHubUtils;
import lsp.resourceImplementations.transshipmentHub.TransshipmentHubResource;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import static org.junit.Assert.*;


public class FirstReloadResourceTest {

	private static final Id<Link> hubLinkId = Id.createLinkId("(4 2) (4 3)");
	private TransshipmentHubResource transshipmentHubResource;

	@Before
	public void initialize() {


		TranshipmentHubUtils.TranshipmentHubSchedulerBuilder schedulerBuilder = TranshipmentHubUtils.TranshipmentHubSchedulerBuilder.newInstance();
		schedulerBuilder.setCapacityNeedFixed(10);
		schedulerBuilder.setCapacityNeedLinear(1);

		transshipmentHubResource = TranshipmentHubUtils.TransshipmentHubBuilder.newInstance(Id.create("TranshipmentHub1", LSPResource.class), hubLinkId, null)
				.setTransshipmentHubScheduler(schedulerBuilder.build())
				.build();
	}

	@Test
	public void TranshipmentHubTest() {
		assertEquals(10, transshipmentHubResource.getCapacityNeedFixed(), 0.0);
		assertEquals(1, transshipmentHubResource.getCapacityNeedLinear(), 0.0);
		assertFalse(LSPCarrierResource.class.isAssignableFrom(transshipmentHubResource.getClass()));
//		assertSame(TranshipmentHub.getClassOfResource(), TranshipmentHub.class);
		assertNotNull(transshipmentHubResource.getClientElements());
		assertTrue(transshipmentHubResource.getClientElements().isEmpty());
		assertSame(transshipmentHubResource.getEndLinkId(), hubLinkId);
		assertSame(transshipmentHubResource.getStartLinkId(), hubLinkId);
		assertNotNull(transshipmentHubResource.getSimulationTrackers());
		assertFalse(transshipmentHubResource.getSimulationTrackers().isEmpty());
		assertEquals(1, transshipmentHubResource.getSimulationTrackers().size());
		assertNotNull(transshipmentHubResource.getAttributes());
		assertTrue(transshipmentHubResource.getAttributes().isEmpty());
	}
}
