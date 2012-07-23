/* *********************************************************************** *
 * project: org.matsim.*
 * ORCAabTau.java
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

import playground.gregor.sim2d_v3.helper.gisdebug.GisDebugger;
import playground.gregor.sim2d_v3.simulation.floor.Agent2D;
import playground.gregor.sim2d_v3.simulation.floor.forces.deliberative.velocityobstacle.Algorithms;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;

public class ORCAabTauDbg implements Constraint{


	//TODO do we need them as member variables?
	private final double [] tangents = new double[4]; //left x, left y, right x, right y

	private double tauX;
	private double tauY;
	private double tauR;

	private double xpBpA;

	private double ypBpA;

	private double rAB;

	private double tauXleft;

	private double tauYleft;

	private double tauXright;

	private double tauYright;

	private double vxAvxB;

	private double vyAvyB;

	private double VOcenterX;

	private double VOcenterY;

	private double ux;

	private double uy;

	private double vxA;

	private double vyA;

	private double p0x;

	private double p0y;

	private double p1x;

	private double p1y;

	private int sign;

	public ORCAabTauDbg(Agent2D A, Agent2D B, double tau) {
		construct(A,B,tau);
	}

	private void construct(Agent2D a, Agent2D b, double tau) {



		Coordinate pA = a.getPosition();
		Coordinate pB = b.getPosition();

		// 1. construct VO_{A|B}^\tau
		this.xpBpA = pB.x-pA.x;
		this.ypBpA = pB.y-pA.y;
		this.rAB = a.getPhysicalAgentRepresentation().getAgentDiameter()/2 + b.getPhysicalAgentRepresentation().getAgentDiameter()/2;
		computeVOabTangents(this.xpBpA,this.ypBpA,this.rAB);

		this.tauX = (this.xpBpA)/tau;
		this.tauY = (this.ypBpA)/tau;
		this.tauR = this.rAB/tau;


		//check whether tau circle is responsible for u calculation 
		this.tauXleft = this.tangents[0]/tau;
		this.tauYleft = this.tangents[1]/tau;
		this.tauXright = this.tangents[2]/tau;
		this.tauYright = this.tangents[3]/tau;		


		this.vxA = a.getVx();
		this.vyA = a.getVy();

		double vxB = b.getVx();
		double vyB = b.getVy();

		this.vxAvxB = this.vxA - vxB;
		this.vyAvyB = this.vyA - vyB;

		//2. figure out what the responsible component is
		//tau circle is responsible if v_A - v_B is right of line tauLeft ---> tauRight
		if (Algorithms.isLeftOfLine(this.vxAvxB, this.vyAvyB, this.tauXleft, this.tauYleft, this.tauXright, this.tauYright) <= 0) {
			computeVectorUForCircle();
		}else {
			//construct centerline of VO cone
			this.VOcenterX = this.tangents[0]+this.tangents[2];
			this.VOcenterY = this.tangents[1]+this.tangents[3];
			if (Algorithms.isLeftOfLine(this.vxAvxB, this.vyAvyB, 0, 0, this.VOcenterX, this.VOcenterY) > 0){
				computeVectorUForLeftTangent();
			} else {
				computeVectorUForRightTangent();
			}
		}


		calcORCA();
		
		
	}

	
	private void calcORCA() {

		double xBase = this.vxA + this.ux/2;
		double yBase = this.vyA + this.uy/2;
		this.p0x = xBase - this.sign*this.uy;
		this.p0y = yBase + this.sign*this.ux;
		this.p1x = xBase + 5*this.sign*this.uy;
		this.p1y = yBase - 5*this.sign*this.ux;		
		
	}

	private void computeVectorUForLeftTangent() {
		computeVectorUFroTangent(this.tangents[0],this.tangents[1]);
		this.sign = Algorithms.isLeftOfLine(this.vxAvxB, this.vyAvyB, 0, 0, this.tangents[0],this.tangents[1]) > 0 ? -1 : 1;
		
	}
	private void computeVectorUForRightTangent() {
		computeVectorUFroTangent(this.tangents[2],this.tangents[3]);
		this.sign = Algorithms.isLeftOfLine(this.vxAvxB, this.vyAvyB, 0, 0, this.tangents[2],this.tangents[3]) < 0 ? -1 : 1;
		
	}

	private void computeVectorUFroTangent(double vx, double vy) {
		
		double c1 = dot (this.vxAvxB,this.vyAvyB,vx,vy);
		double c2= dot (vx,vy,vx,vy);
		double b = c1 / c2;
		double x = b * vx;
		double y = b * vy;
		this.ux = x - this.vxAvxB;
		this.uy = y - this.vyAvyB;
		
	}

	private double dot(double x0, double y0, double x1, double y1) {
		return x0 * x1 + y0  * y1;
	}


	private void computeVectorUForCircle() {
		double x = this.tauX - this.vxAvxB;
		double y = this.tauY - this.vyAvyB;
		//length of vector u
		double norm = Math.sqrt(Math.pow(x, 2)+Math.pow(y, 2));
		
		this.sign = norm > this.tauR ? -1 : 1;
		
		this.ux = x / norm * (norm - this.tauR);
		this.uy = y / norm * (norm - this.tauR);
//		double dx = - (x *this.tauR)/norm;
//		double dy = - (y *this.tauR)/norm;
//		
//		this.ux = x + dx;
//		this.uy = y + dy;
		
		
	}

	private void computeVOabTangents(double xpApB, double ypApB, double rAB) {

		double dx = -xpApB/2;
		double dy = -ypApB/2;

		double d = Math.sqrt(Math.pow(dx,2) + Math.pow(dy,2)); // hypot avoids underflow/overflow  ... but it is to slow

		double rSqr = Math.pow(rAB, 2);
		double a =  rSqr / (2.0 *d);

		double x2 = xpApB + (dx * a /d);
		double y2 = ypApB + (dy * a /d);

		double aSqr = Math.pow(a, 2);
		double h = Math.sqrt(rSqr - aSqr);

		double rx = -dy * (h/d);
		double ry = dx * (h/d);

		double xi = x2 + rx;
		double yi = y2 + ry;
		double xiPrime = x2 - rx;
		double yiPrime = y2 - ry;

		this.tangents[0] = xiPrime;
		this.tangents[1] = yiPrime;
		this.tangents[2] = xi;
		this.tangents[3] = yi;
	}

	public void gisDump() {


		GisDebugger.addCircle(new Coordinate(this.xpBpA,this.ypBpA), this.rAB, "circle");
		GisDebugger.addCircle(new Coordinate(this.tauX,this.tauY), this.tauR, "tau circle");
		LineString left = GisDebugger.geofac.createLineString(new Coordinate[]{new Coordinate(this.tauXleft,this.tauYleft),new Coordinate(this.tangents[0],this.tangents[1])});
		GisDebugger.addGeometry(left, "left");
		LineString right = GisDebugger.geofac.createLineString(new Coordinate[]{new Coordinate(this.tauXright,this.tauYright),new Coordinate(this.tangents[2],this.tangents[3])});
		GisDebugger.addGeometry(right, "right");

		LineString tauC = GisDebugger.geofac.createLineString(new Coordinate[]{new Coordinate(this.tauXleft,this.tauYleft),new Coordinate(this.tauXright,this.tauYright)});
		GisDebugger.addGeometry(tauC, "tau C");

		LineString vAvB = GisDebugger.geofac.createLineString(new Coordinate[]{new Coordinate(this.vxAvxB,this.vyAvyB),new Coordinate(this.vxAvxB+.05,this.vyAvyB),new Coordinate(this.vxAvxB-.05,this.vyAvyB),new Coordinate(this.vxAvxB,this.vyAvyB)
		,new Coordinate(this.vxAvxB,this.vyAvyB+.05),new Coordinate(this.vxAvxB,this.vyAvyB-.05)});
		GisDebugger.addGeometry(vAvB, "vA-vB");

		LineString u = GisDebugger.geofac.createLineString(new Coordinate[]{new Coordinate(this.vxAvxB,this.vyAvyB), new Coordinate(this.vxAvxB+this.ux,this.vyAvyB+this.uy)});
		GisDebugger.addGeometry(u, "u");

		LineString v = GisDebugger.geofac.createLineString(new Coordinate[]{new Coordinate(this.vxA,this.vyA), new Coordinate(0,0)});
		GisDebugger.addGeometry(v, "v");
		
		LineString vu = GisDebugger.geofac.createLineString(new Coordinate[]{new Coordinate(this.vxA,this.vyA), new Coordinate(this.vxA+this.ux/2,this.vyA+this.uy/2)});
		GisDebugger.addGeometry(vu, "u/2");
		
		LineString ORCA = GisDebugger.geofac.createLineString(new Coordinate[]{new Coordinate(this.p0x,this.p0y),new Coordinate(this.p1x,this.p1y)});
		GisDebugger.addGeometry(ORCA, "ORCA");
	}

	@Override
	public boolean solutionSatisfyConstraint(double x, double y) {
		return Algorithms.isLeftOfLine(x, y, this.p0x, this.p0y, this.p1x, this.p1y) > 0;
	}

	@Override
	public double getP0x() {
		return this.p0x;
	}

	@Override
	public void setP0x(double val) {
		this.p0x = val;
		
	}

	@Override
	public double getP0y() {
		return this.p0y;
	}

	@Override
	public void setP0y(double val) {
		this.p0y = val;
		
	}

	@Override
	public double getP1x() {
		return this.p1x;
	}

	@Override
	public void setP1x(double val) {
		this.p1x = val;		
	}

	@Override
	public double getP1y() {
		return this.p1y;
	}

	@Override
	public void setP1y(double val) {
		this.p1y = val;
		
	}

}
