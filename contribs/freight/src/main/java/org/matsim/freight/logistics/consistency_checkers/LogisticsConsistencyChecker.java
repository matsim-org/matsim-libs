/*
 *   *********************************************************************** *
 *   project: org.matsim.*													 *
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C) 2025 by the members listed in the COPYING, 		 *
 *                          LICENSE and WARRANTY file.  					 *
 *   email           : info at matsim dot org   							 *
 *                                                                         	 *
 *   *********************************************************************** *
 *                                                                        	 *
 *     This program is free software; you can redistribute it and/or modify  *
 *      it under the terms of the GNU General Public License as published by *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.								     *
 *     See also COPYING, LICENSE and WARRANTY file						 	 *
 *                                                                           *
 *   *********************************************************************** *
 */

package org.matsim.freight.logistics.consistency_checkers;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.freight.logistics.LSP;
import org.matsim.freight.logistics.LSPPlan;
import org.matsim.freight.logistics.LSPResource;
import org.matsim.freight.logistics.LSPs;
import org.matsim.freight.logistics.shipment.LspShipment;
import org.matsim.freight.logistics.shipment.LspShipmentPlan;

import java.util.LinkedList;
import java.util.List;

/**
 * @author anton stock
 * this class provides tests to check if resource Ids are unique and if all existing shipments are assigned to exactly one plan.
 */
public class LogisticsConsistencyChecker {

	public enum CheckResult {
		CHECK_SUCCESSFUL, CHECK_FAILED
	}

	//needed for log messages, log level etc.
	private static final Logger log = LogManager.getLogger(LogisticsConsistencyChecker.class);

	private static Level setInternalLogLevel(Level level) {
		if (level == Level.FATAL) {
			return Level.ERROR;
		} else {
			return level;
		}
	}

	/**
	 * This checks the consistency of the LSPs before planning.
	 * Currently, it checks if all resource Ids are unique over all LSPs.
	 *
	 * @param lsps the LSPs to check
	 * @param lvl Level of log messages / errors
	 * @return CheckResult (CHECK_SUCCESSFUL, CHECK_FAILED)
	 */
	public static CheckResult checkBeforePlanning(LSPs lsps, Level lvl) {
		Level level = setInternalLogLevel(lvl);
		log.info("Start checking if all resource Ids are unique.");
		CheckResult result = resourcesAreUnique(lsps, level);
		if (result == CheckResult.CHECK_FAILED) {
			log.log(level, "Check failed. Please check the log messages for more information.");
			return CheckResult.CHECK_FAILED;
		}
		log.info("Check passed: All resource Ids are unique.");
		return CheckResult.CHECK_SUCCESSFUL;
	}


	/**
	 * This checks the consistency of the LSPs after planning.
	 * Currently, it checks if all shipments are included in exactly one ShipmentPlan.
	 *
	 * @param lsps the LSPs to check
	 * @param lvl Level of log messages / errors
	 * @return CheckResult (CHECK_SUCCESSFUL, CHECK_FAILED)
	 */
	public static CheckResult checkAfterPlanning(LSPs lsps, Level lvl) {
		Level level = setInternalLogLevel(lvl);
		int nuOfChecksFailed = 0;
		if (shipmentForEveryShipmentPlanAllPlans(lsps, level) == CheckResult.CHECK_FAILED) {
			nuOfChecksFailed++;
		}
		if (shipmentPlanForEveryShipmentAllPlans(lsps, level) == CheckResult.CHECK_FAILED) {
			nuOfChecksFailed++;
		}

		//If no Check has failed, everything is fine :)
		if (nuOfChecksFailed == 0) {
			log.info("Checks passed.");
			return CheckResult.CHECK_SUCCESSFUL;
		} else {
			log.log(level, "Check(s) failed. Please check the log messages for more information.");
			return CheckResult.CHECK_FAILED;
		}
	}

	/**
	 * this method will check if all existing resource Ids are unique.
	 *
	 * @param lsps the LSPs to check
	 * @param level  level of log messages / errors
	 * @return CheckResult
	 */
	/*package-private*/ static CheckResult resourcesAreUnique(LSPs lsps, Level level) {
		//all resource ids are being saved in this list
		List<Id<LSPResource>> lspResourceList = new LinkedList<>();
		//true, as long as no resource id exists more than once
		boolean resourceIdsAreUnique = true;
		for (LSP lsp : lsps.getLSPs().values()) {
			for (LSPResource resource : lsp.getResources()) {
				//if a resource id is already in this list, the id exists more than once
				if (lspResourceList.contains(resource.getId())) {
					log.log(level, "Resource with Id '{}' exists more than once. This must not be!" +
						"You may also use output_lsp.xml to check were the duplicates are located", resource.getId());
					resourceIdsAreUnique = false;
				} else {
					lspResourceList.add(resource.getId());
				}
			}
		}
		//check is successful, if all resource ids are unique
		if (resourceIdsAreUnique) {
//			log.debug("All resource Ids are unique.");
			return CheckResult.CHECK_SUCCESSFUL;
		} else {
			log.log(level, "There are resource Ids that exist more than once. Please check the log messages for more information.");
			return CheckResult.CHECK_FAILED;
		}
	}

	/**
	 * this method will check if every shipment has got a shipment plan (selected plan only)
	 *
	 * @param lsps the LSPs to check
	 * @param lvl  level of log messages / errors
	 * @return CheckResult
	 */
	/*package-private*/ static CheckResult shipmentPlanForEveryShipmentSelectedPlanOnly(LSPs lsps, Level lvl) {
		//lists for plan Ids, shipment Ids
		List<Id<LspShipment>> lspShipmentPlansList = new LinkedList<>();
		List<Id<LspShipment>> lspShipmentsList = new LinkedList<>();
		List<Id<LspShipment>> shipmentsWithoutPlan = new LinkedList<>();
		Level level = setInternalLogLevel(lvl);

		for (LSP lsp : lsps.getLSPs().values()) {
			for (LspShipment lspShipment : lsp.getLspShipments()) {
				lspShipmentsList.add(lspShipment.getId());
			}
			//for selected plan only
			for (LspShipmentPlan shipmentSelectedPlan : lsp.getSelectedPlan().getShipmentPlans()) {
				lspShipmentPlansList.add(shipmentSelectedPlan.getLspShipmentId());
			}

			for (Id<LspShipment> shipmentId : lspShipmentsList) {
				if (!lspShipmentPlansList.contains(shipmentId)) {
					shipmentsWithoutPlan.add(shipmentId);
					log.log(level, "LSP {}: Shipment with Id '{}' has no corresponding plan!", lsp.getId().toString(), shipmentId);
				}
			}
		}

		if (!shipmentsWithoutPlan.isEmpty()) {
			log.log(level, "Shipment(s) without a plan: {}", shipmentsWithoutPlan);
			return CheckResult.CHECK_FAILED;
		} else {
//			log.debug("All shipments have plans.");
			return CheckResult.CHECK_SUCCESSFUL;
		}
	}
	/**
	 * this method will check if every shipment has got a shipment plan (all plans)
	 *
	 * @param lsps the LSPs to check
	 * @param lvl  level of log messages / errors
	 * @return CheckResult
	 */
	/*package-private*/ static CheckResult shipmentPlanForEveryShipmentAllPlans(LSPs lsps, Level lvl) {
		//all resource Ids are being saved in this list
		List<Id<LspShipment>> lspShipmentPlansList = new LinkedList<>();
		List<Id<LspShipment>> lspShipmentsList = new LinkedList<>();
		List<Id<LspShipment>> shipmentsWithoutPlan = new LinkedList<>();
		Level level = setInternalLogLevel(lvl);

		for (LSP lsp : lsps.getLSPs().values()) {
			for (LspShipment lspShipment : lsp.getLspShipments()) {
				lspShipmentsList.add(lspShipment.getId());
			}
			//for all plans
			for (LSPPlan lspPlan : lsp.getPlans()) {
				for (LspShipmentPlan shipmentPlan : lspPlan.getShipmentPlans()) {
					lspShipmentPlansList.add(shipmentPlan.getLspShipmentId());
				}
			}
			for (Id<LspShipment> shipmentId : lspShipmentsList) {
				if (!lspShipmentPlansList.contains(shipmentId)) {
					shipmentsWithoutPlan.add(shipmentId);
					log.log(level, "LSP {}: Shipment with Id '{}' has no corresponding plan!", lsp.getId().toString(), shipmentId);
				}
			}
		}

		if (!shipmentsWithoutPlan.isEmpty()) {
			log.log(level, "Shipment(s) without a plan: {}", shipmentsWithoutPlan);
			return CheckResult.CHECK_FAILED;
		} else {
//			log.debug("All shipments have plans.");
			return CheckResult.CHECK_SUCCESSFUL;
		}
	}

	/**
	 * this method will check if every shipmentPlan has got a shipment (selected plan only)
	 *
	 * @param lsps the LSPs to check
	 * @param lvl  level of log messages / errors
	 * @return CheckResult
	 */
	/*package-private*/ static CheckResult shipmentForEveryShipmentPlanSelectedPlanOnly(LSPs lsps, Level lvl) {
		Level level = setInternalLogLevel(lvl);

		List<Id<LspShipment>> shipmentPlans = new LinkedList<>();
		List<Id<LspShipment>> lspShipments = new LinkedList<>();
		List<Id<LspShipment>> plansWithoutShipments = new LinkedList<>();

		for (LSP lsp : lsps.getLSPs().values()) {
			//store all plan Ids in shipmentPlans
			for (LspShipmentPlan shipmentSelectedPlan : lsp.getSelectedPlan().getShipmentPlans()) {
				shipmentPlans.add(shipmentSelectedPlan.getLspShipmentId());
			}
			//store all shipment Ids in shipments
			for (LspShipment lspShipment : lsp.getLspShipments()) {
				lspShipments.add(lspShipment.getId());
			}

			for (Id<LspShipment> shipmentPlanId : shipmentPlans) {
				if (!lspShipments.contains(shipmentPlanId)) {
					plansWithoutShipments.add(shipmentPlanId);
					log.log(level, "LSP {}: ShipmentPlan {}  does not have a corresponding shipment.",lsp.getId().toString(),  shipmentPlanId );
				}
			}
		}

		if (!plansWithoutShipments.isEmpty()) {
			log.log(level, "ShipmentPlan(s) without a matching shipment: {}", plansWithoutShipments);
			return CheckResult.CHECK_FAILED;
		} else {
//			log.debug(("All shipment shipmentPlans have a corresponding shipment.");
			return CheckResult.CHECK_SUCCESSFUL;
		}
	}
	/**
	 * this method will check if every shipmentPlan has got a shipment (all plans)
	 *
	 * @param lsps the LSPs to check
	 * @param level  level of log messages / errors
	 * @return CheckResult
	 */
	/*package-private*/ static CheckResult shipmentForEveryShipmentPlanAllPlans(LSPs lsps, Level level) {
		List<Id<LspShipment>> shipmentPlanIds = new LinkedList<>();
		List<Id<LspShipment>> shipmentIds = new LinkedList<>();
		List<Id<LspShipment>> plansWithoutShipments = new LinkedList<>();

		for (LSP lsp : lsps.getLSPs().values()) {
			//for all plans
			for (LSPPlan lspPlan : lsp.getPlans()) {
				//store all plan Ids in plans
				for (LspShipmentPlan shipmentPlan : lspPlan.getShipmentPlans()) {
					shipmentPlanIds.add(shipmentPlan.getLspShipmentId());
				}
			}
			//store all shipment Ids in shipments
			for (LspShipment lspShipment : lsp.getLspShipments()) {
				shipmentIds.add(lspShipment.getId());
			}

			for (Id<LspShipment> shipmentPlanId : shipmentPlanIds) {
				if (!shipmentIds.contains(shipmentPlanId)) {
					plansWithoutShipments.add(shipmentPlanId);
					log.log(level, "LSP {}: ShipmentPlan {} does not have a corresponding shipment.", lsp.getId().toString(), shipmentPlanId);
				}
			}
		}

		if (!plansWithoutShipments.isEmpty()) {
			log.log(level, "ShipmentPlan(s) without a matching shipment: {}", plansWithoutShipments);
			return CheckResult.CHECK_FAILED;
		} else {
//			log.debug("All shipment plans have a corresponding shipment.");
			return CheckResult.CHECK_SUCCESSFUL;
		}
	}
}
