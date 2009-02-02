package playground.jhackney.controler;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.apache.log4j.Logger;
import org.matsim.controler.Controler;
import org.matsim.facilities.Facility;
import org.matsim.replanning.PlanStrategy;
import org.matsim.replanning.StrategyManager;
import org.matsim.replanning.modules.ReRouteLandmarks;
import org.matsim.replanning.modules.StrategyModule;
import org.matsim.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.replanning.selectors.KeepSelected;
import org.matsim.replanning.selectors.RandomPlanSelector;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.router.util.PreProcessLandmarks;
import org.matsim.socialnetworks.mentalmap.TimeWindow;

import playground.jhackney.replanning.SNCoordinateArrivalTimes;

public class SNController3 extends Controler {

	private final Logger log = Logger.getLogger(SNController3.class);
	protected LinkedHashMap<Facility,ArrayList<TimeWindow>> twm;
	protected SNControllerListener3 snControllerListener;
		
	public SNController3(String args[]){
		super(args);
		this.snControllerListener=new SNControllerListener3(this);
		this.addControlerListener(new SNControllerListener3(this));
	}

	public static void main(final String[] args) {
		final Controler controler = new SNController3(args);
//		controler.addControlerListener(new SNControllerListener3());
		controler.setOverwriteFiles(true);
		controler.run();
		System.exit(0);
	}


	/**
	 * @return A fully initialized StrategyManager for the plans replanning.
	 */
	protected StrategyManager loadStrategyManager() {

		StrategyManager manager = new StrategyManager();

		// Adjust activity start times by social network and time windows
		PlanStrategy strategy1 = new PlanStrategy(new KeepSelected());// only facilities visited in last iteration are in time window hastable
		StrategyModule socialNetStrategyModule= new SNCoordinateArrivalTimes(this);
		strategy1.addStrategyModule(socialNetStrategyModule);
		manager.addStrategy(strategy1, 0.15);
		this.log.info("  added strategy SNCoordinateArrivalTimes with probability 0.15");

		// Adjust activity route
		PreProcessLandmarks preProcessRoutingData = new PreProcessLandmarks(new FreespeedTravelTimeCost());
		preProcessRoutingData.run(this.getNetwork());
		PlanStrategy strategy2 = new PlanStrategy(new RandomPlanSelector());
		strategy2.addStrategyModule(new ReRouteLandmarks(this.getNetwork(), this.getTravelCostCalculator(), this.getTravelTimeCalculator(), preProcessRoutingData));
		manager.addStrategy(strategy2,0.15);
		this.log.info("  added strategy ReRouteLandmarks with probability 0.15");

		// Tend to re-use best plan
		PlanStrategy strategy3 = new PlanStrategy(new ExpBetaPlanSelector());
		manager.addStrategy(strategy3, 0.7);
		this.log.info("  added strategy ExpBetaPlan with probability 0.7");

		return manager;
	}
//	JH
	public LinkedHashMap<Facility,ArrayList<TimeWindow>> getTwm() {
		return this.twm;
	}
	public void setTwm(LinkedHashMap<Facility,ArrayList<TimeWindow>> twm){
		this.twm=twm;
	}
// JH end
}
