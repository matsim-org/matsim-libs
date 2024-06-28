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

import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.net.URL;

import java.util.Map;

public final class NetworkConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "network";

	private static final String INPUT_FILE= "inputNetworkFile";

	private static final String TIME_VARIANT_NETWORK = "timeVariantNetwork";
	private static final String CHANGE_EVENTS_INPUT_FILE = "inputChangeEventsFile";

	private static final String LANEDEFINITIONSINPUTFILE = "laneDefinitionsFile";
	private static final String INPUT_CRS = "inputCRS";

	private String inputFile = null;

	@Deprecated private String inputCRS = null;

	private String changeEventsInputFile = null;

	private boolean timeVariantNetwork = false;

	private String laneDefinitionsFile = null;

	public NetworkConfigGroup() {
		super(NetworkConfigGroup.GROUP_NAME);
	}

	@Override
	public Map<String,String> getComments() {
		final Map<String,String> comments = super.getComments();

		comments.put( INPUT_CRS , "(deprecated: rather express CRS in file) The Coordinates Reference System in which the coordinates are expressed in the input file." +
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
		testForLocked();
		// (network is set locked after the (Mutable)Scenario is created ... since it needs to know about the time-dep network before that.  Technically, the network file name
		// could still be changed later.  But we don't have (and maybe don't want) a separate switch for that.  In general, the rule of thumb
		// is: The config should be final _before_ the scenario is created.  I don't think we need to accept every possible use case where
		// this is not strictly necessary.  kai, may'22

		this.inputFile = inputFile;
	}

	public URL getInputFileURL(URL context) {
		return ConfigGroup.getInputFileURL(context, this.inputFile);
	}

	@StringSetter( CHANGE_EVENTS_INPUT_FILE )
	public void setChangeEventsInputFile(final String changeEventsInputFile) {
		this.changeEventsInputFile = changeEventsInputFile;
	}
	
	@StringGetter( CHANGE_EVENTS_INPUT_FILE )
	public String getChangeEventsInputFile() {
		return changeEventsInputFile;
	}
	
	
	public URL getChangeEventsInputFileUrl(URL context) {
		return ConfigGroup.getInputFileURL(context, this.changeEventsInputFile);
	}

	@StringSetter( TIME_VARIANT_NETWORK )
	public void setTimeVariantNetwork(final boolean timeVariantNetwork) {
		testForLocked();
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


	/**
	 * @deprecated Coordinate System can now be set directly in file, which is the better place for this information, and thus the switch here is no longer needed.  kai, feb'24
	 */
	@Deprecated // set directly in file.
	@StringGetter( INPUT_CRS )
	public String getInputCRS() {
		return inputCRS;
	}
	// I think that this should be deprecated since the same functionality can be achieved by writing it directly into the corresponding file.  kai, feb'24

	/**
	 * @deprecated Coordinate System can now be set directly in file, which is the better place for this information, and thus the switch here is no longer needed.  kai, feb'24
	 */
	@Deprecated // set directly in file
	@StringSetter( INPUT_CRS )
	public void setInputCRS(String inputCRS) {
		this.inputCRS = inputCRS;
	}
}
