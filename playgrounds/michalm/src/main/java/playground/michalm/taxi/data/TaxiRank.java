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

package playground.michalm.taxi.data;

import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Vehicle;


public class TaxiRank
    implements BasicLocation<TaxiRank>
{
    private final Id<TaxiRank> id;
    private final String name;
    private final Link link;
    private final static int STANDARDCAPACITY = 5;
    private int capacity;

    private Queue<Vehicle> taxisInRank;


    public TaxiRank(Id<TaxiRank> id, String name, Link link)
    {
        this.id = id;
        this.name = name;
        this.link = link;
        this.capacity = STANDARDCAPACITY;
        this.taxisInRank = new LinkedList<>();
    }


    public TaxiRank(Id<TaxiRank> id, String name, Link link, int capacity)
    {
        this.id = id;
        this.name = name;
        this.link = link;
        this.capacity = capacity;
        this.taxisInRank = new LinkedList<>();

    }


    @Override
    public Id<TaxiRank> getId()
    {
        return id;
    }


    @Override
    public Coord getCoord()
    {
        return link.getCoord();
    }


    public String getName()
    {
        return name;
    }


    public Link getLink()
    {
        return link;
    }


    public boolean addTaxi(Vehicle veh)
    {
        if (taxisInRank.size() < this.capacity) {
            taxisInRank.add(veh);
            return true;
        }
        else
            return false;
    }


    public void removeTaxi(Vehicle veh)
    {
        this.taxisInRank.remove(veh);
    }


    public Vehicle getFirstTaxiFromRank()
    {
        return this.taxisInRank.poll();
    }


    public boolean hasCapacity()
    {
        return (taxisInRank.size() < this.capacity);
    }

}
