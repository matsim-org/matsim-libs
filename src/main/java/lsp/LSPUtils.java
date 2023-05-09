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

package lsp;

import lsp.shipment.LSPShipment;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.controler.FreightUtils;
import org.matsim.utils.objectattributes.attributable.Attributable;

import java.util.ArrayList;
import java.util.Collection;

public final class LSPUtils {
	private static final String lspsString = "lsps";

	private LSPUtils() {
	} // do not instantiate

	public static LSPPlan createLSPPlan() {
		return new LSPPlanImpl();
	}

	public static LogisticChainScheduler createForwardLogisiticChainScheduler() {
		return new ForwardLogisticChainSchedulerImpl();
	}

	public static WaitingShipments createWaitingShipments() {
		return new WaitingShipmentsImpl();
	}

	public static void addLSPs(Scenario scenario, LSPs lsps) {
		Carriers carriers = FreightUtils.addOrGetCarriers( scenario );
		//Register carriers from all lsps
		for (LSP lsp : lsps.getLSPs().values()){
			for (LSPResource lspResource : lsp.getResources()) {
				if (lspResource instanceof LSPCarrierResource lspCarrierResource) {
					carriers.addCarrier(lspCarrierResource.getCarrier());
				}
			}
		}
		scenario.addScenarioElement(lspsString, lsps);
	}

	public static LSPs getLSPs(Scenario scenario) {
		Object result = scenario.getScenarioElement(lspsString);
		if (result == null) {
			throw new RuntimeException("there is no scenario element of type " + lspsString +
					".  You will need something like LSPUtils.addLSPs( scenario, lsps) somewhere.");
		}
		return (LSPs) result;
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
	//	The following would be closer to how we have done it elsewhere (scenario containers are mutable).  kai, may'22'
//	public static LSPs createOrGetLPSs( Scenario scenario ){
//		Object result = scenario.getScenarioElement( lspsString );
//		LSPs lsps;
//		if ( result != null ) {
//			lsps = (LSPs) result;
//		} else {
//			lsps = new LSPs(  );
//			scenario.addScenarioElement( lspsString, lsps );
//		}
//		return lsps;
//	}

	public static void setFixedCost(Attributable attributable, Double fixedCost) {
		attributable.getAttributes().putAttribute("fixedCost", fixedCost);
	}

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
//		 * @deprecated -- It feels attractive to attach this to the "agent".  A big disadvantage with this approach, however, is that
//		 * 		we cannot use injection ... since we cannot inject as many scorers as we have agents.  (At least this is what I think.) Which means
//		 * 		that the approach in matsim core and in carriers to have XxxScoringFunctionFactory is better for what we are doing here.  yyyyyy So
//		 * 		this needs to be changed.  kai, jul'22
//		 */
//		public LSPBuilder setSolutionScorer(LSPScorer scorer) {
//			this.scorer = scorer;
//			return this;
//		}

//		/**
//		 * @deprecated -- It feels attractive to attach this to the "agent".  A big disadvantage with this approach, however, is that
//		 * 		we cannot use injection ... since we cannot inject as many replanners as we have agents.  (At least this is what I think.)  yyyyyy So
//		 * 		this needs to be changed.  kai, jul'22
//		 */
//		public LSPBuilder setReplanner(LSPReplanner replanner) {
//			this.replanner = replanner;
//			return this;
//		}
		// never used.  Thus disabling it.  kai, jul'22


		public LSPBuilder setInitialPlan(LSPPlan plan) {
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
			this.trackers = new ArrayList<LSPSimulationTracker<LogisticChain>>();
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

	/**
	 * Gives back the LspShipment object of the LSP, which matches to the shipmentId
	 * TODO: This is a workaround. In future it is maybe useful, that the LSP contains a map instead if a Collection to store its shipments.
	 * @param lsp
	 * @param shipmentId
	 * @return The lspShipment object.
	 */
	public static LSPShipment findLspShipment(LSP lsp, Id<LSPShipment> shipmentId){
		for (LSPShipment lspShipment : lsp.getShipments()) {
			if (lspShipment.getId().equals(shipmentId)) {
				return lspShipment;
			}
		}
		return null;
	}

}
