package org.matsim.contrib.optimization.simulatedAnnealing.perturbation;

import java.util.ArrayList;
import java.util.List;

/**
 * @author nkuehnel
 */
public class ChainedPerturbator<T> implements Perturbator<T> {

	private final List<Perturbator<T>> perturbators;

	private ChainedPerturbator(List<Perturbator<T>> perturbators) {
		this.perturbators = perturbators;
	}

	@Override
	public T perturbate(T current) {
		for (Perturbator<T> perturbator : perturbators) {
			current = perturbator.perturbate(current);
		}
		return current;
	}


	public final static class Builder<T> {

		private final List<Perturbator<T>> perturbators = new ArrayList<>();

		public Builder<T> add(Perturbator<T> perturbator) {
			perturbators.add(perturbator);
			return this;
		}

		public ChainedPerturbator<T> build() {
			return new ChainedPerturbator<>(perturbators);
		}
	}
}
