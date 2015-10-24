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

import gnu.trove.TDoubleArrayList;
import gnu.trove.TDoubleDoubleHashMap;
import org.matsim.contrib.common.stats.*;
import org.matsim.core.utils.collections.Tuple;
import playground.johannes.gsv.zones.KeyMatrix;
import playground.johannes.gsv.zones.MatrixOperations;
import playground.johannes.gsv.zones.io.KeyMatrixXMLReader;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

/**
 * @author johannes
 * 
 */
public class TTRatio {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String carVolFile = "/home/johannes/gsv/matrices/simmatrices/miv.826.xml";
//		String carVolFile = "/home/johannes/gsv/matrices/refmatrices/itp.xml";
		String railVolFile = "/home/johannes/gsv/matrices/analysis/marketShares/rail.all.nuts3.xml";
		String carTTFile = "/home/johannes/gsv/matrices/analysis/marketShares/ttMatrix.xml";
		String railTTFile = "/home/johannes/gsv/matrices/analysis/marketShares/railTT.xml";

		KeyMatrixXMLReader reader = new KeyMatrixXMLReader();
		reader.setValidating(false);

		reader.parse(carVolFile);
		KeyMatrix carVol = reader.getMatrix();

		reader.parse(railVolFile);
		KeyMatrix railVol = reader.getMatrix();
		MatrixOperations.applyFactor(railVol, 1/365.0);

		reader.parse(carTTFile);
		KeyMatrix carTT = reader.getMatrix();
		MatrixOperations.applyFactor(carTT, 2);
		
		reader.parse(railTTFile);
		KeyMatrix railTT = reader.getMatrix();

		List<Tuple<String, String>> relations = getRelations(railVol, 100000);
//		List<Tuple<String, String>> relations = getRelations(carVol, 10000);
		
		BufferedWriter ratioWriter = new BufferedWriter(new FileWriter("/home/johannes/gsv/matrices/analysis/marketShares/ratio.txt"));
		ratioWriter.write("ratio\tshare");
		ratioWriter.newLine();
		
		TDoubleArrayList shares = new TDoubleArrayList();
		TDoubleArrayList ratios = new TDoubleArrayList();
		
		for(Tuple<String, String> tuple : relations) {
			Double car = carVol.get(tuple.getFirst(), tuple.getSecond());
			if(car == null) car = 0.0;
			
			Double rail = railVol.get(tuple.getFirst(), tuple.getSecond());
			if(rail == null) rail = 0.0;
			
			Double ctt = carTT.get(tuple.getFirst(), tuple.getSecond());
			if(ctt == null) ctt = 0.0;
			ctt = ctt/60.0;
			
			Double rtt = railTT.get(tuple.getFirst(), tuple.getSecond());
			if(rtt == null) rtt = 0.0;
			
			if(rtt > 0) {
			double share = rail/(rail + car);
			double ratio = rtt/ctt;
			
			ratioWriter.write(String.valueOf(ratio));
			ratioWriter.write("\t");
			ratioWriter.write(String.valueOf(share));
			ratioWriter.newLine();
			
			ratios.add(ratio);
			shares.add(share);
			}
		}
		
		ratioWriter.close();
		
		Discretizer disc = FixedSampleSizeDiscretizer.create(ratios.toNativeArray(), 50, 200);
//		TDoubleDoubleHashMap hist = Correlations.mean(ratios.toNativeArray(), shares.toNativeArray(), disc);
		TDoubleDoubleHashMap hist = Correlations.mean(ratios.toNativeArray(), shares.toNativeArray(), new LinearDiscretizer(0.02));
		StatsWriter.writeHistogram(hist, "Ratio", "Share", "/home/johannes/gsv/matrices/analysis/marketShares/ratio.hist.txt");
	}

	private static List<Tuple<String, String>> getRelations(KeyMatrix m, int num) {
		Map<Double, Tuple<String, String>> map = new TreeMap<>(new Comparator<Double>() {

			@Override
			public int compare(Double o1, Double o2) {
				int result = -Double.compare(o1, o2);
				if (result == 0)
					return 1;// o1.hashCode() - o2.hashCode();
				else
					return result;
			}
		});

		Set<String> keys = m.keys();
		for (String i : keys) {
			for (String j : keys) {
				// if(i != j) {
				Double val = m.get(i, j);
				if (val != null) {
					map.put(val, new Tuple<String, String>(i, j));
				}
				// }
			}
		}

		List<Tuple<String, String>> list = new ArrayList<>(num);
		int cnt = 0;
		for (Entry<Double, Tuple<String, String>> entry : map.entrySet()) {
			list.add(entry.getValue());
			cnt++;
			if (cnt > num) {
				break;
			}
		}

		return list;
	}
}
