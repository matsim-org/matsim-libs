package playground.fhuelsmann.emission.analysis;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.benjamin.events.emissions.EmissionEventsReader;



public class CongestionAndEmissionComparison {
	private static final Logger logger = Logger.getLogger(CongestionAndEmissionComparison.class);
	private final static String runDirectory = "../../run980/";
	private final static String netFile = runDirectory + "980.output_network.xml.gz";
	
	private final static String plansFile = runDirectory + "980.output_plans.xml.gz";
	private final static String emissionFile = runDirectory + "emission.events.xml.gz";
	private final static String eventsFile = runDirectory + "ITERS/it.1000/980.1000.events.xml.gz";
	
	private final String configFile = runDirectory + "980.output_config.xml.gz";
	
	private static Scenario scenario;
	
	static int noOfTimeBins = 30;
	double simulationEndTime;
	
	public CongestionAndEmissionComparison(){
		Config config = ConfigUtils.createConfig();
		this.scenario = ScenarioUtils.createScenario(config);
	}
	
	public static void main(String[] args) {
		CongestionAndEmissionComparison congestionAndEmissionComparison = new CongestionAndEmissionComparison();
		congestionAndEmissionComparison.run(args);		
	}
	
	private void run (String[] args){
		loadScenario();
		Network network = scenario.getNetwork();
		this.simulationEndTime = getEndTime(configFile);
	
	EventsManager eventsManager = EventsUtils.createEventsManager();
	EventsManager emissioneventsManager = EventsUtils.createEventsManager();
	
	
	Congestion cong = new Congestion(network, this.simulationEndTime, noOfTimeBins);
	EmissionsPerLinkWarmEventHandler warmHandler = new EmissionsPerLinkWarmEventHandler(this.simulationEndTime, noOfTimeBins);
	
	eventsManager.addHandler(cong);
	emissioneventsManager.addHandler(warmHandler);
	
	MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
	reader.readFile(eventsFile);	
	
	EmissionEventsReader emissionReader = new EmissionEventsReader(emissioneventsManager);
	emissionReader.parse(this.emissionFile);
	}
	
	private void loadScenario() {
		Config config = scenario.getConfig();
		config.network().setInputFile(netFile);
		ScenarioLoaderImpl scenarioLoader = new ScenarioLoaderImpl(scenario);
		scenarioLoader.loadScenario();
	}
	
	private Double getEndTime(String configfile) {
		Config config = new Config();
		config.addCoreModules();
		MatsimConfigReader configReader = new MatsimConfigReader(config);
		configReader.readFile(configfile);
		Double endTime = config.getQSimConfigGroup().getEndTime();
		logger.info("Simulation end time is: " + endTime / 3600 + " hours.");
		logger.info("Aggregating emissions for " + (int) (endTime / 3600 / noOfTimeBins) + " hour time bins.");
		return endTime;
	}
		
}
