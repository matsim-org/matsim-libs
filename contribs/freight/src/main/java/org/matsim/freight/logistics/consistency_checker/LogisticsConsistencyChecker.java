package org.matsim.freight.logistics.consistency_checker;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.matsim.api.core.v01.Id;
import org.matsim.freight.logistics.LSP;
import org.matsim.freight.logistics.LSPPlan;
import org.matsim.freight.logistics.LSPResource;
import org.matsim.freight.logistics.LSPs;
import org.matsim.freight.logistics.shipment.LspShipment;
import org.matsim.freight.logistics.shipment.LspShipmentPlan;

import java.util.*;

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
	private static Level currentLevel;

	public static void setLogLevel(Level level) {
		currentLevel = level;
		Configurator.setLevel(log.getName(), level);
	}

	private static void logMessage(String msg, Object... params) {
		if (currentLevel == Level.WARN) {
			log.warn(msg, params);
		} else if (currentLevel == Level.ERROR) {
			log.error(msg, params);
		} else {
			log.info(msg, params);
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
		setLogLevel(lvl);
		//all resource ids are being saved in this list
		List<Id<LSPResource>> lspResourceList = new LinkedList<>();
		//true, as long as no resource id exists more than once
		boolean resourceIDsAreUnique = true;
		for (LSP lsp : lsps.getLSPs().values()) {
			for (LSPResource resource : lsp.getResources()) {
				//if a resource id is already in this list, the id exists more than once
				if (lspResourceList.contains(resource.getId())) {
					logMessage("Resource with ID '{}' exists more than once.", resource.getId());
					resourceIDsAreUnique = false;
				} else {
					lspResourceList.add(resource.getId());
				}
			}
		}
		//check is successful, if all resource ids are unique
		if (resourceIDsAreUnique) {
			logMessage("All resource IDs are unique.");
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

		setLogLevel(lvl);

		for (LSP lsp : lsps.getLSPs().values()) {
			for (LspShipment lspShipment : lsp.getLspShipments()) {
				lspShipmentsList.add(lspShipment.getId());
			}
			//for selected plan only
			for (LspShipmentPlan shipmentSelectedPlan : lsp.getSelectedPlan().getShipmentPlans()) {
				lspShipmentPlansList.add(shipmentSelectedPlan.getLspShipmentId());
			}

		}
		Set<Id<LspShipment>> plannedShipmentIds = new HashSet<>(lspShipmentPlansList);
		List<Id<LspShipment>> shipmentsWithoutPlan = new LinkedList<>();

		for (Id<LspShipment> shipmentId : lspShipmentsList) {
			if (!plannedShipmentIds.contains(shipmentId)) {
				shipmentsWithoutPlan.add(shipmentId);
			}
		}

		if (!shipmentsWithoutPlan.isEmpty()) {
			logMessage("Shipments without a plan: {}", shipmentsWithoutPlan);
			return CheckResult.CHECK_FAILED;
		} else {
			logMessage("All shipments have plans.");
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

		setLogLevel(lvl);

		for (LSP lsp : lsps.getLSPs().values()) {
			for (LspShipment lspShipment : lsp.getLspShipments()) {
				lspShipmentsList.add(lspShipment.getId());
				logMessage("Shipment ID "+lspShipment.getId().toString());
			}
			//for all plans
			for (LSPPlan lspPlan : lsp.getPlans()) {
				for (LspShipmentPlan shipmentPlan : lspPlan.getShipmentPlans()) {
					lspShipmentPlansList.add(shipmentPlan.getLspShipmentId());
					logMessage("ShipmentPlan ID: "+shipmentPlan.getLspShipmentId().toString());
				}
			}

		}
		Set<Id<LspShipment>> plannedShipmentIds = new HashSet<>(lspShipmentPlansList);
		List<Id<LspShipment>> shipmentsWithoutPlan = new LinkedList<>();

		for (Id<LspShipment> shipmentId : lspShipmentsList) {
			if (!plannedShipmentIds.contains(shipmentId)) {
				shipmentsWithoutPlan.add(shipmentId);
			}
		}

		if (!shipmentsWithoutPlan.isEmpty()) {
			logMessage("Shipments without a plan: {}", shipmentsWithoutPlan);
			return CheckResult.CHECK_FAILED;
		} else {
			logMessage("All shipments have plans.");
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
		// Alle ShipmentPlan-IDs werden in dieser Liste gespeichert
		List<Id<LspShipment>> lspShipmentPlansList = new LinkedList<>();
		List<Id<LspShipment>> lspShipmentsList = new LinkedList<>();

		setLogLevel(lvl);

		for (LSP lsp : lsps.getLSPs().values()) {
			for (LspShipment lspShipment : lsp.getLspShipments()) {
				lspShipmentsList.add(lspShipment.getId());
			}
			// selected plan only
			for (LspShipmentPlan shipmentSelectedPlan : lsp.getSelectedPlan().getShipmentPlans()) {
				lspShipmentPlansList.add(shipmentSelectedPlan.getLspShipmentId());
			}
		}
		Set<Id<LspShipment>> availableShipments = new HashSet<>(lspShipmentsList);
		List<Id<LspShipment>> plansWithoutShipments = new LinkedList<>();

		logMessage(availableShipments.toString());
		for (Id<LspShipment> shipmentPlanId : lspShipmentPlansList) {
			logMessage("ShipmentPlan ID: "+shipmentPlanId.toString());
			if (!availableShipments.contains(shipmentPlanId)) {
				plansWithoutShipments.add(shipmentPlanId);
			}
		}
		if (!plansWithoutShipments.isEmpty()) {
			logMessage("ShipmentPlans without a matching shipment: {}", plansWithoutShipments);
			return CheckResult.CHECK_FAILED;
		} else {
			logMessage("All shipment plans have a corresponding shipment.");
			return CheckResult.CHECK_SUCCESSFUL;
		}
	}

}
