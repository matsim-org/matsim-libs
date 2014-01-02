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

import org.matsim.api.core.v01.network.Link;

import pl.poznan.put.vrp.dynamic.data.model.Customer;
import pl.poznan.put.vrp.dynamic.data.model.impl.RequestImpl;
import pl.poznan.put.vrp.dynamic.extensions.vrppd.model.DeliveryRequest;
import pl.poznan.put.vrp.dynamic.extensions.vrppd.schedule.DeliveryTask;


public class DeliveryRequestImpl
    extends RequestImpl
    implements DeliveryRequest
{
    private final Link toLink;
    private DeliveryTask deliveryTask;


    public DeliveryRequestImpl(int id, Customer customer, int quantity, int t0, int t1,
            int submissionTime, Link toLink)
    {
        super(id, customer, quantity, t0, t1, submissionTime);
        this.toLink = toLink;
    }


    @Override
    public RequestType getRequestType()
    {
        return RequestType.DELIVERY;
    }


    @Override
    public Link getToLink()
    {
        return toLink;
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
