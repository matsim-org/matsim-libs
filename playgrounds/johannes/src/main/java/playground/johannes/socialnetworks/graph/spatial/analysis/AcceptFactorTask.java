/* *********************************************************************** *
 * project: org.matsim.*
 * AcceptFactorTask.java
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
package playground.johannes.socialnetworks.graph.spatial.analysis;

import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TObjectDoubleHashMap;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.analysis.AnalyzerTask;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseGraphBuilder;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.graph.spatial.io.KMLIconVertexStyle;
import org.matsim.contrib.sna.graph.spatial.io.SpatialGraphKMLWriter;
import org.matsim.contrib.sna.math.Discretizer;
import org.matsim.contrib.sna.math.Distribution;
import org.matsim.contrib.sna.math.LinearDiscretizer;
import org.matsim.contrib.sna.snowball.analysis.SnowballPartitions;

import playground.johannes.socialnetworks.gis.DistanceCalculator;
import playground.johannes.socialnetworks.gis.DistanceCalculatorFactory;
import playground.johannes.socialnetworks.graph.spatial.io.NumericAttributeColorizer;
import playground.johannes.socialnetworks.snowball2.social.SocialSampledGraphProjection;
import playground.johannes.socialnetworks.snowball2.social.SocialSampledVertexDecorator;

import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public class AcceptFactorTask extends AnalyzerTask {

	private Set<Point> points;
	
	private DistanceCalculator distCalc;
	
	private Discretizer distDiscretizer = new LinearDiscretizer(1000);
	
	private double gamma = -1.6;

	public AcceptFactorTask(Set<Point> points) {
		this.points = points;
	}
	
	@Override
	public void analyze(Graph g, Map<String, Double> stats) {
		SocialSampledGraphProjection<?, ?, ?> graph = (SocialSampledGraphProjection<?, ?, ?>) g;
		
		distCalc = DistanceCalculatorFactory.createDistanceCalculator(graph.getCoordinateReferenceSysten());
		
		Distribution distr = new Distribution();

		Set<? extends SocialSampledVertexDecorator<?>> vertices = SnowballPartitions.createSampledPartition(graph.getVertices());
		
		Distribution distDistr = new Distance().distribution(vertices);
		TDoubleDoubleHashMap hist = distDistr.absoluteDistribution(1000);
		
		TObjectDoubleHashMap<SpatialVertex> values = new TObjectDoubleHashMap<SpatialVertex>();
		
		SpatialSparseGraphBuilder builder = new SpatialSparseGraphBuilder(graph.getCoordinateReferenceSysten());
		SpatialSparseGraph newGraph = builder.createGraph();
		
		for(SpatialVertex vertex : vertices) {
			Point p1 = vertex.getPoint();
			if(p1 != null) {
				TDoubleDoubleHashMap n_d = new TDoubleDoubleHashMap();
				for(Point p2 : points) {
					double d = distCalc.distance(p1, p2);
					d = distDiscretizer.discretize(d);
					n_d.adjustOrPutValue(d, 1.0, 1.0);
				}
				
				double sum = 0;
				double cnt = 0;
				for(SpatialVertex neighbor : vertex.getNeighbours()) {
					Point p2 = neighbor.getPoint();
					if(p2 != null) {
						double d = distCalc.distance(p1, p2);
						d = distDiscretizer.discretize(d);
						double n = n_d.get(d);
						if(n > 0) {
							double w = 1/(n * Math.pow(d/1000.0, gamma));
							double k = hist.get(d) * w;
							sum += k;
							cnt++;
						}
					}
				}
				if(cnt > 0) {
				double k_i = sum/cnt;
				distr.add(k_i);
				if(k_i < 500)
					values.put(builder.addVertex(newGraph, p1), k_i);
				}
			}
		}
		
		SpatialGraphKMLWriter writer = new SpatialGraphKMLWriter();
		KMLIconVertexStyle style = new KMLIconVertexStyle(newGraph);
		NumericAttributeColorizer colorizer = new NumericAttributeColorizer(values);
		colorizer.setLogscale(true);
		style.setVertexColorizer(colorizer);
		writer.setKmlVertexStyle(style);
		writer.addKMZWriterListener(style);
		
		writer.write(newGraph, getOutputDirectory() + "konstants.kmz");
		
		try {
			writeHistograms(distr, 10, false, "acceptFactors");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
