package playground.jhackney.controler;

import java.util.ArrayList;
import java.util.Hashtable;

import org.apache.log4j.Logger;
import org.matsim.analysis.CalcLegTimes;
import org.matsim.analysis.CalcLinkStats;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.controler.Controler;
import org.matsim.facilities.Facility;
import org.matsim.replanning.PlanStrategy;
import org.matsim.replanning.StrategyManager;
import org.matsim.replanning.modules.ReRouteLandmarks;
import org.matsim.replanning.modules.StrategyModule;
import org.matsim.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.replanning.selectors.RandomPlanSelector;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.router.costcalculators.TravelTimeDistanceCostCalculator;
import org.matsim.router.util.PreProcessLandmarks;
import org.matsim.socialnetworks.mentalmap.TimeWindow;
import org.matsim.socialnetworks.replanning.SNCoordinateArrivalTimes;

public class SNController3 extends Controler {

	private final Logger log = Logger.getLogger(SNController3.class);
	protected Hashtable<Facility,ArrayList<TimeWindow>> twm=null;

	public SNController3(String args[]){
		super(args);
		twm=new Hashtable<Facility,ArrayList<TimeWindow>>();
	}
//	@Override
	protected void setup(){
		super.setup();

		this.log.info("  Setting up the replanning strategies");
		this.strategyManager = loadStrategyManager();
		this.log.info(" ... done.");
	}
	
	public static void main(final String[] args) {
		final Controler controler = new SNController(args);
		controler.addControlerListener(new SNControllerListener3());
		controler.setOverwriteFiles(true);
		controler.run();
		System.exit(0);
	}

	/**
	 * @return A fully initialized StrategyManager for the plans replanning.
	 */
	protected StrategyManager loadStrategyManager() {

		StrategyManager manager = new StrategyManager();
//		StrategyManagerConfigLoader.load(this, this.config, manager);

		// Adjust activity start times by social network and time windows
		PlanStrategy strategy = new PlanStrategy(new RandomPlanSelector());
		StrategyModule socialNetStrategyModule= new SNCoordinateArrivalTimes(this.twm);
		strategy.addStrategyModule(socialNetStrategyModule);
		manager.addStrategy(strategy, 0.15);
		this.log.info("  added strategy SNCoordinateArrivalTimes with probability 0.15");

		// Adjust activity route
		PreProcessLandmarks preProcessRoutingData = new PreProcessLandmarks(new FreespeedTravelTimeCost());
		preProcessRoutingData.run(this.getNetwork());
		strategy.addStrategyModule(new ReRouteLandmarks(this.getNetwork(), this.getTravelCostCalculator(), this.getTravelTimeCalculator(), preProcessRoutingData));
		manager.addStrategy(strategy,0.15);
		this.log.info("  added strategy ReRouteLandmarks with probability 0.15");

		// Tend to re-use best plan
		strategy = new PlanStrategy(new ExpBetaPlanSelector());
		manager.addStrategy(strategy, 0.7);
		this.log.info("  added strategy ExpBetaPlan with probability 0.7");

		return manager;
	}

}
