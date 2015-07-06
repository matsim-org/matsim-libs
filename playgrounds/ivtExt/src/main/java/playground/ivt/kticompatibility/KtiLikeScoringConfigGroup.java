/* *********************************************************************** *
 * project: org.matsim.*
 * KtiLikeScoringConfigGroup.java
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
package playground.ivt.kticompatibility;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author thibautd
 */
public class KtiLikeScoringConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "ktiLikeScoring";

	private double travelCardRatio = 1;

	public KtiLikeScoringConfigGroup() {
		super( GROUP_NAME );
	}

	@StringSetter( "travelCardDistanceCostRatio" )
	public void setTravelCardRatio(final double r) {
		this.travelCardRatio = r;
	}

	@StringGetter( "travelCardDistanceCostRatio" )
	public double getTravelCardRatio() {
		return this.travelCardRatio; 
	}
}

