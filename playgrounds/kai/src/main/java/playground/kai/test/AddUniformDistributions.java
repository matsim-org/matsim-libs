/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.kai.test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

/**
 * @author nagel
 *
 */
public class AddUniformDistributions {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		final int twoto30 = 256*256*256*64 ;
		System.out.println("twoto30: " + twoto30 ) ;
		
		Random rnd = new Random() ;
		
		long[] cnt = new long[30] ;

		for ( long ii=0 ; ii<1000000; ii++ ) {
			double xx = rnd.nextInt(twoto30);
			double yy = rnd.nextInt(twoto30);
			double zz = rnd.nextInt(twoto30) ;
			double sum = xx+yy+zz; 
			sum = sum % twoto30 ;
			double rr = sum/twoto30 ;
			int idx = (int)(10.*rr) ;
			cnt[idx] ++ ;
		}
		
		File file = new File("data.txt") ;
		try {
			FileWriter writer = new FileWriter(file) ;
			for ( int ii=0 ; ii<cnt.length; ii++ ) {
				String str = Integer.toString(ii) + " " + Long.toString(cnt[ii]) + "\n" ;
				System.out.print( str ) ;
				writer.write(str) ;
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		

	}

}
