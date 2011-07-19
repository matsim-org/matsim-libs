/*
package playground.mmoyo;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.matsim.core.utils.io.IOUtils;
import org.matsim.testcases.MatsimTestCase;
import org.xml.sax.SAXException;

public class EquilIterTest extends MatsimTestCase {
	
	private static final String OUT_PLANS_FILE = "output_plans.xml.gz";
	
	public void testEquil() throws IOException, JAXBException, SAXException, ParserConfigurationException{
		/*  TODO: Fix this call
		
		///playgrounds/mmoyo/test/input/playground/mmoyo/EquilIterTest/testEquil/output_plans.xml.gz
		String inputDirectory = "../playgrounds/mmoyo/test/input/playground/mmoyo/EquilIterTest/testEquil/";//getInputDirectory(); 
		String outputDirectory = "../playgrounds/mmoyo/output/transitEquil2";
		String expectedFile =  inputDirectory + OUT_PLANS_FILE;
		String outputFile = outputDirectory + "/" + OUT_PLANS_FILE;
		
		File directoryFile = new File(outputDirectory);
		if (directoryFile.exists()) {
			IOUtils.deleteDirectory(directoryFile);
		}
		
		
		MMoyoEquilnetDemo.main(new String[]{"NoOTFDemo"});
		
		BufferedReader expected = IOUtils.getBufferedReader(expectedFile);
		BufferedReader output = IOUtils.getBufferedReader(outputFile);
		assertNotNull(expected);
		assertNotNull(output);
		expected.close();
		output.close();
		assertEquals(CRCChecksum.getCRCFromFile(expectedFile), CRCChecksum.getCRCFromFile(outputFile));

		//compare event->
		String eventFile = inputDirectory + "ITERS/it.0/0.events.xml.gz"; 
		BufferedReader events = new BufferedReader(new FileReader(new File(expectedFile)));
		assertNotNull(events);
	}
}

*/