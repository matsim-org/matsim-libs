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

import java.util.List;

import playground.gregor.sim2d_v4.cgal.CGAL;
import playground.gregor.sim2d_v4.debugger.VisDebugger;

/**
 * linear problem solver as proposed by van den Berg et al (2009), Reciprocal n-body collision avoidance. In: Inter. Symp. on Robotics Research.
 * the general work flow is similar to linear optimization in de Berg et al (2000), Computational Geometry: Algorithms and Applications. Chapter 4.4,  Springer
 * @author laemmel
 *
 */
public class ORCASolver {
	private static final float ALMOST_ONE = 0.9999f;
	private static final float[] center = {0f,0f};
	private VisDebugger debugger;
	
	
	
	public void run(final List<ORCALine> constraints, final float [] vPref, final float maxSpeed) {
		
		float vxPref = vPref[0];
		float vyPref = vPref[1];
		
		
		int handled = solveProblem(constraints, vPref, vxPref, vyPref, maxSpeed);
		
	}

	private int solveProblem(List<ORCALine> constraints, float[] ret,
			float vxPref, float vyPref, float maxSpeed) {
		
		for (int i = 0; i < constraints.size(); i++) {
			//test whether constraints are not satisfied by current velocity
			if (!constraints.get(i).solutionSatisfyConstraint(ret)) {
				//if not try to find a new optimal solution that satisfies constraint i (and all previous)
				boolean found = handleConstraint(constraints,ret,vxPref,vyPref,maxSpeed,i);
				if(!found) { //linear program is infeasible
					return i;
				}
			} else if (this.debugger != null) {
				constraints.get(i).debug(this.debugger, 0, 255, 0);
				this.debugger.addAll();
			}
		}
		
		return constraints.size();
	}

	private boolean handleConstraint(final List<ORCALine> constraints, final float[] ret,
			final float vxPref, final float vyPref, final float maxSpeed, final int idx) {
		ORCALine constraint = constraints.get(idx);

		if (this.debugger != null) {
			this.debugger.addCircle(0, 0, 2*maxSpeed, 0, 0, 0, 255, 0, false);
			constraint.debug(this.debugger, 255, 0, 0);
			this.debugger.addAll();
		}
		
		
		float dist = CGAL.signDistPointLine(0.f, 0.f, constraint.getPointX(), constraint.getPointY(), constraint.getDirectionX(), constraint.getDirectionY());
		
		//1. check whether constraint violates maxSpeed constraint
		//1a check whether circle center does not satisfy constraint 
		if (!constraint.solutionSatisfyConstraint(center)){
			//1b center does not satisfy constraint so we have to check whether the distance of the center to the constraint is smaller then maxSpeed
			if (Math.abs(dist) > maxSpeed) { //constraint can not be satisfied
				return false;
			}
		}
		
		//it seems constraint can be handled
		float r = CGAL.normVectorCoefOfPerpendicularProjection(0.f, 0.f, constraint.getPointX(), constraint.getPointY(), constraint.getDirectionX(), constraint.getDirectionY());
		
		//2. limit range by maxSpeed circle (intersection points of constraint an circle)
		float rangeHalfe = (float)Math.sqrt(maxSpeed*maxSpeed-dist*dist); //TODO is there a faster approximation of Math.sqrt?
		float leftBound = r-rangeHalfe;
		float rightBound = r+rangeHalfe;
		
//		if (this.debugger != null) {
//			float x0 = leftBound * constraint.getDirectionX() + constraint.getPointX();
//			float y0 = leftBound * constraint.getDirectionY() + constraint.getPointY();
//			float x1 = rightBound * constraint.getDirectionX() + constraint.getPointX();
//			float y1 = rightBound * constraint.getDirectionY() + constraint.getPointY();
//			this.debugger.addLine(x0, y0, x1, y1, 0, 0, 255, 255, 0);
//			this.debugger.addAll();
//			System.out.println(": ");
//		}
		
		//3. now we iterate over all previous handled constraints and check if they are still satisfied, the range has to be further reduced, 
		//or one of the previous constraints contradicts the current one. In this case the linear program is infeasible
		for (int i = 0; i < idx; i++) {
			ORCALine tmp = constraints.get(i);
//			if (this.debugger !=null) {
//				tmp.debug(this.debugger, 192, 192, 192);
//				this.debugger.addAll();
//				System.out.println(": ");
//			}
			//3a. check whether tmp and constraint are collinear
			float cosDirection = CGAL.dot(constraint.getDirectionX(), constraint.getDirectionY(), tmp.getDirectionX(), tmp.getDirectionY());
			if (Math.abs(cosDirection) >= ALMOST_ONE) {
				if (cosDirection < 0) { //opposite direction
					if (constraint.solutionSatisfyConstraint(new float[]{tmp.getPointX(),tmp.getPointY()})){
						continue;
					} else {
						//contradiction --> infeasible linear program
						return false;
					}
				} else { //same direction
					if (tmp.solutionSatisfyConstraint(new float[]{constraint.getPointX(),constraint.getPointY()})) {
						continue;
					} else {
						//contradiction --> infeasible linear program
						return false;
					}
				}
			}
			
			//3b. compute intersection point of constraint and tmp
			float a11 = constraint.getDirectionX();
			float a21 = constraint.getDirectionY();
			float a12 = tmp.getDirectionX();
			float a22 = tmp.getDirectionY();
			float b1 = tmp.getPointX() - constraint.getPointX();
			float b2 = tmp.getPointY() - constraint.getPointY();
			float d = a11*a22 - a12*a21;
			float n = b1*a22 - b2*a12;
			float intersect = n/d;
			
			if (d >= 0) { //right boundary?
				if ((rightBound) > (intersect)) {
					rightBound = intersect;
				}
			} else {
				if (leftBound < (intersect)) {
					leftBound = intersect;
				}
			}
//			if (this.debugger != null) {
//				float x0 = leftBound * constraint.getDirectionX() + constraint.getPointX();
//				float y0 = leftBound * constraint.getDirectionY() + constraint.getPointY();
//				float x1 = rightBound * constraint.getDirectionX() + constraint.getPointX();
//				float y1 = rightBound * constraint.getDirectionY() + constraint.getPointY();
//				this.debugger.addLine(x0, y0, x1, y1, 0, 255/(i+1), 255-(255/(i+1)), 255, 0);
//				this.debugger.addAll();
//				System.out.println(": ");
//			}
			
			
		}
		
		//4. orthogonal projection of vpref on c
		float coef = CGAL.normVectorCoefOfPerpendicularProjection(vxPref, vyPref, constraint.getPointX(), constraint.getPointY(), constraint.getDirectionX(), constraint.getDirectionY());
		if (coef > rightBound) {
			ret[0] = constraint.getPointX() + rightBound*constraint.getDirectionX();
			ret[1] = constraint.getPointY() + rightBound*constraint.getDirectionY();
		} else if (coef < leftBound) {
			ret[0] = constraint.getPointX() + leftBound*constraint.getDirectionX();
			ret[1] = constraint.getPointY() + leftBound*constraint.getDirectionY();
		} else {
			ret[0] = constraint.getPointX() + coef*constraint.getDirectionX();
			ret[1] = constraint.getPointY() + coef*constraint.getDirectionY();
		}
		
		
		if (this.debugger != null) {
			this.debugger.addLine(0, 0, ret[0], ret[1], 0, 0, 0, 255, 0);
			this.debugger.addAll();
			System.out.println(": ");
		}
		
		return true;
	}

	public void addDebugger(VisDebugger debugger) {
//		this.debugger = debugger;
		
	}
}
