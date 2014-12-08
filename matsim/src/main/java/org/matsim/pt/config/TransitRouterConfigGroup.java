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

import org.matsim.core.config.ConfigGroup;

public class TransitRouterConfigGroup extends ConfigGroup {

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
	public void addParam(final String paramName, final String value) {
		if (SEARCH_RADIUS.equals(paramName)) {
			setSearchRadius(Double.parseDouble(value));
		} else if (EXTENSION_RADIUS.equals(paramName)) {
			setExtensionRadius(Double.parseDouble(value));
		} else if (MAX_BEELINE_WALK_CONNECTION_DISTANCE.equals(paramName)) {
			setMaxBeelineWalkConnectionDistance(Double.parseDouble(value));
		} else if (ADDITIONAL_TRANSFER_TIME.equals(paramName)) {
			setAdditionalTransferTime(Double.parseDouble(value));
		} else {
			throw new IllegalArgumentException(paramName);
		}
	}

	@Override
	public String getValue(final String paramName) {
		throw new RuntimeException("use direct access to values.");
	}

	@Override
	public Map<String, String> getParams() {
		Map<String, String> params = super.getParams();
		params.put(SEARCH_RADIUS, Double.toString(this.searchRadius));
		params.put(EXTENSION_RADIUS, Double.toString(this.extensionRadius));
		params.put(MAX_BEELINE_WALK_CONNECTION_DISTANCE, Double.toString(this.maxBeelineWalkConnectionDistance));
		params.put(ADDITIONAL_TRANSFER_TIME, Double.toString(this.additionalTransferTime));
		return params;
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

	public void setSearchRadius(final double searchRadius) {
		this.searchRadius = searchRadius;
	}

	public double getSearchRadius() {
		return this.searchRadius;
	}

	public void setExtensionRadius(final double extensionRadius) {
		this.extensionRadius = extensionRadius;
	}

	public double getExtensionRadius() {
		return this.extensionRadius;
	}

	public void setMaxBeelineWalkConnectionDistance(final double maxBeelineWalkConnectionDistance) {
		this.maxBeelineWalkConnectionDistance = maxBeelineWalkConnectionDistance;
	}

	public double getMaxBeelineWalkConnectionDistance() {
		return this.maxBeelineWalkConnectionDistance;
	}

	public void setAdditionalTransferTime(final double additionalTransferTime) {
		this.additionalTransferTime = additionalTransferTime;
	}

	public double getAdditionalTransferTime() {
		return this.additionalTransferTime;
	}
}
