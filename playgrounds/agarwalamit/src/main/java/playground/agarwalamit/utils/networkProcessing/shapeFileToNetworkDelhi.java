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
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author amit
 */
public class shapeFileToNetworkDelhi {

	private final static String networkShapeFile = "./input/sarojini/abc.shp";
	private final static String matsimNetwork = "./input/sarojini/matsimNetwork.xml";
	final  static CoordinateTransformation ct =TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,"EPSG:24378");
	private static Map<Coord, String> coordId = new HashMap<Coord, String>();

	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);

		ShapeFileReader reader = new ShapeFileReader();
		Collection<SimpleFeature> features = reader.readFileAndInitialize(networkShapeFile);

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
					node1 = ((NetworkImpl) network).createAndAddNode(fromNodeId, ct.transform(fromCoord));
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
					node2 = ((NetworkImpl) network).createAndAddNode(toNodeId, ct.transform(toCoord));
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
					((NetworkImpl) network).createAndAddLink(linkId1, node1, node2,	linkLength, freeSpeed, capacity,numberOfLanes);
				}

				if (!network.getLinks().containsKey(linkId2)) {
					((NetworkImpl) network).createAndAddLink(linkId2, node2, node1,	linkLength, freeSpeed, capacity,numberOfLanes);
				}
			}
		}
		// write network to a file
		new NetworkCleaner().run(network);
		NetworkWriter writer = new NetworkWriter(network);
		writer.write(matsimNetwork);
	}
}
