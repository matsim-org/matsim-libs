/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.jbischoff.taxibus.scenario.analysis.quick;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

/**
 * @author jbischoff
 *
 */
public class TBTravelTimeStatistics {

	public static void main(String[] args) {
		String inputFile = "D:/runs-svn/vw_rufbus/vwTB01/vwTB01.output_events.xml.gz";

		EventsManager events = EventsUtils.createEventsManager();
		TaxiBusTravelTimesAnalyzer a = new TaxiBusTravelTimesAnalyzer();

		events.addHandler(a);
		new MatsimEventsReader(events).readFile(inputFile);
		System.out.println(inputFile);
		a.printOutput();
	}

}
