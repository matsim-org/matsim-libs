/* *********************************************************************** *
 * project: org.matsim.*
 * ORCASolver.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.gregor.sim2d_v4.simulation.physics.orca;

import java.util.ArrayList;
import java.util.List;

import playground.gregor.sim2d_v4.cgal.CGAL;

/**
 * linear problem solver as proposed by van den Berg et al (2009), Reciprocal n-body collision avoidance. In: Inter. Symp. on Robotics Research.
 * the general work flow is similar to linear optimization in de Berg et al (2000), Computational Geometry: Algorithms and Applications. Chapter 4.4,  Springer
 * @author laemmel
 *
 */
public class ORCASolver {
	private static final double ALMOST_ONE = 0.9999;


	public void run(final List<ORCALine> constraints, final double [] vPref, final double maxDeltaSpeed, final double [] v) {

		double vxPref = vPref[0];
		double vyPref = vPref[1];


		int handled = solveProblem(constraints, vPref, vxPref, vyPref, maxDeltaSpeed,v);
		if (handled < constraints.size()) {
			pushORCALinesOutward(constraints, handled, maxDeltaSpeed, vPref,v);
			//			vPref[0] = 100;
			//			vPref[1] = 0;
		}

	}

	private void pushORCALinesOutward(List<ORCALine> constraints, int failedLine, double maxDeltaSpeed, final double [] ret, final double [] v) {

		List<ORCALine> obst = new ArrayList<ORCALine>();
		for (ORCALine oRCALine : constraints) {
			if (oRCALine instanceof ORCALineEnvironment) {
				obst.add(oRCALine);
			}
		}

		double distance = 0.f;
		for (int i = failedLine; i < constraints.size(); i++) {
			ORCALine ci = constraints.get(i);
			if (ci instanceof ORCALineEnvironment) {
				continue;
			}
			double tmp = CGAL.det(ci.getDirectionX(), ci.getDirectionY(), ci.getPointX()-ret[0], ci.getPointY()-ret[1]);
			if (tmp > distance) {
				List<ORCALine> projLines = new ArrayList<ORCALine>(obst);
				for (int j = 0; j < i; j++) {
					if (constraints.get(j) instanceof ORCALineEnvironment) {
						continue;
					}
					ORCALine cj = constraints.get(j);
					ORCALineUniversal line = new ORCALineUniversal();
					double determinant = CGAL.det(ci.getDirectionX(),ci.getDirectionY(),cj.getDirectionX(),cj.getDirectionY());
					
					double cos = CGAL.dot(ci.getDirectionX(), ci.getDirectionY(), cj.getDirectionX(), cj.getDirectionY()); // /|ci|*|cj| (= 1)
					if (Math.abs(cos) >= ALMOST_ONE) { //parallel
						if ((ci.getDirectionX()*cj.getDirectionX()+ci.getDirectionY()*cj.getDirectionY()) > 0.f) {
							continue;
						} else {
							line.setPointX(.5 * (ci.getPointX()+cj.getPointX()));
							line.setPointY(.5 * (ci.getPointY()+cj.getPointY()));
						}
					} else {
						double detTmp = CGAL.det(cj.getDirectionX(), cj.getDirectionY(), ci.getPointX()-cj.getPointX(),ci.getPointY()-cj.getPointY());
						double quotient = detTmp/determinant;
						line.setPointX(ci.getPointX()+ quotient * ci.getDirectionX());
						line.setPointY(ci.getPointY()+ quotient * ci.getDirectionY());
					}
					double dxLine = cj.getDirectionX()-ci.getDirectionX();
					double dyLine = cj.getDirectionY()-ci.getDirectionY();
					double length = Math.sqrt(dxLine*dxLine+dyLine*dyLine);
					dxLine /= length;
					dyLine /= length;
					line.setDirectionX(dxLine);
					line.setDirectionY(dyLine);
					projLines.add(line);
				}
				final double[] tmpRet = ret;
				if (solveProblem(projLines, ret, -ci.getDirectionY(), ci.getDirectionX(),maxDeltaSpeed,v) < projLines.size()) {
					ret[0] = tmpRet[0];
					ret[1] = tmpRet[1];
				}
				distance = CGAL.det(ci.getDirectionX(), ci.getDirectionY(), ci.getPointX()-ret[0], ci.getPointY()-ret[1]);
			}
		}
	}

	//
	//	        distance = det(lines[i].direction, lines[i].point - result);
	//	      }
	//	    }
	//	  }

	private int solveProblem(List<ORCALine> constraints, double[] ret,
			double vxPref, double vyPref, double maxDeltaSpeed, final double [] v) {

		for (int i = 0; i < constraints.size(); i++) {
			//test whether constraints are not satisfied by current velocity


			if (!constraints.get(i).solutionSatisfyConstraint(ret)) {
				//if not try to find a new optimal solution that satisfies constraint i (and all previous)
				final double [] tmp = ret.clone();
				boolean found = handleConstraint(constraints,ret,vxPref,vyPref,maxDeltaSpeed,i,v);
				if(!found) { //linear program is infeasible
					ret[0] = tmp[0];
					ret[1] = tmp[1];
					return i;
				}
			}
			//			else if (this.debugger != null) {
			//				constraints.get(i).debug(this.debugger, 0, 255, 0);
			//				this.debugger.addAll();
			//			}
		
		}

		return constraints.size();
	}

	private boolean handleConstraint(final List<ORCALine> constraints, final double[] ret,
			final double vxPref, final double vyPref, final double maxDeltaSpeed, final int idx, final double [] v) {
		ORCALine constraint = constraints.get(idx);

		//1. check whether constraint contradicts max speed constraint
		//TODO replace max speed by max speed delta
		//1.a calculate distance 
		double dist = CGAL.signDistPointLine(v[0],v[1], constraint.getPointX(), constraint.getPointY(), constraint.getDirectionX(), constraint.getDirectionY());
		if (dist > maxDeltaSpeed) {
			//constraint contradicts maxSpeed circle 
			return false;
		}

		//2. calculate intersections of constraint with max speed circle, since constraint was not fulfilled but max speed circle does not contradict the constraint, 
		//there must be two intersection points

		double b = CGAL.normVectorCoefOfPerpendicularProjection(v[0], v[1], constraint.getPointX(), constraint.getPointY(), constraint.getDirectionX(), constraint.getDirectionY());//center of the line segment inside the circle
		double rangeHalf = Math.sqrt(maxDeltaSpeed*maxDeltaSpeed-dist*dist);//distance from b to the circle boundary (on either side)

		double leftBound = b-rangeHalf; //left intersection
		double rightBound = b+rangeHalf;//right intersection

		for (int i = 0; i < idx; i++) {
			ORCALine tmp = constraints.get(i);
			double cos = CGAL.dot(constraint.getDirectionX(), constraint.getDirectionY(), tmp.getDirectionX(), tmp.getDirectionY()); // /|constraint|*|tmp| (= 1)
			if (Math.abs(cos) >= ALMOST_ONE) { //parallel
				if (cos < 0) { //opposite direction
					if (constraint.solutionSatisfyConstraint(new double [] {tmp.getPointX(),tmp.getPointY()})) {
						continue;
					} else {
						return false;
					}
				} else { //same direction
					if (tmp.solutionSatisfyConstraint(new double []{constraint.getPointX(),constraint.getPointY()})) {
						continue;
					} else {
						return false;
					}
				}
			}
			//line intersection (see, e.g. http://en.wikipedia.org/wiki/Line-line_intersection or http://en.wikipedia.org/wiki/Cramer%27s_rule) 
			final double denominator = CGAL.det(constraint.getDirectionX(), constraint.getDirectionY(),tmp.getDirectionX(),tmp.getDirectionY());
			final double numerator = CGAL.det(tmp.getDirectionX(), tmp.getDirectionY(), constraint.getPointX()-tmp.getPointX(), constraint.getPointY()-tmp.getPointY());
			double c = numerator/denominator;

			if (denominator > 0) { //right boundary constrain?
				rightBound = Math.min(rightBound, c);
			} else { //left boundary constraint?
				leftBound = Math.max(leftBound, c);
			}

			if (leftBound > rightBound) {//contradiction
				return false;
			}

		}
		
		double tmp = CGAL.normVectorCoefOfPerpendicularProjection(vxPref, vyPref, constraint.getPointX(), constraint.getPointY(), constraint.getDirectionX(), constraint.getDirectionY());//orthogonal projection of preferred velocity on the unsatisfied constraint

		if (tmp < leftBound) {
			ret[0] = constraint.getPointX() + leftBound * constraint.getDirectionX();
			ret[1] = constraint.getPointY() + leftBound * constraint.getDirectionY();
		} else if (tmp > rightBound){
			ret[0] = constraint.getPointX() + rightBound * constraint.getDirectionX();
			ret[1] = constraint.getPointY() + rightBound * constraint.getDirectionY();
		} else {
			ret[0] = constraint.getPointX() + tmp * constraint.getDirectionX();
			ret[1] = constraint.getPointY() + tmp * constraint.getDirectionY();
		}
		  
		return true;
	}


}
