package playground.mmoyo.ptRouterAdapted;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.population.algorithms.PlansFilterByLegMode;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.xml.sax.SAXException;
import playground.mmoyo.utils.FileCompressor;
import playground.mmoyo.utils.PlanFragmenter;
import playground.mmoyo.utils.TransScenarioLoader;

/**routes scenario based on the values of the transitRouterConfig**/
public class AdaptedLauncher {
	MyTransitRouterConfig myTransitRouterConfig;
	
	public AdaptedLauncher(MyTransitRouterConfig myTransitRouterConfig) {
		this.myTransitRouterConfig = myTransitRouterConfig;
	}

	public void route(String configFile) throws FileNotFoundException {
		
		//load scenario
		ScenarioImpl scenarioImpl = new TransScenarioLoader ().loadScenario(configFile); 
		
		if (!new File(scenarioImpl.getConfig().controler().getOutputDirectory()).exists()){
			throw new FileNotFoundException("Can not find output directory: " + scenarioImpl.getConfig().controler().getOutputDirectory());
		}
		
		String routedPlansFile = scenarioImpl.getConfig().controler().getOutputDirectory()+ "/routedPlan_" + myTransitRouterConfig.scenarioName + ".xml";
		
		//Get rid of only car plans
		if (myTransitRouterConfig.noCarPlans){
			PlansFilterByLegMode plansFilter = new PlansFilterByLegMode( TransportMode.car, PlansFilterByLegMode.FilterType.removeAllPlansWithMode) ;
			plansFilter.run(scenarioImpl.getPopulation());
		}
		
		//route
		DijkstraFactory dijkstraFactory = new DijkstraFactory();
		FreespeedTravelTimeCost freespeedTravelTimeCost = new FreespeedTravelTimeCost(scenarioImpl.getConfig().charyparNagelScoring());
		TransitConfigGroup transitConfig = new TransitConfigGroup();

		// yy The following comes from PlansCalcRoute; in consequence, one can configure the car router attributes.  In addition, one can configure _some_
		// of the transit attributes from here (transitSchedule, transitConfig), but not some others.  Please describe the design reason for this.  kai, apr'10
		// The design reason is, I guess, that these are the arguments that are used for the constructor of the super-class (designed by others)?  kai, apr'10
		AdaptedPlansCalcTransitRoute adaptedPlansCalcTransitRoute = new AdaptedPlansCalcTransitRoute(scenarioImpl.getConfig().plansCalcRoute(), scenarioImpl.getNetwork(), 
				freespeedTravelTimeCost, freespeedTravelTimeCost, dijkstraFactory, scenarioImpl.getTransitSchedule(), transitConfig, myTransitRouterConfig);

		adaptedPlansCalcTransitRoute.run(scenarioImpl.getPopulation());

		//fragment plans
		if (myTransitRouterConfig.fragmentPlans){
			scenarioImpl.setPopulation(new PlanFragmenter().run(scenarioImpl.getPopulation()));					
		}
		
		//write 
		System.out.println("writing output plan file..." + routedPlansFile);
		PopulationWriter popwriter = new PopulationWriter(scenarioImpl.getPopulation(), scenarioImpl.getNetwork()) ;
		popwriter.write(routedPlansFile) ;
		
		//compress
		if (myTransitRouterConfig.compressPlan){
			new FileCompressor().run(routedPlansFile);
		}
	}
	
	private static double round2dec(double dblNum){
		return Math.round(dblNum*100)/100.0;
	}
	
	public static void main(String[] args) throws SAXException, ParserConfigurationException, IOException {
		String configFile = null;
		MyTransitRouterConfig myTransitRouterConfig = new MyTransitRouterConfig();
		
		if (args.length>0){
			configFile = args[0];
		}else{
			configFile = "../playgrounds/mmoyo/output/seventh/config.xml";
		}
		
		myTransitRouterConfig.searchRadius = 600.0;
		myTransitRouterConfig.extensionRadius = 200.0; 
		myTransitRouterConfig.beelineWalkConnectionDistance = 300.0; 	
		myTransitRouterConfig.fragmentPlans = false;
		myTransitRouterConfig.noCarPlans= true;
		myTransitRouterConfig.allowDirectWalks= true;
		myTransitRouterConfig.compressPlan = true;

		//once
		/*
		myTransitRouterConfig.scenarioName = "_costLineSwitch" + myTransitRouterConfig.costLineSwitch ;
		System.out.println(myTransitRouterConfig.scenarioName) ;
		AdaptedLauncher adaptedLauncher	= new AdaptedLauncher(myTransitRouterConfig);
		adaptedLauncher.route(configFile);
		*/
		
		//many times
		for ( double dUtility = 0; dUtility <= 0.001 ; dUtility += 0.0001 ) {
			//myTransitRouterConfig.marginalUtilityOfTravelDistanceTransit = -distanceUtility / 1000.0; ????
			myTransitRouterConfig.marginalUtilityOfTravelDistanceTransit = -dUtility;
			myTransitRouterConfig.scenarioName = "_dis" + dUtility ;
			System.out.println(myTransitRouterConfig.scenarioName) ;
			AdaptedLauncher adaptedLauncher	= new AdaptedLauncher(myTransitRouterConfig);
			adaptedLauncher.route(configFile);
			System.out.println(myTransitRouterConfig.marginalUtilityOfTravelDistanceTransit);
		}
	}	
}


