package org.matsim.demandmodeling.primloc;

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
