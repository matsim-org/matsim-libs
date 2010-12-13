package playground.mmoyo.ptRouterAdapted;

import java.io.File;
import java.io.FileNotFoundException;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.pt.config.TransitConfigGroup;

import playground.mmoyo.utils.FileCompressor;
import playground.mmoyo.utils.PlanFragmenter;
import playground.mmoyo.utils.DataLoader;
import playground.mmoyo.utils.calibration.PlanScoreRemover;


/**routes scenario based on the values of the transitRouterConfig**/
public class AdaptedLauncher {
	final ScenarioImpl scenario;
	
	private boolean noCarPlans = false;
	private boolean fragmentPlans = false;
	
	double betaWalk;
	double betaDistance;
	double betaTransfer;
	
	final String sep = "_";
	final String strDist = "dist";
	final String strWalk = "walk";
	final String strTr = "tran";
	final String routing = "routing ";
	
	public AdaptedLauncher(final String configFile) {
		this.scenario = new DataLoader ().loadScenarioWithTrSchedule(configFile); 
		validateOutDir();
	}
	
	public AdaptedLauncher(final ScenarioImpl scenario) {
		this.scenario = scenario; 
		validateOutDir();
	}
	
	private void validateOutDir(){
		if (!new File(scenario.getConfig().controler().getOutputDirectory()).exists()){
			try {
				throw new FileNotFoundException("Can not find output directory: " + scenario.getConfig().controler().getOutputDirectory());
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}			
	}
	
	public String route(MyTransitRouterConfig myTransitRouterConfig){
		//round beta values
		this.betaWalk = 	Math.round(this.betaWalk    *100)/100.0;
		this.betaDistance = Math.round(this.betaDistance*100)/100.0;
		this.betaTransfer= 	Math.round(this.betaTransfer*100)/100.0;

		//set margin utility values
		myTransitRouterConfig.marginalUtilityOfTravelTimeWalk        = -this.betaWalk     / 3600.0;
		myTransitRouterConfig.marginalUtilityOfTravelDistanceTransit = -this.betaDistance / 1000.0;
		myTransitRouterConfig.costLineSwitch = this.betaTransfer * -myTransitRouterConfig.marginalUtilityOfTravelTimeTransit;

		myTransitRouterConfig.scenarioName = strWalk + this.betaWalk + sep+ strDist + this.betaDistance + sep +  strTr + this.betaTransfer ;
		System.out.println (routing  + myTransitRouterConfig.scenarioName);
		
		if (noCarPlans){
			/*
			if (myTransitRouterConfig.noCarPlans){
				PlansFilterByLegMode plansFilter = new PlansFilterByLegMode( TransportMode.car, PlansFilterByLegMode.FilterType.removeAllPlansWithMode) ;
				plansFilter.run(scenarioImpl.getPopulation());
			}
			*/
		}
		
		//route
		DijkstraFactory dijkstraFactory = new DijkstraFactory();
		FreespeedTravelTimeCost freespeedTravelTimeCost = new FreespeedTravelTimeCost(scenario.getConfig().charyparNagelScoring());
		TransitConfigGroup transitConfig = new TransitConfigGroup();

		// yy The following comes from PlansCalcRoute; in consequence, one can configure the car router attributes.  In addition, one can configure _some_
		// of the transit attributes from here (transitSchedule, transitConfig), but not some others.  Please describe the design reason for this.  kai, apr'10
		// The design reason is, I guess, that these are the arguments that are used for the constructor of the super-class (designed by others)?  kai, apr'10
		//Yes, it uses matsim.pt.router.PlansCalcTransitRoute class to keep the compatibility of potentially handling many TransportMode types. Manuel,.
		
		AdaptedPlansCalcTransitRoute adaptedPlansCalcTransitRoute = new AdaptedPlansCalcTransitRoute(scenario.getConfig().plansCalcRoute(), scenario.getNetwork(), 
				freespeedTravelTimeCost, freespeedTravelTimeCost, dijkstraFactory, scenario.getTransitSchedule(), transitConfig, myTransitRouterConfig);

		Population pop = scenario.getPopulation();
		new PlanScoreRemover().run(pop);
		adaptedPlansCalcTransitRoute.run(pop);

		if (this.fragmentPlans){
			new PlanFragmenter().run(pop);					
		}
		
		//write 
		String routedPlansFile = scenario.getConfig().controler().getOutputDirectory()+ "routedPlan_" + myTransitRouterConfig.scenarioName + ".xml";
		System.out.println("writing output plan file..." + routedPlansFile);
		PopulationWriter popwriter = new PopulationWriter(pop, scenario.getNetwork()) ;
		popwriter.write(routedPlansFile) ;
		
		//compress
		if (myTransitRouterConfig.compressPlan){
			FileCompressor fileCompressor = new FileCompressor();
			fileCompressor.run(routedPlansFile);
			routedPlansFile += ".gz";
		}
		
		adaptedPlansCalcTransitRoute= null;
		return routedPlansFile;
	}
	
	public void set_muttWalk(double betaWalk){
		this.betaWalk = betaWalk;
	}
	
	public void set_mutDistance(double betaDistance){
		this.betaDistance = betaDistance;
	}
	
	public void set_mutTransfer(double betaTransfer){
		this.betaTransfer = betaTransfer;
	}

	public void setNoCarPland(boolean noCarPlans){
		this.noCarPlans = noCarPlans;
	}
	
	public void setFragmentPlans (boolean fragmentPlans){
		this.fragmentPlans = fragmentPlans;
	}
	
	public static void main(String[] args) throws FileNotFoundException{
		String configFilePath = null;
		double betaWalk;
		double betaDistance;
		double betaTransfer;

		if (args.length>0){
			configFilePath = args[0];
			betaWalk= Double.parseDouble(args[1]) ;
			betaDistance= Double.parseDouble(args[2]);
			betaTransfer= Double.parseDouble(args[3]);
		}else{
			configFilePath = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/calibration/100plans_bestValues_config.xml";
			
			betaWalk= 6.0 ;   	//"best values"
			betaDistance= 0.6;  
			betaTransfer= 240.0;
		}
		AdaptedLauncher adaptedLauncher	= new AdaptedLauncher(configFilePath);
		
		MyTransitRouterConfig myTransitRouterConfig = new MyTransitRouterConfig();
		myTransitRouterConfig.searchRadius = 600.0;
		myTransitRouterConfig.extensionRadius = 200.0;
		myTransitRouterConfig.beelineWalkConnectionDistance = 300.0;
		myTransitRouterConfig.fragmentPlans = false;
		myTransitRouterConfig.noCarPlans= true;
		myTransitRouterConfig.allowDirectWalks= true;
		myTransitRouterConfig.compressPlan = true;

		//route once
		adaptedLauncher.set_muttWalk(betaWalk);
		adaptedLauncher.set_mutDistance(betaDistance);
		adaptedLauncher.set_mutTransfer(betaTransfer);
		adaptedLauncher.route(myTransitRouterConfig);
		
		
		// for combination of values
		/*
		for (betaWalk=10.0; betaWalk<= 10.0; betaWalk++){  
			for (betaDistance=0.0; betaDistance<= 0.0; betaDistance+=0.1){  //dist=0.0; dist<= 1.5; dist+=0.1
				for (betaTransfer=1200.0; betaTransfer<= 1200.0; betaTransfer+=60.0){   //double transfer=0.0; transfer<= 1200.0; transfer+=60.0
					adaptedLauncher.set_muttWalk(betaWalk);
					adaptedLauncher.set_mutDistance(betaDistance);
					adaptedLauncher.set_mutTransfer(betaTransfer);
					adaptedLauncher.route(myTransitRouterConfig);
				}
			}
		}
		*/

	
	}
	
}


