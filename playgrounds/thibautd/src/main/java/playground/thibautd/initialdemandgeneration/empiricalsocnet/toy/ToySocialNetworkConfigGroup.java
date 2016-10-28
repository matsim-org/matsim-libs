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
package playground.thibautd.initialdemandgeneration.empiricalsocnet.toy;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author thibautd
 */
public class ToySocialNetworkConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "toySocialNetwork";

	private double median_distance_m = 1000;
	private double mean_distance_m = 3000;

	private int cliqueSize = 3;
	private int numberOfCliques = 5;

	private int populationSize = 1000;
	private double width_m = 10000;

	public ToySocialNetworkConfigGroup() {
		super( GROUP_NAME );
	}

	@StringGetter("median_distance_m")
	public double getMedian_distance_m() {
		return median_distance_m;
	}

	@StringSetter("median_distance_m")
	public void setMedian_distance_m( final double median_distance_m ) {
		if ( median_distance_m <= 0 ) throw new IllegalArgumentException( median_distance_m+" < 0" );
		this.median_distance_m = median_distance_m;
	}

	@StringGetter("mean_distance_m")
	public double getMean_distance_m() {
		return mean_distance_m;
	}

	@StringSetter("mean_distance_m")
	public void setMean_distance_m( final double mean_distance_m ) {
		if ( mean_distance_m <= 0 ) throw new IllegalArgumentException( mean_distance_m+" < 0" );
		this.mean_distance_m = mean_distance_m;
	}

	@Override
	protected void checkConsistency() {
		super.checkConsistency();
		if ( median_distance_m > mean_distance_m ) throw new IllegalStateException( "median cannot be higher than mean for lognormal" );
	}

	public double getLognormalLocation_m() {
		return Math.log( median_distance_m );
	}

	public double getLognormalScale_m() {
		if ( mean_distance_m <= median_distance_m ) throw new IllegalStateException( "median cannot be higher than mean for lognormal" );
		return Math.sqrt( 2 * Math.log( mean_distance_m / median_distance_m ) );
	}

	@StringGetter("cliqueSize")
	public int getCliqueSize() {
		return cliqueSize;
	}

	@StringSetter("cliqueSize")
	public void setCliqueSize( final int cliqueSize ) {
		this.cliqueSize = cliqueSize;
	}

	@StringGetter("populationSize")
	public int getPopulationSize() {
		return populationSize;
	}

	@StringSetter("populationSize")
	public void setPopulationSize( final int populationSize ) {
		this.populationSize = populationSize;
	}

	@StringGetter("width_m")
	public double getWidth_m() {
		return width_m;
	}

	@StringSetter("width_m")
	public void setWidth_m( final double width_m ) {
		this.width_m = width_m;
	}

	@StringGetter("numberOfCliques")
	public int getNumberOfCliques() {
		return numberOfCliques;
	}

	@StringSetter("numberOfCliques")
	public void setNumberOfCliques( final int numberOfCliques ) {
		this.numberOfCliques = numberOfCliques;
	}
}
