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

package pl.poznan.put.vrp.dynamic.data.schedule.impl;

import pl.poznan.put.vrp.dynamic.data.model.Request;
import pl.poznan.put.vrp.dynamic.data.network.Vertex;
import pl.poznan.put.vrp.dynamic.data.schedule.ServeTask;


public class ServeTaskImpl
    extends AbstractTask
    implements ServeTask
{
    private final Vertex atVertex;
    private final Request request;


    public ServeTaskImpl(int beginTime, int endTime, Vertex atVertex, Request request)
    {
        super(beginTime, endTime);
        this.atVertex = atVertex;
        this.request = request;
    }


    @Override
    protected void notifyAdded()
    {
        request.notifyScheduled(this);
    }


    @Override
    protected void notifyRemoved()
    {
        request.notifyUnscheduled();
    }


    @Override
    public Request getRequest()
    {
        return request;
    }


    @Override
    public Vertex getAtVertex()
    {
        return atVertex;
    }


    @Override
    public TaskType getType()
    {
        return TaskType.SERVE;
    }


    @Override
    public String toString()
    {
        return "S(R_" + request.getId() + ",@" + atVertex.getId() + ")" + commonToString();
    }
}