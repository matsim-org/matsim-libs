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
	 * this method will check if all resources are assigned to exactly one plan
	 *
	 * @param lsps the LSPs to check
	 * @param lvl  level of log messages / errors
	 * @return CheckResult
	 */
	public static CheckResult shipmentsArePlannedExactlyOnceSelectedPlanOnly(LSPs lsps, Level lvl) {
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
				//Todo: @KMT: Hier wird immer nur 1x shipmentNorth ausgegeben, obwohl es in der XML zweimal auftaucht...
				//@ Anton: Im selectedPlan (--> selected=True) ist doch jedes nur einmal drin. (lsps.xml) Folglich sieht das für mich an der Stelle richtig aus.
				logMessage(shipmentSelectedPlan.getLspShipmentId().toString());
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
	 *
	 */
	public static CheckResult shipmentsArePlannedExactlyOnceAllPlans(LSPs lsps, Level lvl) {
		setLogLevel(lvl);
		for (LSP lsp : lsps.getLSPs().values()) {
			for (LspShipment lspShipment : lsp.getLspShipments()) {
				//TODO: Hier fehlt noch die Implementierung
			}
			for (LSPPlan lspPlan : lsp.getPlans()) { //Alle Pläne
				for (LspShipmentPlan shipmentPlan : lspPlan.getShipmentPlans()) {
					//TODO: Hier fehlt noch die Implementierung
				}
			}
		}
		return null;
	}
}
