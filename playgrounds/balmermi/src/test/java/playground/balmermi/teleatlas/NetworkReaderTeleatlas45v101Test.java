/* *********************************************************************** *
 * project: org.matsim.*
 * QuadTreePerfTest.java
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

package playground.balmermi.teleatlas;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author balmermi
 */
public class NetworkReaderTeleatlas45v101Test {

	private static final Logger log = Logger.getLogger(NetworkReaderTeleatlas45v101Test.class);

	@Test
	public void testJcElement() {
		log.info(new JcElement());
	}

	@Test
	public void testNwElement() {
		log.info(new NwElement());
	}

	@Test
	public void testJunctionParser() throws IOException {
		TeleatlasData data = new TeleatlasData();
		NetworkReaderTeleatlas45v101 reader = new NetworkReaderTeleatlas45v101(data);
		Assert.assertEquals(0, data.junctionElements.size());
		reader.parseJc("D:/balmermi/My Documents/axonActive/Servers/Raumdaten/Geodaten/TeleAtlas/02_Base/02_Raw/che/che/jc.shp");
		Assert.assertTrue(data.junctionElements.size() > 0);
	}

	@Test
	public void testNetworkParser() throws IOException {
		TeleatlasData data = new TeleatlasData();
		NetworkReaderTeleatlas45v101 reader = new NetworkReaderTeleatlas45v101(data);
		reader.parseJc("D:/balmermi/My Documents/axonActive/Servers/Raumdaten/Geodaten/TeleAtlas/02_Base/02_Raw/che/che/jc.shp");
		Assert.assertEquals(0, data.networkElements.size());
		reader.parseNw("D:/balmermi/My Documents/axonActive/Servers/Raumdaten/Geodaten/TeleAtlas/02_Base/02_Raw/che/che/nw.shp");
		Assert.assertTrue(data.networkElements.size() > 0);
	}
}
