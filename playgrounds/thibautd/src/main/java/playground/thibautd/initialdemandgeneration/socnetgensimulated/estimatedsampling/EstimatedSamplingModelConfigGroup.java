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
package playground.thibautd.initialdemandgeneration.socnetgensimulated.estimatedsampling;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author thibautd
 */
public class EstimatedSamplingModelConfigGroup extends ReflectiveConfigGroup {
	private static final String GROUP_NAME = "estimatedSamplingUtility";

	private double b_logDist = -0.5271;
	private double b_sameGender = 2.7568;
	private double b_ageDiff = -0.0955;

	private Transformation distanceTransformation = Transformation.log;
	private Transformation ageTransformation = Transformation.linear;

	private double primarySample = 15682.0677;

	public enum Transformation { linear, log; }

	public EstimatedSamplingModelConfigGroup() {
		super(GROUP_NAME);
	}

	@StringGetter("b_logDist")
	public double getB_logDist() {
		return b_logDist;
	}

	@StringSetter("b_logDist")
	public void setB_logDist(double b_logDist) {
		this.b_logDist = b_logDist;
	}

	@StringGetter("b_sameGender")
	public double getB_sameGender() {
		return b_sameGender;
	}

	@StringSetter("b_sameGender")
	public void setB_sameGender(double b_sameGender) {
		this.b_sameGender = b_sameGender;
	}

	@StringGetter("b_ageDiff")
	public double getB_ageDiff() {
		return b_ageDiff;
	}

	@StringSetter("b_ageDiff")
	public void setB_ageDiff(double b_ageDiff) {
		this.b_ageDiff = b_ageDiff;
	}

	@StringGetter("primarySample")
	public double getPrimarySample() {
		return primarySample;
	}

	@StringSetter("primarySample")
	public void setPrimarySample( double primarySample ) {
		if ( primarySample < 0 ) throw new IllegalArgumentException( "negative sample size "+primarySample );
		this.primarySample = primarySample;
	}

	@StringGetter("ageTransformation")
	public Transformation getAgeTransformation() {
		return ageTransformation;
	}

	@StringSetter("ageTransformation")
	public void setAgeTransformation( Transformation ageTransformation ) {
		this.ageTransformation = ageTransformation;
	}

	@StringGetter("distanceTransformation")
	public Transformation getDistanceTransformation() {
		return distanceTransformation;
	}

	@StringSetter("distanceTransformation")
	public void setDistanceTransformation( Transformation distanceTransformation ) {
		this.distanceTransformation = distanceTransformation;
	}

	public static double transform( final Transformation transformation , double value ) {
		switch ( transformation ) {
			case linear: return value;
			case log: return Math.log( value + 1 );
			default: throw new RuntimeException( transformation+" not implemented???");
		}
	}
}
