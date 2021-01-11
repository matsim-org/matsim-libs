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

package org.matsim.contrib.ev.temperature;/*
 * created by jbischoff, 15.08.2018
 */

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;

public class TemperatureChangeEvent extends Event {
    public static final String EVENT_TYPE = "temperature_changed";
    public static final String ATTRIBUTE_LINK = "link";
    public static final String ATTRIBUTE_TEMP = "newTemperature";

    private final Id<Link> linkId;
    private final double newTemperatureC;

    /**
     * @param time               eventTime
     * @param linkId             linkId of measurement point
     * @param newTemperatureDegC temperature measured
     */
    public TemperatureChangeEvent(double time, Id<Link> linkId, double newTemperatureDegC) {
        super(time);
        this.newTemperatureC = newTemperatureDegC;
        this.linkId = linkId;
    }

    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }

    @Override
    public Map<String, String> getAttributes() {
        Map<String, String> attr = super.getAttributes();
        attr.put(ATTRIBUTE_TEMP, String.valueOf(this.newTemperatureC));
        attr.put(ATTRIBUTE_LINK, this.linkId.toString());
        return attr;
    }

    public double getNewTemperatureC() {
        return newTemperatureC;
    }

    public Id<Link> getLinkId() {
        return linkId;
    }
}
