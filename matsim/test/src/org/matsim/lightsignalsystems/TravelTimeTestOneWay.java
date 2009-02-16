/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeTestOneWay
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

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.basic.lightsignalsystems.BasicLightSignalSystems;
import org.matsim.basic.lightsignalsystemsconfig.BasicLightSignalGroupConfiguration;
import org.matsim.basic.lightsignalsystemsconfig.BasicLightSignalSystemConfiguration;
import org.matsim.basic.lightsignalsystemsconfig.BasicLightSignalSystemPlan;
import org.matsim.basic.lightsignalsystemsconfig.BasicPlanBasedLightSignalSystemControlInfo;
import org.matsim.basic.v01.IdImpl;
import org.matsim.config.Config;
import org.matsim.controler.ScenarioData;
import org.matsim.events.Events;
import org.matsim.events.LinkEnterEvent;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.events.handler.LinkEnterEventHandler;
import org.matsim.events.handler.LinkLeaveEventHandler;
import org.matsim.mobsim.queuesim.QueueNetwork;
import org.matsim.mobsim.queuesim.QueueSimulation;
import org.matsim.testcases.MatsimTestCase;


/**
 * @author aneumann
 * @author dgrether
 * 
 *
 */
public class TravelTimeTestOneWay extends MatsimTestCase implements	LinkLeaveEventHandler, LinkEnterEventHandler {
	
	private static final Logger log = Logger
			.getLogger(TravelTimeTestOneWay.class);
	
	private MeasurementPoint beginningOfLink2 = null;	

	final static int timeToWaitBeforeMeasure = 498; // Make sure measurement starts with second 0 in signalsystemplan 
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		QueueNetwork.setSimulateAllLinks(true);
		QueueNetwork.setSimulateAllNodes(true);
	}
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		QueueNetwork.setSimulateAllLinks(false);
		QueueNetwork.setSimulateAllNodes(false);
	}
		
	public void estTrafficLightIntersection2arms_w_TrafficLight_0_60(){
  	Config conf = loadConfig(this.getClassInputDirectory() + "config.xml");
		String lsaDefinition = this.getClassInputDirectory() + "lsa.xml";
		String lsaConfig = this.getClassInputDirectory() + "lsa_config.xml";
		String tempFile = this.getOutputDirectory() + "__tempFile__.xml";
		conf.signalSystems().setSignalSystemFile(lsaDefinition);
		conf.signalSystems().setSignalSystemConfigFile(lsaConfig);
		
		ScenarioData data = new ScenarioData(conf);
		BasicLightSignalSystems signalSystems = data.getSignalSystems();
		
		Events events = new Events();
		events.addHandler(this);
		
		TreeMap<Integer, MeasurementPoint> results = new TreeMap<Integer, MeasurementPoint>();		
		
		int umlaufzeit = 60;
		
		
		
		for (int i = 1; i <= umlaufzeit; i++) {
			this.beginningOfLink2 = null;

			List<BasicLightSignalSystemConfiguration> lssConfigs = new ArrayList<BasicLightSignalSystemConfiguration>();
			MatsimLightSignalSystemConfigurationReader reader = new MatsimLightSignalSystemConfigurationReader(lssConfigs);
			reader.readFile(lsaConfig);
			for (BasicLightSignalSystemConfiguration lssConfig : lssConfigs) {
				BasicPlanBasedLightSignalSystemControlInfo controlInfo = (BasicPlanBasedLightSignalSystemControlInfo) lssConfig.getControlInfo();
				BasicLightSignalSystemPlan p = controlInfo.getPlans().get(new IdImpl("2"));
				p.setCirculationTime((double)umlaufzeit);
				BasicLightSignalGroupConfiguration group = p.getGroupConfigs().get(new IdImpl("100"));
				group.setDropping(i);
			}
			
			MatsimLightSignalSystemConfigurationWriter writer = new MatsimLightSignalSystemConfigurationWriter(lssConfigs);
			writer.writeFile(tempFile);
			QueueSimulation sim = new QueueSimulation(data.getNetwork(), data.getPopulation(), events);
			sim.setSignalSystems(data.getSignalSystems(), data.getSignalSystemsConfiguration());
			sim.run();
//			new QSim(events, data.getPopulation(), data.getNetwork(), false, lsaDefinition, tempFile).run();
			results.put(Integer.valueOf(i), this.beginningOfLink2);
		}
		
		int j = 1;
		for (MeasurementPoint resMeasurePoint : results.values()) {
			log.debug(j + ", " + resMeasurePoint.numberOfVehPassedDuringTimeToMeasure_ + ", " + resMeasurePoint.numberOfVehPassed_ + ", " + this.beginningOfLink2.timeToStartMeasurement + ", " + resMeasurePoint.firstVehPassTime_s + ", " + resMeasurePoint.lastVehPassTime_s + ", " + (resMeasurePoint.numberOfVehPassedDuringTimeToMeasure_ - j * 2000 / umlaufzeit));
			assertEquals((j * 2000 / umlaufzeit), resMeasurePoint.numberOfVehPassedDuringTimeToMeasure_, 1);
			j++;
			assertEquals(5000.0, resMeasurePoint.numberOfVehPassed_, EPSILON);
		}
	}	
	
	public void testTrafficLightIntersection2arms_w_TrafficLight(){
  	Config conf = loadConfig(this.getClassInputDirectory() + "config.xml");
		String lsaDefinition = this.getClassInputDirectory() + "lsa.xml";
		String lsaConfig = this.getClassInputDirectory() + "lsa_config.xml";
		conf.signalSystems().setSignalSystemFile(lsaDefinition);
		conf.signalSystems().setSignalSystemConfigFile(lsaConfig);
		
		ScenarioData data = new ScenarioData(conf);
		Events events = new Events();
		events.addHandler(this);
		QueueSimulation sim = new QueueSimulation(data.getNetwork(), data.getPopulation(), events);
		sim.setSignalSystems(data.getSignalSystems(), data.getSignalSystemsConfiguration());
		sim.run();
//		new QSim(events, data.getPopulation(), data.getNetwork(), false, lsaDefinition, lsaConfig).run();
		log.debug("tF = 60s, " + this.beginningOfLink2.numberOfVehPassedDuringTimeToMeasure_ + ", " + this.beginningOfLink2.numberOfVehPassed_ + ", " + this.beginningOfLink2.firstVehPassTime_s + ", " + this.beginningOfLink2.lastVehPassTime_s);
		
		MeasurementPoint qSim = this.beginningOfLink2;		
		this.beginningOfLink2 = null;
		
		new QueueSimulation(data.getNetwork(), data.getPopulation(), events).run();
		log.debug("tF = 60s, " + this.beginningOfLink2.numberOfVehPassedDuringTimeToMeasure_ + 
				", " + this.beginningOfLink2.numberOfVehPassed_ + 
				", " + this.beginningOfLink2.firstVehPassTime_s + 
				", " + this.beginningOfLink2.lastVehPassTime_s);
		MeasurementPoint queueSimulation = this.beginningOfLink2;
				
		// circle time is 60s, green 60s
		assertEquals(5000.0, qSim.numberOfVehPassed_, EPSILON);

		assertEquals(qSim.firstVehPassTime_s, queueSimulation.firstVehPassTime_s, EPSILON);
		assertEquals(qSim.numberOfVehPassed_, queueSimulation.numberOfVehPassed_, EPSILON);
		assertEquals(qSim.numberOfVehPassedDuringTimeToMeasure_, queueSimulation.numberOfVehPassedDuringTimeToMeasure_, EPSILON);
		
  	}  	

	public void handleEvent(LinkEnterEvent event) {
//		log.info("link enter event id :" + event.linkId);
		if (event.linkId.equalsIgnoreCase("2")) {
			if (this.beginningOfLink2 == null){				
				this.beginningOfLink2 = new MeasurementPoint(event.time + TravelTimeTestOneWay.timeToWaitBeforeMeasure);
			}
			
			this.beginningOfLink2.numberOfVehPassed_++;
			
			if( this.beginningOfLink2.timeToStartMeasurement <= event.time){				

				if (this.beginningOfLink2.firstVehPassTime_s == -1){
					this.beginningOfLink2.firstVehPassTime_s = event.time;
				}
				
				if (event.time < this.beginningOfLink2.timeToStartMeasurement + this.beginningOfLink2.timeToMeasure_s){
					this.beginningOfLink2.numberOfVehPassedDuringTimeToMeasure_++;
					this.beginningOfLink2.lastVehPassTime_s = event.time;
				}		
			}
		}		
	}	
	
	public void handleEvent(@SuppressWarnings("unused") LinkLeaveEvent event) {
		// Not used in that TestCase
	}

	public void reset(@SuppressWarnings("unused") int iteration) {
		// Not used in that TestCase
	}
	
	private class MeasurementPoint{
		
		private final int timeToMeasure_s = 60 * 60;
		double timeToStartMeasurement;
		double firstVehPassTime_s = -1;
		double lastVehPassTime_s;
	  	int numberOfVehPassed_ = 0;
	  	int numberOfVehPassedDuringTimeToMeasure_ = 0;
		
		public MeasurementPoint(double time) {
			this.timeToStartMeasurement = time;
		}		
	}

}
