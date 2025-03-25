package org.matsim.core.population;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scoring.functions.ModeUtilityParameters;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

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
}
