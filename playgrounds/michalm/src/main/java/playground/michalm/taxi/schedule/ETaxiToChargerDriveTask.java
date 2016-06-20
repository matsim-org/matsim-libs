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

package playground.michalm.taxi.schedule;

import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.DriveTaskImpl;

import playground.michalm.ev.data.Charger;


public class ETaxiToChargerDriveTask
    extends DriveTaskImpl
    implements ETaxiTask
{
    private final Charger charger;


    public ETaxiToChargerDriveTask(VrpPathWithTravelData path, Charger charger)
    {
        super(path);

        if (charger.getLink() != path.getToLink()) {
            throw new IllegalArgumentException();
        }

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
        return TaxiTaskType.EMPTY_DRIVE;
    }


    @Override
    protected String commonToString()
    {
        return "[" + getTaxiTaskType().name() + "]" + super.commonToString();
    }
}
