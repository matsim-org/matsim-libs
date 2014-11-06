/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * ODMatrixEventHandler.java
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.matrices.Entry;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

class IncrementMatrixCellEventHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler {

    @Inject
    TimedMatrices timedMatrices;

    private Map<Id, PersonDepartureEvent> departures = new HashMap<>();

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
