package org.matsim.contrib.optimization.simulatedAnnealing.perturbation;

import org.matsim.contrib.util.random.WeightedRandomSelection;
import org.matsim.core.gbl.MatsimRandom;

import java.util.Random;

/**
 * @author nkuehnel
 */
public class ChainedPeturbatorFactory<T> implements PerturbatorFactory<T> {


	private final WeightedRandomSelection<PerturbatorFactory<T>> perturbatorFactories;

	private final Random random = MatsimRandom.getLocalInstance();
	private final int maxPerturbations;

	private ChainedPeturbatorFactory(WeightedRandomSelection<PerturbatorFactory<T>> perturbatorFactories, int maxPerturbations) {
		this.perturbatorFactories = perturbatorFactories;
		this.maxPerturbations = maxPerturbations;
	}


	@Override
	public Perturbator<T> createPerturbator() {
		int perturbations = random.nextInt(1, maxPerturbations +1);
		ChainedPerturbator.Builder<T> builder = new ChainedPerturbator.Builder<>();
		for (int i = 0; i < perturbations; i++) {
			PerturbatorFactory<T> perturbatorFactory = perturbatorFactories.select();
			builder.add(perturbatorFactory.createPerturbator());
		}
		return builder.build();
	}


	public static class Builder<T> {

		private final WeightedRandomSelection<PerturbatorFactory<T>> perturbatorFactories = new WeightedRandomSelection<>();

		private int maxPerturbations = 1;

		public Builder<T> add(PerturbatorFactory<T> perturbatorFactory, double weight) {
			perturbatorFactories.add(perturbatorFactory, weight);
			return this;
		}

		public Builder<T> maxPerturbations(int n) {
			maxPerturbations = n;
			return this;
		}

		public ChainedPeturbatorFactory<T> build() {
			return new ChainedPeturbatorFactory<>(perturbatorFactories, maxPerturbations);
		}

	}
}
