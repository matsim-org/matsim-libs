package playground.meisterk.kti.controler;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.locationchoice.facilityload.FacilitiesLoadCalculator;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.meisterk.kti.config.KtiConfigGroup;
import playground.meisterk.kti.controler.listeners.CalcLegTimesKTIListener;
import playground.meisterk.kti.controler.listeners.KtiPopulationPreparation;
import playground.meisterk.kti.controler.listeners.LegDistanceDistributionWriter;
import playground.meisterk.kti.controler.listeners.ScoreElements;
import playground.meisterk.kti.router.KtiNodeNetworkRouteFactory;
import playground.meisterk.kti.router.KtiPtRouteFactory;
import playground.meisterk.kti.router.KtiTravelCostCalculatorFactory;
import playground.meisterk.kti.router.PlansCalcRouteKti;
import playground.meisterk.kti.router.PlansCalcRouteKtiInfo;
import playground.meisterk.kti.scoring.KTIYear3ScoringFunctionFactory;

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
	protected static final String LEG_DISTANCE_DISTRIBUTION_FILE_NAME = "legDistanceDistribution.txt";
	protected static final String LEG_TRAVEL_TIME_DISTRIBUTION_FILE_NAME = "legTravelTimeDistribution.txt";
	
	private final KtiConfigGroup ktiConfigGroup = new KtiConfigGroup();
	private final PlansCalcRouteKtiInfo plansCalcRouteKtiInfo = new PlansCalcRouteKtiInfo(ktiConfigGroup);

	public KTIControler(String[] args) {
		super(args);

		super.config.addModule(KtiConfigGroup.GROUP_NAME, this.ktiConfigGroup);

		this.getNetwork().getFactory().setRouteFactory(TransportMode.car, new KtiNodeNetworkRouteFactory(super.getConfig().planomat()));
		this.getNetwork().getFactory().setRouteFactory(TransportMode.pt, new KtiPtRouteFactory(this.plansCalcRouteKtiInfo));

	}
	
	@Override
	protected void setUp() {

		if (this.ktiConfigGroup.isUsePlansCalcRouteKti()) {
			this.plansCalcRouteKtiInfo.prepare(this.getNetwork());
		}
		
		KTIYear3ScoringFunctionFactory kTIYear3ScoringFunctionFactory = new KTIYear3ScoringFunctionFactory(
				super.config, 
				this.ktiConfigGroup,
				this.getFacilityPenalties(),
				this.getFacilities());
		this.setScoringFunctionFactory(kTIYear3ScoringFunctionFactory);

		KtiTravelCostCalculatorFactory costCalculatorFactory = new KtiTravelCostCalculatorFactory(ktiConfigGroup);
		this.setTravelCostCalculatorFactory(costCalculatorFactory);
		
		super.setUp();
	}

	
	@Override
	protected void loadControlerListeners() {
		
		super.loadControlerListeners();
		
		// the scoring function processes facility loads
		this.addControlerListener(new FacilitiesLoadCalculator(this.getFacilityPenalties()));
		this.addControlerListener(new ScoreElements(SCORE_ELEMENTS_FILE_NAME));
		this.addControlerListener(new CalcLegTimesKTIListener(CALC_LEG_TIMES_KTI_FILE_NAME, LEG_TRAVEL_TIME_DISTRIBUTION_FILE_NAME));
		this.addControlerListener(new LegDistanceDistributionWriter(LEG_DISTANCE_DISTRIBUTION_FILE_NAME));
		this.addControlerListener(new KtiPopulationPreparation(this.ktiConfigGroup));
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
