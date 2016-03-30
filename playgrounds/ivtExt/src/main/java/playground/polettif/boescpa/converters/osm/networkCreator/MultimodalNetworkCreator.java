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

package playground.polettif.boescpa.converters.osm.networkCreator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;

/**
 * Abstract contract for the creation of a multimodal network from an osmFile.
 * Hereby "multimodal" is understood in the sense of
 * 	"private and public transport modes that use the street network".
 *
 * @author boescpa
 */
public abstract class MultimodalNetworkCreator {

	protected static Logger log = Logger.getLogger(MultimodalNetworkCreator.class);

	protected final Network network;

	protected MultimodalNetworkCreator(Network network) {
		this.network = network;
	}

	/**
	 * Creates a multimodal network from the provided osmFile.
	 * Hereby "multimodal" is understood in the sense of
	 *	"private and public transport modes that use the street network".
	 *
	 * @param osmFile
	 */
	public abstract void createMultimodalNetwork(String osmFile);

}
