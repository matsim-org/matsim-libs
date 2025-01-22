package org.matsim.contrib.ev.strategic.replanning;

import java.util.Random;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.ev.strategic.plan.ChargingPlan;
import org.matsim.contrib.ev.strategic.plan.ChargingPlans;
import org.matsim.contrib.ev.strategic.replanning.innovator.ChargingPlanInnovator;
import org.matsim.contrib.ev.strategic.replanning.selector.ChargingPlanSelector;
import org.matsim.contrib.ev.withinday.WithinDayEvEngine;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.algorithms.PlanAlgorithm;

/**
 * This class is a PlanAlgorithm that manages the selection and innovation of
 * charging plans for a regular MATSim plan.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class StrategicChargingReplanningAlgorithm implements PlanAlgorithm {
	private final ChargingPlanSelector selector;
	private final ChargingPlanInnovator creator;

	private final Random random;
	private final double selectionProbability;
	private final int maximumPlans;

	public StrategicChargingReplanningAlgorithm(ChargingPlanSelector selector, ChargingPlanInnovator creator,
			double selectionProbability, int maximumPlans) {
		this.selector = selector;
		this.creator = creator;
		this.random = MatsimRandom.getLocalInstance();
		this.selectionProbability = selectionProbability;
		this.maximumPlans = maximumPlans;
	}

	@Override
	public void run(Plan plan) {
		if (WithinDayEvEngine.isActive(plan.getPerson())) {
			ChargingPlans chargingPlans = ChargingPlans.get(plan);

			if (chargingPlans.getChargingPlans().size() > 0 && random.nextDouble() <= selectionProbability) {
				chargingPlans.setSelectedPlan(selector.select(plan.getPerson(), plan, chargingPlans));
			} else {
				while (chargingPlans.getChargingPlans().size() >= maximumPlans) {
					removeWorst(chargingPlans);
				}

				ChargingPlan chargingPlan = creator.createChargingPlan(plan.getPerson(), plan, chargingPlans);

				chargingPlans.addChargingPlan(chargingPlan);
				chargingPlans.setSelectedPlan(chargingPlan);
			}
		}
	}

	private void removeWorst(ChargingPlans chargingPlans) {
		ChargingPlan removal = chargingPlans.getChargingPlans().stream()
				.sorted((a, b) -> Double.compare(a.getScore(), b.getScore())).findFirst().get();
		chargingPlans.removeChargingPlan(removal);
	}
}
