/* *********************************************************************** *
 * project: org.matsim.*
 * OnTheFlyQueueSimSWISS.java
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

package playground.david.otfvis;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.ptproject.qsim.QSim;


/**
 * @author DS
 *
 */
public class OnTheFlyQueueSimSWISS {

	public static void main(final String[] args) {
		QSim sim;
		EventsManagerImpl events;

		String netFileName = "../../tmp/network.xml.gz";

		Gbl.startMeasurement();
		ScenarioImpl scenario = new ScenarioLoaderImpl(args[0]).getScenario();
		Config config = scenario.getConfig();
		config.setParam("global", "localDTDBase", "dtd/");

		new MatsimNetworkReader(scenario).readFile(netFileName);

		Gbl.printElapsedTime();

		events = new EventsManagerImpl();

		config.simulation().setSnapshotFormat("otfvis");
		config.simulation().setSnapshotPeriod(600);
		config.simulation().setStartTime(Time.parseTime("00:00:00"));
		config.simulation().setEndTime(Time.parseTime("00:00:11"));
		sim = new QSim(scenario, events);

		sim.run();

		Gbl.printElapsedTime();
	}

}
