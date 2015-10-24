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

package playground.johannes.gsv.matrices.analysis;

import com.vividsolutions.jts.geom.Point;
import gnu.trove.TDoubleArrayList;
import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TObjectDoubleHashMap;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.common.gis.OrthodromicDistanceCalculator;
import org.matsim.contrib.common.stats.DescriptivePiStatistics;
import org.matsim.contrib.common.stats.FixedSampleSizeDiscretizer;
import org.matsim.contrib.common.stats.Histogram;
import org.matsim.contrib.common.stats.StatsWriter;
import org.matsim.contrib.socnetgen.sna.gis.Zone;
import org.matsim.contrib.socnetgen.sna.gis.ZoneLayer;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrix;
import org.matsim.visum.VisumMatrixReader;
import playground.johannes.gsv.matrices.MatrixOperations;
import playground.johannes.socialnetworks.gis.io.ZoneLayerSHP;
import playground.johannes.socialnetworks.statistics.Correlations;

import java.io.IOException;
import java.util.*;

/**
 * @author johannes
 * 
 */
public class MatrixCompare {

	public static DescriptiveStatistics relErrorAll(Matrix m1, Matrix m2, boolean absolute, boolean ignoreZeros) {
		Set<String> zoneIds = new HashSet<>();
		zoneIds.addAll(m1.getFromLocations().keySet());
		zoneIds.addAll(m1.getToLocations().keySet());
		zoneIds.addAll(m2.getFromLocations().keySet());
		zoneIds.addAll(m2.getToLocations().keySet());

		DescriptiveStatistics stats = new DescriptiveStatistics();

		int zeros = 0;

		for (String id1 : zoneIds) {
			for (String id2 : zoneIds) {
				if (!id1.equals(id2)) {
					Entry e1 = m1.getEntry(id1, id2);
					Entry e2 = m2.getEntry(id1, id2);

					if (e1 != null && e2 != null) {
						Double val1 = e1.getValue();
						Double val2 = e2.getValue();

						if (!(ignoreZeros && val2 == 0)) {
							if (val1 > 0) {
								double err = calcRelError(val1, val2, absolute);
								stats.addValue(err);
							}
						}
					} else
						zeros++;
				}
			}
		}

		if (zeros > 0) {
			System.out.println(String.format("%s cells where either old or new is zero.", zeros));
		}

		return stats;
	}

	private static double calcRelError(double d1, double d2, boolean absolute) {
		double diff = d2 - d1;

//		diff = Math.abs(diff);
		if(absolute) diff = Math.abs(diff);
		
		double err = diff/d1;
		
//		if(d2 > d1) {
//			err = diff/d1;
//		} else {
//			err = - diff/d2;
//		}
//		if(err < 0) {
//			return - (1/(1-Math.abs(err)) - 1);
//		} else return err;
//		if(absolute) err = Math.abs(err);
		return err;
	}

	public static TObjectDoubleHashMap<String> relErrorDestinations(Matrix m1, Matrix m2, boolean absolute, boolean ignoreZeros) {
		TObjectDoubleHashMap<String> sums1 = MatrixOperations.destinationSum(m1);
		TObjectDoubleHashMap<String> sums2 = MatrixOperations.destinationSum(m2);

		return calcErrors(sums1, sums2, absolute, ignoreZeros);
	}

	public static TObjectDoubleHashMap<String> relErrorOrigins(Matrix m1, Matrix m2, boolean absolute, boolean ignoreZeros) {
		TObjectDoubleHashMap<String> sums1 = MatrixOperations.originSum(m1);
		TObjectDoubleHashMap<String> sums2 = MatrixOperations.originSum(m2);

		return calcErrors(sums1, sums2, absolute, ignoreZeros);
	}

	private static TObjectDoubleHashMap<String> calcErrors(TObjectDoubleHashMap<String> sums1, TObjectDoubleHashMap<String> sums2, boolean absolute, boolean ignoreZeros) {
		Set<String> zoneIds = new HashSet<>();
		for (Object id : sums1.keys()) {
			zoneIds.add((String) id);
		}
		for (Object id : sums2.keys()) {
			zoneIds.add((String) id);
		}

		TObjectDoubleHashMap<String> values = new TObjectDoubleHashMap<>();

		int zeros = 0;

		for (String id : zoneIds) {
			double sum1 = sums1.get(id);
			double sum2 = sums2.get(id);

			if (sum1 > 0) {
				if (!(ignoreZeros && sum2 == 0)) {
					double err = calcRelError(sum1, sum2, absolute);
					values.put(id, err);
				}
			} else
				zeros++;
		}

		if (zeros > 0) {
			System.out.println(String.format("%s sums of m1 = 0.", zeros));
		}

		return values;
	}

	public static DescriptiveStatistics errorStats(TObjectDoubleHashMap<String> values) {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		for (double val : values.getValues()) {
			stats.addValue(val);
		}

		return stats;
	}

	public static Map<String, double[]> relError(Matrix m1, Matrix m2, Map<String, String> ids, boolean absolute) {
		List<String> idList = new ArrayList<>(ids.keySet());
		Collections.sort(idList);
		
		Map<String, double[]> values = new LinkedHashMap<>();
		
		for(int i = 0; i < idList.size(); i++) {
			String id1 = idList.get(i);
			
			for(int j = i+1; j < idList.size(); j++) {
				String id2 = idList.get(j);
				
				Entry e1 = m1.getEntry(id1, id2);
				Entry e2 = m2.getEntry(id1, id2);
				
//				double diff = e2.getValue() - e1.getValue();
//				if(absolute) diff = Math.abs(diff);
//				double err = diff/e1.getValue();
				double err = calcRelError(e1.getValue(), e2.getValue(), absolute);
				
				values.put(String.format("%s -> %s", ids.get(id1), ids.get(id2)), new double[]{err, e1.getValue(), e2.getValue()});
				/*
				 * return trip
				 */
				e1 = m1.getEntry(id2, id1);
				e2 = m2.getEntry(id2, id1);
				
//				diff = e2.getValue() - e1.getValue();
//				if(absolute) diff = Math.abs(diff);
//				err = diff/e1.getValue();
				err = calcRelError(e1.getValue(), e2.getValue(), absolute);
				
				values.put(String.format("%s -> %s", ids.get(id2), ids.get(id1)), new double[]{err, e1.getValue(), e2.getValue()});
			}
		}
		
		return values;
	}
	
	public static TDoubleDoubleHashMap distErrCorrelation(Matrix m1, Matrix m2, ZoneLayer<Map<String, Object>> zones,  boolean absolute, boolean ignoreZeros) {
		Set<String> zoneIds = new HashSet<>();
		zoneIds.addAll(m1.getFromLocations().keySet());
		zoneIds.addAll(m1.getToLocations().keySet());
		zoneIds.addAll(m2.getFromLocations().keySet());
		zoneIds.addAll(m2.getToLocations().keySet());
		
		return distErrCorrelation(m1, m2, zones, zoneIds, absolute, ignoreZeros);
	}
	
	public static TDoubleDoubleHashMap distErrCorrelation(Matrix m1, Matrix m2, ZoneLayer<Map<String, Object>> zones,  Set<String> zoneIds, boolean absolute, boolean ignoreZeros) {
		int zeros = 0;

		Map<String, Zone<?>> zoneMapping = new HashMap<>();
		for(Zone<Map<String, Object>> zone : zones.getZones()) {
			zoneMapping.put(zone.getAttribute().get("NO").toString(), zone);
		}
		
		Matrix distMatrix = new Matrix("dist", null);
		TDoubleArrayList errs = new TDoubleArrayList();
		TDoubleArrayList dists = new TDoubleArrayList();
		
		DescriptivePiStatistics stats1 = new DescriptivePiStatistics();
		DescriptivePiStatistics stats2 = new DescriptivePiStatistics();
		
		for (String id1 : zoneIds) {
			for (String id2 : zoneIds) {
				if (!id1.equals(id2)) {
					Entry e1 = m1.getEntry(id1, id2);
					Entry e2 = m2.getEntry(id1, id2);

					if (e1 != null && e2 != null) {
						Double val1 = e1.getValue();
						Double val2 = e2.getValue();

						if (!(ignoreZeros && val2 == 0)) {
							if (val1 > 0) {
								double err = calcRelError(val1, val2, absolute);
								
								Entry distEntry = distMatrix.getEntry(id1, id2);
								if(distEntry == null) {
									Zone<?> z1 = zoneMapping.get(id1);
									Zone<?> z2 = zoneMapping.get(id2);
									
									Point p1 = z1.getGeometry().getCentroid();
									Point p2 = z2.getGeometry().getCentroid();
									p1.setSRID(4326);
									p2.setSRID(4326);
									
									double d = OrthodromicDistanceCalculator.getInstance().distance(p1, p2);
									
									distEntry = distMatrix.createEntry(id1, id2, d);
									
//									stats1.addValue(d, val1);
//									stats2.addValue(d, val2);
								}
								
								double dist = distEntry.getValue();
		
								dists.add(dist);
								errs.add(err);
								
								stats1.addValue(dist, 1/val1);
								stats2.addValue(dist, 1/val2);
							}
						}
					} else
						zeros++;
				}
			}
		}

		if (zeros > 0) {
			System.out.println(String.format("%s zero reference values.", zeros));
		}

		try {
			StatsWriter.writeHistogram(Histogram.createHistogram(stats1, FixedSampleSizeDiscretizer.create(stats1.getValues(), 1, 50), true), "Disctance", "p", "/home/johannes/gsv/matrices/dist1.txt");
			StatsWriter.writeHistogram(Histogram.createHistogram(stats2, FixedSampleSizeDiscretizer.create(stats2.getValues(), 1, 50), true), "Disctance", "p", "/home/johannes/gsv/matrices/dist2.txt");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return Correlations.mean(dists.toNativeArray(), errs.toNativeArray(), 10000);
	}
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Matrix m1 = new Matrix("1", null);
		VisumMatrixReader reader = new VisumMatrixReader(m1);
//		reader.readFile("/home/johannes/gsv/matrices/netz2030.fma");
		reader.readFile("/home/johannes/gsv/matrices/itp.fma");
		
		Matrix m2 = new Matrix("2", null);
		reader = new VisumMatrixReader(m2);
		reader.readFile("/home/johannes/gsv/matrices/miv.489.fma");
//		reader.readFile("/home/johannes/gsv/matrices/netz2030.fma");

		MatrixOperations.applyFactor(m1, 1 / 365.0);
//		MatrixOperations.applyFactor(m2, 11);
//		MatrixOperations.applyIntracellFactor(m2, 1.3);
		
		System.out.println(String.format("PSMobility - matrix sum: %s", MatrixOperations.sum(m1, false)));
		System.out.println(String.format("Matsim - matrix sum: %s", MatrixOperations.sum(m2, false)));
		
		System.out.println(String.format("PSMobility: %s cells with zero value.", MatrixOperations.countEmptyCells(m1)));
		System.out.println(String.format("Matsim: %s cells with zero value.", MatrixOperations.countEmptyCells(m2)));

		boolean ignoreZeros = false;
		DescriptiveStatistics stats = relErrorAll(m1, m2, false, ignoreZeros);
		System.out.println(String.format("Relative error all cells: mean=%s, med=%s, min=%s, max=%s", stats.getMean(), stats.getPercentile(0.5),
				stats.getMin(), stats.getMax()));
		stats = relErrorAll(m1, m2, true, ignoreZeros);
		System.out.println(String.format("Relative error all cells (abs): mean=%s, med=%s, min=%s, max=%s", stats.getMean(),
				stats.getPercentile(0.5), stats.getMin(), stats.getMax()));

		stats = errorStats(relErrorDestinations(m1, m2, false, ignoreZeros));
		System.out.println(String.format("Destination Error: mean=%s, med=%s, min=%s, max=%s", stats.getMean(), stats.getPercentile(0.5),
				stats.getMin(), stats.getMax()));
		stats = errorStats(relErrorDestinations(m1, m2, true, ignoreZeros));
		System.out.println(String.format("Destination Error (abs): mean=%s, med=%s, min=%s, max=%s", stats.getMean(), stats.getPercentile(0.5),
				stats.getMin(), stats.getMax()));

		stats = errorStats(relErrorOrigins(m1, m2, false, ignoreZeros));
		System.out.println(String.format("Origin Error: mean=%s, med=%s, min=%s, max=%s", stats.getMean(), stats.getPercentile(0.5), stats.getMin(),
				stats.getMax()));
		stats = errorStats(relErrorOrigins(m1, m2, true, ignoreZeros));
		System.out.println(String.format("Origin Error (abs): mean=%s, med=%s, min=%s, max=%s", stats.getMean(), stats.getPercentile(0.5),
				stats.getMin(), stats.getMax()));

		ZoneLayer<Map<String, Object>> zones = ZoneLayerSHP.read("/home/johannes/gsv/matrices/zones_zone.SHP");
		TDoubleDoubleHashMap distErrCorrelation = distErrCorrelation(m1, m2, zones, false, ignoreZeros);
		StatsWriter.writeHistogram(distErrCorrelation, "distance", "rel. error", "/home/johannes/gsv/matrices/distErr.txt");
		
		Map<String, String> ids = new HashMap<>();
		ids.put("6412", "FRA");
		ids.put("11000", "BER");
		ids.put("2000", "HAM");
		ids.put("3241", "HAN");
		ids.put("5315", "KLN");
		ids.put("9162", "MUN");
		ids.put("8111", "STG");

		zones = ZoneLayerSHP.read("/home/johannes/gsv/matrices/zones_zone.SHP");
//		distErrCorrelation = distErrCorrelation(m1, m2, zones, ids.keySet(), false, ignoreZeros);
//		TXTWriter.writeHistogram(distErrCorrelation, "distance", "rel. error", "/home/johannes/gsv/matrices/distErr.sel.txt");
		
		Map<String, double[]> relErrs = relError(m1, m2, ids, false);
		for(java.util.Map.Entry<String, double[]> entry : relErrs.entrySet()) {
			System.out.println(String.format("%s: %.4f; old: %.4f, new; %.4f", entry.getKey(), entry.getValue()[0], entry.getValue()[1], entry.getValue()[2]));
		}
		
		System.out.println("\nDestination errors:");
		TObjectDoubleHashMap<String> destErrors = relErrorDestinations(m1, m2, false, ignoreZeros);
		for(java.util.Map.Entry<String, String> entry : ids.entrySet()) {
			System.out.println(String.format("%s: %.4f", entry.getValue(), destErrors.get(entry.getKey())));
		}
		
		System.out.println("\nOrigin errors:");
		TObjectDoubleHashMap<String> origErrors = relErrorOrigins(m1, m2, false, ignoreZeros);
		for(java.util.Map.Entry<String, String> entry : ids.entrySet()) {
			System.out.println(String.format("%s: %.4f", entry.getValue(), origErrors.get(entry.getKey())));
		}
	}

}
