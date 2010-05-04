/* *********************************************************************** *
 * project: org.matsim.*
 * ThetaSolver.java
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

import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.DifferentiableUnivariateRealFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.solvers.NewtonSolver;
import org.apache.log4j.Logger;
import org.matsim.contrib.sna.gis.CRSUtils;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.math.Distribution;

import playground.johannes.socialnetworks.graph.spatial.io.Population2SpatialGraph;

/**
 * @author illenberger
 *
 */
public class ThetaSolver {
	
	private static final Logger logger = Logger.getLogger(ThetaSolver.class);

	private NewtonSolver solver;
	
	private EdgeCostFunction costFunction;
	
	private static final double THETA_MIN = -200;
	
	private static final double THETA_MAX = 200;
	
	public ThetaSolver(EdgeCostFunction costFunction) {
		this.costFunction = costFunction;
	}
	
	public TObjectDoubleHashMap<SpatialVertex> solve(TObjectDoubleHashMap<SpatialVertex> budgets) {
		Set<SpatialVertex> vertices = new HashSet<SpatialVertex>();
		for(Object v : budgets.keys())
			vertices.add((SpatialVertex)v);
		
		TObjectDoubleHashMap<SpatialVertex> thetas = new TObjectDoubleHashMap<SpatialVertex>();
		
		solver = new NewtonSolver();
		solver.setMaximalIterationCount(1000);
		solver.setAbsoluteAccuracy(0.001);
		
		int iterCount = 0;
		
		TObjectDoubleIterator<SpatialVertex> it = budgets.iterator();
		for(int i = 0; i < budgets.size(); i++) {
			it.advance();
			double theta = solve(it.key(), it.value(), vertices);
			thetas.put(it.key(), theta);
			iterCount += solver.getIterationCount();
			
			if(i % 100 == 0)
				logger.info(String.format("Processed %1$s out of %2$s vertices.", i, budgets.size()));
		}
		
		return thetas;
	}
	
	private double solve(SpatialVertex vertex, double budget, Set<SpatialVertex> vertices) {
		DifferentiableUnivariateRealFunction func = new Primitive(vertex, costFunction, budget, vertices);
		try {
			return solver.solve(func, THETA_MIN, THETA_MAX, 80.0);
		} catch (MaxIterationsExceededException e) {
			e.printStackTrace();
		} catch (FunctionEvaluationException e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	private class Primitive implements DifferentiableUnivariateRealFunction {

		private final Set<SpatialVertex> vertices;
		
		private final SpatialVertex vi;
		
		private final EdgeCostFunction costFunction;
		
		private final double budget;
		
		private final Derivative derivative;
		
		public Primitive(SpatialVertex vi, EdgeCostFunction costFunction, double budget, Set<SpatialVertex> vertices) {
			this.vi = vi;
			this.vertices = vertices;
			this.costFunction = costFunction;
			this.budget = budget;
			
			derivative = new Derivative(this);
		}
		
		@Override
		public UnivariateRealFunction derivative() {
			return derivative;
		}

		@Override
		public double value(double x) throws FunctionEvaluationException {
			double sum = 0;
			
			for(SpatialVertex vj : vertices) {
				if(vi != vj) {
					double c = costFunction.edgeCost(vi, vj);
					sum += c * Math.exp(-x * c) - budget;
				}
			}
			
			return sum;
		}
		
	}
	
	private class Derivative implements UnivariateRealFunction {

		private final Primitive primitive;
		
		public Derivative(Primitive primitive) {
			this.primitive = primitive;
		}
		
		@Override
		public double value(double x) throws FunctionEvaluationException {
			double sum = 0;
			
			for(SpatialVertex vj : primitive.vertices) {
				if(primitive.vi != vj) {
					double c = primitive.costFunction.edgeCost(primitive.vi, vj);
					sum += c * c * Math.exp(-x * c) - primitive.budget;
				}
			}
			
			return sum;
		}
		
	}
	
	public static void main(String args[]) throws FileNotFoundException, IOException {
		Population2SpatialGraph reader = new Population2SpatialGraph(CRSUtils.getCRS(21781));
		SpatialSparseGraph graph = reader.read(args[0]);
		
		GravityCostFunction func = new GravityCostFunction(-1.6, 1.0);
		ThetaSolver solver = new ThetaSolver(func);
		
		TObjectDoubleHashMap<SpatialVertex> budgets = new TObjectDoubleHashMap<SpatialVertex>();
		for(SpatialVertex vertex : graph.getVertices()) {
			budgets.put(vertex, 25);
		}
		
		TObjectDoubleHashMap<SpatialVertex> thetas = solver.solve(budgets);
		
		Distribution distr = new Distribution();
		TObjectDoubleIterator<SpatialVertex> it = thetas.iterator();
		for(int i = 0; i < thetas.size(); i++) {
			it.advance();
			distr.add(it.value());
		}
		
		double binsize = (distr.max() - distr.min())/100.0;
		Distribution.writeHistogram(distr.absoluteDistribution(binsize), args[1]);
	}
}
