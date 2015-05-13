/* *********************************************************************** *
 * project: org.matsim.*
 * RandomJointLocationChoiceConfigGroup.java
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
package playground.thibautd.socnetsim.jointactivities.replanning.modules.randomlocationchoice;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.matsim.core.config.experimental.ReflectiveConfigGroup;

/**
 * @author thibautd
 */
public class RandomJointLocationChoiceConfigGroup extends ReflectiveConfigGroup {
	public final static String GROUP_NAME = "randomGroupLocationChoice";

	private Collection<String> types = Collections.singleton( "leisure" );
	// distance under which 95 % of the sampled points should be from the barycenter
	// of the mutated locations
	// (conversion to standad dev s using the fact that upperLimit95 ~= 2s)
	private double upperLimit95 = 5000;
	private double tieActivationProb = 0.5;

	public RandomJointLocationChoiceConfigGroup() {
		super( GROUP_NAME );
	}

	@Override
	public Map<String, String> getComments() {
		final Map<String, String> map = super.getComments();
		map.put(
				"upperLimit95",
				"the distance to the reference point (barycenter of the mutated locations) will be below this limit in 95% of the cases." );
		return map;
	}

	@StringGetter( "activityTypes" )
	private String getTypesAsString() {
		final StringBuilder builder = new StringBuilder();
		final Iterator<String> strings = types.iterator();
		builder.append( strings.next() );
		while ( strings.hasNext() ) builder.append( ","+strings.next() );
		return builder.toString();
	}

	public Collection<String> getTypes() {
		return this.types;
	}

	@StringSetter( "activityTypes" )
	private void setTypes(final String types) {
		final String[] typesarr = types.split( "," );
		final Collection<String> coll = new HashSet<String>( typesarr.length );
		for ( String s : typesarr ) coll.add( s );
		setTypes( coll );
	}

	public void setTypes(final Collection<String> types) {
		this.types = types != null ? types : Collections.<String>emptySet();
	}

	@StringGetter( "upperLimit95" )
	public double getUpperLimit95() {
		return this.upperLimit95;
	}

	public double getStandardDeviation() {
		return upperLimit95 / 2d;
	}

	@StringSetter( "upperLimit95" )
	public void setUpperLimit95(final double upperLimit95) {
		this.upperLimit95 = upperLimit95;
	}

	@StringGetter( "tieActivationProb" )
	public double getTieActivationProb() {
		return this.tieActivationProb;
	}

	@StringSetter( "tieActivationProb" )
	public void setTieActivationProb(final double tieActivationProb) {
		if ( !(tieActivationProb >= 0 && tieActivationProb <= 1) ) {
			throw new IllegalArgumentException( "proba must be in [0;1], got "+tieActivationProb );
		}
		this.tieActivationProb = tieActivationProb;
	}
}

