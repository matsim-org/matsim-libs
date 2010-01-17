package playground.mmoyo.analysis.comp;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.population.algorithms.PlansFilterByLegMode;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.pt.router.PlansCalcTransitRoute;
import org.matsim.pt.routes.ExperimentalTransitRouteFactory;
import org.matsim.transitSchedule.TransitScheduleReaderV1;
import org.xml.sax.SAXException;

import playground.mmoyo.PTRouter.PTValues;
import playground.mmoyo.TransitSimulation.MMoyoPlansCalcTransitRoute;
import playground.mmoyo.analysis.PTLegIntoPlanConverter;

/**reads a config file, routes the transit plans and writes a routed plans file*/
public class PlanRouter {

	public PlanRouter(ScenarioImpl scenario) {

		//Get rid of only car plans
		PlansFilterByLegMode plansFilter = new PlansFilterByLegMode( TransportMode.pt, false ) ;
		plansFilter.run(scenario.getPopulation()) ;

		//split pt connections into plans
		new PTLegIntoPlanConverter().run(scenario);

		PlansCalcRoute router= null;
		String routedPlansFile = "../playgrounds/mmoyo/output";

		/**route plans*/
		DijkstraFactory dijkstraFactory = new DijkstraFactory();
		FreespeedTravelTimeCost timeCostCalculator = new FreespeedTravelTimeCost(scenario.getConfig().charyparNagelScoring());
		TransitConfigGroup transitConfig = new TransitConfigGroup();

		System.out.println( PTValues.routerCalculator );
		switch (PTValues.routerCalculator){
			case 1:  //rieser
				router = new PlansCalcTransitRoute(scenario.getConfig().plansCalcRoute(), scenario.getNetwork(), timeCostCalculator, timeCostCalculator, dijkstraFactory, scenario.getTransitSchedule(), transitConfig);
				routedPlansFile += ("/routedPlans_" + PTValues.scenario + ".xml");
				break;
			case 2:	 //moyo time
				router = new MMoyoPlansCalcTransitRoute(scenario.getConfig().plansCalcRoute(), scenario.getNetwork(), timeCostCalculator, timeCostCalculator, dijkstraFactory, scenario.getTransitSchedule(), transitConfig);
				routedPlansFile += ("/moyo_routedPlans_time_" + PTValues.scenario + ".xml" );
				break;
			case 3:	 //moyo parameterized
				router = new MMoyoPlansCalcTransitRoute(scenario.getConfig().plansCalcRoute(), scenario.getNetwork(), timeCostCalculator, timeCostCalculator, dijkstraFactory, scenario.getTransitSchedule(), transitConfig);
				routedPlansFile += ("/moyo_routedPlans_parameterized_" + PTValues.scenario + ".xml");
				 break;
			default:
		}
		router.run(scenario.getPopulation());

		System.out.println("writing output plan file..." + routedPlansFile);
		PopulationWriter popwriter = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()) ;
		popwriter.write(routedPlansFile) ;
		System.out.println("done");
	}

	public static void main(final String[] args) throws SAXException, ParserConfigurationException, IOException {
		double startTime = System.currentTimeMillis();

		String configFile = null;

		if (args.length>0){
			configFile = args[0];
		}else {
			//configFile = "../playgrounds/mmoyo/src/main/java/playground/mmoyo/demo/X5/simplePlan1/config.xml";
		}

		/**load scenario */
		ScenarioLoaderImpl scenarioLoader = new ScenarioLoaderImpl(configFile);
		ScenarioImpl scenario = scenarioLoader.getScenario();
		scenario.getNetwork().getFactory().setRouteFactory(TransportMode.pt, new ExperimentalTransitRouteFactory());
		scenarioLoader.loadScenario();
		new TransitScheduleReaderV1(scenario.getTransitSchedule(), scenario.getNetwork()).parse(scenario.getConfig().getParam("transit", "transitScheduleFile"));
		new PlanRouter(scenario);
		System.out.println("total duration: " + (System.currentTimeMillis()-startTime));
	}
}
