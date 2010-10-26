/* *********************************************************************** *
 * project: org.matsim.*
 * ReplannerAdministrator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.christoph.withinday.mobsim;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;

public class ReplannerAdministrator {
	
	// If I am understanding this correctly, the replanners that are added in the following are not actively used as instances,
	// but they are used in order to identify those agents that possess those replanners.  And only those are submitted to 
	// the replanning process. Kai
	
	private Set<Id> withinDayReplannerIds;

	public ReplannerAdministrator() {
		withinDayReplannerIds = new HashSet<Id>();
	}
	
	public boolean addWithinDayReplanner(Id replannerId) {
		return this.withinDayReplannerIds.add(replannerId);
	}
	
	public boolean removeWithinDayReplanner(Id replannerId) {
		return this.withinDayReplannerIds.remove(replannerId);
	}
	
	public Set<Id> getWithinDayReplannerIds() {
		return Collections.unmodifiableSet(withinDayReplannerIds);
	}
}
