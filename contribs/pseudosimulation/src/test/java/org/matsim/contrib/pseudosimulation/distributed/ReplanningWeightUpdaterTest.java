package org.matsim.contrib.pseudosimulation.distributed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;

class ReplanningWeightUpdaterTest {

	private final ReplanningWeightUpdater updater = new ReplanningWeightUpdater();

	@Test
	void redistributesMasterWeightsAndAppendsBorrowingStrategy() {
		Config config = configWithLastIteration(20);
		StrategySettings firstSelector = addStrategy(config, "ChangeExpBeta", 0.6, -1);
		StrategySettings secondSelector = addStrategy(config, "BestScore", 0.4, -1);
		StrategySettings firstMutator = addStrategy(config, "ReRoute", 0.3, 4);
		StrategySettings secondMutator = addStrategy(config, "TimeAllocationMutator", 0.7, 8);

		int innovationEndsAt = updater.updateMaster(config, 0.2, 0.1);

		List<StrategySettings> settings = settings(config);
		assertEquals(8, innovationEndsAt);
		assertEquals(5, settings.size());
		assertSame(firstSelector, settings.get(0));
		assertSame(secondSelector, settings.get(1));
		assertSame(firstMutator, settings.get(2));
		assertSame(secondMutator, settings.get(3));
		assertEquals(0.42, firstSelector.getWeight(), 1e-12);
		assertEquals(0.28, secondSelector.getWeight(), 1e-12);
		assertEquals(0.06, firstMutator.getWeight(), 1e-12);
		assertEquals(0.14, secondMutator.getWeight(), 1e-12);
		assertBorrowingStrategy(settings.get(4), 0.1, 8);
	}

	@Test
	void masterFallsBackToLastIterationWhenMutatorsHaveNoPositiveDisableAfter() {
		Config config = configWithLastIteration(17);
		addStrategy(config, "ChangeExpBeta", 1, -1);
		addStrategy(config, "ReRoute", 1, -1);

		int innovationEndsAt = updater.updateMaster(config, 0.25, 0.15);

		assertEquals(17, innovationEndsAt);
		assertBorrowingStrategy(settings(config).get(2), 0.15, 17);
	}

	@Test
	void preservesSequentialMasterRateLimitingWhenRatesReachOne() {
		Config config = configWithLastIteration(10);
		StrategySettings selector = addStrategy(config, "ChangeExpBeta", 1, -1);
		StrategySettings mutator = addStrategy(config, "ReRoute", 1, 5);
		double borrowingRate = 0.9999 * 0.4 / (0.8 + 0.4);
		double mutationRate = 0.9999 * 0.8 / (0.8 + borrowingRate);

		updater.updateMaster(config, 0.8, 0.4);

		assertEquals(1 - mutationRate - borrowingRate, selector.getWeight(), 1e-12);
		assertEquals(mutationRate, mutator.getWeight(), 1e-12);
		assertBorrowingStrategy(settings(config).get(2), borrowingRate, 5);
	}

	@Test
	void redistributesSlaveWeightsInPlaceAndPreservesOrdering() {
		Config config = configWithLastIteration(10);
		StrategySettings selector = addStrategy(config, "ChangeExpBeta", 2, -1);
		StrategySettings mutator = addStrategy(config, "ReRoute", 3, 7);

		updater.updateSlave(config, 0.3);

		List<StrategySettings> settings = settings(config);
		assertEquals(2, settings.size());
		assertSame(selector, settings.get(0));
		assertSame(mutator, settings.get(1));
		assertEquals(0.7, selector.getWeight(), 1e-12);
		assertEquals(0.3, mutator.getWeight(), 1e-12);
	}

	@Test
	void capsSlaveMutationRatesAboveOne() {
		Config config = configWithLastIteration(10);
		StrategySettings selector = addStrategy(config, "ChangeExpBeta", 1, -1);
		StrategySettings mutator = addStrategy(config, "ReRoute", 1, 7);

		updater.updateSlave(config, 2);

		assertEquals(0.0001, selector.getWeight(), 1e-12);
		assertEquals(0.9999, mutator.getWeight(), 1e-12);
	}

	@Test
	void treatsUnknownStrategiesAsMutators() {
		Config config = configWithLastIteration(10);
		StrategySettings selector = addStrategy(config, "ChangeExpBeta", 1, -1);
		StrategySettings unknown = addStrategy(config, "unknown-strategy", 3, 7);
		StrategySettings mutator = addStrategy(config, "ReRoute", 1, 4);

		updater.updateSlave(config, 0.4);

		assertEquals(0.6, selector.getWeight(), 1e-12);
		assertEquals(0.3, unknown.getWeight(), 1e-12);
		assertEquals(0.1, mutator.getWeight(), 1e-12);
	}

	private Config configWithLastIteration(int lastIteration) {
		Config config = ConfigUtils.createConfig();
		config.controller().setLastIteration(lastIteration);
		config.replanning().clearStrategySettings();
		return config;
	}

	private StrategySettings addStrategy(Config config, String name, double weight, int disableAfter) {
		StrategySettings settings = new StrategySettings();
		settings.setStrategyName(name);
		settings.setWeight(weight);
		settings.setDisableAfter(disableAfter);
		config.replanning().addStrategySettings(settings);
		return settings;
	}

	private List<StrategySettings> settings(Config config) {
		return new ArrayList<>(config.replanning().getStrategySettings());
	}

	private void assertBorrowingStrategy(StrategySettings settings, double weight, int disableAfter) {
		assertEquals("ReplacePlanFromSlave", settings.getStrategyName());
		assertEquals(weight, settings.getWeight(), 1e-12);
		assertEquals(disableAfter, settings.getDisableAfter());
	}
}
