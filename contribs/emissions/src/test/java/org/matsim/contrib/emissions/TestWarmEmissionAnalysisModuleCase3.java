/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 * TestEmission.java                                                       *
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup.DetailedVsAverageLookupBehavior;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehiclesFactory;

import java.util.*;

import static org.matsim.contrib.emissions.Pollutant.PM;

/**
 * @author julia
 */

/*
 * Case 3 - stop go entry in both tables, free flow entry in average table -> use average (now fail)
 */

/*
 * test for playground.vsp.emissions.WarmEmissionAnalysisModule
 *
 * WarmEmissionAnalysisModule (weam)
 * public methods and corresponding tests:
 * weamParameter - testWarmEmissionAnalysisParameter
 * throw warm EmissionEvent - testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent*, testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent_Exceptions
 * check vehicle info and calculate warm emissions -testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent*, testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent_Exceptions
 * get free flow occurrences - testCounters*()
 * get fraction occurrences - testCounters*()
 * get stop-go occurrences - testCounters*()
 * get km counter - testCounters*()
 * get free flow km counter - testCounters*()
 * get top go km counter - testCounters*()
 * get warm emission event counter - testCounters*()
 *
 * private methods and corresponding tests:
 * rescale warm emissions - rescaleWarmEmissionsTest()
 * calculate warm emissions - implicitly tested
 * convert string 2 tuple - implicitly tested
 *
 * in all cases the needed tables are created manually by the setUp() method
 * see test methods for details on the particular test cases
 **/

public class TestWarmEmissionAnalysisModuleCase3{
	// This used to be one large test class, which had separate table entries for each test, but put them all into the same table.  The result was
	// difficult if not impossible to debug, and the resulting detailed table was inconsistent in the sense that it did not contain all combinations of
	// entries. -- I have now pulled this apart into 6 different test classes, this one here plus "Case1" to "Case5".  Things look ok, but given that the
	// single class before was so large that I could not fully comprehend it, there may now be errors in the ripped-apart classes.  Hopefully, over time,
	// this will help to sort things out.  kai, feb'20

	private final HandlerToTestEmissionAnalysisModules emissionEventManager = new HandlerToTestEmissionAnalysisModules();

	private static final Set<Pollutant> pollutants = new HashSet<>( Arrays.asList( Pollutant.values() ));
	static final String HBEFA_ROAD_CATEGORY = "URB";
	private static final int leaveTime = 0;
	private static final String PASSENGER_CAR = "PASSENGER_CAR";

	private Map<Pollutant, Double> warmEmissions;

	// emission factors for tables - no duplicates!
	private static final Double AVG_PC_FACTOR_FF = 1.;
	private static final Double AVG_PC_FACTOR_SG = 10.;

	// case 3 - stop-go entry in both tables, free flow entry in average table -> use average (now fail)
	private final String dieselTechnology = "PC diesel";
	private final String dieselSizeClass = "diesel";
	private final String dieselConcept = ">=2L";

	/*
	 * this test method creates a diesel vehicle and mock link
	 * for two cases:  "avg speed = free flow speed" & "avg speed = stop-go speed" the average values are used to calculate the PM warm emissions
	 */
	@ParameterizedTest
	@EnumSource(EmissionsConfigGroup.EmissionsComputationMethod.class)
	void testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent3(EmissionsConfigGroup.EmissionsComputationMethod emissionsComputationMethod){

		//-- set up tables, event handler, parameters, module
		WarmEmissionAnalysisModule emissionsModule = setUp(emissionsComputationMethod);

		// case 3 - stop-go entry in both tables, free flow entry in average table -> use average
		Id<Vehicle> dieselVehicleId = Id.create("veh 3", Vehicle.class);
		double dieselLinkLength= 20.;
		Link diesellink = TestWarmEmissionAnalysisModule.createMockLink("link 3", dieselLinkLength, TestWarmEmissionAnalysisModule.AVG_PASSENGER_CAR_SPEED_FF_KMH / 3.6 );

		Id<VehicleType> dieselVehicleTypeId = Id.create(
				PASSENGER_CAR +";"+ dieselTechnology+ ";"+ dieselSizeClass+";"+dieselConcept, VehicleType.class );
		VehiclesFactory vehFac = VehicleUtils.getFactory();
		Vehicle dieselVehicle = vehFac.createVehicle(dieselVehicleId, vehFac.createVehicleType(dieselVehicleTypeId));

		// subcase avg speed = free flow speed
		warmEmissions = emissionsModule.checkVehicleInfoAndCalculateWarmEmissions(dieselVehicle, diesellink,  dieselLinkLength/ TestWarmEmissionAnalysisModule.AVG_PASSENGER_CAR_SPEED_FF_KMH *3.6 );
		Assertions.assertEquals( AVG_PC_FACTOR_FF *dieselLinkLength/1000., warmEmissions.get(PM ), MatsimTestUtils.EPSILON );
		emissionEventManager.reset();
		emissionsModule.throwWarmEmissionEvent(leaveTime, diesellink.getId(), dieselVehicleId, warmEmissions );
		Assertions.assertEquals( pollutants.size() * AVG_PC_FACTOR_FF *dieselLinkLength/1000., emissionEventManager.getSum(), MatsimTestUtils.EPSILON );
		emissionEventManager.reset();
		warmEmissions.clear();

		// subcase avg speed = stop-go speed
		warmEmissions = emissionsModule.checkVehicleInfoAndCalculateWarmEmissions(dieselVehicle, diesellink,  dieselLinkLength/ TestWarmEmissionAnalysisModule.AVG_PASSENGER_CAR_SPEED_SG_KMH *3.6 );
		Assertions.assertEquals( AVG_PC_FACTOR_SG *dieselLinkLength/1000., warmEmissions.get(PM ), MatsimTestUtils.EPSILON );
		emissionEventManager.reset();
		emissionsModule.throwWarmEmissionEvent(leaveTime, diesellink.getId(), dieselVehicleId, warmEmissions );
		Assertions.assertEquals( pollutants.size() * AVG_PC_FACTOR_SG *dieselLinkLength/1000., emissionEventManager.getSum(), MatsimTestUtils.EPSILON );
		emissionEventManager.reset();
		warmEmissions.clear();
	}

	/*
	 * this test method creates a diesel vehicle and mock link
	 * for two cases:  "current speed equals free flow speed" & "current speed equals stop-go speed" the counters are tested
	*/
	@ParameterizedTest
	@EnumSource(EmissionsConfigGroup.EmissionsComputationMethod.class)
	void testCounters4(EmissionsConfigGroup.EmissionsComputationMethod emissionsComputationMethod){

		WarmEmissionAnalysisModule emissionsModule = setUp(emissionsComputationMethod);

		// case 3 - stop-go entry in both tables, free flow entry in average table -> use average
		Id<Vehicle> dieselVehicleId = Id.create("vehicle 3", Vehicle.class);
		double dieselLinkLength= 200.*1000;
		Id<VehicleType> dieselVehicleTypeId = Id.create(
				PASSENGER_CAR +";"+ dieselTechnology+ ";"+ dieselSizeClass+";"+dieselConcept, VehicleType.class );
		VehiclesFactory vehFac = VehicleUtils.getFactory();
		Vehicle dieselVehicle = vehFac.createVehicle(dieselVehicleId, vehFac.createVehicleType(dieselVehicleTypeId));
		Link diesellink = TestWarmEmissionAnalysisModule.createMockLink("link 3", dieselLinkLength, TestWarmEmissionAnalysisModule.AVG_PASSENGER_CAR_SPEED_FF_KMH / 3.6 );

		// subcase: current speed equals free flow speed
		warmEmissions = emissionsModule.checkVehicleInfoAndCalculateWarmEmissions(dieselVehicle, diesellink, dieselLinkLength/ TestWarmEmissionAnalysisModule.AVG_PASSENGER_CAR_SPEED_FF_KMH *3.6 );
		Assertions.assertEquals(0, emissionsModule.getFractionOccurences() );
		Assertions.assertEquals(dieselLinkLength/1000., emissionsModule.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON );
		Assertions.assertEquals(1, emissionsModule.getFreeFlowOccurences() );
		Assertions.assertEquals(dieselLinkLength/1000, emissionsModule.getKmCounter(), MatsimTestUtils.EPSILON );
		Assertions.assertEquals(0., emissionsModule.getStopGoKmCounter(), MatsimTestUtils.EPSILON );
		Assertions.assertEquals(0, emissionsModule.getStopGoOccurences() );
		Assertions.assertEquals(1, emissionsModule.getWarmEmissionEventCounter() );
		emissionsModule.reset();

		// subcase: current speed equals stop-go speed
		warmEmissions = emissionsModule.checkVehicleInfoAndCalculateWarmEmissions(dieselVehicle, diesellink, dieselLinkLength/ TestWarmEmissionAnalysisModule.AVG_PASSENGER_CAR_SPEED_SG_KMH *3.6 );
		Assertions.assertEquals(0, emissionsModule.getFractionOccurences() );
		Assertions.assertEquals(0., emissionsModule.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON );
		Assertions.assertEquals(0, emissionsModule.getFreeFlowOccurences() );
		Assertions.assertEquals(dieselLinkLength/1000, emissionsModule.getKmCounter(), MatsimTestUtils.EPSILON );
		Assertions.assertEquals(dieselLinkLength/1000., emissionsModule.getStopGoKmCounter(), MatsimTestUtils.EPSILON );
		Assertions.assertEquals(1, emissionsModule.getStopGoOccurences() );
		Assertions.assertEquals(1, emissionsModule.getWarmEmissionEventCounter() );
		emissionsModule.reset();
	}

	private WarmEmissionAnalysisModule setUp(EmissionsConfigGroup.EmissionsComputationMethod emissionsComputationMethod) {
		Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> avgHbefaWarmTable = new HashMap<>();
		Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> detailedHbefaWarmTable = new HashMap<>();

		TestWarmEmissionAnalysisModule.fillAverageTable( avgHbefaWarmTable );
		fillDetailedTable( detailedHbefaWarmTable );
		Map<HbefaRoadVehicleCategoryKey, Map<HbefaTrafficSituation, Double>> hbefaRoadTrafficSpeeds = EmissionUtils.createHBEFASpeedsTable(
				avgHbefaWarmTable );
		TestWarmEmissionAnalysisModule.addDetailedRecordsToTestSpeedsTable( hbefaRoadTrafficSpeeds, detailedHbefaWarmTable );

		EventsManager emissionEventManager = this.emissionEventManager;
		EmissionsConfigGroup ecg = new EmissionsConfigGroup();
		ecg.setHbefaVehicleDescriptionSource( EmissionsConfigGroup.HbefaVehicleDescriptionSource.usingVehicleTypeId );
		ecg.setEmissionsComputationMethod( emissionsComputationMethod );
		ecg.setDetailedVsAverageLookupBehavior( DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageThenAverageTable );

		return new WarmEmissionAnalysisModule( avgHbefaWarmTable, detailedHbefaWarmTable, hbefaRoadTrafficSpeeds, pollutants, emissionEventManager, ecg );

	}

	private void fillDetailedTable( Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> detailedHbefaWarmTable) {
		// the interesting thing here is that the first table only contains values for freeflow, and the second one only for stop/go.  So they are
		// inconsistent and thus fail.

		{
			HbefaVehicleAttributes vehAtt = new HbefaVehicleAttributes();
			String ffOnlyConcept = "PC-D-Euro-3";
			vehAtt.setHbefaEmConcept(ffOnlyConcept);
			String ffOnlySizeClass = ">=2L";
			vehAtt.setHbefaSizeClass(ffOnlySizeClass);
			String ffOnlyTechnology = "diesel";
			vehAtt.setHbefaTechnology(ffOnlyTechnology);

			HbefaWarmEmissionFactor detWarmFactorFF = new HbefaWarmEmissionFactor(.0000001, TestWarmEmissionAnalysisModule.AVG_PASSENGER_CAR_SPEED_FF_KMH);

			for (Pollutant wp : pollutants) {
				HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();
				detWarmKey.setComponent( wp );
				detWarmKey.setRoadCategory( HBEFA_ROAD_CATEGORY );
				detWarmKey.setTrafficSituation( HbefaTrafficSituation.FREEFLOW );
				detWarmKey.setVehicleAttributes( vehAtt );
				detWarmKey.setVehicleCategory( HbefaVehicleCategory.PASSENGER_CAR );
				detailedHbefaWarmTable.put( detWarmKey, detWarmFactorFF );
			}

			HbefaWarmEmissionFactor detWarmFactorStG = new HbefaWarmEmissionFactor(.000001, TestWarmEmissionAnalysisModule.AVG_PASSENGER_CAR_SPEED_SG_KMH);

			for( Pollutant wp : pollutants ){
				HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();
				detWarmKey.setComponent( wp );
				detWarmKey.setRoadCategory( HBEFA_ROAD_CATEGORY );
				detWarmKey.setTrafficSituation( HbefaTrafficSituation.STOPANDGO );
				detWarmKey.setVehicleAttributes( vehAtt );
				detWarmKey.setVehicleCategory( HbefaVehicleCategory.PASSENGER_CAR );
				detailedHbefaWarmTable.put( detWarmKey, detWarmFactorStG );
			}
		}

		{
			HbefaVehicleAttributes vehAtt = new HbefaVehicleAttributes();
			String sgOnlyConcept = "PC-Alternative Fuel";
			vehAtt.setHbefaEmConcept(sgOnlyConcept);
			String sgOnlySizeClass = "not specified";
			vehAtt.setHbefaSizeClass(sgOnlySizeClass);
			String sgOnlyTechnology = "bifuel CNG/petrol";
			vehAtt.setHbefaTechnology(sgOnlyTechnology);

			HbefaWarmEmissionFactor detWarmFactorFF = new HbefaWarmEmissionFactor(.000000001, TestWarmEmissionAnalysisModule.AVG_PASSENGER_CAR_SPEED_FF_KMH);

			for( Pollutant wp : pollutants ){
				HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();
				detWarmKey.setComponent( wp );
				detWarmKey.setRoadCategory( HBEFA_ROAD_CATEGORY );
				detWarmKey.setTrafficSituation( HbefaTrafficSituation.FREEFLOW );
				detWarmKey.setVehicleAttributes( vehAtt );
				detWarmKey.setVehicleCategory( HbefaVehicleCategory.PASSENGER_CAR );
				detailedHbefaWarmTable.put( detWarmKey, detWarmFactorFF );
			}

			HbefaWarmEmissionFactor detWarmFactorStG = new HbefaWarmEmissionFactor(.00000001, TestWarmEmissionAnalysisModule.AVG_PASSENGER_CAR_SPEED_SG_KMH);

			for (Pollutant wp : pollutants) {
				HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();
				detWarmKey.setComponent(wp);
				detWarmKey.setRoadCategory(HBEFA_ROAD_CATEGORY);
				detWarmKey.setTrafficSituation(HbefaTrafficSituation.STOPANDGO);
				detWarmKey.setVehicleAttributes(vehAtt);
				detWarmKey.setVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
				detailedHbefaWarmTable.put(detWarmKey, detWarmFactorStG);
			}
		}

	}

}




