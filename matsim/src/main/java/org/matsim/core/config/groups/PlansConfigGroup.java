/* *********************************************************************** *
 * project: org.matsim.*
 * PlansConfigGroup.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.core.config.groups;

import java.util.Map;

import org.matsim.core.config.experimental.ReflectiveModule;

public class PlansConfigGroup extends ReflectiveModule {

	public static final String GROUP_NAME = "plans";

	public abstract static class NetworkRouteType {
		public static final String LinkNetworkRoute = "LinkNetworkRoute";
		public static final String CompressedNetworkRoute = "CompressedNetworkRoute";
	}

	private static final String INPUT_FILE = "inputPlansFile";
	private static final String INPUT_PERSON_ATTRIBUTES_FILE = "inputPersonAttributesFile";
	private static final String NETWORK_ROUTE_TYPE = "networkRouteType";

	private String inputFile = null;
	private String networkRouteType = NetworkRouteType.LinkNetworkRoute;
	private String inputPersonAttributeFile = null;

	public PlansConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> comments = super.getComments();
		comments.put(NETWORK_ROUTE_TYPE, "Defines how routes are stored in memory. Currently supported: " + NetworkRouteType.LinkNetworkRoute + ", " + NetworkRouteType.CompressedNetworkRoute + ".");
		comments.put(INPUT_PERSON_ATTRIBUTES_FILE, "Path to a file containing person attributes (required file format: ObjectAttributes).");
		return comments;
	}

	@StringGetter( INPUT_FILE )
	public String getInputFile() {
		return this.inputFile;
	}

	@StringSetter( INPUT_FILE )
	public void setInputFile(final String inputFile) {
		this.inputFile = inputFile;
	}
	
	@StringGetter( INPUT_PERSON_ATTRIBUTES_FILE )
	public String getInputPersonAttributeFile() {
		return this.inputPersonAttributeFile;
	}

	@StringSetter( INPUT_PERSON_ATTRIBUTES_FILE )
	public void setInputPersonAttributeFile(final String inputPersonAttributeFile) {
		this.inputPersonAttributeFile = inputPersonAttributeFile;
	}

	@StringGetter( NETWORK_ROUTE_TYPE )
	public String getNetworkRouteType() {
		return this.networkRouteType;
	}

	@StringSetter( NETWORK_ROUTE_TYPE )
	public void setNetworkRouteType(final String routeType) {
		this.networkRouteType = routeType;
	}

}
