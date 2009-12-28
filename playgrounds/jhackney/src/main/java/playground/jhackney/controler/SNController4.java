package playground.jhackney.controler;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.modules.ReRouteLandmarks;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.PreProcessLandmarks;

import playground.jhackney.socialnetworks.mentalmap.TimeWindow;
import playground.jhackney.socialnetworks.replanning.SNPickFacilityFromAlter;

public class SNController4 extends Controler {

	private final Logger log = Logger.getLogger(SNController4.class);
	protected LinkedHashMap<ActivityFacility, ArrayList<TimeWindow>> twm;
	protected SNControllerListener4 snControllerListener;
		
	public SNController4(String args[]){
		super(args);
		this.snControllerListener=new SNControllerListener4(this);
		this.addControlerListener(new SNControllerListener4(this));
	}

	public static void main(final String[] args) {
		final Controler controler = new SNController4(args);
//		controler.addControlerListener(new SNControllerListener3());
		controler.setOverwriteFiles(true);
		controler.run();
		System.exit(0);
	}


	/**
	 * @return A fully initialized StrategyManager for the plans replanning.
	 */
	@Override
	protected StrategyManager loadStrategyManager() {

		StrategyManager manager = new StrategyManager();

		manager.setMaxPlansPerAgent(config.strategy().getMaxAgentPlanMemorySize());
		
		// Adjust activity start times by social network and time windows
		PlanStrategy strategy1 = new PlanStrategy(new RandomPlanSelector());// only facilities visited in last iteration are in time window hastable
		PlanStrategyModule socialNetStrategyModule= new SNPickFacilityFromAlter(this.getConfig(), this.network,this.travelCostCalculator,this.travelTimeCalculator, ((ScenarioImpl)this.getScenario()).getKnowledges());
		strategy1.addStrategyModule(socialNetStrategyModule);
		manager.addStrategy(strategy1, 0.15);
		this.log.info("  added strategy SNCoordinateArrivalTimes with probability 0.15");

		// Adjust activity route
		PreProcessLandmarks preProcessRoutingData = new PreProcessLandmarks(new FreespeedTravelTimeCost());
		preProcessRoutingData.run(this.getNetwork());
		PlanStrategy strategy2 = new PlanStrategy(new RandomPlanSelector());
		strategy2.addStrategyModule(new ReRouteLandmarks(this.getConfig(), this.getNetwork(), this.getTravelCostCalculator(), this.getTravelTimeCalculator(), preProcessRoutingData));
		manager.addStrategy(strategy2,0.15);
		this.log.info("  added strategy ReRouteLandmarks with probability 0.15");

		// Tend to re-use best plan
		PlanStrategy strategy3 = new PlanStrategy(new ExpBetaPlanSelector(this.config.charyparNagelScoring()));
		manager.addStrategy(strategy3, 0.7);
		this.log.info("  added strategy ExpBetaPlan with probability 0.7");

		return manager;
	}
	
	public LinkedHashMap<ActivityFacility,ArrayList<TimeWindow>> getTwm() {
		return this.twm;
	}
	public void setTwm(LinkedHashMap<ActivityFacility,ArrayList<TimeWindow>> twm){
		this.twm=twm;
	}
}