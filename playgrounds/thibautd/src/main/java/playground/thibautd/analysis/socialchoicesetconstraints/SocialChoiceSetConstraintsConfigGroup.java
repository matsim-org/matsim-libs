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
package playground.thibautd.analysis.socialchoicesetconstraints;

import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.stream.DoubleStream;

/**
 * @author thibautd
 */
public class SocialChoiceSetConstraintsConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "socialChoiceSetConstraintsAnalysis";

	private double minDistanceKm = 1;
	private double maxDistanceKm = 50;
	private int nDistances = 10;

	private String inputCliquesCsvFile = null;

	public SocialChoiceSetConstraintsConfigGroup() {
		super( GROUP_NAME );
	}

	@StringGetter("minDistanceKm")
	public double getMinDistanceKm() {
		return minDistanceKm;
	}

	@StringSetter("minDistanceKm")
	public void setMinDistanceKm( final double minDistanceKm ) {
		this.minDistanceKm = minDistanceKm;
	}

	@StringGetter("maxDistanceKm")
	public double getMaxDistanceKm() {
		return maxDistanceKm;
	}

	@StringSetter("maxDistanceKm")
	public void setMaxDistanceKm( final double maxDistanceKm ) {
		this.maxDistanceKm = maxDistanceKm;
	}

	@StringGetter("nDistances")
	public int getNDistances() {
		return nDistances;
	}

	@StringSetter("nDistances")
	public void setNDistances( final int nDistances ) {
		this.nDistances = nDistances;
	}

	@StringGetter("inputCliquesCsvFile")
	public String getInputCliquesCsvFile() {
		return inputCliquesCsvFile;
	}

	@StringSetter("inputCliquesCsvFile")
	public void setInputCliquesCsvFile( final String inputCliquesCsvFile ) {
		this.inputCliquesCsvFile = inputCliquesCsvFile;
	}

	public double[] getDistances_m() {
		return DoubleStream
				.iterate( minDistanceKm , d -> d + (maxDistanceKm - minDistanceKm) / nDistances )
				.map( d -> d * 1000 )
				.limit( nDistances )
				.toArray();
	}
}
