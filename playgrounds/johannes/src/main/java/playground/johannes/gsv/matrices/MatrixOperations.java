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

package playground.johannes.gsv.matrices;

import gnu.trove.TObjectDoubleHashMap;
import org.matsim.matrices.Matrix;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author johannes
 * 
 */
public class MatrixOperations {

	public static TObjectDoubleHashMap<String> destinationSum(Matrix m) {
		TObjectDoubleHashMap<String> values = new TObjectDoubleHashMap<>();

		for (Entry<String, ArrayList<org.matsim.matrices.Entry>> entries : m.getFromLocations().entrySet()) {
			double sum = 0;
			for (org.matsim.matrices.Entry entry : entries.getValue()) {
				sum += entry.getValue();
			}

			values.put(entries.getKey(), sum);
		}

		return values;
	}

	public static TObjectDoubleHashMap<String> originSum(Matrix m) {
		TObjectDoubleHashMap<String> values = new TObjectDoubleHashMap<>();

		for (Entry<String, ArrayList<org.matsim.matrices.Entry>> entries : m.getToLocations().entrySet()) {
			double sum = 0;
			for (org.matsim.matrices.Entry entry : entries.getValue()) {
				sum += entry.getValue();
			}

			values.put(entries.getKey(), sum);
		}

		return values;
	}

	public static void applyFactor(Matrix m, double factor) {
		for (Entry<String, ArrayList<org.matsim.matrices.Entry>> entries : m.getFromLocations().entrySet()) {
			for (org.matsim.matrices.Entry entry : entries.getValue()) {
				entry.setValue(entry.getValue() * factor);
			}
		}
	}
	
	public static void applyIntracellFactor(Matrix m, double factor) {
		Set<String> ids = new HashSet<>(m.getFromLocations().keySet());
		ids.addAll(m.getToLocations().keySet());
		
		for(String id : ids) {
			org.matsim.matrices.Entry e = m.getEntry(id, id);
			if(e != null) {
				e.setValue(e.getValue() * factor);
			}
		}
	}

	public static double sum(Matrix m) {
		return sum(m, false);
	}
	
	public static double sum(Matrix m, boolean ingnoreIntraCell) {
		double sum = 0;
		for (Entry<String, ArrayList<org.matsim.matrices.Entry>> entries : m.getFromLocations().entrySet()) {
			for (org.matsim.matrices.Entry entry : entries.getValue()) {
				if(ingnoreIntraCell) {
					if(!entry.getFromLocation().equalsIgnoreCase(entry.getToLocation())) {
						sum += entry.getValue();
					}
				} else {
					sum += entry.getValue();
				}
			}
		}

		return sum;
	}

	public static void normalize(Matrix m) {
		double sum = sum(m);

		for (Entry<String, ArrayList<org.matsim.matrices.Entry>> entries : m.getFromLocations().entrySet()) {
			for (org.matsim.matrices.Entry entry : entries.getValue()) {
				entry.setValue(entry.getValue() / sum);
			}
		}
	}

	public static int countEmptyCells(Matrix m) {
		int N = Math.max(m.getFromLocations().size(), m.getToLocations().size());
		int zeros = 0;
		int cnt = 0;
		for (Entry<String, ArrayList<org.matsim.matrices.Entry>> entries : m.getFromLocations().entrySet()) {
			for (org.matsim.matrices.Entry entry : entries.getValue()) {
				cnt++;
				if (entry.getValue() == 0) {
					zeros++;
				}
			}
		}

		return (N * (N-1)) - cnt + zeros;

	}
}
