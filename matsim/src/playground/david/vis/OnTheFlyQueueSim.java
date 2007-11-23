/* *********************************************************************** *
 * project: org.matsim.*
 * OnTheFlyQueueSim.java
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

import java.io.IOException;
import java.util.Date;

import org.matsim.config.Config;
import org.matsim.events.Events;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.mobsim.QueueSimulation;
import org.matsim.mobsim.Simulation;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansReaderI;
import org.matsim.world.World;

/**
 * @author DS
 *
 */
public class OnTheFlyQueueSim extends QueueSimulation{
	protected OnTheFlyServer myOTFServer = null;
	protected OTFNetFileHandler otfwriter  = null;
	
	@Override
	protected void prepareSim() {
		myOTFServer = OnTheFlyServer.createInstance("AName1", network, plans);
		if (otfwriter == null) otfwriter = new OTFNetFileHandler(10,network,"OTFNetfile.vis");
		otfwriter.open();
		
		super.prepareSim();
		
		// FOR TESTING ONLY!
		//OnTheFlyClient client = new OnTheFlyClient();
		//client.start();
	}
	
	@Override
	protected void cleanupSim() {
		myOTFServer.cleanup();
		myOTFServer = null;
		super.cleanupSim();

		try {
				otfwriter.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
	}

	@Override
	public void afterSimStep(double time) {
		super.afterSimStep(time);
		
		try {
			otfwriter.dump((int)time);
		} catch (IOException e) {
			Gbl.errorMsg("QueueSimulation.dumpWriters(): Unable to dump state.");
		}
		
		int status = myOTFServer.getStatus(time);
		
	}

	public OnTheFlyQueueSim(QueueNetworkLayer net, Plans plans, Events events) {
		super(net, plans, events);
		// TODO Auto-generated constructor stub
	}

	
	public static void main(String[] args) {		
//		String netFileName = "..\\..\\tmp\\studies\\berlin-wip\\network\\wip_net.xml";
//		String popFileName = "..\\..\\tmp\\studies\\berlin-wip\\kutter_population\\kutter010Jakob-Kaiser-RingONLY.plans.v4.xml";
//		String popFileName = "../berlin-wip/kutter_population/kutter001car5.debug.router_wip.plans.xml";

//		String netFileName = "../berlin-wip/network/wip_net.xml";
//		String popFileName = "..\\..\\..\\vsp-cvs\\studies\\berlin-wip\\synpop-2006-04\\kutter_population\\kutter010car_bln.router_wip.plans.v4.xml";
//		String popFileName = "..\\..\\tmp\\studies\\berlin-wip\\kutter_population\\kutter010car_bln.xy.plans.v4.xml";

		
//		String netFileName = "../../tmp/network.xml.gz";
		
		String netFileName = "../../tmp/studies/berlin-wip/network/wip_net.xml";
		String popFileName = "../../tmp/studies/berlin-wip/kutter_population/kutter001car5.debug.router_wip.plans.xml";
				
		Gbl.createConfig(args);
		Gbl.startMeasurement();
		Config config = Gbl.getConfig();
		config.setParam("global", "localDTDBase", "dtd/");
		
		World world = Gbl.getWorld();
		
		QueueNetworkLayer net = new QueueNetworkLayer();
		new MatsimNetworkReader(net).readFile(netFileName);
		world.setNetworkLayer(net);
		
		Plans population = new Plans();
		// Read plans file with special Reader Implementation
		PlansReaderI plansReader = new MatsimPlansReader(population);
		plansReader.readFile(popFileName);
		world.setPopulation(population);

		Events events = new Events() ;
		world.setEvents(events);
		
		config.setParam(Simulation.SIMULATION, Simulation.STARTTIME, "00:00:00");
		config.setParam(Simulation.SIMULATION, Simulation.ENDTIME, "10:02:00");

		OnTheFlyQueueSim sim = new OnTheFlyQueueSim(net, population, events);
		
		sim.run();
		
		Gbl.printElapsedTime();		
	}
	
	protected static final void printNote(String header, String action) {
		if (header != "") {
			System.out.println();
			System.out.println("===============================================================");
			System.out.println("== " + header);
			System.out.println("===============================================================");
		}
		if (action != "") {
			System.out.println("== " + action + " at " + (new Date()) );
		}
		if (header != "") {
			System.out.println();
		}
	}

	public void setOtfwriter(OTFNetFileHandler otfwriter) {
		this.otfwriter = otfwriter;
	}

	
}
