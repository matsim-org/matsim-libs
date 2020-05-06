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
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.matsim.contrib.emissions.EmissionUtils.createIndexFromKey;

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

		this.emissionConfigGroup = (EmissionsConfigGroup) scenario.getConfig().getModules().get(EmissionsConfigGroup.GROUP_NAME );

		if ( !emissionConfigGroup.isWritingEmissionsEvents() ) {
			logger.warn("Emission events are excluded from events file. A new events manager is created.");
			this.eventsManager = EventsUtils.createEventsManager();
		} else {
			this.eventsManager = eventsManager;
		}

		URL context = scenario.getConfig().getContext();

		URL averageFleetWarmEmissionFactorsFile = null;
		URL averageFleetColdEmissionFactorsFile = null;
		URL detailedWarmEmissionFactorsFile = null;
		URL detailedColdEmissionFactorsFile = null;

		switch( this.emissionConfigGroup.getDetailedVsAverageLookupBehavior() ){
			case directlyTryAverageTable:
				if ( this.emissionConfigGroup.getAverageColdEmissionFactorsFile()==null || this.emissionConfigGroup.getAverageColdEmissionFactorsFile().equals( "" ) ) {
					throw new RuntimeException( "You have requested " + this.emissionConfigGroup.getDetailedVsAverageLookupBehavior() + " but are not providing a corresponding" +
										    " cold emissions file.") ;
				}
				if ( this.emissionConfigGroup.getAverageWarmEmissionFactorsFile()==null || this.emissionConfigGroup.getAverageWarmEmissionFactorsFile().equals( "" ) ) {
					throw new RuntimeException( "You have requested " + this.emissionConfigGroup.getDetailedVsAverageLookupBehavior() + " but are not providing a corresponding" +
										    " warm emissions file.") ;
				}
				averageFleetWarmEmissionFactorsFile = emissionConfigGroup.getAverageWarmEmissionFactorsFileURL( context );
				averageFleetColdEmissionFactorsFile = emissionConfigGroup.getAverageColdEmissionFactorsFileURL( context );
				break;
			case tryDetailedThenTechnologyAverageThenAverageTable:
				if ( this.emissionConfigGroup.getAverageColdEmissionFactorsFile()==null || this.emissionConfigGroup.getAverageColdEmissionFactorsFile().equals( "" ) ) {
					throw new RuntimeException( "You have requested " + this.emissionConfigGroup.getDetailedVsAverageLookupBehavior() + " but are not providing a corresponding" +
										    " cold emissions file.") ;
				}
				if ( this.emissionConfigGroup.getAverageWarmEmissionFactorsFile()==null || this.emissionConfigGroup.getAverageWarmEmissionFactorsFile().equals( "" ) ) {
					throw new RuntimeException( "You have requested " + this.emissionConfigGroup.getDetailedVsAverageLookupBehavior() + " but are not providing a corresponding" +
										    " warm emissions file.") ;
				}
				averageFleetWarmEmissionFactorsFile = emissionConfigGroup.getAverageWarmEmissionFactorsFileURL( context );
				averageFleetColdEmissionFactorsFile = emissionConfigGroup.getAverageColdEmissionFactorsFileURL( context );
				// fall-through
			case onlyTryDetailedElseAbort:
				// fall-through
			case tryDetailedThenTechnologyAverageElseAbort:
				//Check if value was loaded
				if ( this.emissionConfigGroup.getDetailedColdEmissionFactorsFile()==null || this.emissionConfigGroup.getDetailedColdEmissionFactorsFile().equals( "" ) ) {
					throw new RuntimeException( "You have requested " + this.emissionConfigGroup.getDetailedVsAverageLookupBehavior() + " but are not providing a corresponding" +
										    " cold emissions file.") ;
				}
				if ( this.emissionConfigGroup.getDetailedWarmEmissionFactorsFile()==null || this.emissionConfigGroup.getDetailedWarmEmissionFactorsFile().equals( "" ) ) {
					throw new RuntimeException( "You have requested " + this.emissionConfigGroup.getDetailedVsAverageLookupBehavior() + " but are not providing a corresponding" +
										    " warm emissions file.") ;
				}
				detailedColdEmissionFactorsFile = emissionConfigGroup.getDetailedColdEmissionFactorsFileURL(context);
				detailedWarmEmissionFactorsFile = emissionConfigGroup.getDetailedWarmEmissionFactorsFileURL(context);
				break;
			default:
				throw new IllegalStateException( "Unexpected value: " + this.emissionConfigGroup.getDetailedVsAverageLookupBehavior() );
		}

		//TODO: create roadtype mapping here from config
		createLookupTables( averageFleetWarmEmissionFactorsFile, averageFleetColdEmissionFactorsFile, detailedWarmEmissionFactorsFile, detailedColdEmissionFactorsFile );

		createEmissionHandler();

		// Event handlers are now added to the event manager inside the respective Handlers, jm march '18
	}
	
	private void createLookupTables( URL averageFleetWarmEmissionFactorsFile, URL averageFleetColdEmissionFactorsFile,
					 URL detailedWarmEmissionFactorsFile, URL detailedColdEmissionFactorsFile ) {
		logger.info("entering createLookupTables");

		switch (emissionConfigGroup.getDetailedVsAverageLookupBehavior()) {
			case onlyTryDetailedElseAbort:
				//fall-through
			case tryDetailedThenTechnologyAverageElseAbort:
				detailedHbefaWarmTable = createDetailedHbefaWarmTable(detailedWarmEmissionFactorsFile);
				detailedHbefaColdTable = createDetailedHbefaColdTable(detailedColdEmissionFactorsFile);
				break;
			case tryDetailedThenTechnologyAverageThenAverageTable:
				detailedHbefaWarmTable = createDetailedHbefaWarmTable(detailedWarmEmissionFactorsFile);
				detailedHbefaColdTable = createDetailedHbefaColdTable(detailedColdEmissionFactorsFile);
				//fall-trough and create additionally average tables
			case directlyTryAverageTable:
				avgHbefaWarmTable = createAvgHbefaWarmTable(averageFleetWarmEmissionFactorsFile);
				avgHbefaColdTable = createAvgHbefaColdTable(averageFleetColdEmissionFactorsFile);
				break;
			default:
				throw new IllegalStateException("Unexpected value: " + emissionConfigGroup.getDetailedVsAverageLookupBehavior());
		}

		logger.info("leaving createLookupTables");

		//create HBEFA Speed tables. try on detailed values first.
		if (detailedHbefaWarmTable != null){
			hbefaRoadTrafficSpeeds = EmissionUtils.createHBEFASpeedsTable(detailedHbefaWarmTable);
		} else if (avgHbefaWarmTable != null){
			hbefaRoadTrafficSpeeds = EmissionUtils.createHBEFASpeedsTable(avgHbefaWarmTable);
		} else {
			throw new RuntimeException("hbefaRoadTrafficSpeed table not created");		//Is table mandatory? -> If yes throw exception
		}
	}

	private void createEmissionHandler() {
		logger.info("entering createEmissionHandler");
		
		loadRoadTypeMappings();

		warmEmissionHandler = new WarmEmissionHandler(scenario, avgHbefaWarmTable, detailedHbefaWarmTable, hbefaRoadTrafficSpeeds, warmPollutants, eventsManager);

		coldEmissionHandler = new ColdEmissionHandler( scenario, avgHbefaColdTable, detailedHbefaColdTable, coldPollutants, eventsManager );
		// this initiates all cold emissions processing!

		logger.info("leaving createEmissionHandler");
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
				throw new RuntimeException(this.emissionConfigGroup.getHbefaRoadTypeSource()+ " is not implemented.");
		}

	}

	
	private Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> createAvgHbefaWarmTable(URL filename){
		logger.info("entering createAvgHbefaWarmTable ...");
		
		Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> avgWarmTable = new HashMap<>();

		try( BufferedReader br = IOUtils.getBufferedReader( filename ) ){
			String strLine = br.readLine();
			Map<String, Integer> indexFromKey = createIndexFromKey( strLine );

			while( (strLine = br.readLine()) != null ){
				Result result = new Result( strLine, indexFromKey ).processMaterialThatIsSameForAverageAndDetailed();
				String[] array = result.getArray();
				HbefaWarmEmissionFactorKey key = result.getKey();

				key.setHbefaVehicleAttributes( new HbefaVehicleAttributes() );

				HbefaWarmEmissionFactor value = new HbefaWarmEmissionFactor();
				value.setSpeed( Double.parseDouble( array[indexFromKey.get( "V_weighted" )] ) );
				value.setWarmEmissionFactor( Double.parseDouble( array[indexFromKey.get( "EFA_weighted" )] ) );

				avgWarmTable.put( key, value );
			}
		} catch( IOException e ){
			e.printStackTrace();
		}

		logger.info("leaving createAvgHbefaWarmTable ...");
		return avgWarmTable;
	}
	
	private Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> createAvgHbefaColdTable(URL filename){
		logger.info("entering createAvgHbefaColdTable ...");
		
		Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> avgColdTable = new HashMap<>();
		try ( BufferedReader br = IOUtils.getBufferedReader(filename) ) {
			String strLine = br.readLine();
			Map<String, Integer> indexFromKey = createIndexFromKey(strLine);
			
			while ((strLine = br.readLine()) != null)   {
				Result2 result2 = new Result2( strLine, indexFromKey ).processMaterialThatIsSameForAverageAndDetailed();
				String[] array = result2.getArray();
				HbefaColdEmissionFactorKey key = result2.getKey();

				key.setHbefaVehicleAttributes(new HbefaVehicleAttributes());

				double weighting = Double.parseDouble(array[indexFromKey.get("EFA_weighted")]); //TODO better name
				HbefaColdEmissionFactor value = new HbefaColdEmissionFactor(weighting);
				
				avgColdTable.put(key, value);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		logger.info("leaving createAvgHbefaColdTable ...");
		return avgColdTable;
	}
	
	private Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> createDetailedHbefaWarmTable(URL filename){
		logger.info("entering createDetailedHbefaWarmTable ...");

		Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> hbefaWarmTableDetailed = new HashMap<>() ;
		try ( BufferedReader br = IOUtils.getBufferedReader(filename) ) {
			String strLine = br.readLine();

			Map<String, Integer> indexFromKey = createIndexFromKey(strLine);

			while ((strLine = br.readLine()) != null) {
				Result result = new Result( strLine, indexFromKey ).processMaterialThatIsSameForAverageAndDetailed();
				String[] array = result.getArray();
				HbefaWarmEmissionFactorKey key = result.getKey();

				HbefaVehicleAttributes hbefaVehicleAttributes = new HbefaVehicleAttributes();
				hbefaVehicleAttributes.setHbefaTechnology(array[indexFromKey.get("Technology")]);
				hbefaVehicleAttributes.setHbefaSizeClass(array[indexFromKey.get("SizeClasse")]);
				hbefaVehicleAttributes.setHbefaEmConcept(array[indexFromKey.get("EmConcept")]);
				key.setHbefaVehicleAttributes(hbefaVehicleAttributes);

				HbefaWarmEmissionFactor value = new HbefaWarmEmissionFactor();
				value.setSpeed(Double.parseDouble(array[indexFromKey.get("V")]));
				value.setWarmEmissionFactor(Double.parseDouble(array[indexFromKey.get("EFA")]));

				hbefaWarmTableDetailed.put(key, value);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		logger.info("leaving createDetailedHbefaWarmTable ...");
		return hbefaWarmTableDetailed;
	}

	private Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> createDetailedHbefaColdTable(URL filename) {
		logger.info("entering createDetailedHbefaColdTable ...");
		
		Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> hbefaColdTableDetailed = new HashMap<>();
		try ( BufferedReader br = IOUtils.getBufferedReader(filename) ) {
			String strLine = br.readLine();
			Map<String, Integer> indexFromKey = createIndexFromKey(strLine);
			
			while ((strLine = br.readLine()) != null)   {
				Result2 result2 = new Result2( strLine, indexFromKey ).processMaterialThatIsSameForAverageAndDetailed();
				String[] array = result2.getArray();
				HbefaColdEmissionFactorKey key = result2.getKey();

				HbefaVehicleAttributes hbefaVehicleAttributes = new HbefaVehicleAttributes();
				hbefaVehicleAttributes.setHbefaTechnology(array[indexFromKey.get("Technology")]);
				hbefaVehicleAttributes.setHbefaSizeClass(array[indexFromKey.get("SizeClasse")]);
				hbefaVehicleAttributes.setHbefaEmConcept(array[indexFromKey.get("EmConcept")]);
				key.setHbefaVehicleAttributes(hbefaVehicleAttributes);

				double weighting = Double.parseDouble(array[indexFromKey.get("EFA")]);
				HbefaColdEmissionFactor value = new HbefaColdEmissionFactor(weighting);
				
				hbefaColdTableDetailed.put(key, value);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		logger.info("leaving createDetailedHbefaColdTable ...");
		return hbefaColdTableDetailed;
	}
	private class Result{
		private final String strLine;
		private final Map<String, Integer> indexFromKey;
		private String[] array;
		private HbefaWarmEmissionFactorKey key;
		Result( String strLine, Map<String, Integer> indexFromKey ){
			this.strLine = strLine;
			this.indexFromKey = indexFromKey;
		}
		public String[] getArray(){
			return array;
		}
		public HbefaWarmEmissionFactorKey getKey(){
			return key;
		}
		Result processMaterialThatIsSameForAverageAndDetailed(){
			array = strLine.split( ";" );

			key = new HbefaWarmEmissionFactorKey();
			key.setHbefaVehicleCategory(EmissionUtils.mapString2HbefaVehicleCategory(array[indexFromKey.get("VehCat")]));

			Pollutant pollutant = EmissionUtils.getPollutant( array[indexFromKey.get("Component" )] );

			warmPollutants.add(pollutant);
			key.setHbefaComponent(pollutant);

			key.setHbefaRoadCategory(mapString2HbefaRoadCategory(array[indexFromKey.get("TrafficSit")]));
			key.setHbefaTrafficSituation(mapString2HbefaTrafficSituation(array[indexFromKey.get("TrafficSit")]));
			// (both the road category and the traffic situation are in the same column in hbefa.  kai, jan'20)
			return this;
		}

		private String mapString2HbefaRoadCategory( String string ) {
			String hbefaRoadCategory;
			String[] parts = string.split("/");
			hbefaRoadCategory = parts[0] + "/" + parts[1] + "/" + parts[2];
			return hbefaRoadCategory;
		}

		private HbefaTrafficSituation mapString2HbefaTrafficSituation(String string) {
			HbefaTrafficSituation hbefaTrafficSituation;
			if(string.endsWith("Freeflow")) hbefaTrafficSituation = HbefaTrafficSituation.FREEFLOW;
			else if(string.endsWith("Heavy")) hbefaTrafficSituation = HbefaTrafficSituation.HEAVY;
			else if(string.endsWith("Satur.")) hbefaTrafficSituation = HbefaTrafficSituation.SATURATED;
			else if(string.endsWith("St+Go")) hbefaTrafficSituation = HbefaTrafficSituation.STOPANDGO;
			else {
				logger.warn("Could not map String " + string + " to any HbefaTrafficSituation; please check syntax in hbefa input file.");
				throw new RuntimeException();
			}
			return hbefaTrafficSituation;
		}

	}

	private class Result2{
		private final String strLine;
		private final Map<String, Integer> indexFromKey;
		private String[] array;
		private HbefaColdEmissionFactorKey key;
		Result2( String strLine, Map<String, Integer> indexFromKey ){
			this.strLine = strLine;
			this.indexFromKey = indexFromKey;
		}
		public String[] getArray(){
			return array;
		}
		public HbefaColdEmissionFactorKey getKey(){
			return key;
		}
		Result2 processMaterialThatIsSameForAverageAndDetailed(){
			array = strLine.split( ";" );

			key = new HbefaColdEmissionFactorKey();
			key.setHbefaVehicleCategory(EmissionUtils.mapString2HbefaVehicleCategory(array[indexFromKey.get("VehCat")]));

			String pollutantString = array[indexFromKey.get("Component")];
			Pollutant pollutant = EmissionUtils.getPollutant( pollutantString );

			coldPollutants.add(pollutant);
			key.setHbefaComponent(pollutant);

			key.setHbefaParkingTime(mapAmbientCondPattern2ParkingTime(array[indexFromKey.get("AmbientCondPattern")]));
			key.setHbefaDistance(mapAmbientCondPattern2Distance(array[indexFromKey.get("AmbientCondPattern")]));
			return this;
		}

		private Integer mapAmbientCondPattern2Distance( String string ) {
			int distance;
			String distanceString = string.split(",")[2];
			String upperbound = distanceString.split("-")[1];
			distance = Integer.parseInt(upperbound.split("k")[0]);
			return distance;
		}

		private Integer mapAmbientCondPattern2ParkingTime( String string ) {
			int parkingTime;
			String parkingTimeString = string.split(",")[1];
			if(parkingTimeString.equals(">12h")){
				parkingTime = 13 ;
			} else {
				String upperbound = parkingTimeString.split("-")[1];
				parkingTime = Integer.parseInt(upperbound.split("h")[0]);
			}
			return parkingTime;
		}

	}

    public LinkEmissionsCalculator getWarmEmissionAnalysisModule() {
		// makes sense to have this public for externalization computations.  kai, jan'20

		return this. warmEmissionHandler.getWarmEmissionAnalysisModule();
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
}
