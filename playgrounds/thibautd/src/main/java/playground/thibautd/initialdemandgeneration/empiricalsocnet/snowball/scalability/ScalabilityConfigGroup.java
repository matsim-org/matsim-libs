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

	private enum Coverage {uniform,powertwo}
	private Coverage coverage = Coverage.uniform;
	private int nPoints = 10;

	public ScalabilityConfigGroup() {
		super( "scalability" );
	}

	public double[] getSamples() {
		switch ( coverage ) {
			case uniform:
				final double step = 1d / nPoints;
				return DoubleStream.iterate( step , c -> c + step )
						.limit( nPoints )
						.toArray();
			case powertwo:
				return DoubleStream.iterate( 1d , c -> c / 2d )
						.limit( nPoints )
						.sorted() // not necessary, but nicer
						.toArray();
			default:
				throw new RuntimeException( coverage.toString() );
		}
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

}
