package playground.fabrice.primloc;


import Jama.Matrix;

public class PrimlocCalibrationErrorMatrix implements PrimlocCalibrationError {

	Matrix reference;
	
	public PrimlocCalibrationErrorMatrix( Matrix reference ){
		this.reference = reference;
	}
	
	public double error( Matrix trips, double[] rents ) {
		// We assume than both reference and trips matrix are
		// based on the same ranking of zones
		
		return trips.minus(reference).normF() ;
		
	}
}
