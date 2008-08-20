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
package playground.andreas.intersection.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.matsim.config.Config;
import org.matsim.controler.ScenarioData;
import org.matsim.events.ActEndEvent;
import org.matsim.events.ActStartEvent;
import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.AgentWait2LinkEvent;
import org.matsim.events.Events;
import org.matsim.events.LinkEnterEvent;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.events.handler.ActEndEventHandler;
import org.matsim.events.handler.ActStartEventHandler;
import org.matsim.events.handler.AgentArrivalEventHandler;
import org.matsim.events.handler.AgentDepartureEventHandler;
import org.matsim.events.handler.AgentWait2LinkEventHandler;
import org.matsim.events.handler.LinkEnterEventHandler;
import org.matsim.events.handler.LinkLeaveEventHandler;
import org.matsim.gbl.MatsimRandom;
import org.matsim.mobsim.queuesim.QueueSimulation;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.CRCChecksum;

import playground.andreas.intersection.sim.QSim;


/**
 * @author dgrether
 *
 */
public class CompareTwoSimsBottleneck extends MatsimTestCase implements	LinkLeaveEventHandler, LinkEnterEventHandler, ActEndEventHandler, ActStartEventHandler, AgentArrivalEventHandler, AgentDepartureEventHandler, AgentWait2LinkEventHandler{
	
	BufferedWriter writer = null;
	
	public void testTrafficLightIntersection2arms_w_TrafficLight(){
  		  		
		System.setProperty("line.separator", "\n"); // Unix
//		System.setProperty("line.separator", "\r\n"); // Win
		
		Config conf = loadConfig("examples/two-routes/config.xml");
		
//		String signalSystems = "./src/playground/andreas/intersection/test/data/oneways/signalSystemConfig.xml";
//		String groupDefinitions = "./src/playground/andreas/intersection/test/data/oneways/signalGroupDefinition.xml";
		
		ScenarioData data = new ScenarioData(conf);
		Events events = new Events();
		events.addHandler(this);
		
		
		try {		
			this.writer = new BufferedWriter(new FileWriter(new File("qsim_events.txt")));
			QSim qsim = new QSim(events, data.getPopulation(), data.getNetwork(), null, null, false);
			qsim.run();
//			new QueueSimulation(data.getNetwork(), data.getPopulation(), events).run();
			this.writer.flush();
			this.writer.close();

			System.out.println();
			System.out.println();
			System.out.println();
			System.out.println();
			System.out.println();
			System.out.println();
			System.out.println();
			System.out.println();
			System.out.println();
			
			MatsimRandom.reset();
			events.resetCounter();
			this.writer = new BufferedWriter(new FileWriter(new File("queuesim_events.txt")));
		
			QueueSimulation queuesim = new QueueSimulation(data.getNetwork(), data.getPopulation(), events);
			queuesim.run();
			this.writer.flush();
			this.writer.close();
			
			assertEquals(CRCChecksum.getCRCFromFile("qsim_events.txt"),	CRCChecksum.getCRCFromFile("queuesim_events.txt"));
			
//			new File("qsim_events.txt").delete();
//			new File("queuesim_events.txt").delete();

		} catch (IOException e) {
			e.printStackTrace();
		}		
		
  	}  	

	public void handleEvent(LinkEnterEvent event) {
		try {
			this.writer.write(event.toString());
			this.writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}	
	
	public void handleEvent(LinkLeaveEvent event) {
		try {
			this.writer.write(event.toString());
			this.writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	
	public void reset(int iteration) {
	}

	public void handleEvent(ActEndEvent event) {
		try {
			this.writer.write(event.toString());
			this.writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public void handleEvent(ActStartEvent event) {
		try {
			this.writer.write(event.toString());
			this.writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public void handleEvent(AgentArrivalEvent event) {
		try {
			this.writer.write(event.toString());
			this.writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public void handleEvent(AgentDepartureEvent event) {
		try {
			this.writer.write(event.toString());
			this.writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public void handleEvent(AgentWait2LinkEvent event) {
		try {
			this.writer.write(event.toString());
			this.writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

}
