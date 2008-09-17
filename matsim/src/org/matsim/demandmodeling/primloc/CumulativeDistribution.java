package org.matsim.demandmodeling.primloc;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.StringTokenizer;
import java.util.Vector;

import org.matsim.utils.io.IOUtils;

public class CumulativeDistribution {

	double[] xs;
	double[] ys;
	int numObservations;
	
	public CumulativeDistribution( double lowerBound, double upperBound, int numBins ){
		xs = new double[ numBins+1 ];
		ys = new double[ numBins+1 ];
		for( int i=0; i<numBins+1;i++)
			xs[i]=lowerBound+((upperBound-lowerBound)*i)/numBins;
		ys[numBins]=1.0;
	}
	
	public CumulativeDistribution( double[] xs, double ys[] ){
		assert( xs.length == ys.length );
		this.xs = xs;
		this.ys = ys;
		numObservations = 1;
	}
	
	public void addObservation( double x ){
		addObservations( x, 1.0 );
	}
	
	public double getLowerBound(){
		return xs[0];
	}
	
	public double getUpperBound(){
		return xs[xs.length-1];
	}
	
	public int getNumBins(){
		return xs.length-1;
	}
	
	public double error( CumulativeDistribution dist2 ){
		// return the difference between two distributions with the same support
		double error = 0.0;
		for( int i=0; i<xs.length-1;i++)
			error += ( ys[i] - dist2.ys[i] )*( ys[i] - dist2.ys[i] );
		return Math.sqrt( error );
	}
	
	public void addObservations( double x, double y ){
		int idx=0;
		while( x>=xs[idx] && idx<xs.length-1)
			idx++;
		for( int i=0; i<xs.length;i++){
			if( i< idx )
				ys[i]=(ys[i]*numObservations)/(numObservations+y);
			else
				ys[i]=(ys[i]*numObservations+y)/(numObservations+y);
		}
		idx = 0;
		while( ys[idx++]<1.0 );
		while( idx < ys.length )
			ys[idx++] = 1.0;

		numObservations += y;
	}
	
	public void print(){
		for( int i=0; i<xs.length;i++)
			System.out.println( xs[i]+"\t"+ys[i]);
	}
	
	static CumulativeDistribution readDistributionFromFile( String filename ){
		// Read a cumulative distribution in the folowing format
		// x[i] \t y[i]
		//
		// x0	0.0
		// x1	...
		// x2	...
		// xN	1.0
		//
		Vector<Double> x = new Vector<Double>();
		Vector<Double> y = new Vector<Double>();
		
		LineNumberReader lnr;
		try {
			lnr = new LineNumberReader( IOUtils.getBufferedReader(filename) );
			String line=null;
			while( (line=lnr.readLine()) != null ){
				StringTokenizer st = new StringTokenizer( line );
				x.add( Double.parseDouble(st.nextToken()));
				y.add( Double.parseDouble(st.nextToken()));
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		double[] xs = new double[ x.size()];
		double[] ys = new double[ x.size()];
		for( int i=0; i<x.size();i++){
			xs[i] = x.get(i);
			ys[i] = y.get(i);
		}
		return new CumulativeDistribution( xs, ys );
	}
}
