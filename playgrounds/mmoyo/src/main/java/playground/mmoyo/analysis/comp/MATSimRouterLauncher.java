package playground.mmoyo.analysis.comp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.RoutingContextImpl;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.population.algorithms.PlansFilterByLegMode;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.pt.router.TransitRouterConfig;
import org.xml.sax.SAXException;

import playground.mmoyo.algorithms.PlanScoreNullifier;
import playground.mmoyo.utils.DataLoader;

/**routes a population specified in a config file with MATSim standard router*/
public class MATSimRouterLauncher {

	public MATSimRouterLauncher(ScenarioImpl scenario) throws FileNotFoundException {

		//Get rid of only car plans
		PlansFilterByLegMode plansFilter = new PlansFilterByLegMode( TransportMode.car, PlansFilterByLegMode.FilterType.removeAllPlansWithMode) ;
		plansFilter.run(scenario.getPopulation()) ;

		File outDirectory = new File(scenario.getConfig().controler().getOutputDirectory());
		if (!outDirectory.exists()){
			throw new FileNotFoundException("Can not find output directory");
		}

		/**route plans*/
		DijkstraFactory dijkstraFactory = new DijkstraFactory();
		FreespeedTravelTimeAndDisutility timeCostCalculator = new FreespeedTravelTimeAndDisutility(scenario.getConfig().planCalcScore());
		TransitConfigGroup transitConfig = new TransitConfigGroup();
		
		TransitRouterConfig tRConfig = new TransitRouterConfig(scenario.getConfig().planCalcScore(), 
				scenario.getConfig().plansCalcRoute(), scenario.getConfig().transitRouter(),
				scenario.getConfig().vspExperimental());

		PlanRouter router =
				new PlanRouter(
						new TripRouterFactoryBuilderWithDefaults().build(
								scenario ).instantiateAndConfigureTripRouter(
										new RoutingContextImpl(
												timeCostCalculator,
												timeCostCalculator ) ) );
	
		Population population = scenario.getPopulation();
		
		//remove scores
		new PlanScoreNullifier().run(population);
		
		for ( Person p : scenario.getPopulation().getPersons().values() ) {
			router.run( p );
		}

		//fragment
		//new PlanFragmenter().run(population);
		
		//write plan
		String routedPlansFile = scenario.getConfig().controler().getOutputDirectory() + "rieserRoutedPlan.xml.gz";
		System.out.println("writing output plan file..." + routedPlansFile);
		PopulationWriter popwriter = new PopulationWriter(population, scenario.getNetwork()) ;
		popwriter.write(routedPlansFile) ;
		System.out.println("done");
	}

	public static void main(final String[] args) throws SAXException, ParserConfigurationException, IOException {
		double startTime = System.currentTimeMillis();

		String configFile = null;

		if (args.length>0){
			configFile = args[0];
		}else {
			configFile = "../mmoyo/src/main/java/playground/mmoyo/demo/X5/waitTime/config.xml";
		}

		ScenarioImpl scenarioImpl = new DataLoader().loadScenario(configFile);

		new MATSimRouterLauncher(scenarioImpl);
		System.out.println("total duration: " + (System.currentTimeMillis()-startTime));
	}

}
