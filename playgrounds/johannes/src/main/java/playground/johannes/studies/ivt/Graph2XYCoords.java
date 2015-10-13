/* *********************************************************************** *
 * project: org.matsim.*
 * Graph2XYCoords.java
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
package playground.johannes.studies.ivt;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import org.matsim.contrib.common.gis.EsriShapeIO;
import org.opengis.feature.simple.SimpleFeature;
import playground.johannes.sna.gis.CRSUtils;
import playground.johannes.sna.graph.GraphBuilder;
import playground.johannes.sna.graph.spatial.SpatialVertex;
import playground.johannes.socialnetworks.graph.spatial.analysis.SpatialFilter;
import playground.johannes.socialnetworks.snowball2.social.SocialSampledGraphProjection;
import playground.johannes.socialnetworks.snowball2.social.SocialSampledGraphProjectionBuilder;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseEdge;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraph;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseVertex;
import playground.johannes.socialnetworks.survey.ivt2009.graph.io.GraphReaderFacade;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author illenberger
 *
 */
public class Graph2XYCoords {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		SocialSampledGraphProjection<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> graph = GraphReaderFacade.read("/Users/jillenberger/Work/socialnets/data/ivt2009/11-2011/graph/graph.graphml");
		
		SocialSampledGraphProjectionBuilder<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> builder = new SocialSampledGraphProjectionBuilder<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge>();
		
		SimpleFeature feature = EsriShapeIO.readFeatures("/Users/jillenberger/Work/socialnets/data/schweiz/complete/zones/G1L08.shp").iterator().next();
		Geometry chBorder = (Geometry) feature.getDefaultGeometry();
		chBorder.setSRID(21781);
		
		graph.getDelegate().transformToCRS(CRSUtils.getCRS(21781));
		
		SpatialFilter filter = new SpatialFilter((GraphBuilder) builder, chBorder);
		graph = (SocialSampledGraphProjection<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge>) filter.apply(graph);
		
		BufferedWriter writer = new BufferedWriter(new FileWriter("/Users/jillenberger/Work/socialnets/data/ivt2009/11-2011/graph/xy.txt"));
		writer.write("x\ty");
		writer.newLine();
		
		for(SpatialVertex v : graph.getVertices()) {
			Point p = v.getPoint();
			if(p != null) {
				writer.write(String.valueOf(p.getX()));
				writer.write("\t");
				writer.write(String.valueOf(p.getY()));
				writer.newLine();
			}
		}
		
		writer.close();
	}

}
