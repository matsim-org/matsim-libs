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
 * @author ikaddoura
 *
 */
public class LinkEnterLeaveInfo {
	
	private Id personId;
	private double linkEnterTime;
	private double linkLeaveTime;
	
	public Id getPersonId() {
		return personId;
	}
	public void setPersonId(Id personId) {
		this.personId = personId;
	}
	public double getLinkEnterTime() {
		return linkEnterTime;
	}
	public void setLinkEnterTime(double linkEnterTime) {
		this.linkEnterTime = linkEnterTime;
	}
	public double getLinkLeaveTime() {
		return linkLeaveTime;
	}
	public void setLinkLeaveTime(double linkLeaveTime) {
		this.linkLeaveTime = linkLeaveTime;
	}
	@Override
	public String toString() {
		return "LinkEnterLeaveInfo [personId=" + personId + ", linkEnterTime="
				+ linkEnterTime + ", linkLeaveTime=" + linkLeaveTime + "]";
	}	

}
