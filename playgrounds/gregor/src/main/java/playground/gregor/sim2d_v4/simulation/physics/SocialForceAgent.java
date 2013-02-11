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
public class SocialForceAgent implements Sim2DAgent {
	
	//Helbing constants
	private final float v0 = 1.34f; //desired velocity
	private final float m = (float) (70 + (MatsimRandom.getRandom().nextDouble()*20)); //mass
	private final float tau = .5f; //acceleration time
	private final float A = 2000f; // ??
	private final float B = .08f; // ??
	private final float k = 1.2f * 100000;
	private final float kappa = 2.4f * 100000;
	private final float r = (float) (MatsimRandom.getRandom().nextDouble()*.1 + 0.25); //radius
	private final float vmx = 1.5f;
	
	//additional constants
	private final float CUTOFF_DIST = 20f;
	
	private final float [] pos = {0,0}; //location
	
	private final float [] v = {0,0}; //velocity
	
	private final QVehicle veh;
	private final MobsimDriverAgent driver;
	private PhysicalSim2DSection currentPSec;
	
	private final Neighbors ncalc = new Neighbors();
	private final Obstacles ocalc = new Obstacles();
	private final DesiredDirection dd = new DesiredDirection();

	private final float dT;
	
	private final float [] oldPos = {0,0};
	private final int notMoved = 0;
	private final float notMovedThreshold = 0.01f*0.01f;
	private final int maxNotMoved = 10;

	public SocialForceAgent(QVehicle veh, float spawnX, float spawnY, float deltaT) {
		this.pos[0] = spawnX;
		this.pos[1] = spawnY;
		this.veh = veh;
		this.driver = veh.getDriver();
		this.dT = deltaT;
		this.ncalc.setRangeAndMaxNrOfNeighbors(5, 8);
	}

	@Override
	public QVehicle getQVehicle() {
		return this.veh;
	}

	@Override
	public void updateVelocity() {
		
//		//experimental
//		float mvX = this.oldPos[0] - this.pos[0];
//		float mvY = this.oldPos[1] - this.pos[1];
//		this.oldPos[0] = this.pos[0];
//		this.oldPos[1] = this.pos[1];
//		float sqrMvd = mvX*mvX + mvY*mvY;
//		if (sqrMvd < this.notMovedThreshold) {
//			this.notMoved++;
//			if (this.notMoved >= this.maxNotMoved) {
//				this.notMoved = 0;
//				this.v[0] = MatsimRandom.getRandom().nextFloat()-.5f;
//				this.v[1] = MatsimRandom.getRandom().nextFloat()-.5f;
//				return;
//			}
//		}
		
		List<Tuple<Float, Sim2DAgent>> neighbors = this.ncalc.computeNeighbors(this);
		List<Segment> obstacles = this.ocalc.computeObstacles(this);

		
		float[] e0 = this.dd.computeDesiredDirection(this);
		float desiredDVx = this.v0 * e0[0] - this.v[0];
		float desiredDVy = this.v0 * e0[1] - this.v[1];
		
		desiredDVx /= this.tau;
		desiredDVy /= this.tau;
		
		//neighbors
		float fnx = 0;
		float fny = 0;
		
	
		for (Tuple<Float, Sim2DAgent> t : neighbors) {
			Sim2DAgent neighbor = t.getSecond();
			float[] nPos = neighbor.getPos();
			float nx = this.pos[0] - nPos[0];
			float ny = this.pos[1] - nPos[1];
			float dist = (float) Math.sqrt(nx * nx  + ny * ny);
			if (dist > this.CUTOFF_DIST) {
				continue;
			}
			nx /= dist;
			ny /= dist;
			
			float r = this.r + neighbor.getRadius();
			
			float exp = playground.gregor.sim2d_v4.math.Math.exp((r-dist)/this.B);
			
			if (dist < r) {
				float overlap = (r-dist);
				exp += this.k * overlap;
				
				float tx = -ny;
				float ty = nx;
				float[] nv = neighbor.getVelocity();
				float dvx = nv[0] - this.v[0];
				float dvy = nv[1] - this.v[1];
				float dv = dvx*tx + dvy*ty;
				fnx += this.kappa * overlap * dv * tx;
				fny += this.kappa * overlap * dv * ty;
			}
			
			fnx += this.A * exp * nx;
			fny += this.A * exp * ny;
		}
		
		//obstacles
		float fwx = 0;
		float fwy = 0;
		for (Segment s : obstacles) {
			float r = CGAL.vectorCoefOfPerpendicularProjection(this.pos[0], this.pos[1], s.x0, s.y0, s.x1, s.y1);
			float nx;
			float ny;
			float dist;
			if (r < 0) {
				nx = this.pos[0] - s.x0;
				ny = this.pos[1] - s.y0;
				dist = (float) Math.sqrt(nx*nx + ny*ny);
				nx /= dist;
				ny /= dist;
			} else if (r > 1) {
				nx = this.pos[0] - s.x1;
				ny = this.pos[1] - s.y1;
				dist = (float) Math.sqrt(nx*nx + ny*ny);
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
			
			float exp = playground.gregor.sim2d_v4.math.Math.exp((this.r-dist)/this.B);

			if (dist < this.r &&  r >=0 && r <= 1) {
				float overlap = (this.r-dist);
				exp += this.k * overlap;
				
				float tx = -ny;
				float ty = nx;
				float dv = this.v[0]*tx + this.v[1]*ty;
				fnx += this.kappa * overlap * dv * tx;
				fny += this.kappa * overlap * dv * ty;
			}
			
			fwx += this.A * exp * nx;
			fwy += this.A * exp * ny;
		}
		
		float dvx = desiredDVx + fnx/this.m + fwx/this.m;
		float dvy = desiredDVy + fny/this.m + fwy/this.m;
		dvx *= this.dT;
		dvy *= this.dT;
		
		this.v[0] += dvx;
		this.v[1] += dvy;
		
		if ((this.v[0]*this.v[0]+this.v[1]*this.v[1]) > (this.vmx*this.vmx)) { //velocity constraint speed never exceeds 5m/s
			float v = (float) Math.sqrt((this.v[0]*this.v[0]+this.v[1]*this.v[1]));
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
	public void move(float dx, float dy) {
		this.pos[0] += dx;
		this.pos[1] += dy;
	}

	@Override
	public float[] getVelocity() {
		return this.v;
	}

	@Override
	public Id getCurrentLinkId() {
		return this.driver.getCurrentLinkId();
	}

	@Override
	public float[] getPos() {
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
	}

	@Override
	public void debug(VisDebugger visDebugger) {
		if (getId().toString().contains("g")) {
			visDebugger.addCircle(this.getPos()[0], this.getPos()[1], this.r, 0, 192, 64, 128,0,true);
		} else if (getId().toString().contains("r")) {
			visDebugger.addCircle(this.getPos()[0], this.getPos()[1], this.r, 192, 0, 64, 128,0,true);
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
			visDebugger.addCircle(this.getPos()[0], this.getPos()[1], this.r, r, g, b, 222,0,true);
//			visDebugger.addText(this.getPos()[0],this.getPos()[1], this.getId().toString(), 0);
		}
		this.ocalc.addDebugger(visDebugger);
		
	}

	@Override
	public PhysicalSim2DSection getPSec() {
		return this.currentPSec;
	}

	@Override
	public float getRadius() {
		return this.r;
	}

	@Override
	public float getXLocation() {
		return this.pos[0];
	}

	@Override
	public float getYLocation() {
		return this.pos[1];
	}

}
