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

import static org.junit.Assert.*;

import lsp.LSPUtils;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import lsp.LogisticsSolutionElement;
import lsp.LSPResource;

public class FirstReloadElementTest {

	private ReloadingPoint point;
	private LogisticsSolutionElement reloadingElement;

	@Before
	public void initialize() {
			UsecaseUtils.ReloadingPointSchedulerBuilder schedulerBuilder =  UsecaseUtils.ReloadingPointSchedulerBuilder.newInstance();
	        schedulerBuilder.setCapacityNeedFixed(10);
	        schedulerBuilder.setCapacityNeedLinear(1);


		Id<LSPResource> reloadingId = Id.create("ReloadingPoint1", LSPResource.class);
		Id<Link> reloadingLinkId = Id.createLinkId("(4 2) (4 3)");
	        
	        UsecaseUtils.ReloadingPointBuilder reloadingPointBuilder = UsecaseUtils.ReloadingPointBuilder.newInstance(reloadingId, reloadingLinkId);
	        reloadingPointBuilder.setReloadingScheduler(schedulerBuilder.build());
	        point = reloadingPointBuilder.build();

		Id<LogisticsSolutionElement> elementId = Id.create("FiretReloadElement", LogisticsSolutionElement.class);
			LSPUtils.LogisticsSolutionElementBuilder reloadingElementBuilder = LSPUtils.LogisticsSolutionElementBuilder.newInstance(elementId);
			reloadingElementBuilder.setResource(point);
			reloadingElement  = reloadingElementBuilder.build();
	}

	@Test
	public void testDistributionElement() {
		assertNotNull(reloadingElement.getIncomingShipments());
		assertNotNull(reloadingElement.getIncomingShipments().getShipments());
		assertTrue(reloadingElement.getIncomingShipments().getSortedShipments().isEmpty());
		assertNotNull(reloadingElement.getInfos());
		assertTrue(reloadingElement.getInfos().isEmpty());
		assertNull(reloadingElement.getLogisticsSolution());
		assertNull(reloadingElement.getNextElement());
		assertNotNull(reloadingElement.getOutgoingShipments());
		assertNotNull(reloadingElement.getOutgoingShipments().getShipments());
		assertTrue(reloadingElement.getOutgoingShipments().getSortedShipments().isEmpty());
		assertNull(reloadingElement.getPreviousElement());
		assertSame(reloadingElement.getResource(), point);
		assertSame(reloadingElement.getResource().getClientElements().iterator().next(), reloadingElement);
	}

}
