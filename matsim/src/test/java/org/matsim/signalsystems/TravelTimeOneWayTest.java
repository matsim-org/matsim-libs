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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.ptproject.qsim.QueueSimulation;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.lanes.LaneDefinitions;
import org.matsim.signalsystems.config.BasicPlanBasedSignalSystemControlInfo;
import org.matsim.signalsystems.config.BasicSignalGroupSettings;
import org.matsim.signalsystems.config.BasicSignalSystemConfiguration;
import org.matsim.signalsystems.config.BasicSignalSystemConfigurations;
import org.matsim.signalsystems.config.BasicSignalSystemPlan;
import org.matsim.signalsystems.systems.SignalSystems;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author aneumann
 * @author dgrether
 */
public class TravelTimeOneWayTest extends MatsimTestCase {

	private static final Logger log = Logger.getLogger(TravelTimeOneWayTest.class);

	final static int timeToWaitBeforeMeasure = 498; // Make sure measurement starts with second 0 in signalsystemplan

	public void testTrafficLightIntersection2arms_w_TrafficLight_0_60() {
		Config conf = loadConfig(this.getClassInputDirectory() + "config.xml");
		String laneDefinitions = this.getClassInputDirectory() + "testLaneDefinitions_v1.1.xml";
		String lsaDefinition = this.getClassInputDirectory() + "testSignalSystems_v1.1.xml";
		String lsaConfig = this.getClassInputDirectory() + "testSignalSystemConfigurations_v1.1.xml";
		conf.network().setLaneDefinitionsFile(laneDefinitions);
		conf.signalSystems().setSignalSystemFile(lsaDefinition);
		conf.signalSystems().setSignalSystemConfigFile(lsaConfig);
//		conf.simulation().setEndTime(21*3600.0);

		conf.scenario().setUseLanes(true);
		conf.scenario().setUseSignalSystems(true);
		ScenarioImpl data = new ScenarioImpl(conf);
		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(data);
		loader.loadScenario();
		
		LaneDefinitions lanedefs = data.getLaneDefinitions();
		
		EventsManagerImpl events = new EventsManagerImpl();
		StubLinkEnterEventHandler eventHandler = new StubLinkEnterEventHandler();
		events.addHandler(eventHandler);

//		TreeMap<Integer, MeasurementPoint> results = new TreeMap<Integer, MeasurementPoint>();

		int circulationTime = 60;

		SignalSystems lssDefs = data.getSignalSystems();
		BasicSignalSystemConfigurations lssConfigs = data.getSignalSystemConfigurations();
		
		Id id2 = new IdImpl(2);
		Id id100 = new IdImpl(100);

		for (int dropping = 1; dropping <= circulationTime; dropping++) {
			eventHandler.reset(1);
			
			for (BasicSignalSystemConfiguration lssConfig : lssConfigs.getSignalSystemConfigurations().values()) {
				BasicPlanBasedSignalSystemControlInfo controlInfo = (BasicPlanBasedSignalSystemControlInfo) lssConfig
						.getControlInfo();
				BasicSignalSystemPlan p = controlInfo.getPlans().get(id2);
				p.setCycleTime(circulationTime);
				BasicSignalGroupSettings group = p.getGroupConfigs().get(id100);
				group.setDropping(dropping);
			}
			QueueSimulation sim = new QueueSimulation(data.getNetwork(), data.getPopulation(), events);
			sim.setLaneDefinitions(lanedefs);
			sim.setSignalSystems(lssDefs, lssConfigs);
			sim.run();
//			results.put(Integer.valueOf(dropping), eventHandler.beginningOfLink2);
			log.debug("circulationTime: " + circulationTime);
			log.debug("dropping  : " + dropping);

			assertEquals((dropping * 2000.0 / circulationTime),
					eventHandler.beginningOfLink2.numberOfVehPassedDuringTimeToMeasure, 1.0);
			assertEquals(5000.0, eventHandler.beginningOfLink2.numberOfVehPassed, EPSILON);
		}
	}

	public void testTrafficLightIntersection2arms_w_TrafficLight() {
		Config conf = loadConfig(this.getClassInputDirectory() + "config.xml");
		String laneDefinitions = this.getClassInputDirectory() + "testLaneDefinitions_v1.1.xml";
		String lsaDefinition = this.getClassInputDirectory() + "testSignalSystems_v1.1.xml";
		String lsaConfig = this.getClassInputDirectory() + "testSignalSystemConfigurations_v1.1.xml";
		conf.network().setLaneDefinitionsFile(laneDefinitions);
		conf.signalSystems().setSignalSystemFile(lsaDefinition);
		conf.signalSystems().setSignalSystemConfigFile(lsaConfig);

		conf.scenario().setUseLanes(true);
		conf.scenario().setUseSignalSystems(true);
		ScenarioImpl data = new ScenarioImpl(conf);
		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(data);
		loader.loadScenario();
		
		EventsManagerImpl events = new EventsManagerImpl();
		StubLinkEnterEventHandler eventHandler = new StubLinkEnterEventHandler();
		events.addHandler(eventHandler);
		QueueSimulation sim = new QueueSimulation(data.getNetwork(), data.getPopulation(), events);
		sim.setLaneDefinitions(data.getLaneDefinitions());
		sim.setSignalSystems(data.getSignalSystems(), data.getSignalSystemConfigurations());
		sim.run();
		// log.debug("tF = 60s, " +
		// this.beginningOfLink2.numberOfVehPassedDuringTimeToMeasure + ", " +
		// this.beginningOfLink2.numberOfVehPassed + ", " +
		// this.beginningOfLink2.firstVehPassTime_s + ", " +
		// this.beginningOfLink2.lastVehPassTime_s);

		MeasurementPoint qSim = eventHandler.beginningOfLink2;
		eventHandler.reset(1);

		new QueueSimulation(data.getNetwork(), data.getPopulation(), events).run();
		if (eventHandler.beginningOfLink2 != null) {
			log.debug("tF = 60s, "
					+ eventHandler.beginningOfLink2.numberOfVehPassedDuringTimeToMeasure + ", "
					+ eventHandler.beginningOfLink2.numberOfVehPassed + ", "
					+ eventHandler.beginningOfLink2.firstVehPassTime_s + ", "
					+ eventHandler.beginningOfLink2.lastVehPassTime_s);
		}
		else {
			fail("seems like no LinkEnterEvent was handled, as this.beginningOfLink2 is not set.");
		}
		MeasurementPoint queueSimulation = eventHandler.beginningOfLink2;

		// circle time is 60s, green 60s
		assertEquals(5000.0, qSim.numberOfVehPassed, EPSILON);

		assertEquals(qSim.firstVehPassTime_s, queueSimulation.firstVehPassTime_s, EPSILON);
		assertEquals(qSim.numberOfVehPassed, queueSimulation.numberOfVehPassed, EPSILON);
		assertEquals(qSim.numberOfVehPassedDuringTimeToMeasure,
				queueSimulation.numberOfVehPassedDuringTimeToMeasure, EPSILON);
	}


	/*package*/ static class StubLinkEnterEventHandler implements LinkEnterEventHandler {

		public MeasurementPoint beginningOfLink2 = null;
		
		public void handleEvent(LinkEnterEvent event) {
			// log.info("link enter event id :" + event.linkId);
			if (event.getLinkId().toString().equalsIgnoreCase("2")) {
				if (this.beginningOfLink2 == null) {
					this.beginningOfLink2 = new MeasurementPoint(event.getTime()
							+ TravelTimeOneWayTest.timeToWaitBeforeMeasure);
				}
				
				this.beginningOfLink2.numberOfVehPassed++;
				
				if (this.beginningOfLink2.timeToStartMeasurement <= event.getTime()) {
					
					if (this.beginningOfLink2.firstVehPassTime_s == -1) {
						this.beginningOfLink2.firstVehPassTime_s = event.getTime();
					}
					
					if (event.getTime() < this.beginningOfLink2.timeToStartMeasurement
							+ MeasurementPoint.timeToMeasure_s) {
						this.beginningOfLink2.numberOfVehPassedDuringTimeToMeasure++;
						this.beginningOfLink2.lastVehPassTime_s = event.getTime();
					}
				}
			}
		}
		
		public void reset(int iteration) {
			this.beginningOfLink2 = null;
		}
	}
	
	private static class MeasurementPoint {

		static final int timeToMeasure_s = 3600;

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
