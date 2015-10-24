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


import org.matsim.contrib.socnetgen.sna.gis.ZoneLayer;
import org.matsim.contrib.socnetgen.sna.gis.io.ZoneLayerKMLWriter;
import org.matsim.contrib.socnetgen.sna.graph.social.analysis.Age;
import org.matsim.contrib.socnetgen.socialnetworks.survey.ivt2009.analysis.VertexPropertyGrid;
import org.matsim.contrib.socnetgen.socialnetworks.survey.ivt2009.graph.SocialSparseGraph;
import org.matsim.contrib.socnetgen.socialnetworks.survey.ivt2009.graph.io.SocialSparseGraphMLReader;

import java.io.IOException;

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
		
		SocialSparseGraphMLReader reader = new SocialSparseGraphMLReader();
		SocialSparseGraph graph = reader.readGraph("/Users/jillenberger/vsp/work/coopsim/data/graph.synth-age.graphml");
		
		ZoneLayer<Double> layer = VertexPropertyGrid.createMeanGrid(graph.getVertices(), Age.getInstance(), 5000.0);
		
//		Set<SimpleFeature> features = FeatureSHP.readFeatures("/Users/jillenberger/Work/socialnets/data/schweiz/complete/zones/G1L08.shp");
//		Geometry ch = (Geometry) features.iterator().next().getDefaultGeometry();
//		
//		Set<Zone> remove = new HashSet<Zone>();
//		for(Zone z : layer.getZones()) {
//			if(!ch.contains(z.getGeometry())) {
//				remove.add(z);
//			}
//		}
//		
//		Set<Zone<Double>> zones = new HashSet<Zone<Double>>(layer.getZones());
//		zones.removeAll(remove);
//		
//		layer = new ZoneLayer<Double>(zones);
		ZoneLayerKMLWriter writer = new ZoneLayerKMLWriter();
		writer.writeWithColor(layer, "/Users/jillenberger/vsp/work/coopsim/data/age.kmz");
	}

}
