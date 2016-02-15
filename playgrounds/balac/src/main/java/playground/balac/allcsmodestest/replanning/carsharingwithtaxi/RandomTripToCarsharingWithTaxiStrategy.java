package playground.balac.allcsmodestest.replanning.carsharingwithtaxi;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.TripRouter;

import javax.inject.Provider;


public class RandomTripToCarsharingWithTaxiStrategy implements PlanStrategy{
	private final PlanStrategyImpl strategy;
	
	public RandomTripToCarsharingWithTaxiStrategy(final Scenario scenario, Provider<TripRouter> tripRouterProvider) {
		this.strategy = new PlanStrategyImpl( new RandomPlanSelector<Plan, Person>() );
		 	
		//addStrategyModule( new TripsToLegsModule(controler.getConfig() ) );   //lets try without this, not sure if it is needed
		CarsharingWithTaxiTripModeChoice smc = new CarsharingWithTaxiTripModeChoice(tripRouterProvider, scenario);
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
