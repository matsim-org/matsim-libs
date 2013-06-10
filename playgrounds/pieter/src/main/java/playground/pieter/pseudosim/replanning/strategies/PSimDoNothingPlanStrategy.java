package playground.pieter.pseudosim.replanning.strategies;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.selectors.PlanSelector;
import org.matsim.core.replanning.selectors.RandomPlanSelector;

import playground.pieter.pseudosim.replanning.modules.PSimDoNothingPlanStrategyModule;

public class PSimDoNothingPlanStrategy implements PlanStrategy {
	PlanStrategyImpl planStrategyDelegate = null;
	
	public PSimDoNothingPlanStrategy(Controler controler) {
	    planStrategyDelegate = new PlanStrategyImpl( new RandomPlanSelector() );
	    PSimDoNothingPlanStrategyModule mod = new PSimDoNothingPlanStrategyModule(controler) ;
	    addStrategyModule(mod) ;
	}

	public void addStrategyModule(PlanStrategyModule module) {
		planStrategyDelegate.addStrategyModule(module);
	}

	public int getNumberOfStrategyModules() {
		// TODO Auto-generated method stub
		return planStrategyDelegate.getNumberOfStrategyModules();
	}

	@Override
	public void run(Person person) {
		// TODO Auto-generated method stub
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

	@Override
	public String toString(){
		return planStrategyDelegate.toString();
	}

}
