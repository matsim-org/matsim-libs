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

import org.matsim.api.core.v01.Id;


/**
 * @author michalm
 */
public class VrpDataImpl
    implements VrpData
{
    private final Map<Id<Vehicle>, Vehicle> vehicles = new LinkedHashMap<>();
    private final Map<Id<Request>, Request> requests = new LinkedHashMap<>();

    private final Map<Id<Vehicle>, Vehicle> unmodifiableVehicles = Collections
            .unmodifiableMap(vehicles);
    private final Map<Id<Request>, Request> unmodifiableRequests = Collections
            .unmodifiableMap(requests);


    @Override
    public Map<Id<Vehicle>, Vehicle> getVehicles()
    {
        return unmodifiableVehicles;
    }


    @Override
    public Map<Id<Request>, Request> getRequests()
    {
        return unmodifiableRequests;
    }


    @Override
    public void addVehicle(Vehicle vehicle)
    {
        vehicles.put(vehicle.getId(), vehicle);
    }


    @Override
    public void addRequest(Request request)
    {
        requests.put(request.getId(), request);
    }
}
