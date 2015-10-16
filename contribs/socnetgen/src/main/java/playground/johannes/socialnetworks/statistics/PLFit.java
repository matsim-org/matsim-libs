/* *********************************************************************** *
 * project: org.matsim.*
 * PLFit.java
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
package playground.johannes.socialnetworks.statistics;

import gnu.trove.TDoubleArrayList;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * @author illenberger
 *
 */
public class PLFit {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader("/Users/jillenberger/Work/socialnets/data/ivt2009/11-2011/analysis/plain/ch/obs/spatial/p_accept.raw.txt"));
		String line = null;
		TDoubleArrayList data = new TDoubleArrayList();
		while((line = reader.readLine()) != null) {
			data.add(Double.parseDouble(line));
		}
		
		double alpha = estimAlpha(data.toNativeArray(), 2000, 250000);
		System.out.println("alpha = " + alpha);
		
//		reader = new BufferedReader(new FileReader("/Users/jillenberger/Desktop/powerlaw/data.txt"));
//		line = null;
//		data = new TDoubleArrayList();
//		while((line = reader.readLine()) != null) {
//			data.add(Double.parseDouble(line));
//		}
//		
//		alpha = estimAlpha(data.toNativeArray(), 1000, 500000);
//		System.out.println("alpha = " + alpha);
	}

	public static double estimAlpha(double[] data, double xmin, double xmax) {
		TDoubleArrayList subdata = new TDoubleArrayList(data.length);
		for(double x : data) {
			if(x >= xmin && x <= xmax)
				subdata.add(x);
		}
		
		double sum = 0;
		for(int i = 0; i < subdata.size(); i++) {
			sum += Math.log(subdata.get(i)/xmin);
		}
		
		return 1 + subdata.size()/sum;
	}
}
