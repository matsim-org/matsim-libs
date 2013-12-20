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

import org.matsim.contrib.dvrp.passenger.*;
import org.matsim.core.mobsim.framework.MobsimAgent;

import pl.poznan.put.vrp.dynamic.data.model.Customer;
import pl.poznan.put.vrp.dynamic.data.model.impl.RequestImpl;
import pl.poznan.put.vrp.dynamic.data.network.Vertex;


public class OneTaxiRequest
    extends RequestImpl
    implements PassengerRequest
{
    private final Vertex fromVertex;
    private final Vertex toVertex;


    public OneTaxiRequest(int id, Customer customer, Vertex fromVertex, Vertex toVertex, int time)
    {
        //I want a taxi now: t0 == t1 == submissionTime
        super(id, customer, 1, time, time, time);
        this.fromVertex = fromVertex;
        this.toVertex = toVertex;
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
    public MobsimAgent getPassengerAgent()
    {
        return ((PassengerCustomer)getCustomer()).getPassengerAgent();
    }
}
