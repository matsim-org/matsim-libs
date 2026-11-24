/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2026 by the members listed in the COPYING,        *
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

package org.matsim.freight.logistics;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.freight.carriers.TimeWindow;
import org.matsim.freight.logistics.resourceImplementations.ResourceImplementationUtils;
import org.matsim.freight.logistics.shipment.LspShipment;
import org.matsim.freight.logistics.shipment.LspShipmentUtils;

/**
 * Tests that {@link LSP#scheduleLogisticChains()} starts with empty {@link WaitingShipments} of the logistic chain
 * elements, so that repeated scheduling runs do not accumulate shipments.
 */
public class LSPImplScheduleLogisticChainsTest {

	private LSP lsp;
	private LspShipment lspShipment;
	private LogisticChainElement firstElement;
	private LogisticChainElement secondElement;

	@BeforeEach
	public void initialize() {
		LSPResource firstResource = new TestLspResource(Id.create("firstResource", LSPResource.class));
		LSPResource secondResource = new TestLspResource(Id.create("secondResource", LSPResource.class));

		firstElement = LSPUtils.LogisticChainElementBuilder.newInstance(Id.create("firstElement", LogisticChainElement.class))
			.setResource(firstResource)
			.build();
		secondElement = LSPUtils.LogisticChainElementBuilder.newInstance(Id.create("secondElement", LogisticChainElement.class))
			.setResource(secondResource)
			.build();

		LogisticChain chain = LSPUtils.LogisticChainBuilder.newInstance(Id.create("chain", LogisticChain.class))
			.addLogisticChainElement(firstElement)
			.addLogisticChainElement(secondElement)
			.build();

		LSPPlan plan = LSPUtils.createLSPPlan()
			.setInitialShipmentAssigner(ResourceImplementationUtils.createSingleLogisticChainShipmentAssigner())
			.addLogisticChain(chain);

		lsp = LSPUtils.LSPBuilder.getInstance(Id.create("lsp", LSP.class))
			.setInitialPlan(plan)
			.setLogisticChainScheduler(
				ResourceImplementationUtils.createDefaultSimpleForwardLogisticChainScheduler(List.of(firstResource, secondResource)))
			.build();

		LspShipmentUtils.LspShipmentBuilder shipmentBuilder = LspShipmentUtils.LspShipmentBuilder.newInstance(Id.create("shipment", LspShipment.class));
		shipmentBuilder.setFromLinkId(Id.create("from", Link.class));
		shipmentBuilder.setToLinkId(Id.create("to", Link.class));
		shipmentBuilder.setStartTimeWindow(TimeWindow.newInstance(0., 3600.));
		shipmentBuilder.setEndTimeWindow(TimeWindow.newInstance(0., 7200.));
		shipmentBuilder.setCapacityDemand(1);
		lspShipment = shipmentBuilder.build();
		lsp.assignShipmentToLspPlan(lspShipment);
	}

	@Test
	public void testRepeatedSchedulingDoesNotAccumulateShipments() {
		lsp.scheduleLogisticChains();
		lsp.scheduleLogisticChains();

		// The test resources do not move the shipments, so they stay in the incoming shipments of the first element.
		assertEquals(List.of(lspShipment), List.copyOf(firstElement.getIncomingShipments().getLspShipmentsWTime()));
	}

	@Test
	public void testLeftoverShipmentsAreRemoved() {
		secondElement.getIncomingShipments().addShipment(0., lspShipment);
		secondElement.getOutgoingShipments().addShipment(0., lspShipment);

		lsp.scheduleLogisticChains();

		assertTrue(secondElement.getIncomingShipments().getLspShipmentsWTime().isEmpty());
		assertTrue(secondElement.getOutgoingShipments().getLspShipmentsWTime().isEmpty());
		assertTrue(firstElement.getOutgoingShipments().getLspShipmentsWTime().isEmpty());
		assertEquals(1, firstElement.getIncomingShipments().getLspShipmentsWTime().size());
	}

	@Test
	public void testShipmentPlansOfSelectedPlanAreRemoved() {
		// assignShipmentToLspPlan has created a shipment plan for the shipment
		assertEquals(1, lsp.getSelectedPlan().getShipmentPlans().size());

		lsp.scheduleLogisticChains();

		// The test resources do not schedule anything, so no shipment plan is re-created.
		assertTrue(lsp.getSelectedPlan().getShipmentPlans().isEmpty());
	}
}
