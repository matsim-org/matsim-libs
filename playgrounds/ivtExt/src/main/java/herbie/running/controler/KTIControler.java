package herbie.running.controler;

import herbie.running.config.KtiConfigGroup;
import herbie.running.controler.listeners.CalcLegTimesKTIListener;
import herbie.running.controler.listeners.KtiPopulationPreparation;
import herbie.running.controler.listeners.LegDistanceDistributionWriter;
import herbie.running.controler.listeners.ScoreElements;
import herbie.running.router.KtiLinkNetworkRouteFactory;
import herbie.running.router.KtiPtRouteFactory;
import herbie.running.router.KtiTravelCostCalculatorFactory;
import herbie.running.router.PlansCalcRouteKtiInfo;
import herbie.running.scenario.KtiScenarioLoaderImpl;
import herbie.running.scoring.KTIYear3ScoringFunctionFactory;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.locationchoice.facilityload.FacilitiesLoadCalculator;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * A special controler for the KTI-Project.
 *
 * @author meisterk
 * @author mrieser
 * @author wrashid
 *
 */
public class KTIControler extends Controler {

	protected static final String SCORE_ELEMENTS_FILE_NAME = "scoreElementsAverages.txt";
	protected static final String CALC_LEG_TIMES_KTI_FILE_NAME = "calcLegTimesKTI.txt";
	protected static final String LEG_DISTANCE_DISTRIBUTION_FILE_NAME = "legDistanceDistribution.txt";
	protected static final String LEG_TRAVEL_TIME_DISTRIBUTION_FILE_NAME = "legTravelTimeDistribution.txt";

	private final KtiConfigGroup ktiConfigGroup = new KtiConfigGroup();
	private final PlansCalcRouteKtiInfo plansCalcRouteKtiInfo = new PlansCalcRouteKtiInfo(ktiConfigGroup);

	public KTIControler(String[] args) {
		super(args);

		super.config.addModule(KtiConfigGroup.GROUP_NAME, this.ktiConfigGroup);

		this.getNetwork().getFactory().setRouteFactory(TransportMode.car, new KtiLinkNetworkRouteFactory(this.getNetwork(), super.getConfig().planomat()));
		this.getNetwork().getFactory().setRouteFactory(TransportMode.pt, new KtiPtRouteFactory(this.plansCalcRouteKtiInfo));

	}

	@Override
	protected void loadData() {
		if (!this.scenarioLoaded) {
			KtiScenarioLoaderImpl loader = new KtiScenarioLoaderImpl(this.scenarioData, this.plansCalcRouteKtiInfo, this.ktiConfigGroup);
			loader.loadScenario();
			this.network = this.scenarioData.getNetwork();
			this.population = this.scenarioData.getPopulation();
			this.scenarioLoaded = true;
		}
	}

	@Override
	protected void setUp() {

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
	}

	@Override
	public PlanAlgorithm createRoutingAlgorithm(final PersonalizableTravelCost travelCosts, final PersonalizableTravelTime travelTimes) {
		PlanAlgorithm router = null;
		router = super.createRoutingAlgorithm(travelCosts, travelTimes);
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
