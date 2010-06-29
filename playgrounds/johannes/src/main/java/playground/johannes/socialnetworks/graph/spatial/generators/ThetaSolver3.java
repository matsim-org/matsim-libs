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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.DifferentiableUnivariateRealFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.solvers.NewtonSolver;
import org.apache.log4j.Logger;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;

/**
 * @author illenberger
 *
 */
public class ThetaSolver3 {
	
	private static final Logger logger = Logger.getLogger(ThetaSolver3.class);

	private NewtonSolver solver;
	
//	private EdgeCostFunction costFunction;
	
	private double budget;
	
	private double theta_edge;
	
	private static final double THETA_MIN = -200;
	
	private static final double THETA_MAX = 200;
	
	public ThetaSolver3(double budget, double theta_edge) {
//		this.costFunction = costFunction;
		this.budget = budget;
		this.theta_edge = theta_edge;
	}
	
	public TObjectDoubleHashMap<SpatialVertex> solve(Map<SpatialVertex, EdgeCostFunction> functions) {
		Set<SpatialVertex> vertices = new HashSet<SpatialVertex>();
		for(Object v : functions.keySet())
			vertices.add((SpatialVertex)v);
		
		TObjectDoubleHashMap<SpatialVertex> thetas = new TObjectDoubleHashMap<SpatialVertex>();
		
		solver = new NewtonSolver();
		solver.setMaximalIterationCount(2000);
		solver.setAbsoluteAccuracy(0.0001);
		
		int iterCount = 0;
		
//		TObjectDoubleIterator<SpatialVertex> it = functions.iterator();
//		for(int i = 0; i < functions.size(); i++) {
		int i = 0;
		for(Entry<SpatialVertex, EdgeCostFunction> entry : functions.entrySet()) {
//			it.advance();
			double theta = solve(entry.getKey(), entry.getValue(), vertices);
			thetas.put(entry.getKey(), theta);
			iterCount += solver.getIterationCount();
//			System.out.println(String.valueOf(theta));
			i++;
			if(i % 100 == 0) {
				logger.info(String.format("Processed %1$s out of %2$s vertices.", i, functions.size()));
				logger.info(String.format("Average number of iterations: %1$s", iterCount/(float)i));
			}
		}
		
		return thetas;
	}
	
	private double solve(SpatialVertex vertex, EdgeCostFunction function, Set<SpatialVertex> vertices) {
		DifferentiableUnivariateRealFunction func = new Primitive(vertex, function, budget, vertices, theta_edge);
		try {
			return solver.solve(func, THETA_MIN, THETA_MAX);
		} catch (MaxIterationsExceededException e) {
			e.printStackTrace();
		} catch (FunctionEvaluationException e) {
			e.printStackTrace();
		}
		return solver.getResult();
	}
	
	private class Primitive implements DifferentiableUnivariateRealFunction {

		private final Set<SpatialVertex> vertices;
		
		private final SpatialVertex vi;
		
		private final EdgeCostFunction costFunction;
		
		private final double budget;
		
		private final Derivative derivative;
		
		private final double theta_edge;
		
		public Primitive(SpatialVertex vi, EdgeCostFunction costFunction, double budget, Set<SpatialVertex> vertices, double theta_edge) {
			this.vi = vi;
			this.vertices = vertices;
			this.costFunction = costFunction;
			this.budget = budget;
			this.theta_edge = theta_edge;
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
					sum += c * Math.exp(-x * c);
				}
			}
			
			return Math.exp(- theta_edge) * sum - budget;
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
					sum += -c * c * Math.exp(-x * c);
				}
			}
			
			return Math.exp(- theta_edge) * sum;
		}
		
	}
	
//	public static void main(String args[]) throws FileNotFoundException, IOException {
//		Population2SpatialGraph reader = new Population2SpatialGraph(CRSUtils.getCRS(21781));
//		SpatialSparseGraph graph = reader.read(args[0]);
//		
//		GravityEdgeCostFunction func = new GravityEdgeCostFunction(1.6, 1.0, new CartesianDistanceCalculator());
//		ThetaSolver3 solver = new ThetaSolver3(func, 1.0);
//		
//		TObjectDoubleHashMap<SpatialVertex> budgets = new TObjectDoubleHashMap<SpatialVertex>();
//		for(SpatialVertex vertex : graph.getVertices()) {
//			budgets.put(vertex, 25);
//		}
//		
//		TObjectDoubleHashMap<SpatialVertex> thetas = solver.solve(budgets);
//		
//		Distribution distr = new Distribution();
//		TObjectDoubleIterator<SpatialVertex> it = thetas.iterator();
//		for(int i = 0; i < thetas.size(); i++) {
//			it.advance();
//			distr.add(it.value());
//		}
//		
//		double binsize = (distr.max() - distr.min())/100.0;
//		Distribution.writeHistogram(distr.absoluteDistribution(binsize), args[1]);
//	}
}
