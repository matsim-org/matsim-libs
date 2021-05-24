package org.matsim.contribs.discrete_mode_choice.model.nested;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.matsim.contribs.discrete_mode_choice.model.utilities.UtilityCandidate;
import org.matsim.contribs.discrete_mode_choice.model.utilities.UtilitySelector;
import org.matsim.contribs.discrete_mode_choice.model.utilities.UtilitySelectorFactory;

public class NestedLogitSelector implements UtilitySelector {
	private final List<NestedUtilityCandidate> candidates = new LinkedList<>();
	private final List<UtilityCandidate> plainCandidates = new LinkedList<>();

	private final NestCalculator calculator;
	private final Nest rootNest;

	private final double minimumUtility;
	private final double maximumUtility;

	public NestedLogitSelector(NestStructure structure, double minimumUtility, double maximumUtility) {
		this.minimumUtility = minimumUtility;
		this.maximumUtility = maximumUtility;

		this.calculator = new NestCalculator(structure);
		this.rootNest = structure.getRoot();
	}

	@Override
	public void addCandidate(UtilityCandidate candidate) {
		NestedUtilityCandidate nestedCandidate;

		if (candidate instanceof NestedUtilityCandidate) {
			nestedCandidate = (NestedUtilityCandidate) candidate;
		} else {
			nestedCandidate = new WrappingNestedUtilityCandidate(candidate, rootNest);
		}

		// TODO: Add more verbosity here
		if (nestedCandidate.getUtility() > minimumUtility) {
			if (nestedCandidate.getUtility() < maximumUtility) {
				candidates.add(nestedCandidate);
				calculator.addCandidate(nestedCandidate);
				plainCandidates.add(candidate);
			}
		}
	}

	@Override
	public Optional<UtilityCandidate> select(Random random) {
		// I) If not candidates are available, give back nothing
		if (candidates.size() == 0) {
			return Optional.empty();
		}

		// II) Create a probability distribution over candidates
		List<Double> density = new ArrayList<>(candidates.size());

		for (NestedUtilityCandidate candidate : candidates) {
			double probability = calculator.calculateProbability(candidate);

			if (!Double.isFinite(probability)) {
				probability = 0.0; // Revise this, maybe it is better to cosntruct the process so we don't have
									// them at all. TODO.
			}

			density.add(probability);
		}

		// III) Build a cumulative density of the distribution
		List<Double> cumulativeDensity = new ArrayList<>(density.size());
		double totalDensity = 0.0;

		for (int i = 0; i < density.size(); i++) {
			totalDensity += density.get(i);
			cumulativeDensity.add(totalDensity);
		}

		// V) Perform a selection using the CDF
		double pointer = random.nextDouble() * totalDensity;

		int selection = (int) cumulativeDensity.stream().filter(f -> f < pointer).count();
		return Optional.of(plainCandidates.get(selection));
	}

	static public class Factory implements UtilitySelectorFactory {
		private final NestStructure structure;
		private final double minimumUtility;
		private final double maximumUtility;

		public Factory(NestStructure structure, double minimumUtility, double maximumUtility) {
			this.structure = structure;
			this.minimumUtility = minimumUtility;
			this.maximumUtility = maximumUtility;
		}

		@Override
		public UtilitySelector createUtilitySelector() {
			return new NestedLogitSelector(structure, minimumUtility, maximumUtility);
		}
	}
}