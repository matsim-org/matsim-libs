package playground.dziemke.feathersMatsim.ikea.CreatePlans;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

public class Case0CreatePopulationAndDemand {

	private final static Logger Log = Logger.getLogger(Case0CreatePopulationAndDemand.class);
	private Scenario scenario;

	private String networkFile = "./input/network/merged-network_UTM31N.xml";
	private String dataFile ="./input/prdToAsciiRectifiedTAZ.csv";


	public static void main(String[] args) {
		Case0CreatePopulationAndDemand creator = new Case0CreatePopulationAndDemand();
		creator.run();
	}

	private void run() {
		this.init();
		CreatePopulation populationCreator = new CreatePopulation();
		populationCreator.run(this.scenario, dataFile);
		Case0CreateDemand demandCreator = new Case0CreateDemand();
		demandCreator.run(this.scenario, populationCreator.getHomeLocations(), dataFile);
		this.write();
	}


	private void init() {
		// Create scenario
		Config config = ConfigUtils.createConfig();
		this.scenario = ScenarioUtils.createScenario(config);

		// Read network
		new MatsimNetworkReader(this.scenario.getNetwork()).readFile(networkFile);

	}

	private void write() {
		PopulationWriter populationWriter = new PopulationWriter(this.scenario.getPopulation(), this.scenario.getNetwork());
		populationWriter.write("./output/Case0plans.xml.gz");
		Log.info("Number of persons: " + this.scenario.getPopulation().getPersons().size());
	}

}
