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
import org.matsim.testcases.MatsimTestCase;
import org.xml.sax.SAXException;


/**
 * Simple test of xml parser for traffic light signal group definitions
 * @author dgrether
 *
 */
public class SignalGroupDefinitionTest extends MatsimTestCase {

  private static final String TESTXML  = "testSignalGroupDefinition.xml";

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
			assertEquals(0, current.getId().compareTo(new Id("123")));

			/*assertEquals(0, current.getFrom().compareTo(new Id("23")));
			assertEquals(Integer.valueOf(3), current.getFromLinkLaneNumber());
			assertEquals(2, current.getToLinkIds().size());
			assertTrue(current.getToLinkIds().contains(new Id("25")));
			assertEquals(false, current.isTurnIfRed());
			assertEquals(2, current.getPassingClearingTime());
			assertEquals(Integer.valueOf(2), current.getToLinkLaneNumber(new Id("24")));
			assertEquals(Integer.valueOf(1), current.getToLinkLaneNumber(new Id("25")));
			*/

			current = signalGroups.get(1);
			assertEquals(0, current.getId().compareTo(new Id("124")));

			/*assertEquals(0, current.getFromLinkId().compareTo(new Id("26")));
			assertEquals(2, current.getToLinkIds().size());
			assertTrue(current.getToLinkIds().contains(new Id("27")));
			assertEquals(true, current.isTurnIfRed());
			assertEquals(15, current.getPassingClearingTime());
			assertEquals(Integer.valueOf(1), current.getFromLinkLaneNumber());
			for (IdI i : current.getToLinkIds()) {
				assertEquals(new Integer(1), current.getToLinkLaneNumber(i));
			}*/

		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}


	}


}
