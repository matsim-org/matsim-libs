/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * CreateCountStationEventsFile.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package playground.mzilske.sensors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;

import java.util.HashMap;
import java.util.Map;

public class CreateCountStationEventsFile {

    public static void main(String[] args) {
        Scenario scenario = BerlinRunCongested.getScenario();
        final Counts<Link> counts = new Counts();
        MatsimCountsReader counts_parser = new MatsimCountsReader(counts);
        counts_parser.readFile(scenario.getConfig().counts().getCountsFileName());
        for (Id countId : counts.getCounts().keySet()) {
            System.out.println(countId);
        }

        final Map<Id, Integer> sensorCounts = new HashMap<Id, Integer>();


        final EventsManager filteredEvents = EventsUtils.createEventsManager(); {
            filteredEvents.addHandler(new EventWriterXML("/Users/michaelzilske/runs-svn/synthetic-cdr/bluetooth/sensor-events.xml"));
            filteredEvents.addHandler(new LinkLeaveEventHandler() {

                @Override
                public void handleEvent(LinkLeaveEvent event) {
                    if (!sensorCounts.containsKey(event.getLinkId())) sensorCounts.put(event.getLinkId(), 0);
                    sensorCounts.put(event.getLinkId(), sensorCounts.get(event.getLinkId()) + 1);
                }

                @Override
                public void reset(int iteration) {

                }
            });
        }

        EventsManager events = EventsUtils.createEventsManager();
        events.addHandler(new LinkLeaveEventHandler() {
            @Override
            public void handleEvent(LinkLeaveEvent event) {
                if (counts.getCounts().keySet().contains(event.getLinkId())) {
                    filteredEvents.processEvent(event);
                }
            }

            @Override
            public void reset(int iteration) {}
        });

        new MatsimEventsReader(events).readFile(scenario.getConfig().controler().getOutputDirectory()+"/ITERS/it.120/120.events.xml.gz");

        int i=0;
        for (Map.Entry<Id, Integer> entry : sensorCounts.entrySet()) {
            System.out.println(i + "\t\t\t" + entry.getKey() + "\t\t\t" + entry.getValue());
            i++;
        }


    }

}
