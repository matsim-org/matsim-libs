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
import org.matsim.api.core.v01.network.Link;

public abstract class CAAgent {

	private int dir;
	private int pos;
//	private final CANetwork net;
//	private CALink l;
	private final Id id;
	private CAEvent currentEvent;
	private double cumWaitTime;
	private double rho;
	
	private AgentInfo ai = new AgentInfo(this);
	
	
	public CAAgent(Id id) {
//		this.net = net;
		this.id = id;
	}
	
	public AgentInfo getAgentInfo(){
		return this.ai;
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
	
	abstract Id<Link> getNextLinkId();
	
	abstract void moveOverNode(CALink nextLink,double time);

	abstract CALink getCurrentLink();
	
	
	public Id getId() {
		return this.id;
	}

	
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
		String a = "agent: " + getId() + " next event:" + this.currentEvent + "\n";
		return a;
//		String myFront = "\t\tmy sp front: " + this.ai.getMySpacingsFront()[0] + " nr front:" + this.ai.getMySpacingsFront()[1] +"\n";
//		String myBehind = "\t\tmy sp behind: " + this.ai.getMySpacingsBehind()[0] + " nr behind:" + this.ai.getMySpacingsBehind()[1] + "\n";
//		String theirFront = "\t\tth sp front: " + this.ai.getTheirSpacingsFront()[0] + " nr front:" + this.ai.getTheirSpacingsFront()[1] +"\n";
//		String theirBehind = "\t\tth sp behind: " + this.ai.getTheirSpacingsBehind()[0] + " nr behind:" + this.ai.getTheirSpacingsBehind()[1] + "\n";
//		return a + myFront + myBehind + theirFront + theirBehind + ai.getScene();
	}

	public double getRho() {
		return this.rho;
	}
	public void setRho(double rho) {
		this.rho = rho;
	}
	
}
