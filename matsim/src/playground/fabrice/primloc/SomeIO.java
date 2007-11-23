/* *********************************************************************** *
 * project: org.matsim.*
 * SomeIO.java
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

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;
import java.util.HashSet;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TreeMap;

import Jama.Matrix;


public class SomeIO {

	/* File format: Zone_ID	Zone_Attribute */
	static double[] readZoneAttribute( int numZ, String filename, TreeMap<Integer,Integer> zonemap ){
		double[] array = new double[ numZ ];
		try{
			URL url = filename.getClass().getResource(filename);
			LineNumberReader lnr = new LineNumberReader(new InputStreamReader(url.openStream()));	
			String line=null;
			while( (line=lnr.readLine()) != null ){
				StringTokenizer st = new StringTokenizer( line );
				Integer zoneid = zonemap.get(Integer.parseInt(st.nextToken()));
				Double value = new Double(st.nextToken());
				array[ zoneid ] = value;
			}
		}
		catch( Exception xc){
			xc.printStackTrace();
		}
		return array;
	}
	
	/* File format: One record Cij per line */
	static Matrix readMatrix( String filename, int nrow, int ncol ){
		Matrix cij = new Matrix( nrow, ncol );
		try{
			URL url = filename.getClass().getResource(filename);
			LineNumberReader lnr = new LineNumberReader(new InputStreamReader(url.openStream()));	
			for( int i=0; i<nrow; i++ )
				for( int j=0; j<ncol; j++ )					
					cij.set( i, j, Double.parseDouble(lnr.readLine()) );
					
		}
		catch( Exception xc){
			xc.printStackTrace();
		}
		return cij;
	}
	
	static HashSet<Integer> readZoneIDs( String zoneFileName ){
		HashSet<Integer> zoneids = new HashSet<Integer>();
		try{
			URL url = zoneFileName.getClass().getResource(zoneFileName);
			LineNumberReader lnr = new LineNumberReader( new InputStreamReader(url.openStream()));
			String line=null;
			while( (line=lnr.readLine()) != null )
				zoneids.add(Integer.parseInt(line) );
		}
		catch( Exception xc){
			xc.printStackTrace();
		}
		return zoneids;
	}
	
	static void saveResults( double[] X, TreeMap<Integer,Integer> zonemap, String filename ){
		
		double minix=Double.POSITIVE_INFINITY;
		for( int i=0; i<X.length; i++ )
			if( X[i] < minix )
				minix = X[i];
		try{
			FileWriter out = new FileWriter( filename ); 
			for( Integer id : zonemap.keySet() ){
				int zone = zonemap.get(id);
				double rent = Math.abs( X[zone] - minix );
				out.write( id + "\t" + rent + "\n");
			}
			out.close();
		}
		catch( Exception xc){
			xc.printStackTrace();
		}
	}
	
	static void loadprop(){
		Properties props = new Properties();
		props.setProperty( "zoneFileName" , "/Users/fmarchal/archives/piz/fmarchal/research/transportation/landuse/experiments/Zurich/zones.txt" );
		props.setProperty( "costFileName" , "/Users/fmarchal/archives/piz/fmarchal/research/transportation/landuse/experiments/Zurich/costs_IT0.txt" );
		props.setProperty( "homesFileName" , "/Users/fmarchal/archives/piz/fmarchal/research/transportation/landuse/experiments/Zurich/homes.txt" );
		props.setProperty( "jobsFileName" , "/Users/fmarchal/archives/piz/fmarchal/research/transportation/landuse/experiments/Zurich/jobs.txt" );
		props.setProperty( "maxiter" , "100");
		try{
			props.storeToXML( new FileOutputStream("/tmp/meprop.xml"), "Hello" );
		}
		catch( Exception xc ){
			xc.printStackTrace();
		}
	}
	
	

	static void testSolver( int n){
		System.out.println("Testing matrix solver");
		System.out.println("Solving A.X = B for size:"+n);
		Matrix A = Matrix.random( n, n );
		Matrix b = Matrix.random( n, 1 );
		long timeMill = System.currentTimeMillis();
		Matrix x = A.solve( b );
		timeMill = System.currentTimeMillis() - timeMill;
		double duration = timeMill / 1000.0;
		System.out.println("Solved in:\t"+duration+" s");
		
        Matrix Residual = A.times(x).minus(b); 
        double rnorm = Residual.normInf();
        System.out.println( "Residual:\t"+rnorm );
        System.out.println("Test over");
	}
}
