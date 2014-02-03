/* *********************************************************************** *
 * project: org.matsim.*
 * PlanLinkConfigGroup.java
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
package playground.thibautd.socnetsim;

import org.matsim.core.config.experimental.ReflectiveModule;

/**
 * @author thibautd
 */
public class PlanLinkConfigGroup extends ReflectiveModule {
	public static final String GROUP_NAME = "planLinkIdentifier";

	private boolean linkJointTrips = true;
	private boolean linkVehicles = true;
	private boolean linkJoinableActivities = false;

	public PlanLinkConfigGroup() {
		super( GROUP_NAME );
	}

	@StringGetter( "linkJointTrips" )
	public boolean getLinkJointTrips() {
		return this.linkJointTrips;
	}

	@StringSetter( "linkJointTrips" )
	public void setLinkJointTrips(boolean linkJointTrips) {
		this.linkJointTrips = linkJointTrips;
	}

	@StringGetter( "linkVehicles" )
	public boolean getLinkVehicles() {
		return this.linkVehicles;
	}

	@StringSetter( "linkVehicles" )
	public void setLinkVehicles(boolean linkVehicles) {
		this.linkVehicles = linkVehicles;
	}

	@StringGetter( "linkJoinableActivities" )
	public boolean getLinkJoinableActivities() {
		return this.linkJoinableActivities;
	}

	@StringSetter( "linkJoinableActivities" )
	public void setLinkJoinableActivities(boolean linkJoinableActivities) {
		this.linkJoinableActivities = linkJoinableActivities;
	}

}

