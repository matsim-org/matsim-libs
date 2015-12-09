package playground.sergioo.singapore2012.transitSubtourModeChoice;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.core.replanning.selectors.RandomPlanSelector;

import org.matsim.core.router.TripRouter;
import playground.sergioo.singapore2012.transitLocationChoice.TransitActsRemoverStrategy;

import javax.inject.Provider;

public class TransitSubtourModeChoiceStrategy implements PlanStrategy {
	private static final Logger log =
		Logger.getLogger(TransitSubtourModeChoiceStrategy.class);


	private PlanStrategyImpl delegate;
	
	public TransitSubtourModeChoiceStrategy(Scenario scenario, Provider<TripRouter> tripRouterProvider) {
		delegate = new PlanStrategyImpl(new RandomPlanSelector());
		delegate.addStrategyModule(new TransitActsRemoverStrategy(scenario.getConfig()));
		log.warn( "your stategy now uses vanilla SubtourModeChoice, not a hacked copy thereof" );
		log.warn( "just set config.subtourModeChoice.considerCarAvailability to true in the config to get the same behavior" );
		log.warn( "... but actually, you may just delete this strategy altogether, it does not provide anything matsim doesn't provide. td, 22. feb. 2013" );
		delegate.addStrategyModule(new SubtourModeChoice(scenario.getConfig(), tripRouterProvider));
		delegate.addStrategyModule(new ReRoute(scenario, tripRouterProvider));
	}
	
	public void addStrategyModule(PlanStrategyModule module) {
		delegate.addStrategyModule(module);
	}

	public int getNumberOfStrategyModules() {
		return delegate.getNumberOfStrategyModules();
	}

	@Override
	public void run(HasPlansAndId<Plan, Person> person) {
		delegate.run(person);
	}

	@Override
	public void init(ReplanningContext replanningContext) {
		delegate.init(replanningContext);
	}

	@Override
	public void finish() {
		delegate.finish();
	}


}
