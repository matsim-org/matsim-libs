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

package playground.michalm.util;

import org.matsim.api.core.v01.Id;

import pl.poznan.put.util.random.WeightedRandomSelection;

import com.google.common.collect.*;


public class IdKeySelection<K, V>
{
    // (Id, K) -> random selection of V
    private final Table<Id, K, WeightedRandomSelection<V>> selectionTable;


    public IdKeySelection(Iterable<? extends Id> ids, Iterable<? extends K> keys)
    {
        selectionTable = ArrayTable.create(ids, keys);
    }


    public IdKeySelection()
    {
        selectionTable = HashBasedTable.create();
    }


    public void add(Id id, K key, V value, double weight)
    {
        WeightedRandomSelection<V> selection = selectionTable.get(id, key);

        if (selection == null) {
            selection = new WeightedRandomSelection<V>();
            selectionTable.put(id, key, selection);
        }

        selection.add(value, weight);
    }


    public boolean contains(Id id, K key)
    {
        return selectionTable.get(id, key) != null;
    }


    public V select(Id id, K key)
    {
        WeightedRandomSelection<V> selection = selectionTable.get(id, key);
        return selection == null ? null : selection.select();
    }
}
