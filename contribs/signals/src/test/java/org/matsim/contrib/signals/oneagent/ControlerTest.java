/* *********************************************************************** *
 * project: org.matsim.*
 * ControlerTests
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package org.matsim.contrib.signals.oneagent;

import junit.framework.Assert;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.contrib.signals.events.SignalGroupStateChangedEvent;
import org.matsim.contrib.signals.events.SignalGroupStateChangedEventHandler;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupSettingsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalSystemControllerData;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.testcases.utils.EventsLogger;


/**
 * Contains tests that use the signals one agent scenario as base and test functionality of the Controler.
 * @author dgrether
 *
 */
public class ControlerTest {
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();
	
	/**
	 * Tests the setup with a traffic light that shows all the time green in the 0th iteration.
	 * After the mobsim is run the signal settings are changed thus in the 1st iteration
	 * the signal should be red in sec [0,99] and green in [100,2000]
	 */
	@Test
	public void testModifySignalControlDataOnsetOffset() {
		//configure and load standard scenario
		Fixture fixture = new Fixture();
		Scenario scenario = fixture.createAndLoadTestScenario(false);
		scenario.getConfig().controler().setFirstIteration(0);
		scenario.getConfig().controler().setLastIteration(1);
		scenario.getConfig().controler().setOutputDirectory(testUtils.getOutputDirectory());
		
		Controler controler = new Controler(scenario);
        controler.getConfig().controler().setCreateGraphs(false);
        controler.addControlerListener(new AfterMobsimListener() {

			@Override
			public void notifyAfterMobsim(AfterMobsimEvent event) {
				Scenario scenario = event.getServices().getScenario();
				int dropping = 0;
				int onset = 100;
				for (SignalSystemControllerData intersectionSignal :
					((SignalsData) scenario
						.getScenarioElement(SignalsData.ELEMENT_NAME)).getSignalControlData()
						.getSignalSystemControllerDataBySystemId().values()) {
					
					for (SignalPlanData plan : intersectionSignal.getSignalPlanData().values()) {
						plan.setCycleTime(2000);
						for (SignalGroupSettingsData data : plan.getSignalGroupSettingsDataByGroupId().values()) {
							data.setDropping(dropping);
							data.setOnset(onset);
						}
					}
				}
			}
		});
		
		controler.addControlerListener(new IterationStartsListener() {
			
			@Override
			public void notifyIterationStarts(IterationStartsEvent event) {
				event.getServices().getEvents().addHandler(new EventsLogger());

				TestLink2EnterEventHandler enterHandler = new TestLink2EnterEventHandler();
				if (0 == event.getIteration()) {
					enterHandler.link2EnterTime = 38.0;
				}

				if (1 == event.getIteration()) {
					enterHandler.link2EnterTime = 100.0;
					SignalGroupStateChangedEventHandler signalsHandler0 = new TestSignalGroupStateChangedHandler();
					event.getServices().getEvents().addHandler(signalsHandler0);
				}
			}
		});
		
		controler.run();
	}
	
	
	private static final class TestSignalGroupStateChangedHandler implements
			SignalGroupStateChangedEventHandler {

		@Override
		public void reset(int i) {}

		@Override
		public void handleEvent(SignalGroupStateChangedEvent e) {
			if (e.getNewState().equals(SignalGroupState.RED)){
				Assert.assertEquals(0.0, e.getTime());
			}
			else if (e.getNewState().equals(SignalGroupState.GREEN)) {
				Assert.assertEquals(100.0, e.getTime());
			}
		}
	}


	private static final class TestLink2EnterEventHandler implements LinkEnterEventHandler {
		double link2EnterTime = 0;
		@Override
		public void reset(int i) {}
		@Override
		public void handleEvent(LinkEnterEvent e){
			if (e.getLinkId().equals(Fixture.linkId2)) {
				Assert.assertEquals(link2EnterTime,  e.getTime(), MatsimTestUtils.EPSILON);
			}
		}
	}
	
}
