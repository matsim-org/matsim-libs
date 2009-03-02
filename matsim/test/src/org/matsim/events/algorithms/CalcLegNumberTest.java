/* *********************************************************************** *
 * project: org.matsim.*
 * CalcLegNumberTest.java.java
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

package org.matsim.events.algorithms;

import org.matsim.testcases.MatsimTestCase;

public class CalcLegNumberTest extends MatsimTestCase {

	public void testCalcLegNumber() {
		/* The events-file contains a list of events with wrong legnumbers:
		 * - For the first group of events (those starting with 10*), the legnumber is always set to 0.
		 * - For the seconds group (those starting with 20*), the legnumber ist always set to 1,
		 *   to test if the first leg is correctly reset to 0.
		 * - The next group (those starting with 30*) has the legnumber set to 5, a legnumber that is never
		 *   valid in this example (the agent has only 3 legs in this example).
		 * - The last group (those startin gwith 60*) has the legnumber incremented by one every time, and
		 *   the initial departure event is missing. For that agent, the first 4 events should retain their
		 *   legnumbers, and the regular number should start with the first departure event at time=60010.
		 */
		// TODO [MR] remove test, as the tested class is deprecated
		//		final Events events = new Events();
//		events.addHandler(new CalcLegNumber());
//		EventWriterTXT writer = new EventWriterTXT(getOutputDirectory() + "events.txt");
//		events.addHandler(writer);
//		new MatsimEventsReader(events).readFile(getInputDirectory() + "events.txt");
//		writer.closeFile();
//
//		long cksumReference = CRCChecksum.getCRCFromFile(getInputDirectory() + "events_reference.txt");
//		long cksum = CRCChecksum.getCRCFromFile(getOutputDirectory() + "events.txt");
//		assertEquals(cksumReference, cksum);
	}

}
