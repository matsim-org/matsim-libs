/* *********************************************************************** *
 * project: org.matsim.*
 * ThetaSolver2.java
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
import java.util.ArrayList;
import java.util.List;

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

import playground.johannes.socialnetworks.gis.CartesianDistanceCalculator;
import playground.johannes.socialnetworks.graph.spatial.io.Population2SpatialGraph;

/**
 * @author illenberger
 *
 */
public class ThetaSolver2 {
	
	private static final Logger logger = Logger.getLogger(ThetaSolver2.class);

	private NewtonSolver solver;
	
	private EdgeCostFunction costFunction;
	
	private playground.johannes.socialnetworks.graph.matrix.EdgeCostFunction cachedCostFunction;
	
	private int nEdges;
	
	private static final double THETA_MIN = -200;
	
	private static final double THETA_MAX = 200;
	
	public ThetaSolver2(EdgeCostFunction costFunction, int nEdges) {
		this.costFunction = costFunction;
		this.nEdges = nEdges;
	}
	
	public TObjectDoubleHashMap<SpatialVertex> solve(TObjectDoubleHashMap<SpatialVertex> budgets) {
		List<SpatialVertex> vertices = new ArrayList<SpatialVertex>(budgets.size());
		for(Object v : budgets.keys())
			vertices.add((SpatialVertex)v);
		
		int N = vertices.size();
		cachedCostFunction = new CachedCostFunction(vertices, costFunction);
		
		TObjectDoubleHashMap<SpatialVertex> thetas = new TObjectDoubleHashMap<SpatialVertex>();
		
		solver = new NewtonSolver();
		solver.setMaximalIterationCount(50);
		solver.setAbsoluteAccuracy(0.00001);
		
		int iterCount = 0;
		
		
		
//		TObjectDoubleIterator<SpatialVertex> it = budgets.iterator();
		for(int i = 0; i < N; i++) {
			logger.info(String.format("Processing %1$s out of %2$s vertices.", i+1, budgets.size()));
//			it.advance();
			double theta = solve(i, budgets.get(vertices.get(i)), N);
			System.out.println("theta:" + theta);
			thetas.put(vertices.get(i), theta);
			iterCount += solver.getIterationCount();
//			System.out.println(String.valueOf(theta));
			if(i % 1 == 0) {
				logger.info(String.format("Processed %1$s out of %2$s vertices.", i+1, budgets.size()));
				logger.info(String.format("Average number of iterations: %1$s", iterCount/(float)(i+1)));
			}
		}
		
		return thetas;
	}
	
	private double solve(int vertex, double budget, int vertices) {
		DifferentiableUnivariateRealFunction func = new Primitive(vertex, budget, vertices, cachedCostFunction, nEdges);
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

		private final int N;
		
		private final int vi;
		
		private final playground.johannes.socialnetworks.graph.matrix.EdgeCostFunction costFunction;
		
		private final double budget;
		
		private final double p;
		
		private final Derivative derivative;
		
		private Primitive(int vi, double budget, int N, playground.johannes.socialnetworks.graph.matrix.EdgeCostFunction costFunction, int nEdges) {
			this.vi = vi;
//			this.vertices = new ArrayList<SpatialVertex>(vertices);
			this.N = N;
			this.costFunction = costFunction;
			this.budget = budget;
			this.p = nEdges/(double)(N*(N-1)/2.0);
			derivative = new Derivative(this);
		}
		
		@Override
		public UnivariateRealFunction derivative() {
			return derivative;
		}

		@Override
		public double value(double x) throws FunctionEvaluationException {
			double sum1 = 0;
			for(int i = 0; i < N; i++) {
				for(int j = i+1 ; j < N; j++) {
					sum1 += Math.exp(-x * costFunction.edgeCost(i, j));
				}
			}
			
			double sum2 = 0;
			for(int j = 0; j < N; j++) {
				if(vi != j) {
					double c = costFunction.edgeCost(vi, j);
					sum2 += c * Math.exp(-x * c);
				}
			}
			
			return p/sum1 * sum2 - budget;
		}
		
	}
	
	private class Derivative implements UnivariateRealFunction {

		private final Primitive primitive;
		
		public Derivative(Primitive primitive) {
			this.primitive = primitive;
		}
		
		@Override
		public double value(double x) throws FunctionEvaluationException {
			double sum1 = 0;
			double sum2 = 0;
			
			for(int i = 0; i < primitive.N; i++) {
				for(int j = i+1 ; j < primitive.N; j++) {
					double c = primitive.costFunction.edgeCost(i, j);
					sum1 += c * Math.exp(-x * c);
					sum2 += Math.exp(-x * c);
				}
			}
			
			double sum3 = 0;
			for(int j = 0; j < primitive.N; j++) {
				if(j != primitive.vi) {
					double c = primitive.costFunction.edgeCost(primitive.vi, j);
					sum3 += -c * c * Math.exp(-x * c);
				}
			}
			
			return sum1/sum2 +sum3;
		}
		
	}
	
	private static class CachedCostFunction implements playground.johannes.socialnetworks.graph.matrix.EdgeCostFunction {

		private double[][] costs;
		
		public CachedCostFunction(List<? extends SpatialVertex> vertices, EdgeCostFunction costFunction) {
			System.out.println("Caching costs...");
			int N = vertices.size();
			costs = new double[N][N];
			for(int i = 0; i < N; i++) {
				for(int j = 0; j < N; j++) {
					if(i != j)
						costs[i][j] = costFunction.edgeCost(vertices.get(i), vertices.get(j));
				}
			}
			System.out.println("Done.");
		}
		
		@Override
		public double edgeCost(int i, int j) {
			return costs[i][j];
		}
		
	}
	
	public static void main(String args[]) throws FileNotFoundException, IOException {
		Population2SpatialGraph reader = new Population2SpatialGraph(CRSUtils.getCRS(21781));
		SpatialSparseGraph graph = reader.read(args[0]);
		
		GravityEdgeCostFunction func = new GravityEdgeCostFunction(1.6, 1.0, new CartesianDistanceCalculator());
		int k = 15;
		int m = (int) (0.5 * k * graph.getVertices().size());
		ThetaSolver2 solver = new ThetaSolver2(func, m);
		
		TObjectDoubleHashMap<SpatialVertex> budgets = new TObjectDoubleHashMap<SpatialVertex>();
		for(SpatialVertex vertex : graph.getVertices()) {
			budgets.put(vertex, 60);
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
