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
public class ZoneLayerLegacy {

	private Map<Geometry, ZoneLegacy> zones;
	
	private Map<Id, ZoneLegacy> ids;
	
	private GeometryLayer geoLayer;
	
	public ZoneLayerLegacy(Set<ZoneLegacy> zones) {
		this.zones = new LinkedHashMap<Geometry, ZoneLegacy>();
		ids = new HashMap<Id, ZoneLegacy>();
		for(ZoneLegacy zone : zones) {
			this.zones.put(zone.getBorder(), zone);
			ids.put(zone.getId(), zone);
		}
		
		geoLayer = new GeometryLayer(this.zones.keySet());
	}
	
	public GeometryLayer getGeometryLayer() {
		return geoLayer;
	}
	
	public ZoneLegacy getZone(Coord c) {
		Geometry g = geoLayer.getZone(c);
		return zones.get(g);
	}
	
	public ZoneLegacy getZone(Id id) {
		return ids.get(id);
	}
	
	public Collection<ZoneLegacy> getZones() {
		return zones.values();
	}
	
	public static ZoneLayerLegacy createFromShapeFile(String filename) throws IOException {
		FeatureSource source = ShapeFileReader.readDataFile(filename);
		
		Set<ZoneLegacy> zones = new LinkedHashSet<ZoneLegacy>();
		
		Iterator<Feature> it = source.getFeatures().iterator();
		while(it.hasNext()) {
			Feature feature = it.next();
			ZoneLegacy zone = new ZoneLegacy(feature.getDefaultGeometry(), new IdImpl(feature.getID()));
			zones.add(zone);
		}
		
		ZoneLayerLegacy zoneLayer = new ZoneLayerLegacy(zones);
		
		return zoneLayer;
	}
	
	public static void main(String args[]) throws IOException {
		ZoneLayerLegacy layer = ZoneLayerLegacy.createFromShapeFile("/Users/fearonni/vsp-work/work/socialnets/data/schweiz/complete/zones/gg-qg.merged.shp");
		
		for(ZoneLegacy zone : layer.getZones()) {
			System.out.println(zone.getId());
		}
		
	}
}
