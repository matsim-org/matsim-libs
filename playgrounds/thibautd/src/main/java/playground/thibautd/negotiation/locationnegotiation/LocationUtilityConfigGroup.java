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

	public enum TravelTimeType { crowFly }

	private TravelTimeType travelTimeType = TravelTimeType.crowFly;
	private double betaTime = -1;
	private double muContact = 1;
	private double sigmaFacility = 1;

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

	@StringGetter("betaTime")
	public double getBetaTime() {
		return betaTime;
	}

	@StringSetter("betaTime")
	public void setBetaTime( final double betaTime ) {
		this.betaTime = betaTime;
	}

	@StringGetter("muContact")
	public double getMuContact() {
		return muContact;
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
