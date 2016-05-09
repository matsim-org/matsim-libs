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

import java.util.*;

import org.apache.commons.lang3.mutable.MutableLong;


public class EnumAdder<K extends Enum<K>>
{
    private final EnumMap<K, MutableLong> sums;


    public EnumAdder(Class<K> clazz)
    {
        sums = new EnumMap<>(clazz);
        for (K e : clazz.getEnumConstants()) {
            sums.put(e, new MutableLong());
        }
    }


    public void add(K e, int value)
    {
        sums.get(e).add(value);
    }


    public void addAll(EnumAdder<K> enumAdder)
    {
        for (Map.Entry<K, MutableLong> e : enumAdder.sums.entrySet()) {
            sums.get(e.getKey()).add(e.getValue().longValue());
        }
    }


    public long getSum(K e)
    {
        return sums.get(e).longValue();
    }


    public long getTotalSum()
    {
        long total = 0;
        for (MutableLong s : sums.values()) {
            total += s.longValue();
        }
        return total;
    }
}
