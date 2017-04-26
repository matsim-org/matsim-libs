/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.agarwalamit.emissions;

import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.events.EmissionEventsReader;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import playground.agarwalamit.analysis.emission.caused.CausedEmissionCostHandler;
import playground.kai.usecases.combinedEventsReader.CombinedMatsimEventsReader;
import playground.vsp.airPollution.flatEmissions.EmissionCostModule;

/**
 * Created by amit on 29/12/2016.
 */

public class CombinedEventsReaderTest {

    @Rule
    public MatsimTestUtils helper = new MatsimTestUtils();

    private final String carPersonIdString = "567417.1#12424";
    private final String bicyclePersonIdString = "567417.1#12425";

    private final String bicycleVehicleIdString = "567417.1#12425_bicycle";

    @Test@Ignore//TODO yet to complete and fix this.
    public void readBothEventsFile() {
        String eventsFile = helper.getClassInputDirectory() + "0.events.xml.gz";
        String emissionEventsFile = helper.getClassInputDirectory() + "0.emission.events.xml.gz";

        String combinedEventsFile = helper.getClassInputDirectory() + "0.combined.events.xml.gz";

        // first merge the events file:
        EventsManager events = EventsUtils.createEventsManager();
        EventWriterXML eventsWriter = new EventWriterXML(combinedEventsFile);
        events.addHandler(eventsWriter);
        new MatsimEventsReader(events).readFile(eventsFile);
        new EmissionEventsReader(events).readFile(emissionEventsFile);
        eventsWriter.closeFile();

        // now read the combinedEvents file
        Map<Id<Vehicle>, Id<Person>> vehicle2driver = new HashMap<>();
        events = EventsUtils.createEventsManager();
        events.addHandler(new VehicleEntersTrafficEventHandler() {
            @Override
            public void handleEvent(VehicleEntersTrafficEvent event) {
                vehicle2driver.put(event.getVehicleId(), event.getPersonId());
            }

            @Override
            public void reset(int iteration) {

            }
        });

        EmissionsConfigGroup emissionsConfigGroup = new EmissionsConfigGroup();
        emissionsConfigGroup.setEmissionCostMultiplicationFactor(1.0);
        emissionsConfigGroup.setConsideringCO2Costs(true);

        CausedEmissionCostHandler emissionHandler = new CausedEmissionCostHandler(new EmissionCostModule(emissionsConfigGroup));
        CombinedMatsimEventsReader reader = new CombinedMatsimEventsReader(events);
        events.addHandler(emissionHandler);
        reader.readFile(combinedEventsFile);

        Map<Id<Person>, Double> personId2EmissionCosts = emissionHandler.getPersonId2TotalEmissionCosts();

        System.out.println(personId2EmissionCosts.get(Id.createPersonId(carPersonIdString)));

        Assert.assertFalse("For car, total emissions costs should be non-zero", personId2EmissionCosts.get(Id.createPersonId(carPersonIdString))==0.);
        Assert.assertFalse("For bicycle, total emissions costs should be zero.", personId2EmissionCosts.get(Id.createPersonId(
                bicycleVehicleIdString))!= 0.);

        Assert.assertFalse("vehicle 2 driver map should not be empty", vehicle2driver.isEmpty());

        Assert.assertEquals("car person is not found.",
                Id.createPersonId(carPersonIdString),
                vehicle2driver.get(Id.createVehicleId(carPersonIdString)));
        Assert.assertEquals("bicycle person is not found.",
                Id.createPersonId(bicyclePersonIdString),
                vehicle2driver.get(Id.createVehicleId(bicycleVehicleIdString)));

    }

    @Test
    public void readEventsFile() {
        String eventsFile = helper.getClassInputDirectory() + "0.events.xml.gz";

        Map<Id<Vehicle>, Id<Person>> vehicle2driver = new HashMap<>();

        EventsManager events = EventsUtils.createEventsManager();
        events.addHandler(new VehicleEntersTrafficEventHandler() {
            @Override
            public void handleEvent(VehicleEntersTrafficEvent event) {
                vehicle2driver.put(event.getVehicleId(), event.getPersonId());
            }

            @Override
            public void reset(int iteration) {

            }
        });

        MatsimEventsReader reader = new MatsimEventsReader(events);
        reader.readFile(eventsFile);

        Assert.assertFalse("vehicle 2 driver map should not be empty", vehicle2driver.isEmpty());

        Assert.assertEquals("car person is not found.",
                Id.createPersonId(carPersonIdString),
                vehicle2driver.get(Id.createVehicleId(carPersonIdString)));
        Assert.assertEquals("bicycle person is not found.",
                Id.createPersonId(bicyclePersonIdString),
                vehicle2driver.get(Id.createVehicleId(bicycleVehicleIdString)));
    }

    @Test
    public void readEmissionEventsFile() {
        String emissionEventsFile = helper.getClassInputDirectory() + "0.emission.events.xml.gz";

        EmissionsConfigGroup emissionsConfigGroup = new EmissionsConfigGroup();
        emissionsConfigGroup.setEmissionCostMultiplicationFactor(1.0);
        emissionsConfigGroup.setConsideringCO2Costs(true);

        CausedEmissionCostHandler handler = new CausedEmissionCostHandler(new EmissionCostModule(emissionsConfigGroup));

        EventsManager events = EventsUtils.createEventsManager();
        events.addHandler(handler);
        EmissionEventsReader reader = new EmissionEventsReader(events);
        reader.readFile(emissionEventsFile);

        Map<Id<Person>, Double> personId2EmissionCosts = handler.getPersonId2TotalEmissionCosts();

        System.out.println(personId2EmissionCosts.get(Id.createPersonId(carPersonIdString)));

        Assert.assertFalse("For car, total emissions costs should be non-zero", personId2EmissionCosts.get(Id.createPersonId(carPersonIdString))==0.);
        Assert.assertFalse("For bicycle, total emissions costs should be zero.", personId2EmissionCosts.get(Id.createPersonId(
                bicycleVehicleIdString))!= 0.);
        //TODO : emission events provide vehicle id rather than person id, thus, the map keys should be vehicle ids.
    }
}