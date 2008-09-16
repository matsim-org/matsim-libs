package org.matsim.demandmodeling.primloc;


import Jama.Matrix;

public class PrimlocCalibrationErrorMatrix implements PrimlocCalibrationError {

	Matrix reference;
	
	public PrimlocCalibrationErrorMatrix( Matrix reference ){
		this.reference = reference;
	}
	
	public double error( Matrix trips, double[] rents ) {
		// We assume than both reference and trips matrix are
		// based on the same ranking of zones
		int numZ = reference.getColumnDimension();
		double sum=0.0;
		for( int i=0;i<numZ;i++){
			for( int j=0;j<numZ;j++){
				double s = reference.get(i,j);
				if( s>0.0 ){
					double dt = reference.get(i, j)-trips.get(i, j);
					sum += dt*dt;
				}
			}
		}
		return Math.sqrt(sum/(numZ*numZ));
		
		//TODO
		// return trips.minus(reference).norm2 / normF ?
		// check consistencyproblems of Cii
	}
}
