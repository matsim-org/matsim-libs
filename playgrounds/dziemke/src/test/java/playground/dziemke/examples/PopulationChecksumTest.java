package playground.dziemke.examples;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.EventsFileComparator;

public class PopulationChecksumTest {
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();
	
	

	@SuppressWarnings("static-method")
	@Test
	public final void testMain() throws IOException {
		
//		writeDummyFileToTestWhereOutputGoes("test/input/scenarios/mstm_annapolis/hello.csv");
		
//		utils.getInputDirectory();
//		
//		String inputDirectory = utils.getInputDirectory() + "/../../../../../../../../matsim/examples/equil/plans100.xml";
		String inputDirectory = utils.getInputDirectory();
		System.out.println(inputDirectory);
		
		new File(inputDirectory + "/bla.csv").createNewFile();
		
		new File(inputDirectory + "/../bla2.csv").createNewFile();
		
		long checksum_ref = CRCChecksum.getCRCFromFile(inputDirectory + "/bla.csv");
		
//		long checksum_ref = CRCChecksum.getCRCFromFile(utils.getInputDirectory() + "/plans100.xml");
//		long checksum_ref = CRCChecksum.getCRCFromFile(utils.getInputDirectory() + "/../../../../../../../../matsim/examples/equil/plans100.xml");
		
//		long checksum_ref = CRCChecksum.getCRCFromFile("./compare/population_2001.xml");
//		long checksum_run = CRCChecksum.getCRCFromFile("./additional_inout/population_2001.xml");
//		assertEquals(checksum_ref, checksum_run);
		
//		assertEquals("different event files.", EventsFileComparator.compare("./compare/run_14_2001.0.events.xml.gz", 
//				"./matsim/run_14_2001/ITERS/it.0/run_14_2001.0.events.xml.gz"), 0);
	}

}
