package playground.balac.freefloating.replanning;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.selectors.RandomPlanSelector;


public class FreeFloatingChangeLegModeStrategy implements PlanStrategy{
	private final PlanStrategyImpl strategy;
	
	public FreeFloatingChangeLegModeStrategy(final Scenario controler) {
		this.strategy = new PlanStrategyImpl( new RandomPlanSelector<Plan>() );

		FreeFloatingTripModeChoice smc = new FreeFloatingTripModeChoice(controler.getConfig());

		addStrategyModule(smc );
		addStrategyModule( new ReRoute(controler) );
	}
	public void addStrategyModule(final PlanStrategyModule module) {
		strategy.addStrategyModule(module);
	}
	@Override
	public void run(HasPlansAndId<Plan> person) {
		strategy.run(person);
		
	}

	@Override
	public void init(ReplanningContext replanningContext) {
		strategy.init(replanningContext);
		
	}

	@Override
	public void finish() {
		// TODO Auto-generated method stub
		strategy.finish();
	}
	@Override
	public String toString() {
		return strategy.toString();
	}
}
