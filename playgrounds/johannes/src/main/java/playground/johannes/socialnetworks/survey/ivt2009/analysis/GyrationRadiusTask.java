/* *********************************************************************** *
 * project: org.matsim.*
 * GyrationRadiusTask.java
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
package playground.johannes.socialnetworks.survey.ivt2009.analysis;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.AnalyzerTask;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.math.Distribution;
import org.matsim.contrib.sna.snowball.SampledVertex;

import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 * 
 */
public class GyrationRadiusTask extends AnalyzerTask {

	@Override
	public void analyze(Graph graph, Map<String, Double> stats) {
		try {
			GyrationRadius radius = new GyrationRadius();
			Distribution distr = new Distribution();
			for (Vertex v : graph.getVertices()) {
				if (((SampledVertex) v).isSampled()) {
					distr.add(radius.radiusOfGyration((SpatialVertex) v));
				}
			}

			Distribution.writeHistogram(distr.absoluteDistribution(5000), getOutputDirectory() + "/gyration.txt");
			
			/*
			 * triangle gyration
			 */
			Distribution triangles = new Distribution();
			for (Vertex v : graph.getVertices()) {
				List<? extends Vertex> n1s = v.getNeighbours();
				for (int i = 0; i < n1s.size(); i++) {
					List<? extends Vertex> n2s = n1s.get(i).getNeighbours();
					for (int k = 0; k < n2s.size(); k++) {
						if (!n2s.get(k).equals(v)) {
							if (n2s.get(k).getNeighbours().contains(v)) {
								/*
								 * found triangle
								 */
								Set<Point> points = new HashSet<Point>();
								points.add(((SpatialVertex)v).getPoint());
								points.add(((SpatialVertex)n1s.get(i)).getPoint());
								points.add(((SpatialVertex)n2s.get(k)).getPoint());
								
								triangles.add(radiusOfGyration(points));
							}
						}
					}
				}
			}
			
			Distribution.writeHistogram(triangles.absoluteDistribution(1000), getOutputDirectory()+"/c_gyration.txt");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private double[] centerMass(Set<Point> points) {
		double xsum = 0;
		double ysum = 0;
		
		double cnt = 0;
		for(Point neighbor : points) {
			if(neighbor != null) {
			xsum += neighbor.getX();
			ysum += neighbor.getY();
			cnt++;
			}
		}
		
		
		return new double[]{xsum/cnt, ysum/cnt};
	}
	
	public double radiusOfGyration(Set<Point> points) {
		double dsum = 0;
		
		
		double[] cm = centerMass(points);
		double xcm = cm[0];
		double ycm = cm[1];
		double cnt = 0;
		for(Point neighbor : points) {
			if(neighbor != null) {
			double dx = neighbor.getX() - xcm;
			double dy = neighbor.getY() - ycm;
			double d = (dx*dx + dy*dy);
			
			dsum += d;
			cnt++;
			}
		}
		
		return Math.sqrt(dsum/cnt);
	}

}
