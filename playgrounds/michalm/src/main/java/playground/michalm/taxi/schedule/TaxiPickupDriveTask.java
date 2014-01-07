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

import org.matsim.contrib.dvrp.data.network.shortestpath.ShortestPath;

import pl.poznan.put.vrp.dynamic.data.schedule.impl.DriveTaskImpl;
import playground.michalm.taxi.model.TaxiRequest;


public class TaxiPickupDriveTask
    extends DriveTaskImpl
    implements TaxiTaskWithRequest
{
    private final TaxiRequest request;


    public TaxiPickupDriveTask(ShortestPath shortestPath, TaxiRequest request)
    {
        super(shortestPath);

        if (request.getFromLink() != shortestPath.getToLink()) {
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
