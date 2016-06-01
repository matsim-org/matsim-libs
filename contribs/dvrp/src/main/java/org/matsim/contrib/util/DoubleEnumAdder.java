/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.util;

import java.util.EnumMap;

import org.apache.commons.lang3.mutable.MutableDouble;


public class DoubleEnumAdder<K extends Enum<K>>
    extends AbstractEnumAdder<K, Double>
{
    private final EnumMap<K, MutableDouble> sums;
    private double totalSum;


    public DoubleEnumAdder(Class<K> clazz)
    {
        super(clazz);
        sums = new EnumMap<>(clazz);
        for (K e : keys) {
            sums.put(e, new MutableDouble());
        }
    }


    public void addDouble(K e, double value)
    {
        sums.get(e).add(value);
        totalSum += value;
    }


    @Override
    public void add(K e, Number value)
    {
        addDouble(e, value.doubleValue());
    }


    public double getDouble(K e)
    {
        return sums.get(e).doubleValue();
    }


    public double getDoubleTotal()
    {
        return totalSum;
    }


    @Override
    public Double get(K e)
    {
        return getDouble(e);
    }


    @Override
    public Double getTotal()
    {
        return getDoubleTotal();
    }


    @Override
    public String toString()
    {
        return sums + ", total=" + totalSum;
    }
}
