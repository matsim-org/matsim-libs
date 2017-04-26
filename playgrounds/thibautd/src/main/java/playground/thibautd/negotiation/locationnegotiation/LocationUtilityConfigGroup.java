/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.thibautd.negotiation.locationnegotiation;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author thibautd
 */
public class LocationUtilityConfigGroup extends ReflectiveConfigGroup {
	private static final String GROUP_NAME = "locationUtility";

	@StringGetter("contactErrorTermDistribution")
	public DistributionType getContactErrorTermDistribution() {
		return contactErrorTermDistribution;
	}

	@StringSetter("contactErrorTermDistribution")
	public void setContactErrorTermDistribution( final DistributionType contactErrorTermDistribution ) {
		this.contactErrorTermDistribution = contactErrorTermDistribution;
	}

	@StringGetter("facilityErrorTermDistribution")
	public DistributionType getFacilityErrorTermDistribution() {
		return facilityErrorTermDistribution;
	}

	@StringSetter("facilityErrorTermDistribution")
	public void setFacilityErrorTermDistribution( final DistributionType facilityErrorTermDistribution ) {
		this.facilityErrorTermDistribution = facilityErrorTermDistribution;
	}

	public enum TravelTimeType { crowFly }
	public enum DistributionType {normal,uniform}

	private TravelTimeType travelTimeType = TravelTimeType.crowFly;
	private double betaDistance = -1;
	private double fixedUtilContact = 1;
	private double muContact = 1;
	private double sigmaFacility = 1;

	private DistributionType contactErrorTermDistribution = DistributionType.normal;
	private DistributionType facilityErrorTermDistribution = DistributionType.normal;

	public LocationUtilityConfigGroup( ) {
		super( GROUP_NAME );
	}

	@StringGetter("travelTimeType")
	public TravelTimeType getTravelTimeType() {
		return travelTimeType;
	}

	@StringSetter("travelTimeType")
	public void setTravelTimeType( final TravelTimeType travelTimeType ) {
		this.travelTimeType = travelTimeType;
	}

	@StringGetter("betaDistance")
	public double getBetaDistance() {
		return betaDistance;
	}

	@StringSetter("betaDistance")
	public void setBetaDistance( final double betaDistance ) {
		this.betaDistance = betaDistance;
	}

	@StringGetter("muContact")
	public double getMuContact() {
		return muContact;
	}

	@StringGetter("fixedUtilContact")
	public double getFixedUtilContact() {
		return fixedUtilContact;
	}

	@StringSetter("fixedUtilContact")
	public void setFixedUtilContact( final double fixedUtilContact ) {
		this.fixedUtilContact = fixedUtilContact;
	}

	@StringSetter("muContact")
	public void setMuContact( final double muContact ) {
		this.muContact = muContact;
	}

	@StringGetter("sigmaFacility")
	public double getSigmaFacility() {
		return sigmaFacility;
	}

	@StringSetter("sigmaFacility")
	public void setSigmaFacility( final double sigmaFacility ) {
		this.sigmaFacility = sigmaFacility;
	}
}
