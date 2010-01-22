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

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Scenario;
import org.xml.sax.SAXException;

/**
 * @author mrieser
 */
public class NetworkWriterReaderV1Test extends AbstractNetworkWriterReaderTest {

	@Override
	protected void writeNetwork(final NetworkLayer network, final String filename) {
		new NetworkWriter(network).writeFile(filename);
	}
	
	@Override
	protected void readNetwork(final Scenario scenario, final String filename) {
		NetworkReaderMatsimV1 reader = new NetworkReaderMatsimV1(scenario);
		try {
			reader.parse(filename);
		} catch (SAXException e) {
			throw new RuntimeException(e);
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
}
