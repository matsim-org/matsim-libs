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

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;

/**
 * @author ikaddoura
 *
 */
public class LinkInfo {
	
	private Id linkId;
	private Map<Id, LinkEnterLeaveInfo> personId2enterLeaveInfo = new HashMap<Id, LinkEnterLeaveInfo>();

	public Id getLinkId() {
		return linkId;
	}
	public void setLinkId(Id linkId) {
		this.linkId = linkId;
	}
	public Map<Id, LinkEnterLeaveInfo> getPersonId2enterLeaveInfo() {
		return personId2enterLeaveInfo;
	}
	public void setPersonId2enterLeaveInfo(
			Map<Id, LinkEnterLeaveInfo> personId2enterLeaveInfo) {
		this.personId2enterLeaveInfo = personId2enterLeaveInfo;
	}
	
}
