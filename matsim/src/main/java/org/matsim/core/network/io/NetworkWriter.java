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

package org.matsim.core.network.io;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.utils.objectattributes.AttributeConverter;

public class NetworkWriter extends MatsimXmlWriter implements MatsimWriter {
	
	private static final Logger log = Logger.getLogger(NetworkWriter.class);
	
	private final Network network;
	private final CoordinateTransformation transformation;
	private final Map<Class<?>,AttributeConverter<?>> converters = new HashMap<>();

	public NetworkWriter(final Network network) {
		this( new IdentityTransformation() , network );
	}

	public NetworkWriter(
			final CoordinateTransformation transformation,
			final Network network) {
		this.transformation = transformation;
		this.network = network;
	}

	public void putAttributeConverters(final Map<Class<?>, AttributeConverter<?>> converters) {
		this.converters.putAll( converters );
	}

	public void putAttributeConverter(Class<?> clazz , AttributeConverter<?> converter) {
		this.converters.put(  clazz , converter );
	}

	@Override
	public void write(final String filename) {
		log.info("Writing network to file: " + filename  + "...");
		// always write out in newest version, currently v2
		writeFileV2(filename);
		log.info("done.");
	}

	public void writeFileV1(final String filename) {
		String dtd = "http://www.matsim.org/files/dtd/network_v1.dtd";
		NetworkWriterHandler handler = new NetworkWriterHandlerImplV1(transformation);

		writeFile( dtd , handler , filename );
	}

	public void writeFileV2(final String filename) {
		String dtd = "http://www.matsim.org/files/dtd/network_v2.dtd";
		NetworkWriterHandlerImplV2 handler = new NetworkWriterHandlerImplV2(transformation);

		handler.putAttributeConverters( converters );

		writeFile( dtd , handler , filename );
	}

	private void writeFile(
			final String dtd,
			final NetworkWriterHandler handler,
			final String filename) {

		try {
			openFile(filename);
			writeXmlHead();
			writeDoctype("network", dtd);

			handler.startNetwork(network, this.writer);
			handler.writeSeparator(this.writer);
			handler.startNodes(network, this.writer);
			for (Node n : NetworkUtils.getSortedNodes(network)) {
				handler.startNode(n, this.writer);
				handler.endNode(this.writer);
			}
			handler.endNodes(this.writer);
			handler.writeSeparator(this.writer);
			handler.startLinks(network, this.writer);
			for (Link l : NetworkUtils.getSortedLinks(network)) {
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
