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

package solutionElementTests;

import static org.junit.Assert.*;

import lsp.LSPUtils;
import lsp.usecase.UsecaseUtils;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import lsp.LogisticsSolutionElement;
import lsp.LSPResource;

public class SecondReloadElementTest {

	private LSPResource point;
	private LogisticsSolutionElement reloadElement;
	
	@Before
	public void initialize() {
		UsecaseUtils.ReloadingPointSchedulerBuilder schedulerBuilder =  UsecaseUtils.ReloadingPointSchedulerBuilder.newInstance();
        schedulerBuilder.setCapacityNeedFixed(10);
        schedulerBuilder.setCapacityNeedLinear(1);


		Id<LSPResource> reloadingId = Id.create("ReloadingPoint2", LSPResource.class);
		Id<Link> reloadingLinkId = Id.createLinkId("(14 2) (14 3)");
        
        UsecaseUtils.ReloadingPointBuilder reloadingPointBuilder = UsecaseUtils.ReloadingPointBuilder.newInstance(reloadingId, reloadingLinkId);
        reloadingPointBuilder.setReloadingScheduler(schedulerBuilder.build());
        point = reloadingPointBuilder.build();
        
        Id<LogisticsSolutionElement> elementId = Id.create("SecondReloadElement", LogisticsSolutionElement.class);
		LSPUtils.LogisticsSolutionElementBuilder reloadingElementBuilder = LSPUtils.LogisticsSolutionElementBuilder.newInstance(elementId );
		reloadingElementBuilder.setResource(point);
		reloadElement = reloadingElementBuilder.build();
	
	}
	
	@Test
	public void testDistributionElement() {
		assertNotNull(reloadElement.getIncomingShipments());
		assertNotNull(reloadElement.getIncomingShipments().getShipments());
		assertTrue(reloadElement.getIncomingShipments().getSortedShipments().isEmpty());
		assertNotNull(reloadElement.getInfos());
		assertTrue(reloadElement.getInfos().isEmpty());
		assertNull(reloadElement.getLogisticsSolution());
		assertNull(reloadElement.getNextElement());
		assertNotNull(reloadElement.getOutgoingShipments());
		assertNotNull(reloadElement.getOutgoingShipments().getShipments());
		assertTrue(reloadElement.getOutgoingShipments().getSortedShipments().isEmpty());
		assertNull(reloadElement.getPreviousElement());
		assertSame(reloadElement.getResource(), point);
		assertSame(reloadElement.getResource().getClientElements().iterator().next(), reloadElement);
	}
}
