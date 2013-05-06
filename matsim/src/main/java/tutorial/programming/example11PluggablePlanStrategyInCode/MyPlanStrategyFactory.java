package tutorial.programming.example11PluggablePlanStrategyInCode;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.selectors.RandomPlanSelector;


public class MyPlanStrategyFactory implements PlanStrategyFactory {

	@Override
	public PlanStrategy createPlanStrategy(Scenario scenario, EventsManager eventsManager) {
		// also possible: MyStrategy( Scenario scenario ).  But then I do not have events.  kai, aug'10

		// A PlanStrategy is something that can be applied to a person(!).  

		// It first selects one of the plans:
		PlanStrategyImpl planStrategy = new PlanStrategyImpl( new MyPlanSelector(scenario) );
		// alternative:
//		PlanStrategyImpl planStrategy = new PlanStrategyImpl( new RandomPlanSelector() );

		// if you just want to select plans, you can stop here.  

		// Otherwise, to do something with that plan, one needs to add modules into the strategy.  If there is at least 
		// one module added here, then the plan is copied and then modified.
		MyPlanStrategyModule mod = new MyPlanStrategyModule( scenario ) ;
		planStrategy.addStrategyModule(mod) ;

		// these modules may, at the same time, be events listeners (so that they can collect information):
		eventsManager.addHandler( mod ) ;

		
		return planStrategy;
	}

}
