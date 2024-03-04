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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.signals.events.SignalGroupStateChangedEvent;
import org.matsim.contrib.signals.events.SignalGroupStateChangedEventHandler;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalGroupSettingsData;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemControllerData;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.testcases.utils.EventsLogger;


/**
 * Contains tests that use the signals one agent scenario as base and test functionality of the Controler.
 * @author dgrether
 *
 */
public class ControlerTest {

	@RegisterExtension
	private MatsimTestUtils testUtils = new MatsimTestUtils();

	/**
	 * Tests the setup with a traffic light that shows all the time green in the 0th iteration.
	 * After the mobsim is run the signal settings are changed thus in the 1st iteration
	 * the signal should be red in sec [0,99] and green in [100,2000]
	 */
	@Test
	void testModifySignalControlDataOnsetOffset() {
		//configure and load standard scenario
		Fixture fixture = new Fixture();
		Scenario scenario = fixture.createAndLoadTestScenarioOneSignal(false);
		scenario.getConfig().controller().setFirstIteration(0);
		scenario.getConfig().controller().setLastIteration(1);
		scenario.getConfig().controller().setOutputDirectory(testUtils.getOutputDirectory());
		scenario.getConfig().controller().setWriteEventsInterval(1);

		Controler controler = new Controler(scenario);
        controler.getConfig().controller().setCreateGraphs(false);
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

		controler.addControlerListener((IterationStartsListener)event -> {
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
				Assertions.assertEquals(0.0, e.getTime(), 1e-7);
			}
			else if (e.getNewState().equals(SignalGroupState.GREEN)) {
				Assertions.assertEquals(100.0, e.getTime(), 1e-7);
			}
		}
	}


	private static final class TestLink2EnterEventHandler implements LinkEnterEventHandler {
		final Id<Link> linkId2 = Id.create(2, Link.class);
		double link2EnterTime = 0;
		@Override
		public void reset(int i) {}
		@Override
		public void handleEvent(LinkEnterEvent e){
			if (e.getLinkId().equals(linkId2)) {
				Assertions.assertEquals(link2EnterTime,  e.getTime(), MatsimTestUtils.EPSILON);
			}
		}
	}

}
