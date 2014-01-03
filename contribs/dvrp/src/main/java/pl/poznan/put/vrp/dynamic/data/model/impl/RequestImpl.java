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

package pl.poznan.put.vrp.dynamic.data.model.impl;

import org.matsim.api.core.v01.Id;

import pl.poznan.put.vrp.dynamic.data.model.*;


/**
 * @author michalm
 */
public class RequestImpl
    implements Request
{
    private final Id id;

    private final Customer customer;

    private final int quantity;

    private final int t0;// earliest start time
    private final int t1;// latest start time

    private final int submissionTime;


    public RequestImpl(Id id, Customer customer, int quantity, int t0, int t1, int submissionTime)
    {
        this.id = id;
        this.customer = customer;
        this.quantity = quantity;
        this.t0 = t0;
        this.t1 = t1;
        this.submissionTime = submissionTime;
    }


    @Override
    public Id getId()
    {
        return id;
    }


    @Override
    public Customer getCustomer()
    {
        return customer;
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
    public String toString()
    {
        return "Request_" + id + " [S=(" + t0 + ", ???, " + t1 + "), F=???]";
    }
}
