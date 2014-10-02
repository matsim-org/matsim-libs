/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.misc;

import java.util.List;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrix;
import org.matsim.visum.VisumMatrixReader;

/**
 * @author johannes
 * 
 */
public class MatrixCompare {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Matrix m1 = new Matrix("1", null);
		VisumMatrixReader reader = new VisumMatrixReader(m1);
		reader.readFile("/home/johannes/gsv/matrices/IV_gesamt.O.fma");

		Matrix m2 = new Matrix("2", null);
		reader = new VisumMatrixReader(m2);
		reader.readFile("/home/johannes/gsv/matrices/miv.fma");

		int notfound = 0;
		
		DescriptiveStatistics oRelErrs = new DescriptiveStatistics();
		Set<String> origs = m1.getFromLocations().keySet();
		for (String origId : origs) {
			List<Entry> entries1 = m1.getFromLocEntries(origId);
			List<Entry> entries2 = m2.getFromLocEntries(origId);

			double sum1 = 0;
			for (Entry entry : entries1) {
				sum1 += entry.getValue() / 365.0;
			}

			if(entries2 == null) {
				oRelErrs.addValue(1);
			} else if (entries2 != null && sum1 > 0) {
				double sum2 = 0;
				for (Entry entry : entries2) {
					sum2 += entry.getValue() * 50;
				}
				
				oRelErrs.addValue(Math.abs(sum1 - sum2) / sum1);
			} else {
				notfound++;
			}

			
		}

		System.err.println(String.format("%s entries out of %s not found or with zero value.", notfound, notfound + oRelErrs.getN()));
		System.out.println(String.format("Rel err of origins: mean=%s, med=%s, min=%s, max=%s", oRelErrs.getMean(), oRelErrs.getPercentile(0.5),
				oRelErrs.getMin(), oRelErrs.getMax()));
		
		DescriptiveStatistics dRelErrs = new DescriptiveStatistics();
		Set<String> dests = m1.getToLocations().keySet();
		for (String destId : dests) {
			List<Entry> entries1 = m1.getToLocEntries(destId);
			List<Entry> entries2 = m2.getToLocEntries(destId);

			double sum1 = 0;
			for (Entry entry : entries1) {
				sum1 += entry.getValue() / 365.0;
			}

			if (entries2 != null && sum1 > 0) {
				double sum2 = 0;
				for (Entry entry : entries2) {
					sum2 += entry.getValue() * 50;
				}
				
				dRelErrs.addValue(Math.abs(sum1 - sum2) / sum1);
			}

			
		}

		System.out.println(String.format("Rel err of origins: mean=%s, med=%s, min=%s, max=%s", dRelErrs.getMean(), dRelErrs.getPercentile(0.5),
				dRelErrs.getMin(), dRelErrs.getMax()));
	}

}
