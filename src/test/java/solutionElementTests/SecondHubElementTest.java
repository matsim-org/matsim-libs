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

import lsp.LogisticsSolutionElement;
import lsp.LSPResource;

public class SecondHubElementTest {

	private LSPResource point;
	private LogisticsSolutionElement hubElement;

	@Before
	public void initialize() {
		UsecaseUtils.TranshipmentHubSchedulerBuilder schedulerBuilder = UsecaseUtils.TranshipmentHubSchedulerBuilder.newInstance();
		schedulerBuilder.setCapacityNeedFixed(10);
		schedulerBuilder.setCapacityNeedLinear(1);


		point = UsecaseUtils.TransshipmentHubBuilder
				.newInstance(Id.create("TranshipmentHub2", LSPResource.class), Id.createLinkId("(14 2) (14 3)"), null)
				.setTransshipmentHubScheduler(schedulerBuilder.build())
				.build();

		hubElement = LSPUtils.LogisticsSolutionElementBuilder
				.newInstance(Id.create("SecondHubElement", LogisticsSolutionElement.class))
				.setResource(point)
				.build();
	}

	@Test
	public void testDistributionElement() {
		assertNotNull(hubElement.getIncomingShipments());
		assertNotNull(hubElement.getIncomingShipments().getShipments());
		assertTrue(hubElement.getIncomingShipments().getSortedShipments().isEmpty());
		assertNotNull(hubElement.getAttributes());
		assertTrue(hubElement.getAttributes().isEmpty());
//		assertNull(hubElement.getEmbeddingContainer() );
		assertNull(hubElement.getNextElement());
		assertNotNull(hubElement.getOutgoingShipments());
		assertNotNull(hubElement.getOutgoingShipments().getShipments());
		assertTrue(hubElement.getOutgoingShipments().getSortedShipments().isEmpty());
		assertNull(hubElement.getPreviousElement());
		assertSame(hubElement.getResource(), point);
		assertSame(hubElement.getResource().getClientElements().iterator().next(), hubElement);
	}
}
