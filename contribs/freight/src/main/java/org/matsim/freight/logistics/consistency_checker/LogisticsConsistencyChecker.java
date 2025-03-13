package org.matsim.freight.logistics.consistency_checker;

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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author anton stock
 * this class provides tests to check if resource IDs are unique and if all existing shipments are assigned to exactly one plan.
 */
public class LogisticsConsistencyChecker {

	public enum CheckResult {
		CHECK_SUCCESSFUL, CHECK_FAILED, ERROR
	}

	//needed for log messages, log level etc.
	private static final Logger log = LogManager.getLogger(LogisticsConsistencyChecker.class);

	public static Level setInternalLogLevel(Level level) {
		if (level == Level.FATAL) {
			return Level.ERROR;
		} else {
			return level;
		}
	}

	/**
	 * this method will check if all existing resource IDs are unique.
	 *
	 * @param lsps the LSPs to check
	 * @param lvl  level of log messages / errors
	 * @return CheckResult
	 */
	public static CheckResult resourcesAreUnique(LSPs lsps, Level lvl) {
		Level level = setInternalLogLevel(lvl);
		//all resource ids are being saved in this list
		List<Id<LSPResource>> lspResourceList = new LinkedList<>();
		//true, as long as no resource id exists more than once
		boolean resourceIDsAreUnique = true;
		for (LSP lsp : lsps.getLSPs().values()) {
			for (LSPResource resource : lsp.getResources()) {
				//if a resource id is already in this list, the id exists more than once
				if (lspResourceList.contains(resource.getId())) {
					log.log(level, "Resource with Id:'{}' exists more than once.", resource.getId());
					resourceIDsAreUnique = false;
				} else {
					lspResourceList.add(resource.getId());
				}
			}
		}
		//check is successful, if all resource ids are unique
		if (resourceIDsAreUnique) {
			log.log(level, "All resource IDs are unique.");
			return CheckResult.CHECK_SUCCESSFUL;
		} else {
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
	public static CheckResult shipmentPlanForEveryShipmentSelectedPlanOnly(LSPs lsps, Level lvl) {
		//all resource ids are being saved in this list
		List<Id<LspShipment>> lspShipmentPlansList = new LinkedList<>();
		List<Id<LspShipment>> lspShipmentsList = new LinkedList<>();
		List<Id<LspShipment>> shipmentsWithoutPlan = new LinkedList<>();
		Level level = setInternalLogLevel(lvl);

		for (LSP lsp : lsps.getLSPs().values()) {
			for (LspShipment lspShipment : lsp.getLspShipments()) {
				lspShipmentsList.add(lspShipment.getId());
				log.log(level,lspShipment.getId().toString());
			}
			//for selected plan only
			for (LspShipmentPlan shipmentSelectedPlan : lsp.getSelectedPlan().getShipmentPlans()) {
				lspShipmentPlansList.add(shipmentSelectedPlan.getLspShipmentId());
			}

			Set<Id<LspShipment>> plannedShipmentIds = new HashSet<>(lspShipmentPlansList);
			for (Id<LspShipment> shipmentId : lspShipmentsList) {
				if (!plannedShipmentIds.contains(shipmentId)) {
					shipmentsWithoutPlan.add(shipmentId);
				}
			}
		}
		if (!shipmentsWithoutPlan.isEmpty()) {
			log.log(level, "Shipments without a plan: {}", shipmentsWithoutPlan);
			return CheckResult.CHECK_FAILED;
		} else {
			log.log(level, "All shipments have plans.");
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
	public static CheckResult shipmentPlanForEveryShipmentAllPlans(LSPs lsps, Level lvl) {
		//all resource ids are being saved in this list
		List<Id<LspShipment>> lspShipmentPlansList = new LinkedList<>();
		List<Id<LspShipment>> lspShipmentsList = new LinkedList<>();
		List<Id<LspShipment>> shipmentsWithoutPlan = new LinkedList<>();
		Level level = setInternalLogLevel(lvl);

		for (LSP lsp : lsps.getLSPs().values()) {
			for (LspShipment lspShipment : lsp.getLspShipments()) {
				lspShipmentsList.add(lspShipment.getId());
				log.log(level, "Shipment Id:{} ", lspShipment.getId().toString());
			}
			//for all plans
			for (LSPPlan lspPlan : lsp.getPlans()) {
				for (LspShipmentPlan shipmentPlan : lspPlan.getShipmentPlans()) {
					lspShipmentPlansList.add(shipmentPlan.getLspShipmentId());
					log.log(level, "ShipmentPlan Id: {}", shipmentPlan.getLspShipmentId().toString());
				}
			}
			Set<Id<LspShipment>> plannedShipmentIds = new HashSet<>(lspShipmentPlansList);
			for (Id<LspShipment> shipmentId : lspShipmentsList) {
				if (!plannedShipmentIds.contains(shipmentId)) {
					shipmentsWithoutPlan.add(shipmentId);
				}
			}
		}
		if (!shipmentsWithoutPlan.isEmpty()) {
			log.log(level, "Shipments without a plan: {}", shipmentsWithoutPlan);
			return CheckResult.CHECK_FAILED;
		} else {
			log.log(level, "All shipments have plans.");
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
	public static CheckResult shipmentForEveryShipmentPlanSelectedPlanOnly(LSPs lsps, Level lvl) {
		//
		List<Id<LspShipment>> plansWithoutShipments = new LinkedList<>();
		Level level = setInternalLogLevel(lvl);

		for (LSP lsp : lsps.getLSPs().values()) {
			for (LspShipmentPlan shipmentSelectedPlan : lsp.getSelectedPlan().getShipmentPlans()) {
				log.log(level, "Plan: {}", shipmentSelectedPlan.getLspShipmentId().toString());
			}
		}

		//TODO: @Anton: Ich habe in der Testmethode nun poer Hand einen ShipmentPlan eingefügt, der hoffentlich das macht, was du hier testen willst.
		// Folglich könntest du dich mMn nun hier an eine Implentierung wagen. :)


		if (!plansWithoutShipments.isEmpty()) {
			log.log(level, "ShipmentPlan without a matching shipment: {}", plansWithoutShipments);
			return CheckResult.CHECK_FAILED;
		} else {
			log.log(level, "All shipment plans have a corresponding shipment.");
			return CheckResult.CHECK_SUCCESSFUL;
		}
	}
}
