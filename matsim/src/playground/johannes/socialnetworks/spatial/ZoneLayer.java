/* *********************************************************************** *
 * project: org.matsim.*
 * ZoneLayer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.spatial;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.gis.ShapeFileReader;

import com.vividsolutions.jts.geom.Geometry;

/**
 * @author illenberger
 *
 */
public class ZoneLayer {

	private Map<Geometry, Zone> zones;
	
	private Map<Id, Zone> ids;
	
	private GeometryLayer geoLayer;
	
	public ZoneLayer(Set<Zone> zones) {
		this.zones = new LinkedHashMap<Geometry, Zone>();
		ids = new HashMap<Id, Zone>();
		for(Zone zone : zones) {
			this.zones.put(zone.getBorder(), zone);
			ids.put(zone.getId(), zone);
		}
		
		geoLayer = new GeometryLayer(this.zones.keySet());
	}
	
	public GeometryLayer getGeometryLayer() {
		return geoLayer;
	}
	
	public Zone getZone(Coord c) {
		Geometry g = geoLayer.getZone(c);
		return zones.get(g);
	}
	
	public Zone getZone(Id id) {
		return ids.get(id);
	}
	
	public Collection<Zone> getZones() {
		return zones.values();
	}
	
	public static ZoneLayer createFromShapeFile(String filename) throws IOException {
		FeatureSource source = ShapeFileReader.readDataFile(filename);
		
		Set<Zone> zones = new LinkedHashSet<Zone>();
		
		Iterator<Feature> it = source.getFeatures().iterator();
		while(it.hasNext()) {
			Feature feature = it.next();
			Zone zone = new Zone(feature.getDefaultGeometry(), new IdImpl(feature.getID()));
			zones.add(zone);
		}
		
		ZoneLayer zoneLayer = new ZoneLayer(zones);
		
		return zoneLayer;
	}
	
	public static void main(String args[]) throws IOException {
		ZoneLayer layer = ZoneLayer.createFromShapeFile("/Users/fearonni/vsp-work/work/socialnets/data/schweiz/complete/zones/gg-qg.merged.shp");
		
		for(Zone zone : layer.getZones()) {
			System.out.println(zone.getId());
		}
		
	}
}
