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

package org.matsim.contrib.dvrp.extensions.electric;

public class BatteryImpl
    implements Battery
{
    private final double capacity;
    private double energy;


    public BatteryImpl(double energy, double capacity)
    {
        this.energy = energy;
        this.capacity = capacity;
    }


    @Override
    public double getCapacity()
    {
        return capacity;
    }


    @Override
    public double getEnergy()
    {
        return energy;
    }


    @Override
    public void setEnergy(double energy)
    {
        this.energy = energy;
    }
}
