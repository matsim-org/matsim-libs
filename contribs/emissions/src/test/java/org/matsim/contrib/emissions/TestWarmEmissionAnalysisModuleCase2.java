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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
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
import java.util.stream.Stream;

import static org.matsim.contrib.emissions.Pollutant.NMHC;
import static org.matsim.contrib.emissions.TestWarmEmissionAnalysisModule.fillAverageTable;

/**
 * @author julia
 * /

/*
 *  Case 2 - free flow entry in both tables, stop go entry in average table -> use average
 * 	see (*) below.  kai, jan'20
 */

/*
 * test for playground.vsp.emissions.WarmEmissionAnalysisModule
 *
 * WarmEmissionAnalysisModule (weam)
 * public methods and corresponding tests:
 * weamParameter - testWarmEmissionAnalysisParameter
 * throw warm EmissionEvent - testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent*, testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent_Exceptions
 * check vehicle info and calculate warm emissions -testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent*, testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent_Exceptions
 * get free flow occurences - testCounters*()
 * get fraction occurences - testCounters*()
 * get stop go occurences - testCounters*()
 * get km counter - testCounters*()
 * get free flow km counter - testCounters*()
 * get top go km couter - testCounters*()
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

public class TestWarmEmissionAnalysisModuleCase2{
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


	//private WarmEmissionAnalysisModule emissionsModule;
	private Map<Pollutant, Double> warmEmissions;

	private static final Double AVG_PC_FACTOR_SG = 10.;

	// vehicle information for regular test cases

	// case 2 - free flow entry in both tables, stop go entry in average table -> use average (now fail)
	private static final String PC_TECHNOLOGY = "PC petrol <1,4L <ECE";
	private static final String PC_SIZE_CLASS = "petrol (4S)";
	private static final String PC_CONCEPT = "<1,4L";
	private static final Double PC_FREE_VELOCITY_KMH = 20.; //km/h
	private static final Double PCSG_VELOCITY_KMH = 10.; //km/h
	private static final double DETAILED_PC_FACTOR_FF = .0001;

	/*
	 * this test method creates a mock link and mock vehicle with a complete vehicleTypId --> lookUpBehaviour: tryDetailedThenTechnologyAverageThenAverageTable
	 * for two speed cases: avg speed = free flow speed & avg speed = stop go speed the NMHC warm emissions and emissions sum are computed using the two emissionsComputationMethods StopAndGoFraction & AverageSpeed
	 */

	@ParameterizedTest
	@EnumSource(EmissionsConfigGroup.EmissionsComputationMethod.class)
	void testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent2(EmissionsConfigGroup.EmissionsComputationMethod emissionsComputationMethod){
		//-- set up tables, event handler, parameters, module

		WarmEmissionAnalysisModule emissionsModule = setUp(emissionsComputationMethod);

		// case 2 - free flow entry in both tables, stop go entry in average table -> use average
		// see (*) below.  kai, jan'20

		// create a link:
		double pclinkLength= 100.;
		Link pclink = TestWarmEmissionAnalysisModule.createMockLink("link 2", pclinkLength, PC_FREE_VELOCITY_KMH / 3.6 );

		// create a vehicle:
		Id<Vehicle> pcVehicleId = Id.create("veh 2", Vehicle.class);
		Id<VehicleType> pcVehicleTypeId = Id.create( PASSENGER_CAR + ";"+ PC_TECHNOLOGY + ";"+ PC_SIZE_CLASS +";"+ PC_CONCEPT, VehicleType.class );
		VehiclesFactory vehFac = VehicleUtils.getFactory();
		Vehicle pcVehicle = vehFac.createVehicle(pcVehicleId, vehFac.createVehicleType(pcVehicleTypeId));

		// sub case avg speed = free flow speed
		{
			// compute warm emissions with travel time coming from free flow:
			warmEmissions = emissionsModule.checkVehicleInfoAndCalculateWarmEmissions( pcVehicle, pclink, pclinkLength / PC_FREE_VELOCITY_KMH * 3.6 );
					//KMT Feb'20: Trying to understand the result:
					// - StopAndGo fraction is computed as 0 --> ONLY freeFlow values needed and looked up
					// - DETAILED FreeFlow value is available in the table (1.0E-4 g/km);  StopAndGo value is NOT available in the table
					// - AVERAGE  FreeFlow value is available in the table (1.0 g/km) ;    StopAndGo value is NOT available in the table ( 10.0 g/km)
					// --> It seems like it was intended (or only implemented) in a way, that if one of the detailed values is missing (FreeFlow or StopGo) there is a fallback to average. So the result of this would be 1.0 g/km * 0.1 km = 0.1 g/km
					// --> Now, after implementing the new fallback behaviour, it is looking up both values (FreeFlow or StopGo) ways independently from each other. Therefore the result comes from the detailed table (1.0E-4 g/km) * * 0.1 km = 1.0E-5 g/km
					// -----> We need a decision here, if we want allow that inconsistent(?) lookup of FreeFlow and Detailed values with different grade of detail or not.
					// After discussion with Kai N. we decided to let it as it is for the time being. I will add a log.info in the consistency checker.  KMT Jul'20

			//results should be equal here, because in both cases only the freeflow value is relevant (100% freeflow, 0% stop&go).
			Assertions.assertEquals( 0.1, warmEmissions.get( NMHC ), MatsimTestUtils.EPSILON ); //(*#)

			// throw and test corresponding event:
			emissionsModule.throwWarmEmissionEvent( leaveTime, pclink.getId(), pcVehicleId, warmEmissions );
			Assertions.assertEquals( 2.3, emissionEventManager.getSum(), MatsimTestUtils.EPSILON ); //seems to be (0.1 (g/km -- see expected values a few lines above(*#) * number of entries in enum Pollutant)

			emissionEventManager.reset();
			warmEmissions.clear();
		}

		// sub case avg speed = stop go speed
		{
			warmEmissions = emissionsModule.checkVehicleInfoAndCalculateWarmEmissions( pcVehicle, pclink, pclinkLength / PCSG_VELOCITY_KMH * 3.6 );
			Assertions.assertEquals( AVG_PC_FACTOR_SG * pclinkLength / 1000., warmEmissions.get( NMHC ), MatsimTestUtils.EPSILON );

			emissionsModule.throwWarmEmissionEvent( leaveTime, pclink.getId(), pcVehicleId, warmEmissions );
			Assertions.assertEquals(pollutants.size() * AVG_PC_FACTOR_SG * pclinkLength / 1000., emissionEventManager.getSum(), MatsimTestUtils.EPSILON );
			emissionsModule.reset();
			warmEmissions.clear();
		}
	}

	/*
	 * this test method creates a vehicle and mock link
	 * for three cases:  "current speed equals free flow speed" & "current speed equals stop go speed" & "current speed equals stop go speed" the counters are  tested
	 * average values are used
	 */
	@ParameterizedTest
	@EnumSource(EmissionsConfigGroup.EmissionsComputationMethod.class)
	void testCounters3(EmissionsConfigGroup.EmissionsComputationMethod emissionsComputationMethod){

		WarmEmissionAnalysisModule emissionsModule = setUp(emissionsComputationMethod);

		// case 2 - free flow entry in both tables, stop go entry in average table -> use average
		Id<Vehicle> pcVehicleId = Id.create("vehicle 2", Vehicle.class);
		double pclinkLength= 20.*1000;
		Id<VehicleType> pcVehicleTypeId = Id.create( PASSENGER_CAR + ";"+ PC_TECHNOLOGY + ";"+ PC_SIZE_CLASS +";"+ PC_CONCEPT, VehicleType.class );
		VehiclesFactory vehFac = VehicleUtils.getFactory();
		Vehicle pcVehicle = vehFac.createVehicle(pcVehicleId, vehFac.createVehicleType(pcVehicleTypeId));
		Link pclink = TestWarmEmissionAnalysisModule.createMockLink("link 2", pclinkLength, PC_FREE_VELOCITY_KMH / 3.6 );

		// sub case: current speed equals free flow speed
		warmEmissions = emissionsModule.checkVehicleInfoAndCalculateWarmEmissions(pcVehicle,pclink, pclinkLength/ PC_FREE_VELOCITY_KMH *3.6 );
		Assertions.assertEquals(0, emissionsModule.getFractionOccurences() );
		Assertions.assertEquals(pclinkLength/1000, emissionsModule.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON );
		Assertions.assertEquals(1, emissionsModule.getFreeFlowOccurences() );
		Assertions.assertEquals(pclinkLength/1000, emissionsModule.getKmCounter(), MatsimTestUtils.EPSILON );
		Assertions.assertEquals(0., emissionsModule.getStopGoKmCounter(), MatsimTestUtils.EPSILON );
		Assertions.assertEquals(0, emissionsModule.getStopGoOccurences() );
		Assertions.assertEquals(1, emissionsModule.getWarmEmissionEventCounter() );
		emissionsModule.reset();

		// sub case: current speed equals stop go speed
		warmEmissions = emissionsModule.checkVehicleInfoAndCalculateWarmEmissions(pcVehicle, pclink, pclinkLength/ PCSG_VELOCITY_KMH *3.6 );
		Assertions.assertEquals(0, emissionsModule.getFractionOccurences() );
		Assertions.assertEquals(0., emissionsModule.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON );
		Assertions.assertEquals(0, emissionsModule.getFreeFlowOccurences() );
		Assertions.assertEquals(pclinkLength/1000, emissionsModule.getKmCounter(), MatsimTestUtils.EPSILON );
		Assertions.assertEquals(pclinkLength/1000, emissionsModule.getStopGoKmCounter(), MatsimTestUtils.EPSILON );
		Assertions.assertEquals(1, emissionsModule.getStopGoOccurences() );
		Assertions.assertEquals(1, emissionsModule.getWarmEmissionEventCounter() );

	}

	private WarmEmissionAnalysisModule setUp(EmissionsConfigGroup.EmissionsComputationMethod emissionsComputationMethod) {
		Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> avgHbefaWarmTable = new HashMap<>();
		Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> detailedHbefaWarmTable = new HashMap<>();

		fillAverageTable( avgHbefaWarmTable );
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


		// entry for second test case "pc" -- should not be used
		{
			HbefaVehicleAttributes vehAtt = new HbefaVehicleAttributes();
			vehAtt.setHbefaTechnology( PC_TECHNOLOGY );
			vehAtt.setHbefaSizeClass( PC_SIZE_CLASS );
			vehAtt.setHbefaEmConcept( PC_CONCEPT );
			String ffOnlyConceptAlt = "PC-Alternative Fuel";
			vehAtt.setHbefaEmConcept( ffOnlyConceptAlt );
			String ffOnlySizeClassAlt = "not specified";
			vehAtt.setHbefaSizeClass( ffOnlySizeClassAlt );
			String ffOnlyTechnologyAlt = "bifuel CNG/petrol";
			vehAtt.setHbefaTechnology( ffOnlyTechnologyAlt );

			HbefaWarmEmissionFactor detWarmFactor = new HbefaWarmEmissionFactor(DETAILED_PC_FACTOR_FF, PC_FREE_VELOCITY_KMH);

			for( Pollutant wp : pollutants ){
				HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();
				detWarmKey.setComponent(wp);
				detWarmKey.setRoadCategory(HBEFA_ROAD_CATEGORY);
				detWarmKey.setTrafficSituation(HbefaTrafficSituation.FREEFLOW);
				detWarmKey.setVehicleAttributes(vehAtt);
				detWarmKey.setVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
				detailedHbefaWarmTable.put(detWarmKey, detWarmFactor);
			}
		}

		// entry for ffOnlyTestcase
		{
			HbefaVehicleAttributes vehAtt = new HbefaVehicleAttributes();
			String ffOnlyConcept = "PC-D-Euro-3";
			vehAtt.setHbefaEmConcept(ffOnlyConcept);
			String ffOnlySizeClass = ">=2L";
			vehAtt.setHbefaSizeClass(ffOnlySizeClass);
			String ffOnlyTechnology = "diesel";
			vehAtt.setHbefaTechnology(ffOnlyTechnology);

			String ffOnlyConceptAlt = "PC-Alternative Fuel";
			vehAtt.setHbefaEmConcept( ffOnlyConceptAlt );
			String ffOnlySizeClassAlt = "not specified";
			vehAtt.setHbefaSizeClass( ffOnlySizeClassAlt );
			String ffOnlyTechnologyAlt = "bifuel CNG/petrol";
			vehAtt.setHbefaTechnology( ffOnlyTechnologyAlt );

			double detailedFfOnlyFactorFf = .0000001;
			double ffOnlyffSpeed = 20.;
			HbefaWarmEmissionFactor detWarmFactor = new HbefaWarmEmissionFactor(detailedFfOnlyFactorFf, ffOnlyffSpeed);

			for (Pollutant wp : pollutants) {
				HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();
				detWarmKey.setComponent(wp);
				detWarmKey.setRoadCategory(HBEFA_ROAD_CATEGORY);
				detWarmKey.setTrafficSituation(HbefaTrafficSituation.FREEFLOW);
				detWarmKey.setVehicleAttributes(vehAtt);
				detWarmKey.setVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
				detailedHbefaWarmTable.put(detWarmKey, detWarmFactor);
			}
		}

		// entry for sgOnlyTestcase
		{
			HbefaVehicleAttributes vehAtt = new HbefaVehicleAttributes();
			String sgOnlyConcept = "PC-Alternative Fuel";
			vehAtt.setHbefaEmConcept(sgOnlyConcept);
			String sgOnlySizeClass = "not specified";
			vehAtt.setHbefaSizeClass(sgOnlySizeClass);
			String sgOnlyTechnology = "bifuel CNG/petrol";
			vehAtt.setHbefaTechnology(sgOnlyTechnology);

			double detailedSgOnlyFactorSg = .00000001;
			double sgOnlysgSpeed = 10.;
			HbefaWarmEmissionFactor detWarmFactor = new HbefaWarmEmissionFactor(detailedSgOnlyFactorSg, sgOnlysgSpeed);

			for (Pollutant wp : pollutants) {
				HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();
				detWarmKey.setComponent(wp);
				detWarmKey.setRoadCategory(HBEFA_ROAD_CATEGORY);
				detWarmKey.setTrafficSituation(HbefaTrafficSituation.STOPANDGO);
				detWarmKey.setVehicleAttributes(vehAtt);
				detWarmKey.setVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
				detailedHbefaWarmTable.put(detWarmKey, detWarmFactor);
			}
		}
	}
}




