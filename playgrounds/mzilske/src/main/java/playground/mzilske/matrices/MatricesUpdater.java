/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * MatricesUpdater.java
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

package playground.mzilske.matrices;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.matrices.Entry;

class MatricesUpdater implements StartupListener, BeforeMobsimListener, AfterMobsimListener {

    @Inject
    EventsManager eventsManager;

    @Inject
    TimedMatrices timedMatrices;

    private ODMatrixEventHandler odMatrixEventHandler = new ODMatrixEventHandler();

    @Override
    public void notifyStartup(StartupEvent event) {
        eventsManager.addHandler(odMatrixEventHandler);
    }

    @Override
    public void notifyBeforeMobsim(BeforeMobsimEvent event) {
        for (TimedMatrix matrix : timedMatrices.getMatrices()) {
            for (Collection<Entry> entries : matrix.getMatrix().getFromLocations().values()) {
                for (Entry entry : entries) {
                    entry.setValue(0.0);
                }
            }
        }
    }

    @Override
    public void notifyAfterMobsim(AfterMobsimEvent event) {
        System.out.println("Got matrix!");
    }

    class ODMatrixEventHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler {

        Map<Id, PersonDepartureEvent> departures = new HashMap<Id, PersonDepartureEvent>();

        @Override
        public void handleEvent(PersonDepartureEvent event) {
            departures.put(event.getPersonId(), event);
        }

        @Override
        public void handleEvent(PersonArrivalEvent arrival) {
            PersonDepartureEvent departure = departures.remove(arrival.getPersonId());
            for (TimedMatrix matrix : timedMatrices.getMatrices()) {
                if (departure.getTime() >= matrix.getStartTime() &&
                        departure.getTime() < matrix.getEndTime()) {
                    Entry entry = matrix.getMatrix().getEntry(departure.getLinkId().toString(), arrival.getLinkId().toString());
                    if (entry != null) {
                        entry.setValue(entry.getValue() + 1.0);
                    }
                }
            }
        }

        @Override
        public void reset(int iteration) {

        }

    }

}
