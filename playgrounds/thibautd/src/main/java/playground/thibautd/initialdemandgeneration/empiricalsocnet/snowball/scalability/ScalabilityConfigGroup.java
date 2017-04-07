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
package playground.thibautd.initialdemandgeneration.empiricalsocnet.snowball.scalability;

import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.stream.DoubleStream;

/**
 * @author thibautd
 */
class ScalabilityConfigGroup extends ReflectiveConfigGroup {
	private int nTries = 1;

	private enum Coverage {uniform,power}
	private Coverage coverage = Coverage.uniform;
	private int nPoints = 10;

	private double firstSample = 0;
	private double lastSample = 1;
	private double powerBase = 2;


	public ScalabilityConfigGroup() {
		super( "scalability" );
	}

	public double[] getSamples() {
		switch ( coverage ) {
			case uniform:
				final double step = (lastSample - firstSample) / nPoints;
				return DoubleStream.iterate( firstSample + step , c -> c + step )
						.limit( nPoints )
						.toArray();
			case power:
				return DoubleStream.iterate( lastSample , c -> c / powerBase )
						.limit( nPoints )
						.sorted() // not necessary, but nicer
						.toArray();
			default:
				throw new RuntimeException( coverage.toString() );
		}
	}

	@StringGetter("lastSample")
	private double getLastSample() {
		return lastSample;
	}

	@StringSetter("lastSample")
	private void setLastSample( final double lastSample ) {
		if ( lastSample < 0 || lastSample > 1 ) throw new IllegalArgumentException( lastSample+" not in [0,1]" );
		this.lastSample = lastSample;
	}

	@StringGetter("coverage")
	private Coverage getCoverage() {
		return coverage;
	}

	@StringSetter("coverage")
	private void setCoverage( final Coverage coverage ) {
		this.coverage = coverage;
	}

	@StringGetter("nPoints")
	private int getnPoints() {
		return nPoints;
	}

	@StringSetter("nPoints")
	private void setnPoints( final int nPoints ) {
		this.nPoints = nPoints;
	}

	@StringGetter("nTries")
	public int getnTries() {
		return nTries;
	}

	@StringSetter("nTries")
	public void setnTries( final int nTries ) {
		this.nTries = nTries;
	}

	@StringGetter("powerBase")
	private double getPowerBase() {
		return powerBase;
	}

	@StringSetter("powerBase")
	private void setPowerBase( final double powerBase ) {
		if ( powerBase <= 1 ) throw new IllegalArgumentException( "power base "+powerBase+" < 1" );
		this.powerBase = powerBase;
	}

	@StringGetter("firstSample")
	private double getFirstSample() {
		return firstSample;
	}

	@StringSetter("firstSample")
	private void setFirstSample( final double firstSample ) {
		this.firstSample = firstSample;
	}
}
