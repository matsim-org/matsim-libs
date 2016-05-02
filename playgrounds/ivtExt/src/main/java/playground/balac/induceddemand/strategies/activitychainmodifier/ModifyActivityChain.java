package playground.balac.induceddemand.strategies.activitychainmodifier;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Provider;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelScoringParametersForPerson;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.population.algorithms.PlanAlgorithm;

public class ModifyActivityChain extends AbstractMultithreadedModule{
	
	private Provider<TripRouter> tripRouterProvider;
	private Scenario scenario;
	private final QuadTree shopFacilityQuadTree;
	private final QuadTree leisureFacilityQuadTree;
	private LeastCostPathCalculatorFactory pathCalculatorFactory;
	private Map<String, TravelTime> travelTimes;
	private Map<String, TravelDisutilityFactory> travelDisutilityFactories;
	private ScoringFunctionFactory scoringFunctionFactory;
	private HashMap scoreChange;
	private CharyparNagelScoringParametersForPerson parametersForPerson;
	public ModifyActivityChain(final Scenario scenario, Provider<TripRouter> tripRouterProvider,
			 QuadTree shopFacilityQuadTree, QuadTree leisureFacilityQuadTree,
			 LeastCostPathCalculatorFactory pathCalculatorFactory, Map<String, TravelTime> travelTimes,
			 Map<String, TravelDisutilityFactory> travelDisutilityFactories, ScoringFunctionFactory scoringFunctionFactory,
			 HashMap scoreChange, CharyparNagelScoringParametersForPerson parametersForPerson) {
		
		super(scenario.getConfig().global().getNumberOfThreads());
		this.scenario = scenario;
		this.tripRouterProvider = tripRouterProvider;
		this.shopFacilityQuadTree = shopFacilityQuadTree;
		this.leisureFacilityQuadTree = leisureFacilityQuadTree;
		this.pathCalculatorFactory = pathCalculatorFactory;
		this.travelTimes = travelTimes;
		this.travelDisutilityFactories = travelDisutilityFactories;
		this.scoringFunctionFactory = scoringFunctionFactory;
		this.scoreChange = scoreChange;
		this.parametersForPerson = parametersForPerson;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		final TripRouter tripRouter = tripRouterProvider.get();
		TravelTime travelTime = travelTimes.get( TransportMode.car ) ;

		TravelDisutilityFactory travelDisutilityFactory = travelDisutilityFactories.get( TransportMode.car ) ;
		TravelDisutility travelDisutility = travelDisutilityFactory.createTravelDisutility(travelTime) ;
		LeastCostPathCalculator pathCalculator = pathCalculatorFactory.createPathCalculator(scenario.getNetwork(), travelDisutility, travelTime ) ;
		ModifyAndChooseChain algo = new ModifyAndChooseChain(MatsimRandom.getLocalInstance(), tripRouter.getStageActivityTypes(),
				this.scenario, pathCalculator, this.shopFacilityQuadTree, this.leisureFacilityQuadTree, scoringFunctionFactory,
				this.scoreChange, this.parametersForPerson);

		return algo;
	}

}
