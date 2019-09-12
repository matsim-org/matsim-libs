package org.matsim.vehicles;

import org.apache.log4j.Logger;
import org.junit.*;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.io.*;
import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class VehicleWriteReadTest{
	private static final Logger log = Logger.getLogger( VehicleWriteReadTest.class ) ;

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;


	private static final String TESTXML_v1 = "testVehicles_v1_withDefaultValues.xml";
	private static final String OUTXML_v1 = "testOutputVehicles_v1.xml";

	private static final String TESTXML_v2 = "testVehicles_v2_withDefaultValues.xml";
	private static final String OUTXML_v2 = "testOutputVehicles_v2.xml";


	@Before
	public void setUp() throws IOException {
	}

	@Test
	public void v1_isWrittenCorrect () throws FileNotFoundException, IOException {
		//----- V1 --------
		//read it
		Vehicles vehicles1 = VehicleUtils.createVehiclesContainer();
		MatsimVehicleReader reader1 = new MatsimVehicleReader(vehicles1);
		final String inputFilename = utils.getClassInputDirectory() + TESTXML_v1;
		reader1.readFile( inputFilename );

		//write it
		VehicleWriterV1 writerV1 = new VehicleWriterV1(vehicles1);
		final String outputDirectory = utils.getOutputDirectory();
		final String outputFilename = outputDirectory + OUTXML_v1;
		writerV1.writeFile( outputFilename );

		MatsimTestUtils.compareFilesLineByLine( inputFilename, outputFilename );
	}

	@Test
	public void v2_isWrittenCorrect () throws FileNotFoundException, IOException {
		//----- V2 --------
		//read it
		Vehicles vehicles2 = VehicleUtils.createVehiclesContainer();
		MatsimVehicleReader reader2 = new MatsimVehicleReader(vehicles2);
		final String inFile = utils.getClassInputDirectory() + TESTXML_v2;
		reader2.readFile( inFile );

		//write it
		VehicleWriterV2 writerV2 = new VehicleWriterV2(vehicles2);
		final String outputDirectory = utils.getOutputDirectory();
		final String outFile = outputDirectory + OUTXML_v2;
		writerV2.writeFile( outFile );

		MatsimTestUtils.compareFilesLineByLine( inFile, outFile );

	}
}
