/*
 * Copyright (C) 2023 MOIA GmbH - All Rights Reserved
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package simulatedannealing.acceptor;

import org.apache.commons.math3.random.RandomGenerator;
import org.matsim.contrib.common.util.random.RandomUtils;
import org.matsim.core.gbl.Gbl;
import simulatedannealing.SimulatedAnnealing;
import simulatedannealing.SimulatedAnnealingConfigGroup;

/**
 * @author nkuehnel / MOIA
 */
public final class DefaultAnnealingAcceptor<T> implements Acceptor<T> {

	private final RandomGenerator random;

	private final SimulatedAnnealingConfigGroup simAnCfg;



	public DefaultAnnealingAcceptor(SimulatedAnnealingConfigGroup simAnCfg) {
		this.simAnCfg = simAnCfg;
		this.random = RandomUtils.getLocalGenerator();
	}

	@Override
	public boolean accept(SimulatedAnnealing.Solution<T> currentSolution,
                          SimulatedAnnealing.Solution<T> acceptedSolution,
                          double temperature) {

		Gbl.assertNotNull(currentSolution);
		Gbl.assertNotNull(acceptedSolution);

		Gbl.assertIf(currentSolution.getCost().isPresent());
		Gbl.assertIf(acceptedSolution.getCost().isPresent());

		// If the new solution is better, accept it
		double currentCost = currentSolution.getCost().getAsDouble();
		double acceptedCost = acceptedSolution.getCost().getAsDouble();

		if (currentCost < acceptedCost) {
			return true;
		}

		// If the new solution is worse, calculate an acceptance probability
		double acceptanceProbability = Math.exp(-(simAnCfg.k * (currentCost - acceptedCost) / temperature));

		return random.nextDouble() < acceptanceProbability;
	}
}
