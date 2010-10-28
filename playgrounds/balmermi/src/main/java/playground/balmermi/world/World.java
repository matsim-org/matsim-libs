/* *********************************************************************** *
 * project: org.matsim.*
 * World.java
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

package playground.balmermi.world;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

public class World {

	private final Map<Id, Layer> layers = new TreeMap<Id, Layer>();

	@Deprecated
	public final Layer createLayer(final Id type) {
		if (this.layers.containsKey(type)) {
			throw new IllegalArgumentException("Layer type=" + type + " already exixts.");
		}
		return this.createZoneLayer(type);
	}

	@Deprecated
	private final ZoneLayer createZoneLayer(final Id type) {
		ZoneLayer l = new ZoneLayer();
		this.layers.put(type,l);
		return l;
	}

	public final Layer getLayer(final Id layer_type) {
		return this.layers.get(layer_type);
	}

	@Deprecated
	public final Layer getLayer(final String layer_type) {
		return this.layers.get(new IdImpl(layer_type));
	}

	@Override
	public final String toString() {
		return "[nof_layers=" + this.layers.size() + "]";
	}

}
