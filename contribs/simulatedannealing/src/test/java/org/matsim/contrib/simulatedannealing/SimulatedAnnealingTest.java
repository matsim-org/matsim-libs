/*
 * Copyright (C) 2023 MOIA GmbH - All Rights Reserved
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package org.matsim.contrib.simulatedannealing;

import java.util.Random;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.contrib.simulatedannealing.acceptor.Acceptor;
import org.matsim.contrib.simulatedannealing.acceptor.DefaultAnnealingAcceptor;
import org.matsim.contrib.simulatedannealing.cost.CostCalculator;
import org.matsim.contrib.simulatedannealing.perturbation.PerturbatorFactory;
import org.matsim.contrib.simulatedannealing.temperature.NonMonotonicAdaptiveTemperatureFunction;
import org.matsim.contrib.simulatedannealing.temperature.TemperatureFunction;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.gbl.MatsimRandom;

/**
 * @author nkuehnel / MOIA
 */
public class SimulatedAnnealingTest {


	private final Random r = new Random(42);

	@Test
	void testSimulatedAnnealing() {
		MatsimRandom.reset();

		LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		Configuration config = ctx.getConfiguration();
		LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
		loggerConfig.setLevel(Level.DEBUG);
		ctx.updateLoggers();

		TemperatureFunction temperatureFunction = new NonMonotonicAdaptiveTemperatureFunction(TemperatureFunction.DefaultFunctions.exponentialMultiplicative);
		SimulatedAnnealingConfigGroup simAnCfg = new SimulatedAnnealingConfigGroup();
		simAnCfg.k = 3;
		Acceptor<String> acceptor = acceptor(simAnCfg);

		CostCalculator<String> costCalculator = costCalculator();

		PerturbatorFactory<String> perturbator = perturbatorFactory();

		SimulatedAnnealing<String> simulatedAnnealing = new SimulatedAnnealing<>(costCalculator, acceptor, perturbator, "mistam", temperatureFunction, simAnCfg);

		for (int i = 0; i < 550; i++) {
			simulatedAnnealing.notifyBeforeMobsim(new BeforeMobsimEvent(null, i, false));
			simulatedAnnealing.notifyAfterMobsim(new AfterMobsimEvent(null, i, false));
		}

		Assertions.assertEquals("matsim", simulatedAnnealing.getCurrentState().get().accepted().get());
	}

	private PerturbatorFactory<String> perturbatorFactory() {
		return (iteration, temperature) -> current -> {
			boolean changeCharacterCount = r.nextBoolean();

			if (current.equals("")) {
				return String.valueOf(randomCharacter(r));
			}
			if (changeCharacterCount) {
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

	private Acceptor<String> acceptor(SimulatedAnnealingConfigGroup simAnCfg) {
		return new DefaultAnnealingAcceptor<>(simAnCfg);
	}

	private CostCalculator<String> costCalculator() {
		return solution -> {
			LevenshteinDistance defaultInstance = LevenshteinDistance.getDefaultInstance();
			return defaultInstance.apply("matsim", solution);
		};
	}
}
