/* *********************************************************************** *
 * project: org.matsim.*
 * DegreeGridTask.java
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

import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.Degree;
import org.matsim.contrib.sna.graph.analysis.ModuleAnalyzerTask;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.math.Distribution;

import playground.johannes.socialnetworks.gis.SpatialGrid;
import playground.johannes.socialnetworks.gis.SpatialGridKMLWriter;

/**
 * @author illenberger
 *
 */
public class DegreeGridTask extends ModuleAnalyzerTask<Degree> {

	/* (non-Javadoc)
	 * @see org.matsim.contrib.sna.graph.analysis.AnalyzerTask#analyze(org.matsim.contrib.sna.graph.Graph, java.util.Map)
	 */
	@Override
	public void analyze(Graph graph, Map<String, Double> stats) {
		SpatialGraph spatialGraph = (SpatialGraph)graph;
		
		double xMin = Double.MAX_VALUE;
		double yMin = Double.MAX_VALUE;
		double xMax = - Double.MAX_VALUE;
		double yMax = - Double.MAX_VALUE;
		
		for(SpatialVertex vertex : spatialGraph.getVertices()) {
			xMin = Math.min(xMin, vertex.getPoint().getX());
			yMin = Math.min(yMin, vertex.getPoint().getY());
			xMax = Math.max(xMax, vertex.getPoint().getX());
			yMax = Math.max(yMax, vertex.getPoint().getY());
		}
		
		SpatialGrid<Set<SpatialVertex>> vertexGrid = new SpatialGrid<Set<SpatialVertex>>(xMin, yMin, xMax, yMax, 0.3);
		
		TObjectDoubleHashMap<Vertex> kValues = module.values(spatialGraph.getVertices());
		
		TObjectDoubleIterator<Vertex> it = kValues.iterator();
		for(int i = 0; i < kValues.size(); i++) {
			it.advance();
			Set<SpatialVertex> set = vertexGrid.getValue(((SpatialVertex)it.key()).getPoint());
			if(set == null) {
				set = new HashSet<SpatialVertex>();
				vertexGrid.setValue(set, ((SpatialVertex)it.key()).getPoint());
			}
			set.add((SpatialVertex) it.key());
		}
		
		SpatialGrid<Double> kGrid = new SpatialGrid<Double>(xMin, yMin, xMax, yMax, 0.3);
		for(int i = 0; i < kGrid.getNumRows(); i++) {
			for(int j = 0; j < kGrid.getNumCols(i); j++) {
				Set<SpatialVertex> set = vertexGrid.getValue(i, j);
				if(set != null) {
					DescriptiveStatistics distr = module.distribution(set);
					kGrid.setValue(i, j, distr.getMean());
				} else {
					kGrid.setValue(i, j, 0.0);
				}
			}
		}
		
		SpatialGridKMLWriter writer = new SpatialGridKMLWriter();
		writer.write(kGrid, getOutputDirectory() + "/kGrid.kmz");
	}

}
