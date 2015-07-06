/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.pt.config;

import java.util.Map;

import org.matsim.core.config.ReflectiveConfigGroup;

public class TransitRouterConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "transitRouter";

	/*package*/ static final String SEARCH_RADIUS = "searchRadius";
	/*package*/ static final String EXTENSION_RADIUS = "extensionRadius";
	/*package*/ static final String MAX_BEELINE_WALK_CONNECTION_DISTANCE = "maxBeelineWalkConnectionDistance";
	/*package*/ static final String ADDITIONAL_TRANSFER_TIME = "additionalTransferTime";

	private double searchRadius = 1000.0;
	private double extensionRadius = 200.0;
	private double maxBeelineWalkConnectionDistance = 100.0;
	private double additionalTransferTime = 0.0;

	public TransitRouterConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> comments = super.getComments();
		comments.put(SEARCH_RADIUS, "the radius in which stop locations are searched, given a start or target coordinate");
		comments.put(EXTENSION_RADIUS, "step size to increase searchRadius if no stops are found");
		comments.put(MAX_BEELINE_WALK_CONNECTION_DISTANCE, "maximum beeline distance between stops that agents could transfer to by walking");
		comments.put(ADDITIONAL_TRANSFER_TIME, "additional time the router allocates when a line switch happens. Can be interpreted as a 'safety' time that agents need to safely transfer from one line to another");
		return comments;
	}

	@StringSetter( SEARCH_RADIUS )
	public void setSearchRadius(final double searchRadius) {
		testForLocked() ;
		this.searchRadius = searchRadius;
	}

	@StringGetter( SEARCH_RADIUS )
	public double getSearchRadius() {
		return this.searchRadius;
	}

	@StringSetter( EXTENSION_RADIUS )
	public void setExtensionRadius(final double extensionRadius) {
		testForLocked() ;
		this.extensionRadius = extensionRadius;
	}

	@StringGetter( EXTENSION_RADIUS )
	public double getExtensionRadius() {
		return this.extensionRadius;
	}

	@StringSetter( MAX_BEELINE_WALK_CONNECTION_DISTANCE )
	public void setMaxBeelineWalkConnectionDistance(final double maxBeelineWalkConnectionDistance) {
		testForLocked() ;
		this.maxBeelineWalkConnectionDistance = maxBeelineWalkConnectionDistance;
	}

	@StringGetter( MAX_BEELINE_WALK_CONNECTION_DISTANCE )
	public double getMaxBeelineWalkConnectionDistance() {
		return this.maxBeelineWalkConnectionDistance;
	}

	@StringSetter( ADDITIONAL_TRANSFER_TIME )
	public void setAdditionalTransferTime(final double additionalTransferTime) {
		testForLocked() ;
		this.additionalTransferTime = additionalTransferTime;
	}

	@StringGetter( ADDITIONAL_TRANSFER_TIME )
	public double getAdditionalTransferTime() {
		return this.additionalTransferTime;
	}
}
