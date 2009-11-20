package org.matsim.evacuation.base;

import org.matsim.core.config.Config;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestCase;


public class EvacuationPlansGeneratorAndNetworkTrimmerTest extends MatsimTestCase{

	public void testEvacuationPlansGeneratorAndNetworkTrimmer() {
		String config = getInputDirectory() + "config.xml";
		Config c = super.loadConfig(config);
		String refPlans = getInputDirectory() + "evacuationplans.xml";
		String refNet = getInputDirectory() + "evacuationnetwork.xml";
		
		
		NetworkLayer net = new NetworkLayer();
		new MatsimNetworkReader(net).readFile(c.network().getInputFile());
				
		PopulationImpl pop = new PopulationImpl();
		new MatsimPopulationReader(pop,net).readFile(c.plans().getInputFile());
		
		new EvacuationNetGenerator(net,c).run();
		
		new EvacuationPlansGenerator(pop,net,net.getLink("el1")).run();
		
		new PopulationWriter(pop).writeFile(c.plans().getOutputFile());
		assertEquals("different plans-files.", CRCChecksum.getCRCFromFile(refPlans),	CRCChecksum.getCRCFromFile(c.plans().getOutputFile()));
		
		new NetworkWriter(net).writeFile(c.network().getOutputFile());
		assertEquals("different network-files.", CRCChecksum.getCRCFromFile(refNet),	CRCChecksum.getCRCFromFile(c.network().getOutputFile()));
	}
}
