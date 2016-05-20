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

public abstract class AbstractEnumAdder<K extends Enum<K>, N extends Number>
    implements EnumAdder<K, N>
{
    protected final K[] keys;


    public AbstractEnumAdder(Class<K> clazz)
    {
        this.keys = clazz.getEnumConstants();
    }


    public K[] getKeys()
    {
        return keys;

    }


    public void increment(K e)
    {
        add(e, 1);//(Integer)1 is cached internally by JVM, so shouldn't be so costly
    }


    @Override
    public void addAll(EnumAdder<K, ?> enumAdder)
    {
        for (K e : keys) {
            add(e, enumAdder.get(e));
        }
    }
}
