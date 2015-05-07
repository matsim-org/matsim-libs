/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * EventsConverter.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
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

package playground.tobiqui.master;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class EventsConverter {

    public static void main(String[] args) {

        List<Event> events = new ArrayList<>();
        events.add(new PersonDepartureEvent(0.0, Id.createPersonId("wurst"), Id.createLinkId("blubb"), "car"));
        Collections.sort(events, new Comparator<Event>() {
            @Override
            public int compare(Event o1, Event o2) {
                return Double.compare(o1.getTime(), o2.getTime());
            }
        });

        EventsManager eventsManager = EventsUtils.createEventsManager();
        EventWriterXML eventWriterXML = new EventWriterXML("../matsim/output/siouxfalls-2014/events.xml");
        eventsManager.addHandler(eventWriterXML);
        for (Event event : events) {
            eventsManager.processEvent(event);
        }
        eventsManager.processEvent(new PersonDepartureEvent(0.0, Id.createPersonId("wurst"), Id.createLinkId("blubb"), "car"));
        eventWriterXML.closeFile();
    }

}
