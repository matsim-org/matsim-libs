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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author fabrice
 * @author jjoubert
 */
public class CumulativeDistribution {

	double[] xs;
	double[] ys;
	int numObservations;

	private final static Logger log = Logger.getLogger(CumulativeDistribution.class);

	public CumulativeDistribution( double lowerBound, double upperBound, int numBins ){
		xs = new double[ numBins+1 ];
		ys = new double[ numBins+1 ];
		for( int i=0; i<numBins+1;i++)
			xs[i]=lowerBound+((upperBound-lowerBound)*i)/numBins;
		ys[numBins]=1.0;
	}

	public CumulativeDistribution( double[] xs, double ys[] ){
		assert( xs.length == ys.length );
		this.xs = xs.clone();
		this.ys = ys.clone();
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
		while( ys[idx++]<1.0 ) {
			// idx is already increased
		}
		while( idx < ys.length )
			ys[idx++] = 1.0;

		numObservations += y;
	}

	public void print(){
		for( int i=0; i<xs.length;i++)
			System.out.println( xs[i]+"\t"+ys[i]);
	}

	public static CumulativeDistribution readDistributionFromFile( String filename ){
		// Read a cumulative distribution in the following format
		// x[i] \t y[i]
		//
		// x0	0.0
		// x1	...
		// x2	...
		// xN	1.0
		//
		Vector<Double> x = new Vector<Double>();
		Vector<Double> y = new Vector<Double>();

		LineNumberReader lnr = null;
		try {
			lnr = new LineNumberReader( IOUtils.getBufferedReader(filename) );
			String line=null;
			while( (line=lnr.readLine()) != null ){
				StringTokenizer st = new StringTokenizer( line );
				x.add( Double.valueOf(st.nextToken()));
				y.add( Double.valueOf(st.nextToken()));
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			if (lnr != null) {
				try { lnr.close(); }
				catch (IOException e) { log.warn("Could not close stream.", e); }
			}
		}
		double[] xs = new double[ x.size()];
		double[] ys = new double[ x.size()];
		for( int i=0; i<x.size();i++){
			xs[i] = x.get(i);
			ys[i] = y.get(i);
		}
		return new CumulativeDistribution( xs, ys );
	}

	/**
	 * Draws a single sample from the cumulative distribution function.
	 *
	 * @return the mid-value of the bin from which the sampled value originates.
	 */
	public double sampleFromCDF(){
		Double d = null;
		double rnd = MatsimRandom.getRandom().nextDouble();

		int index = 1;
		while(d == null && index <= this.getNumBins() ){
			if( this.ys[index] > rnd){
				d = this.xs[index-1] + (this.xs[index] - this.xs[index-1]) / 2;
			} else{
				index++;
			}
		}
		assert(d != null) : "Could not draw from the cumulative distribution function";
		return d;
	}

}
