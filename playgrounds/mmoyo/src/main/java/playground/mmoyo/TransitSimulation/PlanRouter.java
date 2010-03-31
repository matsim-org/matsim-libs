package playground.mmoyo.TransitSimulation;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.population.algorithms.PlansFilterByLegMode;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.pt.router.PlansCalcTransitRoute;
import org.xml.sax.SAXException;

import playground.mmoyo.PTRouter.PTValues;
import playground.mmoyo.utils.FileCompressor;
import playground.mmoyo.utils.PlanFragmenter;
import playground.mmoyo.utils.TransScenarioLoader;

/**reads a config file, routes the transit plans and writes a routed plans file*/
public class PlanRouter {

	public PlanRouter(ScenarioImpl scenario) {

		//Get rid of only car plans
		PlansFilterByLegMode plansFilter = new PlansFilterByLegMode( TransportMode.car, PlansFilterByLegMode.FilterType.removeAllPlansWithMode) ;
		plansFilter.run(scenario.getPopulation()) ;

		PlansCalcRoute router= null;
		String routedPlansFile = scenario.getConfig().controler().getOutputDirectory();
		
		/**route plans*/
		DijkstraFactory dijkstraFactory = new DijkstraFactory();
		FreespeedTravelTimeCost timeCostCalculator = new FreespeedTravelTimeCost(scenario.getConfig().charyparNagelScoring());
		TransitConfigGroup transitConfig = new TransitConfigGroup();

		System.out.println( PTValues.routerCalculator );
		switch (PTValues.routerCalculator){
			case 1:  //rieser
				router = new PlansCalcTransitRoute(scenario.getConfig().plansCalcRoute(), scenario.getNetwork(), timeCostCalculator, timeCostCalculator, dijkstraFactory, scenario.getTransitSchedule(), transitConfig);
				routedPlansFile += ("/rieser_" + PTValues.scenarioName + ".xml");
				break;
			case 3:	 //moyo parameterized
				router = new MMoyoPlansCalcTransitRoute(scenario.getConfig().plansCalcRoute(), scenario.getNetwork(), timeCostCalculator, timeCostCalculator, dijkstraFactory, scenario.getTransitSchedule(), transitConfig);
				routedPlansFile += ("/moyo_" + PTValues.scenarioName + ".xml");
				 break;
			default:
		}
		router.run(scenario.getPopulation());

		//write plan
		/*
		System.out.println("writing output plan file..." + routedPlansFile);
		PopulationWriter popwriter = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()) ;
		popwriter.write(routedPlansFile) ;
		*/
		
		
		//write fragmented version of the plan
		scenario.setPopulation(new PlanFragmenter().run(scenario.getPopulation()));		
		System.out.println("writing output plan file..." + routedPlansFile + "frag");
		PopulationWriter popwriter2 = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()) ;
		popwriter2.write(routedPlansFile) ;
		
		
		new FileCompressor().run(routedPlansFile);  
		
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

		ScenarioImpl scenarioImpl = new TransScenarioLoader ().loadScenario(configFile); 
		
		new PlanRouter(scenarioImpl);
		System.out.println("total duration: " + (System.currentTimeMillis()-startTime));
	}
	
}
