package playground.mmoyo.ptRouterAdapted.replanning;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.pt.config.TransitConfigGroup;

import playground.mmoyo.ptRouterAdapted.AdaptedPlansCalcTransitRoute;
import playground.mmoyo.ptRouterAdapted.MyTransitRouterConfig;

public class AdapPlanStrategyModule extends AbstractMultithreadedModule{ //implements PlanStrategyModule, ActivityEndEventHandler { // this is just there as an example
	private static final Logger log = Logger.getLogger(AdapPlanStrategyModule.class);
	private Controler controler;
	
	public AdapPlanStrategyModule(Controler controler) {
		super(controler.getConfig().global());
		this.controler = controler ;
	}
	
	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		log.info("Creating adaptedRouter algo.");
		Config config =  this.controler.getConfig();
		FreespeedTravelTimeCost freespeedTravelTimeCost = new FreespeedTravelTimeCost(config.charyparNagelScoring());
			
		MyTransitRouterConfig myTransitRouterConfig = new MyTransitRouterConfig();
		myTransitRouterConfig.beelineWalkConnectionDistance = 300.0;  			//distance to search stations when transfering
		myTransitRouterConfig.beelineWalkSpeed = 3.0/3.6;  						// presumably, in m/sec.  3.0/3.6 = 3000/3600 = 3km/h.  kai, apr'10
		myTransitRouterConfig.marginalUtilityOfTravelTimeWalk = -6.0 / 3600.0; 	//-6.0 / 3600.0; // in Eu/sec; includes opportunity cost of time.  kai, apr'10
		myTransitRouterConfig.marginalUtilityOfTravelTimeTransit = -6.0 / 3600.0;//-6.0 / 3600.0; // in Eu/sec; includes opportunity cost of time.  kai, apr'10
		myTransitRouterConfig.marginalUtilityOfTravelDistanceTransit = -0.7/1000.0; //-0.7/1000.0;    // yyyy presumably, in Eu/m ?????????  so far, not used.  kai, apr'10
		myTransitRouterConfig.costLineSwitch = 240.0 * - myTransitRouterConfig.marginalUtilityOfTravelTimeTransit;	//* -this.marginalUtilityOfTravelTimeTransit; // == 1min travel time in vehicle  // in Eu.  kai, apr'10
		myTransitRouterConfig.searchRadius = 600.0;								//initial distance for stations around origin and destination points
		myTransitRouterConfig.extensionRadius = 200.0; 
		myTransitRouterConfig.allowDirectWalks= true;
		AdaptedPlansCalcTransitRoute adaptedPlansCalcTransitRoute = new AdaptedPlansCalcTransitRoute(config.plansCalcRoute(), this.controler.getScenario().getNetwork(), freespeedTravelTimeCost, freespeedTravelTimeCost,  new DijkstraFactory(), this.controler.getScenario().getTransitSchedule(), new TransitConfigGroup(), myTransitRouterConfig);
		return adaptedPlansCalcTransitRoute;
	}	
}