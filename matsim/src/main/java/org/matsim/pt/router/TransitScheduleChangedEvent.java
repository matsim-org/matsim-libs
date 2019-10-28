
/* *********************************************************************** *
 * project: org.matsim.*
 * TransitScheduleChangedEvent.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.pt.router;

import org.matsim.api.core.v01.events.Event;

public class TransitScheduleChangedEvent extends Event {
    public TransitScheduleChangedEvent(double time) {
        super(time);
    }

    @Override
    public String getEventType() {
        return "transit_schedule_changed";
    }
}
