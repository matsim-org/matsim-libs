package org.matsim.contrib.pseudosimulation.replanning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;
import org.matsim.core.replanning.strategies.SelectRandom;

class DistributedPlanStrategyTranslationAndRegistrationTest {

	@Test
	void builtInRegistriesRemainInitializedAndExtensible() {
		assertTrue(DistributedPlanStrategyTranslationAndRegistration.isStrategySupported("ChangeExpBeta"));
		assertTrue(DistributedPlanStrategyTranslationAndRegistration.isStrategySupported("ReRoute"));
		assertEquals('A', DistributedPlanStrategyTranslationAndRegistration.SupportedMutatorGenes.get("ReRoute"));

		String customName = "CharacterizedCustomSelector";
		try {
			DistributedPlanStrategyTranslationAndRegistration.SupportedSelectors.put(customName, SelectRandom.class);

			assertTrue(DistributedPlanStrategyTranslationAndRegistration.isStrategySupported(customName));
			assertEquals(SelectRandom.class,
					DistributedPlanStrategyTranslationAndRegistration.SupportedSelectors.get(customName));
		} finally {
			DistributedPlanStrategyTranslationAndRegistration.SupportedSelectors.remove(customName);
		}
	}

	@Test
	void substitutesSelectorsAndMutatorsAndInflatesOnlySelectorWeight() {
		Config config = ConfigUtils.createConfig();
		StrategySettings selector = strategy("ChangeExpBeta", 0.25);
		StrategySettings mutator = strategy("ReRoute", 0.5);
		config.replanning().addStrategySettings(selector);
		config.replanning().addStrategySettings(mutator);

		DistributedPlanStrategyTranslationAndRegistration.substituteStrategies(config, true, 4);

		assertEquals("ChangeExpBetaPSIM", selector.getStrategyName());
		assertEquals(1.0, selector.getWeight());
		assertEquals("ReRoutePSIM", mutator.getStrategyName());
		assertEquals(0.5, mutator.getWeight());
	}

	@Test
	void rejectsUnknownStrategiesWithLegacyMessage() {
		Config config = ConfigUtils.createConfig();
		config.replanning().addStrategySettings(strategy("unknown", 1.0));

		RuntimeException error = assertThrows(RuntimeException.class,
				() -> DistributedPlanStrategyTranslationAndRegistration.substituteStrategies(config, false, 1));

		assertEquals("Strategy unknown not known to be compatible with (Distributed) PSim. Exiting.",
				error.getMessage());
	}

	private StrategySettings strategy(String name, double weight) {
		StrategySettings settings = new StrategySettings();
		settings.setStrategyName(name);
		settings.setWeight(weight);
		return settings;
	}
}
