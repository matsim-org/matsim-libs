package playground.dziemke.ikea;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

public class CreatePopulationAndDemandIKEA {

	private final static Logger Log = Logger.getLogger(CreatePopulationAndDemandIKEA.class);
	private Scenario scenario;
	
	private String networkFile = "./input/merged-network_UTM31N_IKEA_2.xml";
	
	public static void main(String[] args) {
CreatePopulationAndDemandIKEA creator = new CreatePopulationAndDemandIKEA();
creator.run();
		}

	private void run() {
this.init();
CreatePopulation populationCreator = new CreatePopulation();
populationCreator.run(this.scenario);
CreateDemandIKEA demandCreator = new CreateDemandIKEA();
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
populationWriter.write("./output/plansIKEA/plans.xml.gz");
Log.info("Number of persons: " + this.scenario.getPopulation().getPersons().size());
	}
	
}
