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

package org.matsim.contrib.util.random;

import com.google.common.collect.*;


public class WeightedRandomSelectionTable<R, C, V>
{
    public static <R, C, V> WeightedRandomSelectionTable<R, C, V> createWithArrayTable(
            Iterable<? extends R> rowKeys, Iterable<? extends C> colKeys)
    {
        Table<R, C, WeightedRandomSelection<V>> selectionTable = ArrayTable
                .create(rowKeys, colKeys);
        return new WeightedRandomSelectionTable<R, C, V>(selectionTable);
    }


    public static <R, C, V> WeightedRandomSelectionTable<R, C, V> createWithHashBasedTable()
    {
        Table<R, C, WeightedRandomSelection<V>> selectionTable = HashBasedTable.create();
        return new WeightedRandomSelectionTable<R, C, V>(selectionTable);
    }


    // (K1, K2) -> random selection of V
    private final Table<R, C, WeightedRandomSelection<V>> selectionTable;


    private WeightedRandomSelectionTable(Table<R, C, WeightedRandomSelection<V>> selectionTable)
    {
        this.selectionTable = selectionTable;
    }


    public void add(R rowKey, C colKey, V value, double weight)
    {
        WeightedRandomSelection<V> selection = selectionTable.get(rowKey, colKey);

        if (selection == null) {
            selection = new WeightedRandomSelection<>();
            selectionTable.put(rowKey, colKey, selection);
        }

        selection.add(value, weight);
    }


    public boolean contains(R rowKey, C colKey)
    {
        return selectionTable.get(rowKey, colKey) != null;
    }


    public V select(R rowKey, C colKey)
    {
        WeightedRandomSelection<V> selection = selectionTable.get(rowKey, colKey);
        return selection == null ? null : selection.select();
    }
}
