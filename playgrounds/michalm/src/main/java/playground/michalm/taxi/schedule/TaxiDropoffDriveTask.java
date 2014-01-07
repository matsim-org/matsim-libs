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


public class TaxiDropoffDriveTask
    extends DriveTaskImpl
    implements TaxiTaskWithRequest
{
    private final TaxiRequest request;


    public TaxiDropoffDriveTask(ShortestPath shortestPath, TaxiRequest request)
    {
        super(shortestPath);

        if (request.getFromLink() != shortestPath.getFromLink()
                && request.getToLink() != shortestPath.getToLink()) {
            throw new IllegalArgumentException();
        }

        this.request = request;
        request.setDropoffDriveTask(this);
    }


    @Override
    public void removeFromRequest()
    {
        request.setDropoffDriveTask(null);
    }


    @Override
    public TaxiTaskType getTaxiTaskType()
    {
        return TaxiTaskType.DROPOFF_DRIVE;
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
