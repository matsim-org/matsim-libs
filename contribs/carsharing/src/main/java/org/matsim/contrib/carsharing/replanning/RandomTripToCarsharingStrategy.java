package org.matsim.contrib.carsharing.replanning;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.contrib.carsharing.manager.demand.membership.MembershipContainer;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.selectors.RandomPlanSelector;

import com.google.inject.Inject;
import org.matsim.core.router.TripRouter;

import javax.inject.Provider;


public class RandomTripToCarsharingStrategy implements PlanStrategy{

	private final PlanStrategyImpl strategy;
	@Inject
	public RandomTripToCarsharingStrategy(final Scenario scenario, Provider<TripRouter> tripRouterProvider, MembershipContainer memberships) {
		this.strategy = new PlanStrategyImpl( new RandomPlanSelector<Plan, Person>() );
		 	
		//addStrategyModule( new TripsToLegsModule(controler.getConfig() ) );   //lets try without this, not sure if it is needed
		CarsharingTripModeChoice smc = new CarsharingTripModeChoice(tripRouterProvider, scenario, memberships);
		addStrategyModule(smc );
		addStrategyModule( new ReRoute(scenario, tripRouterProvider) );
	}
	public void addStrategyModule(final PlanStrategyModule module) {
		strategy.addStrategyModule(module);
	}
	@Override
	public void run(HasPlansAndId<Plan, Person> person) {
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
