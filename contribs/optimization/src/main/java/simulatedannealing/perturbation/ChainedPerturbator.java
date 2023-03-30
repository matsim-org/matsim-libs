/*
 * Copyright (C) 2023 MOIA GmbH - All Rights Reserved
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package simulatedannealing.perturbation;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * Perturbator that allows to apply multiple chained perturbations per iteration.
 * The basic idea is a list of {@link PerturbatorFactory}s from which a (weighted) random
 * combination of actual perturbators is drawn in every iteration.
 *
 * @author nkuehnel / MOIA
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
