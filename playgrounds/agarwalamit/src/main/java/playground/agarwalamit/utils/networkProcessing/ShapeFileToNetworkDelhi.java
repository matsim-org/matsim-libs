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
package playground.agarwalamit.utils.networkProcessing;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.geotools.feature.simple.SimpleFeatureTypeImpl;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

/**
 * @author amit
 */
public class ShapeFileToNetworkDelhi {

	private final static String NETWORK_SHAPE_FILE = "./input/sarojini/abc.shp";
	private final static String MATSIM_NETWORK = "./input/sarojini/matsimNetwork.xml";
	private final static CoordinateTransformation CT =TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,"EPSG:24378");
	private static final Map<Coord, String> coordId = new HashMap<>();

	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);

		ShapeFileReader reader = new ShapeFileReader();
		Collection<SimpleFeature> features = reader.readFileAndInitialize(NETWORK_SHAPE_FILE);

		Network network = scenario.getNetwork();

		for(SimpleFeature sf : features){
			// reads every feature here (corresponding to every line in attribute table)
			if(sf.getFeatureType() instanceof SimpleFeatureTypeImpl){
				// create from Node
				Double fromNodeX=	(Double) sf.getAttribute("fromX");
				Double fromNodeY = (Double) sf.getAttribute("fromY");
				Coord fromCoord = new Coord(fromNodeX, fromNodeY);
				String fNodeIdStr;

				if(coordId.containsKey(fromCoord)) {
					fNodeIdStr = coordId.get(fromCoord);
				} else {
					fNodeIdStr = String.valueOf(sf.getAttribute("fromId"));
					coordId.put(fromCoord, fNodeIdStr);
				}

				Id<Node> fromNodeId = Id.create(fNodeIdStr,Node.class);
				Node node1;

				if(!network.getNodes().containsKey((fromNodeId))){
					final Id<Node> id = fromNodeId;
					node1 = NetworkUtils.createAndAddNode(network, id, CT.transform(fromCoord));
				}else{
					node1=network.getNodes().get((fromNodeId));
				}

				// create to Node
				Double toNodeX = (Double) sf.getAttribute("toX");
				Double toNodeY = (Double) sf.getAttribute("toY");
				Coord toCoord = new Coord(toNodeX, toNodeY);

				String tNodeIdStr;

				if(coordId.containsKey(toCoord)) {
					tNodeIdStr = coordId.get(toCoord);
				} else {
					tNodeIdStr = String.valueOf(sf.getAttribute("toId"));
					coordId.put(toCoord, tNodeIdStr);
				}

				Id<Node> toNodeId = Id.create(tNodeIdStr,Node.class);

				Node node2;

				if(!network.getNodes().containsKey((toNodeId))){
					final Id<Node> id = toNodeId;
					node2 = NetworkUtils.createAndAddNode(network, id, CT.transform(toCoord));
				}else{
					node2 = network.getNodes().get((toNodeId));
				}

				// matsim have one way links thus create two links for both directions
				Id<Link> linkId1 = Id.create(node1.getId().toString()+"_"+node2.getId().toString(),Link.class);
				Id<Link> linkId2 =Id.create(node2.getId().toString()+"_"+node1.getId().toString(),Link.class);

				if(node1.equals(node2)){
					//					throw new RuntimeException();
				}

				// following parameters are necessary for simulation, I just used some data to show how it works.
				double linkLength = (Double) sf.getAttribute("LENGTH");
				double capacity = 1800;//(1800*(Double) sf.getAttribute("WIDTH"))/3.5;;
				double numberOfLanes = 2;
				double freeSpeed = 60/3.6;

				// add links to network
				if (!network.getLinks().containsKey(linkId1)) {
					final Id<Link> id = linkId1;
					final Node fromNode = node1;
					final Node toNode = node2;
					final double length = linkLength;
					final double freespeed = freeSpeed;
					final double capacity1 = capacity;
					final double numLanes = numberOfLanes;
					NetworkUtils.createAndAddLink(network,id, fromNode, toNode, length, freespeed, capacity1, numLanes );
				}

				if (!network.getLinks().containsKey(linkId2)) {
					final Id<Link> id = linkId2;
					final Node fromNode = node2;
					final Node toNode = node1;
					final double length = linkLength;
					final double freespeed = freeSpeed;
					final double capacity1 = capacity;
					final double numLanes = numberOfLanes;
					NetworkUtils.createAndAddLink(network,id, fromNode, toNode, length, freespeed, capacity1, numLanes );
				}
			}
		}
		// write network to a file
		new NetworkCleaner().run(network);
		NetworkWriter writer = new NetworkWriter(network);
		writer.write(MATSIM_NETWORK);
	}
}