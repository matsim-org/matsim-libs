/* *********************************************************************** *
 * project: org.matsim.*
 * SomeIO.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 * 
 * @author Fabrice Marchal
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

package org.matsim.demandmodeling.primloc;


import java.io.FileWriter;
import java.io.LineNumberReader;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.matsim.utils.io.IOUtils;

import Jama.Matrix;


public class PrimlocIO {

	/* File format: Zone_ID	Zone_Attribute */
	static double[] readZoneAttribute( int numZ, String filename, TreeMap<Integer,Integer> zonemap ){
		double[] array = new double[ numZ ];
		try{
			LineNumberReader lnr = new LineNumberReader( IOUtils.getBufferedReader(filename) );
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
			LineNumberReader lnr = new LineNumberReader( IOUtils.getBufferedReader(filename) );	
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
			LineNumberReader lnr = new LineNumberReader( IOUtils.getBufferedReader(zoneFileName) );
			String line=null;
			while( (line=lnr.readLine()) != null )
				zoneids.add(Integer.parseInt(line) );
		}
		catch( Exception xc){
			xc.printStackTrace();
			System.exit(-1);
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
	
	

	
	static Matrix readODMatrix( String  costFileName, int nrow, int ncol ){
//		readODMatrix method for csv file with ";" separate
//		File format: origin; destination; travel time;/
		String inputString = null, value= null;
		int col = 0;
		Matrix cij = new Matrix( nrow, ncol );
		
		try{
			LineNumberReader lnr = new LineNumberReader( IOUtils.getBufferedReader(costFileName) );
			for (int i=0; i<nrow; i++)
				for (int j=0;j<ncol;j++){
					inputString = lnr.readLine();
					int found = -2 ;
					while (found != -1){
						found = inputString.indexOf(";");
						if (found != -1) {
							value = inputString.substring(0,found);
							inputString = inputString.substring(found+1);						
						}
						else{
							value = inputString;
						}
						col = col+1;
						if (col == 3){
							if(value.compareTo("")!=0) cij.set( i, j, Double.parseDouble(value));
							else cij.set(i, j, 0);
						}
					}	
					col = 0;
			}
		}
		catch( Exception xc){
			xc.printStackTrace();
			System.exit(-1);
		}
		return cij;
	}
}
