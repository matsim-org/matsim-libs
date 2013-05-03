/* *********************************************************************** *
 * project: org.matsim.*
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

/**
 * 
 */
package playground.ikaddoura.optimization.externalDelayEffects;

import org.matsim.api.core.v01.Id;

/**
 * This information only refers to the flow capacity constraint.
 * Storage capacity constraints are not considered.
 * 
 * @author ikaddoura
 *
 */
public class PersonDelayInfo {
	
	
	private Id personId;
	private double linkLeaveTime;
	private double delay;
	
	public Id getPersonId() {
		return personId;
	}
	public void setPersonId(Id personId) {
		this.personId = personId;
	}
	public double getLinkLeaveTime() {
		return linkLeaveTime;
	}
	public void setLinkLeaveTime(double linkLeaveTime) {
		this.linkLeaveTime = linkLeaveTime;
	}
	public double getDelay() {
		return delay;
	}
	public void setDelay(double delay) {
		this.delay = delay;
	}
	
	@Override
	public String toString() {
		return "PersonDelayInfo [personId=" + personId + ", linkLeaveTime="
				+ linkLeaveTime + ", delay=" + delay + "]";
	}

}
