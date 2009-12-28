package playground.mmoyo.demo.berlin.linesM34_344;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.EventsManagerFactoryImpl;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.algorithms.EventWriterTXT;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.pt.queuesim.TransitQueueSimulation;
import org.matsim.pt.routes.ExperimentalTransitRouteFactory;
import org.matsim.pt.utils.CreateVehiclesForSchedule;
import org.matsim.transitSchedule.TransitScheduleReaderV1;
import org.xml.sax.SAXException;
import org.matsim.core.router.PlansCalcRoute;

import playground.mmoyo.TransitSimulation.MMoyoPlansCalcTransitRoute;
import playground.mrieser.OTFDemo;
import playground.mrieser.pt.config.TransitConfigGroup;
import playground.mrieser.pt.router.PlansCalcTransitRoute;

/**read a config file, routes the transit plans and plays OFTDemo*/ 
public class PlanRouter {

	private static final String SERVERNAME = "ScenarioPlayer";
	private boolean useMoyoRouter  = true;     //true= PtRouter(MMoyo)   false= transitRouter
	
	public PlanRouter(ScenarioImpl scenario) {
		PlansCalcRoute router;
		String routedPlansFile = scenario.getConfig().controler().getOutputDirectory();

		/**route plans*/
		DijkstraFactory dijkstraFactory = new DijkstraFactory();
		FreespeedTravelTimeCost timeCostCalculator = new FreespeedTravelTimeCost(scenario.getConfig().charyparNagelScoring());
		TransitConfigGroup transitConfig = new TransitConfigGroup();
		if (useMoyoRouter){
			router = new MMoyoPlansCalcTransitRoute(scenario.getConfig().plansCalcRoute(), scenario.getNetwork(), timeCostCalculator, timeCostCalculator, dijkstraFactory, scenario.getTransitSchedule(), transitConfig);
			routedPlansFile += "/moyo_routedPlans.xml" ;
		}else {
			router = new PlansCalcTransitRoute(scenario.getConfig().plansCalcRoute(), scenario.getNetwork(), timeCostCalculator, timeCostCalculator, dijkstraFactory, scenario.getTransitSchedule(), transitConfig);
			routedPlansFile += "/rieser_routedPlans.xml" ;
		}

		router.run(scenario.getPopulation());	
		new PopulationWriter(scenario.getPopulation()).write(routedPlansFile);  //Writes routed plans file
		
		/**prepare simulation*/
		scenario.getConfig().plans().setInputFile(routedPlansFile);  //routed plans are now the inputFile for the simulation
		new CreateVehiclesForSchedule(scenario.getTransitSchedule(), scenario.getVehicles()).run();
		final EventsManager events = (new EventsManagerFactoryImpl()).createEventsManager() ;
		EventWriterXML writer = new EventWriterXML("./output/testEvents.xml");
		EventWriterTXT writertxt = new EventWriterTXT("./output/testEvents.txt");
		events.addHandler(writer);
		events.addHandler(writertxt);

		/**play scenario*/
		scenario.getConfig().simulation().setSnapshotStyle("queue");
		final TransitQueueSimulation sim = new TransitQueueSimulation(scenario, (EventsManagerImpl) events);
		sim.startOTFServer(SERVERNAME);
		OTFDemo.ptConnect(SERVERNAME);
		sim.run();

		writer.closeFile();
		writertxt.closeFile();
	}
	
	public static void main(final String[] args) throws SAXException, ParserConfigurationException, IOException {
		String configFile;
	
		if (args.length==1){
			configFile = args[0];} 
		else {
			configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/lines344_M44/config_baseplan_900s_bignetwork.xml";
		}
 
		/**load scenario */
		ScenarioLoaderImpl scenarioLoader = new ScenarioLoaderImpl(configFile);
		ScenarioImpl scenario = scenarioLoader.getScenario();
		scenario.getNetwork().getFactory().setRouteFactory(TransportMode.pt, new ExperimentalTransitRouteFactory());
		scenarioLoader.loadScenario();
		new TransitScheduleReaderV1(scenario.getTransitSchedule(), scenario.getNetwork()).parse(scenario.getConfig().getParam("transit", "transitScheduleFile"));
		//new CreateVehiclesForSchedule(scenario.getTransitSchedule(), scenario.getVehicles()).run();
		new PlanRouter(scenario);
	}
}
