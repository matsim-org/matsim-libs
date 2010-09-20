/* *********************************************************************** *
 * project: org.matsim.*
 * SimplexOptimization.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.andreas.optimization;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 *  Implementation of the Simplex Optimization Algorithm. Works in N-dimensional
 *  parameter spaces with a convex objective function.
 *
 *  http://mathworld.wolfram.com/SimplexMethod.html
 */
public final class SimplexOptimization {

	private static final double EPSILON = 0.1;

	public static ParamPoint getBestParams(final Objective objective) {
		// prepare simplex algorithm
		int dimension = objective.getNewParamPoint().getDimension();
		ArrayList<ParamPoint> points = new ArrayList<ParamPoint>(dimension + 1);

		for (int i = 0; i <= dimension; i++) {	// create one point more than #dimensions
			ParamPoint p = objective.getInitialParamPoint(i);
			points.add(p);
		}


		// run simplex algorithm
		Comparator<ParamPoint> comparator = new ResponseComparator(objective);

		double improvement = EPSILON + 1;	// just something bigger then EPSILON

		// START ITERATION

		// sort points by ascending response (high response = bad response)
		Collections.sort(points, comparator);
		// the best ParamPoint is now at the beginning of the list, the worst at the end

		ParamPoint best = points.get(0);
		double rB = objective.getResponse(best);
		ParamPoint worst = points.get(points.size()-1);
		double rW = objective.getResponse(worst);

		int cnt = 0;
		while (improvement > EPSILON || cnt < 100) {
			cnt++;
			// calc centroid
			double factor = 1.0 / dimension;
			ParamPoint centroid = ParamPoint.multiply(points.get(0), factor);
			for (int i = 1; i < dimension; i++) {
				centroid = ParamPoint.add(centroid, ParamPoint.multiply(points.get(i), factor));
			}

			ParamPoint diff = ParamPoint.subtract(centroid, worst);

			ParamPoint r = ParamPoint.add(centroid, diff);	// reflected point R
			if (objective.isValidParamPoint(r)) {
				double rR = objective.getResponse(r);
				if (rR < rW && rR > rB) {		// rR is better than rW, but worse than rB
					points.remove(worst);
					points.add(r);
				} else if (rR <= rB) {		// rR is at least as good as rB
					// if the direction we take seems okay, we go a step further in the same direction and check again
					// this can be seen as an optimization to the algorithm to find the past param-values faster
					ParamPoint e = ParamPoint.add(centroid, ParamPoint.multiply(diff, 2));	// extended point E
					if (objective.isValidParamPoint(e)) {
						double rE = objective.getResponse(e);
						if (rE < rR) {		// rE is even better than rR
							points.remove(worst);
							points.add(e);
						} else {
							points.remove(worst);
							points.add(r);
						}
					} else {
						// e is not valid --> use r
						points.remove(worst);
						points.add(r);
					}
				} else {
					// rR is worse than rW, do not use it
					ParamPoint c = ParamPoint.add(worst, ParamPoint.multiply(diff, 0.5));	// contracted point C
					points.remove(worst);
					points.add(c);
				}
			} else {
				// r is not valid
				ParamPoint c = ParamPoint.add(worst, ParamPoint.multiply(diff, 0.5));	// contracted point C
				points.remove(worst);
				points.add(c);
			}

			// sort points by descending response
			Collections.sort(points, comparator);
			// the best ParamPoint is now at the beginning of the list, the worst at the end

			best = points.get(0);
			rB = objective.getResponse(best);
			worst = points.get(points.size()-1);
			double rWnew = objective.getResponse(worst);
			improvement = rW - rWnew;
//			if (improvement < 0) {
//				System.out.println("Neg. Imrovements!");
//			}
			rW = rWnew;
//			System.out.println("Iteration: " + cnt + ": Improvement: " + improvement);
		}

		// store the result
		// `best' contains now the ParamPoint best matching the objective function / activity space calculation function;

		return best;
	}


	/**
	 * A Comparator based on response-values of an objective function. Note that lower responses
	 * are considered better, so after sorting an array the best parameter point is at index 0.
	 */
	/*package*/ static class ResponseComparator implements Comparator<ParamPoint>, Serializable {

		private static final long serialVersionUID = 1L;
		private final Objective objective;

		protected ResponseComparator(final Objective objective) {
			this.objective = objective;
		}

		public int compare(final ParamPoint p1, final ParamPoint p2) {
			double r1 = this.objective.getResponse(p1);
			double r2 = this.objective.getResponse(p2);
			if (r1 < r2) return -1;
			if (r1 > r2) return +1;
			return 0;
		}
	}

}
