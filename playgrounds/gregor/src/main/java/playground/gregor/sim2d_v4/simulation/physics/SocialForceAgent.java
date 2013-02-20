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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.utils.collections.Tuple;

import playground.gregor.sim2d_v4.cgal.CGAL;
import playground.gregor.sim2d_v4.debugger.VisDebugger;
import playground.gregor.sim2d_v4.simulation.physics.PhysicalSim2DSection.Segment;
import playground.gregor.sim2d_v4.simulation.physics.algorithms.DesiredDirection;
import playground.gregor.sim2d_v4.simulation.physics.algorithms.Neighbors;
import playground.gregor.sim2d_v4.simulation.physics.algorithms.Obstacles;

/**
 * Social force model according to: D. Helbing, I. Farkas, T. Vicsek,
 * Nature 407, 487-490 (2000)
 * @author laemmel
 *
 */
public class SocialForceAgent implements Sim2DAgent, DelegableSim2DAgent {

	//Helbing constants
	private double v0 = 1.34f; //desired velocity
	private final double m = 70 + (MatsimRandom.getRandom().nextDouble()*20); //mass
	private final double tau = .5f; //acceleration time
	private final double A = 2000f; // ??
	private final double B = .08f; // ??
	private final double k = 1.2f * 100000;
	private final double kappa = 2.4f * 100000;
	private final double r = MatsimRandom.getRandom().nextDouble()*.1 + 0.25; //radius
	private final double vmx = 1.5f;

	//additional constants
	private final double CUTOFF_DIST = 20f;

	private final double [] pos = {0,0}; //location

	private final double [] v = {0,0}; //velocity

	private final QVehicle veh;
	private final MobsimDriverAgent driver;
	private PhysicalSim2DSection currentPSec;

	private final Neighbors ncalc = new Neighbors();
	private final Obstacles ocalc = new Obstacles();
	private DesiredDirection dd;

	private final double dT;


	public SocialForceAgent(QVehicle veh, double spawnX, double spawnY, double deltaT) {
		this.pos[0] = spawnX;
		this.pos[1] = spawnY;
		this.veh = veh;
		this.driver = veh.getDriver();
		this.dT = deltaT;
		this.ncalc.setRangeAndMaxNrOfNeighbors(5, 8);
		this.dd = new DesiredDirection(this);
	}

	@Override
	public QVehicle getQVehicle() {
		return this.veh;
	}

	@Override
	public void updateVelocity() {


		List<Tuple<Double, Sim2DAgent>> neighbors = this.ncalc.computeNeighbors(this);
		List<Segment> obstacles = this.ocalc.computeObstacles(this);


		double[] e0 = this.dd.computeDesiredDirection();
		double desiredDVx = this.v0 * e0[0] - this.v[0];
		double desiredDVy = this.v0 * e0[1] - this.v[1];

		desiredDVx /= this.tau;
		desiredDVy /= this.tau;

		//neighbors
		double fnx = 0;
		double fny = 0;


		for (Tuple<Double, Sim2DAgent> t : neighbors) {
			Sim2DAgent neighbor = t.getSecond();
			double[] nPos = neighbor.getPos();
			double nx = this.pos[0] - nPos[0];
			double ny = this.pos[1] - nPos[1];
			double dist = Math.sqrt(nx * nx  + ny * ny);
			if (dist > this.CUTOFF_DIST) {
				continue;
			}
			nx /= dist;
			ny /= dist;

			double r = this.r + neighbor.getRadius();

			double exp = playground.gregor.sim2d_v4.math.Math.exp((r-dist)/this.B);

			if (dist < r) {
				double overlap = (r-dist);
				exp += this.k * overlap;

				double tx = -ny;
				double ty = nx;
				double[] nv = neighbor.getVelocity();
				double dvx = nv[0] - this.v[0];
				double dvy = nv[1] - this.v[1];
				double dv = dvx*tx + dvy*ty;
				fnx += this.kappa * overlap * dv * tx;
				fny += this.kappa * overlap * dv * ty;
			}

			fnx += this.A * exp * nx;
			fny += this.A * exp * ny;
		}

		//obstacles
		double fwx = 0;
		double fwy = 0;
		for (Segment s : obstacles) {
			double r = CGAL.vectorCoefOfPerpendicularProjection(this.pos[0], this.pos[1], s.x0, s.y0, s.x1, s.y1);
			double nx;
			double ny;
			double dist;
			if (r < 0) {
				nx = this.pos[0] - s.x0;
				ny = this.pos[1] - s.y0;
				dist = Math.sqrt(nx*nx + ny*ny);
				nx /= dist;
				ny /= dist;
			} else if (r > 1) {
				nx = this.pos[0] - s.x1;
				ny = this.pos[1] - s.y1;
				dist = Math.sqrt(nx*nx + ny*ny);
				nx /= dist;
				ny /= dist;
			} else {
				dist = CGAL.signDistPointLine(this.pos[0], this.pos[1], s.x0, s.y0, s.dx, s.dy);
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

			double exp = playground.gregor.sim2d_v4.math.Math.exp((this.r-dist)/this.B);

			if (dist < this.r &&  r >=0 && r <= 1) {
				double overlap = (this.r-dist);
				exp += this.k * overlap;

				double tx = -ny;
				double ty = nx;
				double dv = this.v[0]*tx + this.v[1]*ty;
				fnx += this.kappa * overlap * dv * tx;
				fny += this.kappa * overlap * dv * ty;
			}

			fwx += this.A * exp * nx;
			fwy += this.A * exp * ny;
		}

		double dvx = desiredDVx + fnx/this.m + fwx/this.m;
		double dvy = desiredDVy + fny/this.m + fwy/this.m;
		dvx *= this.dT;
		dvy *= this.dT;

		if (Double.isNaN(dvy)) {
			System.out.println("stop");
		}
		
		this.v[0] += dvx;
		this.v[1] += dvy;
		
	

		if ((this.v[0]*this.v[0]+this.v[1]*this.v[1]) > (this.vmx*this.vmx)) { //velocity constraint speed never exceeds 5m/s
			double v = Math.sqrt((this.v[0]*this.v[0]+this.v[1]*this.v[1]));
			this.v[0] /= v;
			this.v[1] /= v;
			this.v[0] *= this.vmx;
			this.v[1] *= this.vmx;
		}


	}

	

	@Override
	public void setPSec(PhysicalSim2DSection physicalSim2DSection) {
		this.currentPSec = physicalSim2DSection;

	}

	@Override
	public void move(double dx, double dy) {
		this.pos[0] += dx;
		this.pos[1] += dy;
	}

	@Override
	public double[] getVelocity() {
		return this.v;
	}

	@Override
	public Id getCurrentLinkId() {
		return this.driver.getCurrentLinkId();
	}

	@Override
	public double[] getPos() {
		return this.pos;
	}

	@Override
	public Id chooseNextLinkId() {
		Id id = this.driver.chooseNextLinkId();
		return id;
	}

	@Override
	public Id getId() {
		return this.driver.getId();
	}

	@Override
	public void notifyMoveOverNode(Id nextLinkId) {
		this.driver.notifyMoveOverNode(nextLinkId);
		final Link l = this.currentPSec.getSim2dsc().getMATSimScenario().getNetwork().getLinks().get(nextLinkId);
		this.setDesiredSpeed(l.getFreespeed());
	}

	@Override
	public void debug(VisDebugger visDebugger) {
		if (getId().toString().equals("g3242")){
			visDebugger.addCircle((float)this.getPos()[0], (float)this.getPos()[1], (float)this.r, 255, 0, 0, 255,0,true);
			visDebugger.addText((float)this.getPos()[0],(float)this.getPos()[1], this.getCurrentLinkId().toString(), 90);
		}else if (getId().toString().contains("g")) {
			visDebugger.addCircle((float)this.getPos()[0],(float) this.getPos()[1],(float) this.r, 0, 192, 64, 128,0,true);
		} else if (getId().toString().contains("r")) {
			visDebugger.addCircle((float)this.getPos()[0],(float) this.getPos()[1],(float) this.r, 192, 0, 64, 128,0,true);
		} else {
			int nr = this.hashCode()%3*255;
			int r,g,b;
			if (nr > 2*255) {
				r= nr-2*255;
				g =0;
				b=64;
			} else if (nr > 255) {
				r=0;
				g=nr-255;
				b=64;
			} else {
				r=64;
				g=0;
				b=nr;
			}
			visDebugger.addCircle((float)this.getPos()[0], (float)this.getPos()[1], (float)this.r, r, g, b, 222,0,true);
		}
	
		this.ocalc.addDebugger(visDebugger);

	}

	@Override
	public PhysicalSim2DSection getPSec() {
		return this.currentPSec;
	}

	@Override
	public double getRadius() {
		return this.r;
	}

	@Override
	public double getXLocation() {
		return this.pos[0];
	}

	@Override
	public double getYLocation() {
		return this.pos[1];
	}

	@Override
	public void setDesiredDirectionCalculator(DesiredDirection dd) {
		this.dd = dd;
		
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Sim2DAgent) {
			return getId().equals(((Sim2DAgent) obj).getId());
		}
		return false;
	}

	@Override
	public void setDesiredSpeed(double v) {
		double g = MatsimRandom.getRandom().nextGaussian()*.26;
		this.v0 = v+g;
	}

}
