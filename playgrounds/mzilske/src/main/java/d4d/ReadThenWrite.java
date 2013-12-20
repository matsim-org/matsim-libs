package d4d;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.PopulationReaderMatsimV5;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

public class ReadThenWrite {

	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		config.controler().setLastIteration(0);
		config.global().setCoordinateSystem("EPSG:3395");
		ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class).setShowTeleportedAgents(true);
		config.controler().setWriteEventsInterval(0);
		config.network().setInputFile("/Users/zilske/d4d/output/network.xml");
		config.plans().setInputFile("/Users/zilske/d4d/output/population.xml");
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario).parse("/Users/zilske/d4d/output/network.xml");
		new PopulationReaderMatsimV5(scenario).readFile("/Users/zilske/d4d/output/population10.xml");
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write("/Users/zilske/d4d/output/population2.xml");
		
		
//		new MatsimNetworkReader(scenario).parse("examples/equil/network.xml");
//		new MatsimPopulationReader(scenario).readFile("examples/equil/plans100.xml");
//		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write("examples/equil/plans_out.xml");
	}
	
}
