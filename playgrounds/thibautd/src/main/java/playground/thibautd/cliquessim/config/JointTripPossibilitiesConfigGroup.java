/* *********************************************************************** *
 * project: org.matsim.*
 * JointTripsPossibilitiesConfigGroup.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.cliquessim.config;

import playground.thibautd.utils.ReflectiveModule;

/**
 * @author thibautd
 */
public class JointTripPossibilitiesConfigGroup extends ReflectiveModule {
	public static final String GROUP_NAME = "jointTripPossibilities";

	private String possibilitiesFile = null;

	public JointTripPossibilitiesConfigGroup() {
		super( GROUP_NAME );
	}

	/**
	 * Gets the possibilitiesFile for this instance.
	 *
	 * @return The possibilitiesFile.
	 */
	public String getPossibilitiesFile()
	{
		return this.possibilitiesFile;
	}

	/**
	 * Sets the possibilitiesFile for this instance.
	 *
	 * @param possibilitiesFile The possibilitiesFile.
	 */
	public void setPossibilitiesFile(String possibilitiesFile)
	{
		this.possibilitiesFile = possibilitiesFile;
	}
}

