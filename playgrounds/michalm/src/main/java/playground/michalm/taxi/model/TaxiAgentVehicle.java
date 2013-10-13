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

import org.matsim.contrib.dvrp.vrpagent.VrpAgentVehicle;

import pl.poznan.put.vrp.dynamic.data.model.Depot;


public class TaxiAgentVehicle
    extends VrpAgentVehicle
{
    public TaxiAgentVehicle(int id, String name, Depot depot, int capacity, int t0, int t1,
            int timeLimit)
    {
        super(id, name, depot, capacity, t0, t1, timeLimit);
    }
}
