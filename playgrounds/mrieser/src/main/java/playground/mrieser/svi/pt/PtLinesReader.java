/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.mrieser.svi.pt;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.StringUtils;

/**
 * @author mrieser
 */
public class PtLinesReader {

	private final static Logger log = Logger.getLogger(PtLinesReader.class);
	
	private final PtLines lines;
	private final Network network;
	
	public PtLinesReader(final PtLines lines, final Network network) {
		this.lines = lines;
		this.network = network;
	}
	
	public void readFile(final String filename) {
		BufferedReader reader = IOUtils.getBufferedReader(filename);
		try {
			String line;
			while ((line = reader.readLine()) != null) {
				String[] parts = StringUtils.explode(line, '\t');
				String name = parts[0];
				String direction = parts[1];
				Node prevNode = null;
				List<Link> links = new ArrayList<Link>();
				for (int i = 2; i < parts.length; i++) {
					Id<Node> id = Id.create(parts[i], Node.class);
					Node node = this.network.getNodes().get(id);
					if (node == null) {
						log.error("Could not find node with id " + id.toString() + " in line " + line + "/" + direction);
						links.clear();
						break;
					}
					if (prevNode != null) {
						Link link = NetworkUtils.getConnectingLink(prevNode, node);
						if (link == null) {
							log.warn("Bad pt line description for line " + line + "/" + direction + ": No link between node " + prevNode.getId() + " and " + node.getId());
						} else {
							links.add(link);
						}
					}
					prevNode = node;
				}
				if (links.size() > 0) {
					this.lines.add(new PtLine(name, direction, links));
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				log.error("Could not close reader for " + filename);
			}
		}
	}
}
