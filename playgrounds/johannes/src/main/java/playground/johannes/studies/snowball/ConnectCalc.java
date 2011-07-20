/* *********************************************************************** *
 * project: org.matsim.*
 * ConnectCalc.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.studies.snowball;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author illenberger
 *
 */
public class ConnectCalc {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		final int N = 4000000;
		
		BufferedWriter writer = new BufferedWriter(new FileWriter("/Users/jillenberger/Work/socialnets/snowball/connect.txt"));
		for(int num = 1; num < 100; num += 10) {
			writer.write(String.valueOf(num));
			writer.write("\t");
		}
		writer.newLine();
		
		for(int size = 1; size < 500; size += 10) {
			writer.write(String.valueOf(size));
			for(int num = 1; num < 100; num += 10) {
				double p = 1 - Math.pow(((N-size)/(double)N), (num-1)*size);
				
				writer.write("\t");
				writer.write(String.valueOf(p));
			}
			writer.newLine();
		}
		writer.close();
	}

}
