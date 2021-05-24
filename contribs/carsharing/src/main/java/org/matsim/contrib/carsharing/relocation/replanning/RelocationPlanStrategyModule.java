package org.matsim.contrib.carsharing.relocation.replanning;

import javax.inject.Provider;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.carsharing.manager.demand.membership.MembershipContainer;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.router.TripRouter;

import com.google.inject.Inject;

public class RelocationPlanStrategyModule implements PlanStrategy, IterationStartsListener {
	Scenario scenario;
	int iteration = 0;
	private final PlanStrategyImpl strategy;
	@Inject
	public RelocationPlanStrategyModule(final Scenario scenario, Provider<TripRouter> tripRouterProvider, MembershipContainer memberships) {
		this.strategy = new PlanStrategyImpl(new RelocationPlanSelector());		
	}
	
	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		iteration = event.getIteration();
		
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
