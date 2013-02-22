package playground.pieter.mentalsim.replanning;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.replanning.ReplanningContext;

import playground.pieter.mentalsim.controler.MentalSimControler;

public class MentalSimPlanMarkerModule implements PlanStrategyModule {

	private MentalSimControler controler;

	public MentalSimPlanMarkerModule(MentalSimControler controler) {
		this.controler = controler;
	}

	@Override
	public void prepareReplanning(ReplanningContext replanningContext) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handlePlan(Plan plan) {
		// TODO Auto-generated method stub
		plan.setScore(0.0);
		controler.addPlanForMentalSimulation(plan);
	}

	@Override
	public void finishReplanning() {
		// TODO Auto-generated method stub

	}

}
