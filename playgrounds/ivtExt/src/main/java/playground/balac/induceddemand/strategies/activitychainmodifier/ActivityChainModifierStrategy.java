package playground.balac.induceddemand.strategies.activitychainmodifier;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Provider;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelScoringParametersForPerson;
import org.matsim.core.utils.collections.QuadTree;

import com.google.inject.Inject;
import com.google.inject.name.Named;


public class ActivityChainModifierStrategy implements PlanStrategy{
	private final PlanStrategy planStrategyDelegate;
	
	

	@Inject
	public ActivityChainModifierStrategy(final Scenario scenario, 
			Provider<TripRouter> tripRouterProvider, @Named("shopQuadTree") QuadTree shopFacilityQuadTree,
			   @Named("leisureQuadTree") QuadTree leisureFacilityQuadTree,
			   LeastCostPathCalculatorFactory pathCalculatorFactory, Map<String,TravelTime> travelTimes,
			   Map<String,TravelDisutilityFactory> travelDisutilityFactories, ScoringFunctionFactory scoringFunctionFactory,
			   @Named("scoreChangeMap") HashMap scoreChange, CharyparNagelScoringParametersForPerson parametersForPerson) {
		PlanStrategyImpl.Builder builder = new PlanStrategyImpl.Builder(new RandomPlanSelector<Plan, Person>() );
		
		
		
	    ModifyActivityChain mac = new ModifyActivityChain(scenario, tripRouterProvider,
	    		shopFacilityQuadTree, leisureFacilityQuadTree, pathCalculatorFactory, travelTimes,
	    		travelDisutilityFactories,scoringFunctionFactory, scoreChange, parametersForPerson);
	    
		builder.addStrategyModule(mac);
		builder.addStrategyModule(new ReRoute(scenario, tripRouterProvider));
		
		planStrategyDelegate = builder.build();	
		
	}
	@Override
	public void run(HasPlansAndId<Plan, Person> person) {
		planStrategyDelegate.run(person);
		
	}

	@Override
	public void init(ReplanningContext replanningContext) {
		planStrategyDelegate.init(replanningContext);
	}

	@Override
	public void finish() {
		planStrategyDelegate.finish();

	}

}
