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
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.common.gis.CRSUtils;
import org.matsim.contrib.common.util.ProgressLogger;
import org.matsim.contrib.socnetgen.sna.graph.spatial.io.ColorUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsReaderMatsimV1;
import org.matsim.counts.Volume;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.LineString;
import org.wololo.jts2geojson.GeoJSONWriter;
import playground.johannes.coopsim.utils.MatsimCoordUtils;
import playground.johannes.gsv.sim.cadyts.ODCalibrator;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.gis.ZoneGeoJsonIO;
import playground.johannes.synpop.matrix.MatrixOperations;
import playground.johannes.synpop.matrix.NumericMatrix;
import playground.johannes.synpop.matrix.NumericMatrixXMLReader;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * @author johannes
 * 
 */
public class NetLoad {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		String matrixFile = "/home/johannes/gsv/fpd/fraunhofer/study/data/matrix/24-04-2015/iais.2h.xml";
		String netFile = "/home/johannes/gsv/osm/network/germany-20140909.5.xml";
		String zonesFile = "/home/johannes/gsv/gis/modena/zones.gk3.geojson";
		String outFile = "/home/johannes/gsv/fpd/fraunhofer/study/analysis/24-04-2015/netload.geojson";
		String countsFile = "/home/johannes/gsv/counts/counts.2013.net20140909.5.24h.xml";
		String outDir = "/home/johannes/gsv/fpd/fraunhofer/study/analysis/24-04-2015/";
		/*
		 * load matrix
		 */
		NumericMatrixXMLReader mReader = new NumericMatrixXMLReader();
		mReader.setValidating(false);
		mReader.parse(matrixFile);
		NumericMatrix m = mReader.getMatrix();

		MatrixOperations.applyFactor(m, 1.952898582487276);
		/*
		 * create scenario
		 */
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		/*
		 * lead network
		 */
		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario.getNetwork());
		netReader.readFile(netFile);
		Network network = scenario.getNetwork();
		/*
		 * load counts
		 */
		Counts<Link> counts = new Counts();
		CountsReaderMatsimV1 cReader = new CountsReaderMatsimV1(counts);
		cReader.parse(countsFile);
		/*
		 * load zones
		 */
		ZoneCollection zones = ZoneGeoJsonIO.readFromGeoJSON(zonesFile, "NO", null);
		/*
		 * setup router
		 */
		FreespeedTravelTimeAndDisutility travelCost = new FreespeedTravelTimeAndDisutility(-1, 0, 0);
		DijkstraFactory dFactory = new DijkstraFactory();
		LeastCostPathCalculator router = dFactory.createPathCalculator(network, travelCost, travelCost);

		TObjectDoubleHashMap<Link> linkVolumes = new TObjectDoubleHashMap<>();
		/*
		 * assign volumes
		 */

		Set<String> keys = m.keys();
		ProgressLogger.init(keys.size(), 2, 10);
		for (String i : keys) {
			for (String j : keys) {
				if (i != j) {
					Double val = m.get(i, j);
					if (val != null && val > 0) {
						Point p1 = zones.get(i).getGeometry().getCentroid();
						Point p2 = zones.get(j).getGeometry().getCentroid();

						Node source = NetworkUtils.getNearestLink(network, new Coord(p1.getX(), p1.getY())).getFromNode();
						Node target = NetworkUtils.getNearestLink(network, new Coord(p2.getX(), p2.getY())).getFromNode();

						Path path = router.calcLeastCostPath(source, target, 0, null, null);

						for (Link link : path.links) {
							linkVolumes.adjustOrPutValue(link, val, val);
						}
					}
				}
			}
			ProgressLogger.step();
		}
		ProgressLogger.terminate();
		/*
		 * compare counts
		 */
		BufferedWriter cwriter = new BufferedWriter(new FileWriter(outDir + "counts.txt"));
		cwriter.write("obs\tsim");
		cwriter.newLine();
		for (Count count : counts.getCounts().values()) {
			Id<Link> id = count.getLocId();
			Link link = network.getLinks().get(id);
			double simVol = linkVolumes.get(link);
			double obsVol = count.getVolume(1).getValue() * 24;
			if (simVol > 0) {
				cwriter.write(String.valueOf(obsVol));
				cwriter.write("\t");
				cwriter.write(String.valueOf(simVol));
				cwriter.newLine();
			}
		}
		cwriter.close();

		writeCountsJson(network, linkVolumes, counts, outDir);
		/*
		 * write json
		 */
		Map<Link, Double> volumes = new HashMap<>(network.getLinks().size());
		Map<Link, Double> loads = new HashMap<>(network.getLinks().size());

		double maxVolume = 0;
		double minVolume = Double.MAX_VALUE;

		for (Link link : network.getLinks().values()) {
			double vol = linkVolumes.get(link);
			double load = vol / (link.getCapacity() * 24);
			volumes.put(link, vol);
			loads.put(link, load);

			maxVolume = Math.max(maxVolume, vol);
			minVolume = Math.min(minVolume, vol);

		}

		MathTransform transform = null;
		try {
			transform = CRS.findMathTransform(CRSUtils.getCRS(31467), CRSUtils.getCRS(4326));
		} catch (FactoryException e) {
			e.printStackTrace();
		}

		GeoJSONWriter jsonWriter = new GeoJSONWriter();
		List<Feature> features = new ArrayList<>(network.getLinks().size());

		for (Link link : volumes.keySet()) {
			double coord1[] = new double[] { link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY() };
			double coord2[] = new double[] { link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY() };

			try {
				transform.transform(coord1, 0, coord1, 0, 1);
				transform.transform(coord2, 0, coord2, 0, 1);
			} catch (TransformException e) {
				e.printStackTrace();
			}

			double coords[][] = new double[2][2];
			coords[0][0] = coord1[0];
			coords[0][1] = coord1[1];
			coords[1][0] = coord2[0];
			coords[1][1] = coord2[1];

			LineString lineString = new LineString(coords);

			Map<String, Object> properties = new HashMap<>();
			double volume = volumes.get(link);
			double load = loads.get(link);
			properties.put("volume", volume);
			properties.put("load", load);
			properties.put("capacity", link.getCapacity());
			double width = (volume - minVolume) / (maxVolume - minVolume);
			properties.put("width", width);
			Color color = ColorUtils.getRedGreenColor(load);
			properties.put("color", "#" + String.format("%06x", color.getRGB() & 0x00FFFFFF));

			Feature feature = new Feature(lineString, properties);
			features.add(feature);
		}

		try {
			FeatureCollection fCollection = jsonWriter.write(features);
			BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
			writer.write(fCollection.toString());
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void writeCountsJson(Network network, TObjectDoubleHashMap<Link> linkVolumes, Counts<Link> obsCounts, String outDir) {
		MathTransform transform = null;
		try {
			transform = CRS.findMathTransform(CRSUtils.getCRS(31467), CRSUtils.getCRS(4326));
		} catch (FactoryException e) {
			e.printStackTrace();
		}

		GeoJSONWriter jsonWriter = new GeoJSONWriter();
		List<Feature> simFeatures = new ArrayList<>(obsCounts.getCounts().size());
		List<Feature> obsFeatures = new ArrayList<>(obsCounts.getCounts().size());

		for (Count<Link> count : obsCounts.getCounts().values()) {
			Id<Link> linkId = count.getLocId();
			if (!linkId.toString().startsWith(ODCalibrator.VIRTUAL_ID_PREFIX)) {
				Link link = network.getLinks().get(linkId);

				double simVal = linkVolumes.get(link);
				if (simVal > 0) {
					double obsVal = 0;
					for (Volume vol : count.getVolumes().values()) {
						obsVal += vol.getValue();
					}

					double relErr = (simVal - obsVal) / obsVal;

					Coord simPos = link.getCoord();
					Coord obsPos = count.getCoord();

					Point simPoint = MatsimCoordUtils.coordToPoint(simPos);
					Point obsPoint = MatsimCoordUtils.coordToPoint(obsPos);

					simPoint = CRSUtils.transformPoint(simPoint, transform);
					obsPoint = CRSUtils.transformPoint(obsPoint, transform);

					Map<String, Object> properties = new HashMap<>();

					properties.put("simulation", simVal);
					properties.put("observation", obsVal);
					properties.put("error", relErr);
					properties.put("color",
							"#" + String.format("%06x", ColorUtils.getRedGreenColor(Math.min(1, Math.abs(relErr))).getRGB() & 0x00FFFFFF));

					Feature obsFeature = new Feature(jsonWriter.write(obsPoint), properties);
					Feature simFeature = new Feature(jsonWriter.write(simPoint), properties);

					simFeatures.add(simFeature);
					obsFeatures.add(obsFeature);
				}
			}
		}

		FeatureCollection simFCollection = jsonWriter.write(simFeatures);
		FeatureCollection obsFCollection = jsonWriter.write(obsFeatures);

		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(String.format("%s/simCounts.json", outDir)));
			writer.write(simFCollection.toString());
			writer.close();

			writer = new BufferedWriter(new FileWriter(String.format("%s/obsCounts.json", outDir)));
			writer.write(obsFCollection.toString());
			writer.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
