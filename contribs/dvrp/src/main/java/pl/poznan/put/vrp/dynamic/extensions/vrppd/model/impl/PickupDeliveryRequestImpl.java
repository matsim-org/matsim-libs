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
import pl.poznan.put.vrp.dynamic.extensions.vrppd.model.PickupDeliveryRequest;
import pl.poznan.put.vrp.dynamic.extensions.vrppd.schedule.*;


public class PickupDeliveryRequestImpl
    extends RequestImpl
    implements PickupDeliveryRequest
{
    private final Vertex fromVertex;
    private final Vertex toVertex;

    private PickupTask pickupTask;
    private DeliveryTask deliveryTask;


    public PickupDeliveryRequestImpl(int id, Customer customer, int quantity, int t0, int t1,
            int submissionTime, Vertex fromVertex, Vertex toVertex)
    {
        super(id, customer, quantity, t0, t1, submissionTime);
        this.fromVertex = fromVertex;
        this.toVertex = toVertex;
    }


    @Override
    public RequestType getRequestType()
    {
        return RequestType.PICKUP_DELIVERY;
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
    public PickupTask getPickupTask()
    {
        return pickupTask;
    }


    public void setPickupTask(PickupTask pickupTask)
    {
        this.pickupTask = pickupTask;
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
