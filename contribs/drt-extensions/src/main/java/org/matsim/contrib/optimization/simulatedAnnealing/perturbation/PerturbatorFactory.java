package org.matsim.contrib.optimization.simulatedAnnealing.perturbation;

public interface PerturbatorFactory<T> {

	Perturbator<T> createPerturbator();

}
