/* *********************************************************************** *
 * project: org.matsim.*
 * DgCommodityUtils
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
package playground.dgrether.koehlerstrehlersignal.data;

import java.util.ArrayList;
import java.util.List;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;


/**
 * @author dgrether
 *
 */
public class DgCommodityUtils {

	public static void write2Shapefile(DgCommodities commodities, Network network, CoordinateReferenceSystem crs, String outputFilename) {
		GeometryFactory geoFac = new GeometryFactory(); 
		List<SimpleFeature> featureCollection = new ArrayList<SimpleFeature>();
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setCRS(crs);
		b.setName("commodities");
		b.add("location", LineString.class);
		b.add("id", String.class);
		b.add("source_node", String.class);
		b.add("drain_node", String.class);
		b.add("flow", Double.class);
		SimpleFeatureBuilder builder = new SimpleFeatureBuilder(b.buildFeatureType());
		for (DgCommodity commodity : commodities.getCommodities().values()) {
			Node sourceNode = network.getNodes().get(commodity.getSourceNodeId());
			Node drainNode = network.getNodes().get(commodity.getDrainNodeId());
			Coordinate startCoordinate = MGC.coord2Coordinate(sourceNode.getCoord());
			Coordinate endCoordinate = MGC.coord2Coordinate(drainNode.getCoord());
			Coordinate[] coordinates = { startCoordinate, endCoordinate };
			LineString lineString = geoFac.createLineString(coordinates);
			Object[] atts = { lineString, commodity.getId().toString(), commodity.getSourceNodeId().toString(),
					commodity.getDrainNodeId().toString(), Double.toString(commodity.getFlow()) };
			SimpleFeature feature = builder.buildFeature(commodity.getId().toString(), atts);
			featureCollection.add(feature);
		}
		if (! featureCollection.isEmpty()) {
			ShapeFileWriter.writeGeometries(featureCollection, outputFilename);
		}
		
	}

}
