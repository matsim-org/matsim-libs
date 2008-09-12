package org.matsim.demandmodeling.primloc;


import Jama.Matrix;

public class PrimlocCalibrationErrorMatrix implements PrimlocCalibrationError {

	Matrix matrix;
	
	public PrimlocCalibrationErrorMatrix( Matrix matrix ){
		this.matrix = matrix;
	}
	
	public double error() {
		// TODO Auto-generated method stub
		return 0;
	}

}
