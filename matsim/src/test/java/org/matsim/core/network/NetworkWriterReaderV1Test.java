/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkWriterReaderV1Test.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.io.NetworkReaderMatsimV1;
import org.matsim.core.network.io.NetworkWriter;

/**
 * @author mrieser
 */
public class NetworkWriterReaderV1Test extends AbstractNetworkWriterReaderTest {

	@Override
	protected void writeNetwork(final Network network, final String filename) {
		new NetworkWriter(network).writeFileV1(filename);
	}
	
	@Override
	protected void readNetwork(final Scenario scenario, final String filename) {
		NetworkReaderMatsimV1 reader = new NetworkReaderMatsimV1(scenario.getNetwork());
		reader.readFile(filename);
	}
	
}
