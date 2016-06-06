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

package org.matsim.contrib.util.histogram;

import org.matsim.contrib.util.EnumAdder;


public class EnumAdderHistogram<T extends Enum<T>, N extends Number>
    implements Histogram<T>
{
    private final EnumAdder<T, N> adder;


    public EnumAdderHistogram(EnumAdder<T, N> adder)
    {
        this.adder = adder;
    }


    @Override
    public int getBinCount()
    {
        return adder.getKeys().length;
    }


    @Override
    public T getBin(int idx)
    {
        return adder.getKeys()[idx];
    }


    @Override
    public long getCount(int idx)
    {
        T key = getBin(idx);
        return adder.get(key).longValue();
    }


    @Override
    public long getTotalCount()
    {
        return adder.getTotal().longValue();
    }
}
