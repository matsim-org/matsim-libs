package org.matsim.contrib.freight.carrier;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleType;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CarrierVehicleTypeReaderTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	private static final Logger log = Logger.getLogger(CarrierVehicleTypeReaderTest.class) ;

	private CarrierVehicleTypes types;
	private String inFilename;

	@Before
	public void setUp() {
		types = new CarrierVehicleTypes();
		inFilename = utils.getClassInputDirectory() + "vehicleTypes.xml";
		new CarrierVehicleTypeReader(types).readFile( inFilename );
	}
	
	@Test
	public void test_whenReadingTypes_nuOfTypesIsReadCorrectly(){
		assertEquals(2, types.getVehicleTypes().size());
	}
	
	@Test
	public void test_whenReadingTypes_itReadyExactlyTheTypesFromFile(){
		assertTrue(types.getVehicleTypes().containsKey(Id.create("medium", org.matsim.vehicles.VehicleType.class ) ) );
		assertTrue(types.getVehicleTypes().containsKey(Id.create("light", org.matsim.vehicles.VehicleType.class ) ) );
		assertEquals(2, types.getVehicleTypes().size());
	}
	
	@Test
	public void test_whenReadingTypeMedium_itReadsDescriptionCorrectly(){
		VehicleType medium = types.getVehicleTypes().get(Id.create("medium", org.matsim.vehicles.VehicleType.class ) );
		assertEquals("Medium Vehicle", medium.getDescription());
	}

	@Test
	public void test_whenReadingTypeMedium_itReadsCapacityCorrectly(){
		VehicleType medium = types.getVehicleTypes().get(Id.create("medium", org.matsim.vehicles.VehicleType.class ) );
		assertEquals(30., (double) medium.getCapacity().getOther(), Double.MIN_VALUE );
	}
	
	@Test
	public void test_whenReadingTypeMedium_itReadsCostInfoCorrectly(){
		VehicleType medium = types.getVehicleTypes().get(Id.create("medium", org.matsim.vehicles.VehicleType.class ) );
		assertEquals(50.0, medium.getCostInformation().getFixedCosts(),0.01);
		assertEquals(0.4, medium.getCostInformation().getCostsPerMeter(),0.01);
		assertEquals(30.0, medium.getCostInformation().getCostsPerSecond(),0.01);
	}
	
	@Test
	public void test_whenReadingTypeMedium_itReadsEngineInfoCorrectly(){
		VehicleType medium = types.getVehicleTypes().get(Id.create("medium", org.matsim.vehicles.VehicleType.class ) );
		assertEquals(0.02, medium.getEngineInformation().getFuelConsumption(),0.01);
		assertEquals("gasoline", medium.getEngineInformation().getFuelType().toString());
	}

	@Test
	public void readV1andWriteV2(){
		final String outFilename = utils.getOutputDirectory() + "/vehicleTypes_v2.xml";
		new CarrierVehicleTypeWriter( types ).write( outFilename ) ;
		final String referenceFilename = utils.getClassInputDirectory() + "/vehicleTypes_v2.xml" ;
		MatsimTestUtils.compareFilesLineByLine( referenceFilename, outFilename );
	}

	@Test
	public void readV2andWriteV2() {
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

		MatsimTestUtils.compareFilesLineByLine( inFilename1, outFilename );
	}
}
