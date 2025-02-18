package org.matsim.freight.logistics.consistency_checker;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.matsim.freight.carriers.Carriers;
import org.matsim.freight.carriers.consistency_checkers.CarrierConsistencyCheckers;
import org.matsim.freight.logistics.LSP;
import org.matsim.freight.logistics.LSPPlan;
import org.matsim.freight.logistics.LSPResource;
import org.matsim.freight.logistics.LSPs;
import org.matsim.freight.logistics.shipment.LspShipment;
import org.matsim.freight.logistics.shipment.LspShipmentPlan;

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
	 * this method will run through allJobsInToursCheck to check, if every job is part of only one tour
	 * allJobsInToursCheck returns enum CheckResult:
	 * CHECK_SUCCESSFUL if allJobsInToursCheck returns CHECK_SUCCESSFUL
	 * CHECK_FAILED if allJobsInToursCheck returns CHECK_FAIL
	 * @param lsps the LSPs to check
	 * @param lvl level of log messages / errors
	 * @return CheckResult
	 */
	public static CheckResult uniqueResourcesCheck(LSPs lsps, Level lvl) {
		setLogLevel(lvl);
		int checkFailed = 0;
		if (resourcesAreUnique(lsps, lvl) == CheckResult.CHECK_FAILED) {
			checkFailed++;
		}
		return CheckResult.CHECK_FAILED;
	}

	private static LogisticsConsitencyChecker.CheckResult resourcesAreUnique(LSPs lsps, Level lvl) {
		var result =  CheckResult.CHECK_FAILED; //TODO...

		for (LSP lsp : lsps.getLSPs().values()) {
			for (LSPResource resource : lsp.getResources()) {
				resource.getId(); //TODO...
			}
		}

		return result;
	}

	private static LogisticsConsitencyChecker.CheckResult shipmentsArePlannedExactlyOnce(LSPs lsps, Level lvl) {
		var result =  CheckResult.CHECK_FAILED; //TODO...

		for (LSP lsp : lsps.getLSPs().values()) {
			for (LspShipment lspShipment : lsp.getLspShipments()) {
				//TODO... das sind die Aufträge
			}
			for (LSPPlan lspPlan : lsp.getPlans()) { //Alle Pläne
				for (LspShipmentPlan shipmentPlan : lspPlan.getShipmentPlans()) {
					shipmentPlan.getLspShipmentId(); //Tada :)
				}
			}
			lsp.getSelectedPlan(); //Nur selected Plan :)
		}

		return result;
	}

}
