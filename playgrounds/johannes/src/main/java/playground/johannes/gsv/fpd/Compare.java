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
import gnu.trove.TDoubleDoubleHashMap;
import playground.johannes.gsv.zones.KeyMatrix;
import playground.johannes.gsv.zones.io.KeyMatrixXMLReader;
import playground.johannes.sna.math.DescriptivePiStatistics;
import playground.johannes.sna.math.Discretizer;
import playground.johannes.sna.math.FixedSampleSizeDiscretizer;
import playground.johannes.sna.math.Histogram;
import playground.johannes.sna.util.TXTWriter;
import playground.johannes.socialnetworks.gis.CartesianDistanceCalculator;
import playground.johannes.socialnetworks.gis.DistanceCalculator;
import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneCollection;

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
		KeyMatrixXMLReader reader = new KeyMatrixXMLReader();
		reader.setValidating(false);

//		reader.parse("/home/johannes/gsv/fpd/fraunhofer/study/data/matrix/24-04-2015/iais.3d.xml");
		reader.parse("/home/johannes/gsv/fpd/telefonica/matrix/avr.xml");
		KeyMatrix iais = reader.getMatrix();

		reader.parse("/home/johannes/gsv/miv-matrix/refmatrices/tomtom.xml");
		KeyMatrix tomtom = reader.getMatrix();

		reader.parse("/home/johannes/sge/prj/synpop/run/902/output/nuts3/modena.miv.xml");
		KeyMatrix model = reader.getMatrix();

		reader.parse("/home/johannes/sge/prj/matsim/run/874/output/nuts3/miv.xml");
		KeyMatrix sim = reader.getMatrix();

//		ZoneCollection zones = ZoneCollection.readFromGeoJSON("/home/johannes/gsv/fpd/fraunhofer/study/data/gis/zones.geojson", "NO");
//		ODUtils.cleanDistances(iais, zones, 10000, WGS84DistanceCalculator.getInstance());
//		

//		double c = ODUtils.calcNormalization(iais, model);
//		playground.johannes.gsv.zones.MatrixOperations.applyFactor(model, 1/c);
//
//		c = ODUtils.calcNormalization(iais, tomtom);
//		playground.johannes.gsv.zones.MatrixOperations.applyFactor(tomtom, 1/c);
//
//		c = ODUtils.calcNormalization(iais, sim);
//		playground.johannes.gsv.zones.MatrixOperations.applyFactor(sim, 1/c);

		BufferedWriter writer = new BufferedWriter(new FileWriter("/home/johannes/gsv/fpd/telefonica/analysis/compare.txt"));
		writer.write("from\tto\tvolIais\tvolTomTom");
		writer.newLine();

//		TDoubleArrayList iaisVols = new TDoubleArrayList();
//		TDoubleArrayList tomtomVols = new TDoubleArrayList();

		BufferedWriter scatterWriter = new BufferedWriter(new FileWriter("/home/johannes/gsv/fpd/telefonica/analysis/scatter.txt"));
		scatterWriter.write("iais\ttomtom\tmodel\tsim");
		scatterWriter.newLine();

		Set<String> keys = iais.keys();
		for(String i : keys) {
			for(String j : keys) {
				Double volIais = iais.get(i, j);
				if(volIais != null) {

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

		ZoneCollection zones = ZoneCollection.readFromGeoJSON("/home/johannes/gsv/gis/nuts/ger/geojson/de.nuts3.gk3.geojson", "gsvId");
		TDoubleDoubleHashMap hist = calcDistDistribution(zones, iais, iais);
		TXTWriter.writeMap(hist, "d", "p", "/home/johannes/gsv/fpd/telefonica/analysis/fpd.dist.txt");

		hist = calcDistDistribution(zones, model, iais);
		TXTWriter.writeMap(hist, "d", "p", "/home/johannes/gsv/fpd/telefonica/analysis/model.dist.txt");

		hist = calcDistDistribution(zones, sim, iais);
		TXTWriter.writeMap(hist, "d", "p", "/home/johannes/gsv/fpd/telefonica/analysis/sim.dist.txt");
	}

	private static TDoubleDoubleHashMap calcDistDistribution(ZoneCollection zones, KeyMatrix m, KeyMatrix relations) {
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
						if (val != null && refVal != null) {
							Point pj = zj.getGeometry().getCentroid();
							double d = dCalc.distance(pi, pj);
							stats.addValue(d, 1/val);
						}
					}
				}
			}
		}
		if(disc == null) disc = FixedSampleSizeDiscretizer.create(stats.getValues(), 20);
		TDoubleDoubleHashMap hist = Histogram.createHistogram(stats, disc, true);
		Histogram.normalize(hist);

		return hist;
	}
}
