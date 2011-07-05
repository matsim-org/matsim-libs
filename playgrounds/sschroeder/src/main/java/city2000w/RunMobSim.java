package city2000w;

import kid.KiDPlanAgentCreator;
import kid.WIVERMobsimFactory;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.SimulationConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;


public class RunMobSim implements StartupListener, BeforeMobsimListener{
	
	private static Logger logger = Logger.getLogger(RunMobSim.class);
	
	private static String NETWORK_FILENAME;
	
	private static String PLAN_FILENAME;
	
	private ScenarioImpl scenario;
	
	private KiDPlanAgentCreator planAgentCreator;
	
	public static void main(String[] args) {
		RunMobSim mobSim = new RunMobSim();
		mobSim.run();
	}
	
	private void run(){
		logger.info("run");
		init();
		Config config = new Config();
		config.addCoreModules();
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(0);
		config.addSimulationConfigGroup(new SimulationConfigGroup());
		config.simulation().setEndTime(12*3600);
		scenario = (ScenarioImpl) ScenarioUtils.loadScenario(config);
		readNetwork(NETWORK_FILENAME);
		readPopulation(PLAN_FILENAME);
		Controler controler = new Controler(scenario);
		controler.setCreateGraphs(false);
		controler.addControlerListener(this);
		controler.setOverwriteFiles(true);
		
		controler.run();
	}

	private void init() {
		logger.info("initialise model");
		NETWORK_FILENAME = "../playgrounds/sschroeder/networks/berlinRoads.xml";
		PLAN_FILENAME = "../playgrounds/sschroeder/output/berlinPlans.xml";
	}

	private void readNetwork(String networkFilename) {
		new MatsimNetworkReader(scenario).readFile(networkFilename);
	}

	public void notifyStartup(StartupEvent event) {
		planAgentCreator = new KiDPlanAgentCreator(scenario.getPopulation());
		WIVERMobsimFactory mobsimFactory = new WIVERMobsimFactory(0, planAgentCreator);
		mobsimFactory.setUseOTFVis(false);
		event.getControler().setMobsimFactory(mobsimFactory);
	}
	
	private void readPopulation(String filename){
		MatsimPopulationReader reader = new MatsimPopulationReader(scenario);
		reader.readFile(PLAN_FILENAME);
	}

	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		event.getControler().getEvents().removeHandler(event.getControler().getPlansScoring().getPlanScorer());
		
		
	}

}
