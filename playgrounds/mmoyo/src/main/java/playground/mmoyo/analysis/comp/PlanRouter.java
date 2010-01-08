package playground.mmoyo.analysis.comp;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.pt.routes.ExperimentalTransitRouteFactory;
import org.matsim.transitSchedule.TransitScheduleReaderV1;
import org.xml.sax.SAXException;
import org.matsim.core.router.PlansCalcRoute;

import playground.mmoyo.PTRouter.PTValues;
import playground.mmoyo.TransitSimulation.MMoyoPlansCalcTransitRoute;
import playground.mrieser.pt.config.TransitConfigGroup;
import playground.mrieser.pt.router.PlansCalcTransitRoute;

/**reads a config file, routes the transit plans and writes a routed plans file*/ 
public class PlanRouter {

	///private static boolean useMoyoRouter  = true;     //true= MmoyoPtRouter)   false= transitRouter
	
	public PlanRouter(ScenarioImpl scenario) {
		
		PlansCalcRoute router= null;
		String routedPlansFile = scenario.getConfig().controler().getOutputDirectory();

		/**route plans*/
		DijkstraFactory dijkstraFactory = new DijkstraFactory();
		FreespeedTravelTimeCost timeCostCalculator = new FreespeedTravelTimeCost(scenario.getConfig().charyparNagelScoring());
		TransitConfigGroup transitConfig = new TransitConfigGroup();
		System.out.println( PTValues.routerCalculator );
		switch (PTValues.routerCalculator){
			case 1:  //moyo time
				router = new MMoyoPlansCalcTransitRoute(scenario.getConfig().plansCalcRoute(), scenario.getNetwork(), timeCostCalculator, timeCostCalculator, dijkstraFactory, scenario.getTransitSchedule(), transitConfig);
				routedPlansFile += "/moyo_routedPlans_time.xml" ;
				 break;
			case 2:	 //moyo parameterized
				router = new MMoyoPlansCalcTransitRoute(scenario.getConfig().plansCalcRoute(), scenario.getNetwork(), timeCostCalculator, timeCostCalculator, dijkstraFactory, scenario.getTransitSchedule(), transitConfig);
				routedPlansFile += "/moyo_routedPlans_parameterized.xml" ;
				 break;
			default:
				router = new PlansCalcTransitRoute(scenario.getConfig().plansCalcRoute(), scenario.getNetwork(), timeCostCalculator, timeCostCalculator, dijkstraFactory, scenario.getTransitSchedule(), transitConfig);
				routedPlansFile += "/routedPlans.xml" ;
		}
		router.run(scenario.getPopulation());	

		System.out.println("writing output plan file..." + routedPlansFile);
		new PopulationWriter(scenario.getPopulation()).write(routedPlansFile);  //Writes routed plans file
		System.out.println("done");	
	}
	
	public static void main(final String[] args) throws SAXException, ParserConfigurationException, IOException {
		double startTime = System.currentTimeMillis();
		
		String configFile;
	
		if (args.length>0){
			configFile = args[0]; 
		}else {
			//configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/config_10x_900s_big.xml";
			//configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/config_900s_big.xml";
			//configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/config_900s_small.xml";
			configFile = "../playgrounds/mmoyo/src/main/java/playground/mmoyo/demo/X5/simplePlan1/config.xml";
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
