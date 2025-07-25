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

package org.matsim.freight.logistics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.freight.carriers.Carriers;
import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.freight.logistics.io.LSPPlanXmlReader;
import org.matsim.freight.logistics.shipment.LspShipment;
import org.matsim.freight.logistics.shipment.LspShipmentPlan;
import org.matsim.utils.objectattributes.attributable.Attributable;

public final class LSPUtils {
	private static final String lspsString = "lsps";

	private LSPUtils() {} // do not instantiate

	public static LSPPlan createLSPPlan() {
		return new LSPPlanImpl();
	}


	/**
	 * Checks, is the plan the selcted plan.
	 * (This is adapted copy from PersonUtils.isSelected(plan) )
	 * @param lspPlan the plan to check
	 * @return true if the plan is the selected plan. false, if not.
	 */
	public static boolean isPlanTheSelectedPlan(LSPPlan lspPlan) {
		return lspPlan.getLSP().getSelectedPlan() == lspPlan;
	}

	public static LogisticChainScheduler createForwardLogisticChainScheduler() {
		return new ForwardLogisticChainSchedulerImpl();
	}

	public static WaitingShipments createWaitingShipments() {
		return new WaitingShipmentsImpl();
	}

	/**
	 * Will return the lsps container of the scenario.
	 * If it does not exist, it will be created.
	 *
	 * @param scenario The scenario where the LSPs should be added.
	 * @return the lsps container
	 */
	public static LSPs addOrGetLsps( Scenario scenario ) {
		LSPs lsps = (LSPs) scenario.getScenarioElement(lspsString);
		if (lsps == null) {
			lsps = new LSPs(Collections.emptyList());
			scenario.addScenarioElement(lspsString, lsps);
		}
		return lsps;
	}

	public static void loadLspsIntoScenario(Scenario scenario, Collection<LSP> lspsToLoad) {
		Carriers carriers = CarriersUtils.addOrGetCarriers(scenario);
		// Register carriers from all lspsToLoad
		for (LSP lsp : lspsToLoad) {
			for (LSPResource lspResource : lsp.getResources()) {
				if (lspResource instanceof LSPCarrierResource lspCarrierResource) {
					carriers.addCarrier(lspCarrierResource.getCarrier());
				}
			}
		}
		addOrGetLsps(scenario).addLsps(lspsToLoad);
	}

	public static LSPs getLSPs(Scenario scenario) {
		Object result = scenario.getScenarioElement(lspsString);
		if (result == null) {
			throw new RuntimeException("there is no scenario element of type " + lspsString + ".  You will need something like LSPUtils.addLSPs( scenario, lsps) somewhere.");
		}
		return (LSPs) result;
	}

	public static void loadLspsAccordingToConfig(Scenario scenario) {
		CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);

		LSPs lsps = addOrGetLsps(scenario);
		Carriers carriers = CarriersUtils.getCarriers(scenario);

		FreightLogisticsConfigGroup logisticsConfig = ConfigUtils.addOrGetModule(scenario.getConfig(), FreightLogisticsConfigGroup.class);
		String lspsFilePath = logisticsConfig.getLspsFile();

		if (lspsFilePath == null || lspsFilePath.isEmpty()) {
			throw new IllegalArgumentException("LSPs file path should not be null or empty");
		}
		new LSPPlanXmlReader(lsps, carriers).readURL(IOUtils.extendUrl(scenario.getConfig().getContext(), lspsFilePath));

	}


	public static Double getVariableCost(Attributable attributable) {
		return (Double) attributable.getAttributes().getAttribute("variableCost");
	}

	public static void setVariableCost(Attributable attributable, Double variableCost) {
		attributable.getAttributes().putAttribute("variableCost", variableCost);
	}

	public static Double getFixedCost(Attributable attributable) {
		return (Double) attributable.getAttributes().getAttribute("fixedCost");
	}



	public static void setFixedCost(Attributable attributable, Double fixedCost) {
		attributable.getAttributes().putAttribute("fixedCost", fixedCost);
	}

	/**
	 * Gives back the {@link LspShipment} object of the {@link LSP}, which matches to the shipmentId
	 *
	 * @param lsp In this LSP this method tries to find the shipment.
	 * @param shipmentId Id of the shipment that should be found.
	 * @return the lspShipment object or null, if it is not found.
	 */
	public static LspShipment findLspShipment(LSP lsp, Id<LspShipment> shipmentId) {
		for (LspShipment lspShipment : lsp.getLspShipments()) {
			if (lspShipment.getId().equals(shipmentId)) {
				return lspShipment;
			}
		}
		return null;
	}

	/**
	 * Returns the {@link LspShipmentPlan} of an {@link LspShipment}.
	 *
	 * @param lspPlan the lspPlan: It contains the information of its shipmentPlans
	 * @param shipmentId Id of the shipment that should be found.
	 * @return the shipmentPlan object or null, if it is not found.
	 */
	public static LspShipmentPlan findLspShipmentPlan(LSPPlan lspPlan, Id<LspShipment> shipmentId) {
		for (LspShipmentPlan lspShipmentPlan : lspPlan.getShipmentPlans()) {
			if (lspShipmentPlan.getLspShipmentId().equals(shipmentId)) {
				return lspShipmentPlan;
			}
		}
		return null;
	}

	public enum LogicOfVrp {serviceBased, shipmentBased}

	public static final class LSPBuilder {
		final Collection<LSPResource> resources;
		final Id<LSP> id;
		LogisticChainScheduler logisticChainScheduler;
		LSPPlan initialPlan;

		private LSPBuilder(Id<LSP> id) {
			this.id = id; // this line was not there until today.  kai, may'22
			this.resources = new ArrayList<>();
		}

		public static LSPBuilder getInstance(Id<LSP> id) {
			return new LSPBuilder(id);
		}

		public LSPBuilder setLogisticChainScheduler(LogisticChainScheduler logisticChainScheduler) {
			this.logisticChainScheduler = logisticChainScheduler;
			return this;
		}

		//		/**
		//		 * @deprecated -- It feels attractive to attach this to the "agent".  A big disadvantage
		// with this approach, however, is that
		//		 * 		we cannot use injection ... since we cannot inject as many scorers as we have agents.
		// (At least this is what I think.) Which means
		//		 * 		that the approach in matsim core and in carriers to have XxxScoringFunctionFactory is
		// better for what we are doing here.  yyyyyy So
		//		 * 		this needs to be changed.  kai, jul'22
		//		 */
		//		public LSPBuilder setSolutionScorer(LSPScorer scorer) {
		//			this.scorer = scorer;
		//			return this;
		//		}

		//		/**
		//		 * @deprecated -- It feels attractive to attach this to the "agent".  A big disadvantage
		// with this approach, however, is that
		//		 * 		we cannot use injection ... since we cannot inject as many replanners as we have
		// agents.  (At least this is what I think.)  yyyyyy So
		//		 * 		this needs to be changed.  kai, jul'22
		//		 */
		//		public LSPBuilder setReplanner(LSPReplanner replanner) {
		//			this.replanner = replanner;
		//			return this;
		//		}
		// never used.  Thus disabling it.  kai, jul'22

		@Deprecated // see comment below method name.
		public LSPBuilder setInitialPlan(LSPPlan plan) {
			// yy maybe one could use the first plan by default, and then get rid of this method here?  kai, jul'25
			this.initialPlan = plan;
			for (LogisticChain solution : plan.getLogisticChains()) {
				for (LogisticChainElement element : solution.getLogisticChainElements()) {
					if (!resources.contains(element.getResource())) {
						resources.add(element.getResource());
					}
				}
			}
			return this;
		}

		public LSP build() {
			return new LSPImpl(this);
		}
	}

	public static final class LogisticChainBuilder {
		final Id<LogisticChain> id;
		final Collection<LogisticChainElement> elements;
		//		final Collection<EventHandler> eventHandlers;
		final Collection<LSPSimulationTracker<LogisticChain>> trackers;

		private LogisticChainBuilder(Id<LogisticChain> id) {
			this.elements = new ArrayList<>();
			this.trackers = new ArrayList<>();
			this.id = id;
		}

		public static LogisticChainBuilder newInstance(Id<LogisticChain> id) {
			return new LogisticChainBuilder(id);
		}

		public LogisticChainBuilder addLogisticChainElement(LogisticChainElement element) {
			elements.add(element);
			return this;
		}

		public LogisticChainBuilder addTracker(LSPSimulationTracker<LogisticChain> tracker) {
			trackers.add(tracker);
			return this;
		}

		public LogisticChain build() {
			//TODO: Prüfe of das alle Elemente Verbunden sind (in irgendeiner Art). Plus Hinweis auf die Änderung.
			// yyyyyy was ist mit "Hinweis auf Änderung" gemeint???

			// connect all elements:
			LogisticChainElement prev = null;
			for( LogisticChainElement logisticChainElement : this.elements ){
				if ( prev != null ) {
					prev.connectWithNextElement( logisticChainElement );
				}
				prev = logisticChainElement;
			}

			return new LogisticChainImpl(this);
		}
	}

	public static final class LogisticChainElementBuilder {
		final Id<LogisticChainElement> id;
		final WaitingShipments incomingShipments;
		final WaitingShipments outgoingShipments;
		LSPResource resource;

		private LogisticChainElementBuilder(Id<LogisticChainElement> id) {
			this.id = id;
			this.incomingShipments = createWaitingShipments();
			this.outgoingShipments = createWaitingShipments();
		}

		public static LogisticChainElementBuilder newInstance(Id<LogisticChainElement> id) {
			return new LogisticChainElementBuilder(id);
		}

		public LogisticChainElementBuilder setResource(LSPResource resource) {
			this.resource = resource;
			return this;
		}

		public LogisticChainElement build() {
			return new LogisticChainElementImpl(this);
		}
	}
}
