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

import org.matsim.core.config.Config;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.ptproject.qsim.QueueNetwork;
import org.matsim.ptproject.qsim.QueueSimulation;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.utils.misc.Time;



/**
 * @author DS
 *
 */
public class OnTheFlyQueueSimSWISS2_3Mill {

	public static void main(String[] args) {
		QueueSimulation sim;
		NetworkLayer net;
		PopulationImpl population;
		EventsManagerImpl events;

		String netFileName = "../../tmp/studies/ivtch/network.xml";
//		String netFileName = "../../tmp/network.xml.gz";
		String popFileName = "../../tmp/studies/ivtch/plans_10pct_miv_zrh.xml.gz";
//		String popFileName = "../../tmp/studies/ivtch/all_plans.xml.gz";

		Gbl.printSystemInfo();

		args = new String [] {"../../tmp/studies/ivtch/config.xml"};
		Gbl.createConfig(args);
		Gbl.startMeasurement();
		Config config = Gbl.getConfig();
		config.setParam("global", "localDTDBase", "dtd/");

		net = new NetworkLayer();
		new MatsimNetworkReader(net).readFile(netFileName);

		Gbl.printElapsedTime();

		population = new PopulationImpl();
		PopulationReader plansReader = new MatsimPopulationReader(population, net);
		plansReader.readFile(popFileName);

		events = new EventsManagerImpl();


		config.simulation().setSnapshotFormat("otfvis");
		config.simulation().setSnapshotPeriod(600);
		config.simulation().setSnapshotFile("output/OTFQuadfileSCHWEIZ10p.mvi");
		//config.simulation().setStartTime(Time.parseTime("00:00:00"));
		config.simulation().setEndTime(Time.parseTime("12:00:11"));
		QueueNetwork qnet = new QueueNetwork(net);
		sim = new QueueSimulation(net, population, events);


		sim.run();

		Gbl.printElapsedTime();

	}


}
