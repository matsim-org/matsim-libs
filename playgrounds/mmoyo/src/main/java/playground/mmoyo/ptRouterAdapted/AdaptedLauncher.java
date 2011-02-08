package playground.mmoyo.ptRouterAdapted;

import java.io.File;
import java.io.FileNotFoundException;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.population.algorithms.PlansFilterByLegMode;
import org.matsim.pt.config.TransitConfigGroup;

import playground.mmoyo.utils.PlanFragmenter;
import playground.mmoyo.utils.DataLoader;
import playground.mmoyo.utils.calibration.PlanScoreRemover;

/**routes scenario with configurable travel parameter values**/
public class AdaptedLauncher {
	private final ScenarioImpl scenario;
	private MyTransitRouterConfig myTransitRouterConfig;
	
	private boolean noCarPlans = false;
	private boolean fragmentPlans = false;
	private boolean compressPlan = true;
	
	double betaWalk;
	double betaDistance;
	double betaTransfer;
	double betaTime;
	
	final String sep = "_";
	final String strDist = "dist";
	final String strWalk = "walk";
	final String strTr = "tran";
	final String routing = "routing ";
	
	public AdaptedLauncher(final String configFile) {
		this(new DataLoader ().loadScenario(configFile)); 
	}
	
	public AdaptedLauncher(final ScenarioImpl scenario) {
		this.scenario = scenario; 
		
		//validate OutDir
		if (!new File(scenario.getConfig().controler().getOutputDirectory()).exists()){
			try {
				throw new FileNotFoundException("Can not find output directory: " + scenario.getConfig().controler().getOutputDirectory());
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		
		// load config values
		Config cfg = this.scenario.getConfig();
		myTransitRouterConfig = new MyTransitRouterConfig(this.scenario.getConfig().planCalcScore(),
				this.scenario.getConfig().plansCalcRoute() ); 
		myTransitRouterConfig.searchRadius = Double.parseDouble(cfg.getParam("ptRouter", "searchRadius")); 
		myTransitRouterConfig.extensionRadius = Double.parseDouble(cfg.getParam("ptRouter", "extensionRadius")); 
		myTransitRouterConfig.beelineWalkConnectionDistance = Double.parseDouble(cfg.getParam("ptRouter", "beelineWalkConnectionDistance")); 
		myTransitRouterConfig.allowDirectWalks = Boolean.parseBoolean(cfg.getParam("ptRouter", "allowDirectWalks")); 
		myTransitRouterConfig.minStationsNum = Integer.parseInt(cfg.getParam("ptRouter", "minIniStations"));

		this.set_betaWalk(Double.parseDouble(cfg.getParam("ptRouter", "walkValue")));
		this.set_betaTransfer(Double.parseDouble(cfg.getParam("ptRouter", "transferValue")));
		this.set_betaDistance(Double.parseDouble(cfg.getParam("ptRouter", "distanceValue")));
		this.set_betaTime(Double.parseDouble(cfg.getParam("ptRouter", "timeValue")));		
	}
	
	public String route(){
		//round beta values
		this.betaWalk = 	Math.round(this.betaWalk    *100)/100.0;
		this.betaDistance = Math.round(this.betaDistance*100)/100.0;
		this.betaTransfer= 	Math.round(this.betaTransfer*100)/100.0;

		//set margin utility values
		myTransitRouterConfig.setEffectiveMarginalUtilityOfTravelTimeWalk_utl_s(-this.betaWalk     / 3600.0);
		myTransitRouterConfig.setMarginalUtilityOfTravelDistancePt_utl_m(-this.betaDistance / 1000.0);
//		myTransitRouterConfig.setUtilityOfLineSwitch_utl(this.betaTransfer * -myTransitRouterConfig.getEffectiveMarginalUtilityOfTravelTimePt_utl_s());
		myTransitRouterConfig.setUtilityOfLineSwitch_utl(this.betaTransfer * myTransitRouterConfig.getEffectiveMarginalUtilityOfTravelTimePt_utl_s());

		myTransitRouterConfig.scenarioName = strWalk + this.betaWalk + sep+ strDist + this.betaDistance + sep +  strTr + this.betaTransfer ;
		System.out.println (routing  + myTransitRouterConfig.scenarioName);
		
		Population pop = scenario.getPopulation();
		if (this.noCarPlans){
			PlansFilterByLegMode plansFilter = new PlansFilterByLegMode( TransportMode.car, PlansFilterByLegMode.FilterType.removeAllPlansWithMode) ;
			plansFilter.run(pop);
		}
		
		//route
		DijkstraFactory dijkstraFactory = new DijkstraFactory();
		FreespeedTravelTimeCost freespeedTravelTimeCost = new FreespeedTravelTimeCost(scenario.getConfig().planCalcScore());
		TransitConfigGroup transitConfig = new TransitConfigGroup();

		// yy The following comes from PlansCalcRoute; in consequence, one can configure the car router attributes.  In addition, one can configure _some_
		// of the transit attributes from here (transitSchedule, transitConfig), but not some others.  Please describe the design reason for this.  kai, apr'10
		// The design reason is, I guess, that these are the arguments that are used for the constructor of the super-class (designed by others)?  kai, apr'10
		//Yes, it uses matsim.pt.router.PlansCalcTransitRoute class to keep the compatibility of potentially handling many TransportMode types. Manuel,.
		AdaptedPlansCalcTransitRoute adaptedPlansCalcTransitRoute = new AdaptedPlansCalcTransitRoute(scenario.getConfig().plansCalcRoute(), scenario.getNetwork(), 
				freespeedTravelTimeCost, freespeedTravelTimeCost, dijkstraFactory, scenario.getTransitSchedule(), transitConfig, myTransitRouterConfig);

		
		new PlanScoreRemover().run(pop);
		adaptedPlansCalcTransitRoute.run(pop);

		if (this.fragmentPlans){
			new PlanFragmenter().run(pop);					
		}

		//write routed plan
		String routedPlansFile = scenario.getConfig().controler().getOutputDirectory()+ "routedPlan_" + myTransitRouterConfig.scenarioName + ".xml";
		if (this.compressPlan){
			routedPlansFile += ".gz";
		}
		System.out.println("writing output plan file..." + routedPlansFile);
		PopulationWriter popwriter = new PopulationWriter(pop, scenario.getNetwork()) ;
		popwriter.write(routedPlansFile) ;
		
		adaptedPlansCalcTransitRoute= null;
		return routedPlansFile;
	}
	
	/////////getters & setters
	public void set_betaWalk(double betaWalk){
		this.betaWalk = betaWalk;
	}
	
	public void set_betaDistance(double betaDistance){
		this.betaDistance = betaDistance;
	}
	
	public void set_betaTransfer(double betaTransfer){
		this.betaTransfer = betaTransfer;
	}
	
	public void set_betaTime(double betaTime){
		this.betaTime = betaTime;
	}
	
	public double get_betaWalk(){
		return this.betaWalk;
	}
	
	public double get_betaDistance(){
		return this.betaDistance;
	}
	
	public double get_betaTransfer(){
		return this.betaTransfer;
	}
	
	public double get_betaTime(){
		return this.betaTime;
	}

	public void setNoCarPlans(boolean noCarPlans){
		this.noCarPlans = noCarPlans;
	}
	
	public void setFragmentPlans (boolean fragmentPlans){
		this.fragmentPlans = fragmentPlans;
	}
	
	public static void main(String[] args) throws FileNotFoundException{
		String configFilePath = null;
		if (args.length ==0){
			configFilePath = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/calibration/100plans_bestValues_config.xml";
		}else{
			configFilePath = args[0];
		}
		AdaptedLauncher adaptedLauncher	= new AdaptedLauncher(configFilePath);
		adaptedLauncher.setFragmentPlans(false);
		adaptedLauncher.setNoCarPlans(false);
		
		//route once, setting own parameter values 
		adaptedLauncher.set_betaDistance(0.7);
		adaptedLauncher.set_betaTransfer(240);
		adaptedLauncher.set_betaWalk(6.0);
		adaptedLauncher.route();
		
		// for many times with combination of values
		/*
		for (double betaWalk=10.0; betaWalk<= 10.0; betaWalk++){  
			for (double betaDistance=0.0; betaDistance<= 0.0; betaDistance+=0.1){  //dist=0.0; dist<= 1.5; dist+=0.1
				for (double betaTransfer=1200.0; betaTransfer<= 1200.0; betaTransfer+=60.0){   //double transfer=0.0; transfer<= 1200.0; transfer+=60.0
					adaptedLauncher.set_betaWalk(betaWalk);
					adaptedLauncher.set_betaDistance(betaDistance);
					adaptedLauncher.set_betaTransfer(betaTransfer);
					adaptedLauncher.route();
				}
			}
		}
		*/
	}
	
}


