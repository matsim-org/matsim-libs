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
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.utils.io.IOUtils;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
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
		createLookupTables(
				emissionConfigGroup.getAverageWarmEmissionFactorsFileURL(scenario.getConfig().getContext()),
				emissionConfigGroup.getAverageColdEmissionFactorsFileURL(scenario.getConfig().getContext()),
				emissionConfigGroup.getDetailedColdEmissionFactorsFileURL(scenario.getConfig().getContext()),
				emissionConfigGroup.getDetailedWarmEmissionFactorsFileURL(scenario.getConfig().getContext()));

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

	private void createLookupTables(URL averageFleetWarmEmissionFactorsFile, URL averageFleetColdEmissionFactorsFile,
									URL detailedWarmEmissionFactorsFile, URL detailedColdEmissionFactorsFile) {
		logger.info("entering createLookupTables");

		if (shouldCreateAverageTables()) {
			this.avgHbefaColdTable = new HbefaAvarageColdTableLoader().load(averageFleetColdEmissionFactorsFile);
			this.avgHbefaWarmTable = new HbefaAverageWarmTableLoader().load(averageFleetWarmEmissionFactorsFile);
		}
		if (shouldCreateDetailedTables()) {
			this.detailedHbefaColdTable = new HbefaDetailedColdTableLoader().load(detailedColdEmissionFactorsFile);
			this.detailedHbefaWarmTable = new HbefaDetailedWarmTableLoader().load(detailedWarmEmissionFactorsFile);
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

	private boolean shouldCreateAverageTables() {
		switch (emissionConfigGroup.getDetailedVsAverageLookupBehavior()) {
			case tryDetailedThenTechnologyAverageThenAverageTable:
			case directlyTryAverageTable:
				return true;
			default:
				return true;
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

	static class HbefaDetailedWarmTableLoader extends HbefaAverageWarmTableLoader {

		@Override
		protected HbefaWarmEmissionFactorKey createKey(CSVRecord record) {

			var key = super.createKey(record);
			super.setCommonDetailedParametersOnKey(key, record);
			return key;
		}
	}

	static class HbefaAverageWarmTableLoader extends HbefaTableLoader<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> {

		private static HbefaTrafficSituation mapString2HbefaTrafficSituation(String string) {

			if (string.endsWith("Freeflow")) return HbefaTrafficSituation.FREEFLOW;
			else if (string.endsWith("Heavy")) return HbefaTrafficSituation.HEAVY;
			else if (string.endsWith("Satur.")) return HbefaTrafficSituation.SATURATED;
			else if (string.endsWith("St+Go")) return HbefaTrafficSituation.STOPANDGO;
			else if (string.endsWith("St+Go2")) return HbefaTrafficSituation.STOPANDGO_HEAVY;
			else {
				logger.warn("Could not map String " + string + " to any HbefaTrafficSituation; please check syntax in hbefa input file.");
				throw new RuntimeException();
			}
		}

		@Override
		protected HbefaWarmEmissionFactorKey createKey(CSVRecord record) {

			var key = new HbefaWarmEmissionFactorKey();
			setCommonParametersOnKey(key, record);
			var trafficSit = record.get("TrafficSit");
			key.setRoadCategory(trafficSit.substring(0, trafficSit.lastIndexOf('/')));
			key.setTrafficSituation(mapString2HbefaTrafficSituation(trafficSit));
			key.setVehicleAttributes(new HbefaVehicleAttributes());
			return key;
		}

		@Override
		protected HbefaWarmEmissionFactor createValue(CSVRecord record) {

			var factor = Double.parseDouble(record.get("EFA_weighted"));
			var speed = Double.parseDouble(record.get("V_weighted"));
			return new HbefaWarmEmissionFactor(factor, speed);
		}
	}

	static class HbefaAvarageColdTableLoader extends HbefaTableLoader<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> {

		@Override
		protected HbefaColdEmissionFactorKey createKey(CSVRecord record) {

			var key = new HbefaColdEmissionFactorKey();
			setCommonParametersOnKey(key, record);
			key.setParkingTime(mapAmbientCondPattern2ParkingTime(record.get("AmbientCondPattern")));
			key.setDistance(mapAmbientCondPattern2Distance(record.get("AmbientCondPattern")));
			key.setVehicleAttributes(new HbefaVehicleAttributes());
			return key;
		}

		@Override
		protected HbefaColdEmissionFactor createValue(CSVRecord record) {
			return new HbefaColdEmissionFactor(Double.parseDouble(record.get("EFA_weighted")));
		}

		private int mapAmbientCondPattern2Distance(String string) {
			int distance;
			String distanceString = string.split(",")[2];
			String upperbound = distanceString.split("-")[1];
			distance = Integer.parseInt(upperbound.split("k")[0]);
			return distance;
		}

		private int mapAmbientCondPattern2ParkingTime(String string) {
			try {
				int parkingTime;
				String parkingTimeString = string.split(",")[1];
				if (parkingTimeString.equals(">12h")) {
					parkingTime = 13;
				} else {
					String upperbound = parkingTimeString.split("-")[1];
					parkingTime = Integer.parseInt(upperbound.split("h")[0]);
				}
				return parkingTime;
			} catch (Exception e) {
				logger.info("");
			}
			return 1;

		}
	}

	static class HbefaDetailedColdTableLoader extends HbefaAvarageColdTableLoader {

		@Override
		protected HbefaColdEmissionFactorKey createKey(CSVRecord record) {

			var key = super.createKey(record);
			setCommonDetailedParametersOnKey(key, record);
			return key;
		}
	}

	abstract static class HbefaTableLoader<K extends HbefaEmissionFactorKey, V extends HbefaEmissionFactor> {

		Map<K, V> result = new HashMap<>();

		Map<K, V> load(URL file) {
			try (var reader = IOUtils.getBufferedReader(file);
				 var parser = CSVParser.parse(reader, CSVFormat.newFormat(';').withFirstRecordAsHeader())) {

				for (var record : parser) {

					var key = createKey(record);
					var value = createValue(record);
					result.put(key, value);

				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return result;
		}

		protected void setCommonParametersOnKey(K key, CSVRecord record) {

			key.setComponent(EmissionUtils.getPollutant(record.get("Component")));
			key.setVehicleCategory(EmissionUtils.mapString2HbefaVehicleCategory(record.get("VehCat")));
		}

		// this violates some principle but I don't care
		protected void setCommonDetailedParametersOnKey(K key, CSVRecord record) {
			key.getVehicleAttributes().setHbefaTechnology(record.get("Technology"));
			key.getVehicleAttributes().setHbefaEmConcept(record.get("EmConcept"));
			key.getVehicleAttributes().setHbefaSizeClass(record.get("SizeClasse"));
		}

		protected abstract K createKey(CSVRecord record);

		protected abstract V createValue(CSVRecord record);
	}
}
