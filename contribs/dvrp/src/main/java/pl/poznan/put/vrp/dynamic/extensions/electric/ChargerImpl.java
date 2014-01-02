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

package pl.poznan.put.vrp.dynamic.extensions.electric;

import org.matsim.api.core.v01.network.Link;


public class ChargerImpl
    implements Charger
{
    private final int id;
    private final String name;
    private final double powerInWatts;
    private final Link link;

    private final ChargingSchedule<? extends ChargeTask> schedule;


    public ChargerImpl(int id, String name, double powerInWatts, Link link)
    {
        this.id = id;
        this.name = name;
        this.powerInWatts = powerInWatts;
        this.link = link;

        schedule = new ChargingScheduleImpl<ChargeTask>(this);
    }


    @Override
    public int getId()
    {
        return id;
    }


    @Override
    public String getName()
    {
        return name;
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
    public ChargingSchedule<? extends ChargeTask> getSchedule()
    {
        return schedule;
    }
}
