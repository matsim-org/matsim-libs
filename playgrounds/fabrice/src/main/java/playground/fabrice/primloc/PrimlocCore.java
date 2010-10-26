/* *********************************************************************** *
 * project: org.matsim.*
 * PrimlocCore.java
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

import java.text.DecimalFormat;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.MatsimRandom;

import Jama.Matrix;

/**
 * Solves primary location choice with capacity constraints.
 *
 * The method is described in : F. Marchal (2005), "A trip generation method for time-dependent
 * large-scale simulations of transport and land-use", Networks and Spatial Economics 5(2),
 * Kluwer Academic Publishers, 179-192.
 *
 * @author Fabrice Marchal
 *
 */
public class PrimlocCore {

	//
	// Main data structures
	//

	int numZ;    // number of zones
	double N;    // total number of trips

	double P[];  // number of residents per zone
	double J[];  // number of jobs per zone
	double X[];  // rent patterns

	Matrix cij;  // Input:		zone to zone travel costs
	Matrix ecij; // Internal:	exp( - cij/ mu )
	Matrix trips;  // Output:	Trip matrix

	//
	// Parameters of the solver
	//

	int maxiter=100;
	double mu=1.0;
	double theta=0.5;
	double threshold1 = 1E-2;
	double threshold2 = 1E-6;
	double threshold3 = 1E-2;
	DecimalFormat df;
	boolean verbose;

	PrimlocCalibrationError calib;

	// Travel Cost statistics
	double minCost, maxCost, avgCost, stdCost;

	private final static Logger log = Logger.getLogger(PrimlocCore.class);

	public PrimlocCore(){
		df = (DecimalFormat) DecimalFormat.getInstance();
		df.applyPattern("0.###E0");
	}

	public void setCalibrationError( PrimlocCalibrationError calib ){
		this.calib = calib;
	}

	public void runModel(){

		log.info("PLCM: Running model with mu = "+df.format(mu));

		setup_expCij();
		initRent();

		if( verbose ){
			log.info("PLCM:\tInitial rent statistics:");
			rentStatistics();
		}

		iterativeSubstitutions();
		solvequs();

		computeTripMatrix();
		computeFinalRents();

	}

	public void runCalibrationProcess(){

		double muC = mu;
		double muL = mu;
		double muR = mu;
		double errC = 0.0;
		double errL = Double.NEGATIVE_INFINITY;
		double errR = Double.NEGATIVE_INFINITY;


		runModel();
		errC = calib.error( trips, X );

		log.info("PLCM:\tCalibration loop. Initial condition run completed");
		calibrationLoopInfo( mu, errC );

		while( errL < errC ){
			mu = muL = mu/2;
			runModel();
			errL = calib.error( trips, X );
			calibrationLoopInfo( mu, errL );
			if( errL < errC ){
				errR = errC;
				errC = errL;
				errL = Double.NEGATIVE_INFINITY;
				muR = muC;
				mu = muC = muL;
			}
		}
		mu = muR;
		while( errR < errC ){
			mu = muR = 2*mu;
			runModel();
			errR = calib.error( trips, X );
			calibrationLoopInfo( mu, errR );
		}

		double omuL = muL;
		double omuC = muC;
		double omuR = muR;
		int count=0;
		while( (count++<maxiter) && (Math.min( errR-errC, errL-errC)/errC > threshold3) ){
			mu = (muC+muL)/2;
			runModel();
			double err = calib.error( trips, X );
			calibrationLoopInfo( mu, err );
			if( err< errC ){
				muR = muC;
				errR = errC;
				muC = mu;
				errC = err;
			}
			else if( err < errL ){
				muL = mu;
				errL = err;
			}
			mu = (muC+muR)/2;
			runModel();
			err = calib.error( trips, X );
			calibrationLoopInfo( mu, err );
			if( err< errC ){
				muL = muC;
				errL = errC;
				muC = mu;
				errC = err;
			}
			else if( err < errR ){
				muR = mu;
				errR = err;
			}
			if( (omuL==muL)&&(omuC==muC)&&(omuR==muR)){
				muC = (muL+muR)/2+(MatsimRandom.getRandom().nextDouble()-0.5)*(muR-muL)/10;
			}
			omuL = muL;
			omuC = muC;
			omuR = muR;
		}
	}

	void calibrationLoopInfo( double mu, double err){
		log.info("PLCM:\tCalibration loop: mu = "+df.format(mu)+",\t error = "+df.format(err));
	}


	void normalizeJobVector(){
		double sumJ=0.0;
		for( int i=0;i<numZ;i++)
			sumJ += J[i];
		sumJ = N/sumJ;
		for( int i=0;i<numZ;i++)
			J[i] = J[i]*sumJ;
	}

	private void computeTripMatrix(){
		// Compute the logsums Zk
		double Z[] = new double[ numZ ];
		for( int j=0; j<numZ; j++ )
			for( int k=0; k<numZ; k++ )
				Z[j] += ecij.get(k, j) * X[k];

		// Compute the O-D matrix Trips
		trips = new Matrix( numZ, numZ );
		for(int i=0;i<numZ;i++)
			for( int j=0;j<numZ;j++)
				trips.set(i, j, J[j] * ecij.get(i, j) * X[i] / Z[j] );
	}

	private void computeFinalRents(){
		// Restore rent values in X
		double minir = Double.POSITIVE_INFINITY;

		// renormalize according to initial values
		for( int i=0; i<numZ; i++)
			if(P[i] > 0)
				X[i]=X[i]/P[i];


		for( int i=0; i<numZ; i++){
			X[i] = -mu*Math.log(X[i]);
			if( minir > X[i] )
				minir = X[i];
		}
		for( int i=0; i<numZ; i++)
			X[i] -= minir;
	}


	private void initRent(){
		X = new double[ numZ ];
		// Initialize rent with the solution of the
		// problem without transportation cost
		int undefs=0;
		for( int i=0; i<numZ; i++ ){
			X[i]=P[i];
			if( X[i] == 0.0 )
				undefs++;
		}
		if( undefs> 0 )
			log.warn("rent undefined in "+undefs+" locations");
	}

	private void rentStatistics(){

		double minix=Double.POSITIVE_INFINITY;
		double maxix=Double.NEGATIVE_INFINITY;
		double sumR1=0.0;
		double sumR2=0.0;

		int count=0;

		for( int i=0; i<numZ; i++ ){
			if( X[i] == 0 )
				continue;
			if( X[i] < minix )
				minix = X[i];
			if( X[i] > maxix )
				maxix = X[i];
			double ri = Math.log(X[i]);
			sumR1 += ri;
			sumR2 += ri*ri;
			count++;
		}

		double sigmaR = mu * Math.sqrt( 1.0/count*sumR2 - 1.0/(count*count)*sumR1*sumR1 );
		double minR = -mu*Math.log(maxix);
		double maxR = -mu*Math.log(minix);
		log.info("PLCM:\t\tRent interval: [ "+ df.format(minR) + ", " + df.format(maxR) + " ]\t std.dev. = " + df.format(sigmaR) );
	}

	private void setup_expCij(){
		ecij = new Matrix( numZ, numZ );
		for( int i=0; i<numZ; i++)
			for( int j=0; j<numZ; j++)
				ecij.set(i,j,Math.exp(-cij.get(i, j)/mu));
	}

	private void iterativeSubstitutions(){
		Matrix PR = new Matrix( numZ, numZ );
		double[] Z  = new double[ numZ ];
		double[] DX = new double[ numZ ];

		//
		// External data: P[i], J[i], C[ij]
		//
		// Unkown: X[i] where X[i] = exp( -rent[i] /mu )
		//
		// Let PR[i][j] = X[i] * exp( ( -1.0 * C[ij] )
		//
		// Z[j] = Sum_k ( p[jk] )
		//
		// T[ij] = J[j] * p[ij] / Z[i]
		//
		// DX[i] = Sum_j( T[ij] ) - P[i]
		//
		// X' = X - Theta * DX[i]
		//

		for( int iterations=0; iterations<maxiter; iterations++){
			int i;

//			#pragma omp parallel for
			for(i=0;i<numZ;i++){
				for(int j=0;j<numZ;j++)
					PR.set(i, j, ecij.get(i,j)*X[i] );
				Z[i] = 0.0;
			}

//			#pragma omp parallel for
			for(i=0;i<numZ;i++)
				for(int k=0;k<numZ;k++)
					Z[i] +=  PR.get(k, i);


//			#pragma omp parallel for
			for(i=0;i<numZ;i++){
				DX[i] = 0.0;
				for(int j=0;j<numZ;j++)
					DX[i] += J[j] * PR.get(i, j) / Z[j];
				DX[i] = DX[i] - P[i];
			}

			double sumx = 0.0;
			double residual = 0.0;
			for(i=0;i<numZ;i++){
				if( X[i] == 0.0 )
					continue;
				sumx += X[i]*X[i];
				residual += ( DX[i] * DX[i] );
				double fac=1.0;
				while( fac*theta*DX[i] >= X[i] )
					fac = fac / 10.0;
				X[i] = X[i] - fac * theta * DX[i];
			}
			residual = Math.sqrt(residual/sumx);
			if (verbose && ( (iterations %10 == 0) || (residual < threshold1 ) )) {
				log.info("PLCM:\t\tResidual = " + df.format(residual) +" (iterative substitution #" + iterations + " )");
				rentStatistics();
			}
			if( residual < threshold1 )
				break;
		}
	}

	private void solvequs(){
		// Since X[i] are known up to a multiplicative factor,
		// we assume that X[numZ-1] is known and
		// X[numZ-1] = P[numZ-1]
		int numR=numZ-1;
		int count=0;
		double residual = Double.POSITIVE_INFINITY;

		while( (count++ < maxiter) && (residual>threshold2) ){
			double[] Z = new double[ numZ ];
			Matrix A = new Matrix( numR, numR ); // Jacobi Matrix
			double[] B = new double[numR];

			// Compute the logsums Zk

			//#pragma omp parallel for
			for( int j=0; j<numZ; j++ ){
				Z[j] = 0.0;
				for( int k=0; k<numZ; k++ )
					Z[j] += ecij.get(k, j) * X[k];
			}

			// Compute the RHS
			//#pragma omp parallel for
			for( int i=0; i<numR; i++ ){
				B[i] = 0.0;
				for( int j=0; j<numZ; j++ )
					B[i] += ( ecij.get(i, j) * J[j] ) / Z[j];
				B[i] = B[i]*X[i];
				B[i] = B[i]-P[i];
			}

			// Compute the Jacobi matrix A
			//#pragma omp parallel for
			for( int i=0; i<numR; i++ ){
				for( int j=0; j<numR; j++ ){
					if( i == j ){
						double sum=0.0;
						for( int k=0; k<numZ; k++ )
							sum += ( ecij.get(i, k) * J[k] / Z[k] );
						A.set(i, j, sum);
					}

					double tsum=0.0;
					for( int k=0; k<numZ; k++ )
						tsum += ( ecij.get(i, k) * ecij.get(j, k) * J[k] ) / (Z[k]*Z[k] );
					A.set(i,j, A.get(i, j)- tsum*X[i] );
				}
			}

			// Solve the linear system
			Matrix b = new Matrix( B, numR );
			long timeMill = System.currentTimeMillis();
			Matrix x = A.solve( b );
			timeMill = System.currentTimeMillis() - timeMill;
			double duration = timeMill / 1000.0;

			for( int i=0; i<numR; i++ )
				X[i] = X[i] - theta*x.get(i, 0);

			// Compute residual
			double sumx=0.0;
			double sumy=0.0;
			for( int i=0; i<numR; i++ ){
				sumx += X[i]*X[i];
				sumy += B[i]*B[i];
			}
			residual =  Math.sqrt( sumy/sumx );
			if( verbose ){
				log.info( "PLCM:\t\tResidual = "+df.format(residual)+" (linear system solved in "+duration+" s)" );
				rentStatistics();
			}

		}
	}



	void setupCostStatistics(){
		minCost = Double.POSITIVE_INFINITY;
		maxCost = Double.NEGATIVE_INFINITY;
		avgCost = stdCost = 0.0;
		for( int i=0; i<numZ; i++ ){
			for( int j=0; j<numZ; j++ ){
				double v=cij.get(i, j);
				minCost = Math.min(minCost, v);
				maxCost = Math.max(maxCost, v);
				avgCost += v;
				stdCost += v*v;
			}
		}
		avgCost = avgCost/(numZ*numZ);
		stdCost = Math.sqrt(stdCost/(numZ*numZ) - avgCost*avgCost);
	}

}