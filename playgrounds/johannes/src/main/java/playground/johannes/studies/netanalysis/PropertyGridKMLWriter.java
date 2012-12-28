/* *********************************************************************** *
 * project: org.matsim.*
 * PropertyGridKMLWriter.java
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
package playground.johannes.studies.netanalysis;


import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.opengis.feature.simple.SimpleFeature;

import playground.johannes.sna.gis.Zone;
import playground.johannes.sna.gis.ZoneLayer;
import playground.johannes.sna.graph.analysis.Degree;
import playground.johannes.sna.graph.spatial.SpatialGraph;
import playground.johannes.sna.graph.spatial.io.SpatialGraphMLReader;
import playground.johannes.socialnetworks.gis.io.FeatureSHP;
import playground.johannes.socialnetworks.gis.io.ZoneLayerSHP;
import playground.johannes.socialnetworks.survey.ivt2009.analysis.VertexPropertyGrid;

import com.vividsolutions.jts.geom.Geometry;

/**
 * @author illenberger
 *
 */
public class PropertyGridKMLWriter {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
//		SocialSparseGraphMLReader reader = new SocialSparseGraphMLReader();
//		SocialGraph graph = reader.readGraph(args[0]);
//		
//		ZoneLayer<Double> layer = VertexPropertyGrid.createMeanGrid(graph.getVertices(), GenderNumeric.getInstance());
//		
//		ZoneLayerKMLWriter writer = new ZoneLayerKMLWriter();
//		writer.writeWithColor(layer, args[1]);
		
		SpatialGraphMLReader reader = new SpatialGraphMLReader();
		SpatialGraph graph = reader.readGraph("/Users/jillenberger/Work/socialnets/mcmc/output/plain/graph.graphml");
		
		ZoneLayer<Double> layer = VertexPropertyGrid.createMeanGrid(graph.getVertices(), Degree.getInstance(), 1000.0);
		
		Set<SimpleFeature> features = FeatureSHP.readFeatures("/Users/jillenberger/Work/socialnets/data/schweiz/complete/zones/G1L08.shp");
		Geometry ch = (Geometry) features.iterator().next().getDefaultGeometry();
		
		Set<Zone> remove = new HashSet<Zone>();
		for(Zone z : layer.getZones()) {
			if(!ch.contains(z.getGeometry())) {
				remove.add(z);
			}
		}
		
		Set<Zone<Double>> zones = new HashSet<Zone<Double>>(layer.getZones());
		zones.removeAll(remove);
		
		layer = new ZoneLayer<Double>(zones);
		ZoneLayerSHP.write(layer, "/Users/jillenberger/Work/socialnets/mcmc/output/plain/degreegrid.shp");
	}

}
