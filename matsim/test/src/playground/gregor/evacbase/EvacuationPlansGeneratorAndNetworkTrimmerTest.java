package playground.gregor.evacbase;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Population;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkWriter;
import org.matsim.population.PopulationImpl;
import org.matsim.population.PopulationReaderMatsimV4;
import org.matsim.population.PopulationWriter;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.CRCChecksum;
import org.xml.sax.SAXException;

import playground.gregor.sims.evacbase.EvacuationAreaFileReader;
import playground.gregor.sims.evacbase.EvacuationAreaLink;
import playground.gregor.sims.evacbase.EvacuationPlansGeneratorAndNetworkTrimmer;

public class EvacuationPlansGeneratorAndNetworkTrimmerTest extends MatsimTestCase{

	public void testEvacuationPlansGeneratorAndNetworkTrimmer() {
		String config = getInputDirectory() + "config.xml";
		Config c = Gbl.createConfig(new String [] {config});
		String refPlans = getInputDirectory() + "evacuationplans.xml";
		String refNet = getInputDirectory() + "evacuationnetwork.xml";
		
		
		NetworkLayer net = new NetworkLayer();
		new MatsimNetworkReader(net).readFile(c.network().getInputFile());
				
		Population pop = new PopulationImpl();
		new PopulationReaderMatsimV4(pop,net).readFile(c.plans().getInputFile());
		
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
		
		new EvacuationPlansGeneratorAndNetworkTrimmer().generatePlans(pop, net, el1);
		
		new PopulationWriter(pop,c.plans().getOutputFile()).write();
		assertEquals("different plans-files.", CRCChecksum.getCRCFromFile(refPlans),	CRCChecksum.getCRCFromFile(c.plans().getOutputFile()));
		
		new NetworkWriter(net,c.network().getOutputFile()).write();
		assertEquals("different plans-files.", CRCChecksum.getCRCFromFile(refNet),	CRCChecksum.getCRCFromFile(c.network().getOutputFile()));
	}
}
