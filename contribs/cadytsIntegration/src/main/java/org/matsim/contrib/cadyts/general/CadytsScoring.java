package org.matsim.contrib.cadyts.general;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.scoring.SumScoringFunction;
import cadyts.calibrators.analytical.AnalyticalCalibrator;

public class CadytsScoring<T> implements SumScoringFunction.BasicScoring {

	private double score = 0.;
	private PlansTranslator<T> plansTranslator;
	private AnalyticalCalibrator<T> matsimCalibrator;
	private Plan plan;
	private final double beta;
	private double weightOfCadytsCorrection = 1.;
	private double agentWeight = 1.0;

	public CadytsScoring(final Plan plan, Config config, final CadytsContextI<T> context) {
		this.plansTranslator = context.getPlansTranslator();
		this.matsimCalibrator = context.getCalibrator();
		this.plan = plan;
		this.beta = config.scoring().getBrainExpBeta();
		// Get PCU weight
		this.agentWeight = context.getAgentWeight(plan.getPerson());
	}

	@Override
	public void finish() {
		cadyts.demand.Plan<T> currentPlanSteps = this.plansTranslator.getCadytsPlan(plan);
		// calcLinearPlanEffect returns the sum of lambdas (corrections per unit flow)
		// We multiply by agentWeight to scale correction to the agent's actual flow contribution
		double currentPlanCadytsCorrection = this.matsimCalibrator.calcLinearPlanEffect(currentPlanSteps) / this.beta;
		this.score = weightOfCadytsCorrection * currentPlanCadytsCorrection * this.agentWeight;
	}

	@Override
	public double getScore() {
		return score;
	}

	public void setWeightOfCadytsCorrection(double weightOfCadytsCorrection) {
		this.weightOfCadytsCorrection = weightOfCadytsCorrection;
	}
}
