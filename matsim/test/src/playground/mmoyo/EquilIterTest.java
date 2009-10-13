package playground.mmoyo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestCase;
import org.xml.sax.SAXException;

import playground.mmoyo.equilDemo.MMoyoEquilnetDemo;

public class EquilIterTest extends MatsimTestCase {
	
	private static final String HISTOG_FILE = "1.legHistogram_pt.png";
	
	public void testEquil() throws IOException, JAXBException, SAXException, ParserConfigurationException{
		
		//test\input\playground\mmoyo\EquilIterTest\testEquil\1.legHistogram_pt.png
		String inputDirectory = getInputDirectory(); 
		String outputDirectory = "output/transitEquil2";
		String expectedFile =  inputDirectory + HISTOG_FILE;
		String outputFile = outputDirectory + "/ITERS/it.1/" + HISTOG_FILE;
		
		File directoryFile = new File(outputDirectory);
		if (directoryFile.exists()) {
			IOUtils.deleteDirectory(directoryFile);
		}
		
		MMoyoEquilnetDemo demo = new MMoyoEquilnetDemo();
		demo.run();
		
		BufferedReader expected = new BufferedReader(new FileReader(new File(expectedFile)));
		BufferedReader output = new BufferedReader(new FileReader(new File(outputFile)));
		assertNotNull(expected);
		assertNotNull(output);
		expected.close();
		output.close();
		assertEquals(CRCChecksum.getCRCFromFile(expectedFile), CRCChecksum.getCRCFromFile(outputFile));
		

		//compare event->
		String eventFile = inputDirectory + "1.events.xml.gz"; 
		BufferedReader events = new BufferedReader(new FileReader(new File(expectedFile)));
		assertNotNull(events);
		
		
	}
}
