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

import com.vividsolutions.jts.geom.Point;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.hash.TDoubleDoubleHashMap;
import org.matsim.contrib.common.gis.CartesianDistanceCalculator;
import org.matsim.contrib.common.gis.DistanceCalculator;
import org.matsim.contrib.common.stats.Correlations;
import org.matsim.contrib.common.stats.FixedSampleSizeDiscretizer;
import org.matsim.contrib.common.stats.StatsWriter;
import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.gis.ZoneGeoJsonIO;
import playground.johannes.synpop.matrix.MatrixOperations;
import playground.johannes.synpop.matrix.NumericMatrix;
import playground.johannes.synpop.matrix.NumericMatrixXMLReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

/**
 * @author johannes
 *
 */
public class ShareCorrelation {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		NumericMatrixXMLReader reader = new NumericMatrixXMLReader();
		reader.setValidating(false);
		
		reader.parse("/home/johannes/gsv/matrices/refmatrices/itp.xml");
//		reader.parse("/home/johannes/gsv/matrices/simmatrices/miv.798.xml");
		NumericMatrix sim = reader.getMatrix();
//		MatrixOperations.multiply(sim, 1/365.0);
		
		reader.parse("/home/johannes/gsv/matrices/refmatrices/tomtom.de.xml");
		NumericMatrix ref = reader.getMatrix();
		MatrixOperations.applyFactor(ref, 1/16.0);
		removeLowVolumeEntries(ref, 100);
		
		reader.parse("/home/johannes/gsv/matrices/refmatrices/itp.miv-share.xml");
		NumericMatrix share = reader.getMatrix();
		
		NumericMatrix err = new NumericMatrix();
		MatrixOperations.errorMatrix(ref, sim, err);
		
		ZoneCollection zones = new ZoneCollection(null);
		String data = new String(Files.readAllBytes(Paths.get("/home/johannes/gsv/gis/nuts/de.nuts3.gk3.geojson")));
		zones.addAll(ZoneGeoJsonIO.parseFeatureCollection(data));
		data = null;
		zones.setPrimaryKey("gsvId");
		
		removeEntries(err, zones, 100000);
		
		TDoubleArrayList shareVals = new TDoubleArrayList();
		TDoubleArrayList errVals = new TDoubleArrayList();
		TDoubleArrayList volVals = new TDoubleArrayList();
		
		Set<String> keys = new HashSet<>(ref.keys());
		for(String i : keys) {
			for(String j : keys) {
				Double errVal = err.get(i, j);
				Double shareVal = share.get(i, j);
				Double vol = ref.get(i, j);
				if(vol != null & errVal != null && shareVal != null) {
					shareVals.add(shareVal);
					errVals.add(errVal);
					volVals.add(vol);
				}
			}
		}
		
		System.out.println(String.format("%s relations", errVals.size()));
		TDoubleDoubleHashMap values = Correlations.mean(shareVals.toArray(), errVals.toArray(), FixedSampleSizeDiscretizer.create(shareVals.toArray(), 50));
//		TDoubleDoubleHashMap values = Correlations.mean(errVals.toArray(), shareVals.toArray(), 0.05);
		StatsWriter.writeHistogram(values, "share", "error", "/home/johannes/gsv/matrices/analysis/marketShares/shareCorrelation.txt");
		
		values = Correlations.mean(volVals.toArray(), errVals.toArray(), FixedSampleSizeDiscretizer.create(volVals.toArray(), 50));
		StatsWriter.writeHistogram(values, "volume", "error", "/home/johannes/gsv/matrices/analysis/marketShares/volCorrelation.txt");
	}
	
	private static void removeEntries(NumericMatrix m, ZoneCollection zones, double distThreshold) {
		DistanceCalculator dCalc = new CartesianDistanceCalculator();
		Set<String> keys = m.keys();
		int cnt = 0;
		for (String i : keys) {
			for (String j : keys) {
				Zone zone_i = zones.get(i);
				Zone zone_j = zones.get(j);

				if (zone_i != null && zone_j != null) {
					Point pi = zone_i.getGeometry().getCentroid();
					Point pj = zone_j.getGeometry().getCentroid();

					double d = dCalc.distance(pi, pj);

					if (d < distThreshold) {
						Double val = m.get(i, j);
						if (val != null) {
							m.set(i, j, null);
							cnt++;
						}
					}
				}
			}
		}
		
//		logger.info(String.format("Removed %s trips with less than %s KM.", cnt, distThreshold));
	}

	private static void removeLowVolumeEntries(NumericMatrix m, double threshold) {
		int cnt = 0;
		Set<String> keys = m.keys();
		for(String i : keys) {
			for(String j : keys) {
				Double val = m.get(i, j);
				if(val != null) {
					if(val < threshold) {
						m.set(i, j, null);
						cnt++;
					}
				}
			}
		}
		
		System.out.println(String.format("Removed %s entries below %s trips", cnt, threshold));
	}
}
