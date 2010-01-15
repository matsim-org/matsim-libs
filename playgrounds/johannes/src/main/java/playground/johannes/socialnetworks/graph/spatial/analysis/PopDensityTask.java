/* *********************************************************************** *
 * project: org.matsim.*
 * PopDensityTask.java
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

import org.matsim.contrib.sna.gis.Zone;
import org.matsim.contrib.sna.gis.ZoneLayer;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;

import playground.johannes.socialnetworks.graph.analysis.AbstractGraphAnalyzerTask;
import playground.johannes.socialnetworks.graph.analysis.Degree;
import playground.johannes.socialnetworks.graph.analysis.DegreeTask;
import playground.johannes.socialnetworks.statistics.Correlations;

/**
 * @author illenberger
 *
 */
public class PopDensityTask extends AbstractGraphAnalyzerTask {

	private ZoneLayer zones;
	
	/**
	 * @param output
	 */
	public PopDensityTask(String output) {
		super(output);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see playground.johannes.socialnetworks.graph.analysis.GraphAnalyzerTask#analyze(org.matsim.contrib.sna.graph.Graph, java.util.Map, java.util.Map)
	 */
	@Override
	public void analyze(Graph graph, Map<String, Object> analyzers,	Map<String, Double> stats) {
		if(getOutputDirectory() != null) {
			Set<? extends SpatialVertex> vertices = (Set<? extends SpatialVertex>) graph.getVertices();
			TObjectDoubleHashMap<SpatialVertex> densityValues = new TObjectDoubleHashMap<SpatialVertex>();

			for (SpatialVertex v : vertices) {
				Zone zone = zones.getZone(v.getPoint());
				if (zone != null) {
					double rho = zone.getInhabitants()
							/ zone.getGeometry().getArea() * 1000 * 1000;
					densityValues.put(v, rho);
				}
			}
		
			Object obj;
			
			Degree degree;
			obj = analyzers.get(DegreeTask.class.getCanonicalName());
			if(obj == null)
				degree = new Degree();
			else {
				degree = (Degree)obj;
			}
			try {
				Correlations.writeToFile(getCorrelation(degree.values(vertices), densityValues), "", "", "");
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private TDoubleDoubleHashMap getCorrelation(TObjectDoubleHashMap<? extends Vertex> vValues, TObjectDoubleHashMap<? extends Vertex> dValues) {
		return null;
	}
}
