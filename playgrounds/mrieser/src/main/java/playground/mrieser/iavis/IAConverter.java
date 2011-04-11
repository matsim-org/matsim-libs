/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.mrieser.iavis;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.xml.sax.SAXException;

public class IAConverter {

	private final static Logger log = Logger.getLogger(IAConverter.class);

	private static final String IN_NETWORK = "/home/mrieser/zrh25pct/network.xml.gz";
	private static final String IN_PLANS = "/home/mrieser/zrh25pct/60.plans.xml.gz";
	private static final String IN_EVENTS = "/home/mrieser/zrh25pct/60.events.txt.gz";

	private static final String OUT_LINKS = "/home/mrieser/zrh25pct/ia/links.txt";
	private static final String OUT_ACTIVITIES = "/home/mrieser/zrh25pct/ia/agentactivity.txt";
	private static final String OUT_VEHICLES = "/home/mrieser/zrh25pct/ia/agentdrive.txt";

	public static void main(String[] args) throws SAXException, ParserConfigurationException, IOException {
		log.info("start conversion");
		Scenario s = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(s).parse(IN_NETWORK);

		new IANetworkWriter().write(s.getNetwork(), OUT_LINKS);

		EventsManager e = (EventsManager) EventsUtils.createEventsManager();
		IADrivingWriter driving = new IADrivingWriter(s.getNetwork(), OUT_VEHICLES);
		e.addHandler(driving);
		IAActivitiesWriter activities = new IAActivitiesWriter(s, IN_PLANS, OUT_ACTIVITIES);
		e.addHandler(activities);

		new MatsimEventsReader(e).readFile(IN_EVENTS);

		driving.close();
		activities.close();
		log.info("conversion done.");
	}

}
