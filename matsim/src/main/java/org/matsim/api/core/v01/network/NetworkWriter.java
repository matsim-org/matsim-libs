/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.api.core.v01.network;

import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.utils.objectattributes.AttributeConverter;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author nagel
 *
 */
public class NetworkWriter implements MatsimWriter {

	private final Network network ;
	private final Map<Class<?>,AttributeConverter<?>> converters = new HashMap<>();

	public NetworkWriter(final Network network) {
		this.network = network ;
	}

	public void putAttributeConverters(final Map<Class<?>, AttributeConverter<?>> converters) {
		this.converters.putAll( converters );
	}

	public void putAttributeConverter(Class<?> clazz , AttributeConverter<?> converter) {
		this.converters.put(  clazz , converter );
	}

	/**
	 * Writes the network in the current default format (currently network_v1.dtd). 
	 */
	@Override
	public void write(final String filename) {
		writeV2(filename);
	}

	public void write(final OutputStream stream) {
		writeV2(stream);
	}

	/**
	 * Writes the network in the format of network_v1.dtd
	 * 
	 * @param filename
	 */
	public void writeV1(final String filename) {
		new org.matsim.core.network.io.NetworkWriter(network).writeFileV1(filename);
	}

	public void writeV2(final String filename) {
		final org.matsim.core.network.io.NetworkWriter writer =
				new org.matsim.core.network.io.NetworkWriter(network);
		writer.putAttributeConverters( converters );
		writer.writeFileV2(filename);
	}

	public void writeV2(final OutputStream stream) {
		final org.matsim.core.network.io.NetworkWriter writer =
				new org.matsim.core.network.io.NetworkWriter(network);
		writer.putAttributeConverters( converters );
		writer.writeStreamV2(stream);
	}

}
