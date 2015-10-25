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
import org.matsim.contrib.common.gis.CRSUtils;
import org.matsim.contrib.common.gis.EsriShapeIO;
import org.matsim.contrib.socnetgen.sna.graph.GraphBuilder;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialSparseEdge;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialSparseGraph;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialSparseVertex;
import org.matsim.contrib.socnetgen.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.socnetgen.sna.graph.spatial.analysis.SpatialFilter;
import org.matsim.contrib.socnetgen.sna.snowball.social.SocialSampledGraphProjection;
import org.matsim.contrib.socnetgen.sna.snowball.social.SocialSampledGraphProjectionBuilder;
import org.opengis.feature.simple.SimpleFeature;
import playground.johannes.studies.sbsurvey.io.GraphReaderFacade;

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
