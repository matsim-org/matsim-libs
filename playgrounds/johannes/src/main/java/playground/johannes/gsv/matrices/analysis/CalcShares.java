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

import gnu.trove.list.array.TDoubleArrayList;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.core.utils.collections.Tuple;
import playground.johannes.gsv.sim.cadyts.ODUtils;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.gis.ZoneGeoJsonIO;
import playground.johannes.synpop.matrix.MatrixOperations;
import playground.johannes.synpop.matrix.NumericMatrix;
import playground.johannes.synpop.matrix.NumericMatrixXMLReader;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;

/**
 * @author johannes
 *
 */
public class CalcShares {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		NumericMatrixXMLReader reader = new NumericMatrixXMLReader();
		reader.setValidating(false);
		
		reader.parse("/home/johannes/gsv/matrices/refmatrices/itp.xml");
		NumericMatrix carItpMatrix = reader.getMatrix();
		MatrixOperations.applyFactor(carItpMatrix, 1/365.0);
				
		reader.parse("/home/johannes/gsv/matrices/simmatrices/miv.819.2.xml");
		NumericMatrix carSimMatrix = reader.getMatrix();
//		MatrixOperations.symmetrize(carSimMatrix);
//		MatrixOperations.multiply(carSimMatrix, 11.8);
		
		reader.parse("/home/johannes/gsv/matrices/refmatrices/tomtom.de.xml");
		NumericMatrix carTomTomMatrix = reader.getMatrix();
		
		reader.parse("/home/johannes/gsv/matrices/analysis/marketShares/rail.all.nuts3.xml");
		NumericMatrix railSimMatrix = reader.getMatrix();
		MatrixOperations.applyFactor(railSimMatrix, 1/365.0);
		
		reader.parse("/home/johannes/gsv/matrices/analysis/marketShares/car.share.xml");
		NumericMatrix shareRefMatrix = reader.getMatrix();
		
		ZoneCollection zones = new ZoneCollection(null);
		String data = new String(Files.readAllBytes(Paths.get("/home/johannes/gsv/gis/nuts/de.nuts3.gk3.geojson")));
		zones.addAll(ZoneGeoJsonIO.parseFeatureCollection(data));
		data = null;
		zones.setPrimaryKey("gsvId");
		
		ODUtils.cleanDistances(railSimMatrix, zones, 100000);
		ODUtils.cleanDistances(carSimMatrix, zones, 100000);
		
		ODUtils.cleanDistances(carTomTomMatrix, zones, 100000);
//		ODUtils.cleanVolumes(carTomTomMatrix, zones, 100);
		double c = ODUtils.calcNormalization(carTomTomMatrix, carSimMatrix);
		MatrixOperations.applyFactor(carTomTomMatrix, c);
		
		removeUnknownZones(railSimMatrix);
		List<Tuple<String, String>> relations = getRelations(railSimMatrix, 3000);
//		List<Tuple<String, String>> relations = getRelations(carTomTomMatrix, 3000);
		
		BufferedWriter writer = new BufferedWriter(new FileWriter("/home/johannes/gsv/matrices/analysis/marketShares/miv-shares.txt"));
		writer.write("from\tto\tshareRef\tshareSim\tcarVol\trailVol\tshareItp\tvolItp\tcarTomTom\tshareTomTom");
		writer.newLine();
		
		DescriptiveStatistics simStats = new DescriptiveStatistics();
		DescriptiveStatistics itpStats = new DescriptiveStatistics();
		DescriptiveStatistics simItpStats = new DescriptiveStatistics();
		
		TDoubleArrayList railVolumes = new TDoubleArrayList();
		TDoubleArrayList shareSimDiff = new TDoubleArrayList();
		TDoubleArrayList shareItpDiff = new TDoubleArrayList();
		
		TDoubleArrayList refShares = new TDoubleArrayList();
		TDoubleArrayList simShares = new TDoubleArrayList();
		TDoubleArrayList itpShares = new TDoubleArrayList();
		TDoubleArrayList tomtomShares = new TDoubleArrayList();
		
		for(Tuple<String, String> relation : relations) {
			String i = relation.getFirst();
			String j = relation.getSecond();
			
			Double carSimVal = carSimMatrix.get(i, j);
			if(carSimVal == null) carSimVal = 0.0;
			
			Double carItpVal = carItpMatrix.get(i, j);
			if(carItpVal == null) carItpVal = 0.0;
			
			Double carTomTomVal = carTomTomMatrix.get(i, j);
			if(carTomTomVal == null) carTomTomVal = 0.0;
			
			Double railVal = railSimMatrix.get(i, j);
			if(railVal == null) railVal = 0.0;
			
			Double shareVal = shareRefMatrix.get(i, j);
			if(shareVal == null) shareVal = 0.0;
			
			double simShare = carSimVal/(carSimVal + railVal);
			double itpShare = carItpVal/(carItpVal + railVal);
			double tomtomShare = carTomTomVal/(carTomTomVal + railVal);
			
			simStats.addValue(simShare - shareVal);
			itpStats.addValue(itpShare - shareVal);
			simItpStats.addValue(itpShare - simShare);
		
			refShares.add(shareVal);
			simShares.add(simShare);
			itpShares.add(itpShare);
			tomtomShares.add(tomtomShare);
			
			railVolumes.add(railVal);
			shareSimDiff.add(shareVal  -simShare );
			shareItpDiff.add(shareVal - itpShare );
			
			writer.write(i);
			writer.write("\t");
			writer.write(j);
			writer.write("\t");
			writer.write(String.valueOf(shareVal));
			writer.write("\t");
			writer.write(String.valueOf(simShare));
			writer.write("\t");
			writer.write(String.valueOf(carSimVal));
			writer.write("\t");
			writer.write(String.valueOf(railVal));
			writer.write("\t");
			writer.write(String.valueOf(itpShare));
			writer.write("\t");
			writer.write(String.valueOf(carItpVal));
			writer.write("\t");
			writer.write(String.valueOf(carTomTomVal));
			writer.write("\t");
			writer.write(String.valueOf(tomtomShare));
			
			writer.newLine();
		}
		writer.close();
		
		System.out.println("Avr sim share error: " + simStats.getMean());
		System.out.println("Avr itp share error: " + itpStats.getMean());
		System.out.println("Avr sim-itp share error: " + simItpStats.getMean());
		
		writeCorrelation(refShares, simShares, "model", "sim", "/home/johannes/gsv/matrices/analysis/marketShares/modelSim.txt");
		writeCorrelation(itpShares, simShares, "itp", "sim", "/home/johannes/gsv/matrices/analysis/marketShares/itpSim.txt");
		writeCorrelation(tomtomShares, simShares, "tomtom", "sim", "/home/johannes/gsv/matrices/analysis/marketShares/tomtomSim.txt");
		writeCorrelation(itpShares, tomtomShares, "itp", "tomtom", "/home/johannes/gsv/matrices/analysis/marketShares/itpTomtom.txt");
		
//		double[] samples = railVolumes.toArray();
//		TDoubleDoubleHashMap hist = Correlations.mean(samples, shareSimDiff.toArray(), FixedSampleSizeDiscretizer.create(samples, 50));
//		TXTWriter.writeHistogram(hist, "rail volume", "share diff", "/home/johannes/gsv/matrices/analysis/marketShares/simVolDiff.txt");
//		
//		hist = Correlations.mean(samples, shareItpDiff.toArray(), FixedSampleSizeDiscretizer.create(samples, 50));
//		TXTWriter.writeHistogram(hist, "rail volume", "share diff", "/home/johannes/gsv/matrices/analysis/marketShares/itpVolDiff.txt");
//		
//		hist = Correlations.mean(refShares.toArray(), simShares.toArray());
//		TXTWriter.writeHistogram(hist, "ref share", "sim share", "/home/johannes/gsv/matrices/analysis/marketShares/simShareCorrel.txt");
//		
//		hist = Correlations.mean(refShares.toArray(), itpShares.toArray());
//		TXTWriter.writeHistogram(hist, "ref share", "itp share", "/home/johannes/gsv/matrices/analysis/marketShares/itpShareCorrel.txt");
	}

	private static List<Tuple<String, String>> getRelations(NumericMatrix m, int num) {
		Map<Double, Tuple<String, String>> map = new TreeMap<>(new Comparator<Double>() {

			@Override
			public int compare(Double o1, Double o2) {
				int result = - Double.compare(o1, o2);
				if(result == 0)
					return 1;//o1.hashCode() - o2.hashCode();
				else
					return result;
			}
		});
		
		Set<String> keys = m.keys();
		for(String i : keys) {
			for(String j : keys) {
//				if(i != j) {
				Double val = m.get(i, j);
				if(val != null) {
					map.put(val, new Tuple<String, String>(i, j));
				}
//				}
			}
		}
		
		List<Tuple<String, String>> list = new ArrayList<>(num);
		int cnt = 0;
		for(Entry<Double, Tuple<String, String>> entry : map.entrySet()) {
			list.add(entry.getValue());
			cnt++;
			if(cnt > num) {
				break;
			}
		}
		
		return list;
	}
	
	private static void writeCorrelation(TDoubleArrayList valuesX, TDoubleArrayList valuesY, String keyX, String keyY, String file) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		writer.write(keyX);
		writer.write("\t");
		writer.write(keyY);
		writer.newLine();
		
		for(int i = 0; i < valuesX.size(); i++) {
			writer.write(String.valueOf(valuesX.get(i)));
			writer.write("\t");
			writer.write(String.valueOf(valuesY.get(i)));
			writer.newLine();
		}
		
		writer.close();
	}
	private static void removeUnknownZones(NumericMatrix m) {
		Set<String> keys = m.keys();
		for(String i : keys) {
			for(String j : keys) {
				if(i.startsWith("unknown") || j.startsWith("unknown")) {
					m.set(i, j, null);
				}
			}
		}
	}
}
