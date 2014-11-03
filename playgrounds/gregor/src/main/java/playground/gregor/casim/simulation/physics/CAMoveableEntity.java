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

public abstract class CAMoveableEntity {

	private int dir;
	private int pos;
	private CAEvent currentEvent;
	
	private double rho;
	
	
	
	//HACK for testing w/o mobsim
	private boolean arrived = false;
	public boolean arrived() {
		return arrived;
	}
	public void letAgentArrive(){
		this.arrived = true;
	}
	
	public void proceed() {
		this.pos += this.dir;
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
	}
	
	abstract Id<Link> getNextLinkId();
	
	abstract void moveOverNode(CALink nextLink,double time);
	
	//TODO add generic 
	public abstract Id getId();

	
	public abstract CANetworkEntity getCurrentCANetworkEntity();
	
	public abstract void moveToNode(CANode n);

	public void setCurrentEvent(CAEvent event) {
		if (this.currentEvent != null) {
			this.currentEvent.setObsolete();
		}
		this.currentEvent = event;
	}
	
	public abstract Link getCurrentLink();
	
	
	@Override
	public String toString() {
		String a = "agent: " + getId() + " next event:" + this.currentEvent + "\n";
		return a;
	}
	public double getRho() {
		return this.rho;
	}
	public void setRho(double rho) {
		this.rho = rho;
	}
	
	
}
