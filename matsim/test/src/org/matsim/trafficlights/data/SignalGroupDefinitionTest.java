/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
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
package org.matsim.trafficlights.data;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestCase;
import org.xml.sax.SAXException;


/**
 * Simple test of xml parser for traffic light signal group definitions
 * @author dgrether
 *
 */
public class SignalGroupDefinitionTest extends MatsimTestCase {

  private static final String TESTXML  = "testSignalGroupDefinition.xml";

  private Id id1 = new IdImpl("1");
  
  private Id id2 = new IdImpl("2");

  /**
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}


	public void testParser() {
		List<SignalGroupDefinition> signalGroups = new LinkedList<SignalGroupDefinition>();
		SignalGroupDefinitionParser parser = new SignalGroupDefinitionParser(signalGroups);
		try {
			parser.parse(this.getPackageInputDirectory() + TESTXML);
			assertEquals(2, signalGroups.size());
			SignalGroupDefinition current = signalGroups.get(0);
			assertEquals(0, current.getId().compareTo(new IdImpl("123")));
			assertEquals(2, current.getFromLanes().size());
			for (SignalLane s : current.getFromLanes()) {
				assertTrue(s.getId().equals(id1) || s.getId().equals(id2));
				assertEquals(new IdImpl("23"), s.getLinkId());
				if (s.getId().equals(id2)) {
					assertEquals(2, s.getNumberOfRepresentedLanes());				
				}
			}
			assertEquals(2, current.getToLanes().size());
			for (SignalLane s : current.getFromLanes()) {
				assertTrue(s.getId().equals(id1) || s.getId().equals(id2));
				assertEquals(new IdImpl("23"), s.getLinkId());
			}
			assertEquals(false, current.isTurnIfRed());
			assertEquals(2, current.getPassingClearingTime());

			assertTrue(current.getFromSignalLane(new IdImpl("1")).isMixedLane());
			assertEquals(45.0d, current.getFromSignalLane(new IdImpl("1")).getLength());

			current = signalGroups.get(1);
			assertEquals(0, current.getId().compareTo(new IdImpl("124")));

			assertEquals(23.3d, current.getToSignalLane(new IdImpl("3")).getLength());
			assertEquals(true, current.isTurnIfRed());
			assertEquals(15, current.getPassingClearingTime());
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}


	}


}
