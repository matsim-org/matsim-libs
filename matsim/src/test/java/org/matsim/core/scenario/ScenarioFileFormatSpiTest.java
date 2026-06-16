package org.matsim.core.scenario;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import java.util.Optional;

/**
 * Tests for the {@link ScenarioFileFormat} SPI mechanism.
 */
public class ScenarioFileFormatSpiTest {

	@Test
	void testDummyProviderIsDiscovered() {
		Optional<ScenarioFileFormat> provider = ScenarioFileFormatRegistry.getProvider("population.dummypop");
		Assertions.assertTrue(provider.isPresent());
		Assertions.assertInstanceOf(DummyScenarioFileFormat.class, provider.get());
	}

	@Test
	void testUnknownExtensionReturnsEmpty() {
		Optional<ScenarioFileFormat> provider = ScenarioFileFormatRegistry.getProvider("population.xml");
		Assertions.assertFalse(provider.isPresent());
	}

	@Test
	void testCompressedExtensionResolvesInner() {
		Optional<ScenarioFileFormat> provider = ScenarioFileFormatRegistry.getProvider("population.dummypop.gz");
		Assertions.assertTrue(provider.isPresent());
		Assertions.assertInstanceOf(DummyScenarioFileFormat.class, provider.get());
	}

	@Test
	void testEffectiveExtensionParsing() {
		Assertions.assertEquals("pb", ScenarioFileFormatRegistry.getEffectiveExtension("population.pb.zst"));
		Assertions.assertEquals("parquet", ScenarioFileFormatRegistry.getEffectiveExtension("network.parquet"));
		Assertions.assertEquals("xml", ScenarioFileFormatRegistry.getEffectiveExtension("plans.xml.gz"));
		Assertions.assertEquals("xml", ScenarioFileFormatRegistry.getEffectiveExtension("plans.xml.bz2"));
		Assertions.assertEquals("gz", ScenarioFileFormatRegistry.getEffectiveExtension("noprefix.gz"));
		Assertions.assertEquals("", ScenarioFileFormatRegistry.getEffectiveExtension("nodotfile"));
	}

	@Test
	void testSpiPopulationLoadingViaScenarioLoader() {
		Config config = ConfigUtils.createConfig();
		config.plans().setInputFile("population.dummypop");

		Scenario scenario = ScenarioUtils.createScenario(config);
		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(scenario);
		loader.loadScenario();

		Assertions.assertTrue(scenario.getPopulation().getPersons().containsKey(
				org.matsim.api.core.v01.Id.createPersonId("dummy_person_from_spi")));
	}
}