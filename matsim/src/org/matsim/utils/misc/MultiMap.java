/* *********************************************************************** *
 * project: org.matsim.*
 * MultiMap.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.utils.misc;


/**
 * @author mrieser
 *
 * A minimal interface for a multimap. Feel free to extend with other map-functions when needed
 *
 * @param <K> The type of keys in the map.
 * @param <V> The type of values in the map.
 */
public interface MultiMap<K, V> {
	V put(K key, V value);
	K firstKey();
	V remove(K key);
}
