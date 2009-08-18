package playground.meisterk.kti.controler;

import org.matsim.core.controler.Controler;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.locationchoice.facilityload.FacilitiesLoadCalculator;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.marcel.kti.router.PlansCalcRouteKti;
import playground.meisterk.kti.config.KtiConfigGroup;
import playground.meisterk.kti.router.PlansCalcRouteKtiInfo;
import playground.meisterk.kti.scoring.KTIYear3ScoringFunctionFactory;
import playground.meisterk.org.matsim.controler.listeners.CalcLegTimesKTIListener;
import playground.meisterk.org.matsim.controler.listeners.SaveRevisionInfo;
import playground.meisterk.org.matsim.controler.listeners.ScoreElements;

/**
 * A special controler for the KTI-Project.
 * 
 * @author meisterk
 * @author mrieser
 * @author wrashid
 *
 */
public class KTIControler extends Controler {

	protected static final String SVN_INFO_FILE_NAME = "svninfo.txt";
	protected static final String SCORE_ELEMENTS_FILE_NAME = "scoreElementsAverages.txt";
	protected static final String CALC_LEG_TIMES_KTI_FILE_NAME = "calcLegTimesKTI.txt";
	
	private PlansCalcRouteKtiInfo plansCalcRouteKtiInfo = null;

	private final KtiConfigGroup ktiConfigGroup;

	public KTIControler(String[] args) {
		super(args);

		this.ktiConfigGroup = new KtiConfigGroup();
		super.config.addModule(KtiConfigGroup.GROUP_NAME, this.ktiConfigGroup);

	}
	
	@Override
	protected void setUp() {

		KTIYear3ScoringFunctionFactory kTIYear3ScoringFunctionFactory = new KTIYear3ScoringFunctionFactory(
				super.config.charyparNagelScoring(), 
				this.getFacilityPenalties(),
				this.ktiConfigGroup);
		this.setScoringFunctionFactory(kTIYear3ScoringFunctionFactory);

		if (this.ktiConfigGroup.isUsePlansCalcRouteKti()) {
			this.plansCalcRouteKtiInfo = new PlansCalcRouteKtiInfo();
			this.plansCalcRouteKtiInfo.prepare(this.ktiConfigGroup, this.getNetwork());
		}
		
		super.setUp();
	}

	@Override
	protected void loadControlerListeners() {
		
		super.loadControlerListeners();
		
		// the scoring function processes facility loads
		this.addControlerListener(new FacilitiesLoadCalculator(this.getFacilityPenalties()));
		this.addControlerListener(new ScoreElements(SCORE_ELEMENTS_FILE_NAME));
		this.addControlerListener(new CalcLegTimesKTIListener(CALC_LEG_TIMES_KTI_FILE_NAME));
		// TODO balmermi: there is a problem with that listener. It uses a system call,
		// and this call needs at least as much memory as the main process (i do not know why, but it is like that).
		// Therefore, for a big run, we cannot use that.
//		this.addControlerListener(new SaveRevisionInfo(SVN_INFO_FILE_NAME));
	}

	@Override
	public PlanAlgorithm getRoutingAlgorithm(final TravelCost travelCosts, final TravelTime travelTimes) {

		PlanAlgorithm router = null;

		if (!this.ktiConfigGroup.isUsePlansCalcRouteKti()) {
			router = super.getRoutingAlgorithm(travelCosts, travelTimes);
		} else {

			router = new PlansCalcRouteKti(
					super.getConfig().plansCalcRoute(), 
					super.network, 
					travelCosts, 
					travelTimes, 
					super.getLeastCostPathCalculatorFactory(), 
					this.plansCalcRouteKtiInfo);

		}

		return router;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: KtiControler config-file [dtd-file]");
			System.out.println();
		} else {
			final Controler controler = new KTIControler(args);
			controler.run();
		}
		System.exit(0);
	}


}
