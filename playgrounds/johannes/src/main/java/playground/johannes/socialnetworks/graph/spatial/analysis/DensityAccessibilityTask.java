/* *********************************************************************** *
 * project: org.matsim.*
 * DensityAccessibilityTask.java
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
import gnu.trove.TDoubleDoubleIterator;
import gnu.trove.TDoubleIntHashMap;
import gnu.trove.TDoubleObjectHashMap;
import gnu.trove.TDoubleObjectIterator;
import gnu.trove.TObjectDoubleHashMap;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.StatUtils;
import org.hsqldb.lib.HashSet;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.analysis.AnalyzerTask;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.math.Discretizer;
import org.matsim.contrib.sna.math.Distribution;
import org.matsim.contrib.sna.math.LinearDiscretizer;

import playground.johannes.socialnetworks.gis.SpatialCostFunction;

import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public class DensityAccessibilityTask extends AnalyzerTask {


	private Set<Point> opportunities;
	
	private SpatialCostFunction costFunction;
	
	public DensityAccessibilityTask(Set<Point> opportunities, SpatialCostFunction costFunction) {
		this.costFunction = costFunction;
		this.opportunities = opportunities;
	}

	/* (non-Javadoc)
	 * @see org.matsim.contrib.sna.graph.analysis.AnalyzerTask#analyze(org.matsim.contrib.sna.graph.Graph, java.util.Map)
	 */
	@Override
	public void analyze(Graph g, Map<String, Double> stats) {
		SpatialGraph graph = (SpatialGraph) g;
		TObjectDoubleHashMap<SpatialVertex> accessMap = new Accessibility().values(graph.getVertices(), costFunction, opportunities);
		double[] values = accessMap.getValues();
		int categories = 2;
		double binsize = (StatUtils.max(values) - StatUtils.min(values)) / (double)categories;
		
		Discretizer discretizer = new LinearDiscretizer(binsize);
		TDoubleObjectHashMap<Set<SpatialVertex>> partitions = new TDoubleObjectHashMap<Set<SpatialVertex>>();
		for(SpatialVertex vertex : graph.getVertices()) {
			double access = accessMap.get(vertex);
			access = discretizer.discretize(access)*binsize;
			Set<SpatialVertex> partition = partitions.get(access);
			if(partition == null) {
				partition = new java.util.HashSet<SpatialVertex>();
				partitions.put(access, partition);
			}
			partition.add(vertex);
		}
		
		TDoubleDoubleHashMap densities = new TDoubleDoubleHashMap();
		
		TDoubleObjectIterator<Set<SpatialVertex>> it = partitions.iterator();
		for(int i = 0; i < partitions.size(); i++) {
			it.advance();
			Set<SpatialVertex> partition = it.value();
			int n = partition.size();
			double M = (n * (n-1));
			double m = 0;
			for(SpatialVertex vertex : partition) {
				for(SpatialVertex neighbor : vertex.getNeighbours()) {
					if(partition.contains(neighbor))
						m++;
				}
			}
			densities.put(it.key(), m/M);
		}
		
		try {
			Distribution.writeHistogram(densities, getOutputDirectory() + "rho_access.txt");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
