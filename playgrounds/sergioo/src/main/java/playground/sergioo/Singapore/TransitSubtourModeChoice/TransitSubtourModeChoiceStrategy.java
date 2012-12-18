package playground.sergioo.Singapore.TransitSubtourModeChoice;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.selectors.PlanSelector;
import org.matsim.core.replanning.selectors.RandomPlanSelector;

import playground.sergioo.Singapore.TransitLocationChoice.TransitActsRemoverStrategy;

public class TransitSubtourModeChoiceStrategy implements PlanStrategy {

	private PlanStrategyImpl delegate;
	
	public TransitSubtourModeChoiceStrategy(Controler controler) {
		delegate = new PlanStrategyImpl(new RandomPlanSelector());
		delegate.addStrategyModule(new TransitActsRemoverStrategy(controler.getConfig()));
		delegate.addStrategyModule(new SubtourModeChoice(controler.getConfig()));
		delegate.addStrategyModule(new ReRoute(controler.getScenario()));
	}
	
	public void addStrategyModule(PlanStrategyModule module) {
		delegate.addStrategyModule(module);
	}

	public int getNumberOfStrategyModules() {
		return delegate.getNumberOfStrategyModules();
	}

	@Override
	public void run(Person person) {
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
