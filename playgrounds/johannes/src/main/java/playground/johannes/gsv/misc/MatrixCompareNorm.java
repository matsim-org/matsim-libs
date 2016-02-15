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

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrix;
import org.matsim.visum.VisumMatrixReader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author johannes
 * 
 */
public class MatrixCompareNorm {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Matrix m1 = new Matrix("1", null);
		VisumMatrixReader reader = new VisumMatrixReader(m1);
		reader.readFile("/home/johannes/gsv/matrices/IV_gesamt.O.fma");
		normMatrix(m1);
		
		Matrix m2 = new Matrix("2", null);
		reader = new VisumMatrixReader(m2);
		reader.readFile("/home/johannes/gsv/matrices/miv.277.fma");
		normMatrix(m2);

		int notfound = 0;
				DescriptiveStatistics oRelErrs = new DescriptiveStatistics();
		Set<String> origs = m1.getFromLocations().keySet();
		for (String origId : origs) {
			List<Entry> entries1 = m1.getFromLocEntries(origId);
			List<Entry> entries2 = m2.getFromLocEntries(origId);

			double sum1 = 0;
			for (Entry entry : entries1) {
				sum1 += entry.getValue();
			}

			if(entries2 == null) {
				oRelErrs.addValue(-1);
			} else if (entries2 != null && sum1 > 0) {
				double sum2 = 0;
				for (Entry entry : entries2) {
					sum2 += entry.getValue();
				}
				
				oRelErrs.addValue((sum2 - sum1)/sum1);
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
				sum1 += entry.getValue();
			}

			if (entries2 != null && sum1 > 0) {
				double sum2 = 0;
				for (Entry entry : entries2) {
					sum2 += entry.getValue();
				}
				
				dRelErrs.addValue((sum2 - sum1)/sum1);
			}

			
		}

		System.out.println(String.format("Rel err of destinations: mean=%s, med=%s, min=%s, max=%s", dRelErrs.getMean(), dRelErrs.getPercentile(0.5),
				dRelErrs.getMin(), dRelErrs.getMax()));
		
		Map<String, String> ids = new HashMap<>();
		ids.put("6412", "FRA");
		ids.put("11000", "BER");
		ids.put("2000", "HAM");
		ids.put("3241", "HAN");
		ids.put("5315", "KLN");
		ids.put("9162", "MUN");
		ids.put("8111", "STG");
		
		Map<String, Double> errors = new HashMap<>();
		for(String id1 : ids.keySet()) {
			for(String id2 : ids.keySet()) {
				if(!id1.equalsIgnoreCase(id2)) {
					
					Entry e1 = m1.getEntry(id1, id2);
					double val1 = e1.getValue();
					
					Entry e2 = m2.getEntry(id1, id2);
					double val2 = e2.getValue();
					
					double err = (val2 - val1)/val1;
				
					System.out.print(ids.get(id1));
					System.out.print(" -> ");
					System.out.print(ids.get(id2));
					System.out.print(": ");
					System.out.println(String.valueOf(err));
					
				}
			}
		}
		
		
	}

	private static void normMatrix(Matrix m) {
		double sum = 0;
		for(String from : m.getFromLocations().keySet()) {
			for(String to : m.getToLocations().keySet()) {
				Entry e = m.getEntry(from, to);
				if(e != null) {
					sum += e.getValue();
				}
			}
		}
		
		for(String from : m.getFromLocations().keySet()) {
			for(String to : m.getToLocations().keySet()) {
				Entry e = m.getEntry(from, to);
				if(e != null) {
					e.setValue(e.getValue()/sum);
				}
			}
		}
	}
}
