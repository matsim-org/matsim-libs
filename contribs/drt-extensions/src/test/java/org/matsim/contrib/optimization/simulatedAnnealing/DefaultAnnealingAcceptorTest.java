package org.matsim.contrib.optimization.simulatedAnnealing;

import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.contrib.optimization.simulatedAnnealing.temperature.TemperatureFunction;
import org.matsim.core.controler.IterationCounter;

public class DefaultAnnealingAcceptorTest {

	@Test
	public void testAcceptor() {
		SimulatedAnnealingConfigGroup simAnCfg = new SimulatedAnnealingConfigGroup();

		MutableInt iteration = new MutableInt(0);
		IterationCounter iterationCounter = iteration::getValue;

		DefaultAnnealingAcceptor<String> acceptor = new DefaultAnnealingAcceptor<>(iterationCounter, simAnCfg, TemperatureFunction.DefaultFunctions.exponentialMultiplicative);

		SimulatedAnnealing.Solution<String> currentSolution = new SimulatedAnnealing.Solution<>("current", 10);
		SimulatedAnnealing.Solution<String> acceptedSolution = new SimulatedAnnealing.Solution<>("accepted", 15);

		boolean accept = acceptor.accept(currentSolution, acceptedSolution, null);
		Assert.assertTrue(accept);

		currentSolution = new SimulatedAnnealing.Solution<>("current", 15);
		Assert.assertTrue(acceptor.accept(currentSolution, acceptedSolution, null));

		currentSolution = new SimulatedAnnealing.Solution<>("current", 20);
		Assert.assertTrue(acceptor.accept(currentSolution, acceptedSolution, null));

		iteration.setValue(10);
		Assert.assertTrue(acceptor.accept(currentSolution, acceptedSolution, null));

		iteration.setValue(1000);
		Assert.assertFalse(acceptor.accept(currentSolution, acceptedSolution, null));

		currentSolution = new SimulatedAnnealing.Solution<>("current", 10);
		Assert.assertTrue(acceptor.accept(currentSolution, acceptedSolution, null));


		Assert.assertThrows(RuntimeException.class, () -> acceptor.accept(null, null, null));
		Assert.assertThrows(RuntimeException.class, () -> acceptor.accept(new SimulatedAnnealing.Solution<>("notnull"), null, null));
		Assert.assertThrows(RuntimeException.class, () -> acceptor.accept(new SimulatedAnnealing.Solution<>("notnull"), new SimulatedAnnealing.Solution<>("notnull"), null));
	}
}
