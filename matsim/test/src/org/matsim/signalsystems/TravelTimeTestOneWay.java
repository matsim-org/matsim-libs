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
package org.matsim.signalsystems;

import java.util.List;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.basic.signalsystems.BasicSignalSystems;
import org.matsim.basic.signalsystemsconfig.BasicPlanBasedSignalSystemControlInfo;
import org.matsim.basic.signalsystemsconfig.BasicSignalGroupConfiguration;
import org.matsim.basic.signalsystemsconfig.BasicSignalSystemConfiguration;
import org.matsim.basic.signalsystemsconfig.BasicSignalSystemPlan;
import org.matsim.basic.v01.IdImpl;
import org.matsim.config.Config;
import org.matsim.controler.ScenarioData;
import org.matsim.events.Events;
import org.matsim.events.LinkEnterEvent;
import org.matsim.events.handler.LinkEnterEventHandler;
import org.matsim.mobsim.queuesim.QueueNetwork;
import org.matsim.mobsim.queuesim.QueueSimulation;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author aneumann
 * @author dgrether
 */
public class TravelTimeTestOneWay extends MatsimTestCase implements	LinkEnterEventHandler {
	
	private static final Logger log = Logger.getLogger(TravelTimeTestOneWay.class);
	
	private MeasurementPoint beginningOfLink2 = null;	

	final static int timeToWaitBeforeMeasure = 498; // Make sure measurement starts with second 0 in signalsystemplan 
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		QueueNetwork.setSimulateAllLinks(true);
//		QueueNetwork.setSimulateAllNodes(true);
	}
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		QueueNetwork.setSimulateAllLinks(false);
//		QueueNetwork.setSimulateAllNodes(false);
	}
		
	public void testTrafficLightIntersection2arms_w_TrafficLight_0_60(){
  	Config conf = loadConfig(this.getClassInputDirectory() + "config.xml");
		String lsaDefinition = this.getClassInputDirectory() + "lsa.xml";
		String lsaConfig = this.getClassInputDirectory() + "lsa_config.xml";
		conf.signalSystems().setSignalSystemFile(lsaDefinition);
		conf.signalSystems().setSignalSystemConfigFile(lsaConfig);
		
		ScenarioData data = new ScenarioData(conf);
		Events events = new Events();
		events.addHandler(this);
		
		TreeMap<Integer, MeasurementPoint> results = new TreeMap<Integer, MeasurementPoint>();		
		
		int circulationTime = 60;

		BasicSignalSystems lssDefs = data.getSignalSystems();
		List<BasicSignalSystemConfiguration> lssConfigs = data.getSignalSystemsConfiguration();

		for (int dropping = 1; dropping <= circulationTime; dropping=dropping+3) {
			this.beginningOfLink2 = null;
			
			for (BasicSignalSystemConfiguration lssConfig : lssConfigs) {
				BasicPlanBasedSignalSystemControlInfo controlInfo = (BasicPlanBasedSignalSystemControlInfo) lssConfig.getControlInfo();
				BasicSignalSystemPlan p = controlInfo.getPlans().get(new IdImpl("2"));
				p.setCirculationTime((double)circulationTime);
				BasicSignalGroupConfiguration group = p.getGroupConfigs().get(new IdImpl("100"));
				group.setDropping(dropping);
			}
			QueueSimulation sim = new QueueSimulation(data.getNetwork(), data.getPopulation(), events);
			sim.setSignalSystems(lssDefs, lssConfigs);
			sim.run();
			results.put(Integer.valueOf(dropping), this.beginningOfLink2);
			log.debug("circulationTime: " + circulationTime);
			log.debug("dropping  : " + dropping);
			
			assertEquals((dropping * 2000 / circulationTime), this.beginningOfLink2.numberOfVehPassedDuringTimeToMeasure, 1);
			assertEquals(5000.0, beginningOfLink2.numberOfVehPassed, EPSILON);
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
//		log.debug("tF = 60s, " + this.beginningOfLink2.numberOfVehPassedDuringTimeToMeasure + ", " + this.beginningOfLink2.numberOfVehPassed + ", " + this.beginningOfLink2.firstVehPassTime_s + ", " + this.beginningOfLink2.lastVehPassTime_s);
		
		MeasurementPoint qSim = this.beginningOfLink2;		
		this.beginningOfLink2 = null;
		
		new QueueSimulation(data.getNetwork(), data.getPopulation(), events).run();
		if (this.beginningOfLink2 != null) {
			log.debug("tF = 60s, " + this.beginningOfLink2.numberOfVehPassedDuringTimeToMeasure + 
					", " + this.beginningOfLink2.numberOfVehPassed + 
					", " + this.beginningOfLink2.firstVehPassTime_s + 
					", " + this.beginningOfLink2.lastVehPassTime_s);
		} else {
			fail("seems like no LinkEnterEvent was handled, as this.beginningOfLink2 is not set.");
		}
		MeasurementPoint queueSimulation = this.beginningOfLink2;
				
		// circle time is 60s, green 60s
		assertEquals(5000.0, qSim.numberOfVehPassed, EPSILON);

		assertEquals(qSim.firstVehPassTime_s, queueSimulation.firstVehPassTime_s, EPSILON);
		assertEquals(qSim.numberOfVehPassed, queueSimulation.numberOfVehPassed, EPSILON);
		assertEquals(qSim.numberOfVehPassedDuringTimeToMeasure, queueSimulation.numberOfVehPassedDuringTimeToMeasure, EPSILON);
		
  	}  	

	public void handleEvent(LinkEnterEvent event) {
//		log.info("link enter event id :" + event.linkId);
		if (event.linkId.equalsIgnoreCase("2")) {
			if (this.beginningOfLink2 == null){				
				this.beginningOfLink2 = new MeasurementPoint(event.time + TravelTimeTestOneWay.timeToWaitBeforeMeasure);
			}
			
			this.beginningOfLink2.numberOfVehPassed++;
			
			if( this.beginningOfLink2.timeToStartMeasurement <= event.time){				

				if (this.beginningOfLink2.firstVehPassTime_s == -1){
					this.beginningOfLink2.firstVehPassTime_s = event.time;
				}
				
				if (event.time < this.beginningOfLink2.timeToStartMeasurement + MeasurementPoint.timeToMeasure_s) {
					this.beginningOfLink2.numberOfVehPassedDuringTimeToMeasure++;
					this.beginningOfLink2.lastVehPassTime_s = event.time;
				}		
			}
		}		
	}	

	public void reset(int iteration) {
		// Not used in that TestCase
	}

	private static class MeasurementPoint{

		static final int timeToMeasure_s = 60 * 60;
		double timeToStartMeasurement;
		double firstVehPassTime_s = -1;
		double lastVehPassTime_s;
		int numberOfVehPassed = 0;
		int numberOfVehPassedDuringTimeToMeasure = 0;

		public MeasurementPoint(double time) {
			this.timeToStartMeasurement = time;
		}		
	}

}
