/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.fabrice.primloc;

import Jama.Matrix;

public class PrimlocTripDistributionError implements PrimlocCalibrationError {

	CumulativeDistribution tripDist; // Trip distribution by costs from external data (e.g. survey)
	Matrix travelCosts; // Matrix of travel costs;
	
	public PrimlocTripDistributionError( CumulativeDistribution tripDist, Matrix travelCosts ){
		this.tripDist = tripDist;
		this.travelCosts = travelCosts;
	}
	
	public double error(Matrix trips, double[] rents) {
		// This class illustrates the computation of the calibration error
		// based on the difference between two cumulative distributions
		
		CumulativeDistribution modelTripDist = new CumulativeDistribution( tripDist.getLowerBound(), tripDist.getUpperBound(), tripDist.getNumBins() );
		int numZ = trips.getColumnDimension();
		for( int i=0; i<numZ; i++)
			for( int j=0; j<numZ; j++)
				modelTripDist.addObservations( travelCosts.get(i, j), trips.get(i, j));
		
		
		return tripDist.error( modelTripDist );
	}

}
