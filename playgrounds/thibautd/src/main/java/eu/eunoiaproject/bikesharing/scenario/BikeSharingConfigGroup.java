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
 * Stores the parameters from the config file for a bike sharing simulation.
 * @author thibautd
 */
public class BikeSharingConfigGroup extends ReflectiveModule {
	public static final String GROUP_NAME = "bikeSharing";

	private String facilitiesAttributesFile = null;
	private String facilitiesFile = null;
	private double searchRadius = 500;

	public BikeSharingConfigGroup() {
		super( GROUP_NAME );
	}

	@StringGetter( "facilitiesAttributesFile" )
	public String getFacilitiesAttributesFile() {
		return this.facilitiesAttributesFile;
	}

	@StringSetter( "facilitiesAttributesFile" )
	public void setFacilitiesAttributesFile(String facilitiesAttributesFile) {
		this.facilitiesAttributesFile = facilitiesAttributesFile;
	}

	@StringGetter( "facilitiesFile" )
	public String getFacilitiesFile() {
		return this.facilitiesFile;
	}

	@StringSetter( "facilitiesFile" )
	public void setFacilitiesFile(final String facilitiesFile) {
		this.facilitiesFile = facilitiesFile;
	}

	@StringGetter( "searchRadius" )
	public double getSearchRadius() {
		return this.searchRadius;
	}

	@StringSetter( "searchRadius" )
	public void setSearchRadius(double searchRadius) {
		this.searchRadius = searchRadius;
	}
}

