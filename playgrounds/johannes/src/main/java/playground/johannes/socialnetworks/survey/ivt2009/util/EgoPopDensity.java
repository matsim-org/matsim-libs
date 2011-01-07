/* *********************************************************************** *
 * project: org.matsim.*
 * EgoPopDensity.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.survey.ivt2009.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.matsim.contrib.sna.gis.CRSUtils;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;

import playground.johannes.socialnetworks.gis.PointUtils;
import playground.johannes.socialnetworks.gis.SpatialGrid;
import playground.johannes.socialnetworks.graph.social.SocialVertex;
import playground.johannes.socialnetworks.graph.spatial.io.Population2SpatialGraph;
import playground.johannes.socialnetworks.snowball2.social.SocialSampledGraphProjection;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseEdge;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraph;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseVertex;
import playground.johannes.socialnetworks.survey.ivt2009.graph.io.GraphReaderFacade;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public class EgoPopDensity {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Population2SpatialGraph reader1 = new Population2SpatialGraph(CRSUtils.getCRS(21781));
		SpatialGraph spatialGraph = reader1.read("/Users/jillenberger/Work/socialnets/data/schweiz/complete/plans/plans.0.10.xml");
		Set<Point> points = new HashSet<Point>();
		for(SpatialVertex v : spatialGraph.getVertices())
			points.add(v.getPoint());
		
		double size = 1000.0;
		
		Envelope env = PointUtils.envelope(points);
		SpatialGrid<Set<Point>> grid = new SpatialGrid<Set<Point>>(env.getMinX(), env.getMinY(), env.getMaxX(), env.getMaxY(), size);
		
		for(Point point : points) {
			Set<Point> set = grid.getValue(point);
			if(set == null) {
				set = new HashSet<Point>();
				grid.setValue(set, point);
			}
			set.add(point);
		}
		
		SpatialGrid<Double> densityGrid = new SpatialGrid<Double>(grid);
		for(int i = 0; i < densityGrid.getNumRows(); i++) {
			for(int j = 0; j < densityGrid.getNumCols(i); j++) {
				Set<Point> cell = grid.getValue(i, j);
				if(cell != null)
					densityGrid.setValue(i, j, cell.size()/(size*size));
				else
					densityGrid.setValue(i, j, 0.0);
			}
		}
		
		SocialSampledGraphProjection<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> socialGraph = GraphReaderFacade.read("/Users/jillenberger/Work/socialnets/data/ivt2009/09-2010/graph/noH/graph.graphml");
		socialGraph.getDelegate().transformToCRS(CRSUtils.getCRS(21781));
		
		BufferedWriter writer = new BufferedWriter(new FileWriter("/Users/jillenberger/Desktop/rho.txt"));
		writer.write("id\trho");
		writer.newLine();
		for(SocialVertex v : socialGraph.getVertices()) {
			writer.write(v.getPerson().getId().toString());
			writer.write("\t");
			if (v.getPoint() != null) {
				Double rho = densityGrid.getValue(v.getPoint());
				if (rho != null) {
					writer.write(String.valueOf(rho));
				} else {
					writer.write("NA");
				}
			} else {
				writer.write("NA");
			}
			writer.newLine();
		}
		writer.close();
	}

}
