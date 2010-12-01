/* *********************************************************************** *
 * project: org.matsim.*
 * ErgmEdgeCost.java
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
package playground.johannes.socialnetworks.graph.spatial.generators;

import gnu.trove.TDoubleObjectHashMap;
import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.StatUtils;
import org.apache.log4j.Logger;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.matrix.AdjacencyMatrix;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.math.Discretizer;
import org.matsim.contrib.sna.math.Distribution;
import org.matsim.contrib.sna.math.LinearDiscretizer;

import playground.johannes.socialnetworks.gis.BeelineCostFunction;
import playground.johannes.socialnetworks.gis.CartesianDistanceCalculator;
import playground.johannes.socialnetworks.gis.DistanceCalculator;
import playground.johannes.socialnetworks.graph.mcmc.GraphProbability;
import playground.johannes.socialnetworks.graph.spatial.analysis.Accessibility;

import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public class ErgmEdgeCost2 implements GraphProbability {

	private static final Logger logger = Logger.getLogger(ErgmEdgeCost2.class);
	
	private double[] thetas;
	
	private List<EdgeCostFunction> functionList;
	
	public ErgmEdgeCost2(AdjacencyMatrix<? extends SpatialVertex> y, double gammaMean, double budget, String thetaFile, double theta_edge) {
		Set<SpatialVertex> vertices = new HashSet<SpatialVertex>();
		Set<Point> opportunities = new HashSet<Point>();
		
		for(int i = 0; i < y.getVertexCount(); i++) {
			SpatialVertex v = y.getVertex(i); 
			vertices.add(v);
			opportunities.add(v.getPoint());
		}

		logger.info("Calculating accessability...");
		Accessibility access = new Accessibility();
		BeelineCostFunction cFunc = new BeelineCostFunction();
		cFunc.setDistanceCalculator(new CartesianDistanceCalculator());
		TObjectDoubleHashMap<SpatialVertex> values = access.values(vertices, cFunc, opportunities);
		logger.info("Done.");
		
		double values2[] = values.getValues();
		double gammaMax = 2.0;
		double gammaMin = 1.0;
		
		double aMin = StatUtils.min(values2);
		double aMax = StatUtils.max(values2);
		double delta = (gammaMin - gammaMax) / (aMax - aMin);
		double b = gammaMax - (delta * aMin);
		
		TDoubleObjectHashMap<EdgeCostFunction> tmpFunctions = new TDoubleObjectHashMap<EdgeCostFunction>();
		Map<SpatialVertex, EdgeCostFunction> functions = new HashMap<SpatialVertex, EdgeCostFunction>();
		TObjectDoubleIterator<SpatialVertex> it = values.iterator();
		Discretizer discretizer = new LinearDiscretizer(0.1);
		DistanceCalculator calculator = new CartesianDistanceCalculator();
		for(int i = 0; i < values.size(); i++) {
			it.advance();
			double gamma = discretizer.discretize(delta * it.value() + b) * 0.1;
			EdgeCostFunction func = tmpFunctions.get(gamma);
			if(func == null) {
				func = new GravityEdgeCostFunction(gamma, 1.0, calculator);
				tmpFunctions.put(gamma, func);
			}
			functions.put(it.key(), func);
		}
		
		ThetaSolver3 solver = new ThetaSolver3(budget, theta_edge);
		TObjectDoubleHashMap<SpatialVertex> tmpThetas = solver.solve(functions);
		
//		ThetaApproximator approximator = new ThetaApproximator();
//		
//		TObjectDoubleHashMap<SpatialVertex> tmpThetas = approximator.approximate(vertices, budget, costFunction, theta_edge);
		functionList = new ArrayList<EdgeCostFunction>(functions.size());
		for(int i = 0; i < y.getVertexCount(); i++) {
			functionList.add(functions.get(y.getVertex(i)));
		}
		
		thetas = new double[y.getVertexCount()];
		Distribution distr = new Distribution();
		for(int i = 0; i < thetas.length; i++) {
			thetas[i] = tmpThetas.get(y.getVertex(i));
			distr.add(thetas[i]);
		}
		
		if(thetaFile != null) {
			double binsize = (distr.max() - distr.min())/100.0;
			try {
				Distribution.writeHistogram(distr.absoluteDistribution(binsize), thetaFile);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <V extends Vertex> double difference(AdjacencyMatrix<V> y, int i, int j, boolean yIj) {
		SpatialVertex vi = ((AdjacencyMatrix<SpatialVertex>)y).getVertex(i);
		SpatialVertex vj = ((AdjacencyMatrix<SpatialVertex>)y).getVertex(j);
		
		return Math.exp(thetas[i] * functionList.get(i).edgeCost(vi, vj));
	}

}
