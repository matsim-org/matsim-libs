/* *********************************************************************** *
 * project: org.matsim.*
 * BikeSharingConfigGroup.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package eu.eunoiaproject.bikesharing.scenario;

import org.matsim.core.config.experimental.ReflectiveModule;

/**
 * @author thibautd
 */
public class BikeSharingConfigGroup extends ReflectiveModule {
	public static final String GROUP_NAME = "bikeSharing";

	private String facilitiesFile = null;

	public BikeSharingConfigGroup() {
		super( GROUP_NAME );
	}

	@StringGetter( "facilitiesFile" )
	public String getFacilitiesFile() {
		return this.facilitiesFile;
	}

	@StringSetter( "facilitiesFile" )
	public void setFacilitiesFile(final String facilitiesFile) {
		this.facilitiesFile = facilitiesFile;
	}

}

