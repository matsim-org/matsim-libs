package playground.mmoyo.demo.berlin.linesM34_344;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerFactoryImpl;
import org.matsim.core.events.algorithms.EventWriterTXT;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.pt.qsim.TransitQSimulation;
import org.matsim.pt.router.PlansCalcTransitRoute;
import org.matsim.pt.utils.CreateVehiclesForSchedule;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.vis.otfvis.OTFVisMobsimFeature;
import org.xml.sax.SAXException;

import playground.mmoyo.TransitSimulation.MMoyoPlansCalcTransitRoute;
import playground.mmoyo.utils.TransScenarioLoader;

public class PlanRouter {

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
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(routedPlansFile);  //Writes routed plans file

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
		final QSim sim = new QSim(scenario, events);
		sim.addFeature(new OTFVisMobsimFeature(sim));
		sim.run();

		writer.closeFile();
		writertxt.closeFile();
	}

	public static void main(final String[] args) throws SAXException, ParserConfigurationException, IOException {
		String configFile;

		if (args.length==1){
			configFile = args[0];}
		else {
			configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/config_900s_small.xml";
		}

		/**load scenario */
		new PlanRouter(new TransScenarioLoader().loadScenario(configFile));
	}
}
