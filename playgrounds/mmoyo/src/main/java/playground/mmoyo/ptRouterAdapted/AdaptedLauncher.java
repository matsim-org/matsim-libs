package playground.mmoyo.ptRouterAdapted;

import java.io.File;
import java.io.FileNotFoundException;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.population.algorithms.PlansFilterByLegMode;
import org.matsim.pt.config.TransitConfigGroup;

import playground.mmoyo.utils.FileCompressor;
import playground.mmoyo.utils.PlanFragmenter;
import playground.mmoyo.utils.TransScenarioLoader;


/**routes scenario based on the values of the transitRouterConfig**/
public class AdaptedLauncher {
	MyTransitRouterConfig myTransitRouterConfig;

	public AdaptedLauncher(MyTransitRouterConfig myTransitRouterConfig) {
		this.myTransitRouterConfig = myTransitRouterConfig;
	}

	public String route(final String configFile){
		
		//load scenario
		ScenarioImpl scenarioImpl = new TransScenarioLoader ().loadScenario(configFile); 
		
		//create output directory if does not exist
		if (!new File(scenarioImpl.getConfig().controler().getOutputDirectory()).exists()){
			try {
				throw new FileNotFoundException("Can not find output directory: " + scenarioImpl.getConfig().controler().getOutputDirectory());
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		
		String routedPlansFile = scenarioImpl.getConfig().controler().getOutputDirectory()+ "/routedPlan_" + this.myTransitRouterConfig.scenarioName + ".xml";
		
		//Get rid of only car plans
		if (this.myTransitRouterConfig.noCarPlans){
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
		//Yes, it uses matsim.pt.router.PlansCalcTransitRoute class to keep the compatibility of potentially handling many TransportMode types. Manuel,.
		
		AdaptedPlansCalcTransitRoute adaptedPlansCalcTransitRoute = new AdaptedPlansCalcTransitRoute(scenarioImpl.getConfig().plansCalcRoute(), scenarioImpl.getNetwork(), 
				freespeedTravelTimeCost, freespeedTravelTimeCost, dijkstraFactory, scenarioImpl.getTransitSchedule(), transitConfig, this.myTransitRouterConfig);

		adaptedPlansCalcTransitRoute.run(scenarioImpl.getPopulation());

		//fragment plans
		if (this.myTransitRouterConfig.fragmentPlans){
			scenarioImpl.setPopulation(new PlanFragmenter().run(scenarioImpl.getPopulation()));					
		}
		
		//write 
		System.out.println("writing output plan file..." + routedPlansFile);
		PopulationWriter popwriter = new PopulationWriter(scenarioImpl.getPopulation(), scenarioImpl.getNetwork()) ;
		popwriter.write(routedPlansFile) ;
		
		//compress
		if (this.myTransitRouterConfig.compressPlan){
			new FileCompressor().run(routedPlansFile);
			routedPlansFile += ".gz";
		}
		return routedPlansFile;
		
	}
	
	public static void main(String[] args) throws FileNotFoundException{
		String configFilePath = null;
		String varName = null;
		
		if (args.length==2){
			configFilePath = args[0];
			varName = args[1];
		}else{
			configFilePath = "../shared-svn/studies/countries/de/berlin-bvg09/pt/nullfall_berlin_brandenburg/config.xml";
			varName =  "adap"; 
		}

		MyTransitRouterConfig myTransitRouterConfig = new MyTransitRouterConfig();
		myTransitRouterConfig.searchRadius = 600.0;
		myTransitRouterConfig.extensionRadius = 200.0; 
		myTransitRouterConfig.beelineWalkConnectionDistance = 300.0;
		myTransitRouterConfig.fragmentPlans = false;
		myTransitRouterConfig.noCarPlans= true;
		myTransitRouterConfig.allowDirectWalks= true;
		myTransitRouterConfig.compressPlan = true;

		//once
		myTransitRouterConfig.scenarioName = varName;
		System.out.println(myTransitRouterConfig.scenarioName) ;
		AdaptedLauncher adaptedLauncher	= new AdaptedLauncher(myTransitRouterConfig);
		String routedPlan = adaptedLauncher.route(configFilePath);
		
		
		//many times
		/*
		for ( double dVariable = 1.5; dVariable <= 1.5 ; dVariable += 0.1 ) {
			double roundedVarValue = Math.round(dVariable*100)/100.0;
			String strVarValue = String.valueOf(roundedVarValue);			
			myTransitRouterConfig.marginalUtilityOfTravelDistanceTransit = -roundedVarValue / 3600.0; //modify 
			myTransitRouterConfig.scenarioName = varName + strVarValue;
			System.out.println ("routing " + myTransitRouterConfig.scenarioName);
			AdaptedLauncher adaptedLauncher	= new AdaptedLauncher(myTransitRouterConfig);
			String routedPlan = adaptedLauncher.route(configFilePath);
		}
		*/
	}
	
}


