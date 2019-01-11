package org.matsim.contrib.emissions.analysis;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.analysis.time.TimeBinMap;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.types.Pollutant;
import org.matsim.contrib.emissions.utils.TestEmissionUtils;
import org.matsim.vehicles.Vehicle;

import java.util.*;
import java.util.stream.Collectors;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class EmissionsOnLinkEventHandlerTest {

    @Test
    public void handleWarmEmissionsEvent() {

        Id<Link> linkId = Id.createLinkId(UUID.randomUUID().toString());
        Id<Vehicle> vehicleId = Id.createVehicleId(UUID.randomUUID().toString());
        double time = 1;
        Map<Pollutant, Double> emissions = TestEmissionUtils.createEmissions();
        Map<String, Double> weaklyTypedEmissions = emissions.entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey().getText(), Map.Entry::getValue));

        WarmEmissionEvent event = new WarmEmissionEvent(time, linkId, vehicleId, weaklyTypedEmissions);

        EmissionsOnLinkEventHandler handler = new EmissionsOnLinkEventHandler(10);

        handler.handleEvent(event);

        TimeBinMap.TimeBin<Map<Id<Link>, LinkEmissions>> timeBin = handler.getTimeBins().getTimeBin(time);

        assertTrue(timeBin.hasValue());
        emissions.forEach((key, value) -> assertEquals(value, timeBin.getValue().get(linkId).getEmission(key), 0.0001));
    }

    @Test
    public void handleColdEmissionEvent() {

        Id<Link> linkId = Id.createLinkId(UUID.randomUUID().toString());
        Id<Vehicle> vehicleId = Id.createVehicleId(UUID.randomUUID().toString());
        double time = 1;
        Map<Pollutant, Double> emissions = TestEmissionUtils.createEmissions();
        Map<String, Double> weaklyTypedEmissions = emissions.entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey().getText(), Map.Entry::getValue));

        ColdEmissionEvent event = new ColdEmissionEvent(time, linkId, vehicleId, weaklyTypedEmissions);

        EmissionsOnLinkEventHandler handler = new EmissionsOnLinkEventHandler(10);

        handler.handleEvent(event);

        TimeBinMap.TimeBin<Map<Id<Link>, LinkEmissions>> timeBin = handler.getTimeBins().getTimeBin(time);

        assertTrue(timeBin.hasValue());
        emissions.forEach((key, value) -> assertEquals(value, timeBin.getValue().get(linkId).getEmission(key), 0.0001));
    }

    @Test
    public void handleMultipleEvents() {

        final Id<Link> linkId = Id.createLinkId(UUID.randomUUID().toString());
        final int numberOfEvents = 1000;
        final double emissionValue = 1.0;
        Collection<WarmEmissionEvent> firstTimestep = createWarmEmissionEvents(linkId, 18, emissionValue, numberOfEvents);
        Collection<WarmEmissionEvent> secondTimestep = createWarmEmissionEvents(linkId, 19, emissionValue, numberOfEvents);
        Collection<WarmEmissionEvent> thirdTimestep = createWarmEmissionEvents(linkId, 20, emissionValue, numberOfEvents);

        EmissionsOnLinkEventHandler handler = new EmissionsOnLinkEventHandler(10);

        firstTimestep.forEach(handler::handleEvent);
        secondTimestep.forEach(handler::handleEvent);
        thirdTimestep.forEach(handler::handleEvent);

        TimeBinMap<Map<Id<Link>, LinkEmissions>> summedEmissions = handler.getTimeBins();

        assertEquals(2, summedEmissions.getTimeBins().size());
        TimeBinMap.TimeBin<Map<Id<Link>, LinkEmissions>> firstBin = summedEmissions.getTimeBin(18);
        assertTrue(firstBin.hasValue());
        assertEquals(numberOfEvents * emissionValue * 2, firstBin.getValue().get(linkId).getEmission(Pollutant.NO2), 0.001);

        TimeBinMap.TimeBin<Map<Id<Link>, LinkEmissions>> secondBin = summedEmissions.getTimeBin(20);
        assertTrue(secondBin.hasValue());
        assertEquals(numberOfEvents * emissionValue, secondBin.getValue().get(linkId).getEmission(Pollutant.HC));
    }

    private Collection<WarmEmissionEvent> createWarmEmissionEvents(Id<Link> linkId, double time, double emissionValue, int numberOfEvents) {

        List<WarmEmissionEvent> result = new ArrayList<>();
        for (int i = 0; i < numberOfEvents; i++) {
            result.add(createWarmEmissionEvent(linkId, time, emissionValue));
        }

        return result;
    }

    private WarmEmissionEvent createWarmEmissionEvent(Id<Link> linkId, double time, double emissionValue) {

        return new WarmEmissionEvent(time, linkId, Id.createVehicleId(UUID.randomUUID().toString()),
                TestEmissionUtils.createEmissionsWithFixedValue(emissionValue).entrySet().stream()
                        .collect(Collectors.toMap(entry -> entry.getKey().getText(), Map.Entry::getValue)));
    }
}
