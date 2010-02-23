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
import java.util.Map;

import org.apache.log4j.Logger;
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
		NetworkReaderTeleatlas45v101 reader = new NetworkReaderTeleatlas45v101();
		Map<Long,JcElement> junctions = reader.parseJc("D:/balmermi/My Documents/axonActive/Servers/Raumdaten/Geodaten/TeleAtlas/02_Base/02_Raw/che/che/jc.shp");
	}

	@Test
	public void testNetworkParser() throws IOException {
		NetworkReaderTeleatlas45v101 reader = new NetworkReaderTeleatlas45v101();
		Map<Long,JcElement> junctions = reader.parseJc("D:/balmermi/My Documents/axonActive/Servers/Raumdaten/Geodaten/TeleAtlas/02_Base/02_Raw/che/che/jc.shp");
		Map<Long,NwElement> netElements = reader.parseNw("D:/balmermi/My Documents/axonActive/Servers/Raumdaten/Geodaten/TeleAtlas/02_Base/02_Raw/che/che/nw.shp",junctions);
	}
}
