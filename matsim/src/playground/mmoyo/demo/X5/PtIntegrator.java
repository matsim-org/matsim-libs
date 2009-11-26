package playground.mmoyo.demo.X5;

import java.io.IOException;
import java.util.EnumSet;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.EventsManagerFactoryImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.Module;
import org.matsim.core.config.groups.ControlerConfigGroup.EventsFileFormat;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.algorithms.EventWriterTXT;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.pt.queuesim.TransitQueueSimulation;
import org.matsim.pt.routes.ExperimentalTransitRouteFactory;
import org.matsim.pt.utils.CreatePseudoNetwork;
import org.matsim.pt.utils.CreateVehiclesForSchedule;
import org.matsim.transitSchedule.TransitScheduleReaderV1;
import org.matsim.transitSchedule.api.TransitScheduleWriter;
import org.xml.sax.SAXException;

import playground.marcel.OTFDemo;
import playground.marcel.pt.application.DataPrepare;
import playground.marcel.pt.config.TransitConfigGroup;
import playground.marcel.pt.utils.MergeNetworks;
import playground.mmoyo.TransitSimulation.MMoyoPlansCalcTransitRoute;
import playground.marcel.pt.router.PlansCalcTransitRoute;


public class PtIntegrator {

	private static final Logger log = Logger.getLogger(DataPrepare.class);

	private static final String SERVERNAME = "ScenarioPlayer";
	
	private final static String NETWORK_FILE = "src/playground/mmoyo/demo/X5/network.xml";
	private final static String INPUT_PLANS_FILE = "src/playground/mmoyo/demo/X5/simplePlan1/plansFile.xml";
	//private final static String TRANSIT_SCHEDULE_FILE = "src/playground/mmoyo/demo/X5/simplePlan1/blueTransitSchedule.xml";
	private final static String TRANSIT_SCHEDULE_FILE = "src/playground/mmoyo/demo/X5/transfer/blue_green_TransitSchedule.xml";
	//private final static String TRANSIT_SCHEDULE_FILE = "src/playground/mmoyo/demo/X5/transfer_det/blue_green2_TransitSchedule.xml";
	private final static String VEHICLE_FILE = "src/playground/mmoyo/demo/X5/vehicles.xml";	
	
	private final static String ROUTED_PLANS_FILE = "src/playground/mmoyo/demo/X5/routedPlans.xml";
	//private final static String MULTI_MODAL_NETWORK_FILE = "src/playground/mmoyo/demo/X5/simplePlan1/multimodalnetwork.xml";
	//private final static String TRANSIT_SCHEDULE_WITH_NETWORK_FILE = "src/playground/mmoyo/demo/X5/simplePlan1/Schedule_With_Net_File";
	
	public PtIntegrator() {

		/**set scenario config*/
		ScenarioImpl scenario = new ScenarioImpl();
		Config config = scenario.getConfig();

		config.scenario().setUseTransit(true);
		config.scenario().setUseVehicles(true);
		config.network().setInputFile(NETWORK_FILE);
		config.controler().setOutputDirectory("./OUTPUT/EXPERIMENTAL");
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(0);
		config.controler().setEventsFileFormats(EnumSet.of(EventsFileFormat.xml));
		config.controler().addParam("routingAlgorithmType", "AStarLandmarks");
		config.charyparNagelScoring().addParam("activityType_0", "h");
		config.charyparNagelScoring().addParam("activityPriority_0", "1");
		config.charyparNagelScoring().addParam("activityTypicalDuration_0", "12:00:00");
		config.charyparNagelScoring().addParam("activityMinimalDuration_0", "18:00:00");
		
		config.charyparNagelScoring().addParam("activityType_1", "w");
		config.charyparNagelScoring().addParam("activityPriority_1", "1");
		config.charyparNagelScoring().addParam("activityTypicalDuration_1", "08:00:00");
		config.charyparNagelScoring().addParam("activityMinimalDuration_1", "06:00:00");
		config.charyparNagelScoring().addParam("activityOpeningTime_1", "07:00:00");

		config.charyparNagelScoring().addParam("activityType_2", "wait pt");
		config.charyparNagelScoring().addParam("activityPriority_2", "1");
		config.charyparNagelScoring().addParam("activityTypicalDuration_2", "00:10:00");
		config.charyparNagelScoring().addParam("activityMinimalDuration_2", "00:00:00");
		config.charyparNagelScoring().addParam("activityOpeningTime_2", "05:00:00");
		
		config.charyparNagelScoring().addParam("activityType_3", "transf");
		config.charyparNagelScoring().addParam("activityPriority_3", "1");
		config.charyparNagelScoring().addParam("activityTypicalDuration_3", "00:10:00");
		config.charyparNagelScoring().addParam("activityMinimalDuration_3", "00:00:00");
		config.charyparNagelScoring().addParam("activityOpeningTime_3", "04:00:00");
		
		config.charyparNagelScoring().addParam("activityType_4", "get off");
		config.charyparNagelScoring().addParam("activityPriority_4", "1");
		config.charyparNagelScoring().addParam("activityTypicalDuration_4", "00:01:00");
		config.charyparNagelScoring().addParam("activityMinimalDuration_4", "00:00:00");
		config.charyparNagelScoring().addParam("activityOpeningTime_4", "05:00:00");
		
		config.charyparNagelScoring().addParam("activityType_5", "transitInteraction");  //for compatibility with normal controller
		config.charyparNagelScoring().addParam("activityPriority_5", "1");
		config.charyparNagelScoring().addParam("activityTypicalDuration_5", "00:10:00");
		config.charyparNagelScoring().addParam("activityMinimalDuration_5", "00:00:00");
		config.charyparNagelScoring().addParam("activityOpeningTime_5", "05:00:00");

		config.simulation().setEndTime(30.0*3600);
		config.strategy().addParam("maxAgentPlanMemorySize", "5");
		config.strategy().addParam("ModuleProbability_1", "0.1");
		config.strategy().addParam("Module_1", "TimeAllocationMutator");
		config.strategy().addParam("ModuleProbability_2", "0.1");
		config.strategy().addParam("Module_2", "ReRoute");
		config.strategy().addParam("ModuleProbability_3", "0.1");
		config.strategy().addParam("Module_3", "ChangeLegMode");
		config.strategy().addParam("ModuleProbability_4", "0.1");
		config.strategy().addParam("Module_4", "SelectExpBeta");
		Module changeLegModeModule = config.createModule("changeLegMode");
		changeLegModeModule.addParam("modes", "car,pt");
		Module transitModule = config.createModule("transit");
		transitModule.addParam("transitScheduleFile", TRANSIT_SCHEDULE_FILE);
		transitModule.addParam("vehiclesFile", VEHICLE_FILE);
		transitModule.addParam("transitModes", "pt");		
		
		scenario.getNetwork().getFactory().setRouteFactory(TransportMode.pt, new ExperimentalTransitRouteFactory());

		/**read street network*/
		//NetworkLayer streetNetwork= new NetworkLayer(new NetworkFactoryImpl());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(NETWORK_FILE);

		/**read the transitSchedule file*/
		try {
			new TransitScheduleReaderV1(scenario.getTransitSchedule(), scenario.getNetwork()).readFile(TRANSIT_SCHEDULE_FILE);
		} catch (SAXException e){
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e){
			e.printStackTrace();
		}
		
		/**create pseudo-network and merge it with street network into the multimodal_network to visualize*/
		//do not use pseudonetwork, routes will be described with street links 
		/*
		NetworkLayer pseudoNetwork = new NetworkLayer();
		new CreatePseudoNetwork(scenario.getTransitSchedule(), pseudoNetwork, "tr_").createNetwork();
		
		try {
			new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(TRANSIT_SCHEDULE_WITH_NETWORK_FILE);
		} catch (IOException e) {
			e.printStackTrace();
		}
		MergeNetworks.merge(streetNetwork, "", pseudoNetwork, "", scenario.getNetwork());
		//new NetworkWriter(scenario.getNetwork()).writeFile(MULTI_MODAL_NETWORK_FILE);
		*/
		
		/**read input plans*/
		PopulationImpl population = scenario.getPopulation();
		try {
			new MatsimPopulationReader(scenario).parse(INPUT_PLANS_FILE);
			population.printPlansCount();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		/**route plans*/
		DijkstraFactory dijkstraFactory = new DijkstraFactory();
		FreespeedTravelTimeCost timeCostCalculator = new FreespeedTravelTimeCost(scenario.getConfig().charyparNagelScoring());
		TransitConfigGroup transitConfig = new TransitConfigGroup();
		MMoyoPlansCalcTransitRoute router = new MMoyoPlansCalcTransitRoute(	scenario.getConfig().plansCalcRoute(), scenario.getNetwork(), timeCostCalculator, timeCostCalculator, dijkstraFactory, scenario.getTransitSchedule(), transitConfig);
		//PlansCalcTransitRoute router = new PlansCalcTransitRoute(scenario.getConfig().plansCalcRoute(), scenario.getNetwork(), timeCostCalculator, timeCostCalculator, dijkstraFactory, scenario.getTransitSchedule(), transitConfig);
		router.run(population);	
		
		new PopulationWriter(population).write(ROUTED_PLANS_FILE);
		
		/**prepare simulation*/
		config.plans().setInputFile(ROUTED_PLANS_FILE);
		new CreateVehiclesForSchedule(scenario.getTransitSchedule(), scenario.getVehicles()).run();
		final EventsManager events = (new EventsManagerFactoryImpl()).createEventsManager() ;
		
		EventWriterXML writer = new EventWriterXML("./output/testEvents.xml");
		EventWriterTXT writertxt = new EventWriterTXT("./output/testEvents.txt");
		events.addHandler(writer);
		events.addHandler(writertxt);

		//play scenario
		scenario.getConfig().simulation().setSnapshotStyle("queue");
		final TransitQueueSimulation sim = new TransitQueueSimulation(scenario, (EventsManagerImpl) events);
		sim.startOTFServer(SERVERNAME);
		OTFDemo.ptConnect(SERVERNAME);
		sim.run();
		//////////

		writer.closeFile();
		writertxt.closeFile();
	}
	
	public static void main(final String[] args) {
		double startTime = System.currentTimeMillis();
		new PtIntegrator();	
		log.info(((System.currentTimeMillis()-startTime)/1000) + " seconds done.");
	}
}
