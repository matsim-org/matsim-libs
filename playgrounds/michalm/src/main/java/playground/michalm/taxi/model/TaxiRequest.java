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

import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.model.impl.RequestImpl;
import pl.poznan.put.vrp.dynamic.data.network.Vertex;
import playground.michalm.taxi.schedule.*;


public class TaxiRequest
    extends RequestImpl
{
    private TaxiPickupDriveTask pickupDriveTask;
    private TaxiPickupStayTask pickupStayTask;
    private TaxiDropoffDriveTask dropoffDriveTask;
    private TaxiDropoffStayTask dropoffStayTask;


    public TaxiRequest(int id, Customer customer, Vertex fromVertex, Vertex toVertex, int quantity,
            int t0, int t1, int submissionTime)
    {
        super(id, customer, fromVertex, toVertex, quantity, t0, t1, submissionTime);
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
