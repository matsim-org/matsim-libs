package playground.dziemke.feathersMatsim.ikea.CreatePlans;


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

public class Case2CreatePopulationAndDemand {

	private final static Logger Log = Logger.getLogger(Case2CreatePopulationAndDemand.class);
	private Scenario scenario;

	private String networkFile = "C:/Users/jeffw_000/Desktop/Dropbox/Uni/Master/Masterarbeit/MT/workspace new/ikeaStudy/input/network/merged-network_UTM31N_IKEA.xml";
	private String dataFile ="C:/Users/jeffw_000/Desktop/Dropbox/Uni/Master/Masterarbeit/MT/workspace new/ikeaStudy/input/prdToAsciiOwnDemand.csv";

	public static void main(String[] args) {
		Case2CreatePopulationAndDemand creator = new Case2CreatePopulationAndDemand();
		creator.run();
	}

	private void run() {
		this.init();
		CreatePopulation populationCreator = new CreatePopulation();
		populationCreator.run(this.scenario, dataFile);
		Case2CreateDemand demandCreator = new Case2CreateDemand();
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
		populationWriter.write("C:/Users/jeffw_000/Desktop/Dropbox/Uni/Master/Masterarbeit/MT/workspace new/ikeaStudy/output/Case2plans.xml.gz");
		Log.info("Number of persons: " + this.scenario.getPopulation().getPersons().size());
	}

}
