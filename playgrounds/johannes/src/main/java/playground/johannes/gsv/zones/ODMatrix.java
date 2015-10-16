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

package playground.johannes.gsv.zones;

import playground.johannes.synpop.gis.Zone;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author johannes
 * 
 */
public class ODMatrix {

	private KeyMatrix delegate;

	private final String primaryKey;

	private Map<Zone, String> index;

	public ODMatrix() {
		this(null);
	}

	public ODMatrix(String primaryKey) {
		index = new HashMap<Zone, String>();
		delegate = new KeyMatrix();
		this.primaryKey = primaryKey;
	}

	public void set(Zone origin, Zone destination, Double value) {
		String key1 = index.get(origin);
		String key2 = index.get(destination);

		if (key1 == null) {
			key1 = createKey(origin);
			index.put(origin, key1);
		}

		if (key2 == null) {
			key2 = createKey(destination);
			index.put(destination, key2);
		}

		delegate.set(key1, key2, value);
	}

	public Double get(Zone origin, Zone destination) {
		String key1 = index.get(origin);
		String key2 = index.get(destination);

		return delegate.get(key1, key2);
	}

	private String createKey(Zone zone) {
		if (primaryKey != null) {
			return zone.getAttribute(primaryKey);
		} else {
			return String.valueOf(zone.hashCode());
		}
	}

	public Set<Zone> zones() {
		return index.keySet();
	}

	public String getPrimaryKey() {
		return primaryKey;
	}

	public Set<Zone> keySet() {
		return index.keySet();
	}

	public KeyMatrix toKeyMatrix(String key) {
		KeyMatrix m = new KeyMatrix();
		Set<Zone> keys = keySet();
		for (Zone i : keys) {
			for (Zone j : keys) {
				Double val = get(i, j);
				if (val != null) {
					String key1 = i.getAttribute(key);
					String key2 = j.getAttribute(key);
					if(key1 == null || key2 == null)
						throw new NullPointerException();
					m.set(key1, key2, val);
				}
			}
		}

		return m;
	}
}
