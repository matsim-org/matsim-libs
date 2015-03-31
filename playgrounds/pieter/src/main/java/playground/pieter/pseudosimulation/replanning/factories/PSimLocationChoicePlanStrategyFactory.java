package playground.pieter.pseudosimulation.replanning.factories;

import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyFactory;
import playground.pieter.pseudosimulation.controler.PSimControler;
import playground.pieter.pseudosimulation.replanning.strategies.PSimLocationChoicePlanStrategy;

public class PSimLocationChoicePlanStrategyFactory implements PlanStrategyFactory {

	public PSimLocationChoicePlanStrategyFactory(PSimControler controler) {
		super();
		this.controler = controler;
	}

	private final PSimControler controler;

	@Override
	public PlanStrategy get() {
		return new PSimLocationChoicePlanStrategy(controler.getScenario(), controler);
	}


}
