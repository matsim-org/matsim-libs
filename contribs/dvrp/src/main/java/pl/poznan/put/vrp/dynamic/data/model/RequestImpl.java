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

package pl.poznan.put.vrp.dynamic.data.model;

import pl.poznan.put.vrp.dynamic.data.network.Vertex;


/**
 * @author michalm
 */
public class RequestImpl
    implements Request
{
    private final int id;

    private final Customer customer;

    private final Vertex fromVertex;
    private final Vertex toVertex;

    private final int quantity;

    private final int t0;// earliest start time
    private final int t1;// latest start time

    private final int submissionTime;

    private RequestStatus status = RequestStatus.INACTIVE;// based on: serveTask.getStatus();


    public RequestImpl(int id, Customer customer, Vertex fromVertex, Vertex toVertex, int quantity,
            int t0, int t1, int submissionTime)
    {
        this.id = id;
        this.customer = customer;
        this.fromVertex = fromVertex;
        this.toVertex = toVertex;
        this.quantity = quantity;
        this.t0 = t0;
        this.t1 = t1;
        this.submissionTime = submissionTime;
    }


    @Override
    public int getId()
    {
        return id;
    }


    @Override
    public Customer getCustomer()
    {
        return customer;
    }


    @Override
    public Vertex getFromVertex()
    {
        return fromVertex;
    }


    @Override
    public Vertex getToVertex()
    {
        return toVertex;
    }


    @Override
    public int getQuantity()
    {
        return quantity;
    }


    @Override
    public int getT0()
    {
        return t0;
    }


    @Override
    public int getT1()
    {
        return t1;
    }


    @Override
    public int getSubmissionTime()
    {
        return submissionTime;
    }


    @Override
    public RequestStatus getStatus()
    {
        return status;
    }


    public void setStatus(RequestStatus status)
    {
        this.status = status;
    }


    @Override
    public String toString()
    {
        return "Request_" + id + " [S=(" + t0 + ", ???, " + t1 + "), F=???]";
    }
}
