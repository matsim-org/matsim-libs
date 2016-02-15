package playground.balac.iduceddemand.strategies;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.selectors.RandomPlanSelector;

import com.google.inject.Inject;
import org.matsim.core.router.TripRouter;

import javax.inject.Provider;


public class RandomActivitiesSwaperStrategy implements PlanStrategy{
	private final PlanStrategy planStrategyDelegate;
	
	@Inject
	public  RandomActivitiesSwaperStrategy(final Scenario scenario, Provider<TripRouter> tripRouterProvider) {
			
	    PlanStrategyImpl.Builder builder = new PlanStrategyImpl.Builder(new RandomPlanSelector<Plan, Person>() );
		RandomActivitySwaper ras = new RandomActivitySwaper(scenario, tripRouterProvider);
		builder.addStrategyModule(ras);
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
