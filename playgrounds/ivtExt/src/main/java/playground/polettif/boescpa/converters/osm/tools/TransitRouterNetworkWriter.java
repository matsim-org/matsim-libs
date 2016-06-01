/* *********************************************************************** *
 * project: org.matsim.*
 * TransitNetworkWriter.java
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

package playground.polettif.boescpa.converters.osm.tools;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Counter;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkNode;

// copy from christoph-playground
public class TransitRouterNetworkWriter extends MatsimXmlWriter implements MatsimWriter {
	
	private static final Logger log = Logger.getLogger(TransitRouterNetworkWriter.class);
	
	private final TransitRouterNetwork network;

	private final Counter nodesCounter = new Counter("# written nodes: "); 
	private final Counter linksCounter = new Counter("# written links: ");
	
	public TransitRouterNetworkWriter(final TransitRouterNetwork network) {
		super();
		this.network = network;
	}

	@Override
	public void write(final String filename) throws UncheckedIOException {		
		log.info("Writing transit router network to file: " + filename  + "...");
		// always write out in newest version, currently v1
		writeFileV1(filename);
		log.info("done.");
	}

	public void writeFileV1(final String filename) throws UncheckedIOException {
//		String dtd = "http://www.matsim.org/files/dtd/transitRouterNetwork_v1.dtd";
		String dtd = "./src/main/resources/playground/christoph/evacuation/pt/transitRouterNetwork_v1.dtd";

		nodesCounter.reset();
		linksCounter.reset();

		openFile(filename);
		writeXmlHead();
		writeDoctype("transitRouterNetwork", dtd);
		this.writeStartTag("transitRouterNetwork", null);
		this.writeNodes();
		this.writeLinks();
		this.writeEndTag("transitRouterNetwork");
		this.close();
	}
		
	private void writeNodes() throws UncheckedIOException {
		this.writeStartTag("nodes", null);

		List<Tuple<String, String>> attributes = new ArrayList<Tuple<String, String>>();
		for (TransitRouterNetworkNode node : network.getNodes().values()) {
			attributes.clear();
			attributes.add(this.createTuple("id", node.getId().toString()));
			attributes.add(this.createTuple("stopfacility", node.getStop().getStopFacility().getId().toString()));
			attributes.add(this.createTuple("route", node.getRoute().getId().toString()));
			attributes.add(this.createTuple("line", node.getLine().getId().toString()));		
			this.writeStartTag("node", attributes, true);
			this.nodesCounter.incCounter();
		}

		this.writeEndTag("nodes");
		this.nodesCounter.printCounter();
	}
	
	private void writeLinks() throws UncheckedIOException {
		this.writeStartTag("links", null);

		List<Tuple<String, String>> attributes = new ArrayList<Tuple<String, String>>();
		for (TransitRouterNetworkLink link : network.getLinks().values()) {
			attributes.clear();
			attributes.add(this.createTuple("id", link.getId().toString()));
			attributes.add(this.createTuple("from", link.getFromNode().getId().toString()));
			attributes.add(this.createTuple("to", link.getToNode().getId().toString()));
			attributes.add(this.createTuple("length", link.getLength()));
			if (link.getRoute() != null) {
				attributes.add(this.createTuple("route", link.getRoute().getId().toString()));
			}
			if (link.getLine() != null) {
				attributes.add(this.createTuple("line", link.getLine().getId().toString()));
			}
			this.writeStartTag("link", attributes, true);
			this.linksCounter.incCounter();
		}

		this.writeEndTag("links");
		this.linksCounter.printCounter();
	}
}