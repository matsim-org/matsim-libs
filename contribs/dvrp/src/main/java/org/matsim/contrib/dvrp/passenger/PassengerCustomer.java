/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.passenger;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.data.model.Customer;
import org.matsim.core.mobsim.framework.MobsimAgent;


public class PassengerCustomer
    implements Customer
{
    private final MobsimAgent passenger;


    public PassengerCustomer(MobsimAgent passenger)
    {
        this.passenger = passenger;
    }


    @Override
    public Id getId()
    {
        return passenger.getId();
    }


    @Override
    public String getName()
    {
        return passenger.getId().toString();
    }


    /*package*/ MobsimAgent getPassengerAgent()
    {
        return passenger;
    }
}
