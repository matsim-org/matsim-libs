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

/**
 * An event that can be handled by CA sim
 * 
 * @author laemmel
 *
 */
public class CAEvent implements Comparable<CAEvent> {

	public enum CAEventType {
		SWAP, TTA, TTE, END_OF_FRAME, END_OF_SIM
	};

	private final double time;
	private final CAMoveableEntity agent;
	private final CANetworkEntity entity;
	private final CAEventType type;
	public long cnt;
	private boolean isObsolete = false;
	private boolean innerLockOnly;
	private String eventId;

	public CAEvent(double time, CAMoveableEntity agent, CANetworkEntity entity,
			CAEventType type) {
		this.time = time;
		this.agent = agent;
		this.entity = entity;
		this.type = type;
		this.eventId = Thread.currentThread().getName();
	}

	public CAEventType getCAEventType() {
		return this.type;
	}

	@Override
	public int compareTo(CAEvent o) {
		double diff = this.getEventExcexutionTime()
				- o.getEventExcexutionTime();

		if (diff < 0) {
			return -1;
		} else if (diff > 0) {
			return 1;
		}
		// int oDir = o.getCAAgent().getDir();
		// int dir = agent.getDir();
		// if (dir < oDir) {
		// return -1;
		// }
		// if (dir > oDir) {
		// return 1;
		// }
		// double ox = o.getCANetworkEntity().getX();
		// double x = this.getCANetworkEntity().getX();
		// double oy = o.getCANetworkEntity().getY();
		// double y = this.getCANetworkEntity().getY();
		// double d = x + y;
		// double od = ox + oy;
		// if (d < od) {
		// return -1;
		// }
		// if (d > od) {
		// return 1;
		// }
		// return MatsimRandom.getRandom().nextBoolean() ? -1 : 1;
		return 0;

	}

	public double getEventExcexutionTime() {
		return this.time;
	}

	public CAMoveableEntity getCAAgent() {
		return this.agent;
	}

	public CANetworkEntity getCANetworkEntity() {
		return this.entity;
	}

	@Override
	public String toString() {
		return "time:" + this.time + " type:" + this.type + " obsolete:"
				+ isObsolete() + " agent:" + this.agent.getId() + " pos:"
				+ this.agent.getPos() + " entity:" + this.entity + " thread:"
				+ this.eventId;
	}

	public void setObsolete() {
		this.isObsolete = true;

	}

	public boolean isObsolete() {
		return this.isObsolete;
	}

	public synchronized int tryLock() {
		if (this.entity instanceof CALink) {
			if (this.agent.getPos() > 2
					&& this.agent.getPos() < ((CALink) this.entity).getSize() - 3) {
				return 0;
			}

		}
		return this.entity.tryLock() ? 1 : -1;
	}

	public void unlock() {
		if (!innerLockOnly) {
			this.entity.unlock();
		}
		// if (this.innerLockOnly) {
		// ((CALinkDynamic) this.entity).lock.unlock();
		//
		// } else {
		// }
	}
}
