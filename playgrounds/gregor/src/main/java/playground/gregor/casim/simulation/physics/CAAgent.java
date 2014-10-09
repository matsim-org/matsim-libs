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

import org.matsim.api.core.v01.Id;

public abstract class CAAgent {

	private int dir;
	private int pos;
//	private final CANetwork net;
//	private CALink l;
	private final Id id;
	private CAEvent currentEvent;
	private double cumWaitTime;
	
	
	public CAAgent(Id id) {
//		this.net = net;
		this.id = id;
	}
	
	public void proceed() {
		this.pos += this.dir;
		
//		//DEBUG
//		double lCoeff = (this.pos+1.)/this.l.getNumOfCells();
//		double dx = this.l.getLink().getToNode().getCoord().getX()-this.l.getLink().getFromNode().getCoord().getX();
//		double dy = this.l.getLink().getToNode().getCoord().getY()-this.l.getLink().getFromNode().getCoord().getY();
//		dx *= lCoeff;
//		dy *= lCoeff;
//		double x = this.l.getLink().getFromNode().getCoord().getX() + dx;
//		double y = this.l.getLink().getFromNode().getCoord().getY() + dy;
//		XYVxVyEventImpl e = new XYVxVyEventImpl(this.id, x, y, 1, 1, time);
//		this.net.getEventsManager().processEvent(e);
	}
	
	public int getPos() {
		return this.pos;
	}
	
	public int getDir() {
		return this.dir;
	}
	
	public void materialize(int pos,int dir) {
		this.pos = pos;
		this.dir = dir;
//		this.l = l;
	}
	
	abstract Id getNextLinkId();
	
	abstract void moveOverNode(CALink nextLink,double time);

	abstract CALink getCurrentLink();
	
	
	public Id getId() {
		return this.id;
	}

	public abstract double getZ();

	public abstract double getD();
	
	public abstract CANetworkEntity getCurrentCANetworkEntity();
	
	public abstract void moveToNode(CANode n);

	public void setCurrentEvent(CAEvent event) {
		if (this.currentEvent != null) {
			this.currentEvent.setObsolete();
		}
		this.currentEvent = event;
	}
	
	@Override
	public String toString() {
		return "agent: " + getId() + " next event:" + this.currentEvent;
	}
	
	public abstract double getCumWaitTime();

	public abstract void setCumWaitTime(double tFree);
}
