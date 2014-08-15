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

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.io.MatsimXmlParser;

import java.util.HashSet;
import java.util.Set;

/**
 * Provides a visum-anm specific implementation of NetworkMapper.
 *
 * @author boescpa
 */
public class AmNetworkMapper extends AbstractNetworkMapper {

	/**
	 * Parses the provided Visum-Anm-File (xml-format) and transform the network into a matsim network.
	 *
	 * @param path2VissimNetworkAnm Path to a Visum-Anm-File
	 * @param notUsed	Not used here, might be provided as emtpy string
	 * @return
	 */
	@Override
	protected Network providePreparedNetwork(String path2VissimNetworkAnm, String notUsed) {
		final Network network = NetworkUtils.createNetwork();
		final NetworkFactory networkFactory = new NetworkFactoryImpl(network);
		final Set<SimpleAnmParser.AnmLink> links = new HashSet<SimpleAnmParser.AnmLink>();

		// parse anm-file:
		MatsimXmlParser xmlParser = new SimpleAnmParser(new NodeAndLinkHandler() {
			@Override
			public void handleLink(SimpleAnmParser.AnmLink anmLink) {
				links.add(anmLink);
			}
			@Override
			public void handleNode(SimpleAnmParser.AnmNode anmNode) {
				network.addNode(networkFactory.createNode(anmNode.id, anmNode.coord));
			}
		});
		xmlParser.parse(path2VissimNetworkAnm);

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

		return network;
	}

	private interface NodeAndLinkHandler extends SimpleAnmParser.AnmNodeHandler, SimpleAnmParser.AnmLinkHandler {}
}
