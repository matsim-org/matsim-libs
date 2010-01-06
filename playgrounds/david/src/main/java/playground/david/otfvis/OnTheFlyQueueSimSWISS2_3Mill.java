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
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.utils.misc.Time;
import org.matsim.ptproject.qsim.QueueSimulation;

/**
 * @author DS
 */
public class OnTheFlyQueueSimSWISS2_3Mill {

	public static void main(String[] args) {
		QueueSimulation sim;
		EventsManagerImpl events;

		String netFileName = "../../tmp/studies/ivtch/network.xml";
//		String netFileName = "../../tmp/network.xml.gz";
		String popFileName = "../../tmp/studies/ivtch/plans_10pct_miv_zrh.xml.gz";
//		String popFileName = "../../tmp/studies/ivtch/all_plans.xml.gz";

		Gbl.printSystemInfo();

		args = new String [] {"../../tmp/studies/ivtch/config.xml"};
		Config config = Gbl.createConfig(args);
		Gbl.startMeasurement();
		config.setParam("global", "localDTDBase", "dtd/");

		ScenarioImpl scenario = new ScenarioImpl(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(netFileName);

		Gbl.printElapsedTime();

		PopulationReader plansReader = new MatsimPopulationReader(scenario);
		plansReader.readFile(popFileName);

		events = new EventsManagerImpl();

		config.simulation().setSnapshotFormat("otfvis");
		config.simulation().setSnapshotPeriod(600);
		config.simulation().setSnapshotFile("output/OTFQuadfileSCHWEIZ10p.mvi");
		//config.simulation().setStartTime(Time.parseTime("00:00:00"));
		config.simulation().setEndTime(Time.parseTime("12:00:11"));
		sim = new QueueSimulation(scenario, events);

		sim.run();

		Gbl.printElapsedTime();
	}

}
