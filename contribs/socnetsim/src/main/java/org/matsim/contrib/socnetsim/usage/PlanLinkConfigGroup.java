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
package org.matsim.contrib.socnetsim.usage;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.core.config.experimental.ReflectiveConfigGroup;
import org.matsim.core.utils.collections.CollectionUtils;

/**
 * @author thibautd
 */
public class PlanLinkConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "planLinkIdentifier";

	public static enum LinkStrength {
		strong( true , true ), // a strong link is also weak
		weak( false , true ),
		none( false , false );

		private final boolean isStrong, isWeak;

		private LinkStrength( final boolean strong , final boolean weak ) {
			this.isStrong = strong;
			this.isWeak = weak;
		}

		public boolean isStrong() {
			return this.isStrong;
		}

		public boolean isWeak() {
			return this.isWeak;
		}
	}

	private LinkStrength linkJointTrips = LinkStrength.strong;
	private LinkStrength linkVehicles = LinkStrength.weak;
	private LinkStrength linkJoinableActivities = LinkStrength.weak;
	private final Set<String> joinableTypes = new TreeSet<String>( Collections.singleton( "leisure" ) );

	public PlanLinkConfigGroup() {
		super( GROUP_NAME );
	}

	@StringGetter( "linkJointTrips" )
	public LinkStrength getLinkJointTrips() {
		return this.linkJointTrips;
	}

	@StringSetter( "linkJointTrips" )
	public void setLinkJointTrips(LinkStrength linkJointTrips) {
		this.linkJointTrips = linkJointTrips;
	}

	@StringGetter( "linkVehicles" )
	public LinkStrength getLinkVehicles() {
		return this.linkVehicles;
	}

	@StringSetter( "linkVehicles" )
	public void setLinkVehicles(LinkStrength linkVehicles) {
		this.linkVehicles = linkVehicles;
	}

	@StringGetter( "linkJoinableActivities" )
	public LinkStrength getLinkJoinableActivities() {
		return this.linkJoinableActivities;
	}

	@StringSetter( "linkJoinableActivities" )
	public void setLinkJoinableActivities(LinkStrength linkJoinableActivities) {
		this.linkJoinableActivities = linkJoinableActivities;
	}

	public Collection<String> getJoinableTypes() {
		return this.joinableTypes;
	}

	public void addJoinableType(String type) {
		this.joinableTypes.add( type );
	}

	@StringGetter( "joinableTypes" )
	public String getJoinableTypesString() {
		return CollectionUtils.setToString( joinableTypes );
	}

	@StringSetter( "joinableTypes" )
	public void setJoinableTypes(final String types) {
		joinableTypes.clear();
		joinableTypes.addAll( CollectionUtils.stringToSet( types ) );
	}
}

