package org.matsim.contrib.optimization.simulatedAnnealing;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.contrib.optimization.simulatedAnnealing.temperature.TemperatureFunction;
import org.matsim.core.controler.IterationCounter;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.gbl.MatsimRandom;

import java.util.Random;

public class SimulatedAnnealingTest {


	@Test
	public void testSimulatedAnnealing() {

		LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		Configuration config = ctx.getConfiguration();
		LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
		loggerConfig.setLevel(Level.DEBUG);
		ctx.updateLoggers();

		MutableInt iteration = new MutableInt(0);
		IterationCounter iterationCounter = iteration::getValue;
		TemperatureFunction temperatureFunction = TemperatureFunction.DefaultFunctions.exponentialMultiplicative;
		Acceptor<String> acceptor = acceptor(iterationCounter, temperatureFunction);

		CostCalculator<String> costCalculator = costCalculator();

		Perturbator<String> perturbator = perturbator();

		SimulatedAnnealing<String> simulatedAnnealing = new SimulatedAnnealing<>(costCalculator, acceptor, perturbator, "mistam");

		for (int i = 0; i < 2000; i++) {
			iteration.setValue(i);
			simulatedAnnealing.notifyIterationEnds(new IterationEndsEvent(null, i, false));
		}

		Assert.assertEquals("matsim", simulatedAnnealing.getAcceptedSolution());
	}

	private Perturbator<String> perturbator() {
		return current -> {
			Random r = MatsimRandom.getRandom();
			boolean changeCharacterCount = r.nextBoolean();

			if(current.equals("")) {
				return String.valueOf(randomCharacter(r));
			}
			if(changeCharacterCount) {
				boolean addCharacter = r.nextBoolean();
				char c = randomCharacter(r);
				int idx = r.nextInt(current.length());
				return addCharacter ? current.substring(0, idx) + c + current.substring(idx) : current.substring(0, idx) + current.substring(idx + 1);
			} else {
				char c = randomCharacter(r);
				return current.replace(current.charAt(r.nextInt(current.length())), c);
			}
		};
	}

	private char randomCharacter(Random r) {
		return (char) (r.nextInt(26) + 'a');
	}

	private Acceptor<String> acceptor(IterationCounter iterationCounter, TemperatureFunction temperatureFunction) {
		return new DefaultAnnealingAcceptor<>(iterationCounter, new SimulatedAnnealingConfigGroup(), temperatureFunction);
	}

	private CostCalculator<String> costCalculator() {
		return solution -> {
			LevenshteinDistance defaultInstance = LevenshteinDistance.getDefaultInstance();
			return defaultInstance.apply("matsim", solution);
		};
	}
}
