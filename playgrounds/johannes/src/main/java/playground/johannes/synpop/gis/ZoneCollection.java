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

package playground.johannes.synpop.gis;

import com.vividsolutions.jts.algorithm.locate.IndexedPointInAreaLocator;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Location;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.strtree.STRtree;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * @author johannes
 * 
 */
public class ZoneCollection {

	private static final Logger logger = Logger.getLogger(ZoneCollection.class);

	private final String id;

	private final Set<Zone> zones;

	private SpatialIndex spatialIndex;

	private Map<String, Zone> keyIndex;

	private String primaryKey;

	public ZoneCollection(String id) {
		this.id = id;
		zones = new LinkedHashSet<>();
	}

	public String getId() {
		return id;
	}

	public void setPrimaryKey(String key) {
		this.primaryKey = key;
		buildKeyIndex();
	}

	public String getPrimaryKey() {
		return primaryKey;
	}

	public void add(Zone zone) {
		this.zones.add(zone);
		buildIndex();
	}
	
	public void addAll(Collection<Zone> zones) {
		this.zones.addAll(zones);
		buildIndex();
	}

	private void buildIndex() {
		buildKeyIndex();
		buildSpatialIndex();
	}

	private void buildKeyIndex() {
		keyIndex = new HashMap<>();
		if (primaryKey != null) {
			for (Zone zone : zones) {
				String key = zone.getAttribute(primaryKey);
				if(key == null)
					throw new NullPointerException();
				if(null != keyIndex.put(key, zone)) {
					logger.warn(String.format("Overwriting key %s.", zone.getAttribute(primaryKey)));
//					throw new RuntimeException("Overwriting key " + zone.getAttribute(primaryKey));
				}
			}
		}
	}

	private void buildSpatialIndex() {
		//TODO check SRID fields
		//TODO check for overlapping polygons
		
		STRtree tree = new STRtree();
		for (Zone zone : zones) {
			tree.insert(zone.getGeometry().getEnvelopeInternal(), new IndexEntry(zone));
		}

		tree.build();

		spatialIndex = tree;

	}

	public Set<Zone> getZones() {
		return Collections.unmodifiableSet(zones);
	}
	
	public Zone get(String key) {
		return keyIndex.get(key);
	}

	public Zone get(Coordinate c) {
		List<IndexEntry> candidates = spatialIndex.query(new Envelope(c));

		if (candidates.size() == 1) {
			return candidates.get(0).zone;
		}

		for (IndexEntry entry : candidates) {
			if (entry.contains(c)) {
				return entry.zone;
			}
		}

		return null;
	}

	private class IndexEntry {

		private IndexedPointInAreaLocator locator;

		private final Zone zone;

		private IndexEntry(Zone zone) {
			this.zone = zone;
		}

		private boolean contains(Coordinate c) {
			if (locator == null) {
				locator = new IndexedPointInAreaLocator(zone.getGeometry());
			}

			if (locator.locate(c) == Location.INTERIOR)
				return true;
			else
				return false;
		}
	}
}
