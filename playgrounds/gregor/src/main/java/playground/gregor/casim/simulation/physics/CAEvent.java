/* *********************************************************************** *
 * project: org.matsim.*
 * CAEvent.java
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

public class CAEvent implements Comparable<CAEvent>{

	public enum CAEventType {SWAP,TTA,TTE};
	
	
	private final double time;
	private final CAAgent agent;
	private final CANetworkEntity entity;
	private final CAEventType type;
	public long cnt;

	public CAEvent(double time, CAAgent agent, CANetworkEntity entity, CAEventType type){
		this.time = time;
		this.agent = agent;
		this.entity = entity;
		this.type = type;
	}
	
	
	public CAEventType getCAEventType() {
		return this.type;
	}
	
	
	@Override
	public int compareTo(CAEvent o) {
		if (this.getEventExcexutionTime() < o.getEventExcexutionTime()) {
			return -1;
		} else if (this.getEventExcexutionTime() > o.getEventExcexutionTime()) {
			return 1;
		}
		return 0;
	}

	public double getEventExcexutionTime() {
		return this.time;
	}

	public CAAgent getCAAgent() {
		return this.agent;
	}

	public CANetworkEntity getCANetworkEntity() {
		return this.entity;
	}
	
	@Override
	public String toString() {
		return "time:" + this.time + " a:" + this.agent + " " + this.type;
	}
}
