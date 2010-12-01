/* *********************************************************************** *
 * project: org.matsim.*
 * GammaSolver.java
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Set;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.DifferentiableUnivariateRealFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.solvers.NewtonSolver;
import org.apache.commons.math.stat.StatUtils;
import org.apache.log4j.Logger;
import org.matsim.contrib.sna.gis.CRSUtils;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.math.Discretizer;
import org.matsim.contrib.sna.math.Distribution;
import org.matsim.contrib.sna.math.LinearDiscretizer;

import playground.johannes.socialnetworks.gis.CartesianDistanceCalculator;
import playground.johannes.socialnetworks.gis.DistanceCalculator;
import playground.johannes.socialnetworks.graph.spatial.io.Population2SpatialGraph;

/**
 * @author illenberger
 *
 */
public class GammaSolver {

	private static final Logger logger = Logger.getLogger(GammaSolver.class);
	
	private Discretizer discretizer = new LinearDiscretizer(1000);
	
	private DistanceCalculator distanceCalculator = new CartesianDistanceCalculator();
	
	public static void main(String args[]) throws FileNotFoundException, IOException {
		Population2SpatialGraph reader = new Population2SpatialGraph(CRSUtils.getCRS(21781));
		SpatialGraph graph = reader.read("/Users/jillenberger/Work/work/socialnets/data/schweiz/complete/plans/plans.0.001.xml");
		
		GammaSolver solver = new GammaSolver();
		TObjectDoubleHashMap<SpatialVertex> gammas = solver.solve((Set<SpatialVertex>) graph.getVertices(), 20, 0.5);
		
		Distribution dist = new Distribution(gammas.getValues());
		logger.info("Median = " + StatUtils.geometricMean(dist.getValues()));
		Distribution.writeHistogram(dist.absoluteDistribution(0.1), "/Users/jillenberger/Work/work/socialnets/mcmc/output/gammas.txt");
	}
	
	public TObjectDoubleHashMap<SpatialVertex> solve(Set<SpatialVertex> vertices, double k, double scaleConstant) {
		NewtonSolver solver = new NewtonSolver();
		solver.setAbsoluteAccuracy(0.01);
		solver.setMaximalIterationCount(20);
		
		Primitive primitive = new Primitive(vertices, k, scaleConstant);
		
		TObjectDoubleHashMap<SpatialVertex> gammas = new TObjectDoubleHashMap<SpatialVertex>();
		
		int i = 0;
		int iters = 0;
		
		for(SpatialVertex v : vertices) {
			primitive.vi = v;
			try {
				double gamma = solver.solve(primitive, -50, 50, 1.5);
				gammas.put(v, gamma);
				i++;
				iters += solver.getIterationCount();
				
//				logger.info(gamma);
				if(i % 100 == 0) {
					logger.info(String.format("Processed %1$s out of %2$s vertices.", i, vertices.size()));
					logger.info(String.format("Average number of iterations: %1$s", iters/(float)i));
				}
			} catch (MaxIterationsExceededException e) {
//				e.printStackTrace();
			} catch (FunctionEvaluationException e) {
//				e.printStackTrace();
			}
		}
		
		return gammas;
	}
	
	private class Primitive implements DifferentiableUnivariateRealFunction {

		private final Set<SpatialVertex> vertices;
		
		private SpatialVertex vi;
		
		private final double k;
		
		private final double scaleConstant;

		private final Derivitive derivative;
		
		public Primitive(Set<SpatialVertex> vertices, double k, double scaleConstant) {
			this.vertices = vertices;
			this.k = k;
			this.scaleConstant = scaleConstant;
			derivative = new Derivitive();
			derivative.primitive = this;
		}
		
		@Override
		public UnivariateRealFunction derivative() {
			return derivative;
		}

		@Override
		public double value(double x) throws FunctionEvaluationException {
			double sum = 0;
			for(SpatialVertex vj : vertices) {
				if (vi != vj) {
					double d = distanceCalculator.distance(vi.getPoint(), vj.getPoint());
					d = discretizer.index(d);
					d = Math.max(1, d);
					sum += Math.pow(d, -x);
				}
			}
			return scaleConstant * sum - k;
		}
		
	}
	
	private class Derivitive implements UnivariateRealFunction {

		private Primitive primitive;
		
		@Override
		public double value(double x) throws FunctionEvaluationException {
			double sum = 0;
			for(SpatialVertex vj : primitive.vertices) {
				if (primitive.vi != vj) {
					double d = distanceCalculator.distance(primitive.vi.getPoint(), vj.getPoint());
					d = discretizer.index(d);
					d = Math.max(1, d);
					sum += -Math.log(d) * Math.pow(d, -x);
				}
			}
			
			return primitive.scaleConstant * sum;
		}
		
	}
}
