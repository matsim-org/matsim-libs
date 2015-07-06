package tutorial.programming.demandGenerationWithFacilities;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.FacilitiesReaderMatsimV1;

public class RunCreatePopulationAndDemand {
	
	private final static Logger log = Logger.getLogger(RunCreatePopulationAndDemand.class);
	private Scenario scenario;
	
	private String facilitiesFile = "./input/facilities.xml.gz";
	private String networkFile = "./input/network.xml";
	
	// --------------------------------------------------------------------------
	public static void main(String[] args) {
		RunCreatePopulationAndDemand creator = new RunCreatePopulationAndDemand();
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
