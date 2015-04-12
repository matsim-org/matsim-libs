package org.matsim.contrib.pseudosimulation.replanning.modules;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.replanning.ReplanningContext;

import org.matsim.contrib.pseudosimulation.controler.PSimControler;

public class PSimPlanMarkerModule implements PlanStrategyModule {

	private final PSimControler controler;

	public PSimPlanMarkerModule(PSimControler controler) {
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
