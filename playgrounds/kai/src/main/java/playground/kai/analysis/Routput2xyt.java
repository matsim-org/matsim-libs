/* *********************************************************************** *
 * project: org.matsim.*												   *
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
package playground.kai.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import org.matsim.core.utils.io.IOUtils;

class Routput2xyt {

	private static final String dir = "/Users/nagel/runs-svn/detEval/exposureInternalization/internalize1pct/output/output_baseCase_ctd/analysis/spatialAveraging/data/movie" ;

	public static void main(String[] args) throws IOException {

		try ( BufferedWriter writer = IOUtils.getBufferedWriter("/Users/nagel/t.txt.gz") ) {
			writer.write( "x" + "\t" + "y" + "\t" + "time" + "\t" + "value\n");

			for ( int time=1800 ; time<=24*3600 ; time+= 1800 ) {
				readAndWriteForGivenTime(writer, time);
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		System.out.println("done");
	}

	private static void readAndWriteForGivenTime(BufferedWriter writer, int time) throws FileNotFoundException {
		String filename = "baseCase.1500.Routput.NOX.g." + time + ".0.txt" ;
		File file = new File( dir + "/" + filename ) ;
		if ( !file.exists() || !file.canRead() ) {
			filename = "baseCase.1500.Routput.NOX.g.0" + time + ".0.txt" ;
			file = new File( dir + "/" + filename ) ;
		}

		ArrayList<String> list = new ArrayList<String>();
		{
			Scanner s = new Scanner( file ) ;
			while (s.hasNextLine()){
				list.add(s.nextLine());
			}
			s.close();
		}
		Iterator<String> it = list.iterator() ;
		List<Double> xx = new ArrayList<Double>() ;
		{
			Scanner firstLineScanner = new Scanner( it.next() ) ;
			firstLineScanner.next() ; // pull empty element
			while ( firstLineScanner.hasNext() ) {
				xx.add( firstLineScanner.nextDouble() ) ;
			}
			firstLineScanner.close();
		}


		while ( it.hasNext() ) { // new line
			Scanner otherLineScanner = new Scanner( it.next() ) ;
			Double yy = otherLineScanner.nextDouble() ; // first column is y coord

			ArrayList<Double> vals = new ArrayList<>() ;
			while( otherLineScanner.hasNext() ) {
				vals.add( otherLineScanner.nextDouble() ) ;
			}

			otherLineScanner.close(); 

			for ( int ii=0 ; ii<xx.size() ; ii++ ) {
				if  ( vals.get(ii) > 0.1 ) {
				try {
					writer.write( xx.get(ii) + "\t" + yy + "\t" + time + "\t" + vals.get(ii) + "\n");
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				}
			}
		}
	}

}
