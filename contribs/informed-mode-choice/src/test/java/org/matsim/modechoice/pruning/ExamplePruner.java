package org.matsim.modechoice.pruning;

import org.matsim.modechoice.PlanCandidate;
import org.matsim.modechoice.PlanModel;

import java.util.List;
import java.util.Random;

public class ExamplePruner implements CandidatePruner {

	private final String mode;

	public ExamplePruner(String mode) {
		this.mode = mode;
	}

	@Override
	public void pruneCandidates(PlanModel model, List<PlanCandidate> candidates, Random rnd) {

        candidates.removeIf(candidate -> candidate.containsMode(mode));
	}

	@Override
	public double planThreshold(PlanModel planModel) {
		return Double.POSITIVE_INFINITY;
	}

	@Override
	public double tripThreshold(PlanModel planModel, int idx) {
		return Double.POSITIVE_INFINITY;
	}
}
