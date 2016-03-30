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

package playground.johannes.gsv.fpd;

import com.vividsolutions.jts.geom.Point;
import gnu.trove.map.hash.TDoubleDoubleHashMap;
import org.matsim.contrib.common.gis.CartesianDistanceCalculator;
import org.matsim.contrib.common.gis.DistanceCalculator;
import org.matsim.contrib.common.stats.*;
import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.gis.ZoneGeoJsonIO;
import playground.johannes.synpop.matrix.MatrixOperations;
import playground.johannes.synpop.matrix.NumericMatrix;
import playground.johannes.synpop.matrix.NumericMatrixXMLReader;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

/**
 * @author johannes
 *
 */
public class Compare {

	private static Discretizer disc;

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		NumericMatrixXMLReader reader = new NumericMatrixXMLReader();
		reader.setValidating(false);

		String suffix = "v2";
		double threshold = 0;

		reader.parse(String.format("/home/johannes/gsv/fpd/telefonica/matrix%s/avr.xml", suffix));
		NumericMatrix iais = reader.getMatrix();
		MatrixOperations.applyFactor(iais, 2.0);

		reader.parse("/home/johannes/gsv/miv-matrix/refmatrices/tomtom.xml");
		NumericMatrix tomtom = reader.getMatrix();

		reader.parse("/home/johannes/sge/prj/synpop/run/902/output/nuts3/modena.miv.xml");
		NumericMatrix model = reader.getMatrix();

		reader.parse("/home/johannes/sge/prj/matsim/run/874/output/nuts3/miv.xml");
		NumericMatrix sim = reader.getMatrix();

		BufferedWriter writer = new BufferedWriter(new FileWriter
				(String.format("/home/johannes/gsv/fpd/telefonica/analysis%s/compare.txt", suffix)));
		writer.write("from\tto\tvolIais\tvolTomTom");
		writer.newLine();

		BufferedWriter scatterWriter = new BufferedWriter(new FileWriter
				(String.format("/home/johannes/gsv/fpd/telefonica/analysis%s/scatter.txt", suffix)));
		scatterWriter.write("iais\ttomtom\tmodel\tsim");
		scatterWriter.newLine();

		Set<String> keys = iais.keys();
		System.out.println("NUmber of zones: " + keys.size());
		for(String i : keys) {
			for(String j : keys) {
				Double volIais = iais.get(i, j);
				if(volIais != null && volIais >= threshold) {

					Double volTomTom = tomtom.get(i, j);
					if(volTomTom == null) volTomTom = 0.0;

					Double volModel = model.get(i, j);
					if(volModel == null) volModel = 0.0;

					Double volSim = sim.get(i, j);
					if(volSim == null) volSim = 0.0;

					writer.write(i);
					writer.write("\t");
					writer.write(j);
					writer.write("\t");
					writer.write(String.valueOf(volIais));
					writer.write("\t");
					writer.write(String.valueOf(volTomTom));
					writer.newLine();

					scatterWriter.write(String.valueOf(volIais));
					scatterWriter.write("\t");
					scatterWriter.write(String.valueOf(volTomTom));
					scatterWriter.write("\t");
					scatterWriter.write(String.valueOf(volModel));
					scatterWriter.write("\t");
					scatterWriter.write(String.valueOf(volSim));
					scatterWriter.newLine();
				}
			}
		}

		writer.close();
		scatterWriter.close();

		ZoneCollection zones = ZoneGeoJsonIO.readFromGeoJSON("/home/johannes/gsv/gis/nuts/ger/geojson/de.nuts3.gk3.geojson", "gsvId", null);
		TDoubleDoubleHashMap hist = calcDistDistribution(zones, iais, iais, threshold);
		StatsWriter.writeHistogram(hist, "d", "p", String.format("/home/johannes/gsv/fpd/telefonica/analysis%s/fpd" +
				".dist.txt", suffix));

		hist = calcDistDistribution(zones, model, iais, threshold);
		StatsWriter.writeHistogram(hist, "d", "p", String.format("/home/johannes/gsv/fpd/telefonica/analysis%s/model" +
				".dist.txt", suffix));

		hist = calcDistDistribution(zones, sim, iais, threshold);
		StatsWriter.writeHistogram(hist, "d", "p", String.format("/home/johannes/gsv/fpd/telefonica/analysis%s/sim" +
				".dist.txt", suffix));
	}

	private static TDoubleDoubleHashMap calcDistDistribution(ZoneCollection zones, NumericMatrix m, NumericMatrix relations,
															 double thrshold) {
		DistanceCalculator dCalc = new CartesianDistanceCalculator();
		DescriptivePiStatistics stats = new DescriptivePiStatistics();

		Set<String> keys = relations.keys();
		for (String i : keys) {
			Zone zi = zones.get(i);
			if(zi != null) {
//			if ("DE".equalsIgnoreCase(zi.getAttribute("ISO_CODE"))) {
				Point pi = zi.getGeometry().getCentroid();
				for (String j : keys) {
					Zone zj = zones.get(j);
					if(zj != null) {
//					if ("DE".equalsIgnoreCase(zj.getAttribute("ISO_CODE"))) {
						Double val = m.get(i, j);
						Double refVal = relations.get(i, j);
						if (val != null && refVal != null && val >= thrshold) {
							Point pj = zj.getGeometry().getCentroid();
							double d = dCalc.distance(pi, pj);
							stats.addValue(d, 1/val);
						}
					}
				}
			}
		}
		if(disc == null) disc = FixedSampleSizeDiscretizer.create(stats.getValues(), 1, 50);
		TDoubleDoubleHashMap hist = Histogram.createHistogram(stats, disc, true);
		Histogram.normalize(hist);

		return hist;
	}
}
