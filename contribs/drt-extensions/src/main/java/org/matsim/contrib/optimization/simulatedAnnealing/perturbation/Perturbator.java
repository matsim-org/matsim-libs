package org.matsim.contrib.optimization.simulatedAnnealing.perturbation;

public interface Perturbator<T> {

	T perturbate(T current);


}
