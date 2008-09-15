package org.matsim.demandmodeling.primloc;

import Jama.Matrix;

public interface PrimlocCalibrationError {
	// Return the calibation error measurement based on the difference
	// between available data (e.g. trip by distance histogram)
	// and the results of the Primloc module: rents and trips
	double error( Matrix trips, double[] rents );
}
