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

import java.util.*;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.freight.carriers.CarrierVehicle;
import org.matsim.freight.carriers.Carriers;
import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.freight.logistics.consistency_checkers.LogisticsConsistencyChecker;
import org.matsim.freight.logistics.io.LSPPlanXmlReader;
import org.matsim.freight.logistics.shipment.LspShipment;
import org.matsim.freight.logistics.shipment.LspShipmentPlan;
import org.matsim.freight.logistics.shipment.LspShipmentUtils;
import org.matsim.utils.objectattributes.attributable.Attributable;


public final class LSPUtils {

	static final Logger log = LogManager.getLogger(LSPUtils.class);

	private static final String lspsString = "lsps";

	private LSPUtils() {
	} // do not instantiate

	public static LSPPlan createLSPPlan() {
		return new LSPPlanImpl();
	}


	/**
	 * Checks, is the plan the selected plan.
	 * (This is adapted copy from PersonUtils.isSelected(plan) )
	 *
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
	public static LSPs addOrGetLsps(Scenario scenario) {
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
	 * @param lsp        In this LSP this method tries to find the shipment.
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
	 * @param lspPlan    the lspPlan: It contains the information of its shipmentPlans
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

	public static void scheduleLsps(LSPs lsps) {
		LogisticsConsistencyChecker.checkBeforePlanning(lsps, Level.ERROR);
		for (LSP lsp : lsps.getLSPs().values()) {

			log.info("schedule the LSP: {} with the shipments and according to the scheduler of the Resource", lsp.getId());
			lsp.scheduleLogisticChains();
		}
		LogisticsConsistencyChecker.checkAfterPlanning(lsps, Level.ERROR);
	}

	/**
	 * Splits the shipments of the given LSP if they are larger than the smallest vehicle capacity of the LSP's resources.
	 * This method will create new shipments with adjusted sizes and service times.
	 * It will also clear the existing shipments and plans in the LSP before adding the new shipments.
	 * The new shipments will be assigned to the LSP's plans.
	 * <p></p>
	 * To avoid any issues, this method works with a copy of the original LSP that was handed over.
	 *
	 * @param lspOrig the lsp for which the shipments should be split if needed
	 * @return the lsp with the updated shipments
	 */
	public static LSP splitShipmentsIfNeeded(LSP lspOrig) {
		LSP lsp = lspOrig;
		if (lsp.getLspShipments().isEmpty()) {
			log.warn("LSP {} has no shipments. No splitting will be done. ", lsp.getId());
			return lsp;
		}

		double lowestCapacity = Double.MAX_VALUE;
		for (LSPResource lspResource : lsp.getResources()) {
			if (lspResource instanceof LSPCarrierResource lspCarrierResource) {
				for (CarrierVehicle carrierVehicle : lspCarrierResource.getCarrier().getCarrierCapabilities().getCarrierVehicles().values()) {
					var vehCapacity= carrierVehicle.getType().getCapacity().getOther();
					if (vehCapacity < lowestCapacity) {
						lowestCapacity = vehCapacity;
					}
				}
			}
		}

		if (lowestCapacity == Double.MAX_VALUE) {
			log.error("LSP: {}: Did not find the capacities of the vehicles from the CarrierResources. Aborting", lsp.getId());
			throw new IllegalStateException();
		} else {
			lowestCapacity = (int) lowestCapacity; // ensure that the capacity is an integer value
		}

		List<LspShipment> newShipments = new LinkedList<>();

		for (LspShipment lspShipment : lsp.getLspShipments()) {
			int sizeOfShipment = lspShipment.getSize();
			int sizeOfNewShipments =0;
			double durationPickupOfNewShipments = 0;

			if (lspShipment.getSize() > lowestCapacity && lowestCapacity > 0 ) {
				log.warn("Shipment {} of LSP {} has a size of {}, which is larger than the smallest vehicle capacity of {}. This may lead to problems during scheduling. Will split it into smaller parts.",
					lspShipment.getId(), lsp.getId(), lspShipment.getSize(), lowestCapacity);
				int fullParts = (int) (lspShipment.getSize() / lowestCapacity);
				double rest = lspShipment.getSize() % lowestCapacity;
				char suffix = 'a';
				for (int i = 0; i < fullParts; i++) {
					LspShipment part = createLspShipmentWithNewIdAndSize(lspShipment, Id.create(lspShipment.getId().toString() + "_" + suffix, LspShipment.class), (int) lowestCapacity);
					newShipments.add(part);
					sizeOfNewShipments = sizeOfNewShipments + part.getSize();
					durationPickupOfNewShipments = durationPickupOfNewShipments + part.getPickupServiceTime();
					suffix++;
				}
				if (rest > 0) {
					LspShipment part = createLspShipmentWithNewIdAndSize(lspShipment, Id.create(lspShipment.getId().toString() + "_" + suffix, LspShipment.class), (int) rest);
					newShipments.add(part);
					sizeOfNewShipments = sizeOfNewShipments + part.getSize();
					durationPickupOfNewShipments = durationPickupOfNewShipments + part.getPickupServiceTime();
				}
				log.info("Shipment {} of LSP {} was split into {} parts due to capacity limit {}.", lspShipment.getId(), lsp.getId(), newShipments.size(), lowestCapacity);

				//Assert that the size of the new shipments matches the original shipment size
				if (sizeOfNewShipments != sizeOfShipment) {
					log.error("The size of the new shipments {} ({}) does not match the original shipment size ({}). This may lead to problems during scheduling.", lspShipment.getId(), sizeOfNewShipments, sizeOfShipment);
					throw new IllegalStateException("Sum of demand of the split shipments does not match the original shipment size.");
				}

				//Assert that the duration of the pickup of the new created shipments matches the original shipment duration
				if (durationPickupOfNewShipments != lspShipment.getPickupServiceTime()) {
					log.error("The pickupServiceTime of all new shipments {} ({}) does not match the original pickupServiceTime ({}). This will change the problem.", lspShipment.getId(), durationPickupOfNewShipments, lspShipment.getPickupServiceTime());
					throw new IllegalStateException("Sum of pickupDurations of the split shipments does not match the original shipment pickupDurations.");
				}

			} else { //keep the shipment as it is
				newShipments.add(lspShipment);
			}
		}

		// Clear the existing shipments and plans in the LSP
		for (LSPPlan lspPlan : lsp.getPlans()) {
			lspPlan.getShipmentPlans().clear();
			for (LogisticChain logisticChain : lspPlan.getLogisticChains()) {
				logisticChain.getLspShipmentIds().clear();
			}
		}
		lsp.getLspShipments().clear();

		// Add the new shipments to the LSP and assign them to the lspPlans
		// Todo: Now, this is done with the some algorithm as in the InitialShipmentAssigner.
		// so it may be that the (split) shipments are not assigned to the same plans as before and/or that some parts of the same original shipment are assigned to different plans.
		for (LspShipment newLspShipment : newShipments) {
			lsp.assignShipmentToLspPlan(newLspShipment);
		}

		return lsp;
	}

	/**
	 * Creates a new {@link LspShipment} with a new Id and size.
	 * The durations for pickup and delivery are adjusted proportionally to the new size.
	 * All other parameters are copied from the given {@link LspShipment}.
	 * @param lspShipment the original LspShipment to copy parameters from
	 * @param newId the new Id for the LspShipment
	 * @param size	the new size for the LspShipment
	 * @return the new LspShipment with the given Id and size
	 */
	static LspShipment createLspShipmentWithNewIdAndSize(LspShipment lspShipment, Id<LspShipment> newId, int size) {
		var proportion = (double) size / lspShipment.getSize();

		var builder = LspShipmentUtils.LspShipmentBuilder.newInstance(newId);
			builder.setCapacityDemand(size);
			builder.setFromLinkId(lspShipment.getFrom());
			builder.setToLinkId(lspShipment.getTo());
			builder.setPickupServiceTime(lspShipment.getPickupServiceTime() * proportion);
			builder.setStartTimeWindow(lspShipment.getPickupTimeWindow());
			builder.setDeliveryServiceTime(lspShipment.getDeliveryServiceTime() * proportion);
			builder.setEndTimeWindow(lspShipment.getDeliveryTimeWindow());
		return builder.build();
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
