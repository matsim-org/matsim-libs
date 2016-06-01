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

import org.apache.commons.lang3.mutable.MutableLong;


public class LongEnumAdder<K extends Enum<K>>
    extends AbstractEnumAdder<K, Long>
{
    private final EnumMap<K, MutableLong> sums;
    private long totalSum = 0;


    public LongEnumAdder(Class<K> clazz)
    {
        super(clazz);
        sums = new EnumMap<>(clazz);
        for (K e : keys) {
            sums.put(e, new MutableLong());
        }
    }


    public void addLong(K e, long value)
    {
        sums.get(e).add(value);
        totalSum += value;
    }


    @Override
    public void add(K e, Number value)
    {
        addLong(e, value.longValue());
    }


    public long getLong(K e)
    {
        return sums.get(e).longValue();
    }


    public long getLongTotal()
    {
        return totalSum;
    }


    @Override
    public Long get(K e)
    {
        return getLong(e);
    }


    @Override
    public Long getTotal()
    {
        return getLongTotal();
    }


    @Override
    public String toString()
    {
        return sums + ", total=" + totalSum;
    }
}
