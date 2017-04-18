/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.contrib.bicycle.network;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

/**
 * @author smetzler, dziemke
 */
public class CreateNetworkV3MergedNetwork {

	public static void main(String[] args) {
		
		String inputCRS = "EPSG:4326";
		String outputCRS = "EPSG:31468";

		String inputOSM = "../../../shared-svn/studies/countries/de/berlin-bike/networkRawData/berlin/massnahmen/BerlinBikeNet_mod.osm";
		String scenarioName = "Berlin_falke";
		String outputXML = "../../../shared-svn/studies/countries/de/berlin-bike/input/szenarios/berlin/network/massnahmen/" + scenarioName + "_MATsim_dz2.xml.gz";
		String outputBikeXML = "../../../shared-svn/studies/countries/de/berlin-bike/input/szenarios/berlin/network/massnahmen/" + scenarioName + "_bikeObjectAtt_dz2.xml.gz";

		Network bikeNetwork = NetworkUtils.createNetwork();
//		Network carNetwork = NetworkUtils.createNetwork();
		
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(inputCRS, outputCRS);

		BicycleOsmNetworkReader bikeNetworkReader = new BicycleOsmNetworkReader(bikeNetwork, ct);
//		bikeNetworkReader.constructBikeNetwork(inputOSM); 
		bikeNetworkReader.parse(inputOSM);
		
		new ObjectAttributesXmlWriter(bikeNetworkReader.getBikeAttributes()).writeFile(outputBikeXML);

//		BicycleOsmNetworkReader carNetworkReader = new BicycleOsmNetworkReader(carNetwork,ct);
//		carNetworkReader.constructCarNetwork(inputOSM);
		
		new NetworkCleaner().run(bikeNetwork);
//		new NetworkCleaner().run(carNetwork);
		
//		Network mergedNetwork = NetworkUtils.createNetwork();
//		
//		List<Link> bikeLinks = new ArrayList<Link>(bikeNetwork.getLinks().values());
//		List<Link> carLinks = new ArrayList<Link>(carNetwork.getLinks().values());
//		
//		for (Node node : new ArrayList<Node>(bikeNetwork.getNodes().values())) {
//			bikeNetwork.removeNode(node.getId());
//			mergedNetwork.addNode(node);
//		}
//		for (Node node : new ArrayList<Node>(carNetwork.getNodes().values())) {
//			carNetwork.removeNode(node.getId());
//			if (!mergedNetwork.getNodes().containsKey(node.getId())) {
//				mergedNetwork.addNode(node);
//			}
//		}
//		for (Link link : bikeLinks) {
//			mergedNetwork.addLink(link);
//		}
//		for (Link link : carLinks) {
//			mergedNetwork.addLink(link);
//		}
//		
//		new NetworkWriter(mergedNetwork).write(outputXML);
		new NetworkWriter(bikeNetwork).write(outputXML);
	}
}