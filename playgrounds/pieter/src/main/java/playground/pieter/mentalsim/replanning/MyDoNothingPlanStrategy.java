package playground.pieter.mentalsim.replanning;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.selectors.PlanSelector;
import org.matsim.core.replanning.selectors.RandomPlanSelector;

import playground.pieter.mentalsim.replanning.MyDoNothingPlanStrategyModule;

public class MyDoNothingPlanStrategy implements PlanStrategy {
	PlanStrategy planStrategyDelegate = null;
	
	public MyDoNothingPlanStrategy(Controler controler) {
	    planStrategyDelegate = new PlanStrategyImpl( new RandomPlanSelector() );
	    MyDoNothingPlanStrategyModule mod = new MyDoNothingPlanStrategyModule(controler) ;
	    addStrategyModule(mod) ;
	}

	@Override
	public void addStrategyModule(PlanStrategyModule module) {
		planStrategyDelegate.addStrategyModule(module);

	}

	@Override
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
	public void init() {
		planStrategyDelegate.init();

	}

	@Override
	public void finish() {
		planStrategyDelegate.finish();

	}

	@Override
	public PlanSelector getPlanSelector() {
		// TODO Auto-generated method stub
		return planStrategyDelegate.getPlanSelector();
	}
	
	@Override
	public String toString(){
		return planStrategyDelegate.toString();
	}

}
