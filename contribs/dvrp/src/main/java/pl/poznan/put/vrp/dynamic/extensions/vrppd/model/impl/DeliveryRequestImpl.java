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

package pl.poznan.put.vrp.dynamic.extensions.vrppd.model.impl;

import pl.poznan.put.vrp.dynamic.data.model.Customer;
import pl.poznan.put.vrp.dynamic.data.model.impl.RequestImpl;
import pl.poznan.put.vrp.dynamic.data.network.Vertex;
import pl.poznan.put.vrp.dynamic.extensions.vrppd.model.DeliveryRequest;
import pl.poznan.put.vrp.dynamic.extensions.vrppd.schedule.DeliveryTask;


public class DeliveryRequestImpl
    extends RequestImpl
    implements DeliveryRequest
{
    private final Vertex toVertex;
    private DeliveryTask deliveryTask;


    public DeliveryRequestImpl(int id, Customer customer, int quantity, int t0, int t1,
            int submissionTime, Vertex toVertex)
    {
        super(id, customer, quantity, t0, t1, submissionTime);
        this.toVertex = toVertex;
    }


    @Override
    public RequestType getRequestType()
    {
        return RequestType.DELIVERY;
    }


    @Override
    public Vertex getToVertex()
    {
        return toVertex;
    }


    @Override
    public DeliveryTask getDeliveryTask()
    {
        return deliveryTask;
    }


    public void setDeliveryTask(DeliveryTask deliveryTask)
    {
        this.deliveryTask = deliveryTask;
    }
}
