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
public class LocationAlternativesConfigGroup extends ReflectiveConfigGroup {
	private static final String GROUP_NAME = "locationAlternatives";

	private int nOutOfHomeAlternatives = 100;
	private double maxOutOfHomeRadius_km = 30;

	private String activityType = "leisure";

	public LocationAlternativesConfigGroup( ) {
		super( GROUP_NAME );
	}

	@StringGetter("nOutOfHomeAlternatives")
	public int getnOutOfHomeAlternatives() {
		return nOutOfHomeAlternatives;
	}

	@StringSetter("nOutOfHomeAlternatives")
	public void setnOutOfHomeAlternatives( final int nOutOfHomeAlternatives ) {
		this.nOutOfHomeAlternatives = nOutOfHomeAlternatives;
	}

	@StringGetter("maxOutOfHomeRadius_km")
	public double getMaxOutOfHomeRadius_km() {
		return maxOutOfHomeRadius_km;
	}

	@StringSetter("maxOutOfHomeRadius_km")
	public void setMaxOutOfHomeRadius_km( final double maxOutOfHomeRadius_km ) {
		this.maxOutOfHomeRadius_km = maxOutOfHomeRadius_km;
	}

	@StringGetter("activityType")
	public String getActivityType() {
		return activityType;
	}

	@StringSetter("activityType")
	public void setActivityType( final String activityType ) {
		this.activityType = activityType;
	}
}
