package org.matsim.vehicles;

import org.apache.log4j.Logger;
import org.junit.*;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.io.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class VehicleWriteReadTest{
	private static final Logger log = Logger.getLogger( VehicleWriteReadTest.class ) ;

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;


	private static final String TESTXML_v1 = "testVehicles_v1.xml";
	private static final String OUTXML_v1 = "testOutputVehicles_v1.xml";

	private static final String TESTXML_v2 = "testVehicles_v2.xml";
	private static final String OUTXML_v2 = "testOutputVehicles_v2.xml";


	@Before
	public void setUp() throws IOException {
	}

	@Test @Ignore
	public void v1_isWrittenCorrect () throws FileNotFoundException, IOException {
		//----- V1 --------
		//read it
		Vehicles vehicles1 = VehicleUtils.createVehiclesContainer();
		MatsimVehicleReader reader1 = new MatsimVehicleReader(vehicles1);
		reader1.readFile(utils.getPackageInputDirectory() + TESTXML_v1);

		//write it
		VehicleWriterV1 writerV1 = new VehicleWriterV1(vehicles1);
		final String outputDirectory = utils.getOutputDirectory();
		writerV1.writeFile( outputDirectory + OUTXML_v1 );

		assertTrue(new File( outputDirectory + OUTXML_v1).exists() );

		BufferedReader readerV1Input = IOUtils.getBufferedReader(utils.getPackageInputDirectory() + TESTXML_v1);
		BufferedReader readerV1Output = IOUtils.getBufferedReader( outputDirectory + OUTXML_v1 );

		String lineInput;
		String lineOutput;

		while (((lineInput = readerV1Input.readLine()) != null) && ((lineOutput = readerV1Output.readLine()) != null)) {
			log.info("Reading line... value in input file: " + lineInput + " , value in output File: "  + lineOutput);
			assertEquals("Lines have different content: ", lineInput, lineOutput);
		}
		readerV1Input.close();
		readerV1Output.close();
	}

	@Test @Ignore
	public void v2_isWrittenCorrect () throws FileNotFoundException, IOException {
		//----- V2 --------
		//read it
		Vehicles vehicles2 = VehicleUtils.createVehiclesContainer();
		MatsimVehicleReader reader2 = new MatsimVehicleReader(vehicles2);
		reader2.readFile(utils.getPackageInputDirectory() + TESTXML_v2);

		//write it
		VehicleWriterV2 writerV2 = new VehicleWriterV2(vehicles2);
		final String outputDirectory = utils.getOutputDirectory();
		writerV2.writeFile( outputDirectory + OUTXML_v2 );
		assertTrue(new File( outputDirectory + OUTXML_v2).exists() );

		BufferedReader readerV2Input = IOUtils.getBufferedReader(utils.getPackageInputDirectory() + TESTXML_v2);
		BufferedReader readerV2Output = IOUtils.getBufferedReader( outputDirectory + OUTXML_v2 );

		String lineInput;
		String lineOutput;

		while (((lineInput = readerV2Input.readLine()) != null) && ((lineOutput = readerV2Output.readLine()) != null)) {

			log.info("in/out:");
			log.info(lineInput);
			log.info(lineOutput);
			log.info("") ;
			assertEquals("Lines have different content: ", lineInput, lineOutput);
		}
		readerV2Input.close();
		readerV2Output.close();
	}
}
