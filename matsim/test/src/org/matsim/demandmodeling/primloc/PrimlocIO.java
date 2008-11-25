/* *********************************************************************** *
 * project: org.matsim.*
 * PrimlocIO.java
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

package org.matsim.demandmodeling.primloc;

import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.HashSet;
import java.util.Map;
import java.util.StringTokenizer;

import org.matsim.utils.io.IOUtils;

import Jama.Matrix;

/**
 * @author Fabrice Marchal
 */
public class PrimlocIO {

	/* File format: Zone_ID	Zone_Attribute */
	static double[] readZoneAttribute( final int numZ, final String filename, final Map<Integer,Integer> zonemap ){
		double[] array = new double[ numZ ];
		try{
			LineNumberReader lnr = new LineNumberReader( IOUtils.getBufferedReader(filename) );
			String line=null;
			while( (line=lnr.readLine()) != null ){
				StringTokenizer st = new StringTokenizer( line );
				int zoneid = zonemap.get(Integer.valueOf(st.nextToken())).intValue();
				double value = Double.parseDouble(st.nextToken());
				array[ zoneid ] = value;
			}
		}
		catch( IOException xc){
			xc.printStackTrace();
		}
		return array;
	}

	/* File format: One record Cij per line */
	static Matrix readMatrix( final String filename, final int nrow, final int ncol ){
		Matrix cij = new Matrix( nrow, ncol );
		try{
			LineNumberReader lnr = new LineNumberReader( IOUtils.getBufferedReader(filename) );
			for( int i=0; i<nrow; i++ )
				for( int j=0; j<ncol; j++ )
					cij.set( i, j, Double.parseDouble(lnr.readLine()) );

		}
		catch( IOException xc){
			xc.printStackTrace();
		}
		return cij;
	}

	static HashSet<Integer> readZoneIDs( final String zoneFileName ){
		HashSet<Integer> zoneids = new HashSet<Integer>();
		try{
			LineNumberReader lnr = new LineNumberReader( IOUtils.getBufferedReader(zoneFileName) );
			String line=null;
			while( (line=lnr.readLine()) != null )
				zoneids.add(Integer.valueOf(line) );
		}
		catch( IOException xc){
			xc.printStackTrace();
			System.exit(-1);
		}
		return zoneids;
	}

	static void saveResults( final double[] X, final Map<Integer,Integer> zonemap, final String filename ){

		double minix=Double.POSITIVE_INFINITY;
		for( int i=0; i<X.length; i++ )
			if( X[i] < minix )
				minix = X[i];
		try{
			FileWriter out = new FileWriter( filename );
			for( Map.Entry<Integer, Integer> entry : zonemap.entrySet() ){
				Integer id = entry.getKey();
				int zone = entry.getValue().intValue();
				double rent = Math.abs( X[zone] - minix );
				out.write( id + "\t" + rent + "\n");
			}
			out.close();
		}
		catch( IOException xc){
			xc.printStackTrace();
		}
	}

	static Matrix readODMatrix( final String  costFileName, final int nrow, final int ncol ){
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
		catch( IOException xc){
			xc.printStackTrace();
			System.exit(-1);
		}
		return cij;
	}
}
