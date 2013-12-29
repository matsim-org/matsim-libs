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

package pl.poznan.put.vrp.dynamic.extensions.electric;

public class BatteryImpl
    implements Battery
{
    private double chargeInJoules;

    private final double capacityInJoules;


    public BatteryImpl(double chargeInJoules, double capacityInJoules)
    {
        this.chargeInJoules = chargeInJoules;
        this.capacityInJoules = capacityInJoules;
    }


    @Override
    public double getChargeInJoules()
    {
        return chargeInJoules;
    }


    @Override
    public double getCapacityInJoules()
    {
        return capacityInJoules;
    }


    @Override
    public void setChargeInJoules(double chargeInJoules)
    {
        this.chargeInJoules = chargeInJoules;
    }
}
