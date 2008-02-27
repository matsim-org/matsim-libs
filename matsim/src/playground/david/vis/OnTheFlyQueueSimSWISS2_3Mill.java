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

package playground.david.vis;

import org.matsim.config.Config;
import org.matsim.events.Events;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansReaderI;
import org.matsim.utils.misc.Time;
import org.matsim.world.World;

import playground.david.vis.executables.OnTheFlyQueueSim;


/**
 * @author DS
 *
 */
public class OnTheFlyQueueSimSWISS2_3Mill {
	
	public static void main(String[] args) {		
		OnTheFlyQueueSim sim;
		QueueNetworkLayer net;
		Plans population;
		Events events;
		
		String netFileName = "../../tmp/studies/ivtch/network.xml";
//		String popFileName = "../../tmp/studies/ivtch/plans_10pct_miv_zrh.xml.gz";
		String popFileName = "../../tmp/studies/ivtch/all_plans.xml.gz";
				
		args = new String [] {"../../tmp/studies/ivtch/config.xml"};
		Gbl.createConfig(args);
		Gbl.startMeasurement();
		Config config = Gbl.getConfig();
		config.setParam("global", "localDTDBase", "dtd/");
		
		World world = Gbl.getWorld();

		net = new QueueNetworkLayer();
		new MatsimNetworkReader(net).readFile(netFileName);
		world.setNetworkLayer(net);

		Gbl.printElapsedTime();		
		
		population = new Plans();
		PlansReaderI plansReader = new MatsimPlansReader(population);
		plansReader.readFile(popFileName);
		world.setPopulation(population);

		events = new Events() ;
		world.setEvents(events);
		
		config.simulation().setStartTime(Time.parseTime("00:00:00"));
		config.simulation().setEndTime(Time.parseTime("12:00:11"));

		sim = new OnTheFlyQueueSim(net, population, events);
		sim.setOtfwriter(new OTFQuadFileHandler.Writer (600,net,"output/OTFQuadfileSCHWEIZ2.3.mvi.gz"));
		

		sim.run();

		Gbl.printElapsedTime();		

	}
	
		
}
