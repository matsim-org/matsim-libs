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

import org.matsim.contrib.dvrp.data.network.VrpPath;
import org.matsim.contrib.dvrp.data.schedule.impl.DriveTaskImpl;

import playground.michalm.taxi.model.TaxiRequest;


public class TaxiPickupDriveTask
    extends DriveTaskImpl
    implements TaxiTaskWithRequest
{
    private final TaxiRequest request;


    public TaxiPickupDriveTask(VrpPath path, TaxiRequest request)
    {
        super(path);

        if (request.getFromLink() != path.getToLink()) {
            throw new IllegalArgumentException();
        }

        this.request = request;
        request.setPickupDriveTask(this);
    }


    @Override
    public void removeFromRequest()
    {
        request.setPickupDriveTask(null);
    }


    @Override
    public TaxiTaskType getTaxiTaskType()
    {
        return TaxiTaskType.PICKUP_DRIVE;
    }


    public TaxiRequest getRequest()
    {
        return request;
    }


    @Override
    protected String commonToString()
    {
        return "[" + getTaxiTaskType().name() + "]" + super.commonToString();
    }
}
