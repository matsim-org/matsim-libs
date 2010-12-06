/* *********************************************************************** *
 * project: org.matsim.*
 * DegreeAccessabilityTask.java
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

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.StatUtils;
import org.apache.log4j.Logger;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.Degree;
import org.matsim.contrib.sna.graph.analysis.ModuleAnalyzerTask;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.math.Discretizer;
import org.matsim.contrib.sna.math.FixedSampleSizeDiscretizer;

import playground.johannes.socialnetworks.gis.SpatialCostFunction;
import playground.johannes.socialnetworks.statistics.Correlations;

import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public class DegreeAccessabilityTask extends ModuleAnalyzerTask<Degree> {
	
	private static final Logger logger = Logger.getLogger(DegreeAccessabilityTask.class);

	private Set<Point> opportunities;
	
	private SpatialCostFunction costFunction;
	
	public DegreeAccessabilityTask(Set<Point> opportunities, SpatialCostFunction costFunction) {
		setModule(Degree.getInstance());
		this.costFunction = costFunction;
		this.opportunities = opportunities;
	}
	
	@Override
	public void analyze(Graph graph, Map<String, Double> stats) {
		if(getOutputDirectory() != null) {
			TObjectDoubleHashMap<Vertex> kMap = module.values(graph.getVertices());
			TObjectDoubleHashMap<SpatialVertex> accessMap = new Accessibility().values((Set<? extends SpatialVertex>) graph.getVertices(), costFunction, opportunities);
			
			double[] accessValues = new double[kMap.size()];
			double[] kValues = new double[kMap.size()];
			
			TObjectDoubleIterator<Vertex> it = kMap.iterator();
			for(int i = 0; i < kMap.size(); i++) {
				it.advance();
				accessValues[i] = accessMap.get((SpatialVertex) it.key());
				kValues[i] = it.value();
			}
			
			try{
				double binsize = (StatUtils.max(accessValues) - StatUtils.min(accessValues))/20.0;
				Discretizer disc = FixedSampleSizeDiscretizer.create(accessValues, 20);
				Correlations.writeToFile(Correlations.correlationMean(accessValues, kValues, disc), String.format("%1$s/k_access.txt", getOutputDirectory()), "access", "k_mean");
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			logger.warn("No output directory specified!");
		}
		
	}

}
