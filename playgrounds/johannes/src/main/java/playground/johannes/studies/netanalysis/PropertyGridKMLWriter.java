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

import org.matsim.contrib.sna.gis.ZoneLayer;

import playground.johannes.socialnetworks.gis.io.ZoneLayerKMLWriter;
import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.social.analysis.GenderNumeric;
import playground.johannes.socialnetworks.survey.ivt2009.analysis.VertexPropertyGrid;
import playground.johannes.socialnetworks.survey.ivt2009.graph.io.SocialSparseGraphMLReader;

/**
 * @author illenberger
 *
 */
public class PropertyGridKMLWriter {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SocialSparseGraphMLReader reader = new SocialSparseGraphMLReader();
		SocialGraph graph = reader.readGraph(args[0]);
		
		ZoneLayer<Double> layer = VertexPropertyGrid.createMeanGrid(graph.getVertices(), GenderNumeric.getInstance());
		
		ZoneLayerKMLWriter writer = new ZoneLayerKMLWriter();
		writer.writeWithColor(layer, args[1]);
	}

}
