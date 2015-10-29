/* *********************************************************************** *
 * project: org.matsim.*
 * CollectionUtils.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package org.matsim.contrib.common.collections;

import java.util.*;
import java.util.Map.Entry;

/**
 * @author illenberger
 */
public class CollectionUtils {

    /**
     * Splits a collection in <tt>n</tt> new collections. The last collection may contain more elements than the others
     * if {@code set.size() % n != 0}.
     *
     * @param set the input collection
     * @param n   the number of new collections
     * @param <T> the element type
     * @return a list containing the new collections
     */
    public static <T> List<T>[] split(Collection<T> set, int n) {
        if (set.size() >= n) {
            @SuppressWarnings("unchecked")
            List<T>[] arrays = new List[n];
            int minSegmentSize = (int) Math.floor(set.size() / (double) n);

            int start = 0;
            int stop = minSegmentSize;

            Iterator<T> it = set.iterator();

            for (int i = 0; i < n - 1; i++) {
                int segmentSize = stop - start;
                List<T> segment = new ArrayList<T>(segmentSize);
                for (int k = 0; k < segmentSize; k++) {
                    segment.add(it.next());
                }
                arrays[i] = segment;
                start = stop;
                stop += segmentSize;
            }

            int segmentSize = set.size() - start;
            List<T> segment = new ArrayList<T>(segmentSize);
            for (int k = 0; k < segmentSize; k++) {
                segment.add(it.next());
            }
            arrays[n - 1] = segment;

            return arrays;
        } else {
            throw new IllegalArgumentException("n must not be smaller set size!");
        }
    }

    /**
     * Sorts a map by its values in ascending order.
     *
     * @param map a map
     * @param <K> the key type
     * @param <V> the value type
     * @return a new map
     */
    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        return sortByValue(map, false);
    }

    /**
     * Sorts a map by its values.
     *
     * @param map        a map
     * @param descending if <tt>true</tt> the map is sorted descending, otherwise ascending
     * @param <K>        the key type
     * @param <V>        the value type
     * @return a new map
     */
    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map, boolean descending) {
        int s2 = 1;
        if (descending)
            s2 = -1;
        final int sign = s2;

        List<Map.Entry<K, V>> list = new LinkedList<>(map.entrySet());

        Collections.sort(list, new Comparator<Map.Entry<K, V>>() {

            @Override
            public int compare(Entry<K, V> o1, Entry<K, V> o2) {
                return sign * o1.getValue().compareTo(o2.getValue());
            }
        });

        Map<K, V> sorted = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            sorted.put(entry.getKey(), entry.getValue());
        }

        return sorted;
    }
}
