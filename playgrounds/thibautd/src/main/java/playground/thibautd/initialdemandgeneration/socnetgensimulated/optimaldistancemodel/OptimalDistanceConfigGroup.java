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
package playground.thibautd.initialdemandgeneration.socnetgensimulated.optimaldistancemodel;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author thibautd
 */
@Singleton
public class OptimalDistanceConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "optimalDistanceScoring";

	private double minDistance_m = 1000;
	private double maxDistance_m = 10000;

	// the higher it is, the less random is the decision
	private double utilityOfMatch = 1000;

	@Inject
	public OptimalDistanceConfigGroup() {
		super( GROUP_NAME );
	}

	@StringGetter( "minDistance_m" )
	public double getMinDistance_m() {
		return minDistance_m;
	}

	@StringSetter( "minDistance_m" )
	public void setMinDistance_m(double minDistance_m) {
		this.minDistance_m = minDistance_m;
	}

	@StringGetter( "maxDistance_m" )
	public double getMaxDistance_m() {
		return maxDistance_m;
	}

	@StringSetter( "maxDistance_m" )
	public void setMaxDistance_m(double maxDistance_m) {
		this.maxDistance_m = maxDistance_m;
	}

	@StringGetter( "utilityOfMatch" )
	public double getUtilityOfMatch() {
		return utilityOfMatch;
	}

	@StringSetter( "utilityOfMatch" )
	public void setUtilityOfMatch(double utilityOfMatch) {
		this.utilityOfMatch = utilityOfMatch;
	}
}
