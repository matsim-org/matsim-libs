/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkConfigGroup.java
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

import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.Map;

public final class NetworkConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "network";

	private static final String INPUT_FILE= "inputNetworkFile";

	private static final String TIME_VARIANT_NETWORK = "timeVariantNetwork";
	private static final String CHANGE_EVENTS_INPUT_FILE = "inputChangeEventsFile";

	private static final String LANEDEFINITIONSINPUTFILE = "laneDefinitionsFile";
	private static final String INPUT_CRS = "inputCRS";

	private String inputFile = null;

	private String inputCRS = null;

	private String changeEventsInputFile = null;

	private boolean timeVariantNetwork = false;

	private String laneDefinitionsFile = null;

	public NetworkConfigGroup() {
		super(NetworkConfigGroup.GROUP_NAME);
	}

	@Override
	public Map<String,String> getComments() {
		final Map<String,String> comments = super.getComments();

		comments.put( INPUT_CRS , "The Coordinates Reference System in which the coordinates are expressed in the input file." +
				" At import, the coordinates will be converted to the coordinate system defined in \"global\", and will" +
				"be converted back at export. If not specified, no conversion happens." );

		return comments;
	}

	/* direct access */

	@StringGetter( INPUT_FILE )
	public String getInputFile() {
		return this.inputFile;
	}
	@StringSetter( INPUT_FILE )
	public void setInputFile(final String inputFile) {
		this.inputFile = inputFile;
	}

	@StringSetter( CHANGE_EVENTS_INPUT_FILE )
	public void setChangeEventInputFile(final String changeEventsInputFile) {
		this.changeEventsInputFile = changeEventsInputFile;
	}
	@StringGetter( CHANGE_EVENTS_INPUT_FILE )
	public String getChangeEventsInputFile() {
		return this.changeEventsInputFile;
	}

	@StringSetter( TIME_VARIANT_NETWORK )
	public void setTimeVariantNetwork(final boolean timeVariantNetwork) {
		this.timeVariantNetwork = timeVariantNetwork;
	}
	@StringGetter( TIME_VARIANT_NETWORK )
	public boolean isTimeVariantNetwork() {
		return this.timeVariantNetwork;
	}

	@StringSetter( LANEDEFINITIONSINPUTFILE )
	public void setLaneDefinitionsFile(final String laneDefinitions) {
		this.laneDefinitionsFile = laneDefinitions;
	}

	@StringGetter( LANEDEFINITIONSINPUTFILE )
	public String getLaneDefinitionsFile(){
		return this.laneDefinitionsFile;
	}


	@StringGetter( INPUT_CRS )
	public String getInputCRS() {
		return inputCRS;
	}

	@StringSetter( INPUT_CRS )
	public void setInputCRS(String inputCRS) {
		this.inputCRS = inputCRS;
	}
}
