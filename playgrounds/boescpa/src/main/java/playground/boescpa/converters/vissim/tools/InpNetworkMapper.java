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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.utils.objectattributes.attributeconverters.DoubleConverter;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

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
			Id linkId = null;
			Node fromLinkNode = null;
			Node toLinkNode = null;
			while (line != null) {
				// LINKS:
				if (linkPattern.matcher(line).matches()) {
					String[] lineVals = line.split(" +");
					linkId = new IdImpl(Long.parseLong(lineVals[1]));
					inLink = true;
				}
				if (inLink) {
					if (fromPattern.matcher(line).matches()) {
						String[] lineVals = line.split(" +");
						network.addNode(networkFactory.createNode(new IdImpl(linkId.toString() + "FROM"),
								new CoordImpl(Double.parseDouble(lineVals[2]), Double.parseDouble(lineVals[3]))));
					}
					if (toPattern.matcher(line).matches()) {
						String[] lineVals = line.split(" +");
						network.addNode(networkFactory.createNode(new IdImpl(linkId.toString() + "TO"),
								new CoordImpl(Double.parseDouble(lineVals[2]), Double.parseDouble(lineVals[3]))));
						// Add link:
						Node fromNode = network.getNodes().get(new IdImpl(linkId.toString() + "FROM"));
						Node toNode = network.getNodes().get(new IdImpl(linkId.toString() + "TO"));
						network.addLink(networkFactory.createLink(linkId, fromNode, toNode));
						inLink = false;
					}
				}

				// CONNECTORS:
				if (concPattern.matcher(line).matches()) {
					String[] lineVals = line.split(" +");
					linkId = new IdImpl(Long.parseLong(lineVals[1]));
					inConc = true;
				}
				if (inConc) {
					if (fromPattern.matcher(line).matches()) {
						String[] lineVals = line.split(" +");
						String link = lineVals[3];
						double pos = Double.parseDouble(lineVals[lineVals.length - 1]);
						if (pos < 50) {
							fromLinkNode = network.getNodes().get(new IdImpl(link + "FROM"));
						} else {
							fromLinkNode = network.getNodes().get(new IdImpl(link + "TO"));
						}
					}
					if (toPattern.matcher(line).matches()) {
						String[] lineVals = line.split(" +");
						String link = lineVals[3];
						double pos = Double.parseDouble(lineVals[lineVals.length - 6]);
						if (pos < 50) {
							toLinkNode = network.getNodes().get(new IdImpl(link + "FROM"));
						} else {
							toLinkNode = network.getNodes().get(new IdImpl(link + "TO"));
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
/*
		// create links:
		int countErrLinks = 0;
		for (SimpleAnmParser.AnmLink link : links) {
			try {
				Node fromNode = network.getNodes().get(link.fromNode);
				Node toNode = network.getNodes().get(link.toNode);
				network.addLink(networkFactory.createLink(link.id, fromNode, toNode));
			} catch (NullPointerException e) {
				System.out.println("Link " + link.id.toString() + " lacks one or both nodes.");
				countErrLinks++;
			}
		}
		System.out.print(countErrLinks + " links found (and dropped) with one or both nodes lacking.\n");
*/
		return network;
	}

}
