package org.matsim.contrib.emissions.analysis;

import org.junit.Test;
import org.matsim.contrib.emissions.types.Pollutant;
import org.matsim.contrib.emissions.utils.TestEmissionUtils;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class EmissionsByPollutantTest {

    @Test
    public void initialize() {

        Map<Pollutant, Double> emissions = TestEmissionUtils.createEmissions();

        EmissionsByPollutant linkEmissions = new EmissionsByPollutant(emissions);

        Map<Pollutant, Double> emissionsByPollutant = linkEmissions.getEmissions();

        emissions.forEach((key, value) -> {
            assertTrue(emissionsByPollutant.containsKey(key));
            assertEquals(value, emissionsByPollutant.get(key), 0.0001);
        });
    }

    @Test
    public void addEmission() {

        Map<Pollutant, Double> emissions = TestEmissionUtils.createEmissions();
        final double valueToAdd = 5;
        final Pollutant pollutant = Pollutant.CO;
        final double expectedValue = emissions.get(pollutant) + valueToAdd;
        EmissionsByPollutant emissionsByPollutant = new EmissionsByPollutant(emissions);

        double result = emissionsByPollutant.addEmission(pollutant, valueToAdd);
        double retrievedResult = emissionsByPollutant.getEmission(pollutant);

        assertEquals(expectedValue, result);
        assertEquals(expectedValue, retrievedResult);
    }

    @Test
    public void addEmission_PollutantNotPresentYet() {

        Map<Pollutant, Double> initialPollutants = new HashMap<>();
        initialPollutants.put(Pollutant.CO, Math.random());
        final double valueToAdd = 5;
        final Pollutant pollutantToAdd = Pollutant.NO2;
        EmissionsByPollutant emissionsByPollutant = new EmissionsByPollutant(initialPollutants);

        double result = emissionsByPollutant.addEmission(pollutantToAdd, valueToAdd);
        double retrievedResult = emissionsByPollutant.getEmission(pollutantToAdd);

        assertEquals(valueToAdd, result);
        assertEquals(valueToAdd, retrievedResult);
    }

    @Test
    public void addEmissions() {

        Map<Pollutant, Double> emissions = TestEmissionUtils.createEmissions();
        EmissionsByPollutant linkEmissions = new EmissionsByPollutant(new HashMap<>(emissions));

        linkEmissions.addEmissions(emissions);

        Map<Pollutant, Double> emissionsByPollutant = linkEmissions.getEmissions();

        emissions.forEach((key, value) -> {
            assertTrue(emissionsByPollutant.containsKey(key));
            assertEquals(value * 2, emissionsByPollutant.get(key), 0.001);
        });
    }
}
