/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.extensions.electric;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.Link;


public class ChargerImpl
    implements Charger
{
    private final Id<Charger> id;
    private final double powerInWatts;
    private final Link link;

    private final ChargingSchedule<? extends ChargeTask> schedule;


    public ChargerImpl(Id<Charger> id, double powerInWatts, Link link)
    {
        this.id = id;
        this.powerInWatts = powerInWatts;
        this.link = link;

        schedule = new ChargingScheduleImpl<>(this);
    }


    @Override
    public Id<Charger> getId()
    {
        return id;
    }


    @Override
    public double getPowerInWatts()
    {
        return powerInWatts;
    }


    @Override
    public Link getLink()
    {
        return link;
    }


    @Override
    public Coord getCoord()
    {
        return link.getCoord();
    }


    @Override
    public ChargingSchedule<? extends ChargeTask> getSchedule()
    {
        return schedule;
    }
}
