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

package playground.michalm.vrp.data.model;

import org.matsim.core.mobsim.framework.MobsimAgent;

import pl.poznan.put.vrp.dynamic.data.model.Customer;
import pl.poznan.put.vrp.dynamic.data.network.Vertex;


public class TaxiCustomer
    implements Customer
{
    private int id;
    private Vertex vertex;
    private MobsimAgent passenger;


    public TaxiCustomer(int id, Vertex vertex, MobsimAgent passanger)
    {
        this.id = id;
        this.vertex = vertex;
        this.passenger = passanger;
    }


    @Override
    public int getId()
    {
        return id;
    }


    @Override
    public String getName()
    {
        return passenger.getId().toString();
    }


    @Override
    public Vertex getVertex()
    {
        return vertex;
    }


    public MobsimAgent getPassenger()
    {
        return passenger;
    }
}
