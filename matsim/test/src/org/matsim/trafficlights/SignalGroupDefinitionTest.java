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
package org.matsim.trafficlights;

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

  private static final String TESTXML  = "signalGroupDefinition.xml";

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
			parser.parse(this.getClassInputDirectory() + TESTXML);
			assertEquals(2, signalGroups.size());
			SignalGroupDefinition current = signalGroups.get(0);
			assertEquals(0, current.getId().compareTo(new Id("123")));
			assertEquals(0, current.getFromLinkId().compareTo(new Id("23")));
			assertEquals(2, current.getToLinkIds().size());
			assertEquals(0, current.getToLinkIds().get(1).compareTo(new Id("25")));
			assertEquals(false, current.isTurnIfRed());
			assertEquals(2, current.getPassingClearingTime());

			current = signalGroups.get(1);
			assertEquals(0, current.getId().compareTo(new Id("124")));
			assertEquals(0, current.getFromLinkId().compareTo(new Id("26")));
			assertEquals(2, current.getToLinkIds().size());
			assertEquals(0, current.getToLinkIds().get(0).compareTo(new Id("27")));
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
