/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
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

package org.matsim.freight.carriers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CarrierVehicleTypeReaderTest {
	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils() ;

	private static final Logger log = LogManager.getLogger(CarrierVehicleTypeReaderTest.class) ;

	private CarrierVehicleTypes types;
	private String inFilename;

	@BeforeEach
	public void setUp() {
		types = new CarrierVehicleTypes();
		inFilename = utils.getClassInputDirectory() + "vehicleTypes.xml";
		new CarrierVehicleTypeReader(types).readFile( inFilename );
	}

	@Test
	void test_whenReadingTypes_nuOfTypesIsReadCorrectly(){
		assertEquals(2, types.getVehicleTypes().size());
	}

	@Test
	void test_whenReadingTypes_itReadyExactlyTheTypesFromFile(){
		assertTrue(types.getVehicleTypes().containsKey(Id.create("medium", org.matsim.vehicles.VehicleType.class ) ) );
		assertTrue(types.getVehicleTypes().containsKey(Id.create("light", org.matsim.vehicles.VehicleType.class ) ) );
		assertEquals(2, types.getVehicleTypes().size());
	}

	@Test
	void test_whenReadingTypeMedium_itReadsDescriptionCorrectly(){
		VehicleType medium = types.getVehicleTypes().get(Id.create("medium", org.matsim.vehicles.VehicleType.class ) );
		assertEquals("Medium Vehicle", medium.getDescription());
	}

	@Test
	void test_whenReadingTypeMedium_itReadsCapacityCorrectly(){
		VehicleType medium = types.getVehicleTypes().get(Id.create("medium", org.matsim.vehicles.VehicleType.class ) );
		assertEquals(30., (double) medium.getCapacity().getOther(), Double.MIN_VALUE );
	}

	@Test
	void test_whenReadingTypeMedium_itReadsCostInfoCorrectly(){
		VehicleType medium = types.getVehicleTypes().get(Id.create("medium", org.matsim.vehicles.VehicleType.class ) );
		assertEquals(50.0, medium.getCostInformation().getFixedCosts(),0.01 );
		assertEquals(0.4, medium.getCostInformation().getCostsPerMeter(),0.01 );
		assertEquals(30.0, medium.getCostInformation().getCostsPerSecond(),0.01 );
	}

	@Test
	void test_whenReadingTypeMedium_itReadsEngineInfoCorrectly(){
		VehicleType medium = types.getVehicleTypes().get(Id.create("medium", org.matsim.vehicles.VehicleType.class ) );
		assertEquals(0.02, medium.getEngineInformation().getFuelConsumption(),0.01);
		assertEquals("gasoline", medium.getEngineInformation().getFuelType().toString());
	}

	@Test
	void readV1andWriteV2(){
		final String outFilename = utils.getOutputDirectory() + "/vehicleTypes_v2.xml";
		new CarrierVehicleTypeWriter( types ).write( outFilename ) ;
		final String referenceFilename = utils.getClassInputDirectory() + "/vehicleTypes_v2.xml" ;
		MatsimTestUtils.assertEqualFilesLineByLine( referenceFilename, outFilename );
	}

	@Test
	void readV2andWriteV2() {
		// yyyyyy FIXME because of "setUp" this will be doing an irrelevant read first.
		log.info("") ;
		log.info("") ;
		log.info("") ;
		log.info("") ;
		log.info("now starting for real") ;
		log.info("") ;
		String inFilename1 = utils.getClassInputDirectory() + "vehicleTypes_v2.xml";;
		CarrierVehicleTypes types1 = new CarrierVehicleTypes();
		new CarrierVehicleTypeReader( types1 ).readFile( inFilename1 );

		final String outFilename = utils.getOutputDirectory() + "/vehicleTypes_v2.xml";
		new CarrierVehicleTypeWriter( types1 ).write( outFilename ) ;

		MatsimTestUtils.assertEqualFilesLineByLine( inFilename1, outFilename );
	}
}
