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

/**
 * @author mrieser / Senozon AG
 */
public final class FacilitiesConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "facilities";

	private static final String INPUT_FILE= "inputFacilitiesFile";
	private static final String INPUT_FACILITY_ATTRIBUTES_FILE = "inputFacilityAttributesFile";

	private String inputFile = null;
	private String inputFacilitiesAttributesFile = null;

	public FacilitiesConfigGroup() {
		super(GROUP_NAME);
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

}
