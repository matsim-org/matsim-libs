package playground.balac.induceddemand.strategies;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.utils.collections.QuadTree;

public class InsertRandomActivityStrategy implements PlanStrategy {
	private final PlanStrategy planStrategyDelegate;

		
	@Inject
	public  InsertRandomActivityStrategy(final Scenario scenario, 
			@Named("shopQuadTree") QuadTree shopFacilityQuadTree,
			@Named("leisureQuadTree") QuadTree leisureFacilityQuadTree) {
		
	    PlanStrategyImpl.Builder builder = new PlanStrategyImpl.Builder(new RandomPlanSelector<Plan, Person>() );
	    InsertRandomActivity ira = new InsertRandomActivity(scenario, shopFacilityQuadTree,
	    		leisureFacilityQuadTree);
	    
		builder.addStrategyModule(ira);
		builder.addStrategyModule(new ReRoute(scenario));
		
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
