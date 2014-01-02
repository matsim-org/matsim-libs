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

package pl.poznan.put.vrp.dynamic.data.network.impl;

import org.matsim.api.core.v01.network.Link;


public class ConstantArc
    extends AbstractArc
{
    private int arcTime;
    private double arcCost;


    public ConstantArc(Link fromLink, Link toLink, int arcTime, double arcCost)
    {
        super(fromLink, toLink);
        this.arcTime = arcTime;
        this.arcCost = arcCost;
    }


    @Override
    public int getTimeOnDeparture(int departureTime)
    {
        return arcTime;
    }


    @Override
    public int getTimeOnArrival(int arrivalTime)
    {
        return arcTime;
    }


    @Override
    public double getCostOnDeparture(int departureTime)
    {
        return arcCost;
    }
}
