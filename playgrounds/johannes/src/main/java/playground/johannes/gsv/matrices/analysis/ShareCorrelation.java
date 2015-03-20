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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import com.vividsolutions.jts.geom.Point;

import playground.johannes.gsv.zones.KeyMatrix;
import playground.johannes.gsv.zones.MatrixOperations;
import playground.johannes.gsv.zones.Zone;
import playground.johannes.gsv.zones.ZoneCollection;
import playground.johannes.gsv.zones.io.KeyMatrixXMLReader;
import playground.johannes.gsv.zones.io.Zone2GeoJSON;
import playground.johannes.sna.math.FixedSampleSizeDiscretizer;
import playground.johannes.sna.util.TXTWriter;
import playground.johannes.socialnetworks.gis.CartesianDistanceCalculator;
import playground.johannes.socialnetworks.gis.DistanceCalculator;
import playground.johannes.socialnetworks.statistics.Correlations;

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
		KeyMatrixXMLReader reader = new KeyMatrixXMLReader();
		reader.setValidating(false);
		
		reader.parse("/home/johannes/gsv/matrices/refmatrices/itp.xml");
//		reader.parse("/home/johannes/gsv/matrices/simmatrices/miv.798.xml");
		KeyMatrix sim = reader.getMatrix();
//		MatrixOperations.applyFactor(sim, 1/365.0);
		
		reader.parse("/home/johannes/gsv/matrices/refmatrices/tomtom.de.xml");
		KeyMatrix ref = reader.getMatrix();
		MatrixOperations.applyFactor(ref, 1/16.0);
		removeLowVolumeEntries(ref, 100);
		
		reader.parse("/home/johannes/gsv/matrices/refmatrices/itp.miv-share.xml");
		KeyMatrix share = reader.getMatrix();
		
		KeyMatrix err = MatrixOperations.errorMatrix(ref, sim);
		
		ZoneCollection zones = new ZoneCollection();
		String data = new String(Files.readAllBytes(Paths.get("/home/johannes/gsv/gis/nuts/de.nuts3.gk3.geojson")));
		zones.addAll(Zone2GeoJSON.parseFeatureCollection(data));
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
		TDoubleDoubleHashMap values = Correlations.mean(shareVals.toNativeArray(), errVals.toNativeArray(), FixedSampleSizeDiscretizer.create(shareVals.toNativeArray(), 50));
//		TDoubleDoubleHashMap values = Correlations.mean(errVals.toNativeArray(), shareVals.toNativeArray(), 0.05);
		TXTWriter.writeMap(values, "share", "error", "/home/johannes/gsv/matrices/analysis/marketShares/shareCorrelation.txt");
		
		values = Correlations.mean(volVals.toNativeArray(), errVals.toNativeArray(), FixedSampleSizeDiscretizer.create(volVals.toNativeArray(), 50));
		TXTWriter.writeMap(values, "volume", "error", "/home/johannes/gsv/matrices/analysis/marketShares/volCorrelation.txt");
	}
	
	private static void removeEntries(KeyMatrix m, ZoneCollection zones, double distThreshold) {
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

	private static void removeLowVolumeEntries(KeyMatrix m, double threshold) {
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
