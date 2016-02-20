/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.jbischoff.taxi.optimizer.rank;

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.contrib.dvrp.data.Vehicle;

import playground.michalm.taxi.data.TaxiRank;


public class TaxiRankHandler
    implements PersonArrivalEventHandler, PersonDepartureEventHandler
{
    private Map<Id, Vehicle> vehicles;
    private Map<Id, TaxiRank> ranks;


    public TaxiRankHandler()
    {
        this.vehicles = new HashMap<Id, Vehicle>();
        this.ranks = new HashMap<Id, TaxiRank>();
    }


    @Override
    public void reset(int iteration)
    {

    }


    public void addVehicle(Vehicle veh)
    {
        this.vehicles.put(veh.getId(), veh);
    }


    public void addRank(TaxiRank rank)
    {
        this.ranks.put(rank.getLink().getId(), rank);
    }


    @Override
    public void handleEvent(PersonDepartureEvent event)
    {
        if (!this.isRankLocation(event.getLinkId()))
            return;
        if (!this.isMonitoredVehicle(event.getPersonId()))
            return;
        this.ranks.get(event.getLinkId()).removeTaxi(vehicles.get(event.getPersonId()));
    }


    @Override
    public void handleEvent(PersonArrivalEvent event)
    {
        if (!this.isRankLocation(event.getLinkId()))
            return;
        if (!this.isMonitoredVehicle(event.getPersonId()))
            return;
        TaxiRank rank = this.ranks.get(event.getLinkId());
        if (rank.hasCapacity()) {
            rank.addTaxi(vehicles.get(event.getPersonId()));
        }
    }


    private boolean isMonitoredVehicle(Id vid)
    {
        return (this.vehicles.containsKey(vid));
    }


    public boolean isRankLocation(Id linkId)
    {
        return (this.ranks.containsKey(linkId));
    }


    public boolean hasCapacityAtRank(Id linkId)
    {
        if (!isRankLocation(linkId))
            throw new IllegalStateException(linkId + "has no taxirank");
        return (this.ranks.get(linkId).hasCapacity());
    }


    public Map<Id, TaxiRank> getRanks()
    {
        return ranks;
    }


  

}
