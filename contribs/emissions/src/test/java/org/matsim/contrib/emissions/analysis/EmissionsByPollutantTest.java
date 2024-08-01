package org.matsim.contrib.emissions.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.matsim.contrib.emissions.Pollutant.CO;
import static org.matsim.contrib.emissions.Pollutant.NO2;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.utils.EmissionUtilsTest;

public class EmissionsByPollutantTest {

	// The EmissionsByPollutant potentially adds up the same emissions coming from cold and warm.  Thus, this cannot be combined into the enum approach
	// without some thinking.  kai, jan'20
	// Quite possibly, should just combine them into an enum "pollutant"?!  There is, anyway, the JM map of those emissions that are actually present in the
	// input file.  kai, jan'20

	@Test
	void initialize() {

        Map<Pollutant, Double> emissions = EmissionUtilsTest.createEmissions();

		Map<Pollutant, Double> map = new LinkedHashMap<>(emissions);

        EmissionsByPollutant linkEmissions = new EmissionsByPollutant( map ) ;

        Map<Pollutant, Double> emissionsByPollutant = linkEmissions.getEmissions();

        emissions.forEach((key, value) -> {
            assertTrue(emissionsByPollutant.containsKey(key));
            assertEquals(value, emissionsByPollutant.get(key), 0.0001);
        });
    }

	@Test
	void addEmission() {

        Map<Pollutant, Double> emissions = EmissionUtilsTest.createEmissions();
        final double valueToAdd = 5;
        final Pollutant pollutant = CO;
        final double expectedValue = emissions.get(pollutant) + valueToAdd;

		Map<Pollutant, Double> map = new LinkedHashMap<>(emissions);

        EmissionsByPollutant emissionsByPollutant = new EmissionsByPollutant(map);

        double result = emissionsByPollutant.addEmission(pollutant, valueToAdd);
        double retrievedResult = emissionsByPollutant.getEmission(pollutant);

        assertEquals(expectedValue, result, 0);
        assertEquals(expectedValue, retrievedResult, 0);
    }

	@Test
	void addEmission_PollutantNotPresentYet() {

        Map<Pollutant, Double> initialPollutants = new HashMap<>();
        initialPollutants.put(CO, Math.random());
        final double valueToAdd = 5;
        final Pollutant pollutantToAdd = NO2;
        EmissionsByPollutant emissionsByPollutant = new EmissionsByPollutant(initialPollutants);

        double result = emissionsByPollutant.addEmission(pollutantToAdd, valueToAdd);
        double retrievedResult = emissionsByPollutant.getEmission(pollutantToAdd);

        assertEquals(valueToAdd, result, 0);
        assertEquals(valueToAdd, retrievedResult, 0);
    }

	@Test
	void addEmissions() {

        Map<Pollutant, Double> emissions = EmissionUtilsTest.createEmissions();

		Map<Pollutant, Double> map = new LinkedHashMap<>(emissions);

        EmissionsByPollutant linkEmissions = new EmissionsByPollutant(map);

		Map<Pollutant, Double> map2 = new LinkedHashMap<>(emissions);

        linkEmissions.addEmissions(map2);

        Map<Pollutant, Double> emissionsByPollutant = linkEmissions.getEmissions();

        emissions.forEach((key, value) -> {
            assertTrue(emissionsByPollutant.containsKey(key));
            assertEquals(value * 2, emissionsByPollutant.get(key), 0.001);
        });
    }
}
