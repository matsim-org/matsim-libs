/* *********************************************************************** *
 * project: org.matsim.*
 * SimpleAgent.java
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

package playground.gregor.sim2d_v4.simulation.physics;

import java.util.List;

import playground.gregor.sim2d_v4.cgal.CGAL;
import playground.gregor.sim2d_v4.cgal.LineSegment;
import playground.gregor.sim2d_v4.scenario.Sim2DConfig;
import playground.gregor.sim2d_v4.simulation.physics.algorithms.DesiredDirectionCalculator;
import playground.gregor.sim2d_v4.simulation.physics.algorithms.Neighbors;
import playground.gregor.sim2d_v4.simulation.physics.algorithms.Obstacles;

/**
 * Social force model according to: D. Helbing, L. Buzna, A. Johansson,
 * T. Werner, Transportation Science 39(1), 1-24 (2005)
 * @author laemmel
 *
 */
public class SocialForce2005VelocityUpdater implements VelocityUpdater {

	//Helbing constants
//	private final double v0 = 1.3 * MatsimRandom.getRandom().nextGaussian()*0.3; //desired velocity
	private final double m = 1;//70 + (MatsimRandom.getRandom().nextDouble()*20); //mass
	private final double tau = 1; //acceleration time
	private final double Aw = 5; //repulsion of walls
	private final double Bw = .1; //walls
	
	private final double A1 = 3; //direction dependent repulsion
	private final double B1 = .2; //direction dependent repulsion
	private final double A2 = 3; //direction independent repulsion
	private final double B2 = .2; //direction independent repulsion
	
	private final double lamda = 0.75;//
	
//	private final double k = 1.2f * 100000;
//	private final double kappa = 2.4f * 100000;
//	private final double r
	@Deprecated //move to Sim2DAgent [gl April'13]
	private final double vmx = 1.5f;

	//additional constants
	private final double CUTOFF_DIST = 20f;


	private final Neighbors ncalc;
	private final Obstacles ocalc = new Obstacles();
	private DesiredDirectionCalculator dd;

	private final double dT;
	private final Sim2DAgent agent;
	
	public SocialForce2005VelocityUpdater(DesiredDirectionCalculator dd, Neighbors ncalc, Sim2DConfig conf, Sim2DAgent agent) {
		this.ncalc = ncalc;
		this.dd = dd;
		this.agent = agent;
		this.dT = conf.getTimeStepSize();
	}

	/* (non-Javadoc)
	 * @see playground.gregor.sim2d_v4.simulation.physics.VelocityUpdater#updateVelocity()
	 */
	@Override
	public void updateVelocity() {

		
		List<Sim2DAgent> neighbors = this.ncalc.getNeighbors();
		List<LineSegment> obstacles = this.ocalc.computeObstacles(this.agent);

		double v0 = this.agent.getV0();
		
		double[] v = this.agent.getVelocity();
		double[] e0 = this.dd.computeDesiredDirection();
		double desiredDVx = v0 * e0[0] - v[0];
		double desiredDVy = v0 * e0[1] - v[1];

		
		desiredDVx /= this.tau;
		desiredDVy /= this.tau;

		//neighbors
		double fnx = 0;
		double fny = 0;
		
		double[] pos = this.agent.getPos();

		for (Sim2DAgent neighbor : neighbors) {
			double[] nPos = neighbor.getPos();
			double nx = pos[0] - nPos[0];
			double ny = pos[1] - nPos[1];
			double dist = Math.sqrt(nx * nx  + ny * ny);
			if (dist > this.CUTOFF_DIST) {
				continue;
			}
			nx /= dist;
			ny /= dist;

			double r = this.agent.getRadius() + neighbor.getRadius();

			double exp = Math.exp((r-dist)/this.B2);

			double cosPhi = CGAL.dot(-nx, -ny, e0[0], e0[1]);
			double exp2 = Math.exp((r-dist)/this.B1)*(this.lamda+(1-this.lamda)*(1+cosPhi)/2);
			
//			if (dist < r) {
//				double overlap = (r-dist);
//				exp += this.k * overlap;
//
//				double tx = -ny;
//				double ty = nx;
//				double[] nv = neighbor.getVelocity();
//				double dvx = nv[0] - v[0];
//				double dvy = nv[1] - v[1];
//				double dv = dvx*tx + dvy*ty;
//				fnx += this.kappa * overlap * dv * tx;
//				fny += this.kappa * overlap * dv * ty;
//			}

			fnx += this.A2 * exp * nx + this.A1 * exp2 * nx;
			fny += this.A2 * exp * ny + this.A1 * exp2 * ny;
		}

		//obstacles
		double fwx = 0;
		double fwy = 0;
		for (LineSegment s : obstacles) {
			double r = CGAL.vectorCoefOfPerpendicularProjection(pos[0], pos[1], s.x0, s.y0, s.x1, s.y1);
			double nx;
			double ny;
			double dist;
			if (r < 0) {
				nx = pos[0] - s.x0;
				ny = pos[1] - s.y0;
				dist = Math.sqrt(nx*nx + ny*ny);
				nx /= dist;
				ny /= dist;
			} else if (r > 1) {
				nx = pos[0] - s.x1;
				ny = pos[1] - s.y1;
				dist = Math.sqrt(nx*nx + ny*ny);
				nx /= dist;
				ny /= dist;
			} else {
				dist = CGAL.signDistPointLine(pos[0], pos[1], s.x0, s.y0, s.dx, s.dy);
				if (dist < 0) {
					dist = -dist;
					nx = s.dy;
					ny = -s.dx;
				} else {
					nx = s.dy;
					ny = -s.dx;
				}
			}

			//here we can optimize to avoid the sqrt if 0 <= r <= 1!
			if (dist > this.CUTOFF_DIST) {
				continue;
			}

			double radius = this.agent.getRadius(); 
			double exp = Math.exp((radius-dist)/this.Bw);
			

//			if (dist < radius &&  r >=0 && r <= 1) {
//				double overlap = (radius-dist);
//				exp += this.k * overlap;
//
//				double tx = -ny;
//				double ty = nx;
//				double dv = v[0]*tx + v[1]*ty;
//				fnx += this.kappa * overlap * dv * tx;
//				fny += this.kappa * overlap * dv * ty;
//			}

			fwx += this.Aw * exp * nx;
			fwy += this.Aw * exp * ny;
		}

		double dvx = desiredDVx + fnx/this.m + fwx/this.m;
		double dvy = desiredDVy + fny/this.m + fwy/this.m;
		dvx *= this.dT;
		dvy *= this.dT;

		v[0] += dvx;
		v[1] += dvy;
		
	

		if ((v[0]*v[0]+v[1]*v[1]) > (this.vmx*this.vmx)) { //velocity constraint speed never exceeds 5m/s
			double vv = Math.sqrt((v[0]*v[0]+v[1]*v[1]));
			v[0] /= vv;
			v[1] /= vv;
			v[0] *= this.vmx;
			v[1] *= this.vmx;
		}


	}
	
	public void setDesiredDirectionCalculator(DesiredDirectionCalculator dd) {
		this.dd = dd;
	}

}
