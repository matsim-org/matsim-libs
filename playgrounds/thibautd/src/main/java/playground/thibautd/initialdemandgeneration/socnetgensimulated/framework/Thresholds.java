/* *********************************************************************** *
 * project: org.matsim.*
 * Thresholds.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.thibautd.initialdemandgeneration.socnetgensimulated.framework;

/**
 * @author thibautd
 */
public class Thresholds {
	private final double primaryThreshold;
	private final double secondaryThreshold;


	private double resultingAverageDegree = Double.NaN;
	private double resultingClustering = Double.NaN;
	
	public Thresholds(
			final double primaryThreshold,
			final double secondaryThreshold ) {
		this.primaryThreshold = primaryThreshold;
		this.secondaryThreshold = secondaryThreshold;
	}
	
	public double getResultingAverageDegree() {
		if ( Double.isNaN( resultingAverageDegree ) ) throw new IllegalStateException( "no average degree to get" );
		return resultingAverageDegree;
	}

	public void setResultingAverageDegree( double resultingAverageDegree ) {
		if ( !Double.isNaN( resultingAverageDegree ) ) throw new IllegalStateException( "average degree already set" );
		this.resultingAverageDegree = resultingAverageDegree;
	}

	public double getResultingClustering() {
		if ( Double.isNaN( resultingClustering ) ) throw new IllegalStateException( "no clustering set" );
		return resultingClustering;
	}

	public void setResultingClustering( double resultingClustering ) {
		if ( !Double.isNaN( resultingClustering ) ) throw new IllegalStateException( "clustering already set" );
		this.resultingClustering = resultingClustering;
	}

	public double getPrimaryThreshold() {
		return primaryThreshold;
	}

	public double getSecondaryThreshold() {
		return secondaryThreshold;
	}

	@Override
	public String toString() {
		return "[Thresholds: primary="+primaryThreshold+" secondary="+secondaryThreshold+"; clustering="+resultingClustering+"; avgDegree"+resultingAverageDegree+"]";
	}
}

