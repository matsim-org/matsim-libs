package org.matsim.evacuation.base;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.evacuation.base.EvacuationAreaFileReader;
import org.matsim.evacuation.base.EvacuationAreaLink;
import org.matsim.evacuation.base.EvacuationNetGenerator;
import org.matsim.evacuation.base.EvacuationPlansGenerator;
import org.matsim.testcases.MatsimTestCase;
import org.xml.sax.SAXException;


public class EvacuationPlansGeneratorAndNetworkTrimmerTest extends MatsimTestCase{

	public void testEvacuationPlansGeneratorAndNetworkTrimmer() {
		String config = getInputDirectory() + "config.xml";
		Config c = Gbl.createConfig(new String [] {config});
		String refPlans = getInputDirectory() + "evacuationplans.xml";
		String refNet = getInputDirectory() + "evacuationnetwork.xml";
		
		
		NetworkLayer net = new NetworkLayer();
		new MatsimNetworkReader(net).readFile(c.network().getInputFile());
				
		Population pop = new PopulationImpl();
		new MatsimPopulationReader(pop,net).readFile(c.plans().getInputFile());
		
		new EvacuationNetGenerator(net,c).run();
		
		new EvacuationPlansGenerator(pop,net,net.getLink("el1")).run();
		
		new PopulationWriter(pop,c.plans().getOutputFile()).write();
		assertEquals("different plans-files.", CRCChecksum.getCRCFromFile(refPlans),	CRCChecksum.getCRCFromFile(c.plans().getOutputFile()));
		
		new NetworkWriter(net,c.network().getOutputFile()).write();
		assertEquals("different network-files.", CRCChecksum.getCRCFromFile(refNet),	CRCChecksum.getCRCFromFile(c.network().getOutputFile()));
	}
}
