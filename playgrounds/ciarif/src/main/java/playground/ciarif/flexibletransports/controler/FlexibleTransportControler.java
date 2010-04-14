package playground.ciarif.flexibletransports.controler;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.locationchoice.facilityload.FacilitiesLoadCalculator;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.ciarif.flexibletransports.config.FtConfigGroup;
import playground.ciarif.flexibletransports.controler.listeners.FtPopulationPreparation;
import playground.ciarif.flexibletransports.scoring.FtScoringFunctionFactory;
import playground.meisterk.kti.config.KtiConfigGroup;
import playground.meisterk.kti.controler.listeners.CalcLegTimesKTIListener;
import playground.meisterk.kti.controler.listeners.LegDistanceDistributionWriter;
import playground.meisterk.kti.controler.listeners.ScoreElements;
import playground.meisterk.kti.router.KtiLinkNetworkRouteFactory;
import playground.meisterk.kti.router.KtiPtRouteFactory;
import playground.meisterk.kti.router.PlansCalcRouteKti;
import playground.meisterk.kti.router.PlansCalcRouteKtiInfo;

public class FlexibleTransportControler extends Controler {

	protected static final String SVN_INFO_FILE_NAME = "svninfo.txt";
	protected static final String SCORE_ELEMENTS_FILE_NAME = "scoreElementsAverages.txt";
	protected static final String CALC_LEG_TIMES_KTI_FILE_NAME = "calcLegTimesKTI.txt";
	protected static final String LEG_DISTANCE_DISTRIBUTION_FILE_NAME = "legDistanceDistribution.txt";
	protected static final String LEG_TRAVEL_TIME_DISTRIBUTION_FILE_NAME = "legTravelTimeDistribution.txt";

	private final FtConfigGroup ftConfigGroup;
	private KtiConfigGroup kTIConfigGroup; //TODO now is null and dosn't take any value when the FlexibleTransportControler is instantiated

	private final PlansCalcRouteKtiInfo plansCalcRouteKtiInfo = new PlansCalcRouteKtiInfo(kTIConfigGroup);

	public FlexibleTransportControler(String[] args) {
		super(args);

		this.ftConfigGroup = new FtConfigGroup();
		super.config.addModule(FtConfigGroup.GROUP_NAME, this.ftConfigGroup);

		this.getNetwork().getFactory().setRouteFactory(TransportMode.car, new KtiLinkNetworkRouteFactory(this.getNetwork(), super.getConfig().planomat()));
		this.getNetwork().getFactory().setRouteFactory(TransportMode.pt, new KtiPtRouteFactory(this.plansCalcRouteKtiInfo));
		this.getNetwork().getFactory().setRouteFactory(TransportMode.ride, new KtiPtRouteFactory(this.plansCalcRouteKtiInfo));
		//TODO modify the line here over when a specific router for ride is available, now it is not used
	}

	@Override
	protected void setUp() {

		if (this.ftConfigGroup.isUsePlansCalcRouteKti()) {
			this.plansCalcRouteKtiInfo.prepare(this.getNetwork());
		}

		FtScoringFunctionFactory ftScoringFunctionFactory = new FtScoringFunctionFactory(
				super.config,
				this.ftConfigGroup,
				this.getFacilityPenalties(),
				this.getFacilities());
		this.setScoringFunctionFactory(ftScoringFunctionFactory);

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
		this.addControlerListener(new FtPopulationPreparation(this.ftConfigGroup));
		// TODO balmermi: there is a problem with that listener. It uses a system call,
		// and this call needs at least as much memory as the main process (i do not know why, but it is like that).
		// Therefore, for a big run, we cannot use that.
//		this.addControlerListener(new SaveRevisionInfo(SVN_INFO_FILE_NAME));
	}

	@Override
	public PlanAlgorithm createRoutingAlgorithm(final PersonalizableTravelCost travelCosts, final TravelTime travelTimes) {

		PlanAlgorithm router = null;

		if (!this.ftConfigGroup.isUsePlansCalcRouteKti()) {
			router = super.createRoutingAlgorithm(travelCosts, travelTimes);
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
			System.out.println("Usage: FlexibleTransportControler config-file [dtd-file]");
			System.out.println();
		} else {
			final Controler controler = new FlexibleTransportControler(args);
			controler.run();
		}
		System.exit(0);
	}
}
