package org.matsim.contrib.emissions.analysis;

import org.junit.Test;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.contrib.emissions.utils.TestEmissionUtils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class EmissionsByPollutantTest {

    // The EmissionsByPollutant potentially adds up the same emissions coming from cold and warm.  Thus, this cannot be combined into the enum approach
    // without some thinking.  kai, jan'20
    // Quite possibly, should just combine them into an enum "pollutant"?!  There is, anyways, the JM map of those emissions that are actually present in the
    // input file.  kai, jan'20

    @Test
    public void initialize() {

        Map<WarmPollutant, Double> emissions = TestEmissionUtils.createEmissions();

        Map<String,Double> map = new LinkedHashMap<>();
        emissions.forEach( (key,value) -> map.put( key.name(), value ) ) ;

        EmissionsByPollutant linkEmissions = new EmissionsByPollutant( map ) ;

        Map<String, Double> emissionsByPollutant = linkEmissions.getEmissions();

        emissions.forEach((key, value) -> {
            assertTrue(emissionsByPollutant.containsKey(key.name()));
            assertEquals(value, emissionsByPollutant.get(key.name()), 0.0001);
        });
    }

    @Test
    public void addEmission() {

        Map<WarmPollutant, Double> emissions = TestEmissionUtils.createEmissions();
        final double valueToAdd = 5;
        final WarmPollutant pollutant = WarmPollutant.CO;
        final double expectedValue = emissions.get(pollutant) + valueToAdd;

        Map<String,Double> map = new LinkedHashMap<>();
        emissions.forEach( (key,value) -> map.put( key.name(), value ) ) ;

        EmissionsByPollutant emissionsByPollutant = new EmissionsByPollutant(map);

        double result = emissionsByPollutant.addEmission(pollutant.name(), valueToAdd);
        double retrievedResult = emissionsByPollutant.getEmission(pollutant.name());

        assertEquals(expectedValue, result);
        assertEquals(expectedValue, retrievedResult);
    }

    @Test
    public void addEmission_PollutantNotPresentYet() {

        Map<String, Double> initialPollutants = new HashMap<>();
        initialPollutants.put("CO", Math.random());
        final double valueToAdd = 5;
        final String pollutantToAdd = "NO2";
        EmissionsByPollutant emissionsByPollutant = new EmissionsByPollutant(initialPollutants);

        double result = emissionsByPollutant.addEmission(pollutantToAdd, valueToAdd);
        double retrievedResult = emissionsByPollutant.getEmission(pollutantToAdd);

        assertEquals(valueToAdd, result);
        assertEquals(valueToAdd, retrievedResult);
    }

    @Test
    public void addEmissions() {

        Map<WarmPollutant, Double> emissions = TestEmissionUtils.createEmissions();

        Map<String,Double> map = new LinkedHashMap<>();
        emissions.forEach( (key,value) -> map.put( key.name(), value ) ) ;

        EmissionsByPollutant linkEmissions = new EmissionsByPollutant(map);

        Map<String,Double> map2 = new LinkedHashMap<>();
        emissions.forEach( (key,value) -> map2.put( key.name(), value ) ) ;

        linkEmissions.addEmissions(map2);

        Map<String, Double> emissionsByPollutant = linkEmissions.getEmissions();

        emissions.forEach((key, value) -> {
            assertTrue(emissionsByPollutant.containsKey(key.name()));
            assertEquals(value * 2, emissionsByPollutant.get(key.name()), 0.001);
        });
    }
}
