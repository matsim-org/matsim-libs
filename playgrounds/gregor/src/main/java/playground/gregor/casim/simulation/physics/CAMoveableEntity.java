/* *********************************************************************** *
 * project: org.matsim.*
 * CAAgent.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.gregor.casim.simulation.physics;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public abstract class CAMoveableEntity {

	private int dir;
	private int pos;
	private CAEvent currentEvent;

	private double rho;
	private int lane;
	private double timeGap = 1 / (AbstractCANetwork.RHO_HAT + AbstractCANetwork.V_HAT);
	
	private double lastMovementTime = 0;
	private double spd = AbstractCANetwork.V_HAT;
	
//	private final double [] laneSpds = new double[8];
//	private final double [] updates = new double[8];
	private final Map<Integer,Double> laneSpds = new HashMap<>();
	private int nextLane = 0;
	
	public void proceed() {
		this.pos += this.dir;
	}

	public void updateSpdOnProceed(double traveled, double time) {
		double tt = time-this.lastMovementTime;
		double cSpd = traveled/tt;
		this.spd *= 0.95;
		this.spd += 0.05 * cSpd;
		this.lastMovementTime = time;
	}
	
	public double getSpd() {
		return this.spd;
	}
	
	public void updateLaneSpeed(double spd, int lane) {
		
//		double oldW = (this.updates[lane]/(this.updates[lane]+1));
//		double newW = (1/(this.updates[lane]+1));
		
//		double oldW = 0;
//		double newW = 1;
//		if (this.updates[lane] < 10) {
//			System.out.println(this.updates[lane]);
//		}
//		
//		this.laneSpds[lane] *= oldW;
//		this.laneSpds[lane] += newW * spd;
//		this.updates[lane]++;
		this.laneSpds.put(lane, spd);
	}
	
	public double getLaneSpeed(int lane) {
//		return this.laneSpds[lane];
		Double speed = this.laneSpds.get(lane);
		if (speed == null) {
			return AbstractCANetwork.V_HAT;
		}
		return speed;
		
	}
	
	public void setNextLane(int lane) {
		this.nextLane = lane;
	}
	
	public double getLaneScore(int lane) {
		return lane == this.nextLane ? 1 : 0;
//		return this.laneSpds[lane];
	}
	
	
	
	public double getMaxSpd(double time, double maxTraveled) {
		double tt = time-this.lastMovementTime;
		double cSpd = maxTraveled/tt;
		return Math.min(getSpd(), cSpd);
	}
	
	public int getPos() {
		return this.pos;
	}

	public int getDir() {
		return this.dir;
	}

	public int getLane() {
		return this.lane;
	}

	public void materialize(int pos, int dir) {
		this.pos = pos;
		this.dir = dir;
	}

	public void materialize(int pos, int dir, int lane) {
		this.pos = pos;
		this.dir = dir;
		this.lane = lane;
	}

	abstract Id<Link> getNextLinkId();

	abstract void moveOverNode(CALink nextLink, double time);

	// TODO add generic
	public abstract Id getId();

	public abstract CANetworkEntity getCurrentCANetworkEntity();

	public abstract CANetworkEntity getLastCANetworkEntity();

	public abstract void moveToNode(CANode n);

	public CAEvent getCurrentEvent() {
		return this.currentEvent;
	}

	public void setCurrentEvent(CAEvent event) {
		if (this.currentEvent != null) {
			this.currentEvent.setObsolete();
		}
		this.currentEvent = event;
	}

	public void invalidate() {
		if (this.currentEvent != null) {
			this.currentEvent.setObsolete();
		}
	}

	public abstract Link getCurrentLink();

	@Override
	public String toString() {
		String a = "agent: " + getId() + " next event:" + this.currentEvent
				+ "\n";
		return a;
	}

	public double getRho() {
		return this.rho;
	}

	public void setRho(double rho) {
		this.rho = rho;
	}

	public void setTimeGap(double gap) {
		this.timeGap = gap;

	}

	public double getTimeGap() {
		return this.timeGap;
	}

	public void setLane(int newLane) {
		this.lane = newLane;
	}

	

}
