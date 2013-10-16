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

package playground.michalm.taxi.model;

import pl.poznan.put.vrp.dynamic.data.model.Customer;
import pl.poznan.put.vrp.dynamic.data.model.impl.AbstractRequest;
import pl.poznan.put.vrp.dynamic.data.network.Vertex;
import playground.michalm.taxi.schedule.*;


public class TaxiRequest
    extends AbstractRequest
{
    private final Vertex fromVertex;
    private final Vertex toVertex;

    private TaxiPickupDriveTask pickupDriveTask;
    private TaxiPickupStayTask pickupStayTask;
    private TaxiDropoffDriveTask dropoffDriveTask;
    private TaxiDropoffStayTask dropoffStayTask;


    public TaxiRequest(int id, Customer customer, Vertex fromVertex, Vertex toVertex, int t0,
            int submissionTime)
    {
        super(id, customer, 1, t0, t0, submissionTime);
        this.fromVertex = fromVertex;
        this.toVertex = toVertex;
    }


    public Vertex getFromVertex()
    {
        return fromVertex;
    }


    public Vertex getToVertex()
    {
        return toVertex;
    }


    public TaxiPickupDriveTask getPickupDriveTask()
    {
        return pickupDriveTask;
    }


    public void setPickupDriveTask(TaxiPickupDriveTask pickupDriveTask)
    {
        this.pickupDriveTask = pickupDriveTask;
    }


    public TaxiPickupStayTask getPickupStayTask()
    {
        return pickupStayTask;
    }


    public void setPickupStayTask(TaxiPickupStayTask pickupStayTask)
    {
        this.pickupStayTask = pickupStayTask;
    }


    public TaxiDropoffDriveTask getDropoffDriveTask()
    {
        return dropoffDriveTask;
    }


    public void setDropoffDriveTask(TaxiDropoffDriveTask dropoffDriveTask)
    {
        this.dropoffDriveTask = dropoffDriveTask;
    }


    public TaxiDropoffStayTask getDropoffStayTask()
    {
        return dropoffStayTask;
    }


    public void setDropoffStayTask(TaxiDropoffStayTask dropoffStayTask)
    {
        this.dropoffStayTask = dropoffStayTask;
    }
}
