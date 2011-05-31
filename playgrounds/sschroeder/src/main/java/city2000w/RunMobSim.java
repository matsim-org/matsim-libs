package city2000w;

import gis.arcgis.NutsRegionShapeReader;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import kid.KiDDataReader;
import kid.KiDPlanAgentCreator;
import kid.ScheduledVehicles;
import kid.WIVERMobsimFactory;
import kid.filter.And;
import kid.filter.GeoRegionFilter;
import kid.filter.LogicVehicleFilter;
import kid.filter.StuttgartRegionFilter;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.opengis.feature.simple.SimpleFeature;

import city2000w.KiDDataRunner.MobileVehicleFilter;


public class RunMobSim implements StartupListener, BeforeMobsimListener{
	
	private static Logger logger = Logger.getLogger(RunMobSim.class);
	
	private static String NETWORK_FILENAME;
	
	private ScenarioImpl scenario;
	
	private KiDPlanAgentCreator planAgentCreator;
	
	private ScheduledVehicles scheduledVehicles;
	
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
		
		scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
		readNetwork(NETWORK_FILENAME);
		Controler controler = new Controler(scenario);
		
		/*
		 * muss ich auf 'false' setzen, da er mir sonst eine exception wirft, weil er das matsim-logo nicht finden kann
		 * ich hab keine ahnung wo ich den pfad des matsim-logos setzen kann
		 * 
		 */
		controler.setCreateGraphs(false);
		controler.addControlerListener(this);
		controler.setOverwriteFiles(true);
		
		controler.run();
	}

	private void init() {
		logger.info("initialise model");
		NETWORK_FILENAME = "../Diplomarbeit_Matthias/input/stuttgartNetwork.xml";
		
	}

	private void readNetwork(String networkFilename) {
		new MatsimNetworkReader(scenario).readFile(networkFilename);
		NetworkCleaner cleaner = new NetworkCleaner();
		cleaner.run(scenario.getNetwork());
	}

	public void notifyStartup(StartupEvent event) {
		scheduledVehicles = new ScheduledVehicles();
		createScheduledVehicles();
		planAgentCreator = new KiDPlanAgentCreator(scheduledVehicles);
		planAgentCreator.setNetwork(scenario.getNetwork());
		planAgentCreator.setRouter(event.getControler().createRoutingAlgorithm());
		WIVERMobsimFactory mobsimFactory = new WIVERMobsimFactory(0, planAgentCreator);
		mobsimFactory.setUseOTFVis(false);
		event.getControler().setMobsimFactory(mobsimFactory);
		
	}

	private void createScheduledVehicles() {
		logger.info("create scheduled vehicles from KiD");
		KiDDataReader kidReader = new KiDDataReader(scheduledVehicles);
		
		String directory = "/Volumes/projekte/2000-Watt-City/Daten/KiD/";
		kidReader.setVehicleFile(directory + "KiD_2002_Fahrzeug-Datei.txt");
		kidReader.setTransportChainFile(directory + "KiD_2002_Fahrtenketten-Datei.txt");
		kidReader.setTransportLegFile(directory + "KiD_2002_(Einzel)Fahrten-Datei.txt");
		LogicVehicleFilter andFilter = new And();
		andFilter.addFilter(new MobileVehicleFilter());
//		andFilter.addFilter(new LkwKleiner3Punkt5TFilter());
//		andFilter.addFilter(new BusinessSectorFilter());
		kidReader.setVehicleFilter(andFilter);
		List<SimpleFeature> regions = new ArrayList<SimpleFeature>();
		NutsRegionShapeReader regionReader = new NutsRegionShapeReader(regions, new StuttgartRegionFilter(), null);
		try {
			regionReader.read(directory + "regions_europe_wgsUtm32N.shp");
			kidReader.setScheduledVehicleFilter(new GeoRegionFilter(regions));
			kidReader.run();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			logger.error(e);
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			logger.error(e);
			System.exit(1);
		}
		
	}

	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		logger.info("create plan agents for mobsim");
		planAgentCreator.createPlanAgents();
	}

}
