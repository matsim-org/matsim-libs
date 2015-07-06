/* *********************************************************************** *
 * project: org.matsim.*
 * GroupSizePreferencesConfigGroup.java
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
package org.matsim.contrib.socnetsim.framework.scoring;

import java.util.Random;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author thibautd
 */
public class GroupSizePreferencesConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "groupSizePreferences";

	private String activityType = "leisure";
	private int seed = 1234;
	private int minPref = 0;
	private int maxPref = 0;

	private double utilityOfMissingContact_util_s = -6 / 3600d;

	public GroupSizePreferencesConfigGroup() {
		super( GROUP_NAME );
	}

	@StringGetter( "activityType" )
	public String getActivityType() {
		return activityType;
	}

	@StringSetter( "activityType" )
	public void setActivityType( String activityType ) {
		this.activityType = activityType;
	}

	@StringGetter( "seed" )
	public int getSeed() {
		return seed;
	}

	@StringSetter( "seed" )
	public void setSeed( int seed ) {
		this.seed = seed;
	}

	@StringGetter( "minPref" )
	public int getMinPref() {
		return minPref;
	}

	@StringSetter( "minPref" )
	public void setMinPref( int minPref ) {
		this.minPref = minPref;
	}

	@StringGetter( "maxPref" )
	public int getMaxPref() {
		return maxPref;
	}

	@StringSetter( "maxPref" )
	public void setMaxPref( int maxPref ) {
		this.maxPref = maxPref;
	}

	public double getUtilityOfMissingContact_util_s() {
		return utilityOfMissingContact_util_s;
	}

	@StringGetter( "utilityOfMissingContact_util_h" )
	public double getUtilityOfMissingContact_util_h() {
		return getUtilityOfMissingContact_util_s() * 3600d;
	}

	public void setUtilityOfMissingContact_util_s( double utilityOfMissingContact_util_s ) {
		this.utilityOfMissingContact_util_s = utilityOfMissingContact_util_s;
	}

	@StringSetter( "utilityOfMissingContact_util_h" )
	public void setUtilityOfMissingContact_util_h( double utilityOfMissingContact_util_h ) {
		setUtilityOfMissingContact_util_s( utilityOfMissingContact_util_h / 3600d );
	}

	@Override
	protected void checkConsistency() {
		super.checkConsistency();
		if ( maxPref < minPref ) throw new IllegalStateException( "max pref "+maxPref+" lower than min "+minPref+"!" );
	}

	public int getPersonPreference( final Person person ) {
		checkConsistency();

		final int personalSeed = seed + person.getId().toString().hashCode();
		final Random random = new Random( personalSeed );
		// AH argues this is necessary to be closer to random...
		for ( int i=0; i < 5; i++ ) random.nextLong();

		return minPref + ((maxPref != minPref) ? random.nextInt( maxPref - minPref ) : 0);
	}
}

