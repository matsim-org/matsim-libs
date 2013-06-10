package playground.pieter.pseudosim.replanning.modules;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.replanning.ReplanningContext;

import playground.pieter.pseudosim.controler.PseudoSimControler;

public class PSimPlanMarkerModule implements PlanStrategyModule {

	private PseudoSimControler controler;

	public PSimPlanMarkerModule(PseudoSimControler controler) {
		this.controler = controler;
	}

	@Override
	public void prepareReplanning(ReplanningContext replanningContext) {

	}

	@Override
	public void handlePlan(Plan plan) {
		controler.addPlanForPseudoSimulation(plan);
	}

	@Override
	public void finishReplanning() {

	}

}
