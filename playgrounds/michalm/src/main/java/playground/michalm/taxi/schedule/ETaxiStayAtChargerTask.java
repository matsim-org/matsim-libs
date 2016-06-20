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

package playground.michalm.taxi.schedule;

import org.matsim.contrib.dvrp.schedule.StayTaskImpl;

import playground.michalm.ev.data.Charger;


public class ETaxiStayAtChargerTask
    extends StayTaskImpl
    implements ETaxiTask
{
    private final Charger charger;


    public ETaxiStayAtChargerTask(double beginTime, double endTime, Charger charger)
    {
        super(beginTime, endTime, charger.getLink());
        this.charger = charger;
    }


    @Override
    public Charger getCharger()
    {
        return charger;
    }


    @Override
    public TaxiTaskType getTaxiTaskType()
    {
        return TaxiTaskType.STAY;
    }


    @Override
    protected String commonToString()
    {
        return "[" + getTaxiTaskType().name() + "]" + super.commonToString();
    }
}
