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

import gnu.trove.TObjectDoubleHashMap;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.LineString;
import org.wololo.jts2geojson.GeoJSONWriter;

import playground.johannes.gsv.zones.KeyMatrix;
import playground.johannes.gsv.zones.ZoneCollection;
import playground.johannes.gsv.zones.io.KeyMatrixXMLReader;
import playground.johannes.sna.gis.CRSUtils;
import playground.johannes.sna.graph.spatial.io.ColorUtils;

import com.vividsolutions.jts.geom.Point;

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
		String matrixFile = "/home/johannes/gsv/fpd/fraunhofer/study/data/matrix/15-04-2015/iais.2h.xml";
		String netFile = "/home/johannes/gsv/osm/network/germany-20140909.5.xml";
		String zonesFile = "/home/johannes/gsv/gis/modena/zones.gk3.geojson";
		String outFile = "/home/johannes/gsv/fpd/fraunhofer/study/analysis/15-04-2015/netload.geojson";

		KeyMatrixXMLReader mReader = new KeyMatrixXMLReader();
		mReader.setValidating(false);
		mReader.parse(matrixFile);
		KeyMatrix m = mReader.getMatrix();

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);

		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario);
		netReader.readFile(netFile);
		Network network = scenario.getNetwork();

		ZoneCollection zones = ZoneCollection.readFromGeoJSON(zonesFile, "NO");

		FreespeedTravelTimeAndDisutility travelCost = new FreespeedTravelTimeAndDisutility(-1, 0, 0);
		DijkstraFactory dFactory = new DijkstraFactory();
		LeastCostPathCalculator router = dFactory.createPathCalculator(network, travelCost, travelCost);

		TObjectDoubleHashMap<Link> linkVolumes = new TObjectDoubleHashMap<>();

		Set<String> keys = m.keys();
		for (String i : keys) {
			for (String j : keys) {
				if (i != j) {
					Double val = m.get(i, j);
					if (val != null && val > 0) {
						Point p1 = zones.get(i).getGeometry().getCentroid();
						Point p2 = zones.get(j).getGeometry().getCentroid();

						Node source = NetworkUtils.getNearestLink(network, new CoordImpl(p1.getX(), p1.getY())).getFromNode();
						Node target = NetworkUtils.getNearestLink(network, new CoordImpl(p2.getX(), p2.getY())).getFromNode();

						Path path = router.calcLeastCostPath(source, target, 0, null, null);

						for (Link link : path.links) {
							linkVolumes.adjustOrPutValue(link, val, val);
						}
					}
				}
			}
		}

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
}
