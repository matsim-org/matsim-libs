package org.matsim.contrib.cadyts.general;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.selectors.PlanSelector;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import java.util.Map;

public class CadytsPlanChanger<T> implements PlanSelector<Plan, Person> {

	private final double beta ;
	private double cadytsWeight = 1.0;
	private CadytsContextI<T> cadytsContext;

	public static final String CADYTS_CORRECTION = "cadytsCorrection";

	public CadytsPlanChanger(Scenario scenario, CadytsContextI<T> cadytsContext) {
		this.cadytsContext = cadytsContext;
		this.beta = scenario.getConfig().scoring().getBrainExpBeta() ;
	}

	@Override
	public Plan selectPlan(final HasPlansAndId<Plan, Person> person) {
		final Plan currentPlan = person.getSelectedPlan();
		if (person.getPlans().size() <= 1 || currentPlan.getScore() == null) {
			return currentPlan;
		}

		double pcu = 1.0;
		if (person instanceof Person) {
			pcu = cadytsContext.getAgentWeight((Person) person);
		}

		Plan otherPlan;
		do {
			otherPlan = new RandomPlanSelector<Plan, Person>().selectPlan((person));
		} while (otherPlan == currentPlan);

		if (otherPlan.getScore() == null) return otherPlan;

		cadyts.demand.Plan<T> currentPlanSteps = this.cadytsContext.getPlansTranslator().getCadytsPlan(currentPlan);
		// Scale by PCU
		double currentPlanCadytsCorrection = (this.cadytsContext.getCalibrator().calcLinearPlanEffect(currentPlanSteps) / this.beta) * pcu;
		double currentScore = currentPlan.getScore() + cadytsWeight * currentPlanCadytsCorrection;

		cadyts.demand.Plan<T> otherPlanSteps = this.cadytsContext.getPlansTranslator().getCadytsPlan(otherPlan);
		// Scale by PCU
		double otherPlanCadytsCorrection = (this.cadytsContext.getCalibrator().calcLinearPlanEffect(otherPlanSteps) / this.beta) * pcu;
		double otherScore = otherPlan.getScore() + cadytsWeight * otherPlanCadytsCorrection;

		Map<String,Object> planAttributes = currentPlan.getCustomAttributes() ;
		planAttributes.put(CadytsPlanChanger.CADYTS_CORRECTION,currentPlanCadytsCorrection) ;

		Map<String,Object> planAttributesOther = otherPlan.getCustomAttributes() ;
		planAttributesOther.put(CadytsPlanChanger.CADYTS_CORRECTION,otherPlanCadytsCorrection) ;

		double weight = Math.exp(0.5 * this.beta * (otherScore - currentScore));

		Plan selectedPlan = currentPlan;
		if (MatsimRandom.getRandom().nextDouble() < 0.01 * weight) {
			selectedPlan = otherPlan;
		}
		return selectedPlan;
	}

	public void setCadytsWeight(double cadytsWeight) {
		this.cadytsWeight = cadytsWeight;
	}
}
