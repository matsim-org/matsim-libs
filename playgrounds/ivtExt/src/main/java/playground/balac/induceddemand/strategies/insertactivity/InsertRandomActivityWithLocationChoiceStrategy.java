package playground.balac.induceddemand.strategies.insertactivity;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.locationchoice.BestReplyDestinationChoice;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.contrib.locationchoice.bestresponse.preprocess.MaxDCScoreWrapper;
import org.matsim.core.config.Config;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.TripsToLegsModule;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.utils.collections.QuadTree;
import playground.balac.induceddemand.strategies.InsertRandomActivity;

import javax.inject.Provider;
import java.util.Map;

public class InsertRandomActivityWithLocationChoiceStrategy implements PlanStrategy {
	private PlanStrategyImpl planStrategyDelegate;
	private final QuadTree shopFacilityQuadTree;
	private final QuadTree leisureFacilityQuadTree;
	private Scenario scenario;
	private Provider<TripRouter> tripRouterProvider;
	private ScoringFunctionFactory scoringFunctionFactory;
	private Map<String, TravelTime> travelTimes;
	private Map<String, TravelDisutilityFactory> travelDisutilities;


	@Inject
	public  InsertRandomActivityWithLocationChoiceStrategy(final Scenario scenario,
														   @Named("shopQuadTree") QuadTree shopFacilityQuadTree,
														   @Named("leisureQuadTree") QuadTree leisureFacilityQuadTree, Provider<TripRouter> tripRouterProvider, ScoringFunctionFactory scoringFunctionFactory, Map<String, TravelTime> travelTimes, Map<String, TravelDisutilityFactory> travelDisutilities) {
		
	   // PlanStrategyImpl.Builder builder = new PlanStrategyImpl.Builder(new RandomPlanSelector<Plan, Person>() );
	   
		this.scenario = scenario;
		this.shopFacilityQuadTree = shopFacilityQuadTree;
		this.leisureFacilityQuadTree = leisureFacilityQuadTree;
		this.tripRouterProvider = tripRouterProvider;
		this.scoringFunctionFactory = scoringFunctionFactory;
		this.travelTimes = travelTimes;
		this.travelDisutilities = travelDisutilities;
	}
	
	@Override
	public void run(HasPlansAndId<Plan, Person> person) {
		planStrategyDelegate.run(person);
		
	}

	@Override
	public void init(ReplanningContext replanningContext) {
		 InsertRandomActivity ira = new InsertRandomActivity(scenario, shopFacilityQuadTree,
		    		leisureFacilityQuadTree, tripRouterProvider);
	
		/*
		 * Somehow this is ugly. Should be initialized in the constructor. But I do not know, how to initialize the lc scenario elements
		 * such that they are already available at the time of constructing this object. ah feb'13
		 */
		DestinationChoiceBestResponseContext lcContext = (DestinationChoiceBestResponseContext) scenario.getScenarioElement(DestinationChoiceBestResponseContext.ELEMENT_NAME);
		Config config = lcContext.getScenario().getConfig();
		DestinationChoiceConfigGroup dccg = (DestinationChoiceConfigGroup) config.getModule(DestinationChoiceConfigGroup.GROUP_NAME);
		MaxDCScoreWrapper maxDcScoreWrapper = (MaxDCScoreWrapper)scenario.getScenarioElement(MaxDCScoreWrapper.ELEMENT_NAME);
		if ( !DestinationChoiceConfigGroup.Algotype.bestResponse.equals(dccg.getAlgorithm())) {
			throw new RuntimeException("wrong class for selected location choice algorithm type; aborting ...") ;
		}		
		String planSelector = dccg.getPlanSelector();
		if (planSelector.equals("BestScore")) {
			planStrategyDelegate = new PlanStrategyImpl(new BestPlanSelector<Plan, Person>());
		} else if (planSelector.equals("ChangeExpBeta")) {
			planStrategyDelegate = new PlanStrategyImpl(new ExpBetaPlanChanger(config.planCalcScore().getBrainExpBeta()));
		} else if (planSelector.equals("SelectRandom")) {
			planStrategyDelegate = new PlanStrategyImpl(new RandomPlanSelector());
		} else {
			planStrategyDelegate = new PlanStrategyImpl(new ExpBetaPlanSelector(config.planCalcScore()));
		}
		planStrategyDelegate.addStrategyModule(new TripsToLegsModule(scenario.getConfig()));
		planStrategyDelegate.addStrategyModule(ira);
		planStrategyDelegate.addStrategyModule(new BestReplyDestinationChoice(tripRouterProvider, lcContext, maxDcScoreWrapper.getPersonsMaxDCScoreUnscaled(), scoringFunctionFactory, travelTimes, travelDisutilities));
		planStrategyDelegate.addStrategyModule(new ReRoute(lcContext.getScenario(), tripRouterProvider));
		planStrategyDelegate.init(replanningContext);
		
	}

	@Override
	public void finish() {
		planStrategyDelegate.finish();
		
	}

}
