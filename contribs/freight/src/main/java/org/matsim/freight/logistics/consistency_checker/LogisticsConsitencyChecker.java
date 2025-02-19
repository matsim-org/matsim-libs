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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
	* @author anton stock
	 * this class provides tests to check if resource IDs are unique and if all existing shipments are assigned to exactly one plan.
	 */
public class LogisticsConsitencyChecker {

	public enum CheckResult {
		CHECK_SUCCESSFUL, CHECK_FAILED, ERROR
	}

	//needed for log messages, log level etc.
	private static final Logger log = LogManager.getLogger(LogisticsConsitencyChecker.class);
	private static Level currentLevel;
	public static void setLogLevel(Level level) {
		currentLevel = level;
		Configurator.setLevel(log.getName(), level);
	}

	private static void logMessage(String msg, Object... params) {
		if (currentLevel==Level.WARN){
			log.warn(msg, params);
		} else if (currentLevel==Level.ERROR) {
			log.error(msg, params);
		} else {
			log.info(msg, params);
		}
	}

	/**
	 *	this method will check if all existing resource IDs are unique.
	 * @param lsps the LSPs to check
	 * @param lvl level of log messages / errors
	 * @return CheckResult
	 */
	public static CheckResult resourcesAreUnique(LSPs lsps, Level lvl) {
		setLogLevel(lvl);
		//all resource ids are being saved in this list
		List<Id> lspResourceList = new LinkedList<Id>();
		//true, as long as no resource id exists more than once
		boolean resourceIDsAreUnique = true;
		for (LSP lsp : lsps.getLSPs().values()) {
			for (LSPResource resource : lsp.getResources()) {
				//if a resource id is already in this list, the id exists more than once
				if(lspResourceList.contains(resource.getId())){
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
		 *	this method will check if all resources are assigned to exactly one plan
		 * @param lsps the LSPs to check
		 * @param lvl level of log messages / errors
		 * @return CheckResult
		 */
	public static CheckResult shipmentsArePlannedExactlyOnce(LSPs lsps, Level lvl) {
		var result =  CheckResult.CHECK_FAILED; //TODO...
		//all resource ids are being saved in this list
		List<Id> lspShipmentsList = new LinkedList<Id>();

		List<Id> lspPlansList = new LinkedList<Id>();
		//
		boolean allShipmentsHavePlans = true;
		for (LSP lsp : lsps.getLSPs().values()) {
			for (LspShipment lspShipment : lsp.getLspShipments()) {
				lspShipmentsList.add(lspShipment.getId());
			}
			//for (LSPPlan lspPlan : lsp.getPlans()) { //Alle Pläne
				//for (LspShipmentPlan shipmentPlan : lspPlan.getShipmentPlans()) {
				//	lspPlansList.add(shipmentPlan.getLspShipmentId());
				//}
			//}
			for (LspShipmentPlan shipmentSelectedPlan : lsp.getSelectedPlan().getShipmentPlans()) {
				lspPlansList.add(shipmentSelectedPlan.getLspShipmentId());
			}
		}

		Set<Id> shipmentHasNoPlan = new HashSet<>(lspShipmentsList);
		shipmentHasNoPlan.removeAll(lspPlansList);

		if(shipmentHasNoPlan.isEmpty()){
			logMessage("All shipments have a plan.");
			return CheckResult.CHECK_SUCCESSFUL;
		} else {
			//@KMT: Ich verstehe leider nicht, wieso die Ausgabe nicht funktioniert... egal ob .size oder .toString, es kommt einfach nichts nach dem Doppelpunkt :(
			logMessage("The following shipments have no plan: ", shipmentHasNoPlan);
			return CheckResult.CHECK_FAILED;
		}
	}

}
