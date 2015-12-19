/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.matrices.analysis;

import playground.johannes.synpop.matrix.NumericMatrix;
import playground.johannes.synpop.matrix.NumericMatrixXMLReader;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author johannes
 *
 */
public class OriginVolume {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		NumericMatrixXMLReader reader = new NumericMatrixXMLReader();
		reader.setValidating(false);
		reader.parse("/home/johannes/sge/prj/matsim/run/874/output/matrices-averaged/miv.sym.xml");
		
		NumericMatrix m = reader.getMatrix();

		Set<String> keys = m.keys();
		Map<String, Double> vols = new HashMap<>();
		
		for(String i : keys) {
			double sum = 0;
			for(String j : keys) {
				Double val = m.get(i, j);
				if(val != null) {
					sum += val;
				}
			}
			vols.put(i, sum);
		}
		
		BufferedWriter writer = new BufferedWriter(new FileWriter("/home/johannes/gsv/matrices/analysis/origin-volumes.txt"));
		writer.write("id\tvolume");
		writer.newLine();
		
		for(Entry<String, Double> e : vols.entrySet()) {
			writer.write(e.getKey());
			writer.write("\t");
			writer.write(String.valueOf(e.getValue()));
			writer.newLine();
		}
		
		writer.close();
	}

}
