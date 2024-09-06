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

package org.matsim.core.network.io;

import java.io.InputStream;
import java.io.OutputStream;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.AbstractNetworkWriterReaderTest;

/**
 * @author mrieser
 */
public class NetworkWriterReaderV2Test extends AbstractNetworkWriterReaderTest {

	@Override
	protected void writeNetwork(final Network network, final String filename) {
		new NetworkWriter(network).writeFileV2(filename);
	}

	@Override
	protected void readNetwork(final Scenario scenario, final String filename) {
		NetworkReaderMatsimV1 reader = new NetworkReaderMatsimV1(scenario.getNetwork());
		reader.readFile(filename);
	}

	@Override
	protected void writeNetwork(final Network network, final OutputStream stream) {
		new NetworkWriter(network).writeStreamV2(stream);
	}

	@Override
	protected void readNetwork(final Scenario scenario, final InputStream stream) {
		NetworkReaderMatsimV1 reader = new NetworkReaderMatsimV1(scenario.getNetwork());
		reader.parse(stream);
	}

}
