package playground.dziemke.ikea;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

public class CreatePopulationAndDemand {

	private final static Logger Log = Logger.getLogger(CreatePopulationAndDemand.class);
	private Scenario scenario;
	
	private String networkFile = "./input/network.xml";
	
	public static void main(String[] args) {
CreatePopulationAndDemand creator = new CreatePopulationAndDemand();
creator.run();
		}

	private void run() {
this.init();
CreatePopulation populationCreator = new CreatePopulation();
populationCreator.run(this.scenario);
CreateDemand demandCreator = new CreateDemand();
demandCreator.run(this.scenario, populationCreator.getHomeLocations());
	this.write();
	}


	private void init() {
// Create scenario
		Config config = ConfigUtils.createConfig();
this.scenario = ScenarioUtils.createScenario(config);

// Read network
new MatsimNetworkReader(this.scenario).readFile(networkFile);

	}

	private void write() {
PopulationWriter populationWriter = new PopulationWriter(this.scenario.getPopulation(), this.scenario.getNetwork());
populationWriter.write("./output/plans.xml.gz");
Log.info("Number of persons: " + this.scenario.getPopulation().getPersons().size());
	}
	
}
