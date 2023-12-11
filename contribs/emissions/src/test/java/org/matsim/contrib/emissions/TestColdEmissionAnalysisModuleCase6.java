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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.*;

import static org.matsim.contrib.emissions.HbefaVehicleCategory.HEAVY_GOODS_VEHICLE;
import static org.matsim.contrib.emissions.HbefaVehicleCategory.PASSENGER_CAR;


/**
 * @author julia
 * <p>
 * test for playground.vsp.emissions.ColdEmissionAnalysisModule
 * <p>
 * ColdEmissionAnalysisModule (ceam)
 * public methods and corresponding tests:
 * ceam module parameter - implicitly tested in calculateColdEmissionAndThrowEventTest, rescaleColdEmissionTest
 * ceam - constructor, nothing to test
 * reset - nothing to test
 * calculate cold emissions and throw event - calculateColdEmissionsAndThrowEventTest
 * <p>
 * private methods and corresponding tests:
 * rescale cold emissions - rescaleColdEmissionsTest
 * calculate cold emissions - implicitly tested in calculateColdEmissionAndThrowEventTest, rescaleColdEmissionTest
 * convert string to tuple - implicitly tested in calculateColdEmissionAndThrowEventTest, rescaleColdEmissionTest
 * <p>
 * in all cases the needed tables are created manually by the setUp() method
 * see test methods for details on the particular test cases
 */

public class TestColdEmissionAnalysisModuleCase6 {
	private static final Logger logger = LogManager.getLogger(TestColdEmissionAnalysisModuleCase6.class);

	private static final Double parkingDuration = 1.;
	private static final int tableAccDistance = 1;
	private static final Set<Pollutant> pollutants = new HashSet<>(Arrays.asList(Pollutant.values()));
	// strings for test cases
	
	// The material below was confused in the way that strings like "petrol" or "diesel" were given for the
	// size classes, and "<1,4L" or ">=2L" for the emissions concept.  Tried to make it consistent,
	// but I don't know if it is still testing the original functionality.  kai, jul'18
	
	// case: complete data - corresponding entry in average table
	private static final String petrol_technology = "petrol";
	private static final String none_sizeClass = "average";
	private static final String none_emConcept = "average";

	// case: complete data corresponding entries in average and detailed table - use detailed
	private static final String diesel_technology = "diesel";
	private static final String geq2l_sizeClass = ">=2L";
	private static final String PC_D_Euro_3_emConcept = "PC-D-Euro-3";

	
	// emission factors for tables - no dublicates!
	private static final Double detailedPetrolFactor = 100.;
	private static final Double detailedDieselFactor = 10.;
	private static final Double averageAverageFactor = .1;
	private static final Double averagePetrolFactor = .01;

	private static final double heavyGoodsFactor = -1.;


	@Test
	void calculateColdEmissionsAndThrowEventTest_completeData() {

		/*
		 * six test cases with complete input data
		 * or input that should be assigned to average/default cases
		 */

		ColdEmissionAnalysisModule coldEmissionAnalysisModule =setUp();
		ArrayList<Object> testCase6 = new ArrayList<>();

		// sixth case: heavy goods vehicle
		// -> throw warning -> use detailed or average table for passenger cars
		String heavygoodsvehicle = "HEAVY_GOODS_VEHICLE";
		Collections.addAll( testCase6, heavygoodsvehicle, petrol_technology, none_sizeClass, none_emConcept, heavyGoodsFactor);

		logger.info("Running testcase: " + testCase6 + " " +testCase6.toString());
		Id<Link> linkId = Id.create( "linkId" + testCase6, Link.class );
		Id<Vehicle> vehicleId = Id.create( "vehicleId" + testCase6, Vehicle.class );
		Id<VehicleType> vehicleTypeId = Id.create( testCase6.get( 0 ) + ";" + testCase6.get( 1 ) + ";" + testCase6.get( 2 ) + ";" + testCase6.get( 3 ), VehicleType.class );
			
		Vehicle vehicle = VehicleUtils.getFactory().createVehicle( vehicleId, VehicleUtils.getFactory().createVehicleType( vehicleTypeId ) );
		logger.info("VehicleId: " + vehicle.getId().toString());
		logger.info("VehicleTypeId: " + vehicle.getType().getId());

		Map<Pollutant, Double> calculatedPollutants = coldEmissionAnalysisModule.checkVehicleInfoAndCalculateWColdEmissions(vehicle.getType(), vehicle.getId(), linkId, 0.0, parkingDuration, tableAccDistance);
		double sumOfEmissions = calculatedPollutants.values().stream().mapToDouble(Double::doubleValue).sum();

		String message = "The expected emissions for " + testCase6.toString() + " are " + pollutants.size() * (Double) testCase6.get( 4 ) + " but were " + sumOfEmissions;
		Assertions.assertEquals( pollutants.size() * (Double) testCase6.get( 4 ), sumOfEmissions, MatsimTestUtils.EPSILON, message );
	}
	
	private ColdEmissionAnalysisModule setUp() {
		Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> avgHbefaColdTable = new HashMap<>();
		Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> detailedHbefaColdTable = new HashMap<>();
		
		fillAveragesTable( avgHbefaColdTable );
		fillDetailedTable( detailedHbefaColdTable );
		
		EventsManager emissionEventManager = new HandlerToTestEmissionAnalysisModules();
		EmissionsConfigGroup ecg = new EmissionsConfigGroup();
		ecg.setHbefaVehicleDescriptionSource( EmissionsConfigGroup.HbefaVehicleDescriptionSource.usingVehicleTypeId );

		return new ColdEmissionAnalysisModule( avgHbefaColdTable, detailedHbefaColdTable, ecg, pollutants, emissionEventManager );
	}
	
	private static void fillDetailedTable( Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> detailedHbefaColdTable ) {
		// create all needed and one unneeded entry for the detailed table
		{
			// add passenger car entry "diesel;>=2L;PC-D-Euro-3":
			HbefaVehicleAttributes vehAtt = ColdEmissionAnalysisModule.createHbefaVehicleAttributes( diesel_technology, geq2l_sizeClass, PC_D_Euro_3_emConcept );
			putIntoHbefaColdTable( detailedHbefaColdTable, vehAtt, new HbefaColdEmissionFactor( detailedDieselFactor ), PASSENGER_CAR );
		}
		{
			// add heavy goods vehicle entry "petrol;none;none":
			//(pre-existing comment: HEAVY_GOODS_VEHICLE;PC petrol;petrol; --> Should be used, since HGV shpuld be supported and not fallback to average any more, kmt apr'20.
			HbefaVehicleAttributes vehAtt = ColdEmissionAnalysisModule.createHbefaVehicleAttributes( petrol_technology, none_sizeClass, none_emConcept );
			putIntoHbefaColdTable( detailedHbefaColdTable, vehAtt, new HbefaColdEmissionFactor(heavyGoodsFactor), HEAVY_GOODS_VEHICLE );
		}
		{
			// add passenger car entry "petrol;none;nullCase":
			// (pre-existing comment: "PASSENGER_CAR;PC petrol;petrol;nullCase" --???)
			HbefaVehicleAttributes vehAtt = ColdEmissionAnalysisModule.createHbefaVehicleAttributes( petrol_technology, none_sizeClass, none_emConcept );
			// (this is for a test of what happens when the setter is not explicitly used.  This should go away
			// when the now deprecated execution path goes away.  kai, jul'18)
			putIntoHbefaColdTable( detailedHbefaColdTable, vehAtt, new HbefaColdEmissionFactor( detailedPetrolFactor ), PASSENGER_CAR );
		}
	}
	
	private static void fillAveragesTable( Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> avgHbefaColdTable ) {

		// create all needed and one unneeded entry for the average table

		{
			// add passenger car entry "average;average;average":
			HbefaVehicleAttributes vehAtt = ColdEmissionAnalysisModule.createHbefaVehicleAttributes( "average", "average", "average" ) ;
			putIntoHbefaColdTable( avgHbefaColdTable, vehAtt, new HbefaColdEmissionFactor(averageAverageFactor), PASSENGER_CAR );
		}
		{
			HbefaVehicleAttributes vehAtt = ColdEmissionAnalysisModule.createHbefaVehicleAttributes( petrol_technology, none_sizeClass, none_emConcept );
			putIntoHbefaColdTable( avgHbefaColdTable, vehAtt, new HbefaColdEmissionFactor( averagePetrolFactor ), PASSENGER_CAR );
		}
		{
			// duplicate from detailed table, but with different emission factor.
			// this should not be used but is needed to assure that the detailed table is tried before the average table
			HbefaVehicleAttributes vehAtt = ColdEmissionAnalysisModule.createHbefaVehicleAttributes( diesel_technology, geq2l_sizeClass, PC_D_Euro_3_emConcept );
			putIntoHbefaColdTable( avgHbefaColdTable, vehAtt, new HbefaColdEmissionFactor(heavyGoodsFactor), PASSENGER_CAR );
		}
		{
			// add HGV entry "petrol;none;none".
			// (pre-existing comment: HEAVY_GOODS_VEHICLE;PC petrol;petrol;none should not be used --???)
			final HbefaVehicleAttributes vehAtt = ColdEmissionAnalysisModule.createHbefaVehicleAttributes( petrol_technology, none_sizeClass, none_emConcept );
			
			putIntoHbefaColdTable( avgHbefaColdTable, vehAtt, new HbefaColdEmissionFactor(heavyGoodsFactor), HEAVY_GOODS_VEHICLE );
		}
	}
	
	private static void putIntoHbefaColdTable( final Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> detailedHbefaColdTable, final HbefaVehicleAttributes vehAtt, final HbefaColdEmissionFactor detColdFactor, final HbefaVehicleCategory hbefaVehicleCategory ) {
		for ( Pollutant cp : pollutants ) {
			HbefaColdEmissionFactorKey detColdKey = new HbefaColdEmissionFactorKey();
			detColdKey.setDistance(tableAccDistance);
			detColdKey.setParkingTime((int) Math.round( parkingDuration ));
			detColdKey.setVehicleAttributes(vehAtt);
			detColdKey.setVehicleCategory(hbefaVehicleCategory);
			detColdKey.setComponent(cp);
			detailedHbefaColdTable.put(detColdKey, detColdFactor);
		}
	}
	
}
