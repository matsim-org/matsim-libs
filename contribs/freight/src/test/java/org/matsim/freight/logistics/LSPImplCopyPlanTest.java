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

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.freight.logistics.resourceImplementations.ResourceImplementationUtils;
import org.matsim.freight.logistics.shipment.LspShipment;

/**
 * Tests {@link LSPImpl#copyPlan(LSPPlan)} and {@link LSPImpl#createCopyOfSelectedPlanAndMakeSelected()}.
 */
public class LSPImplCopyPlanTest {

	private LSP lsp;
	private LSPPlan originalPlan;
	private LogisticChain originalChain;
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

		originalChain = LSPUtils.LogisticChainBuilder.newInstance(Id.create("chain", LogisticChain.class))
			.addLogisticChainElement(firstElement)
			.addLogisticChainElement(secondElement)
			.build();
		originalChain.getLspShipmentIds().add(Id.create("shipment1", LspShipment.class));
		originalChain.getAttributes().putAttribute("color", "red");
		originalChain.addSimulationTracker(new TestChainTracker());

		originalPlan = LSPUtils.createLSPPlan()
			.setInitialShipmentAssigner(ResourceImplementationUtils.createSingleLogisticChainShipmentAssigner())
			.addLogisticChain(originalChain)
			.setType("originalType");
		originalPlan.setScore(42.);

		lsp = LSPUtils.LSPBuilder.getInstance(Id.create("lsp", LSP.class))
			.setInitialPlan(originalPlan)
			.setLogisticChainScheduler(
				ResourceImplementationUtils.createDefaultSimpleForwardLogisticChainScheduler(List.of(firstResource, secondResource)))
			.build();
	}

	@Test
	public void testPlanPropertiesAreCopied() {
		LSPPlan copiedPlan = LSPImpl.copyPlan(originalPlan);

		assertNotSame(originalPlan, copiedPlan);
		assertSame(lsp, copiedPlan.getLSP());
		assertSame(originalPlan.getInitialShipmentAssigner(), copiedPlan.getInitialShipmentAssigner());
		assertEquals(42., copiedPlan.getScore());
		assertEquals("originalType", copiedPlan.getType());
		assertTrue(copiedPlan.getShipmentPlans().isEmpty());
	}

	@Test
	public void testChainIsNewObjectWithBackpointerToLsp() {
		LogisticChain copiedChain = getOnlyChain(LSPImpl.copyPlan(originalPlan));

		assertNotSame(originalChain, copiedChain);
		assertEquals(originalChain.getId(), copiedChain.getId());
		assertSame(lsp, copiedChain.getLSP());
	}

	@Test
	public void testChainElementsAreShared() {
		LogisticChain copiedChain = getOnlyChain(LSPImpl.copyPlan(originalPlan));

		List<LogisticChainElement> copiedElements = new ArrayList<>(copiedChain.getLogisticChainElements());
		assertEquals(2, copiedElements.size());
		assertSame(firstElement, copiedElements.get(0));
		assertSame(secondElement, copiedElements.get(1));
		assertSame(secondElement, copiedElements.get(0).getNextElement());

		// copying must not register additional client elements with the resources
		assertEquals(1, firstElement.getResource().getClientElements().size());
		assertEquals(1, secondElement.getResource().getClientElements().size());
	}

	@Test
	public void testShipmentIdsAreIndependent() {
		LogisticChain copiedChain = getOnlyChain(LSPImpl.copyPlan(originalPlan));

		assertEquals(originalChain.getLspShipmentIds(), copiedChain.getLspShipmentIds());
		assertNotSame(originalChain.getLspShipmentIds(), copiedChain.getLspShipmentIds());

		copiedChain.getLspShipmentIds().add(Id.create("shipment2", LspShipment.class));
		assertEquals(1, originalChain.getLspShipmentIds().size());
		assertEquals(2, copiedChain.getLspShipmentIds().size());
	}

	@Test
	public void testChainAttributesAreCopiedAndIndependent() {
		LogisticChain copiedChain = getOnlyChain(LSPImpl.copyPlan(originalPlan));

		assertEquals("red", copiedChain.getAttributes().getAttribute("color"));

		copiedChain.getAttributes().putAttribute("color", "blue");
		assertEquals("red", originalChain.getAttributes().getAttribute("color"));
	}

	@Test
	public void testChainTrackersAreNotCopied() {
		LogisticChain copiedChain = getOnlyChain(LSPImpl.copyPlan(originalPlan));

		assertEquals(1, originalChain.getSimulationTrackers().size());
		assertTrue(copiedChain.getSimulationTrackers().isEmpty());
	}

	@Test
	public void testCreateCopyOfSelectedPlanAndMakeSelected() {
		LSPPlan copiedPlan = lsp.createCopyOfSelectedPlanAndMakeSelected();

		assertSame(copiedPlan, lsp.getSelectedPlan());
		assertEquals(2, lsp.getPlans().size());
		assertTrue(lsp.getPlans().contains(originalPlan));
		assertSame(originalChain, getOnlyChain(originalPlan));
	}

	private static LogisticChain getOnlyChain(LSPPlan plan) {
		assertEquals(1, plan.getLogisticChains().size());
		return plan.getLogisticChains().iterator().next();
	}

	/** Tracker that does nothing; only needed to check whether chain trackers are copied. */
	private static final class TestChainTracker implements LSPSimulationTracker<LogisticChain> {
		@Override
		public void setEmbeddingContainer(LogisticChain pointer) {
		}
	}
}
