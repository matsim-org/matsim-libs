package org.matsim.core.population;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scoring.functions.ModeUtilityParameters;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PersonUtilsTest {

    @Test
    void testModeTasteVariations() {
        // Create a person
        PopulationFactory factory = PopulationUtils.createPopulation(ConfigUtils.createConfig()).getFactory();
        Person person = factory.createPerson(null);

        // Create variations map using nested Map.of
        Map<String, Map<ModeUtilityParameters.Type, Double>> variations = Map.of(
            "car", Map.of(
				ModeUtilityParameters.Type.constant, -0.5,
				ModeUtilityParameters.Type.dailyUtilityConstant, -1.2
            ),
            "pt", Map.of(
				ModeUtilityParameters.Type.dailyUtilityConstant, -0.8,
				ModeUtilityParameters.Type.marginalUtilityOfTraveling_s, -0.1
            )
        );

        // Set the variations
        PersonUtils.setModeTasteVariations(person, variations);

        // Get and verify variations using AssertJ map assertions
        assertThat(PersonUtils.getModeTasteVariations(person))
            .isNotNull()
            .hasSize(2)
            .isEqualTo(variations);

        // Test removing variations
        PersonUtils.setModeTasteVariations(person, null);
        assertThat(PersonUtils.getModeTasteVariations(person)).isNull();
    }

	@Test
	void typicalDurationsRoundTrip() {
		List<PersonUtils.TypicalDuration> typicalDurations = List.of(
			new PersonUtils.TypicalDuration("home_morning", 24944),
			new PersonUtils.TypicalDuration("work", 29550),
			new PersonUtils.TypicalDuration("work", 1800.5),
			new PersonUtils.TypicalDuration("home_evening", 41206));

		String encoded = PersonUtils.formatTypicalDurations(typicalDurations);

		// integral seconds without a fraction, repeated types kept in order
		assertThat(encoded).isEqualTo("home_morning=24944;work=29550;work=1800.5;home_evening=41206");
		assertThat(PersonUtils.parseTypicalDurations(encoded)).isEqualTo(typicalDurations);
	}

	@Test
	void typicalDurationsOnThePerson() {
		Person person = PopulationUtils.getFactory().createPerson(Id.createPersonId("p"));
		assertThat(PersonUtils.getTypicalDurations(person)).isNull();

		List<PersonUtils.TypicalDuration> typicalDurations = List.of(
			new PersonUtils.TypicalDuration("home_morning", 25200), new PersonUtils.TypicalDuration("home_evening", 37800));
		PersonUtils.setTypicalDurations(person, typicalDurations);

		assertThat(person.getAttributes().getAttribute(PersonUtils.TYPICAL_DURATIONS)).isEqualTo("home_morning=25200;home_evening=37800");
		assertThat(PersonUtils.getTypicalDurations(person)).isEqualTo(typicalDurations);

		PersonUtils.setTypicalDurations(person, null);
		assertThat(PersonUtils.getTypicalDurations(person)).isNull();
	}

	@Test
	void typicalDurationTypesMustNotContainTheSeparators() {
		assertThatThrownBy(() -> new PersonUtils.TypicalDuration("a;b", 1)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new PersonUtils.TypicalDuration("a=b", 1)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> PersonUtils.parseTypicalDurations("work")).isInstanceOf(IllegalArgumentException.class);
	}
}
