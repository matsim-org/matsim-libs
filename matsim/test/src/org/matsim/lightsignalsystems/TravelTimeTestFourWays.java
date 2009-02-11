/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeTestFourWay
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
package org.matsim.lightsignalsystems;

import java.io.BufferedWriter;
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
import org.matsim.mobsim.queuesim.QueueNetwork;
import org.matsim.mobsim.queuesim.QueueSimulation;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.CRCChecksum;
import org.matsim.utils.io.IOUtils;


/**
 * @author aneumann
 * @author dgrether
 *
 */
public class TravelTimeTestFourWays extends MatsimTestCase implements	LinkLeaveEventHandler, LinkEnterEventHandler, ActEndEventHandler, ActStartEventHandler, AgentArrivalEventHandler, AgentDepartureEventHandler, AgentWait2LinkEventHandler {

	BufferedWriter writer = null;
  
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		QueueNetwork.setSimulateAllLinks(true);
		QueueNetwork.setSimulateAllNodes(true);
	}
	
	public void testTrafficLightIntersection4arms() {
		Config conf = loadConfig(this.getClassInputDirectory() + "config.xml");
		String lsaDefinition = this.getClassInputDirectory() + "lsa.xml";
		String lsaConfig = this.getClassInputDirectory() + "lsa_config.xml";
		conf.signalSystems().setSignalSystemFile(lsaDefinition);
		conf.signalSystems().setSignalSystemConfigFile(lsaConfig);
		ScenarioData data = new ScenarioData(conf);
		Events events = new Events();
		events.addHandler(this);
		String tempout = this.getOutputDirectory() + "temp.txt.gz";
		try {		
			this.writer = IOUtils.getBufferedWriter(tempout, true);
//			new QSim(events, data.getPopulation(), data.getNetwork(), false, lsaDefinition, lsaConfig).run();
			QueueSimulation sim = new QueueSimulation(data.getNetwork(), data.getPopulation(), events, data.getSignalSystems(), data.getSignalSystemsConfiguration());
			sim.run();
			this.writer.flush();
			this.writer.close();
			assertEquals(CRCChecksum.getCRCFromFile(tempout),	CRCChecksum.getCRCFromFile(this.getClassInputDirectory() + "reference.txt.gz"));
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
	public void testTrafficLightIntersection4armsWithUTurn() {
		Config conf = loadConfig(this.getClassInputDirectory() + "config.xml");
		String lsaDefinition = this.getClassInputDirectory() + "lsa.xml";
		String lsaConfig = this.getClassInputDirectory() + "lsa_config.xml";
		conf.signalSystems().setSignalSystemFile(lsaDefinition);
		conf.signalSystems().setSignalSystemConfigFile(lsaConfig);
		conf.plans().setInputFile(this.getClassInputDirectory() + "plans_uturn.xml.gz");
		ScenarioData data = new ScenarioData(conf);
		Events events = new Events();
		events.addHandler(this);
		String tempout = this.getOutputDirectory() + "temp.txt.gz";
		try {		
			this.writer = IOUtils.getBufferedWriter(tempout, true);
//			new QSim(events, data.getPopulation(), data.getNetwork(), false, newLSADef, newLSADefCfg).run();
			QueueSimulation sim = new QueueSimulation(data.getNetwork(), data.getPopulation(), events, data.getSignalSystems(), data.getSignalSystemsConfiguration());
			sim.run();
			this.writer.flush();
			this.writer.close();
			assertEquals(CRCChecksum.getCRCFromFile(tempout),	CRCChecksum.getCRCFromFile(this.getClassInputDirectory() + "reference_uturn.txt.gz"));
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
		// Not used in that TestCase
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
