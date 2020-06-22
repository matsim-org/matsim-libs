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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
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

import static org.matsim.contrib.emissions.Pollutant.*;
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

@RunWith(Parameterized.class)
public class TestWarmEmissionAnalysisModuleCase2{
	// This used to be one large test class, which had separate table entries for each test, but put them all into the same table.  The result was
	// difficult if not impossible to debug, and the resulting detailed table was inconsistent in the sense that it did not contain all combinations of
	// entries. -- I have now pulled this apart into 6 different test classes, this one here plus "Case1" to "Case5".  Things look ok, but given that the
	// single class before was so large that I could not fully comprehend it, there may now be errors in the ripped-apart classes.  Hopefully, over time,
	// this will help to sort things out.  kai, feb'20

	//Old list of pollutants
//	private final Set<String> pollutants = new HashSet<>(Arrays.asList(CO, CO2_TOTAL, FC, HC, NMHC, NOx, NO2,PM, SO2));
	private static final Set<Pollutant> pollutants = new HashSet<>( Arrays.asList( Pollutant.values() ));
	static final String HBEFA_ROAD_CATEGORY = "URB";
	private static final int leaveTime = 0;
	private final EmissionsConfigGroup.EmissionsComputationMethod emissionsComputationMethod;
	private static final String PASSENGER_CAR = "PASSENGER_CAR";

	private WarmEmissionAnalysisModule emissionsModule;
	private Map<Pollutant, Double> warmEmissions;

	// emission factors for tables - no duplicates!
	private static final Double DETAILED_PETROL_FACTOR_FF = .1;
	private static final Double DETAILED_ZERO_FACTOR_FF =  .0011;
	private static final Double DETAILED_SGFF_FACTOR_FF =   .000011;
	private static final Double DETAILED_SGFF_FACTOR_SG = 	.0000011;
	private static final Double AVG_PC_FACTOR_FF = 1.;
	private static final Double AVG_PC_FACTOR_SG = 10.;

	// vehicle information for regular test cases

	// case 2 - free flow entry in both tables, stop go entry in average table -> use average (now fail)
	private static final String PC_TECHNOLOGY = "PC petrol <1,4L <ECE";
	private static final String PC_SIZE_CLASS = "petrol (4S)";
	private static final String PC_CONCEPT = "<1,4L";
	private static final Double PC_FREE_VELOCITY_KMH = 20.; //km/h
	private static final Double PCSG_VELOCITY_KMH = 10.; //km/h
	private static final double DETAILED_PC_FACTOR_FF = .0001;


	@Parameterized.Parameters( name = "{index}: ComputationMethod={0}")
	public static Collection<Object[]> createCombinations() {
		List <Object[]> list = new ArrayList<>();
		list.add( new Object [] {EmissionsConfigGroup.EmissionsComputationMethod.StopAndGoFraction} ) ;
		list.add( new Object [] {EmissionsConfigGroup.EmissionsComputationMethod.AverageSpeed} ) ;
		return list;
	}

	public TestWarmEmissionAnalysisModuleCase2( EmissionsConfigGroup.EmissionsComputationMethod emissionsComputationMethod ) {
		this.emissionsComputationMethod = emissionsComputationMethod;
	}

//	@Test
//	public void testWarmEmissionAnalysisParameter(){
//		setUp();
//		EmissionsConfigGroup ecg = new EmissionsConfigGroup();
//		if ( (Boolean) true ==null ) {
//			ecg.setHbefaVehicleDescriptionSource( EmissionsConfigGroup.HbefaVehicleDescriptionSource.asEngineInformationAttributes );
//		} else if ( true ) {
//			ecg.setHbefaVehicleDescriptionSource( EmissionsConfigGroup.HbefaVehicleDescriptionSource.usingVehicleTypeId );
//		} else {
//			ecg.setHbefaVehicleDescriptionSource( EmissionsConfigGroup.HbefaVehicleDescriptionSource.fromVehicleTypeDescription );
//		}
//
//		WarmEmissionAnalysisModuleParameter weamp
//				= new WarmEmissionAnalysisModuleParameter(avgHbefaWarmTable, null, hbefaRoadTrafficSpeeds, pollutants, ecg);
//		Assert.assertEquals(weamp.getClass(), WarmEmissionAnalysisModuleParameter.class);
//		weamp = new WarmEmissionAnalysisModuleParameter(null, detailedHbefaWarmTable, hbefaRoadTrafficSpeeds, pollutants, ecg);
//		Assert.assertEquals(weamp.getClass(), WarmEmissionAnalysisModuleParameter.class);
//	}
	// that parameter object no longer exists.  kai, jan'20

	//@Test
	//public void testWarmEmissionAnalysisModule_exceptions(){

	/* out-dated
	 * the constructor aborts if either the
	 * warm emission analysis module parameter or
	 * the events mangager is null
	 * EmissionEfficiencyFactor = 'null' is allowed and therefore not tested here
	 */


	@Test(expected = RuntimeException.class)
	public void testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent2(){
		//-- set up tables, event handler, parameters, module
		setUp();

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

//			2020-02-21 19:55:37,509  WARN WarmEmissionAnalysisModule:403 did not find emission factor for efkey=PASSENGER_CAR; PM_non_exhaust; URB; STOPANDGO; PC petrol <1,4L <ECE; petrol (4S); <1,4L


			// test result:
			switch( this.emissionsComputationMethod ) {
				case StopAndGoFraction:
					//KMT Feb'20: Trying to understand the result:
					// - StopAndGo fraction is computed as 0 --> ONLY freeFlow values needed and looked up
					// - DETAILED FreeFlow value is available in the table (1.0E-4 g/km);  StopAndGo value is NOT available in the table
					// - AVERAGE  FreeFlow value is available in the table (1.0 g/km) ;    StopAndGo value is NOT available in the table ( 10.0 g/km)
					// --> It seems like it was intended (or only implemented) in a way, that if one of the detailed values is missing (FreeFlow or StopGo) there is a fallback to average. So the result of this would be 1.0 g/km * 0.1 km = 0.1 g/km
					// --> Now, after implementing the new fallback behaviour, it is looking up both values (FreeFlow or StopGo) ways independently from each other. Therefore the result comes from the detailed table (1.0E-4 g/km) * * 0.1 km = 1.0E-5 g/km
					// -----> We need a decision here, if we want allow that inconsistent(?) lookup of FreeFlow and Detailed values with different grade of detail or not.
					Assert.assertEquals( 0.1, warmEmissions.get( NMHC ), MatsimTestUtils.EPSILON );
					break;
				case AverageSpeed:
					Assert.assertEquals( DETAILED_PC_FACTOR_FF * pclinkLength / 1000., warmEmissions.get( NMHC ), MatsimTestUtils.EPSILON );
					break;
				default:
					throw new IllegalStateException( "Unexpected value: " + this.emissionsComputationMethod );
			}
			// yyyyyy The above are different for the different computation methods, but for the wrong reasons: In the stopGo case, the value is
			// not properly specified, and so some fall-back occurs, but in the stopGoFraction case, that fallback is also triggered here, while for
			// the averageSpeed case, that fallback is only triggered in the stopGo case (following below).  Also see comments elsewhere in
			// this method.  kai, jan'20

			// thow corresponding event:
			emissionsModule.throwWarmEmissionEvent( leaveTime, pclink.getId(), pcVehicleId, warmEmissions );
			// test resulting event:
			switch( emissionsComputationMethod ) {
				case StopAndGoFraction:
					Assert.assertEquals( 2.3, HandlerToTestEmissionAnalysisModules.getSum(), MatsimTestUtils.EPSILON ); //seems to be (0.1 * number of entries in enum Pollutant)
					break;
				case AverageSpeed:
					Assert.assertEquals( pollutants.size() * DETAILED_PC_FACTOR_FF * pclinkLength / 1000., HandlerToTestEmissionAnalysisModules.getSum(),
							MatsimTestUtils.EPSILON );
					break;
				default:
					throw new IllegalStateException( "Unexpected value: " + emissionsComputationMethod );
			}

			HandlerToTestEmissionAnalysisModules.reset();
			warmEmissions.clear();

			// yyyyyy (*) I haven't understood it yet.  My guess is that this is a really confusing test: With the "averageSpeed" computation
			// method, only the free flow hbefa value is pulled here, so it uses the "detailed" emissions value.  With the "stopGoFraction"
			// computation method, however, the stopgo hbefa value is _also_ pullsed (!), and thus it (how??) falls back to the average emissions
			// value.  :-( :-( :-( :-(  kai, jan'20
		}

		// sub case avg speed = stop go speed
		{
			warmEmissions = emissionsModule.checkVehicleInfoAndCalculateWarmEmissions( pcVehicle, pclink, pclinkLength / PCSG_VELOCITY_KMH * 3.6 );
			Assert.assertEquals( AVG_PC_FACTOR_SG * pclinkLength / 1000., warmEmissions.get( NMHC ), MatsimTestUtils.EPSILON );

			emissionsModule.throwWarmEmissionEvent( leaveTime, pclink.getId(), pcVehicleId, warmEmissions );
			Assert.assertEquals(
					pollutants.size() * AVG_PC_FACTOR_SG * pclinkLength / 1000., HandlerToTestEmissionAnalysisModules.getSum(),
					MatsimTestUtils.EPSILON );

			HandlerToTestEmissionAnalysisModules.reset();
			warmEmissions.clear();
		}
	}

	@Test(expected = RuntimeException.class)
	public void testCounters3(){
		setUp();
		emissionsModule.reset();

		// case 2 - free flow entry in both tables, stop go entry in average table -> use average
		Id<Vehicle> pcVehicleId = Id.create("vehicle 2", Vehicle.class);
		double pclinkLength= 20.*1000;
		Id<VehicleType> pcVehicleTypeId = Id.create( PASSENGER_CAR + ";"+ PC_TECHNOLOGY + ";"+ PC_SIZE_CLASS +";"+ PC_CONCEPT, VehicleType.class );
		VehiclesFactory vehFac = VehicleUtils.getFactory();
		Vehicle pcVehicle = vehFac.createVehicle(pcVehicleId, vehFac.createVehicleType(pcVehicleTypeId));
		Link pclink = TestWarmEmissionAnalysisModule.createMockLink("link 2", pclinkLength, PC_FREE_VELOCITY_KMH / 3.6 );

		// sub case: current speed equals free flow speed
		warmEmissions = emissionsModule.checkVehicleInfoAndCalculateWarmEmissions(pcVehicle,pclink, pclinkLength/ PC_FREE_VELOCITY_KMH *3.6 );
		Assert.assertEquals(0, emissionsModule.getFractionOccurences() );
		Assert.assertEquals(pclinkLength/1000, emissionsModule.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON );
		Assert.assertEquals(1, emissionsModule.getFreeFlowOccurences() );
		Assert.assertEquals(pclinkLength/1000, emissionsModule.getKmCounter(), MatsimTestUtils.EPSILON );
		Assert.assertEquals(0., emissionsModule.getStopGoKmCounter(), MatsimTestUtils.EPSILON );
		Assert.assertEquals(0, emissionsModule.getStopGoOccurences() );
		Assert.assertEquals(1, emissionsModule.getWarmEmissionEventCounter() );
		emissionsModule.reset();

		// sub case: current speed equals stop go speed
		warmEmissions = emissionsModule.checkVehicleInfoAndCalculateWarmEmissions(pcVehicle, pclink, pclinkLength/ PCSG_VELOCITY_KMH *3.6 );
		Assert.assertEquals(0, emissionsModule.getFractionOccurences() );
		Assert.assertEquals(0., emissionsModule.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON );
		Assert.assertEquals(0, emissionsModule.getFreeFlowOccurences() );
		Assert.assertEquals(pclinkLength/1000, emissionsModule.getKmCounter(), MatsimTestUtils.EPSILON );
		Assert.assertEquals(pclinkLength/1000, emissionsModule.getStopGoKmCounter(), MatsimTestUtils.EPSILON );
		Assert.assertEquals(1, emissionsModule.getStopGoOccurences() );
		Assert.assertEquals(1, emissionsModule.getWarmEmissionEventCounter() );
		emissionsModule.reset();

	}



	private void setUp() {

		Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> avgHbefaWarmTable = new HashMap<>();
		Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> detailedHbefaWarmTable = new HashMap<>();

		fillAverageTable( avgHbefaWarmTable );
		fillDetailedTable( detailedHbefaWarmTable );
		Map<HbefaRoadVehicleCategoryKey, Map<HbefaTrafficSituation, Double>> hbefaRoadTrafficSpeeds = EmissionUtils.createHBEFASpeedsTable(
				avgHbefaWarmTable );
		TestWarmEmissionAnalysisModule.addDetailedRecordsToTestSpeedsTable( hbefaRoadTrafficSpeeds, detailedHbefaWarmTable );

		EventsManager emissionEventManager = new HandlerToTestEmissionAnalysisModules();
		EmissionsConfigGroup ecg = new EmissionsConfigGroup();
		ecg.setHbefaVehicleDescriptionSource( EmissionsConfigGroup.HbefaVehicleDescriptionSource.usingVehicleTypeId );
		ecg.setEmissionsComputationMethod( this.emissionsComputationMethod );
		ecg.setDetailedVsAverageLookupBehavior( DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageThenAverageTable );

		emissionsModule = new WarmEmissionAnalysisModule( avgHbefaWarmTable, detailedHbefaWarmTable, hbefaRoadTrafficSpeeds, pollutants, emissionEventManager, ecg );

	}




	private void fillDetailedTable( Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> detailedHbefaWarmTable) {


		// entry for second test case "pc" -- should not be used
		{
			HbefaVehicleAttributes vehAtt = new HbefaVehicleAttributes();
			vehAtt.setHbefaTechnology( PC_TECHNOLOGY );
			vehAtt.setHbefaSizeClass( PC_SIZE_CLASS );
			vehAtt.setHbefaEmConcept( PC_CONCEPT );

			HbefaWarmEmissionFactor detWarmFactor = new HbefaWarmEmissionFactor();
			detWarmFactor.setWarmEmissionFactor( DETAILED_PC_FACTOR_FF );
			detWarmFactor.setSpeed( PC_FREE_VELOCITY_KMH );

			for( Pollutant wp : pollutants ){
				HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();
				detWarmKey.setHbefaComponent( wp );
				detWarmKey.setHbefaRoadCategory( HBEFA_ROAD_CATEGORY );
				detWarmKey.setHbefaTrafficSituation( HbefaTrafficSituation.FREEFLOW );
				detWarmKey.setHbefaVehicleAttributes( vehAtt );
				detWarmKey.setHbefaVehicleCategory( HbefaVehicleCategory.PASSENGER_CAR );
				detailedHbefaWarmTable.put( detWarmKey, detWarmFactor );
			}
		}

		// entry for ffOnlyTestcase
		{
			HbefaVehicleAttributes vehAtt = new HbefaVehicleAttributes();
			String ffOnlyConcept = "PC-D-Euro-3";
			vehAtt.setHbefaEmConcept( ffOnlyConcept );
			String ffOnlySizeClass = ">=2L";
			vehAtt.setHbefaSizeClass( ffOnlySizeClass );
			String ffOnlyTechnology = "diesel";
			vehAtt.setHbefaTechnology( ffOnlyTechnology );

			HbefaWarmEmissionFactor detWarmFactor = new HbefaWarmEmissionFactor();
			double detailedFfOnlyFactorFf = .0000001;
			detWarmFactor.setWarmEmissionFactor( detailedFfOnlyFactorFf );
			double ffOnlyffSpeed = 120.;
			detWarmFactor.setSpeed( ffOnlyffSpeed );

			for( Pollutant wp : pollutants ){
				HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();
				detWarmKey.setHbefaComponent( wp );
				detWarmKey.setHbefaRoadCategory( HBEFA_ROAD_CATEGORY );
				detWarmKey.setHbefaTrafficSituation( HbefaTrafficSituation.FREEFLOW );
				detWarmKey.setHbefaVehicleAttributes( vehAtt );
				detWarmKey.setHbefaVehicleCategory( HbefaVehicleCategory.PASSENGER_CAR );
				detailedHbefaWarmTable.put( detWarmKey, detWarmFactor );
			}
		}

		// entry for sgOnlyTestcase
		{
			HbefaVehicleAttributes vehAtt = new HbefaVehicleAttributes();
			String sgOnlyConcept = "PC-Alternative Fuel";
			vehAtt.setHbefaEmConcept( sgOnlyConcept );
			String sgOnlySizeClass = "not specified";
			vehAtt.setHbefaSizeClass( sgOnlySizeClass );
			String sgOnlyTechnology = "bifuel CNG/petrol";
			vehAtt.setHbefaTechnology( sgOnlyTechnology );

			HbefaWarmEmissionFactor detWarmFactor = new HbefaWarmEmissionFactor();
			double detailedSgOnlyFactorSg = .00000001;
			detWarmFactor.setWarmEmissionFactor( detailedSgOnlyFactorSg );
			double sgOnlysgSpeed = 50.;
			detWarmFactor.setSpeed( sgOnlysgSpeed );

			for( Pollutant wp : pollutants ){
				HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();
				detWarmKey.setHbefaComponent( wp );
				detWarmKey.setHbefaRoadCategory( HBEFA_ROAD_CATEGORY );
				detWarmKey.setHbefaTrafficSituation( HbefaTrafficSituation.STOPANDGO );
				detWarmKey.setHbefaVehicleAttributes( vehAtt );
				detWarmKey.setHbefaVehicleCategory( HbefaVehicleCategory.PASSENGER_CAR );
				detailedHbefaWarmTable.put( detWarmKey, detWarmFactor );
			}
		}

	}

}
	

	

