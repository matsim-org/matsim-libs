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

import java.net.URL;

import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * Config group for households
 * @author dgrether
 */
public final class HouseholdsConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "households";

	private static final String INPUT_FILE= "inputFile";
	private static final String INPUT_HOUSEHOLD_ATTRIBUTES_FILE = "inputHouseholdAttributesFile";
	private static final String INSISTING_ON_USING_DEPRECATED_ATTRIBUTE_FILE = "insistingOnUsingDeprecatedHouseholdsAttributeFile" ;

	public static final String HOUSEHOLD_ATTRIBUTES_DEPRECATION_MESSAGE = "using the separate households attribute file is deprecated.  Add the information directly into each household, using " +
			"the Attributable feature.  If you insist on continuing to use the separate household attribute file, set " +
			"insistingOnUsingDeprecatedFacilityAttributeFile to true.  The file will then be read, but the values " +
			"will be entered into each facility using Attributable, and written as such to output_facilities.";

	private String inputFile = null;
	private String inputHouseholdAttributesFile = null;
	private boolean insistingOnUsingDeprecatedAttributeFile = false;

	public HouseholdsConfigGroup() {
		super(GROUP_NAME);
	}

	/* direct access */
	
	@StringGetter( INPUT_FILE )
	public String getInputFile() {
		return this.inputFile;
	}
	public URL getInputFileURL(URL context) {
		return ConfigGroup.getInputFileURL(context, this.inputFile);
	}

	@StringSetter( INPUT_FILE )
	public void setInputFile(final String inputFile) {
		this.inputFile = inputFile;
	}
	
	@StringGetter( INPUT_HOUSEHOLD_ATTRIBUTES_FILE )
	public String getInputHouseholdAttributesFile() {
		return this.inputHouseholdAttributesFile;
	}

	@StringSetter( INPUT_HOUSEHOLD_ATTRIBUTES_FILE )
	public void setInputHouseholdAttributesFile(String inputHouseholdAttributesFile) {
		this.inputHouseholdAttributesFile = inputHouseholdAttributesFile;
	}

	@StringSetter(INSISTING_ON_USING_DEPRECATED_ATTRIBUTE_FILE)
	public final void setInsistingOnUsingDeprecatedHouseholdsAttributeFile( boolean val ) {
		this.insistingOnUsingDeprecatedAttributeFile = val ;
	}
	@StringGetter(INSISTING_ON_USING_DEPRECATED_ATTRIBUTE_FILE)
	public final boolean isInsistingOnUsingDeprecatedHouseholdsAttributeFile() {
		return insistingOnUsingDeprecatedAttributeFile;
	}
}
