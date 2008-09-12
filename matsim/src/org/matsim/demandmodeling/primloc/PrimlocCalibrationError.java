package org.matsim.demandmodeling.primloc;

public interface PrimlocCalibrationError {
	// Return the calibation error measurement based
	// on available data (e.g. trip by distance histogram)
	double error();
}
