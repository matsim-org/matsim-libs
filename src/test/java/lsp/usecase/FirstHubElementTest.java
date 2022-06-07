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

import lsp.LogisticsSolutionElement;
import lsp.LSPResource;

public class FirstHubElementTest {

	private TransshipmentHub point;
	private LogisticsSolutionElement reloadingElement;

	@Before
	public void initialize() {
		UsecaseUtils.TranshipmentHubSchedulerBuilder schedulerBuilder =  UsecaseUtils.TranshipmentHubSchedulerBuilder.newInstance();
		schedulerBuilder.setCapacityNeedFixed(10);
		schedulerBuilder.setCapacityNeedLinear(1);


		point = UsecaseUtils.TransshipmentHubBuilder
				.newInstance(Id.create("TranshipmentHub1", LSPResource.class), Id.createLinkId("(4 2) (4 3)"))
				.setTransshipmentHubScheduler(schedulerBuilder.build())
				.build();

		reloadingElement = LSPUtils.LogisticsSolutionElementBuilder
				.newInstance(Id.create("FirstHubElement", LogisticsSolutionElement.class))
				.setResource(point)
				.build();
	}

	@Test
	public void testDistributionElement() {
		assertNotNull(reloadingElement.getIncomingShipments());
		assertNotNull(reloadingElement.getIncomingShipments().getShipments());
		assertTrue(reloadingElement.getIncomingShipments().getSortedShipments().isEmpty());
		assertNotNull(reloadingElement.getAttributes() );
		assertTrue(reloadingElement.getAttributes().isEmpty() );
		assertNull(reloadingElement.getEmbeddingContainer() );
		assertNull(reloadingElement.getNextElement());
		assertNotNull(reloadingElement.getOutgoingShipments());
		assertNotNull(reloadingElement.getOutgoingShipments().getShipments());
		assertTrue(reloadingElement.getOutgoingShipments().getSortedShipments().isEmpty());
		assertNull(reloadingElement.getPreviousElement());
		assertSame(reloadingElement.getResource(), point);
		assertSame(reloadingElement.getResource().getClientElements().iterator().next(), reloadingElement);
	}

}
