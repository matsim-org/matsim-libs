/* *********************************************************************** *
 * project: org.matsim.*
 * DgZonesUtils
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
package playground.dgrether.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeBuilder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.dgrether.signalsystems.cottbus.scripts.DgCottbus2KoehlerStrehler2010;

import com.vividsolutions.jts.geom.Polygon;


/**
 * @author dgrether
 *
 */
public class DgZonesUtils {
	
	private static final Logger log = Logger.getLogger(DgZonesUtils.class);
	
	public static List<DgZone> createZonesFromGrid(DgGrid grid){
		int id = 0;
		List<DgZone> cells = new ArrayList<DgZone>();
		for (Polygon p : grid){
			id++;
			DgZone cell = new DgZone(Integer.toString(id), p);
			cells.add(cell);
			log.info("Envelope of cell " + id + " is " + cell.getEnvelope());
		}
		return cells;
	}

	
	public static Map<DgZone, Link> createZoneCenter2LinkMapping(List<DgZone> zones, NetworkImpl network){
		Map<DgZone, Link> map = new HashMap<DgZone, Link>();
		for (DgZone zone : zones){
			Coord coord = MGC.coordinate2Coord(zone.getCenter());
			LinkImpl link = network.getNearestLink(coord);
			map.put(zone, link);
		}
		return map;
	}


	public static void writePolygonZones2Shapefile(List<DgZone> cells, CoordinateReferenceSystem crs, String shapeFilename){
		Collection<Feature> featureCollection = new ArrayList<Feature>();
		FeatureType featureType = null;
		AttributeType [] attribs = new AttributeType[3];
		attribs[0] = DefaultAttributeTypeFactory.newAttributeType("Polygon", Polygon.class, true, null, null, crs);
		attribs[1] = AttributeTypeFactory.newAttributeType("to_cell_id", String.class);
		attribs[2] = AttributeTypeFactory.newAttributeType("trips", Integer.class);
			
		try {
			featureType = FeatureTypeBuilder.newFeatureType(attribs, "grid_cell");
			for (DgZone cell : cells){
				DgCottbus2KoehlerStrehler2010.log.info("writing cell: " + cell.getId());
				List<Object> attributes = new ArrayList<Object> ();
				Polygon p = cell.getPolygon();
				attributes.add(p);
				for (Entry<DgZone, Integer> entry : cell.getToRelationships().entrySet()){
					DgCottbus2KoehlerStrehler2010.log.info("  to cell " + entry.getKey().getId() + " # trips: " + entry.getValue());
					attributes.add( entry.getKey().getId());
					attributes.add( entry.getValue());
					Object[] atts = attributes.toArray();
					Feature feature = featureType.create(atts);
					featureCollection.add(feature);
					attributes.clear();
					attributes.add(p);
				}
			}		
			ShapeFileWriter.writeGeometries(featureCollection, shapeFilename);
		} catch (Exception  e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} 
	}
	

}
