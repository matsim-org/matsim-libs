/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
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
 * *********************************************************************** *
 */

package playground.boescpa.converters.vissim.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.regex.Pattern;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.io.IOUtils;

/**
 * Provides a vissim-inp specific implementation of NetworkMapper.
 *
 * @author boescpa
 */
public class InpNetworkMapper extends AbstractNetworkMapper {

	/**
	 * Parses the provided Vissim-Inp-File (Vissim-5.4-format) and transform the network into a matsim network.
	 *
	 * @param path2VissimNetworkInp Path to a Visum-Inp-File
	 * @param notUsed	Not used here, might be provided as emtpy string
	 * @return
	 */
	@Override
	protected Network providePreparedNetwork(String path2VissimNetworkInp, String notUsed) {
		final Network network = NetworkUtils.createNetwork();
		final NetworkFactory networkFactory = new NetworkFactoryImpl(network);

		// parse inp-file:
		try {
			final BufferedReader in = IOUtils.getBufferedReader(path2VissimNetworkInp);
			String line = in.readLine();
			Pattern linkPattern = Pattern.compile("LINK .*");
			Pattern concPattern = Pattern.compile("CONNECTOR .*");
			Pattern fromPattern = Pattern.compile(" +FROM .*");
			Pattern toPattern = Pattern.compile(" +TO .*");
			boolean inLink = false;
			boolean inConc = false;
			Id<Link> linkId = null;
			Node fromLinkNode = null;
			Node toLinkNode = null;
			while (line != null) {
				// LINKS:
				if (linkPattern.matcher(line).matches()) {
					String[] lineVals = line.split(" +");
					linkId = Id.create(Long.parseLong(lineVals[1]), Link.class);
					inLink = true;
				}
				if (inLink) {
					if (fromPattern.matcher(line).matches()) {
						String[] lineVals = line.split(" +");
						network.addNode(networkFactory.createNode(Id.create(linkId.toString() + "FROM", Node.class),
								new Coord(Double.parseDouble(lineVals[2]), Double.parseDouble(lineVals[3]))));
					}
					if (toPattern.matcher(line).matches()) {
						String[] lineVals = line.split(" +");
						network.addNode(networkFactory.createNode(Id.create(linkId.toString() + "TO", Node.class),
								new Coord(Double.parseDouble(lineVals[2]), Double.parseDouble(lineVals[3]))));
						// Add link:
						Node fromNode = network.getNodes().get(Id.create(linkId.toString() + "FROM", Link.class));
						Node toNode = network.getNodes().get(Id.create(linkId.toString() + "TO", Link.class));
						network.addLink(networkFactory.createLink(linkId, fromNode, toNode));
						inLink = false;
					}
				}

				// CONNECTORS:
				if (concPattern.matcher(line).matches()) {
					String[] lineVals = line.split(" +");
					linkId = Id.create(Long.parseLong(lineVals[1]), Link.class);
					inConc = true;
				}
				if (inConc) {
					if (fromPattern.matcher(line).matches()) {
						String[] lineVals = line.split(" +");
						String link = lineVals[3];
						double pos = Double.parseDouble(lineVals[lineVals.length - 1]);
						if (pos < 50) {
							fromLinkNode = network.getNodes().get(Id.create(link + "FROM", Node.class));
						} else {
							fromLinkNode = network.getNodes().get(Id.create(link + "TO", Node.class));
						}
					}
					if (toPattern.matcher(line).matches()) {
						String[] lineVals = line.split(" +");
						String link = lineVals[3];
						double pos = Double.parseDouble(lineVals[lineVals.length - 6]);
						if (pos < 50) {
							toLinkNode = network.getNodes().get(Id.create(link + "FROM", Node.class));
						} else {
							toLinkNode = network.getNodes().get(Id.create(link + "TO", Node.class));
						}
						// Add link:
						network.addLink(networkFactory.createLink(linkId, fromLinkNode, toLinkNode));
						inLink = false;
					}
				}

				line = in.readLine();
			}
		} catch (IOException e) {
			System.out.println("Reading of " + path2VissimNetworkInp + " failed.");
			e.printStackTrace();
		}

		return network;
	}

}
