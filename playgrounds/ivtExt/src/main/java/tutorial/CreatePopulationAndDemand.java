package tutorial;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

public class CreatePopulationAndDemand {
	
	private final static Logger log = Logger.getLogger(CreatePopulationAndDemand.class);
	private Scenario scenario;
	
	private String facilitiesFile = "./input/facilities.xml.gz";
	private String networkFile = "./input/network.xml";
	
	// --------------------------------------------------------------------------
	public static void main(String[] args) {
		CreatePopulationAndDemand creator = new CreatePopulationAndDemand();
		creator.run();		
	}
	
	private void run() {
		this.init();
		CreatePopulation populationCreator = new CreatePopulation();
		populationCreator.run(this.scenario);
		CreateDemand demandCreator = new CreateDemand();
		demandCreator.run(this.scenario, populationCreator.getPersonHomeAndWorkLocations());
		this.write();
	}
	
	private void init() {
		/*
		 * Create the scenario
		 */
		Config config = ConfigUtils.createConfig();
		this.scenario = ScenarioUtils.createScenario(config);
		/*
		 * Read the network and store it in the scenario
		 */
		new MatsimNetworkReader(this.scenario).readFile(networkFile);
		/*
		 * Read the facilities and store them in the scenario
		 */
		new FacilitiesReaderMatsimV1((ScenarioImpl)this.scenario).readFile(this.facilitiesFile);	
	}
	
	private void write() {
		PopulationWriter populationWriter = new PopulationWriter(this.scenario.getPopulation(), this.scenario.getNetwork());
		populationWriter.write("./output/plans.xml.gz");
		log.info("Number of persons: " + this.scenario.getPopulation().getPersons().size());
	}
}
