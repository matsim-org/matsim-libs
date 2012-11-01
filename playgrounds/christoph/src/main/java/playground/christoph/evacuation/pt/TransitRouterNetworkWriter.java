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

package playground.christoph.evacuation.pt;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkNode;

public class TransitRouterNetworkWriter extends MatsimXmlWriter implements MatsimWriter {
	
	private static final Logger log = Logger.getLogger(TransitRouterNetworkWriter.class);
	
	private final TransitRouterNetwork network;

	public TransitRouterNetworkWriter(final TransitRouterNetwork network) {
		super();
		this.network = network;
	}

	@Override
	public void write(final String filename) {
		log.info("Writing transit router network to file: " + filename  + "...");
		// always write out in newest version, currently v1
		writeFileV1(filename);
		log.info("done.");
	}

	public void writeFileV1(final String filename) {
//		String dtd = "http://www.matsim.org/files/dtd/transitRouterNetwork_v1.dtd";
		String dtd = "./src/main/resources/playground/christoph/evacuation/pt/transitRouterNetwork_v1.dtd";
		TransitRouterNetworkWriterHandler handler = new TransitRouterNetworkWriterHandlerImplV1();

		try {
			openFile(filename);
			writeXmlHead();
			writeDoctype("transitRouterNetwork", dtd);

			handler.startNetwork(network, this.writer);
			handler.writeSeparator(this.writer);
			handler.startNodes(network, this.writer);
			for (TransitRouterNetworkNode n : network.getNodes().values()) {
				handler.startNode(n, this.writer);
				handler.endNode(this.writer);
			}
			handler.endNodes(this.writer);
			handler.writeSeparator(this.writer);
			handler.startLinks(network, this.writer);
			for (TransitRouterNetworkLink l : network.getLinks().values()) {
				handler.startLink(l, this.writer);
				handler.endLink(this.writer);
			}
			handler.endLinks(this.writer);
			handler.writeSeparator(this.writer);
			handler.endNetwork(this.writer);
			this.writer.close();
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
}
