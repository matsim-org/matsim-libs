/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package playground.farzadalemi.network;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.*;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Set;

/**
 * Template for a network converter
 *
 * @author boescpa
 */
public class TemplateNetworkConverter {

	private final Network newNetwork;
	private final NetworkFactory factory;

	private TemplateNetworkConverter() {
		newNetwork = NetworkUtils.createNetwork();
		factory = newNetwork.getFactory();
	}

	public static void main(final String[] args) {
		String pathToInputFile_Nodes = args[0];
		String pathToInputFile_Links = args[1];
		String pathToOutputFile = args[2]; // something like C:\MATSimStuff\network.xml.gz

		TemplateNetworkConverter networkConverter = new TemplateNetworkConverter();
		readNodes(pathToInputFile_Nodes, networkConverter);
		readLinks(pathToInputFile_Links, networkConverter);
		writeOutputFile(pathToOutputFile, networkConverter);
		testNetwork(pathToOutputFile);
	}

	/**
	 * This is the node reading method you have to adapt to your input file...
	 */
	private static void readNodes(String pathToInputFile_Nodes, TemplateNetworkConverter networkConverter) {
		BufferedReader fileReader = IOUtils.getBufferedReader(pathToInputFile_Nodes);
		try {
			String line = fileReader.readLine(); // read first line
			while (line != null) {
				String[] lineArgs = line.split(",");
				if (lineArgs[0].equals("node")) {
					// todo Create all arguments to create a MATSim node from your nodes...
					String nodeId = null;
					double xCoord = 0;
					double yCoord = 0;
					networkConverter.addNode(nodeId, xCoord, yCoord);
				} else {
					// header -> do nothing...
				}
				line = fileReader.readLine();
			}
			fileReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This is the link reading method you have to adapt to your input file...
	 */
	private static void readLinks(String pathToInputFile_Links, TemplateNetworkConverter networkConverter) {
		BufferedReader fileReader = IOUtils.getBufferedReader(pathToInputFile_Links);
		try {
			String line = fileReader.readLine(); // read first line
			while (line != null) {
				String[] lineArgs = line.split(",");
				if (lineArgs[0].equals("link")) {
					// todo Create all arguments to create a MATSim link from your links...
					String linkId = null;
					String fromNodeId = null;
					String toNodeId = null;
					Set<String> allowedModes = null;
					double capacity = 0;
					double freeSpeed = 0;
					double length = 0;
					double numberOfLanes = 0;
					networkConverter.addLink(linkId, fromNodeId, toNodeId, allowedModes, capacity, freeSpeed, length, numberOfLanes);
				} else {
					// header -> do nothing...
				}
				line = fileReader.readLine();
			}
			fileReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void addLink(String linkId, String fromNodeId, String toNodeId,
						 Set<String> allowedModes, double capacity, double freeSpeed, double length, double numberOfLanes) {
		// create link
		Node fromNode = newNetwork.getNodes().get(Id.createNodeId(fromNodeId));
		Node toNode = newNetwork.getNodes().get(Id.createNodeId(toNodeId));
		Link newLink = factory.createLink(Id.createLinkId(linkId), fromNode, toNode);
		// characterize link
		newLink.setAllowedModes(allowedModes);
		newLink.setCapacity(capacity);
		newLink.setFreespeed(freeSpeed);
		newLink.setLength(length);
		newLink.setNumberOfLanes(numberOfLanes);
		// add link
		fromNode.addOutLink(newLink);
		toNode.addInLink(newLink);
		newNetwork.addLink(newLink);
	}

	private void addNode(String nodeId, double xCoord, double yCoord) {
		Coord nodeCoords = new Coord(xCoord, yCoord);
		Node newNode = factory.createNode(Id.createNodeId(nodeId), nodeCoords);
		newNetwork.addNode(newNode);
	}

	private static void writeOutputFile(String pathToOutputFile, TemplateNetworkConverter networkConverter) {
		NetworkWriter networkWriter = new NetworkWriter(networkConverter.newNetwork);
		networkWriter.writeV1(pathToOutputFile);
	}

	/**
	 * If the written network can be read with the network reader without error messages to the user,
	 * this is a first indication that the transformation was (technically) successful.
	 */
	private static void testNetwork(String pathToOutputFile) {
		NetworkReaderMatsimV1 networkReader = new NetworkReaderMatsimV1(NetworkUtils.createNetwork());
		networkReader.parse(pathToOutputFile);
	}
}
