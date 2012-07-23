/* *********************************************************************** *
 * project: org.matsim.*
 * LinearProblemSolver.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.gregor.sim2d_v3.simulation.floor.forces.deliberative;

import java.util.List;

import playground.gregor.sim2d_v3.simulation.floor.forces.deliberative.velocityobstacle.Algorithms;

public class ORCAProblemSolver {

	
	private static final double epsilon = 0.00001;

	public double[] run(List<Constraint> constraints, double optX, double optY) {
		final double [] ret = {optX, optY};
//		Collections.shuffle(constraints,MatsimRandom.getRandom());
		for (int i = 1; i < constraints.size(); i++) {
			Constraint hi = constraints.get(i);
			if (!hi.solutionSatisfyConstraint(ret[0],ret[1])) {
				findNewOptSolution(ret,constraints,i-1,hi);
			}
		}
		
		
		
		return ret;
		
	}

	private void findNewOptSolution(double[] ret, List<Constraint> constraints,	int idx, Constraint hi) {
		double rightX = hi.getP1x();//todo make sure p1x is at least max vx
		double rightY = hi.getP1y();//todo make sure p1y is at least max vy
		double leftX = hi.getP0x();//todo make sure p0x is at least - max vx 
		double leftY = hi.getP0y();//todo make sure p0y is at least - max vy
		double [] range = {leftX, leftY,rightX, rightY};
		for (int i = 0; i <= idx; i++) {
			updateRange(range,constraints.get(i),hi);
		}

		//TODO update ret; needs vopt;
	}
	
	private void updateRange(double[] range, Constraint constraint, Constraint hi) {
		if (hi instanceof CircularConstraint) {
			updateRangeCircular(range, constraint, (CircularConstraint) hi);
		} else {
			updateRangeLine(range,constraint,hi);
		}
		

		
	}

	private void updateRangeLine(double[] range, Constraint c,
			Constraint hi) {
		final double [] sigma = new double [2];
		
		computeLineIntersection(hi.getP0x(), hi.getP0y(), hi.getP1x(), hi.getP1y(), c.getP0x(), c.getP0y(), c.getP1x(), c.getP1y(), sigma);
		updateConstraint(c,sigma);
		updateConstraint(hi,sigma);
		if (Algorithms.isLeftOfLine(c.getP0x(), c.getP0y(), hi.getP0x(), hi.getP0y(), hi.getP1x(), hi.getP1y()) > 0) { //left bound update
			double sqrRange = Math.pow(range[0]-range[2], 2) + Math.pow(range[1]-range[3], 2); 
			double propSqrRange = Math.pow(sigma[0]-range[2], 2) + Math.pow(sigma[1]-range[3], 2);
			if (propSqrRange < sqrRange) {
				range[0] = sigma[0];
				range[1] = sigma[1];
			}
		} else {//right bound update
			double sqrRange = Math.pow(range[0]-range[2], 2) + Math.pow(range[1]-range[3], 2); 
			double propSqrRange = Math.pow(sigma[0]-range[0], 2) + Math.pow(sigma[1]-range[1], 2);
			if (propSqrRange < sqrRange) {
				range[2] = sigma[0];
				range[3] = sigma[1];
			}			
		}
		
	}

	private void updateConstraint(Constraint c, double[] sigma) {
		
		double p0SqrDist = (Math.pow(c.getP0x()-sigma[0], 2) +Math.pow(c.getP0y()-sigma[1], 2));
		double p1SqrDist = (Math.pow(c.getP1x()-sigma[0], 2) +Math.pow(c.getP1y()-sigma[1], 2));
		double p0p1SqrDist =(Math.pow(c.getP0x()-c.getP0y(), 2) +Math.pow(c.getP0y()-c.getP1y(), 2));
		
		
		if (!(p0p1SqrDist > p0SqrDist) || !(p0p1SqrDist > p1SqrDist)) {
			
			//mv p1
			if (p0SqrDist > p1SqrDist) {
				double x = c.getP1x() + sigma[0] - c.getP0x();
				double y = c.getP1y() + sigma[1] - c.getP0y();
				c.setP1x(x);
				c.setP1y(y);
			} else {
				//mv p0
				double x = c.getP0x() + sigma[0] - c.getP1x();
				double y = c.getP0y() + sigma[1] - c.getP1y();
				c.setP0x(x);
				c.setP0y(y);
			}
			
		}
		
	}

	private void updateRangeCircular(double[] range, Constraint constraint,
			CircularConstraint hi) {
		//TODO
		throw new RuntimeException("not yet implemented!");
		
	}

	private boolean computeLineIntersection(double a0x, double a0y, double a1x, double a1y, double b0x, double b0y, double b1x, double b1y, double [] intersect) {

		double a = (b1x - b0x) * (a0y - b0y) - (b1y - b0y) * (a0x - b0x);
		double b = (a1x - a0x) * (a0y - b0y) - (a1y - a0y) * (a0x - b0x);
		double denom = (b1y - b0y) * (a1x - a0x) - (b1x - b0x) * (a1y - a0y);

		//conincident
		if (Math.abs(a) < epsilon  && Math.abs(b) < epsilon && Math.abs(denom) < epsilon) {
			intersect[0] = (a0x+a1x) /2;
			intersect[1] = (a0y+a1y) /2;
			return true;
		}

		//parallel
		if (Math.abs(denom) < epsilon) {
			return false;
		}

		double ua = a / denom;
		double ub = b / denom;

		if (ua < 0 || ua > 1 || ub < 0 || ub > 1) {
			return false;
		}

		double x = a0x + ua * (a1x - a0x);
		double y = a0y + ua * (a1y - a0y);
		intersect[0] = x;
		intersect[1] = y;

		return true;
	}

	
}
