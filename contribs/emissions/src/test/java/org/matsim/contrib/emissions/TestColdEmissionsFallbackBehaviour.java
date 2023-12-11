/*
 *   *********************************************************************** *
          *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C) 2020 by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.contrib.emissions;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup.DetailedVsAverageLookupBehavior;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.net.URL;
import java.util.Map;

/**
 *  @author kturner
 *
 *  test the different levels of fallback behaviour -> lookup for detailed or less detailed or avarage values or abort,
 *  depending on @link{DetailedVsAverageLookupBehavior}.
 *  Note this test only focus on one type of emissions and one commutation method.
 *  It will NOT test the variety of commutational methods. For this please look to the other tests.
 *
 */
public class TestColdEmissionsFallbackBehaviour {

	private final Link link = generateLink();
	private final Vehicle vehicleFull = generateFullSpecifiedVehicle();
	private final Vehicle vehicleFallbackToTechnologyAverage = generateVehicleForFallbackToTechnologyAverage();
	private final Vehicle vehicleFallbackToAverageTable = generateVehicleForFallbackToAverageTable();

	private final Double startTime = 0.0;
	private static final Double parkingDuration = 1.;
	private static final int distance = 1;

	//This are the expected values and extracted from  "./scenarios/sampleScenario/sample_41_EFA_ColdStart_vehcat_2020average.csv" and
	// "./scenarios/sampleScenario/sample_41_EFA_ColdStart_SubSegm_2020detailed.csv"
	//Both for AmbientConditionPattern 0-1h, 0-1km
	private final double emissionsFactorInGrammPerKilometer_Detailed = 3.337293625; 		//detailed table
	private final double emissionsFactorInGrammPerKilometer_TechnologyAverage = 3.27173543;	//detailed table
	private final double emissionsFactorInGrammPerKilometer_AverageTable = 5.043848991;		//average table


// ---------   DetailedVsAverageLookupBehavior.onlyTryDetailedElseAbort)   -----------
	/**
	 * vehicles information is complete
	 *
	 * LookupBehavior: onlyTryDetailedElseAbort
	 *
	 * -> should calculate value
	 */
	@Test
	void testColdDetailedValueOnlyDetailed() {
		EmissionModule emissionModule = setUpScenario( DetailedVsAverageLookupBehavior.onlyTryDetailedElseAbort );

		Map<Pollutant, Double> coldEmissions = emissionModule.getColdEmissionAnalysisModule()
				.checkVehicleInfoAndCalculateWColdEmissions(vehicleFull.getType(), vehicleFull.getId(), link.getId(), 
						startTime, parkingDuration, distance);

		Assertions.assertEquals(emissionsFactorInGrammPerKilometer_Detailed, coldEmissions.get(Pollutant.CO2_TOTAL ), MatsimTestUtils.EPSILON );
	}


	/**
	 *
	 * vehicles information is complete but fully specified entry is NOT available in detailed table
	 * LookupBehavior: onlyTryDetailedElseAbort
	 *
	 * -> should abort --> RuntimeException
	 */
	@Test
	void testCold_DetailedElseAbort_ShouldAbort1() {
		assertThrows(RuntimeException.class, () -> {
			EmissionModule emissionModule = setUpScenario(DetailedVsAverageLookupBehavior.onlyTryDetailedElseAbort);

			emissionModule.getColdEmissionAnalysisModule()
					.checkVehicleInfoAndCalculateWColdEmissions(vehicleFallbackToTechnologyAverage.getType(),
							vehicleFallbackToTechnologyAverage.getId(), link.getId(), startTime, parkingDuration, distance);
		});
	}

	/**
	 * vehicles information is complete but fully specified entry is NOT available in detailed table
	 * HbefaTechnology is also not in detailed table -> fall back to technology average is NOT possible as well.
	 *
	 * LookupBehavior: onlyTryDetailedElseAbort
	 *
	 * -> should abort --> RuntimeException
	 */
	@Test
	void testCold_DetailedElseAbort_ShouldAbort2() {
		assertThrows(RuntimeException.class, () -> {
			EmissionModule emissionModule = setUpScenario(DetailedVsAverageLookupBehavior.onlyTryDetailedElseAbort);

			emissionModule.getColdEmissionAnalysisModule()
					.checkVehicleInfoAndCalculateWColdEmissions(vehicleFallbackToTechnologyAverage.getType(),
							vehicleFallbackToAverageTable.getId(), link.getId(), startTime, parkingDuration, distance);
		});
	}


// ---------   DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageElseAbort)   -----------
	/**
	 * vehicles information is complete
	 * LookupBehavior: tryDetailedThenTechnologyAverageElseAbort
	 *
	 * -> do NOT fall back to technology average
	 * ---> should calculate value from detailed value
	 */
	@Test
	void testCold_DetailedThenTechnologyAverageElseAbort_FallbackNotNeeded() {
		EmissionModule emissionModule = setUpScenario( DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageElseAbort );

		Map<Pollutant, Double> coldEmissions = emissionModule.getColdEmissionAnalysisModule()
				.checkVehicleInfoAndCalculateWColdEmissions(vehicleFull.getType(), vehicleFull.getId(), link.getId(), 
						startTime, parkingDuration, distance);

		Assertions.assertEquals(emissionsFactorInGrammPerKilometer_Detailed, coldEmissions.get(Pollutant.CO2_TOTAL ), MatsimTestUtils.EPSILON );
	}


	/**
	 * vehicles information is complete but fully specified entry is NOT available in detailed table
	 * LookupBehavior: tryDetailedThenTechnologyAverageElseAbort
	 *
	 * -> do fall back to technology average
	 * ---> should calculate value from technology average
	 */
	@Test
	void testCold_DetailedThenTechnologyAverageElseAbort_FallbackToTechnologyAverage() {
		EmissionModule emissionModule = setUpScenario( DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageElseAbort );

		Map<Pollutant, Double> coldEmissions = emissionModule.getColdEmissionAnalysisModule()
				.checkVehicleInfoAndCalculateWColdEmissions(vehicleFallbackToTechnologyAverage.getType(),
						vehicleFallbackToTechnologyAverage.getId(), link.getId(),
						startTime, parkingDuration, distance);

		Assertions.assertEquals(emissionsFactorInGrammPerKilometer_TechnologyAverage, coldEmissions.get(Pollutant.CO2_TOTAL ), MatsimTestUtils.EPSILON );
	}

	/**
	 * vehicles information is complete but fully specified entry is NOT available in detailed table
	 * HbefaTechnology is also not in detailed table -> fall back to technology average is NOT possible as well.
	 *
	 * LookupBehavior: onlyTryDetailedElseAbort
	 *
	 * -> should abort --> RuntimeException
	 */
	@Test
	void testCold_DetailedThenTechnologyAverageElseAbort_ShouldAbort() {
		assertThrows(RuntimeException.class, () -> {
			EmissionModule emissionModule = setUpScenario(DetailedVsAverageLookupBehavior.onlyTryDetailedElseAbort);

			emissionModule.getColdEmissionAnalysisModule()
					.checkVehicleInfoAndCalculateWColdEmissions(vehicleFallbackToAverageTable.getType(),
							vehicleFallbackToAverageTable.getId(), link.getId(), startTime, parkingDuration, distance);
		});
	}

// ---------   DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageThenAverageTable   -----------
	/**
	 * vehicles information is complete
	 * LookupBehavior: tryDetailedThenTechnologyAverageElseAbort
	 *
	 * -> do NOT fall back to technology average or average table
	 * ---> should calculate value from detailed value
	 */
	@Test
	void testCold_DetailedThenTechnologyAverageThenAverageTable_FallbackNotNeeded() {
		EmissionModule emissionModule = setUpScenario( DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageThenAverageTable );

		Map<Pollutant, Double> coldEmissions = emissionModule.getColdEmissionAnalysisModule()
				.checkVehicleInfoAndCalculateWColdEmissions(vehicleFull.getType(), vehicleFull.getId(), link.getId(),
						startTime, parkingDuration, distance);

		Assertions.assertEquals(emissionsFactorInGrammPerKilometer_Detailed, coldEmissions.get(Pollutant.CO2_TOTAL ), MatsimTestUtils.EPSILON );
	}


	/**
	 * vehicles information is complete but fully specified entry is NOT available in detailed table
	 * LookupBehavior: tryDetailedThenTechnologyAverageElseAbort
	 *
	 * -> do fall back to technology average; do NOT fall back to average table
	 * ---> should calculate value from technology average
	 */
	@Test
	void testCold_DetailedThenTechnologyAverageThenAverageTable_FallbackToTechnology() {
		EmissionModule emissionModule = setUpScenario( DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageThenAverageTable );

		Map<Pollutant, Double> coldEmissions = emissionModule.getColdEmissionAnalysisModule()
				.checkVehicleInfoAndCalculateWColdEmissions(vehicleFallbackToTechnologyAverage.getType(),
						vehicleFallbackToTechnologyAverage.getId(), link.getId(), startTime, parkingDuration, distance);

		Assertions.assertEquals(emissionsFactorInGrammPerKilometer_TechnologyAverage, coldEmissions.get(Pollutant.CO2_TOTAL ), MatsimTestUtils.EPSILON );
	}

	/**
	 * vehicles information is complete but fully specified entry is NOT available in detailed table
	 * HbefaTechnology is also not in detailed table -> fall back to technology average is NOT possible as well.
	 *
	 * LookupBehavior: tryDetailedThenTechnologyAverageThenAverageTable
	 *
	 * -> do fall back to average table
	 * ---> should calculate value from average table
	 */
	@Test
	void testCold_DetailedThenTechnologyAverageThenAverageTable_FallbackToAverageTable() {
		EmissionModule emissionModule = setUpScenario( DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageThenAverageTable );

		Map<Pollutant, Double> coldEmissions = emissionModule.getColdEmissionAnalysisModule()
				.checkVehicleInfoAndCalculateWColdEmissions(vehicleFallbackToAverageTable.getType(),
						vehicleFallbackToAverageTable.getId(), link.getId(), startTime, parkingDuration, distance);

		Assertions.assertEquals(emissionsFactorInGrammPerKilometer_AverageTable, coldEmissions.get(Pollutant.CO2_TOTAL ), MatsimTestUtils.EPSILON );
	}




// ---------- setup and helper methods -------------

	/**
	 * load and prepare the scenario, create the emissionsModule
	 *
	 * @param lookupBehavior the EmissionsConfigGroup.DetailedVsAverageLookupBehavior
	 * @return EmissionsModule
	 */
	private EmissionModule setUpScenario( DetailedVsAverageLookupBehavior lookupBehavior ) {
		URL scenarioUrl = ExamplesUtils.getTestScenarioURL( "emissions-sampleScenario" );
		URL configUrl = IOUtils.extendUrl( scenarioUrl, "config_empty.xml" );
		Config config = ConfigUtils.loadConfig( configUrl );
		EmissionsConfigGroup emissionsConfig = ConfigUtils.addOrGetModule( config, EmissionsConfigGroup.class );
		emissionsConfig.setDetailedVsAverageLookupBehavior(lookupBehavior);
//		emissionsConfig.setHbefaRoadTypeSource( HbefaRoadTypeSource.fromLinkAttributes );							//Somehow needed even if deprecated, since a null pointer exception ids thrown when not set :( . kmt mar'20
		emissionsConfig.setAverageColdEmissionFactorsFile("sample_41_EFA_ColdStart_vehcat_2020average.csv");
		emissionsConfig.setDetailedColdEmissionFactorsFile("sample_41_EFA_ColdStart_SubSegm_2020detailed.csv");
		emissionsConfig.setAverageWarmEmissionFactorsFile( "sample_41_EFA_HOT_vehcat_2020average.csv" );
		emissionsConfig.setDetailedWarmEmissionFactorsFile("sample_41_EFA_HOT_SubSegm_2020detailed.csv");

		Scenario scenario = ScenarioUtils.loadScenario( config );

		EventsManager emissionEventManager = new HandlerToTestEmissionAnalysisModules();

		return new EmissionModule(scenario, emissionEventManager);
	}

	private Link generateLink() {
		Link link = TestWarmEmissionAnalysisModule.createMockLink("link1", 200, 9.72);
		EmissionUtils.setHbefaRoadType(link, "URB/Local/50" );
		return link;
	}

	private Vehicle generateFullSpecifiedVehicle() {
		VehicleType vehicleType = VehicleUtils.createVehicleType(Id.create("dieselCarFullSpecified", VehicleType.class));
		EngineInformation engineInformation = vehicleType.getEngineInformation();
		VehicleUtils.setHbefaVehicleCategory(engineInformation, "PASSENGER_CAR");
		VehicleUtils.setHbefaTechnology(engineInformation,"diesel");
		VehicleUtils.setHbefaEmissionsConcept(engineInformation, "PC-D-Euro-3");
		VehicleUtils.setHbefaSizeClass(engineInformation, ">1,4L");

		return VehicleUtils.createVehicle(Id.createVehicleId("dieselCarFullSpecified"), vehicleType);
	}

	/**
	 * create a vehicle with all information, which is not full represented in emissions table.
	 *
	 * @return vehicle
	 */
	private Vehicle generateVehicleForFallbackToTechnologyAverage() {
		VehicleType vehicleType = VehicleUtils.createVehicleType(Id.create("dieselCarFullSpecified", VehicleType.class));
		EngineInformation engineInformation = vehicleType.getEngineInformation();
		VehicleUtils.setHbefaVehicleCategory(engineInformation, "PASSENGER_CAR");
		VehicleUtils.setHbefaTechnology(engineInformation,"diesel");
		VehicleUtils.setHbefaEmissionsConcept(engineInformation, "PC-D-Euro-3_NotInTable");  //<--- this value is not in table
		VehicleUtils.setHbefaSizeClass(engineInformation, ">1,4L");

		return VehicleUtils.createVehicle(Id.createVehicleId("dieselCarFallbackToTechnology"), vehicleType);
	}

	/**
	 * create a vehicle with all information, which is not represented in detailed emissions table.
	 *
	 * @return vehicle
	 */
	private Vehicle generateVehicleForFallbackToAverageTable() {
		VehicleType vehicleType = VehicleUtils.createVehicleType(Id.create("dieselCarFullSpecified", VehicleType.class));
		EngineInformation engineInformation = vehicleType.getEngineInformation();
		VehicleUtils.setHbefaVehicleCategory(engineInformation, "PASSENGER_CAR");
		VehicleUtils.setHbefaTechnology(engineInformation,"diesel_NotInTable");	//<--- this value is not in table
		VehicleUtils.setHbefaEmissionsConcept(engineInformation, "PC-D-Euro-3");
		VehicleUtils.setHbefaSizeClass(engineInformation, ">1,4L");

		return VehicleUtils.createVehicle(Id.createVehicleId("dieselCarFallbackToAverage"), vehicleType);
	}




}
