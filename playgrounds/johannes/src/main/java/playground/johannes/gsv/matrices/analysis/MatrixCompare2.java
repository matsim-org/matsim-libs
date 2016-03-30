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

import com.vividsolutions.jts.algorithm.MinimumDiameter;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import gnu.trove.map.hash.TDoubleDoubleHashMap;
import org.matsim.contrib.common.gis.WGS84DistanceCalculator;
import org.matsim.contrib.common.stats.DescriptivePiStatistics;
import org.matsim.contrib.common.stats.Histogram;
import org.matsim.contrib.common.stats.LinearDiscretizer;
import org.matsim.contrib.common.stats.StatsWriter;
import org.matsim.contrib.socnetgen.sna.snowball.analysis.WSMStatsFactory;
import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.gis.ZoneGeoJsonIO;
import playground.johannes.synpop.matrix.MatrixOperations;
import playground.johannes.synpop.matrix.NumericMatrix;
import playground.johannes.synpop.matrix.NumericMatrixXMLReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * @author johannes
 * 
 */
public class MatrixCompare2 {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		String runId = "826";
		String simFile = "/home/johannes/sge/prj/matsim/run/883/output/matrices/280/miv.misc.xml";//String.format("/home/johannes/gsv/matrices/simmatrices/miv.%s.xml", runId);
//		String simFile = "/home/johannes/gsv/matrices/simmatrices/avr/779-780/miv.sym.xml";
		String refFile = "/home/johannes/gsv/matrices/refmatrices/itp.xml";
		/*
		 * load ref matrix
		 */
		NumericMatrixXMLReader reader = new NumericMatrixXMLReader();
		reader.setValidating(false);
		reader.parse(refFile);
		NumericMatrix m1 = reader.getMatrix();

		MatrixOperations.applyFactor(m1, 1 / 365.0);

		/*
		 * load simulated matrix
		 */
//		ODMatrixXMLReader reader2 = new ODMatrixXMLReader();
//		reader2.setValidating(false);
//		reader2.parse(simFile);
		reader.parse(simFile);
		NumericMatrix m2 = reader.getMatrix();
//		KeyMatrix simulation = reader2.getMatrix().toKeyMatrix("gsvId");

//		MatrixOperations.multiply(m2, 11.8);
//		MatrixOperations.applyDiagonalFactor(m2, 1.3);
		/*
		 * load zones
		 */
		ZoneCollection zones = new ZoneCollection(null);
		String data = new String(Files.readAllBytes(Paths.get("/home/johannes/gsv/gis/nuts/de.nuts3.json")));
		zones.addAll(ZoneGeoJsonIO.parseFeatureCollection(data));
		zones.setPrimaryKey("gsvId");

		System.out.println(String.format("Intraplan sum: %s", MatrixOperations.sum(m1)));
		System.out.println(String.format("%s sum: %s", runId, MatrixOperations.sum(m2)));

//		System.exit(-1);
		
		NumericMatrix mErr = new NumericMatrix();
		MatrixOperations.errorMatrix(m1, m2, mErr);
		writeErrorRank(mErr, m1, m2, zones);

		NumericMatrix itp_d = distanceMatrix(m1, zones);
		TDoubleDoubleHashMap hist = writeDistanceHist(m1, itp_d);
		StatsWriter.writeHistogram(hist, "d", "p", "/home/johannes/gsv/matrices/analysis/itp.dist.txt");

		NumericMatrix m_d = distanceMatrix(m2, zones);
		hist = writeDistanceHist(m2, m_d);
		StatsWriter.writeHistogram(hist, "d", "p", "/home/johannes/gsv/matrices/analysis/" + runId + ".dist.stat.txt");
		
//		System.out.println(String.format("PKM Intraplan (> 100 KM): %s", pkm(m1, itp_d)));
//		System.out.println(String.format("PKM matsim (> 100 KM): %s", pkm(m2, m_d)));
		
		System.out.println(String.format("Avr length Intraplan (> 100 KM): %s", avr(m1, itp_d)));
		System.out.println(String.format("Avt length matsim (> 100 KM): %s", avr(m2, m_d)));
	}

	private static void writeErrorRank(NumericMatrix err, NumericMatrix m1, NumericMatrix m2, ZoneCollection zoneCollection) {
		List<Zone> zones = new ArrayList<>(urbanZones(zoneCollection.getZones()));
		SortedSet<Entry> rank = new TreeSet<>(new Comparator<Entry>() {

			@Override
			public int compare(Entry o1, Entry o2) {
				int result = Double.compare(o1.err, o2.err);
				if (result == 0) {
					return Integer.compare(o1.hashCode(), o2.hashCode());
				}
				return result;
			}
		});

		for (int i = 0; i < zones.size(); i++) {
			for (int j = i; j < zones.size(); j++) {
				String zi = zones.get(i).getAttribute("gsvId");
				String zj = zones.get(j).getAttribute("gsvId");

				Double errTo = err.get(zi, zj);
				if (errTo == null)
					errTo = new Double(0);

				Double errFrom = err.get(zj, zi);
				if (errFrom == null)
					errFrom = new Double(0);

				if (errTo > errFrom) {
					Entry e = new Entry();
					e.err = errTo;
					e.i = zi;
					e.j = zj;
					rank.add(e);
				} else {
					Entry e = new Entry();
					e.err = errFrom;
					e.i = zj;
					e.j = zi;
					rank.add(e);
				}
			}
		}

		for (Entry entry : rank) {
			String i = entry.i;
			String j = entry.j;

			Double errTo = err.get(i, j);
			Double errFrom = err.get(j, i);

			Double to1 = m1.get(i, j);
			Double to2 = m2.get(i, j);

			Double from1 = m1.get(j, i);
			Double from2 = m2.get(j, i);

			Zone zi = zoneCollection.get(i);
			Zone zj = zoneCollection.get(j);
			System.out.println(String.format("%s -> %s: %.3f (%.1f/%.1f) -- %.3f (%.1f/%.1f)", zi.getAttribute("nuts3_name"),
					zj.getAttribute("nuts3_name"), errTo, to1, to2, errFrom, from1, from2));
		}
	}

	private static Collection<Zone> urbanZones(Collection<Zone> zones) {
		final double threshold = 600000;

		Set<Zone> urbanZones = new HashSet<>();

		for (Zone zone : zones) {
			double pop = Double.parseDouble(zone.getAttribute("inhabitants"));
			double a = zone.getGeometry().getArea();

			double rho = pop / a;

			if (pop > threshold) {
				urbanZones.add(zone);
			}
		}

		return urbanZones;
	}

	private static class Entry {

		private Double err;

		private String i;

		private String j;
	}

	public static NumericMatrix distanceMatrix(NumericMatrix m, ZoneCollection zones) {
		GeometryFactory factory = new GeometryFactory();
		
		NumericMatrix m_d = new NumericMatrix();
		Set<String> keys = m.keys();
		for (String i : keys) {
			for (String j : keys) {
				if(i.equals(j)) {
					Zone zone = zones.get(i);
					if(zone != null) {
					MinimumDiameter dia = new MinimumDiameter(zone.getGeometry());
					LineString ls = dia.getDiameter();
					Coordinate pi = ls.getCoordinateN(0);
					Coordinate pj = ls.getCoordinateN(1);
//					double d = dia.getLength();
					double d = WGS84DistanceCalculator.getInstance().distance(factory.createPoint(pi), factory.createPoint(pj));
//					m_d.set(i, j, d/2.0);
					m_d.set(i, j, -1.0);
					}
				} else {
					Zone zi = zones.get(i);
					Zone zj = zones.get(j);
					if (zi != null && zj != null) {
						Point pi = zi.getGeometry().getCentroid();
						Point pj = zj.getGeometry().getCentroid();

						// double d =
						// CartesianDistanceCalculator.getInstance().distance(pi,
						// pj);
						double d = WGS84DistanceCalculator.getInstance().distance(pi, pj);
						m_d.set(i, j, d);
					}
				}
			}
		}
		return m_d;
	}

	private static TDoubleDoubleHashMap writeDistanceHist(NumericMatrix m, NumericMatrix m_d) {
		Set<String> keys = m.keys();
		DescriptivePiStatistics stats = new DescriptivePiStatistics();

		for (String i : keys) {
			for (String j : keys) {
				Double val = m.get(i, j);
				Double dist = m_d.get(i, j);
				// if(val == null) val = 1.0;
				if (val != null && dist != null) {
					
					stats.addValue(dist, 1 / val);
				}
			}
		}

//		return Histogram.createHistogram(stats, FixedSampleSizeDiscretizer.create(stats.getValues(), 1, 100), true);
		return Histogram.createHistogram(stats, new LinearDiscretizer(50000), false);
	}

	private static double pkm(NumericMatrix m, NumericMatrix m_d) {
		Set<String> keys = m.keys();

		double sum = 0;
		
		for (String i : keys) {
			for (String j : keys) {
				double d = m_d.get(i, j);
//				if (d > 10000) {
					Double val = m.get(i, j);
					// if(val == null) val = 1.0;
					if (val != null) {
						sum += val*d;
					}
//				}
			}
		}
		
		return sum;
	}
	
	public static double avr(NumericMatrix m, NumericMatrix m_d) {
		Set<String> keys = m.keys();
		DescriptivePiStatistics stats = new WSMStatsFactory().newInstance();
		
		for (String i : keys) {
			for (String j : keys) {
				Double d = m_d.get(i, j);
				if(d != null) {
				if(Double.isInfinite(d)) {
					System.err.println();
				} else if (Double.isNaN(d)) {
					System.err.println();
				}
				
//				if (d > 100000 && d < 1000000) {
				if (d > 0 && d < 1000000) {
					Double val = m.get(i, j);
					// if(val == null) val = 1.0;
					if (val != null && val > 0) {
						stats.addValue(d, 1/val);
					}
				}
				}
			}
		}
		
		return stats.getMean();

	}
}
