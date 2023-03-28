/*
 * Copyright (C) 2023 MOIA GmbH - All Rights Reserved
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package simulatedAnnealing;

import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.core.controler.IterationCounter;
import simulatedAnnealing.acceptor.DefaultAnnealingAcceptor;
import simulatedAnnealing.temperature.TemperatureFunction;

/**
 * @author nkuehnel / MOIA
 */
public class DefaultAnnealingAcceptorTest {

	private final SimulatedAnnealingConfigGroup simAnCfg = new SimulatedAnnealingConfigGroup();

	@Test
	public void testAcceptor() {

		MutableInt iteration = new MutableInt(0);
		IterationCounter iterationCounter = iteration::getValue;

		DefaultAnnealingAcceptor<String> acceptor = new DefaultAnnealingAcceptor<>(simAnCfg);

		SimulatedAnnealing.Solution<String> currentSolution = new SimulatedAnnealing.Solution<>("current", 10);
		SimulatedAnnealing.Solution<String> acceptedSolution = new SimulatedAnnealing.Solution<>("accepted", 15);


		TemperatureFunction.DefaultFunctions temperatureFunction = TemperatureFunction.DefaultFunctions.exponentialMultiplicative;

		boolean accept = acceptor.accept(currentSolution, acceptedSolution, temperature(iteration));
		Assert.assertTrue(accept);

		currentSolution = new SimulatedAnnealing.Solution<>("current", 15);
		Assert.assertTrue(acceptor.accept(currentSolution, acceptedSolution, temperature(iteration)));

		currentSolution = new SimulatedAnnealing.Solution<>("current", 20);
		Assert.assertTrue(acceptor.accept(currentSolution, acceptedSolution, temperature(iteration)));

		iteration.setValue(10);
		Assert.assertTrue(acceptor.accept(currentSolution, acceptedSolution, temperature(iteration)));

		iteration.setValue(1000);
		Assert.assertFalse(acceptor.accept(currentSolution, acceptedSolution, temperature(iteration)));

		currentSolution = new SimulatedAnnealing.Solution<>("current", 10);
		Assert.assertTrue(acceptor.accept(currentSolution, acceptedSolution, temperature(iteration)));


		Assert.assertThrows(RuntimeException.class, () -> acceptor.accept(null, null, temperature(iteration)));
		Assert.assertThrows(RuntimeException.class, () -> acceptor.accept(new SimulatedAnnealing.Solution<>("notnull"), null, temperature(iteration)));
		Assert.assertThrows(RuntimeException.class, () -> acceptor.accept(new SimulatedAnnealing.Solution<>("notnull"), new SimulatedAnnealing.Solution<>("notnull"), temperature(iteration)));
	}

	private double temperature(MutableInt iteration) {
		return TemperatureFunction.DefaultFunctions.exponentialMultiplicative.getTemperature(simAnCfg.alpha, simAnCfg.initialTemperature, simAnCfg.finalTemperature, simAnCfg.nCoolingCycles, iteration.getValue(), Double.NaN, Double.NaN);
	}
}
