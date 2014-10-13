/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.pieter.collections;

import java.util.Iterator;
import java.util.TreeMap;
import java.util.Map.Entry;

class TreeMapWithRowByRowToString<K, V> extends TreeMap<K, V> {

	@Override
	public String toString() {
	    
	        Iterator<Entry<K,V>> i = entrySet().iterator();
	        if (! i.hasNext())
	            return "{}";

	        StringBuilder sb = new StringBuilder();
	        sb.append("{\n");
	        for (;;) {
	            Entry<K,V> e = i.next();
	            K key = e.getKey();
	            V value = e.getValue();
	            sb.append(key   == this ? "(this Map)" : key);
	            sb.append('\t');
	            sb.append(value == this ? "(this Map)" : value);
	            sb.append('\n');
	            if (! i.hasNext())
	                return sb.append('}').toString();
	            sb.append(',').append(' ');
	        }
	    
		
	}

}
