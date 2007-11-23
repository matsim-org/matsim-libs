/* *********************************************************************** *
 * project: org.matsim.*
 * PrimlocEngine.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

/**
 * Solves primary location choice with capacity constraints.
 * The method is described in : F. Marchal (2005), "A trip generation method for time-dependent 
 * large-scale simulations of transport and land-use", Networks and Spatial Economics 5(2),
 * Kluwer Academic Publishers, 179-192.
 * @author Fabrice Marchal
 *
 */

import java.net.URL;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Properties;
import java.util.TreeMap;

import Jama.Matrix;

public class PrimlocEngine {

	/**
	 * @param args
	 */
	
	// 
	// Main data structures
	//
	
	int numZ;    // number of zones
	double N;    // total number of trips
	
	double P[];  // number of residents
	double J[];  // number of jobs
	double X[];  // rent patterns

	Matrix cij;  // Input:		travel impedances
	Matrix ecij; // Internal:	exp( - cij/ mu )
	Matrix trips;  // Output:		Trip matrix
	Matrix calib;  // Input: 		calibration matrix 
	
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
	boolean calibration;
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		SomeIO.testSolver( 800);
		
		testZurich();
	}

	static void testZurich(){
	
		PrimlocEngine plc = new PrimlocEngine();
		
		TreeMap<Integer,Integer> zonemap; // model_ID to internal_ID
		
		zonemap = plc.initProperties( plc.getClass().getResource("/org/matsim/playground/fabrice/primloc/resources/ZurichTestProperties.xml") );
		
		plc.calib = plc.gravityModelMatrix();
		
		plc.calibrationProcess();
		
		SomeIO.saveResults( plc.X, zonemap, "/tmp/rent.txt" );
	}
	
	PrimlocEngine(){
		df = (DecimalFormat) DecimalFormat.getInstance();
		df.applyPattern("0.###E0");
	}
	
	void calibrationProcess(){
		
		double muC = mu;
		double muL = mu;
		double muR = mu;
		double errC = 0.0;
		double errL = Double.NEGATIVE_INFINITY;
		double errR = Double.NEGATIVE_INFINITY;
		
		runModel();
		errC = calibrationError();		
		System.out.println("Mu: "+df.format(mu)+"\tError: "+df.format(errC));
		
		while( errL < errC ){
			mu = muL = mu/2;
			setupECij();
			runModel();		
			errL = calibrationError();
			System.out.println("Mu inf: "+df.format(mu)+"\tError: "+df.format(errL));
			if( errL < errC ){
				errC = errL;
				errL = Double.NEGATIVE_INFINITY;
				muR = muC;
				errR = errC;
				mu = muC = muL;
			}
		}
		mu = muR;
		while( errR < errC ){
			mu = muR = 2*mu;
			setupECij();
			runModel();
			errR = calibrationError();
			System.out.println("Mu sup: "+df.format(mu)+"\tError: "+df.format(errR));
		}
		
		while( Math.min( errR-errC, errL-errC)/errC > threshold3 ){
			mu = (muC+muL)/2;
			setupECij();
			runModel();		
			double err = calibrationError();
			System.out.println("Mu : "+df.format(mu)+"\tError: "+df.format(err));
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
			setupECij();
			runModel();
			err = calibrationError();
			System.out.println("Mu : "+df.format(mu)+"\tError: "+df.format(err));
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
		}
		System.out.println("Final value : "+muC);
	}

	double calibrationError(){
		double sum=0.0;
		for( int i=0;i<numZ;i++){
			for( int j=0;j<numZ;j++){
				double s = calib.get(i,j);
				if( s>0.0 ){
					double dt = calib.get(i, j)-trips.get(i, j);
					sum += dt*dt;
				}
			}
		}
		return Math.sqrt(sum/(numZ*numZ));
	}
	
	
	void normalizeJobVector(){
		double sumJ=0.0;
		for( int i=0;i<numZ;i++)
			sumJ += J[i];
		sumJ = N/sumJ;
		for( int i=0;i<numZ;i++)
			J[i] = J[i]*sumJ;
	}

	void runModel(){
		
		initRent();

		if( verbose ){
			System.out.println("========");
			System.out.println("Initial rent values statistics");
			rentStatistics();
			System.out.println("========");
		}
		
		iterativeSubstitutions();
		solvequs();

		if( verbose ){
			System.out.println("========");
			System.out.println("Final rent values statistics");
			rentStatistics();
			System.out.println("========");
		}
		
		computeTripMatrix();
		computeFinalRents();
	}
	
	void computeTripMatrix(){
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
	
	void computeFinalRents(){
		// Restore rent values in X
		double minir = Double.POSITIVE_INFINITY;
		for( int i=0; i<numZ; i++){
			X[i] = -mu*Math.log(X[i]);
			if( minir > X[i] )
				minir = X[i];
		}
		for( int i=0; i<numZ; i++)
			X[i] -= minir;		
	}

	TreeMap<Integer,Integer> initProperties( URL propurl ){
		
		String zoneFileName=null;
		String costFileName=null;
		String homesFileName=null;
		String jobsFileName=null;
		
		Properties props = new Properties();
		try{
			props.loadFromXML( propurl.openStream() );
			zoneFileName = props.getProperty("zoneFileName");
			costFileName = props.getProperty("costFileName");
			homesFileName = props.getProperty("homesFileName");
			jobsFileName = props.getProperty("jobsFileName");

			maxiter = Integer.parseInt(props.getProperty("maxiter"));
			mu = Double.parseDouble(props.getProperty("mu"));
			theta = Double.parseDouble( props.getProperty("theta"));
			threshold1 = Double.parseDouble(props.getProperty("threshold1"));
			threshold2 = Double.parseDouble(props.getProperty("threshold2"));
			verbose = Boolean.parseBoolean(props.getProperty("verbose"));
		}
		catch( Exception xc ){
			xc.printStackTrace();
			System.exit(-1);
		}
		
		//
		// 1) Read the zone indices
		//	  
		
		HashSet<Integer> zoneids = SomeIO.readZoneIDs(zoneFileName);
		numZ=zoneids.size();
		
		if( verbose ){
			System.out.println("Data:");
			System.out.println(" . #zones:"+numZ);
		}
		
		int idx=0;
		TreeMap<Integer,Integer> zonemap = new TreeMap<Integer,Integer>();
		
		for( Integer id : zoneids )
			zonemap.put( id, idx++ );

		//
		// 2) read the ecij cost file
		// This matrix is full
		//
		// We hack ecij to avoid cii=0
		// cii is select as min_j cij
		//

		cij = SomeIO.readMatrix(costFileName, numZ, numZ);
		for( int i=0; i<numZ; i++ ){
			double mincij = Double.POSITIVE_INFINITY;
			for( int j=0; j<numZ; j++ ){
				double v=cij.get(i, j);
				if( (v < mincij) && (v>0.0) )
					mincij = v;
			}
			if( cij.get(i, i) == 0.0 )
				cij.set(i, i, mincij );
		}
		setupECij();
		
		double meanCost=0.0;
		double stdCost=0.0;
		for( int i=0; i<numZ; i++ ){
			for( int j=0; j<numZ; j++ ){
				double v=cij.get(i, j);
				meanCost += v;
				stdCost += v*v;
			}
		}
		
		meanCost = meanCost/(numZ*numZ);
		stdCost = stdCost/(numZ*numZ) - meanCost*meanCost;
		if( verbose )
			System.out.println(" . Travel costs  mean="+meanCost+" std.dev.= "+Math.sqrt(stdCost));  

		// 
		// 3) read the homes and jobs files
		// These matrices can be sparse
		//
		
		P = SomeIO.readZoneAttribute( numZ, homesFileName, zonemap );
		J = SomeIO.readZoneAttribute( numZ, jobsFileName, zonemap );

		double maxpj=0.0;
		double sp=0.0;
		double sj=0.0;

		for( int i=0; i<numZ; i++ ){
			sp += P[i];
			sj += J[i];
			if( P[i] > maxpj )
				maxpj = P[i];
			if( J[i] > maxpj )
				maxpj = J[i];
		}

		if( Math.abs(sp-sj) > 1.0 ){
			System.err.println("Error: #jobs("+sj+")!= #homes("+sp+")");
			System.exit(-1);
		}
		N=sp;

		if( verbose )
			System.out.println(" . Trip tables: #jobs=#homes= "+N);
		
		return zonemap;
	}

	
	void initRent(){
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
			System.err.println("Warning: rent undefined in "+undefs+" locations");
	}
	
	void rentStatistics(){

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
		System.out.println("Rent: [ "+ df.format(minR) + ", " + df.format(maxR) + " ]\t sigma: " + df.format(sigmaR) );
	}
	
	void setupECij(){
		ecij = new Matrix( numZ, numZ );
		for( int i=0; i<numZ; i++)
			for( int j=0; j<numZ; j++)
				ecij.set(i,j,Math.exp(-cij.get(i, j)/mu));
	}

	void iterativeSubstitutions(){
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
			if( verbose ){
				if( (iterations %10 == 0) || (residual < threshold1 ) ){
					System.out.println("Iteration: " + iterations + " Residual:" + df.format(residual) );
					rentStatistics();      
				}
			}
			if( residual < threshold1 )
				break;
		}
	}
	
	void solvequs(){
		// Since X[i] are known up to a multiplicative factor,
		// we assume that X[numZ-1] is known and
		// X[numZ-1] = P[numZ-1]
		int numR=numZ-1;
		int count=0;
		while( count++ < maxiter ){
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
			
			double sumx=0.0;
			double residual=0.0;
			for( int i=0; i<numR; i++ ){
				sumx += X[i]*X[i];
				residual=residual+B[i]*B[i];
			}
			residual =  Math.sqrt( residual/sumx );	
			if( verbose )
				System.out.println( "Residual:\t"+df.format(residual) );	
			if( residual < threshold2 )
				break;

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
			
			Matrix b = new Matrix( B, numR );			
			long timeMill = System.currentTimeMillis();
			Matrix x = A.solve( b );
			timeMill = System.currentTimeMillis() - timeMill;
			double duration = timeMill / 1000.0;
			if( verbose )
				System.out.println("Solved in:\t"+duration+" s");		

			for( int i=0; i<numR; i++ )
				X[i] = X[i] - theta*x.get(i, 0);
	        
			if( verbose )
				rentStatistics();
			
		}   
	}
	
	
	Matrix gravityModelMatrix(){
		Matrix grav = new Matrix( numZ, numZ );
		double sum1=0.0;
		for( int i=0; i<numZ; i++ ){
			for( int j=0; j<numZ; j++){
				double v = P[i]*J[j] / cij.get(i, j);
				grav.set(i, j, v);
				sum1+=v;
			}
		}
		
		double v=0.0;
		for( int i=0; i<numZ; i++ )
			for( int j=0; j<numZ; j++){
				grav.set(i, j, grav.get(i, j)*N/sum1);
				v+=grav.get(i, j);
			}
		return grav;
	}
	
	double sumofel( Matrix A ){
		double z=0.0;
		for( int i=0; i<A.getRowDimension();i++)
			for( int j=0; j<A.getColumnDimension();j++)
				z+=A.get(i, j);
		return z;
	}
	
	double[] getCijScale( int n ){
		// Return scale of cij
		double[] vals = new double[ n ];
		double max=Double.NEGATIVE_INFINITY;
		double min=Double.POSITIVE_INFINITY;
		for( int i=0;i<numZ;i++){
			for( int j=0;j<numZ;j++){
				double v = cij.get(i, j);
				if( v>max )
					max=v;
				if(v<min)
					min=v;
			}
		}
		for( int i=0; i<n-1; i++)
			vals[i]=min+(i*(max-min))/n;
		return vals;
	}
	
	double[] histogram( Matrix x, double[] vals){
		double[] bins = new double[ vals.length ];		
		for( int i=0;i<numZ;i++){
			for( int j=0;j<numZ;j++){
				double v = x.get(i, j);
				int k=1;
				while( (v>vals[k]) && (k<vals.length-1))
					k++;
				bins[k-1] += x.get(i, j);
			}
		}
		return bins;
	}
	
	double getHistogramError( Matrix x, Matrix y ){
		double error = 0.0;
		double[] scale = getCijScale( 100 );
		double[] u = histogram( x, scale );
		double[] v = histogram( y, scale );
		for( int i=0;i<scale.length;i++)
			error += ( u[i] - v[i] )*( u[i] - v[i] );
		return Math.sqrt( error );
	}
}