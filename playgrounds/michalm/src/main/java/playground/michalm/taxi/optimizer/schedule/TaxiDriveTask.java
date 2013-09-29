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

package playground.michalm.taxi.optimizer.schedule;

import pl.poznan.put.vrp.dynamic.data.model.Request;
import pl.poznan.put.vrp.dynamic.data.network.*;
import pl.poznan.put.vrp.dynamic.data.schedule.impl.DriveTaskImpl;


public class TaxiDriveTask
    extends DriveTaskImpl
{
    public enum TaxiDriveType
    {
        PICKUP, DELIVERY, CRUISE;
    }


    private final Request request;// may be null for non-PICKUP/DELIVERY tasks
    private final TaxiDriveType driveType;


    public TaxiDriveTask(int beginTime, int endTime, Arc arc, Request request)
    {
        super(beginTime, endTime, arc);
        this.request = request;

        Vertex reqFromVertex = request.getFromVertex();

        if (reqFromVertex == arc.getToVertex()) {
            driveType = TaxiDriveType.PICKUP;
        }
        else if (reqFromVertex == arc.getFromVertex()) {
            driveType = TaxiDriveType.DELIVERY;
        }
        else {
            throw new IllegalArgumentException();
        }
    }


    public TaxiDriveTask(int beginTime, int endTime, Arc arc)
    {
        super(beginTime, endTime, arc);

        request = null;
        driveType = TaxiDriveType.CRUISE;
    }


    public TaxiDriveType getDriveType()
    {
        return driveType;
    }


    public Request getRequest()
    {
        return request;// may be null for non-PICKUP/DELIVERY tasks
    }


    @Override
    protected String commonToString()
    {
        return "[" + driveType.name() + "]" + super.commonToString();
    }
}
