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

package playground.pieter.balmermi.world;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;

public class World {

	private final Map<Id, Layer> layers = new TreeMap<>();

	@Deprecated
	public final Layer createLayer(final Id type) {
		if (this.layers.containsKey(type)) {
			throw new IllegalArgumentException("Layer type=" + type + " already exixts.");
		}
		return this.createZoneLayer(type);
	}

	@Deprecated
	private ZoneLayer createZoneLayer(final Id type) {
		ZoneLayer l = new ZoneLayer();
		this.layers.put(type,l);
		return l;
	}

	public final Layer getLayer(final Id layer_type) {
		return this.layers.get(layer_type);
	}

	@Deprecated
	public final Layer getLayer(final String layer_type) {
		return this.layers.get(Id.create(layer_type,Layer.class));
	}

	@Override
	public final String toString() {
		return "[nof_layers=" + this.layers.size() + "]";
	}

}
