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
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;

import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author benjamin
 *
 */
public final class EmissionModule {
	private static final Logger logger = Logger.getLogger(EmissionModule.class);
	
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
		this.emissionConfigGroup = (EmissionsConfigGroup) scenario.getConfig().getModules().get(EmissionsConfigGroup.GROUP_NAME);
		this.eventsManager = emissionConfigGroup.isWritingEmissionsEvents() ? eventsManager : EventsUtils.createEventsManager();

		checkConfigConsistency();
		//TODO: create roadtype mapping here from config
		createLookupTables();
		createEmissionHandlers();

		// Event handlers are now added to the event manager inside the respective Handlers, jm march '18
	}

	public WarmEmissionAnalysisModule getWarmEmissionAnalysisModule() {
		// makes sense to have this public for externalization computations.  kai, jan'20
		return this.warmEmissionHandler.getWarmEmissionAnalysisModule();
	}

	public ColdEmissionAnalysisModule getColdEmissionAnalysisModule() {
		// makes sense to have this public for externalization computations.  kai, jan'20
		return this.coldEmissionHandler.getColdEmissionAnalysisModule();
	}

	// probably, this is useful; e.g., emission events are not written and a few handlers must be attached to events manager
	// for the analysis purpose. Need a test. Amit Apr'17
	public EventsManager getEmissionEventsManager() {
		return eventsManager;
	}

	public void writeEmissionInformation() {
		logger.info("Warm emissions were not calculated for " + warmEmissionHandler.getLinkLeaveWarnCnt() + " of " +
				warmEmissionHandler.getLinkLeaveCnt() + " link leave events (no corresponding link enter event).");

		WarmEmissionAnalysisModule wam = warmEmissionHandler.getWarmEmissionAnalysisModule();

		logger.info("Emission calculation based on `Free flow only' occured for " + wam.getFreeFlowOccurences() + " of " +
				wam.getWarmEmissionEventCounter() + " warm emission events.");
		logger.info("Emission calculation based on `Stop&Go only' occured for " + wam.getStopGoOccurences() + " of " +
				wam.getWarmEmissionEventCounter() + " warm emission events.");
		logger.info("Emission calculation based on `Fractions' occured for " + wam.getFractionOccurences() + " of " +
				wam.getWarmEmissionEventCounter() + " warm emission events.");
		logger.info("Free flow occured on " + wam.getFreeFlowKmCounter() + " km of total " +
				wam.getKmCounter() + " km, where emissions were calculated.");
		logger.info("Stop&Go occured on " + wam.getStopGoKmCounter() + " km of total " +
				wam.getKmCounter() + " km, where emissions were calculated.");
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

	private void createLookupTables() {
		logger.info("entering createLookupTables");

		if (shouldCreateAverageTables()) {
			this.avgHbefaColdTable = HbefaTables.loadAverageCold(emissionConfigGroup.getAverageColdEmissionFactorsFileURL(scenario.getConfig().getContext()));
			addPollutantsToMap(coldPollutants, avgHbefaColdTable.keySet());
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
		switch (emissionConfigGroup.getDetailedVsAverageLookupBehavior()) {
			case tryDetailedThenTechnologyAverageThenAverageTable:
			case directlyTryAverageTable:
				return true;
			default:
				return false;
		}
	}

	private boolean shouldCreateDetailedTables() {
		switch (emissionConfigGroup.getDetailedVsAverageLookupBehavior()) {
			case onlyTryDetailedElseAbort:
			case tryDetailedThenTechnologyAverageElseAbort:
			case tryDetailedThenTechnologyAverageThenAverageTable:
				return true;
			default:
				return false;
		}
	}

	private void createEmissionHandlers() {
		logger.info("entering createEmissionHandlers");

		loadRoadTypeMappings();

		warmEmissionHandler = new WarmEmissionHandler(scenario, avgHbefaWarmTable, detailedHbefaWarmTable, hbefaRoadTrafficSpeeds, warmPollutants, eventsManager);

		coldEmissionHandler = new ColdEmissionHandler(scenario, avgHbefaColdTable, detailedHbefaColdTable, coldPollutants, eventsManager);
		// this initiates all cold emissions processing!

		logger.info("leaving createEmissionHandlers");
	}

	private void loadRoadTypeMappings() {
		HbefaRoadTypeMapping roadTypeMapping;

		switch (this.emissionConfigGroup.getHbefaRoadTypeSource()) {
			case fromOsm:
				logger.warn("It is recommended to directly set the HBEFA road types to link attributes and then chose HbefaRoadTypeSource: "+ EmissionsConfigGroup.HbefaRoadTypeSource.fromLinkAttributes );
				roadTypeMapping = new OsmHbefaMapping();
				roadTypeMapping.addHbefaMappings(scenario.getNetwork());
				break;
			case fromFile:
				logger.warn("It is recommended to directly set the HBEFA road types to link attributes and then chose HbefaRoadTypeSource: "+ EmissionsConfigGroup.HbefaRoadTypeSource.fromLinkAttributes );
				URL roadTypeMappingFile = this.emissionConfigGroup.getEmissionRoadTypeMappingFileURL(scenario.getConfig().getContext());
				roadTypeMapping = VisumHbefaRoadTypeMapping.createVisumRoadTypeMapping(roadTypeMappingFile);
				roadTypeMapping.addHbefaMappings(scenario.getNetwork());
				break;
			case fromLinkAttributes: //no need, road types are already there
				break;
			default:
				throw new RuntimeException(this.emissionConfigGroup.getHbefaRoadTypeSource() + " is not implemented.");
		}
	}
}
