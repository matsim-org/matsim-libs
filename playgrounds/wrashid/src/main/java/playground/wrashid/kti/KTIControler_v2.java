package playground.wrashid.kti;

import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.locationchoice.facilityload.FacilitiesLoadCalculator;
import org.matsim.contrib.locationchoice.facilityload.FacilityPenalty;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.roadpricing.RoadPricing;

import playground.meisterk.kti.config.KtiConfigGroup;
import playground.meisterk.kti.controler.KTIControler;
import playground.meisterk.kti.controler.listeners.CalcLegTimesKTIListener;
import playground.meisterk.kti.controler.listeners.KtiPopulationPreparation;
import playground.meisterk.kti.controler.listeners.LegDistanceDistributionWriter;
import playground.meisterk.kti.controler.listeners.ScoreElements;
import playground.meisterk.kti.router.KtiLinkNetworkRouteFactory;
import playground.meisterk.kti.router.KtiPtRouteFactory;
import playground.meisterk.kti.router.KtiTravelCostCalculatorFactory;
import playground.meisterk.kti.router.PlansCalcRouteKti;
import playground.meisterk.kti.router.PlansCalcRouteKtiInfo;
import playground.meisterk.kti.scenario.KtiScenarioLoaderImpl;
import playground.meisterk.kti.scoring.KTIYear3ScoringFunctionFactory;
import playground.meisterk.org.matsim.config.PlanomatConfigGroup;

public class KTIControler_v2 extends Controler {
	
	protected static final String SCORE_ELEMENTS_FILE_NAME = "scoreElementsAverages.txt";
	protected static final String CALC_LEG_TIMES_KTI_FILE_NAME = "calcLegTimesKTI.txt";
	protected static final String LEG_DISTANCE_DISTRIBUTION_FILE_NAME = "legDistanceDistribution.txt";
	protected static final String LEG_TRAVEL_TIME_DISTRIBUTION_FILE_NAME = "legTravelTimeDistribution.txt";

	private final KtiConfigGroup ktiConfigGroup = new KtiConfigGroup();
	private final PlansCalcRouteKtiInfo plansCalcRouteKtiInfo = new PlansCalcRouteKtiInfo(ktiConfigGroup);

	public KTIControler_v2(String[] args) {
		super(args);

		super.config.addModule(KtiConfigGroup.GROUP_NAME, this.ktiConfigGroup);

		((PopulationFactoryImpl) this.getPopulation().getFactory()).setRouteFactory(TransportMode.car, new KtiLinkNetworkRouteFactory(this.getNetwork(), new PlanomatConfigGroup()));
		((PopulationFactoryImpl) this.getPopulation().getFactory()).setRouteFactory(TransportMode.pt, new KtiPtRouteFactory(this.plansCalcRouteKtiInfo));

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
				getScenario(),
				this.ktiConfigGroup,
				new TreeMap<Id, FacilityPenalty>(),
				this.getFacilities());
		this.setScoringFunctionFactory(kTIYear3ScoringFunctionFactory);

		KtiTravelCostCalculatorFactory costCalculatorFactory = new KtiTravelCostCalculatorFactory(ktiConfigGroup);
		this.setTravelDisutilityFactory(costCalculatorFactory);

		super.setUp();
	}


	@Override
	protected void loadControlerListeners() {

		super.loadControlerListeners();

		// the scoring function processes facility loads
		TreeMap<Id, FacilityPenalty> t=new TreeMap<Id, FacilityPenalty>();
		this.addControlerListener(new FacilitiesLoadCalculator(t));
		this.addControlerListener(new ScoreElements(SCORE_ELEMENTS_FILE_NAME));
		this.addControlerListener(new CalcLegTimesKTIListener(CALC_LEG_TIMES_KTI_FILE_NAME, LEG_TRAVEL_TIME_DISTRIBUTION_FILE_NAME));
		this.addControlerListener(new LegDistanceDistributionWriter(LEG_DISTANCE_DISTRIBUTION_FILE_NAME));
		this.addControlerListener(new KtiPopulationPreparation(this.ktiConfigGroup));
	}

	@Override
	public PlanAlgorithm createRoutingAlgorithm() {
		return this.ktiConfigGroup.isUsePlansCalcRouteKti() ?
				createKtiRoutingAlgorithm(
						this.createTravelCostCalculator(),
						this.getLinkTravelTimes()) :
				super.createRoutingAlgorithm();
	}

	public PlanAlgorithm createKtiRoutingAlgorithm(final TravelDisutility travelCosts, final TravelTime travelTimes) {
		return new PlansCalcRouteKti(
					super.getConfig().plansCalcRoute(),
					super.network,
					travelCosts,
					travelTimes,
					super.getLeastCostPathCalculatorFactory(),
					((PopulationFactoryImpl) this.population.getFactory()).getModeRouteFactory(),
					this.plansCalcRouteKtiInfo);
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
			final Controler controler = new KTIControler_v2(args);
			controler.setOverwriteFiles(true);
			controler.run();
		}
		System.exit(0);
	}

}
