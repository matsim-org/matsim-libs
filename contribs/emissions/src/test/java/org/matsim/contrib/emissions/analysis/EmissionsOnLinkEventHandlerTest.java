package org.matsim.contrib.emissions.analysis;

import static org.junit.jupiter.api.Assertions.*;
import static org.matsim.contrib.emissions.Pollutant.HC;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.analysis.time.TimeBinMap;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.utils.EmissionUtilsTest;
import org.matsim.vehicles.Vehicle;

public class EmissionsOnLinkEventHandlerTest {

    private static WarmEmissionEvent createWarmEmissionEvent(Id<Link> linkId, double time, double emissionValue) {

        return new WarmEmissionEvent(time, linkId, Id.createVehicleId(UUID.randomUUID().toString()),
                EmissionUtilsTest.createEmissionsWithFixedValue(emissionValue).entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    private static Collection<WarmEmissionEvent> createWarmEmissionEvents(Id<Link> linkId, double time, double emissionValue, int numberOfEvents) {
        List<WarmEmissionEvent> result = new ArrayList<>();
        for (int i = 0; i < numberOfEvents; i++) {
            result.add(createWarmEmissionEvent(linkId, time, emissionValue));
        }
        return result;
    }

	@Test
	void handleWarmEmissionsEvent() {

        Id<Link> linkId = Id.createLinkId(UUID.randomUUID().toString());
        Id<Vehicle> vehicleId = Id.createVehicleId(UUID.randomUUID().toString());
        double time = 1;

//        Map<String, Double> emissions = TestEmissionUtils.createEmissions();
//        Map<String, Double> weaklyTypedEmissions = emissions.entrySet().stream()
//                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
//        WarmEmissionEvent event = new WarmEmissionEvent(time, linkId, vehicleId, weaklyTypedEmissions);
        // I don't know what that was testing.  kai, jan'20

        Map<Pollutant, Double> emissions = EmissionUtilsTest.createEmissions();
        WarmEmissionEvent event = new WarmEmissionEvent(time, linkId, vehicleId, emissions);

        EmissionsOnLinkEventHandler handler = new EmissionsOnLinkEventHandler(10);

        handler.handleEvent(event);

        TimeBinMap.TimeBin<Map<Id<Link>, EmissionsByPollutant>> timeBin = handler.getTimeBins().getTimeBin(time);
        Map<Id<Link>, Map<Pollutant, Double>> link2pollutantsMap = handler.getLink2pollutants();

        assertTrue(timeBin.hasValue());
        emissions.forEach((key, value) -> assertEquals(value, timeBin.getValue().get(linkId).getEmission(key), 0.0001));
        assertFalse(link2pollutantsMap.isEmpty());
        emissions.forEach((key, value) -> assertEquals(value, link2pollutantsMap.get(linkId).get(key), 0.0001));
    }

	@Test
	void handleColdEmissionEvent() {

        Id<Link> linkId = Id.createLinkId(UUID.randomUUID().toString());
        Id<Vehicle> vehicleId = Id.createVehicleId(UUID.randomUUID().toString());
        double time = 1;
        Map<Pollutant, Double> emissions = EmissionUtilsTest.createUntypedEmissions();

        ColdEmissionEvent event = new ColdEmissionEvent(time, linkId, vehicleId, emissions);

        EmissionsOnLinkEventHandler handler = new EmissionsOnLinkEventHandler(10);

        handler.handleEvent(event);

        TimeBinMap.TimeBin<Map<Id<Link>, EmissionsByPollutant>> timeBin = handler.getTimeBins().getTimeBin(time);
        Map<Id<Link>, Map<Pollutant, Double>> link2pollutantsMap = handler.getLink2pollutants();

        assertTrue(timeBin.hasValue());
        emissions.forEach((key, value) -> assertEquals(value, timeBin.getValue().get(linkId).getEmission(key), 0.0001));
        assertFalse(link2pollutantsMap.isEmpty());
        emissions.forEach((key, value) -> assertEquals(value, link2pollutantsMap.get(linkId).get(key), 0.0001));
    }

	@Test
	void handleMultipleEvents() {

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

        TimeBinMap<Map<Id<Link>, EmissionsByPollutant>> summedEmissions = handler.getTimeBins();
        Map<Id<Link>, Map<Pollutant, Double>> link2pollutantsMap = handler.getLink2pollutants();

        assertEquals(2, summedEmissions.getTimeBins().size());
        TimeBinMap.TimeBin<Map<Id<Link>, EmissionsByPollutant>> firstBin = summedEmissions.getTimeBin(18);
        assertTrue(firstBin.hasValue());
        assertEquals(numberOfEvents * emissionValue * 2, firstBin.getValue().get(linkId).getEmission(Pollutant.NO2), 0.001);

        TimeBinMap.TimeBin<Map<Id<Link>, EmissionsByPollutant>> secondBin = summedEmissions.getTimeBin(20);
        assertTrue(secondBin.hasValue());
        assertEquals(numberOfEvents * emissionValue, secondBin.getValue().get(linkId).getEmission(HC), 0);

        link2pollutantsMap.get(linkId).forEach((pollutant, value) ->
                assertEquals(3 * numberOfEvents * emissionValue, value, 0.0001));
    }

	@Test
	void handleSingleLinkWithSingleEvent() {

        Id<Link> linkId = Id.createLinkId(UUID.randomUUID().toString());
        Id<Vehicle> vehicleId = Id.createVehicleId(UUID.randomUUID().toString());
        double time = 1;
        double emissionValue = 1;
        Map<Pollutant, Double> emissions = EmissionUtilsTest.createEmissionsWithFixedValue(emissionValue);

        WarmEmissionEvent event = new WarmEmissionEvent(time, linkId, vehicleId, emissions);

        EmissionsOnLinkEventHandler handler = new EmissionsOnLinkEventHandler(10);

        handler.handleEvent(event);

        TimeBinMap.TimeBin<Map<Id<Link>, EmissionsByPollutant>> timeBin = handler.getTimeBins().getTimeBin(time);
        Map<Id<Link>, Map<Pollutant, Double>> link2pollutantsMap = handler.getLink2pollutants();

        timeBin.getValue().values().forEach(emissionsByPollutant ->
                emissionsByPollutant.getEmissions().values().forEach(value -> assertEquals(emissionValue, value, 0.0001)));
        link2pollutantsMap.get(linkId).forEach((pollutant, value) -> assertEquals(emissionValue, value, 0.0001));
    }

	@Test
	void handleSingleLinkWithMultipleEvents() {

        Id<Link> linkId = Id.createLinkId(UUID.randomUUID().toString());
        Id<Vehicle> vehicleId = Id.createVehicleId(UUID.randomUUID().toString());
        double time = 1;
        double emissionValue = 1;
        Map<Pollutant, Double> emissions = EmissionUtilsTest.createEmissionsWithFixedValue(emissionValue);

        EmissionsOnLinkEventHandler handler = new EmissionsOnLinkEventHandler(10);

        handler.handleEvent(new WarmEmissionEvent(time, linkId, vehicleId, emissions));
        handler.handleEvent(new WarmEmissionEvent(time, linkId, vehicleId, emissions));
        handler.handleEvent(new WarmEmissionEvent(time, linkId, vehicleId, emissions));

        TimeBinMap.TimeBin<Map<Id<Link>, EmissionsByPollutant>> timeBin = handler.getTimeBins().getTimeBin(time);
        Map<Id<Link>, Map<Pollutant, Double>> link2pollutantsMap = handler.getLink2pollutants();

        timeBin.getValue().values().forEach(emissionsByPollutant ->
                emissionsByPollutant.getEmissions().values().forEach(value -> assertEquals(emissionValue * 3, value, 0.0001)));
        link2pollutantsMap.get(linkId).forEach((pollutant, value) ->
                assertEquals(emissionValue * 3, value, 0.0001));
    }
}
