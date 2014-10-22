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

import org.matsim.core.gbl.MatsimRandom;


public class CAEvent implements Comparable<CAEvent>{

	public enum CAEventType {SWAP,TTA,TTE};


	private final double time;
	private final CAAgent agent;
	private final CANetworkEntity entity;
	private final CAEventType type;
	public long cnt;
	private final AgentState validState;
	private boolean isObsolete = false;

	public CAEvent(double time, CAAgent agent, CANetworkEntity entity, CAEventType type){
		if (Double.isNaN(time)) {
			throw new RuntimeException("NaN");
		}
		this.time = time;
		this.agent = agent;
		this.entity = entity;
		this.type = type;
		this.validState = new AgentState(agent.getCurrentCANetworkEntity(),agent.getPos(),agent.getDir());
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
		} else if (this.getEventExcexutionTime() == o.getEventExcexutionTime()) {
			int oDir = o.getCAAgent().getDir();
			int dir = agent.getDir();
			if (dir < oDir) {
				return -1;
			} if (dir > oDir) {
				return 1;
			}  
//			return MatsimRandom.getRandom().nextBoolean() ? -1 : 1;
			return 0;

		}
		throw new RuntimeException("Invalid event excecution time!");
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

	//	public boolean isValid() {
	//		if (this.agent.getCurrentCANetworkEntity() != this.validState.e) {
	//			return false;
	//		}
	//		if (this.validState.e instanceof CANode) {
	//			return true;
	//		}
	//		if (this.agent.getPos() != this.validState.pos) {
	//			return false;
	//		}
	//		if (this.agent.getDir() != this.validState.dir) {
	//			return false;
	//		}
	//		return true;
	//	}

	@Override
	public String toString() {
		return "time:" + this.time + " type:" + this.type + " obsolete:" + isObsolete() + " agent:" + this.agent.getId() + " pos:" + this.agent.getPos();
	}

	public class AgentState{
		CANetworkEntity e;
		int pos;
		int dir;
		public AgentState(CANetworkEntity e, int pos,
				int dir) {
			this.e = e;
			this.pos = pos;
			this.dir = dir;
		}
	}

	public void setObsolete() {
		this.isObsolete  = true;

	}

	public boolean isObsolete() {
		return this.isObsolete;
	}
}
