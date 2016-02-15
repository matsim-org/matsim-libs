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
package eu.eunoiaproject.bikesharing.framework.scenario;

import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.Map;

/**
 * Stores the parameters from the config file for a bike sharing simulation.
 * @author thibautd
 */
public class BikeSharingConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "bikeSharing";

	private String facilitiesAttributesFile = null;
	private String facilitiesFile = null;
	private double searchRadius = 500;
	private double ptSearchRadius = 5000;
	
	private double initialBikesRate = 1;
	private double capacityRate = 1;

	public BikeSharingConfigGroup() {
		super( GROUP_NAME );
	}

	@Override
	public Map<String, String> getComments() {
		final Map<String, String> comments = super.getComments();

		comments.put( "searchRadius" , "the radius of the circles, centered on the origin and destination, within which the bike sharing stations will be seached for. In meters." );
		comments.put( "ptSearchRadius" , "the radius of the circles, centered on the origin and destination, within which the public transport stops to be accessed or egressed by bike sharing will be searched for. In meters." );

		return comments;
	}

	@StringGetter( "facilitiesAttributesFile" )
	public String getFacilitiesAttributesFile() {
		return this.facilitiesAttributesFile;
	}

	@StringSetter( "facilitiesAttributesFile" )
	public void setFacilitiesAttributesFile(final String facilitiesAttributesFile) {
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
	public void setSearchRadius(final double searchRadius) {
		this.searchRadius = searchRadius;
	}

	@StringGetter( "ptSearchRadius" )
	public double getPtSearchRadius() {
		return this.ptSearchRadius;
	}

	@StringSetter( "ptSearchRadius" )
	public void setPtSearchRadius(final double ptSearchRadius) {
		this.ptSearchRadius = ptSearchRadius;
	}

	@StringGetter( "initialBikesRate" )
	public double getInitialBikesRate() {
		return initialBikesRate;
	}

	@StringSetter( "initialBikesRate" )
	public void setInitialBikesRate(final double initialBikesRate) {
		if ( initialBikesRate < 0 || initialBikesRate > 1 ) throw new IllegalArgumentException( ""+initialBikesRate );
		this.initialBikesRate = initialBikesRate;
	}

	@StringGetter( "capacityRate" )
	public double getCapacityRate() {
		return capacityRate;
	}

	@StringSetter( "capacityRate" )
	public void setCapacityRate(final double capacityRate) {
		if ( capacityRate < 0 || capacityRate > 1 ) throw new IllegalArgumentException( ""+capacityRate );
		this.capacityRate = capacityRate;
	}
}

