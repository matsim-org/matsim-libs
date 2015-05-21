/* *********************************************************************** *
 * project: org.matsim.*
 * PrismicLocationChoiceConfigGroup.java
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
package org.matsim.contrib.socnetsim.jointactivities.replanning.modules.prismiclocationchoice;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

import org.matsim.core.config.experimental.ReflectiveConfigGroup;

/**
 * @author thibautd
 */
public class PrismicLocationChoiceConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "prismicLocationChoice";

	private double tieActivationProb = 0.5;
	private double jointPlanBreakageProb = 0.5;
	private Collection<String> types = Collections.singleton( "leisure" );
	private double crowflySpeed = 25 / 3.6; // default: 25 km/h
	private double travelTimeBudget = 3600; // default: 1 hour in total
	private double minimalDistanceFactor = 1.2;
	private int maximumExpansionFactor = 3;

	private SamplingMethod samplingMethod = SamplingMethod.random;
	public static enum SamplingMethod {
		random,
		maximumDistanceMinimization,
		maximumDistanceInverselyProportional,
		maximumDistanceLogit;
	}

	private double maximumDistanceLogitBeta = -0.01;

	public PrismicLocationChoiceConfigGroup() {
		super( GROUP_NAME );
	}

	@StringGetter( "tieActivationProb" )
	public double getTieActivationProb() {
		return this.tieActivationProb;
	}

	@StringSetter( "tieActivationProb" )
	public void setTieActivationProb(double tieActivationProb) {
		if ( tieActivationProb < 0 || tieActivationProb > 1 ) {
			throw new IllegalArgumentException( "invalid probability "+tieActivationProb );
		}
		this.tieActivationProb = tieActivationProb;
	}

	@StringGetter( "jointPlanBreakageProb" )
	public double getJointPlanBreakageProb() {
		return this.jointPlanBreakageProb;
	}

	@StringSetter( "jointPlanBreakageProb" )
	public void setJointPlanBreakageProb(double jointPlanBreakageProb) {
		if ( jointPlanBreakageProb < 0 || jointPlanBreakageProb > 1 ) {
			throw new IllegalArgumentException( "invalid probability "+jointPlanBreakageProb );
		}
		this.jointPlanBreakageProb = jointPlanBreakageProb;
	}

	public Collection<String> getTypes() {
		return this.types;
	}

	@StringGetter( "activityTypes" )
	private String getTypesString() {
		final StringBuilder builder = new StringBuilder();
		final Iterator<String> strings = types.iterator();
		builder.append( strings.next() );
		while ( strings.hasNext() ) builder.append( ","+strings.next() );
		return builder.toString();
	}

	@StringSetter( "activityTypes" )
	private void setTypes(final String types) {
		final String[] typesarr = types.split( "," );
		final Collection<String> coll = new HashSet<String>( typesarr.length );
		for ( String s : typesarr ) coll.add( s );
		setTypes( coll );
	}

	public void setTypes(Collection<String> types) {
		this.types = types;
	}

	@StringGetter( "crowflySpeed" )
	public double getCrowflySpeed() {
		return this.crowflySpeed;
	}

	@StringSetter( "crowflySpeed" )
	public void setCrowflySpeed(final double crowflySpeed) {
		if ( crowflySpeed < 0 ) throw new IllegalArgumentException( "negative speed "+crowflySpeed );
		this.crowflySpeed = crowflySpeed;
	}

	@StringGetter( "minimalDistanceFactor" )
	public double getMinimalDistanceFactor() {
		return this.minimalDistanceFactor;
	}

	@StringSetter( "minimalDistanceFactor" )
	public void setMinimalDistanceFactor(double minimalDistanceFactor) {
		if ( minimalDistanceFactor < 1 ) throw new IllegalArgumentException( "minimal distance cannot be lower than the inter-focus distance: wrong factor "+minimalDistanceFactor );
		this.minimalDistanceFactor = minimalDistanceFactor;
	}

	@StringGetter( "maximumExpansionFactor" )
	public int getMaximumExpansionFactor() {
		return this.maximumExpansionFactor;
	}

	@StringSetter( "maximumExpansionFactor" )
	public void setMaximumExpansionFactor(final int maximumExpansionFactor) {
		if ( maximumExpansionFactor <= 1 ) throw new IllegalArgumentException( maximumExpansionFactor+"too small" );
		this.maximumExpansionFactor = maximumExpansionFactor;
	}

	@StringGetter( "samplingMethod" )
	public SamplingMethod getSamplingMethod() {
		return this.samplingMethod;
	}

	@StringSetter( "samplingMethod" )
	public void setSamplingMethod(SamplingMethod samplingMethod) {
		this.samplingMethod = samplingMethod;
	}

	@StringGetter( "maximumDistanceLogitBeta" )
	public double getMaximumDistanceLogitBeta() {
		return this.maximumDistanceLogitBeta;
	}

	@StringSetter( "maximumDistanceLogitBeta" )
	public void setMaximumDistanceLogitBeta(double maximumDistanceLogitBeta) {
		this.maximumDistanceLogitBeta = maximumDistanceLogitBeta;
	}

	@StringGetter( "travelTimeBudget_s" )
	public double getTravelTimBudget_s() {
		return this.travelTimeBudget;
	}

	@StringSetter( "travelTimeBudget_s" )
	public void setTravelTimeBudget_s(final double travelTimeBudget) {
		if ( travelTimeBudget < 0 ) throw new IllegalArgumentException( "negative time "+travelTimeBudget );
		this.travelTimeBudget = travelTimeBudget;
	}

}

