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

package org.matsim.contrib.dvrp.data;

import java.util.*;


/**
 * @author michalm
 */
public class VrpDataImpl
    implements VrpData
{
    private final List<Vehicle> vehicles = new ArrayList<>();
    private final List<Request> requests = new ArrayList<>();

    private final List<Vehicle> unmodifiableVehicles = Collections.unmodifiableList(vehicles);
    private final List<Request> unmodifiableRequests = Collections.unmodifiableList(requests);


    @Override
    public List<Vehicle> getVehicles()
    {
        return unmodifiableVehicles;
    }


    @Override
    public List<Request> getRequests()
    {
        return unmodifiableRequests;
    }


    @Override
    public void addVehicle(Vehicle vehicle)
    {
        vehicles.add(vehicle);
    }


    @Override
    public void addRequest(Request request)
    {
        requests.add(request);
    }
}
