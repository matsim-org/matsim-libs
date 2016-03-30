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

import com.vividsolutions.jts.algorithm.MinimumDiameter;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.common.gis.CartesianDistanceCalculator;
import org.matsim.contrib.common.gis.DistanceCalculator;
import org.matsim.core.utils.collections.Tuple;
import playground.johannes.gsv.sim.cadyts.ODUtils;
import playground.johannes.synpop.gis.Zone;
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
public class DumpRelations {

	private static final double distThreshold = 100000;

	private static final double numRelations = 3000;

	public static void main(String[] args) throws IOException {
		String runId = "874";
		String simFile = "/home/johannes/sge/prj/matsim/run/874/output/nuts3/miv.sym.xml";
//		String simFile = String.format("/home/johannes/gsv/miv-matrix/simmatrices/miv.%s.xml", runId);
		String refFile2 = "/home/johannes/gsv/miv-matrix/refmatrices/tomtom.de.xml";
		String refFile1 = "/home/johannes/gsv/miv-matrix/refmatrices/itp.xml";
		/*
		 * load ref matrix
		 */
		NumericMatrixXMLReader reader = new NumericMatrixXMLReader();
		reader.setValidating(false);
		reader.parse(refFile1);
		NumericMatrix itp = reader.getMatrix();

		MatrixOperations.applyFactor(itp, 1 / 365.0);

		reader.parse(refFile2);
		NumericMatrix tomtom = reader.getMatrix();

		/*
		 * load simulated matrix
		 */
		reader.parse(simFile);
		NumericMatrix simulation = reader.getMatrix();
		removeUnknownZones(simulation);
		// MatrixOperations.symmetrize(simulation);
		// MatrixOperations.multiply(simulation, 11.8);
		// MatrixOperations.applyDiagonalFactor(simulation, 1.3);

		ZoneCollection zones = new ZoneCollection(null);
		String data = new String(Files.readAllBytes(Paths.get("/home/johannes/gsv/gis/nuts/ger/geojson/de.nuts3.gk3.geojson")));
		zones.addAll(ZoneGeoJsonIO.parseFeatureCollection(data));
		data = null;
		zones.setPrimaryKey("gsvId");

		removeEntries(tomtom, zones, distThreshold);
		removeEntries(simulation, zones, distThreshold);
		removeEntries(itp, zones, distThreshold);

//		ODUtils.cleanVolumes(tomtom, zones, numRelations);
		double c = ODUtils.calcNormalization(tomtom, simulation);
		MatrixOperations.applyFactor(tomtom, c);
		MatrixOperations.symmetrize(tomtom);

		List<Tuple<String, String>> relTomTom = getRelations(tomtom);
		List<Tuple<String, String>> relSimu = getRelations(simulation);
		List<Tuple<String, String>> relItp = getRelations(itp);

		System.out.println(String.format("Congruency TomTom-Sim: %s", comapre(relTomTom, relSimu)));
		System.out.println(String.format("Congruency TomTom-ITP: %s", comapre(relTomTom, relItp)));

		System.out.println(String.format("Index of first difference TomTom-Sim: %s", indexOfFirstDiff(relTomTom, relSimu)));
		System.out.println(String.format("Index of first difference TomTom-ITP: %s", indexOfFirstDiff(relTomTom, relItp)));

		Map<Tuple<String, String>, Integer> rankTomTom = makeRankMap(relTomTom);
		Map<Tuple<String, String>, Integer> rankSim = makeRankMap(relSimu);
		Map<Tuple<String, String>, Integer> rankItp = makeRankMap(relItp);

		System.out.println(String.format("Avr rank diff TomTom-Sim: %s", avrRankDiff(rankTomTom, rankSim).getMean()));
		System.out.println(String.format("Avr rank diff TomTom-Itp: %s", avrRankDiff(rankTomTom, rankItp).getMean()));

		NumericMatrix dist = distanceMatrix(simulation, zones);
		writeRelations(tomtom, simulation, itp, dist, relTomTom, "/home/johannes/gsv/matrices/analysis/relations.txt");
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

		// logger.info(String.format("Removed %s trips with less than %s KM.",
		// cnt, distThreshold));
	}

	private static void writeRelations(NumericMatrix tomtom, NumericMatrix sim, NumericMatrix itp, NumericMatrix dist, List<Tuple<String, String>> relations,
									   String file) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		writer.write("from\tto\ttomtom\tsim\titp");
		writer.newLine();

		DescriptiveStatistics simStats = new DescriptiveStatistics();
		DescriptiveStatistics itpStats = new DescriptiveStatistics();

		for (Tuple<String, String> tuple : relations) {
			writer.write(tuple.getFirst());
			writer.write("\t");
			writer.write(tuple.getSecond());
			writer.write("\t");

			Double tomtomVal = tomtom.get(tuple.getFirst(), tuple.getSecond());
			if (tomtomVal == null)
				tomtomVal = 0.0;
			writer.write(String.valueOf(tomtomVal));
			writer.write("\t");

			Double val = sim.get(tuple.getFirst(), tuple.getSecond());
			if (val == null)
				val = 0.0;
			writer.write(String.valueOf(val));
			writer.write("\t");
			simStats.addValue(Math.abs((val - tomtomVal) / tomtomVal));

			val = itp.get(tuple.getFirst(), tuple.getSecond());
			if (val == null)
				val = 0.0;
			writer.write(String.valueOf(val));
			itpStats.addValue(Math.abs((val - tomtomVal) / tomtomVal));

			writer.write("\t");
			writer.write(String.valueOf(dist.get(tuple.getFirst(), tuple.getSecond())));
			writer.newLine();
		}
		writer.close();

		System.out.println("sim: " + simStats.getMean());
		System.out.println("itp: " + itpStats.getMean());
	}

	private static List<Tuple<String, String>> getRelations(final NumericMatrix m) {
//		Map<Double, Tuple<String, String>> map = new TreeMap<>(new Comparator<Double>() {
//
//			@Override
//			public int compare(Double o1, Double o2) {
//				int result = -Double.compare(o1, o2);
//				if (result == 0)
//					return 1;// o1.hashCode() - o2.hashCode();
//				else
//					return result;
//			}
//		});
//
//		Set<String> keys = m.keys();
//		for (String i : keys) {
//			for (String j : keys) {
//				Double val = m.get(i, j);
//				if (val != null) {
//					map.put(val, new Tuple<String, String>(i, j));
//				}
//			}
//		}
//
//		List<Tuple<String, String>> list = new ArrayList<>(3000);
//		int cnt = 0;
//		for (Entry<Double, Tuple<String, String>> entry : map.entrySet()) {
//			list.add(entry.getValue());
//			cnt++;
//			if (cnt > numRelations) {
//				break;
//			}
//		}
//
//		return list;
		Set<Tuple<String, String>> set = new TreeSet<>(new Comparator<Tuple<String, String>>() {

			@Override
			public int compare(Tuple<String, String> o1, Tuple<String, String> o2) {
				Double val1 = m.get(o1.getFirst(), o1.getSecond());
				Double val2 = m.get(o2.getFirst(), o2.getSecond());
			
				if(val1 == null) val1 = 0.0;
				if(val2 == null) val2 = 0.0;
				
				int result = - Double.compare(val1, val2);
				if(result == 0) {
					double id1 = Double.parseDouble(o1.getFirst());
					double id2 = Double.parseDouble(o2.getFirst());
					return Double.compare(id1, id2);
				} else {
					return result;
				}
				
			}
			
		});
		
		Set<String> keys = m.keys();
		for (String i : keys) {
			for (String j : keys) {
				Double val = m.get(i, j);
				if (val != null) {
					set.add(new Tuple<String, String>(i, j));
				}
			}
		}

		List<Tuple<String, String>> list = new ArrayList<>(3000);
		int cnt = 0;
		for(Tuple<String, String> tuple : set) {
			list.add(tuple);
			cnt++;
			if (cnt >= numRelations) {
				break;
			}
		}

		return list;
	}

	private static NumericMatrix distanceMatrix(NumericMatrix m, ZoneCollection zones) {
		GeometryFactory factory = new GeometryFactory();

		DistanceCalculator calc = new CartesianDistanceCalculator();

		NumericMatrix m_d = new NumericMatrix();
		Set<String> keys = m.keys();
		for (String i : keys) {
			Zone zone = zones.get(i);
			if (zone != null) {
				for (String j : keys) {
					if (i.equals(j)) {

						MinimumDiameter dia = new MinimumDiameter(zone.getGeometry());
						LineString ls = dia.getDiameter();
						Coordinate pi = ls.getCoordinateN(0);
						Coordinate pj = ls.getCoordinateN(1);
						// double d = dia.getLength();
						// double d = calc.distance(factory.createPoint(pi),
						// factory.createPoint(pj));
						// m_d.set(i, j, d/2.0);
						m_d.set(i, j, -1.0);

					} else {
						Point pi = zone.getGeometry().getCentroid();
						if (zones.get(j) != null) {
							Point pj = zones.get(j).getGeometry().getCentroid();

							// double d =
							// CartesianDistanceCalculator.getInstance().distance(pi,
							// pj);
							double d = calc.distance(pi, pj);
							m_d.set(i, j, d);
						}
					}

				}
			}
		}
		return m_d;
	}

	private static double comapre(List<Tuple<String, String>> list1, List<Tuple<String, String>> list2) {
		int found = 0;
		for (Tuple<?, ?> t : list2) {
			if (list1.indexOf(t) >= 0) {
				found++;
			}
		}

		return found / (double) list1.size();
	}

	private static Map<Tuple<String, String>, Integer> makeRankMap(List<Tuple<String, String>> list) {
		Map<Tuple<String, String>, Integer> map = new HashMap<>();
		for (int i = 0; i < list.size(); i++) {
			map.put(list.get(i), i);

		}
		return map;
	}

	private static DescriptiveStatistics avrRankDiff(Map<Tuple<String, String>, Integer> ranks1, Map<Tuple<String, String>, Integer> ranks2) {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		for (Entry<Tuple<String, String>, Integer> e1 : ranks1.entrySet()) {
			Integer r1 = e1.getValue();
			Integer r2 = ranks2.get(e1.getKey());
			if (r2 != null) {
				stats.addValue(Math.abs(r1 - r2));
			}
		}

		return stats;
	}

	private static int indexOfFirstDiff(List<Tuple<String, String>> list1, List<Tuple<String, String>> list2) {
		for (int i = 0; i < list1.size(); i++) {
			if (!list1.get(i).equals(list2.get(i))) {
				return i;
			}
		}
		return list1.size();
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
