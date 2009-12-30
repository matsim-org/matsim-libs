/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.core.network;

import java.io.IOException;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.internal.MatsimFileWriter;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.MatsimXmlWriter;

public class NetworkWriter extends MatsimXmlWriter implements MatsimFileWriter {

	private final NetworkLayer network;

	public NetworkWriter(final Network network) {
		super();
		this.network = (NetworkLayer) network;
	}
	
	public void writeFile(final String filename) {
		// always write out in newest version, currently v1
		writeFileV1(filename);
	}

	public void writeFileV1(final String filename) {
		String dtd = "http://www.matsim.org/files/dtd/network_v1.dtd";
		NetworkWriterHandler handler = new NetworkWriterHandlerImplV1();

		try {
			openFile(filename);
			writeXmlHead();
			writeDoctype("network", dtd);

			handler.startNetwork(network, this.writer);
			handler.writeSeparator(this.writer);
			handler.startNodes(network, this.writer);
			for (NodeImpl n : network.getNodes().values()) {
				handler.startNode(n, this.writer);
				handler.endNode(this.writer);
			}
			handler.endNodes(this.writer);
			handler.writeSeparator(this.writer);
			handler.startLinks(network, this.writer);
			for (LinkImpl l : network.getLinks().values()) {
				handler.startLink(l, this.writer);
				handler.endLink(this.writer);
			}
			handler.endLinks(this.writer);
			handler.writeSeparator(this.writer);
			handler.endNetwork(this.writer);
			this.writer.close();
		}
		catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}
	
}
