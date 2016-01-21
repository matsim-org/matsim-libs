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

package playground.johannes.synpop.matrix;

import java.util.*;

/**
 * @author johannes
 */
public class HashMatrix<K, V> implements Matrix<K, V> {

    private Map<K, Map<K, V>> matrix;

    public HashMatrix() {
        matrix = new HashMap<>();
    }

    public V set(K row, K column, V value) {
        Map<K, V> rowCells = matrix.get(row);
        if (rowCells == null) {
            rowCells = new HashMap<>();
            matrix.put(row, rowCells);
        }

        return rowCells.put(column, value);
    }

    public V get(K row, K column) {
        Map<K, V> rowCells = getRow(row);
        if (rowCells == null) {
            return null;
        } else {
            return rowCells.get(column);
        }
    }

    public Map<K, V> getRow(K row) {
        return matrix.get(row);
    }


    public Set<K> keys() {
        Set<K> keys = new HashSet<>(matrix.keySet());
        for (Map.Entry<K, Map<K, V>> entry : matrix.entrySet()) {
            keys.addAll(entry.getValue().keySet());
        }

        return keys;
    }

    public Collection<V> values() {
        List<V> values = new ArrayList<>();
        for (Map.Entry<K, Map<K, V>> entry : matrix.entrySet()) {
            values.addAll(entry.getValue().values());
        }
        return values;
    }
}
