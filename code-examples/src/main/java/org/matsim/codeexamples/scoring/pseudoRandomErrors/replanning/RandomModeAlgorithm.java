package org.matsim.codeexamples.scoring.pseudoRandomErrors.replanning;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.algorithms.PlanAlgorithm;

public class RandomModeAlgorithm implements PlanAlgorithm {
	@Override
	public void run(Plan plan) {
		Random random = MatsimRandom.getLocalInstance();
		List<String> modes = Arrays.asList("car", "pt");

		for (PlanElement element : plan.getPlanElements()) {
			if (element instanceof Leg) {
				Leg leg = (Leg) element;
				leg.setMode(modes.get(random.nextInt(modes.size())));
			}
		}
	}
}
