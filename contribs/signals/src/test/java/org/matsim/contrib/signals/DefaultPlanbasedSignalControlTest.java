/* *********************************************************************** *
 * project: org.matsim.*
 * DefaultPlanbasedSignalControlTest
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package org.matsim.contrib.signals;

import junit.framework.Assert;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.events.SignalGroupStateChangedEvent;
import org.matsim.contrib.signals.events.SignalGroupStateChangedEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.testcases.MatsimTestUtils;


/**
 * Test for the default fixed-time control implementation of MATSim.
 * @author dgrether
 *
 */
public class DefaultPlanbasedSignalControlTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();
	
	/**
	 * Tests fixed-time control, especially for multiple fixed-time plans over a day.
	 */
	@Test
	public final void planSwitchingTest() {
		Config config = ConfigUtils.createConfig();
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(0);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		QSimConfigGroup qsimConfig = config.qsim();
		qsimConfig.setStartTime(0.0);
		qsimConfig.setEndTime(400.0);
		qsimConfig.setSimStarttimeInterpretation(QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime);
	
		String inputDir = utils.getClassInputDirectory();
		//scenario
		ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).setUseSignalSystems(true);
		String networkInputFile = inputDir + "network.xml";
		config.network().setInputFile(networkInputFile);
		//signals
		String signalsFile = inputDir + "signal_systems.xml";
		ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).setSignalSystemFile(signalsFile);
		String signalGroupsFile = inputDir + "signal_groups.xml";
		ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).setSignalGroupsFile(signalGroupsFile);
		String signalControlFile = inputDir + "signal_control.xml";
		ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).setSignalControlFile(signalControlFile);
	
		Controler controler = new Controler(config);
        controler.getConfig().controler().setCreateGraphs(false);
		controler.getConfig().controler().setDumpDataAtEnd(false);
		PlanSwitchingTestListener testListener = new PlanSwitchingTestListener();
		controler.addControlerListener(testListener);
		controler.run();
		Assert.assertTrue(testListener.catchedAllEvents);
	}


	private static class PlanSwitchingTestListener implements StartupListener, SignalGroupStateChangedEventHandler {

		boolean catchedAllEvents = true;
		
		@Override
		public void notifyStartup(StartupEvent e) {
			e.getServices().getEvents().addHandler(this);
		}

		@Override
		public void handleEvent(SignalGroupStateChangedEvent e) {
			if (e.getTime() == 0.0){
				if (e.getSignalGroupId().equals(Id.create("4", SignalGroup.class))){
					Assert.assertEquals(SignalGroupState.GREEN, e.getNewState());
				}
				else if (e.getSignalGroupId().equals(Id.create("5", SignalGroup.class))){
					Assert.assertEquals(SignalGroupState.RED, e.getNewState());
				}
			}
			else if (e.getTime() == 45.0){
				if (e.getSignalGroupId().equals(Id.create("4", SignalGroup.class))){
					Assert.assertEquals(SignalGroupState.RED, e.getNewState());
				}
				else if (e.getSignalGroupId().equals(Id.create("5", SignalGroup.class))){
					Assert.assertEquals(SignalGroupState.GREEN, e.getNewState());
				}
			}
			else if (e.getTime() == 90.0){
				if (e.getSignalGroupId().equals(Id.create("4", SignalGroup.class))){
					Assert.assertEquals(SignalGroupState.GREEN, e.getNewState());
				}
				else if (e.getSignalGroupId().equals(Id.create("5", SignalGroup.class))){
					Assert.assertEquals(SignalGroupState.RED, e.getNewState());
				}
			}
			else if (e.getTime() == 135.0){
				if (e.getSignalGroupId().equals(Id.create("4", SignalGroup.class))){
					Assert.assertEquals(SignalGroupState.RED, e.getNewState());
				}
				else if (e.getSignalGroupId().equals(Id.create("5", SignalGroup.class))){
					Assert.assertEquals(SignalGroupState.GREEN, e.getNewState());
				}
			}
			else if (e.getTime() == 165.0){
				if (e.getSignalGroupId().equals(Id.create("4", SignalGroup.class))){
					Assert.assertEquals(SignalGroupState.GREEN, e.getNewState());
				}
				else if (e.getSignalGroupId().equals(Id.create("5", SignalGroup.class))){
					Assert.assertEquals(SignalGroupState.RED, e.getNewState());
				}
			}
			else if (e.getTime() == 195.0){
				if (e.getSignalGroupId().equals(Id.create("4", SignalGroup.class))){
					Assert.assertEquals(SignalGroupState.RED, e.getNewState());
				}
				else if (e.getSignalGroupId().equals(Id.create("5", SignalGroup.class))){
					Assert.assertEquals(SignalGroupState.GREEN, e.getNewState());
				}
			}
			else if (e.getTime() == 210.0){
					Assert.assertEquals(SignalGroupState.YELLOW, e.getNewState());
			}
			else if (e.getTime() == 215.0){
					Assert.assertEquals(SignalGroupState.OFF, e.getNewState());
			}
			else if (e.getTime() == 240.0){
				if (e.getSignalGroupId().equals(Id.create("4", SignalGroup.class))){
					Assert.assertEquals(SignalGroupState.GREEN, e.getNewState());
				}
				else if (e.getSignalGroupId().equals(Id.create("5", SignalGroup.class))){
					Assert.assertEquals(SignalGroupState.RED, e.getNewState());
				}
			}
			else if (e.getTime() == 255.0){
				if (e.getSignalGroupId().equals(Id.create("4", SignalGroup.class))){
					Assert.assertEquals(SignalGroupState.RED, e.getNewState());
				}
				else if (e.getSignalGroupId().equals(Id.create("5", SignalGroup.class))){
					Assert.assertEquals(SignalGroupState.GREEN, e.getNewState());
				}
			}
			else if (e.getTime() == 300.0){
				if (e.getSignalGroupId().equals(Id.create("4", SignalGroup.class))){
					Assert.assertEquals(SignalGroupState.GREEN, e.getNewState());
				}
				else if (e.getSignalGroupId().equals(Id.create("5", SignalGroup.class))){
					Assert.assertEquals(SignalGroupState.RED, e.getNewState());
				}
			}
			else if (e.getTime() == 315.0){
				Assert.assertEquals(SignalGroupState.YELLOW, e.getNewState());
			}
			else if (e.getTime() == 320.0){
				Assert.assertEquals(SignalGroupState.OFF, e.getNewState());
			}
			else {
				catchedAllEvents = false;
			}
		}

		@Override
		public void reset(int iteration) {}
		
	}
	
}
