package playground.mmoyo.ptRouterAdapted.precalculation;

import java.io.File;
import java.io.FileNotFoundException;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.population.algorithms.PlansFilterByLegMode;
import org.matsim.pt.config.TransitConfigGroup;

import playground.mmoyo.ptRouterAdapted.MyTransitRouterConfig;
import playground.mmoyo.utils.FileCompressor;
import playground.mmoyo.utils.DataLoader;

/**Defines input data and parameters for route precalculation**/
public class PrecLauncher {
	MyTransitRouterConfig myTransitRouterConfig;

	public PrecLauncher(MyTransitRouterConfig myTransitRouterConfig) {
		this.myTransitRouterConfig = myTransitRouterConfig;
	}

	public String run(final String configFile){
		//load scenario
		ScenarioImpl scenarioImpl = new DataLoader().loadScenarioWithTrSchedule(configFile); 
		
		//create output directory if does not exist
		File outDir = new File(scenarioImpl.getConfig().controler().getOutputDirectory());
		if (!outDir.exists()){
			outDir.mkdirs();
		}
		
		File popFile = new File(scenarioImpl.getConfig().findParam("plans", "inputPlansFile"));
		String routedPlansFile = scenarioImpl.getConfig().controler().getOutputDirectory()+ "minTransferRouted" + popFile.getName();
		
		//Get rid of only car plans
		if (this.myTransitRouterConfig.noCarPlans){
			PlansFilterByLegMode plansFilter = new PlansFilterByLegMode( TransportMode.car, PlansFilterByLegMode.FilterType.removeAllPlansWithMode) ;
			plansFilter.run(scenarioImpl.getPopulation());
		}
		
		//route
		DijkstraFactory dijkstraFactory = new DijkstraFactory();
		FreespeedTravelTimeCost freespeedTravelTimeCost = new FreespeedTravelTimeCost(scenarioImpl.getConfig().charyparNagelScoring());
		TransitConfigGroup transitConfig = new TransitConfigGroup();
		
		//precalculation of minimal transfers
		PrecalPlansCalcTransitRoute precPlansCalcTransitRoute = new PrecalPlansCalcTransitRoute(scenarioImpl.getConfig().plansCalcRoute(), scenarioImpl.getNetwork(), 
				freespeedTravelTimeCost, freespeedTravelTimeCost, dijkstraFactory, scenarioImpl.getTransitSchedule(), transitConfig, this.myTransitRouterConfig);
		
		//"normal"
		//PrecalPlansCalcTransitRoute precPlansCalcTransitRoute = new PrecalPlansCalcTransitRoute(scenarioImpl.getConfig().plansCalcRoute(), scenarioImpl.getNetwork(), 
		//		freespeedTravelTimeCost, freespeedTravelTimeCost, dijkstraFactory, scenarioImpl.getTransitSchedule(), transitConfig, this.myTransitRouterConfig);
		
		precPlansCalcTransitRoute.run(scenarioImpl.getPopulation());
		
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
			configFilePath = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/calibration/100plans_bestValues_config.xml";
			varName =  "exp"; 
		}

		MyTransitRouterConfig myTransitRouterConfig = new MyTransitRouterConfig();
		myTransitRouterConfig.beelineWalkConnectionDistance = 300.0;
		myTransitRouterConfig.fragmentPlans = false;
		myTransitRouterConfig.noCarPlans= true;
		myTransitRouterConfig.compressPlan = true;

		//set optimal values
		myTransitRouterConfig.beelineWalkSpeed = 3.0/3.6;  	
		myTransitRouterConfig.marginalUtilityOfTravelTimeWalk = -6.0 / 3600.0; 	//-6.0 / 3600.0; // in Eu/sec; includes opportunity cost of time.  kai, apr'10
		myTransitRouterConfig.marginalUtilityOfTravelTimeTransit = -6.0 / 3600.0;//-6.0 / 3600.0; // in Eu/sec; includes opportunity cost of time.  kai, apr'10
		myTransitRouterConfig.marginalUtilityOfTravelDistanceTransit = -0.7/1000.0; //-0.7/1000.0;    // yyyy presumably, in Eu/m ?????????  so far, not used.  kai, apr'10
		myTransitRouterConfig.costLineSwitch = 240.0 * - myTransitRouterConfig.marginalUtilityOfTravelTimeTransit;	//* -this.marginalUtilityOfTravelTimeTransit; // == 1min travel time in vehicle  // in Eu.  kai, apr'10
		myTransitRouterConfig.searchRadius = 600.0;								//initial distance for stations around origin and destination points
		myTransitRouterConfig.extensionRadius = 200.0; 
		myTransitRouterConfig.allowDirectWalks= false;
		
		myTransitRouterConfig.scenarioName = varName;
		System.out.println(myTransitRouterConfig.scenarioName) ;
		PrecLauncher adaptedLauncher = new PrecLauncher(myTransitRouterConfig);
		adaptedLauncher.run(configFilePath);
	}
}