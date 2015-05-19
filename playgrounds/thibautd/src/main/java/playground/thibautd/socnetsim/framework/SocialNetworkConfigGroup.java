/* *********************************************************************** *
 * project: org.matsim.*
 * SocialNetworkConfigGroup.java
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
package playground.thibautd.socnetsim.framework;

import org.matsim.core.config.experimental.ReflectiveConfigGroup;

/**
 * @author thibautd
 */
public class SocialNetworkConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "socialNetwork";

	private String inputFile = null;

	public SocialNetworkConfigGroup() {
		super( GROUP_NAME );
	}

	@StringGetter( "inputFile" )
	public String getInputFile() {
		return this.inputFile;
	}

	@StringSetter( "inputFile" )
	public void setInputFile(final String inputFile) {
		this.inputFile = inputFile;
	}
}

