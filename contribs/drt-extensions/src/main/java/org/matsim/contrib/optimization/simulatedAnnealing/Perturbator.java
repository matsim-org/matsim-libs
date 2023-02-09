package org.matsim.contrib.optimization.simulatedAnnealing;

public interface Perturbator<T> {

	T perturbate(T current);


}
