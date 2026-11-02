package org.matsim.contrib.msp.capability;

import org.junit.jupiter.api.Test;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the defaults of the capability config group and the matching of scenario activity types
 * against the configured basic types.
 */
public class CapabilityConfigGroupTest {

	@Test
	void hasNoActivityTypeToAnalyseByDefault() {
		// a default type would be one nobody asked for, and a config that declares none would quietly
		// be analysed for it instead of saying so - which is how a misspelled parameter set hides
		assertTrue(new CapabilityConfigGroup().getDiscretionaryActivityParams().isEmpty(),
			"the types with a parameter set are exactly the types the analysis reports on");
	}

	@Test
	void rejectsAMisspelledParameterSet() {

		CapabilityConfigGroup config = new CapabilityConfigGroup();

		assertDoesNotThrow(() -> config.createParameterSet(DiscretionaryActivityParams.SET_NAME));

		// 'discretionalActivity' rather than 'discretionaryActivity': MATSim would otherwise read it as
		// a nameless parameter set and the activity type it declares would never be analysed
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
			() -> config.createParameterSet("discretionalActivity"));

		assertTrue(e.getMessage().contains("discretionalActivity"), "the message names what was wrong");
		assertTrue(e.getMessage().contains(DiscretionaryActivityParams.SET_NAME),
			"and what was meant: " + e.getMessage());
	}

	@Test
	void aMisspelledParameterSetFailsWhenTheConfigIsRead() {

		String file = CapabilityConfigGroupTest.class.getResource("misspelled-parameterset.xml").toString();

		assertThrows(Exception.class, () -> ConfigUtils.loadConfig(file, new CapabilityConfigGroup()),
			"the typo has to surface at load time, not as an empty analysis later");
	}

	@Test
	void hasMandatoryActivitiesByDefault() {
		CapabilityConfigGroup config = new CapabilityConfigGroup();
		assertEquals(List.of("home", "work", "educ", "errands", "accomp"),
			List.copyOf(config.getMandatoryActivities()));
	}

	@Test
	void ignoresTypicalDuration() {

		CapabilityConfigGroup config = new CapabilityConfigGroup();

		// MATSim appends the typical duration to the type
		assertTrue(config.isMandatory("work_7200"));
		assertTrue(config.isMandatory("home_28200"));
		assertTrue(config.isMandatory("work"));

		assertFalse(config.isMandatory("leisure_3600"));
		assertFalse(config.isMandatory("shop_daily_1800"));

		assertEquals("work", CapabilityConfigGroup.stripTypicalDuration("work_7200"));
		assertEquals("work", CapabilityConfigGroup.stripTypicalDuration("work"));
		assertEquals("shop_daily", CapabilityConfigGroup.stripTypicalDuration("shop_daily_1800"));
	}

	@Test
	void coversSubTypesOfAMandatoryType() {

		CapabilityConfigGroup config = new CapabilityConfigGroup();

		// the configured basic type stands for all of its sub-types
		assertEquals(Optional.of("educ"), config.mandatoryTypeOf("educ_primary"));
		assertEquals(Optional.of("educ"), config.mandatoryTypeOf("educ_higher_28800"));
		assertEquals(Optional.of("accomp"), config.mandatoryTypeOf("accomp_other_3600"));
		assertEquals(Optional.of("home"), config.mandatoryTypeOf("home_morning"));
		assertEquals(Optional.of("errands"), config.mandatoryTypeOf("errands"));

		// business is deliberately not mandatory, and leisure and shopping are discretionary
		assertEquals(Optional.empty(), config.mandatoryTypeOf("business_3600"));
		assertEquals(Optional.empty(), config.mandatoryTypeOf("leisure"));
		assertEquals(Optional.empty(), config.mandatoryTypeOf("shop_other"));
		assertEquals(Optional.empty(), config.mandatoryTypeOf("visit"));
	}

	@Test
	void doesNotMatchOnAPlainPrefix() {

		CapabilityConfigGroup config = new CapabilityConfigGroup();

		// 'work' must not swallow 'workout', only 'work' and 'work_*'
		assertFalse(config.isMandatory("workout"));
		assertFalse(config.isMandatory("homeoffice"));
	}

	@Test
	void prefersTheLongestConfiguredType() {

		CapabilityConfigGroup config = new CapabilityConfigGroup();
		config.setMandatoryActivities(Set.of("educ", "educ_primary"));

		// independent of the iteration order of the configured types
		assertEquals(Optional.of("educ_primary"), config.mandatoryTypeOf("educ_primary_28800"));
		assertEquals(Optional.of("educ"), config.mandatoryTypeOf("educ_higher"));
	}

	@Test
	void survivesAConfigRoundTrip() {

		Config config = ConfigUtils.createConfig(new CapabilityConfigGroup());
		CapabilityConfigGroup capability = ConfigUtils.addOrGetModule(config, CapabilityConfigGroup.class);

		assertEquals("home,work,educ,errands,accomp", capability.getMandatoryActivitiesAsString());

		capability.setMandatoryActivitiesAsString("home,work");
		assertEquals(List.of("home", "work"), List.copyOf(capability.getMandatoryActivities()));
		assertFalse(capability.isMandatory("educ_primary"), "educ is no longer configured");
	}
}
