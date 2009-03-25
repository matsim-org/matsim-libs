package playground.gregor.evacbase;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.CRCChecksum;
import org.xml.sax.SAXException;

import playground.gregor.sims.evacbase.EvacuationAreaFileReader;
import playground.gregor.sims.evacbase.EvacuationAreaFileWriter;
import playground.gregor.sims.evacbase.EvacuationAreaLink;

public class EvacuationAreaFileReaderWriterTest extends MatsimTestCase {

	public void testReadWriteEvacuationAreaFile() {
		String config = getInputDirectory() + "config.xml";
		Config c = Gbl.createConfig(new String [] {config});
		String outfile = getOutputDirectory() + "/evacuationArea.xml";
		String ref = getInputDirectory() + "/evacuationArea.xml";
		
		Map<Id,EvacuationAreaLink> el1 = new HashMap<Id, EvacuationAreaLink>();
		try {
			new EvacuationAreaFileReader(el1).readFile(c.evacuation().getEvacuationAreaFile());
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			new EvacuationAreaFileWriter(el1).writeFile(outfile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	
		assertEquals("different evac-area-files.", CRCChecksum.getCRCFromFile(ref),	CRCChecksum.getCRCFromFile(outfile));
	}
}
