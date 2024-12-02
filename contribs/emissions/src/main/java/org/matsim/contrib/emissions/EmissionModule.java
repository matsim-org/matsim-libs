/* *********************************************************************** *
 * project: org.matsim.*
 * EmissionHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package org.matsim.contrib.emissions;

import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author benjamin
 *
 */
public final class EmissionModule {
	private static final Logger logger = LogManager.getLogger(EmissionModule.class);

	private final Scenario scenario;
	private WarmEmissionHandler warmEmissionHandler;
	private ColdEmissionHandler coldEmissionHandler;

	private final EventsManager eventsManager;
	private final EmissionsConfigGroup emissionConfigGroup;

	//===

	private Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> avgHbefaWarmTable;
	private Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> avgHbefaColdTable;

	private Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> detailedHbefaWarmTable;
	private Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> detailedHbefaColdTable;

	private Map<HbefaRoadVehicleCategoryKey, Map<HbefaTrafficSituation, Double>> hbefaRoadTrafficSpeeds;

	private final Set<Pollutant> warmPollutants = new HashSet<>();
	private final Set<Pollutant> coldPollutants = new HashSet<>();
	// these are/were the "automatic" maps collected by JM from the hbefa files.  kai, jan'20

	@Inject
	public EmissionModule(final Scenario scenario, final EventsManager eventsManager) {

		this.scenario = scenario;
		this.emissionConfigGroup = ConfigUtils.addOrGetModule(scenario.getConfig(), EmissionsConfigGroup.class);
		this.eventsManager = emissionConfigGroup.isWritingEmissionsEvents() ? eventsManager : EventsUtils.createEventsManager();

		checkConfigConsistency();
		checkNetworkConsistency();

		createLookupTables();
		createEmissionHandlers();

		// Event handlers are now added to the event manager inside the respective Handlers, jm march '18
	}

	@SuppressWarnings("ClassEscapesDefinedScope")
	public WarmEmissionAnalysisModule getWarmEmissionAnalysisModule() {
		// makes sense to have this public for externalization computations.  kai, jan'20
		return this.warmEmissionHandler.getWarmEmissionAnalysisModule();
	}

	@SuppressWarnings("ClassEscapesDefinedScope")
	public ColdEmissionAnalysisModule getColdEmissionAnalysisModule() {
		// makes sense to have this public for externalization computations.  kai, jan'20
		return this.coldEmissionHandler.getColdEmissionAnalysisModule();
	}

	// make these available for position emission module
	Set<Pollutant> getWarmPollutants() {
		return warmPollutants;
	}

	Set<Pollutant> getColdPollutants() {
		return coldPollutants;
	}

	// probably, this is useful; e.g., emission events are not written and a few handlers must be attached to events manager
	// for the analysis purpose. Need a test. Amit Apr'17
	public EventsManager getEmissionEventsManager() {
		return eventsManager;
	}

	public void writeEmissionInformation() {
		logger.info("Warm emissions were not calculated for {} of {} link leave events (no corresponding link enter event).", warmEmissionHandler.getLinkLeaveWarnCnt(), warmEmissionHandler.getLinkLeaveCnt());
		int noVehicleLeavesTrafficEmissions = warmEmissionHandler.getSameLinkTrafficLeaveWarnCnt() + warmEmissionHandler.getUnusualTrafficLeaveWarnCnt();
		logger.info("Warm emissions were not calculated for {} of {} vehicle leaves traffic events (no corresponding link enter event).", noVehicleLeavesTrafficEmissions, warmEmissionHandler.getTrafficLeaveCnt());
		if ( warmEmissionHandler.getUnusualTrafficLeaveWarnCnt() > 0 ) {
			logger.info("{} events occurred where the vehicle left traffic without entering ANY link (no warm emissions calculated). These events might need to be investigated.", warmEmissionHandler.getUnusualTrafficLeaveWarnCnt()); }

		WarmEmissionAnalysisModule wam = warmEmissionHandler.getWarmEmissionAnalysisModule();

		logger.info("Emission calculation based on `Free flow only' occurred for {} of {} warm emission events.", wam.getFreeFlowOccurences(), wam.getWarmEmissionEventCounter());
		logger.info("Emission calculation based on `Stop&Go only' occurred for {} of {} warm emission events.", wam.getStopGoOccurences(), wam.getWarmEmissionEventCounter());
		logger.info("Emission calculation based on `Fractions' occurred for {} of {} warm emission events.", wam.getFractionOccurences(), wam.getWarmEmissionEventCounter());
		logger.info("Free flow occurred on {} km of total {} km, where emissions were calculated.", wam.getFreeFlowKmCounter(), wam.getKmCounter());
		logger.info("Stop&Go occurred on {} km of total {} km, where emissions were calculated.", wam.getStopGoKmCounter(), wam.getKmCounter());
		logger.info("Emission calculation terminated. Emission events can be found in regular events file.");
	}

	private void checkConfigConsistency() {

		if (shouldCreateDetailedTables()) {
			//Check if value was loaded
			if (StringUtils.isBlank(emissionConfigGroup.getDetailedColdEmissionFactorsFile())) {
				throw new RuntimeException("You have requested " + emissionConfigGroup.getDetailedVsAverageLookupBehavior() + " but are not providing a corresponding" +
						" cold emissions file.");
			}
			if (StringUtils.isBlank(emissionConfigGroup.getDetailedWarmEmissionFactorsFile())) {
				throw new RuntimeException("You have requested " + emissionConfigGroup.getDetailedVsAverageLookupBehavior() + " but are not providing a corresponding" +
						" warm emissions file.");
			}
		} else if (shouldCreateAverageTables()) {
			if (StringUtils.isBlank(emissionConfigGroup.getAverageColdEmissionFactorsFile())) {
				throw new RuntimeException("You have requested " + emissionConfigGroup.getDetailedVsAverageLookupBehavior() + " but are not providing a corresponding" +
						" cold emissions file.");
			}
			if (StringUtils.isBlank(emissionConfigGroup.getAverageWarmEmissionFactorsFile())) {
				throw new RuntimeException("You have requested " + emissionConfigGroup.getDetailedVsAverageLookupBehavior() + " but are not providing a corresponding" +
						" warm emissions file.");
			}
		} else {
			throw new RuntimeException("This should not happen. Check your emission config.");
		}
	}

	private void checkNetworkConsistency() {

		var allMatch = scenario.getNetwork().getLinks().values().parallelStream()
				.map(link -> link.getAttributes().getAttribute(EmissionUtils.HBEFA_ROAD_TYPE))
				.allMatch(roadType -> roadType instanceof String);

		if (!allMatch)
			throw new RuntimeException("The Emission Contrib expects HbefaRoadTypes to be set as link attributes. " +
					"Use EmissionUtils.setHbefaRoadType(link, type) to set HbefaRoadTypes on each link. " +
					"Alternatively use an existing mapper such as OsmHbefaMapping, VisumHbefaRoadTypeMapping or VspHbefaRoadTypeMapping to set HbefaRoadTypes on your network.");
	}

	private void createLookupTables() {
		logger.info("entering createLookupTables");

		if (shouldCreateAverageTables()) {
			this.avgHbefaColdTable = HbefaTables.loadAverageCold(emissionConfigGroup.getAverageColdEmissionFactorsFileURL(scenario.getConfig().getContext()));
			addPollutantsToMap(coldPollutants, avgHbefaColdTable.keySet());
			// yy The naming and signature of the above should presumably be changed:
			// (1) addPollutantsToX implies signature (pollutants,X). But it is actually the other way round (even if it does not read that way.)
			// (2) "coldPollutants" is not a Map.
			// Since this is a private method, maybe one could also have a method "memorizeColdPollutants" and then not have coldPollutants as
			// field. kai, dec'22

			this.avgHbefaWarmTable = HbefaTables.loadAverageWarm(emissionConfigGroup.getAverageWarmEmissionFactorsFileURL(scenario.getConfig().getContext()));
			addPollutantsToMap(warmPollutants, avgHbefaWarmTable.keySet());
		}
		if (shouldCreateDetailedTables()) {
			this.detailedHbefaColdTable = HbefaTables.loadDetailedCold(emissionConfigGroup.getDetailedColdEmissionFactorsFileURL(scenario.getConfig().getContext()));
			addPollutantsToMap(coldPollutants, detailedHbefaColdTable.keySet());
			this.detailedHbefaWarmTable = HbefaTables.loadDetailedWarm(emissionConfigGroup.getDetailedWarmEmissionFactorsFileURL(scenario.getConfig().getContext()));
			addPollutantsToMap(warmPollutants, detailedHbefaWarmTable.keySet());
		}

		logger.info("leaving createLookupTables");

		//create HBEFA Speed tables. try on detailed values first.
		if (detailedHbefaWarmTable != null) {
			hbefaRoadTrafficSpeeds = EmissionUtils.createHBEFASpeedsTable(detailedHbefaWarmTable);
		} else if (avgHbefaWarmTable != null) {
			hbefaRoadTrafficSpeeds = EmissionUtils.createHBEFASpeedsTable(avgHbefaWarmTable);
		} else {
			throw new RuntimeException("hbefaRoadTrafficSpeed table not created");        //Is table mandatory? -> If yes throw exception
		}
	}

	private void addPollutantsToMap(Set<Pollutant> addTo, Set<? extends HbefaEmissionFactorKey> keys) {
		for (HbefaEmissionFactorKey key : keys) {
			addTo.add(key.getComponent());
		}
	}

	private boolean shouldCreateAverageTables() {
		return switch (emissionConfigGroup.getDetailedVsAverageLookupBehavior()) {
			case tryDetailedThenTechnologyAverageThenAverageTable, directlyTryAverageTable -> true;
			default -> false;
		};
	}

	private boolean shouldCreateDetailedTables() {
		return switch (emissionConfigGroup.getDetailedVsAverageLookupBehavior()) {
			case onlyTryDetailedElseAbort, tryDetailedThenTechnologyAverageElseAbort, tryDetailedThenTechnologyAverageThenAverageTable -> true;
			default -> false;
		};
	}

	private void createEmissionHandlers() {
		logger.info("entering createEmissionHandlers");

		warmEmissionHandler = new WarmEmissionHandler(scenario, avgHbefaWarmTable, detailedHbefaWarmTable, hbefaRoadTrafficSpeeds, warmPollutants, eventsManager);

		coldEmissionHandler = new ColdEmissionHandler(scenario, avgHbefaColdTable, detailedHbefaColdTable, coldPollutants, eventsManager);
		// this initiates all cold emissions processing!

		logger.info("leaving createEmissionHandlers");
	}
}
