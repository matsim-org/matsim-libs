/* *********************************************************************** *
 * project: org.matsim.*
 * DepartureAtFacilityEventTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.marcel.pt.events;

import org.matsim.events.XmlEventsTester;
import org.matsim.testcases.MatsimTestCase;

public class DepartureAtFacilityEventTest extends MatsimTestCase {

	public void testWriteReadXml() {
		XmlEventsTester.testWriteReadXml(getOutputDirectory() + "events.xml", new DepartureAtFacilityEvent(3605.0, null, null));
	}
}
