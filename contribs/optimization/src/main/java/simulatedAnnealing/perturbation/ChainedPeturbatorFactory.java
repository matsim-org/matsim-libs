/*
 * Copyright (C) 2023 MOIA GmbH - All Rights Reserved
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package simulatedAnnealing.perturbation;

import org.matsim.contrib.common.util.random.WeightedRandomSelection;
import org.matsim.core.gbl.MatsimRandom;
import simulatedAnnealing.SimulatedAnnealing;

import java.util.*;

/**
 * @author nkuehnel / MOIA
 */
public class ChainedPeturbatorFactory<T> implements PerturbatorFactory<T> {


	private final List<PerturbatorFactory<T>> perturbatorFactories;
	private final WeightedRandomSelection<PerturbatorFactory<T>> perturbatorFactorySampler;

	private final Random random = MatsimRandom.getLocalInstance();
	private final int maxPerturbations;

	private final int minPerturbations;

	private final double initialTemperature;

	private ChainedPeturbatorFactory(Map<PerturbatorFactory<T>, Double> perturbatorFactories,
									 int minPerturbations,
									 int maxPerturbations,
									 double initialTemperature) {
		this.perturbatorFactorySampler = new WeightedRandomSelection<>();
		for (Map.Entry<PerturbatorFactory<T>, Double> entry : perturbatorFactories.entrySet()) {
			perturbatorFactorySampler.add(entry.getKey(), entry.getValue());
		}
		this.perturbatorFactories = new ArrayList<>(perturbatorFactories.keySet());
		this.minPerturbations = minPerturbations;
		this.maxPerturbations = maxPerturbations;
		this.initialTemperature = initialTemperature;
	}


	@Override
	public Perturbator<T> createPerturbator(int iteration, double temperature) {
		int perturbations = random.nextInt(minPerturbations, Math.max(minPerturbations + 1, Math.min(maxPerturbations, (int) Math.ceil(temperature / initialTemperature * maxPerturbations))));
		ChainedPerturbator.Builder<T> builder = new ChainedPerturbator.Builder<>();
		for (int i = 0; i < perturbations; i++) {
			PerturbatorFactory<T> perturbatorFactory = perturbatorFactorySampler.select();
			builder.add(perturbatorFactory.createPerturbator(iteration, temperature));
		}
		return builder.build();
	}

	public void solutionAccepted(SimulatedAnnealing.Solution<T> oldSolution,
                                 SimulatedAnnealing.Solution<T> newSolution) {
		for (PerturbatorFactory<T> perturbatorFactory : perturbatorFactories) {
			perturbatorFactory.solutionAccepted(oldSolution, newSolution);
		}
	}

	public void reset(int iteration){
		for (PerturbatorFactory<T> perturbatorFactory : perturbatorFactories) {
			perturbatorFactory.reset(iteration);
		}
	}


	public static class Builder<T> {

		private final Map<PerturbatorFactory<T>, Double> perturbatorFactories = new HashMap<>();

		private int maxPerturbations = 10;

		private int minPerturbations = 1;

		private double initialTemperature = Double.POSITIVE_INFINITY;

		public Builder<T> add(PerturbatorFactory<T> perturbatorFactory, double weight) {
			perturbatorFactories.put(perturbatorFactory, weight);
			return this;
		}

		public Builder<T> maxPerturbations(int n) {
			maxPerturbations = n;
			return this;
		}

		public Builder<T> minPerturbations(int n) {
			minPerturbations = n;
			return this;
		}

		public Builder<T> initialTemperature(double initialTemperature) {
			this.initialTemperature = initialTemperature;
			return this;
		}

		public ChainedPeturbatorFactory<T> build() {
			return new ChainedPeturbatorFactory<>(perturbatorFactories, minPerturbations, maxPerturbations, initialTemperature);
		}

	}
}
