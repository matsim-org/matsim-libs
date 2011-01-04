package playground.mmoyo.analysis.comp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.population.algorithms.PlansFilterByLegMode;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.pt.router.PlansCalcTransitRoute;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.xml.sax.SAXException;

import playground.mmoyo.utils.DataLoader;
import playground.mmoyo.utils.FileCompressor;
import playground.mmoyo.utils.PlanFragmenter;
import playground.mmoyo.utils.calibration.PlanScoreRemover;

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
		FreespeedTravelTimeCost timeCostCalculator = new FreespeedTravelTimeCost(scenario.getConfig().charyparNagelScoring());
		TransitConfigGroup transitConfig = new TransitConfigGroup();

		PlansCalcRoute router = new PlansCalcTransitRoute(scenario.getConfig().plansCalcRoute(), scenario.getNetwork(), timeCostCalculator, timeCostCalculator, dijkstraFactory, transitConfig, new TransitRouter(scenario.getTransitSchedule(), new TransitRouterConfig()));
	
		Population population = scenario.getPopulation();
		
		//remove scores
		new PlanScoreRemover().run(population);
		
		router.run(scenario.getPopulation());

		//fragment
		//new PlanFragmenter().run(population);
		
		//write plan
		String routedPlansFile = scenario.getConfig().controler().getOutputDirectory() + "rieserRoutedPlan.xml";
		System.out.println("writing output plan file..." + routedPlansFile);
		PopulationWriter popwriter = new PopulationWriter(population, scenario.getNetwork()) ;
		popwriter.write(routedPlansFile) ;

		new FileCompressor().run(routedPlansFile);
		System.out.println("done");
	}

	public static void main(final String[] args) throws SAXException, ParserConfigurationException, IOException {
		double startTime = System.currentTimeMillis();

		String configFile = null;

		if (args.length>0){
			configFile = args[0];
		}else {
			configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/calibration/100plans_bestValues_config.xml";
		}

		ScenarioImpl scenarioImpl = new DataLoader ().loadScenarioWithTrSchedule(configFile);

		new MATSimRouterLauncher(scenarioImpl);
		System.out.println("total duration: " + (System.currentTimeMillis()-startTime));
	}

}
