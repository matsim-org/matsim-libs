package org.matsim.contrib.emissions.analysis;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.types.Pollutant;
import org.matsim.contrib.emissions.utils.TestEmissionUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class LinkEmissionsTest {

    @Test
    public void initialize() {

        Id<Link> linkId = Id.createLinkId(UUID.randomUUID().toString());
        Map<Pollutant, Double> emissions = TestEmissionUtils.createEmissions();

        LinkEmissions linkEmissions = new LinkEmissions(linkId, emissions);

        Map<Pollutant, Double> emissionsByPollutant = linkEmissions.getEmissions();

        emissions.forEach((key, value) -> {
            assertTrue(emissionsByPollutant.containsKey(key));
            assertEquals(value, emissionsByPollutant.get(key), 0.0001);
        });
    }

    @Test
    public void addEmission() {

        Id<Link> linkId = Id.createLinkId(UUID.randomUUID().toString());
        Map<Pollutant, Double> emissions = TestEmissionUtils.createEmissions();
        final double valueToAdd = 5;
        final Pollutant pollutant = Pollutant.CO;
        final double expectedValue = emissions.get(pollutant) + valueToAdd;
        LinkEmissions linkEmissions = new LinkEmissions(linkId, emissions);

        double result = linkEmissions.addEmission(pollutant, valueToAdd);
        double retrievedResult = linkEmissions.getEmission(pollutant);

        assertEquals(expectedValue, result);
        assertEquals(expectedValue, retrievedResult);
    }

    @Test
    public void addEmission_PollutantNotPresentYet() {

        Id<Link> linkId = Id.createLinkId(UUID.randomUUID().toString());
        Map<Pollutant, Double> initialPollutants = new HashMap<>();
        initialPollutants.put(Pollutant.CO, Math.random());
        final double valueToAdd = 5;
        final Pollutant pollutantToAdd = Pollutant.NO2;
        LinkEmissions linkEmissions = new LinkEmissions(linkId, initialPollutants);

        double result = linkEmissions.addEmission(pollutantToAdd, valueToAdd);
        double retrievedResult = linkEmissions.getEmission(pollutantToAdd);

        assertEquals(valueToAdd, result);
        assertEquals(valueToAdd, retrievedResult);
    }

    @Test
    public void addEmissions() {

        Id<Link> linkId = Id.createLinkId(UUID.randomUUID().toString());
        Map<Pollutant, Double> emissions = TestEmissionUtils.createEmissions();
        LinkEmissions linkEmissions = new LinkEmissions(linkId, new HashMap<>(emissions));

        linkEmissions.addEmissions(emissions);

        Map<Pollutant, Double> emissionsByPollutant = linkEmissions.getEmissions();

        emissions.forEach((key, value) -> {
            assertTrue(emissionsByPollutant.containsKey(key));
            assertEquals(value * 2, emissionsByPollutant.get(key), 0.001);
        });
    }
}
