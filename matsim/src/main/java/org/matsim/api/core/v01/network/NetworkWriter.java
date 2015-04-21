/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
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

package org.matsim.api.core.v01.network;

import org.matsim.core.api.internal.MatsimWriter;

/**
 * @author nagel
 *
 */
public class NetworkWriter implements MatsimWriter {

	private final Network network ;
	
	public NetworkWriter(final Network network) {
		this.network = network ;
	}
	
	/**
	 * Writes the network in the current default format (currently network_v1.dtd). 
	 */
	@Override
	public void write(final String filename) {
		writeV1(filename);
	}
		
	/**
	 * Writes the network in the format of network_v1.dtd
	 * 
	 * @param filename
	 */
	public void writeV1(final String filename) {
		new org.matsim.core.network.NetworkWriter(network).writeFileV1(filename);
	}
	
}
