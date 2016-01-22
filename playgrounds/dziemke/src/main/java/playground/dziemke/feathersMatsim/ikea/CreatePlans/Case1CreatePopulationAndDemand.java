package playground.dziemke.feathersMatsim.ikea.CreatePlans;


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

public class Case1CreatePopulationAndDemand {

	private final static Logger Log = Logger.getLogger(Case1CreatePopulationAndDemand.class);
	private Scenario scenario;

	private String networkFile = "./input/network/merged-network_UTM31N.xml";
	private String dataFile ="./input/mergedPrdToAscii.csv";


	public static void main(String[] args) {
		Case1CreatePopulationAndDemand creator = new Case1CreatePopulationAndDemand();
		creator.run();
	}

	private void run() {
		this.init();
		CreatePopulation populationCreator = new CreatePopulation();
		populationCreator.run(this.scenario, dataFile);
		Case1CreateDemand demandCreator = new Case1CreateDemand();
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
		populationWriter.write("./output/Case1plans.xml.gz");
		Log.info("Number of persons: " + this.scenario.getPopulation().getPersons().size());
	}

}
