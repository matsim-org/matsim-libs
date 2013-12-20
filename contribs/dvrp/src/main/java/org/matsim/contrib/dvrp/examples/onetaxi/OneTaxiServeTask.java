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

package org.matsim.contrib.dvrp.examples.onetaxi;

import pl.poznan.put.vrp.dynamic.data.network.Vertex;
import pl.poznan.put.vrp.dynamic.data.schedule.impl.StayTaskImpl;


public class OneTaxiServeTask
    extends StayTaskImpl
{
    private OneTaxiRequest request;


    public OneTaxiServeTask(int beginTime, int endTime, Vertex vertex, String name,
            OneTaxiRequest request)
    {
        super(beginTime, endTime, vertex, name);
        this.request = request;
    }


    public OneTaxiRequest getRequest()
    {
        return request;
    }


    //pickup or dropoff
    public boolean isPickup()
    {
        return getVertex() == request.getFromVertex();
    }
}
