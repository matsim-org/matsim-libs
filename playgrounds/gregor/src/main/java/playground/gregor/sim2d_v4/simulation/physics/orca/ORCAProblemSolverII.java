/* *********************************************************************** *
 * project: org.matsim.*
 * ORCAProblemSolverII.java
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

package playground.gregor.sim2d_v4.simulation.physics.orca;

import java.util.Collections;
import java.util.List;

import playground.gregor.sim2d_v4.cgal.CGAL;

/**
 * linear problem solver as proposed by van den Berg et al (2009), Reciprocal n-body collision avoidance. In: Inter. Symp. on Robotics Research.
 * the general work flow is similar to linear optimization in de Berg et al (2000), Computational Geometry: Algorithms and Applications. Chapter 4.4,  Springer
 * @author laemmel
 *
 */
public class ORCAProblemSolverII {

	private static final float ALMOST_ONE = 0.9999f;
//	private VisDebugger debugger;

	public float[] run(List<ORCALine> constraints, float vxPref, float vyPref, float maxSpeed) {


		//randomize order
		//		Collections.shuffle(constraints, MatsimRandom.getRandom());
				Collections.shuffle(constraints);

		float [] ret = new float[2];

		//scale vPref to maxSpeed if necessary
		float vPrefSqr = (vxPref*vxPref+vyPref*vyPref);
		if (vPrefSqr > maxSpeed*maxSpeed) {
			ret[0] = vxPref/((float)Math.sqrt(vPrefSqr)) * maxSpeed;
			ret[1] = vyPref/((float)Math.sqrt(vPrefSqr)) * maxSpeed;
		} else {
			ret[0] = vxPref;
			ret[1] = vyPref;
		}

//		GuiDebugger.addVector(new float []{0, 0, ret[0], ret[1],0,255,0});
//		GuiDebugger.peek = true;
//		try {
//			Thread.sleep(1000);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
//		gisDump(ret);

		int constraintsProcessed = linearOptimization2D(constraints,maxSpeed,vxPref,vyPref,ret);

		if (constraintsProcessed < constraints.size()) { //linear program is infeasible, i.e. there is no solution
			//van den Berg et al proposed a 3d linear program to find the 'safest possible' velocity under the given circumstances, where the 3rd dimension
			//stands for the amount the ORCA lines have to be moved in order to make the 2d linear program feasible

			//we do it here heuristically 
			int loops = 0;
			int maxLoops = 10;
			while (constraintsProcessed < constraints.size() && loops < maxLoops) {
				moveORCALinesOutward(constraints,constraintsProcessed);
				constraintsProcessed = linearOptimization2D(constraints,maxSpeed,vxPref,vyPref,ret);
				loops++;
			}

			//			
			//			
			////			System.err.println("not implemented yet, setting velocity to 0!");
			if (constraintsProcessed < constraints.size()) {
				ret[0]=Float.NaN;
				ret[1]=Float.NaN;
			}
		}

//		GuiDebugger.addVector(new float []{0, 0, ret[0], ret[1],255,0,0});
		return ret;
	}

	private void moveORCALinesOutward(List<ORCALine> constraints, int start) {
		for (int i = start; i < constraints.size(); i++) {
			ORCALine line = constraints.get(i);
			if (line instanceof ORCALineAgent) {
				line.setPointX(line.getPointX()+.1f*line.getDirectionY());
				line.setPointY(line.getPointY()-.1f*line.getDirectionX());
			}
		}

	}

	private int linearOptimization2D(List<ORCALine> constraints,
			float maxSpeed, float vxPref, float vyPref, float[] ret) {

		for (int i = 0; i < constraints.size(); i++) {
			if (!constraints.get(i).solutionSatisfyConstraint(ret)) { //constraint i not satisfied - if a new optimal solution exist, it must be located on the i-th ORCA line
				//which is a 1d linear optimization problem 
				boolean success = findNewOptimalSolution(constraints,i,maxSpeed,vxPref,vyPref,ret);
				if (!success) {
					return i;
				}
			}
		}
		return constraints.size();
	}

	private boolean findNewOptimalSolution(List<ORCALine> constraints, int idx,
			float maxSpeed, float vxPref, float vyPref, float[] ret) {

		ORCALine c = constraints.get(idx);

		//1. check whether constraint violates maxSpeed constraint
		float wx =  - c.getPointX();
		float wy =  - c.getPointY();


		float c1 = CGAL.dot(wx,wy,c.getDirectionX(),c.getDirectionY());//TODO put static methods to a more central class like Algorithms.java
		float c2 = CGAL.dot(c.getDirectionX(),c.getDirectionY(),c.getDirectionX(),c.getDirectionY());
		float b = c1 / c2;

		float xL = c.getPointX() + b * c.getDirectionX();
		float yL = c.getPointY()+ b * c.getDirectionY();
		float sqrDist = xL * xL + yL * yL;


		float sqrMaxSpeed = maxSpeed*maxSpeed;
		final float [] xxx = {0.f,0.f};
		boolean circleCenterValid = c.solutionSatisfyConstraint(xxx);
		if (sqrDist > sqrMaxSpeed && !circleCenterValid){//TODO debug
			return false;
		}

		//2. limit range by maxSpeed circle (intersection points of constraint with circle)
		float rangeHalf = (float) Math.sqrt(sqrMaxSpeed - sqrDist);
		float rightBound = b-rangeHalf; //boundaries relative to c.getPointX(),c.getPointY() in direction of c
		float leftBound = b+rangeHalf;


		//		//DEBUG		
		//		GisDebugger.addCircle(new Coordinate(0,0), maxSpeed, "vmax");
		//		LineString orcs = GisDebugger.geofac.createLineString(new Coordinate[]{new Coordinate(c.getPointX(),c.getPointY()),new Coordinate(c.getPointX()+c.getDirectionX(),c.getPointY()+c.getDirectionY()),new Coordinate(c.getPointX()+c.getDirectionX()+c.getDirectionY()/10,c.getPointY()+c.getDirectionY()-c.getDirectionX()/10)});
		//		GisDebugger.addGeometry(orcs, "ORCA");
		//		LineString bounds = GisDebugger.geofac.createLineString(new Coordinate[]{new Coordinate(c.getPointX()+c.getDirectionX()*rightBound,c.getPointY()+c.getDirectionY()*rightBound),new Coordinate(c.getPointX()+c.getDirectionX()*leftBound,c.getPointY()+c.getDirectionY()*leftBound)});
		//		GisDebugger.addGeometry(bounds, "bounds left:" + leftBound + " right:" + rightBound);

		//		//DEBUG
		//		int red=0;

		//3. iterate over all previous handled constraints and check whether the range is ok or needs to be further reduced 
		for (int i = 0; i < idx; i++) {
			ORCALine tmp = constraints.get(i);

			float cosDirection = CGAL.dot(c.getDirectionX(), c.getDirectionY(), tmp.getDirectionX(), tmp.getDirectionY());

			if (Math.abs(cosDirection) >= ALMOST_ONE) { //collinear
				if (cosDirection < 0) { //opposite direction
					if (c.solutionSatisfyConstraint(new float []{tmp.getPointX(), tmp.getPointY()})) {
						continue;
					} else {
						//failed
						return false;
					}
				} else { //same direction
					if (tmp.solutionSatisfyConstraint(new float []{c.getPointX(), c.getPointY()})) {
						continue;
					} else {
						//failed
						return false;
					}
				}
			}

			//intersection point of tmp and c is the solution of a system of linear equations
			//since we are in 2d space here, we efficiently can use cramer's rule here
			//see e.g. http://en.wikipedia.org/wiki/Cramer%27s_rule
			float a11 = c.getDirectionX();
			float a21 = c.getDirectionY();
			float a12 = -tmp.getDirectionX();
			float a22 = -tmp.getDirectionY();
			float b1 = tmp.getPointX() - c.getPointX();
			float b2 = tmp.getPointY() - c.getPointY();
			float nom = b1*a22 - b2*a12;
			float denom = a11*a22 - a12*a21;

			float dist = nom/denom;

			//right or left bound --> if denom > 0 counter-clockwise orientation of tmp --> right bound
			if (denom > 0) {
				if (rightBound < dist) {
					rightBound =  dist;
					//					//DEBUG
					//					red++;
				}
			} else {
				if (leftBound > dist) {
					leftBound =  dist;
					//					//DEBUG
					//					red++;
				}
			}

			//			//DEBUG
			//			orcs = GisDebugger.geofac.createLineString(new Coordinate[]{new Coordinate(tmp.getPointX(),tmp.getPointY()),new Coordinate(tmp.getPointX()+tmp.getDirectionX(),tmp.getPointY()+tmp.getDirectionY()),new Coordinate(tmp.getPointX()+tmp.getDirectionX()+tmp.getDirectionY()/10,tmp.getPointY()+tmp.getDirectionY()-tmp.getDirectionX()/10)});
			//			GisDebugger.addGeometry(orcs, "ORCA"+i);
			//			bounds = GisDebugger.geofac.createLineString(new Coordinate[]{new Coordinate(c.getPointX()+c.getDirectionX()*rightBound,c.getPointY()+c.getDirectionY()*rightBound),new Coordinate(c.getPointX()+c.getDirectionX()*leftBound,c.getPointY()+c.getDirectionY()*leftBound)});
			//			GisDebugger.addGeometry(bounds, "bounds"+i);
		}


		if (leftBound < rightBound) {
			return false;
		}
		//orthogonal projection prefVx,prefVy on c
		float dx = vxPref - c.getPointX();
		float dy = vyPref - c.getPointY();
		float tmp = ORCALineAgent.dot(c.getDirectionX(), c.getDirectionY(), dx, dy);

		if (tmp < rightBound) {
			ret[0] = c.getPointX() + rightBound * c.getDirectionX();
			ret[1] = c.getPointY() + rightBound * c.getDirectionY();
		} else if (tmp > leftBound) {
			ret[0] = c.getPointX() + leftBound * c.getDirectionX();
			ret[1] = c.getPointY() + leftBound * c.getDirectionY();
		} else {
			ret[0] = c.getPointX() + tmp * c.getDirectionX();
			ret[1] = c.getPointY() + tmp * c.getDirectionY();
		}
		
//		//DEBUG
//		gisDump(ret);

		//		//DEBUG
		//		if(red>2) {
		//			System.out.println(red);
		//		}
		//
		//		//DEBUG
		//		GisDebugger.dump("/Users/laemmel/devel/OCRA/dbg/lo.shp");


		return true;
	}

//	public void debug(VisDebugger debugger) {
//		this.debugger = debugger;
//	}
//	private void gisDump(float[] ret) {
//		if (ORCAForce.DEBUG) {
//			Coordinate c00 = new Coordinate(ret[0]-.1,ret[1]-.1);
//			Coordinate c11 = new Coordinate(ret[0]+.1,ret[1]+.1);
//			Coordinate c22 = new Coordinate(ret[0],ret[1]);
//			Coordinate c33 = new Coordinate(ret[0]-.1,ret[1]+.1);
//			Coordinate c44 = new Coordinate(ret[0]+.1,ret[1]-.1);
//			Coordinate c55 = new Coordinate(ret[0],ret[1]);
//			Coordinate [] coords = {c00,c11,c22,c33,c44,c55,c00};
//			LinearRing lr = GisDebugger.geofac.createLinearRing(coords);
//			Polygon p = GisDebugger.geofac.createPolygon(lr, null);
//			GisDebugger.addGeometry(p, "v");
//		}
//		
//	}

}
