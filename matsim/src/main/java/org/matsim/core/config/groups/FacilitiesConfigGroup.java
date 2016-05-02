/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesConfigGroup.java
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

/**
 * @author mrieser / Senozon AG
 */
public final class FacilitiesConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "facilities";

	private static final String INPUT_FILE= "inputFacilitiesFile";
	private static final String INPUT_FACILITY_ATTRIBUTES_FILE = "inputFacilityAttributesFile";
	private static final String INPUT_CRS = "inputCRS";

	private String inputFile = null;
	private String inputFacilitiesAttributesFile = null;
	private String inputCRS = null;

	public FacilitiesConfigGroup() {
		super(GROUP_NAME);
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

	@StringGetter( INPUT_FACILITY_ATTRIBUTES_FILE )
	public String getInputFacilitiesAttributesFile() {
		return this.inputFacilitiesAttributesFile;
	}

	@StringSetter( INPUT_FACILITY_ATTRIBUTES_FILE )
	public void setInputFacilitiesAttributesFile(String inputFacilitiesAttributesFile) {
		this.inputFacilitiesAttributesFile = inputFacilitiesAttributesFile;
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
