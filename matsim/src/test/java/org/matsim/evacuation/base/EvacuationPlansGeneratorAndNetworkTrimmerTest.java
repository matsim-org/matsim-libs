package org.matsim.evacuation.base;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.basic.v01.IdImpl;
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

		ScenarioImpl scenario = new ScenarioImpl(c);
		NetworkLayer net = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(c.network().getInputFile());

		PopulationImpl pop = scenario.getPopulation();
		new MatsimPopulationReader(scenario).readFile(c.plans().getInputFile());

		new EvacuationNetGenerator(net,c).run();

		new EvacuationPlansGenerator(pop,net,net.getLinks().get(new IdImpl("el1"))).run();

		new PopulationWriter(pop, net).writeFile(c.plans().getOutputFile());
		assertEquals("different plans-files.", CRCChecksum.getCRCFromFile(refPlans),	CRCChecksum.getCRCFromFile(c.plans().getOutputFile()));

		new NetworkWriter(net).writeFile(c.network().getOutputFile());
		assertEquals("different network-files.", CRCChecksum.getCRCFromFile(refNet),	CRCChecksum.getCRCFromFile(c.network().getOutputFile()));
	}
}
