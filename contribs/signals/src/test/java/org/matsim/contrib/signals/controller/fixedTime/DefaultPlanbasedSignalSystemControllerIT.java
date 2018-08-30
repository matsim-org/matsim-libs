/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package org.matsim.contrib.signals.controller.fixedTime;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.builder.SignalsModule;
import org.matsim.contrib.signals.controller.fixedTime.DefaultPlanbasedSignalSystemController;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlDataFactoryImpl;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlDataFactory;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsDataFactory;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsDataFactoryImpl;
import org.matsim.contrib.signals.events.SignalGroupStateChangedEvent;
import org.matsim.contrib.signals.events.SignalGroupStateChangedEventHandler;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.contrib.signals.utils.SignalUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * Tests fixed-time control, especially for multiple fixed-time plans over a day.
 * 
 * @author tthunig
 *
 */
public class DefaultPlanbasedSignalSystemControllerIT {

	private static final Logger log = Logger.getLogger(DefaultPlanbasedSignalSystemControllerIT.class);
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	@Test
	public void test2SequentialPlansCompleteDay(){
		ScenarioRunner runner = new ScenarioRunner(0.0, 3600*1.0, 3600*1.0, 3600*24.0);
		runner.setNoSimHours(1);
		SignalEventAnalyzer signalAnalyzer = runner.run();
		
		log.info("First signal event at time " + signalAnalyzer.getFirstSignalEventTime());
		log.info("Last start plan event at time " + signalAnalyzer.getLastPlanStartEventTime());
		log.info("Number of start plan events " + signalAnalyzer.getNumberOfPlanStartEvents());
		log.info("Last signal off event at time " + signalAnalyzer.getLastSignalOffEventTime());
		log.info("Number of signal off events " + signalAnalyzer.getNumberOfOffEvents());
		log.info("First cycle time after 0am " + signalAnalyzer.getCycleTimeOfFirstCycleInHour(0));
		log.info("First cycle time after 1am " + signalAnalyzer.getCycleTimeOfFirstCycleInHour(1));
		// test time when signal plans are switched on and off
		Assert.assertEquals("First signal state event unexpected.", 0.0, signalAnalyzer.getFirstSignalEventTime(), MatsimTestUtils.EPSILON);
//		Assert.assertEquals("Number of plan start events is wrong.", 2, signalAnalyzer.getNumberOfPlanStartEvents());		
//		Assert.assertEquals("Time when last plan starts is wrong.", 3600*1, signalAnalyzer.getLastPlanStartEventTime(), MatsimTestUtils.EPSILON); 
		Assert.assertNull("There was an unexpected event that switches off signals.", signalAnalyzer.getLastSignalOffEventTime());
		Assert.assertEquals("Number of signal off events is wrong.", 0, signalAnalyzer.getNumberOfOffEvents());		
		// test if signal plans are both running
		Assert.assertEquals("Cycle time of first signal plan wrong.", 120, signalAnalyzer.getCycleTimeOfFirstCycleInHour(0), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Cycle time of second signal plan wrong.", 60, signalAnalyzer.getCycleTimeOfFirstCycleInHour(1), MatsimTestUtils.EPSILON);
	}
	
	@Test
	public void test2SequentialPlansUncompleteDayEnd(){
		SignalEventAnalyzer signalAnalyzer = (new ScenarioRunner(0.0, 3600*1.0, 3600*1.0, 3600*2.0)).run();
		
		log.info("First signal event at time " + signalAnalyzer.getFirstSignalEventTime());
		log.info("Last start plan event at time " + signalAnalyzer.getLastPlanStartEventTime());
		log.info("Number of start plan events " + signalAnalyzer.getNumberOfPlanStartEvents());
		log.info("Last signal off event at time " + signalAnalyzer.getLastSignalOffEventTime());
		log.info("Number of signal off events " + signalAnalyzer.getNumberOfOffEvents());
		log.info("First cycle time after 0am " + signalAnalyzer.getCycleTimeOfFirstCycleInHour(0));
		log.info("First cycle time after 1am " + signalAnalyzer.getCycleTimeOfFirstCycleInHour(1));
		log.info("First cycle time after 2am " + signalAnalyzer.getCycleTimeOfFirstCycleInHour(2));
		log.info("Number of signal events after 2am " + signalAnalyzer.getNumberOfSignalEventsInHour(2));
		// test time when signal plans are switched on and off
		Assert.assertEquals("First signal state event unexpected.", 0.0, signalAnalyzer.getFirstSignalEventTime(), MatsimTestUtils.EPSILON);
//		Assert.assertEquals("Number of plan start events is wrong.", 2, signalAnalyzer.getNumberOfPlanStartEvents());		
//		Assert.assertEquals("Time when last plan starts is wrong.", 3600*1, signalAnalyzer.getLastPlanStartEventTime(), MatsimTestUtils.EPSILON); 
		Assert.assertEquals("Time when signals are finally switched off is wrong.", 3600*2, signalAnalyzer.getLastSignalOffEventTime(), 5 + MatsimTestUtils.EPSILON); 
		/* "5 + " because there is SignalSystemImpl.SWITCH_OFF_SEQUENCE_LENGTH of 5 seconds that is added to each signal plans end time as buffer */
		Assert.assertEquals("Number of signal off events is wrong.", 1, signalAnalyzer.getNumberOfOffEvents());
		Assert.assertEquals("Signals where unexpectedly switched on after 2am.", 0, signalAnalyzer.getCycleTimeOfFirstCycleInHour(2), MatsimTestUtils.EPSILON);
		Assert.assertTrue("Signals where unexpectedly switched on after 2am.", 3 > signalAnalyzer.getNumberOfSignalEventsInHour(2));
		/* "3 >" because last signal switches of the second plan are allowed at 2am and switch off is only after 5 seconds (in 2:00:05 am) */
		// test if signal plans are both running
		Assert.assertEquals("Cycle time of first signal plan wrong.", 120, signalAnalyzer.getCycleTimeOfFirstCycleInHour(0), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Cycle time of second signal plan wrong.", 60, signalAnalyzer.getCycleTimeOfFirstCycleInHour(1), MatsimTestUtils.EPSILON);
	}
	
	@Test
	public void test2SequentialPlansUncompleteDayStart(){
		SignalEventAnalyzer signalAnalyzer = (new ScenarioRunner(3600*1.0, 3600*2.0, 3600*2.0, 3600*24.0)).run();
		
		log.info("First signal event at time " + signalAnalyzer.getFirstSignalEventTime());
		log.info("Last start plan event at time " + signalAnalyzer.getLastPlanStartEventTime());
		log.info("Number of start plan events " + signalAnalyzer.getNumberOfPlanStartEvents());
		log.info("Last signal off event at time " + signalAnalyzer.getLastSignalOffEventTime());
		log.info("Number of signal off events " + signalAnalyzer.getNumberOfOffEvents());
		log.info("First cycle time after 0am " + signalAnalyzer.getCycleTimeOfFirstCycleInHour(0));
		log.info("First cycle time after 1am " + signalAnalyzer.getCycleTimeOfFirstCycleInHour(1));
		log.info("First cycle time after 2am " + signalAnalyzer.getCycleTimeOfFirstCycleInHour(2));
		log.info("Number of signal events between 0am and 1am " + signalAnalyzer.getNumberOfSignalEventsInHour(0));		
		// test time when signal plans are switched on and off
		Assert.assertEquals("First signal state event unexpected.", 3600*1, signalAnalyzer.getFirstSignalEventTime(), MatsimTestUtils.EPSILON);
//		Assert.assertEquals("Number of plan start events is wrong.", 2, signalAnalyzer.getNumberOfPlanStartEvents());		
//		Assert.assertEquals("Time when last plan starts is wrong.", 3600*2, signalAnalyzer.getLastPlanStartEventTime(), MatsimTestUtils.EPSILON); 
		Assert.assertNull("There was an unexpected event that switches off signals.", signalAnalyzer.getLastSignalOffEventTime());
		Assert.assertEquals("Number of signal off events is wrong.", 0, signalAnalyzer.getNumberOfOffEvents());		
		// test if first hour is simulated correctly without signals
		Assert.assertEquals("Signals where unexpectedly switched on between 0am and 1am.", 0, signalAnalyzer.getCycleTimeOfFirstCycleInHour(0), MatsimTestUtils.EPSILON);
		Assert.assertTrue("Signals where unexpectedly switched on between 0am and 1am.", 1 > signalAnalyzer.getNumberOfSignalEventsInHour(0));
		/* "1 > " because the first signal event should be at 1am, i.e. outside (after) this count interval */
		// test if signal plans are both running
		Assert.assertEquals("Cycle time of first signal plan wrong.", 120, signalAnalyzer.getCycleTimeOfFirstCycleInHour(1), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Cycle time of second signal plan wrong.", 60, signalAnalyzer.getCycleTimeOfFirstCycleInHour(2), MatsimTestUtils.EPSILON);
	}
	
	@Test
	public void test2SequentialPlans1SecGap(){
		ScenarioRunner runner = new ScenarioRunner(0.0, 3600*1.0, 3600*1.0+1, 3600*24.0);
		runner.setNoSimHours(1);
		SignalEventAnalyzer signalAnalyzer = runner.run();
		
		log.info("First signal event at time " + signalAnalyzer.getFirstSignalEventTime());
		log.info("Last start plan event at time " + signalAnalyzer.getLastPlanStartEventTime());
		log.info("Number of start plan events " + signalAnalyzer.getNumberOfPlanStartEvents());
		log.info("Last signal off event at time " + signalAnalyzer.getLastSignalOffEventTime());
		log.info("Number of signal off events " + signalAnalyzer.getNumberOfOffEvents());
		log.info("First cycle time after 0am " + signalAnalyzer.getCycleTimeOfFirstCycleInHour(0));
		log.info("First cycle time after 1am " + signalAnalyzer.getCycleTimeOfFirstCycleInHour(1));
		// test time when signal plans are switched on and off
		Assert.assertEquals("First signal state event unexpected.", 0.0, signalAnalyzer.getFirstSignalEventTime(), MatsimTestUtils.EPSILON);
//		Assert.assertEquals("Number of plan start events is wrong.", 2, signalAnalyzer.getNumberOfPlanStartEvents());		
//		Assert.assertEquals("Time when last plan starts is wrong.", 3600*1+1, signalAnalyzer.getLastPlanStartEventTime(), MatsimTestUtils.EPSILON); 
		Assert.assertNull("There was an unexpected event that switches off signals.", signalAnalyzer.getLastSignalOffEventTime());
		Assert.assertEquals("Number of signal off events is wrong.", 0, signalAnalyzer.getNumberOfOffEvents());		
		// test if signal plans are both running
		Assert.assertEquals("Cycle time of first signal plan wrong.", 120, signalAnalyzer.getCycleTimeOfFirstCycleInHour(0), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Cycle time of second signal plan wrong.", 60, signalAnalyzer.getCycleTimeOfFirstCycleInHour(1), MatsimTestUtils.EPSILON);
	}
	
	@Test
	public void test2SequentialPlans1HourGap(){
		SignalEventAnalyzer signalAnalyzer = (new ScenarioRunner(3600*0.0, 3600*1.0, 3600*2.0, 3600*24.0)).run();
		
		log.info("First signal event at time " + signalAnalyzer.getFirstSignalEventTime());
		log.info("Last start plan event at time " + signalAnalyzer.getLastPlanStartEventTime());
		log.info("Number of start plan events " + signalAnalyzer.getNumberOfPlanStartEvents());
		log.info("Last signal off event at time " + signalAnalyzer.getLastSignalOffEventTime());
		log.info("Number of signal off events " + signalAnalyzer.getNumberOfOffEvents());
		log.info("First cycle time after 0am " + signalAnalyzer.getCycleTimeOfFirstCycleInHour(0));
		log.info("First cycle time after 1am " + signalAnalyzer.getCycleTimeOfFirstCycleInHour(1));
		log.info("First cycle time after 2am " + signalAnalyzer.getCycleTimeOfFirstCycleInHour(2));
		log.info("Number of signal events between 1am and 2am " + signalAnalyzer.getNumberOfSignalEventsInHour(1));
		// test time when signal plans are switched on and off
		Assert.assertEquals("First signal state event unexpected.", 0.0, signalAnalyzer.getFirstSignalEventTime(), MatsimTestUtils.EPSILON);
//		Assert.assertEquals("Number of plan start events is wrong.", 2, signalAnalyzer.getNumberOfPlanStartEvents());		
//		Assert.assertEquals("Time when last plan starts is wrong.", 3600*2, signalAnalyzer.getLastPlanStartEventTime(), MatsimTestUtils.EPSILON); 
		Assert.assertEquals("Time when signals are finally switched off is wrong.", 3600*1, signalAnalyzer.getLastSignalOffEventTime(), 5 + MatsimTestUtils.EPSILON); 
		/* "5 + " because there is SignalSystemImpl.SWITCH_OFF_SEQUENCE_LENGTH of 5 seconds that is added to each signal plans end time as buffer */
		Assert.assertEquals("Number of signal off events is wrong.", 1, signalAnalyzer.getNumberOfOffEvents());		
		// test if break between signal plans is simulated correctly
		Assert.assertEquals("Signals where unexpectedly switched on between the signal plans.", 0, signalAnalyzer.getCycleTimeOfFirstCycleInHour(1), MatsimTestUtils.EPSILON);
		Assert.assertTrue("Signals where unexpectedly switched on between the signal plans.", 3 > signalAnalyzer.getNumberOfSignalEventsInHour(1));
		/* "3 >" because last signal switches of the first plan are allowed at 1am and switch off is only after 5 seconds (in 1:00:05 am) */
		// test if signal plans are both running
		Assert.assertEquals("Cycle time of first signal plan wrong.", 120, signalAnalyzer.getCycleTimeOfFirstCycleInHour(0), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Cycle time of second signal plan wrong.", 60, signalAnalyzer.getCycleTimeOfFirstCycleInHour(2), MatsimTestUtils.EPSILON);
	}
	
	@Test
	public void test2SequentialPlans1HourGap2TimesOff(){
		ScenarioRunner runner = new ScenarioRunner(3600*0.0, 3600*1.0, 3600*2.0, 3600*3.0);
		runner.setNoSimHours(3);
		SignalEventAnalyzer signalAnalyzer = runner.run();
		
		log.info("First signal event at time " + signalAnalyzer.getFirstSignalEventTime());
		log.info("Last start plan event at time " + signalAnalyzer.getLastPlanStartEventTime());
		log.info("Number of start plan events " + signalAnalyzer.getNumberOfPlanStartEvents());
		log.info("Last signal off event at time " + signalAnalyzer.getLastSignalOffEventTime());
		log.info("Number of signal off events " + signalAnalyzer.getNumberOfOffEvents());
		log.info("First cycle time after 0am " + signalAnalyzer.getCycleTimeOfFirstCycleInHour(0));
		log.info("First cycle time after 1am " + signalAnalyzer.getCycleTimeOfFirstCycleInHour(1));
		log.info("First cycle time after 2am " + signalAnalyzer.getCycleTimeOfFirstCycleInHour(2));
		log.info("First cycle time after 3am " + signalAnalyzer.getCycleTimeOfFirstCycleInHour(3));
		log.info("Number of signal events between 1am and 2am " + signalAnalyzer.getNumberOfSignalEventsInHour(1));
		log.info("Number of signal events after 3am " + signalAnalyzer.getNumberOfSignalEventsInHour(3));
		// test time when signal plans are switched on and off
		Assert.assertEquals("First signal state event unexpected.", 0.0, signalAnalyzer.getFirstSignalEventTime(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Time when signals are finally switched off is wrong.", 3600*3, signalAnalyzer.getLastSignalOffEventTime(), 5 + MatsimTestUtils.EPSILON);
		/* "5 + " because there is SignalSystemImpl.SWITCH_OFF_SEQUENCE_LENGTH of 5 seconds that is added to each signal plans end time as buffer */
		Assert.assertEquals("Number of signal off events is wrong.", 2, signalAnalyzer.getNumberOfOffEvents());		
		Assert.assertEquals("Signals where unexpectedly switched on between the signal plans.", 0, signalAnalyzer.getCycleTimeOfFirstCycleInHour(1), MatsimTestUtils.EPSILON);
		Assert.assertTrue("Signals where unexpectedly switched on between the signal plans.", 3 > signalAnalyzer.getNumberOfSignalEventsInHour(1));
		/* "3 >" because last signal switches of the first plan are allowed at 1am and switch off is only after 5 seconds (in 1:00:05 am) */
		Assert.assertEquals("Signals where unexpectedly switched on after the last signal plan.", 0, signalAnalyzer.getCycleTimeOfFirstCycleInHour(3), MatsimTestUtils.EPSILON);
		Assert.assertTrue("Signals where unexpectedly switched on after 3am.", 3 > signalAnalyzer.getNumberOfSignalEventsInHour(3));
		/* "3 >" because last signal switches of the second plan are allowed at 3am and switch off is only after 5 seconds (in 3:00:05 am) */
		// test if signal plans are both running
		Assert.assertEquals("Cycle time of first signal plan wrong.", 120, signalAnalyzer.getCycleTimeOfFirstCycleInHour(0), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Cycle time of second signal plan wrong.", 60, signalAnalyzer.getCycleTimeOfFirstCycleInHour(2), MatsimTestUtils.EPSILON);
	}
	
	@Test
	public void test2SequentialPlansOverMidnight(){
		SignalEventAnalyzer signalAnalyzer = (new ScenarioRunner(3600*22.0, 3600*1.0, 3600*1.0, 3600*22.0)).run();
		
		log.info("First signal event at time " + signalAnalyzer.getFirstSignalEventTime());
		log.info("Last start plan event at time " + signalAnalyzer.getLastPlanStartEventTime());
		log.info("Number of start plan events " + signalAnalyzer.getNumberOfPlanStartEvents());
		log.info("Last signal off event at time " + signalAnalyzer.getLastSignalOffEventTime());
		log.info("Number of signal off events " + signalAnalyzer.getNumberOfOffEvents());
		log.info("First cycle time after 0am " + signalAnalyzer.getCycleTimeOfFirstCycleInHour(0));
		log.info("First cycle time after 1am " + signalAnalyzer.getCycleTimeOfFirstCycleInHour(1));
		// test time when signal plans are switched on and off
		Assert.assertEquals("First signal state event unexpected.", 0.0, signalAnalyzer.getFirstSignalEventTime(), MatsimTestUtils.EPSILON);
//		Assert.assertEquals("Number of plan start events is wrong.", 2, signalAnalyzer.getNumberOfPlanStartEvents());		
//		Assert.assertEquals("Time when last plan starts is wrong.", 3600*1, signalAnalyzer.getLastPlanStartEventTime(), MatsimTestUtils.EPSILON); 
		Assert.assertNull("There was an unexpected event that switches off signals.", signalAnalyzer.getLastSignalOffEventTime());
		Assert.assertEquals("Number of signal off events is wrong.", 0, signalAnalyzer.getNumberOfOffEvents());
		// test if signal plans are both running
		Assert.assertEquals("Cycle time of first signal plan wrong.", 120, signalAnalyzer.getCycleTimeOfFirstCycleInHour(0), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Cycle time of second signal plan wrong.", 60, signalAnalyzer.getCycleTimeOfFirstCycleInHour(1), MatsimTestUtils.EPSILON);
	}
	
	@Test
	public void test1SignalPlanUncompleteDay(){
		SignalEventAnalyzer signalAnalyzer = (new ScenarioRunner(3600*1.0, 3600*2.0, null, null)).run();
		
		log.info("First signal event at time " + signalAnalyzer.getFirstSignalEventTime());
		log.info("Last start plan event at time " + signalAnalyzer.getLastPlanStartEventTime());
		log.info("Number of start plan events " + signalAnalyzer.getNumberOfPlanStartEvents());
		log.info("Last signal off event at time " + signalAnalyzer.getLastSignalOffEventTime());
		log.info("Number of signal off events " + signalAnalyzer.getNumberOfOffEvents());
		log.info("First cycle time after 0am " + signalAnalyzer.getCycleTimeOfFirstCycleInHour(0));
		log.info("First cycle time after 1am " + signalAnalyzer.getCycleTimeOfFirstCycleInHour(1));
		log.info("First cycle time after 2am " + signalAnalyzer.getCycleTimeOfFirstCycleInHour(2));
		log.info("Number of signal events between 0am and 1am " + signalAnalyzer.getNumberOfSignalEventsInHour(0));
		log.info("Number of signal events after 2am " + signalAnalyzer.getNumberOfSignalEventsInHour(2));
		// test time when signal plan is switched on and off
		Assert.assertEquals("First signal state event unexpected.", 3600.0, signalAnalyzer.getFirstSignalEventTime(), MatsimTestUtils.EPSILON);
//		Assert.assertEquals("Number of plan start events is wrong.", 1, signalAnalyzer.getNumberOfPlanStartEvents());		
//		Assert.assertEquals("Time when last plan starts is wrong.", 3600*1, signalAnalyzer.getLastPlanStartEventTime(), MatsimTestUtils.EPSILON); 
		Assert.assertEquals("Time when signals are finally switched off is wrong.", 3600*2, signalAnalyzer.getLastSignalOffEventTime(), 5 + MatsimTestUtils.EPSILON); 
		Assert.assertEquals("Number of signal off events is wrong.", 1, signalAnalyzer.getNumberOfOffEvents());
		Assert.assertEquals("Signals where unexpectedly switched on before 1am.", 0, signalAnalyzer.getCycleTimeOfFirstCycleInHour(0), MatsimTestUtils.EPSILON);
		Assert.assertTrue("Signals where unexpectedly switched on before 1am.", 1 > signalAnalyzer.getNumberOfSignalEventsInHour(0));
		/* "1 > " because the first signal event should be at 1am, i.e. outside (after) this count interval */
		Assert.assertEquals("Signal plan is not active between 1am and 2am.", 120, signalAnalyzer.getCycleTimeOfFirstCycleInHour(1), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Signals where unexpectedly switched on after 2am.", 0, signalAnalyzer.getCycleTimeOfFirstCycleInHour(2), MatsimTestUtils.EPSILON);
		Assert.assertTrue("Signals where unexpectedly switched on after 2am.", 3 > signalAnalyzer.getNumberOfSignalEventsInHour(2));
		/* "3 >" because last signal switches of the first plan are allowed at 2am and switch off is only after 5 seconds (in 2:00:05 am) */
	}
	
	/**
	 * test
	 * 1. one signal plan with default start and end times. should give all day signal plan
	 * 2. all day signal plan starts again/ is still active, when simulation time exceeds 12pm
	 */
	@Test
	public void test1AllDaySignalPlanOverMidnightLateStart(){
		ScenarioRunner runner = new ScenarioRunner(null, null, null, null); // i.e. new ScenarioRunner(0.0, 0.0, null, null);
		runner.setSimStart_h(23);
		SignalEventAnalyzer signalAnalyzer = runner.run();
		
		log.info("First signal event at time " + signalAnalyzer.getFirstSignalEventTime());
		log.info("Last start plan event at time " + signalAnalyzer.getLastPlanStartEventTime());
		log.info("Number of start plan events " + signalAnalyzer.getNumberOfPlanStartEvents());
		log.info("Last signal off event at time " + signalAnalyzer.getLastSignalOffEventTime());
		log.info("Number of signal off events " + signalAnalyzer.getNumberOfOffEvents());
		log.info("First cycle time after 0am " + signalAnalyzer.getCycleTimeOfFirstCycleInHour(23));
		log.info("First cycle time after 0am next day " + signalAnalyzer.getCycleTimeOfFirstCycleInHour(24));
		// test time when signal plan is switched on and off
		Assert.assertEquals("First signal state event unexpected.", 3600*23, signalAnalyzer.getFirstSignalEventTime(), MatsimTestUtils.EPSILON);
//		Assert.assertEquals("Number of plan start events is wrong.", 2, signalAnalyzer.getNumberOfPlanStartEvents());		
//		Assert.assertEquals("Time when last plan starts is wrong.", 3600*24, signalAnalyzer.getLastPlanStartEventTime(), MatsimTestUtils.EPSILON); 
		Assert.assertNull("There was an unexpected event that switches off signals.", signalAnalyzer.getLastSignalOffEventTime());
		Assert.assertEquals("Number of signal off events is wrong.", 0, signalAnalyzer.getNumberOfOffEvents());
		// test if signal plan is running for more than 24h
		Assert.assertEquals("Signal plan is not active between 11pmam and 12pm.", 120, signalAnalyzer.getCycleTimeOfFirstCycleInHour(23), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Signal plan is not active anymore after 0am next day.", 120, signalAnalyzer.getCycleTimeOfFirstCycleInHour(24), MatsimTestUtils.EPSILON);
	}
	
	/**
	 * test
	 * 1. one signal plan with default start and end times. should give all day signal plan
	 * 2. should directly start at time 0.0
	 */
	@Test
	public void test1AllDaySignalPlanMidnightStart(){
		SignalEventAnalyzer signalAnalyzer = (new ScenarioRunner(null, null, null, null)).run(); // i.e. new ScenarioRunner(0.0, 0.0, null, null);
		
		log.info("First signal event at time " + signalAnalyzer.getFirstSignalEventTime());
		log.info("Last start plan event at time " + signalAnalyzer.getLastPlanStartEventTime());
		log.info("Number of start plan events " + signalAnalyzer.getNumberOfPlanStartEvents());
		log.info("Last signal off event at time " + signalAnalyzer.getLastSignalOffEventTime());
		log.info("Number of signal off events " + signalAnalyzer.getNumberOfOffEvents());
		log.info("First cycle time after 0am " + signalAnalyzer.getCycleTimeOfFirstCycleInHour(0));
		// test time when signal plan is switched on and off
		Assert.assertEquals("First signal state event unexpected.", 0.0, signalAnalyzer.getFirstSignalEventTime(), MatsimTestUtils.EPSILON);
//		Assert.assertEquals("Number of plan start events is wrong.", 1, signalAnalyzer.getNumberOfPlanStartEvents());		
//		Assert.assertEquals("Time when last plan starts is wrong.", 0.0, signalAnalyzer.getLastPlanStartEventTime(), MatsimTestUtils.EPSILON); 
		Assert.assertNull("There was an unexpected event that switches off signals.", signalAnalyzer.getLastSignalOffEventTime());
		Assert.assertEquals("Number of signal off events is wrong.", 0, signalAnalyzer.getNumberOfOffEvents());
		// test if signal plan is running for more than 24h
		Assert.assertEquals("Signal plan is not active between 0am and 1am.", 120, signalAnalyzer.getCycleTimeOfFirstCycleInHour(0), MatsimTestUtils.EPSILON);
	}
	
	@Test
	public void test2SignalPlanFor25h(){
		ScenarioRunner runner = new ScenarioRunner(3600*0.0, 3600*12.0, 3600*12.0, 3600*24.0);
		runner.setNoSimHours(25);
		SignalEventAnalyzer signalAnalyzer = runner.run();
		
		log.info("First signal event at time " + signalAnalyzer.getFirstSignalEventTime());
		log.info("Last start plan event at time " + signalAnalyzer.getLastPlanStartEventTime());
		log.info("Number of start plan events " + signalAnalyzer.getNumberOfPlanStartEvents());
		log.info("Last signal off event at time " + signalAnalyzer.getLastSignalOffEventTime());
		log.info("Number of signal off events " + signalAnalyzer.getNumberOfOffEvents());
		log.info("First cycle time after 0am " + signalAnalyzer.getCycleTimeOfFirstCycleInHour(0));
		log.info("First cycle time after 12am " + signalAnalyzer.getCycleTimeOfFirstCycleInHour(12));
		log.info("First cycle time after 0am next day " + signalAnalyzer.getCycleTimeOfFirstCycleInHour(24));
		// test time when signal plans are switched on and off
		Assert.assertEquals("First signal state event unexpected.", 0.0, signalAnalyzer.getFirstSignalEventTime(), MatsimTestUtils.EPSILON);
//		Assert.assertEquals("Number of plan start events is wrong.", 3, signalAnalyzer.getNumberOfPlanStartEvents());		
//		Assert.assertEquals("Time when last plan starts is wrong.", 3600*24, signalAnalyzer.getLastPlanStartEventTime(), MatsimTestUtils.EPSILON); 
		Assert.assertNull("There was an unexpected event that switches off signals.", signalAnalyzer.getLastSignalOffEventTime());
		Assert.assertEquals("Number of signal off events is wrong.", 0, signalAnalyzer.getNumberOfOffEvents());
		// test if signal plans are correctly running for more than 24h
		Assert.assertEquals("First signal plan is not active between 0am and 1am.", 120, signalAnalyzer.getCycleTimeOfFirstCycleInHour(0), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Second signal plan is not active between 12am and 1pm.", 60, signalAnalyzer.getCycleTimeOfFirstCycleInHour(12), MatsimTestUtils.EPSILON);
		Assert.assertEquals("First signal plan is not active again after 0am next day.", 120, signalAnalyzer.getCycleTimeOfFirstCycleInHour(24), MatsimTestUtils.EPSILON);
	}
	
	@Test
	public void testSimStartAfterFirstDayPlan(){
		ScenarioRunner runner = new ScenarioRunner(3600*0.0, 3600*1.0, 3600*23.0, 3600*24.0);
		runner.setSimStart_h(23);
		runner.setNoSimHours(3);
		SignalEventAnalyzer signalAnalyzer = runner.run();
		
		log.info("First signal event at time " + signalAnalyzer.getFirstSignalEventTime());
		log.info("Last start plan event at time " + signalAnalyzer.getLastPlanStartEventTime());
		log.info("Number of start plan events " + signalAnalyzer.getNumberOfPlanStartEvents());
		log.info("Last signal off event at time " + signalAnalyzer.getLastSignalOffEventTime());
		log.info("Number of signal off events " + signalAnalyzer.getNumberOfOffEvents());
		log.info("First cycle time after 10pm " + signalAnalyzer.getCycleTimeOfFirstCycleInHour(22));
		log.info("First cycle time after 11pm " + signalAnalyzer.getCycleTimeOfFirstCycleInHour(23));
		log.info("First cycle time after 0am next day " + signalAnalyzer.getCycleTimeOfFirstCycleInHour(24));
		log.info("First cycle time after 1am next day " + signalAnalyzer.getCycleTimeOfFirstCycleInHour(25));
		log.info("Number of signal events before 11pm " + signalAnalyzer.getNumberOfSignalEventsInHour(22));
		log.info("Number of signal events after 1am next day " + signalAnalyzer.getNumberOfSignalEventsInHour(25));
		// test time when signal plans are switched on and off
		Assert.assertEquals("First signal state event unexpected.", 23*3600, signalAnalyzer.getFirstSignalEventTime(), MatsimTestUtils.EPSILON);
//		Assert.assertEquals("Number of plan start events is wrong.", 2, signalAnalyzer.getNumberOfPlanStartEvents());		
//		Assert.assertEquals("Time when last plan starts is wrong.", 3600*24, signalAnalyzer.getLastPlanStartEventTime(), MatsimTestUtils.EPSILON); 
		Assert.assertEquals("Time when signals are finally switched off is wrong.", 3600*25, signalAnalyzer.getLastSignalOffEventTime(), 5 + MatsimTestUtils.EPSILON); 
		Assert.assertEquals("Number of signal off events is wrong.", 1, signalAnalyzer.getNumberOfOffEvents());
		Assert.assertEquals("Signals where unexpectedly switched on before 11pm.", 0, signalAnalyzer.getCycleTimeOfFirstCycleInHour(22), MatsimTestUtils.EPSILON);
		Assert.assertTrue("Signals where unexpectedly switched on before 11pm.", 1 > signalAnalyzer.getNumberOfSignalEventsInHour(22));
		Assert.assertEquals("Signals where unexpectedly switched on after 1am next day.", 0, signalAnalyzer.getCycleTimeOfFirstCycleInHour(25), MatsimTestUtils.EPSILON);
		Assert.assertTrue("Signals where unexpectedly switched on after 1am next day.", 3 > signalAnalyzer.getNumberOfSignalEventsInHour(25));
		// test if signal plans are correctly running
		Assert.assertEquals("Second signal plan is not active between 11pm and 12pm.", 60, signalAnalyzer.getCycleTimeOfFirstCycleInHour(23), MatsimTestUtils.EPSILON);
		Assert.assertEquals("First signal plan is not active between 0am and 1pm next day.", 120, signalAnalyzer.getCycleTimeOfFirstCycleInHour(24), MatsimTestUtils.EPSILON);
	}
	
	/**
	 * tests:
	 * 1. overlapping signal plans (uncomplete day) result in an exception
	 */
	@Test
	public void test2PlansSameTimesUncompleteDay(){
		final String exceptionMessageOverlapping21 = "Signal plans SignalPlan2 and SignalPlan1 of signal system SignalSystem-3 overlap.";
		
		try{
			(new ScenarioRunner(0.0, 1.0, 0.0, 1.0)).run();
			Assert.fail("The simulation has not stopped with an exception although the signal plans overlap.");
		} catch (UnsupportedOperationException e) {
			log.info("Exception message: " + e.getMessage());
			Assert.assertEquals("Wrong exception message.", exceptionMessageOverlapping21, e.getMessage());
		}
	}
	
	/**
	 * tests:
	 * 1. overlapping signal plans (complete day) result in an exception
	 */
	@Test
	public void test2PlansSameTimesCompleteDay(){
		final String exceptionMessageHoleDay = "Signal system SignalSystem-3 has multiple plans but at least one of them covers the hole day. "
				+ "If multiple signal plans are used, they are not allowed to overlap.";
		
		try{
			(new ScenarioRunner(0.0, 0.0, 0.0, 0.0)).run();
//			(new ScenarioRunner(1.0, 1.0, 1.0, 1.0)).run(); // alternativ. produces same results
			Assert.fail("The simulation has not stopped with an exception although multiple signal plans exist and at least one of them covers the hole day (i.e. they overlap).");
		} catch (UnsupportedOperationException e) {
			log.info("Exception message: " + e.getMessage());
			Assert.assertEquals("Wrong exception message.", exceptionMessageHoleDay, e.getMessage());
		}
	}
	
	@Test
	public void test2OverlappingPlans(){
		final String exceptionMessageOverlapping12 = "Signal plans SignalPlan1 and SignalPlan2 of signal system SignalSystem-3 overlap.";
		
		try{
			(new ScenarioRunner(0.0, 2.0, 1.0, 3.0)).run();
			Assert.fail("The simulation has not stopped with an exception although the signal plans overlap.");
		} catch (UnsupportedOperationException e) {
			log.info("Exception message: " + e.getMessage());
			Assert.assertEquals("Wrong exception message.", exceptionMessageOverlapping12, e.getMessage());
		}
	}
	
	@Test
	public void testNegativeOffset() {
		//plan1 is valid all day
		ScenarioRunner sr = new ScenarioRunner(0.0, 0.0, null, null);
		int offset1 = -3;
		
		sr.setOffsetPlan1(offset1);		
		SignalEventAnalyzer signalAnalyzer = sr.run();

		// in this case, the first event should be a RED-switch at second 57
		log.info("Offset " + offset1 + " leads to the first signal event at second: " + signalAnalyzer.getFirstSignalEventTime());		
		Assert.assertEquals("The first signal event should be at the first second after simulation start corresponding to offset, "
				+ "cycle time and plan start time. Also if the offset is negative!", 60+offset1 , signalAnalyzer.getFirstSignalEventTime() , MatsimTestUtils.EPSILON);		
	}
	
	@Test
	public void testNegativeOffsetEqualCycleTime() {
		//plan1 is valid all day
		ScenarioRunner sr = new ScenarioRunner(0.0, 0.0, null, null);
		int offset1 = -120;
		
		sr.setOffsetPlan1(offset1);		
		SignalEventAnalyzer signalAnalyzer = sr.run();
				
		// in this case, the first event should be a GREEN-switch at second 0
		log.info("Offset " + offset1 + " leads to the first signal event at second: " + signalAnalyzer.getFirstSignalEventTime());		
		Assert.assertEquals("The first signal event should be at the first second after simulation start corresponding to offset, "
				+ "cycle time and plan start time. Also if the offset is negative!", 120+offset1 , signalAnalyzer.getFirstSignalEventTime() , MatsimTestUtils.EPSILON);		
	}
	
	@Test
	public void testTwoPlansWithNegativeOffsets(){
		ScenarioRunner sr = new ScenarioRunner(0.0*3600, 1.0*3600, 1.0*3600, 2.*3600 );
		
		int offset1 = -3;
		int offset2 = -5;
		
		sr.setOffsetPlan1(offset1);
		sr.setOffsetPlan2(offset2);
		
		//Simulation starts 1h late
		int simStart_s = 3600;
		sr.setSimStart_h(simStart_s/3600);
		
		SignalEventAnalyzer signalAnalyzer = sr.run();
		
		// in this case, the first event should be a RED-switch at second 3625
		log.info("Offsets " + offset1 + " and " + offset2 + " with a simulation start at second " + simStart_s + " lead to the first signal event at second: " + signalAnalyzer.getFirstSignalEventTime());
 		Assert.assertEquals("The first signal event should be at the first second after simulation start corresponding to offset, "
				+ "cycle time and plan start time. Also if the offset is negative!", simStart_s+30+offset2 , signalAnalyzer.getFirstSignalEventTime() , MatsimTestUtils.EPSILON);		
	}
	
	@Test
	public void testTwoPlansWithNegativeOffsetsEqualCycleTime(){
		ScenarioRunner sr = new ScenarioRunner(0.0*3600, 1.0*3600, 1.0*3600, 2.*3600 );
		
		int offset1 = -3;
		int offset2 = -60;
		
		sr.setOffsetPlan1(offset1);
		sr.setOffsetPlan2(offset2);
		
		//Simulation starts 1h late
		int simStart_s = 3600;
		sr.setSimStart_h(simStart_s/3600);
		
		SignalEventAnalyzer signalAnalyzer = sr.run();
		
		// in this case, the first event should be a GREEN-switch at second 3600
		log.info("Offsets " + offset1 + " and " + offset2 + " with a simulation start at second " + simStart_s + " lead to the first signal event at second: " + signalAnalyzer.getFirstSignalEventTime());
 		Assert.assertEquals("The first signal event should be at the first second after simulation start corresponding to offset, "
				+ "cycle time and plan start time. Also if the offset is negative!", simStart_s+60+offset2 , signalAnalyzer.getFirstSignalEventTime() , MatsimTestUtils.EPSILON);		
	}
	
	private class ScenarioRunner{
		
		private Double plan1StartTime;
		private Double plan1EndTime;
		private Double plan2StartTime;
		private Double plan2EndTime;
		private int noSimHours = 2;
		private int simStart_h= 0;
		private int offsetPlan1 = 0;
		private int offsetPlan2 = 0;
		
		private Scenario scenario;
		
		/**
		 * Create a scenario with two signal plans with given start end end times. 
		 * If start and end times of second signal plan are null, only one signal plan is created.
		 */
		/* package */ ScenarioRunner(Double plan1StartTime, Double plan1EndTime, Double plan2StartTime, Double plan2EndTime) {
			this.plan1StartTime = plan1StartTime;
			this.plan1EndTime = plan1EndTime;
			this.plan2StartTime = plan2StartTime;
			this.plan2EndTime = plan2EndTime;
		}
		
		/* package */ void setNoSimHours(int hours) {
			this.noSimHours = hours;
		}
		
		/* package */ void setSimStart_h(int simStart_h) {
			this.simStart_h = simStart_h;
		}
		
		/* package */ void setOffsetPlan1(int offset) {
			this.offsetPlan1 = offset;
		}
		
		/* package */ void setOffsetPlan2(int offset) {
			this.offsetPlan2 = offset;
		}
		
		/* package */ SignalEventAnalyzer run() {
			Config config = defineConfig();
			
			scenario = ScenarioUtils.loadScenario(config);	
			// add missing scenario elements
			scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());
			
			createScenarioElements(scenario);
			
			Controler controler = new Controler(scenario);
			// add missing modules
			controler.addOverridingModule(new SignalsModule());
			
			// add signal analysis tool
			final SignalEventAnalyzer signalAnalyzer = new SignalEventAnalyzer();
			controler.addOverridingModule(new AbstractModule() {			
				@Override
				public void install() {
					this.addEventHandlerBinding().toInstance(signalAnalyzer);
				}
			});
			
			controler.run();
			return signalAnalyzer;
		}
		
		private void createScenarioElements(Scenario scenario) {
			createNetwork();
			createPopulation();
			createSignals();
		}
		
		/**
		 * Creates a network like this:
		 * 
		 * 1 ----> 2 ----> 3 ----> 4 ----> 5
		 */
		private void createNetwork() {
			Network net = scenario.getNetwork();
			NetworkFactory fac = net.getFactory();
			
			net.addNode(fac.createNode(Id.createNodeId(1), new Coord(-2000, 0)));
			net.addNode(fac.createNode(Id.createNodeId(2), new Coord(-1000, 0)));
			net.addNode(fac.createNode(Id.createNodeId(3), new Coord(0, 0)));
			net.addNode(fac.createNode(Id.createNodeId(4), new Coord(1000, 0)));
			net.addNode(fac.createNode(Id.createNodeId(5), new Coord(2000, 0)));
			
			String[] links = {"1_2", "2_3", "3_4", "4_5"};
			
			for (String linkId : links){
				String fromNodeId = linkId.split("_")[0];
				String toNodeId = linkId.split("_")[1];
				Link link = fac.createLink(Id.createLinkId(linkId), 
						net.getNodes().get(Id.createNodeId(fromNodeId)), 
						net.getNodes().get(Id.createNodeId(toNodeId)));
				link.setCapacity(7200);
				link.setLength(1000);
				link.setFreespeed(10);
				net.addLink(link);
			}
		}

		/**
		 * Creates a dummy population going from left to right 
		 * with one agent starting every second.
		 */
		private void createPopulation() {
			Population pop = scenario.getPopulation();
			
			// one person every half an hour
			for (int i = simStart_h*3600; i <= simStart_h*3600 + noSimHours*3600; i+=1800) {
				// create a person
				Person person = pop.getFactory().createPerson(Id.createPersonId("1_2-4_5-" + i));
				pop.addPerson(person);

				// create a plan
				Plan plan = pop.getFactory().createPlan();
				person.addPlan(plan);

				Activity startAct = pop.getFactory().createActivityFromLinkId("dummy", Id.createLinkId("1_2"));
				// one agent starts every second
				startAct.setEndTime(i);
				plan.addActivity(startAct);
				plan.addLeg(pop.getFactory().createLeg(TransportMode.car));
				Activity drainAct = pop.getFactory().createActivityFromLinkId("dummy", Id.createLinkId("4_5"));
				plan.addActivity(drainAct);
			}
		}

		/**
		 * Creates a signal at node 3 with two signal plans
		 */
		private void createSignals() {
			SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
			SignalSystemsData signalSystems = signalsData.getSignalSystemsData();
			SignalSystemsDataFactory sysFac = new SignalSystemsDataFactoryImpl();
			SignalGroupsData signalGroups = signalsData.getSignalGroupsData();
			SignalControlData signalControl = signalsData.getSignalControlData();
			SignalControlDataFactory conFac = new SignalControlDataFactoryImpl();
			
			// create signal system at node 3
			Id<SignalSystem> signalSystemId = Id.create("SignalSystem-3", SignalSystem.class);
			SignalSystemData signalSystem = sysFac.createSignalSystemData(signalSystemId);
			signalSystems.addSignalSystemData(signalSystem);
			
			// create a signal for inLink 2_3
			SignalData signal = sysFac.createSignalData(Id.create("Signal-2_3", Signal.class));
			signalSystem.addSignalData(signal);
			signal.setLinkId(Id.createLinkId("2_3"));
			
			// create an one element group for the signal
			Id<SignalGroup> signalGroupId1 = Id.create("SignalGroup-2_3", SignalGroup.class);
			SignalGroupData signalGroup1 = signalGroups.getFactory().createSignalGroupData(signalSystemId, signalGroupId1);
			signalGroup1.addSignalId(Id.create("Signal-2_3", Signal.class));
			signalGroups.addSignalGroupData(signalGroup1);
			
			// create the signal control
			SignalSystemControllerData signalSystemControl = conFac.createSignalSystemControllerData(signalSystemId);
			signalSystemControl.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);
			signalControl.addSignalSystemControllerData(signalSystemControl);
			
			// create a first plan for the signal system (with cycle time 120)
			SignalPlanData signalPlan1 = SignalUtils.createSignalPlan(conFac, 120, offsetPlan1, Id.create("SignalPlan1", SignalPlan.class));
			if (plan1StartTime != null) signalPlan1.setStartTime(plan1StartTime);
			if (plan1EndTime != null) signalPlan1.setEndTime(plan1EndTime);
			signalSystemControl.addSignalPlanData(signalPlan1);
			signalPlan1.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(conFac, signalGroupId1, 0, 60));
			
			if (plan2StartTime != null && plan2EndTime != null) {
				// create a second plan for the signal system (with cycle time 60) if start and end times are not null
				SignalPlanData signalPlan2 = SignalUtils.createSignalPlan(conFac, 60, offsetPlan2, Id.create("SignalPlan2", SignalPlan.class));
				signalPlan2.setStartTime(plan2StartTime);
				signalPlan2.setEndTime(plan2EndTime);
				signalSystemControl.addSignalPlanData(signalPlan2);
				signalPlan2.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(conFac, signalGroupId1, 0, 30));
			}
		}

		private Config defineConfig() {
			Config config = ConfigUtils.createConfig();
			config.controler().setOutputDirectory(testUtils.getOutputDirectory());
			
			// set number of iterations
			config.controler().setLastIteration(0);
			
			config.qsim().setStartTime(simStart_h*3600);
	        config.qsim().setUsingFastCapacityUpdate(false);
		
			// able or enable signals and lanes
			SignalSystemsConfigGroup signalConfigGroup = ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUP_NAME, SignalSystemsConfigGroup.class);
			signalConfigGroup.setUseSignalSystems( true );
			
			// define strategies:
			{
				StrategySettings strat = new StrategySettings();
				strat.setStrategyName(DefaultSelector.KeepLastSelected.toString());
				strat.setWeight(1);
				strat.setDisableAfter(config.controler().getLastIteration());
				config.strategy().addStrategySettings(strat);
			}		
			config.qsim().setStuckTime( 3600 );
			config.qsim().setRemoveStuckVehicles(false);
		
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		
			config.vspExperimental().setWritingOutputEvents(false);
			config.planCalcScore().setWriteExperiencedPlans(false);
			config.controler().setCreateGraphs(false);
			config.controler().setDumpDataAtEnd(false);
			config.controler().setWriteEventsInterval(config.controler().getLastIteration());
			config.controler().setWritePlansInterval(config.controler().getLastIteration());
		
			// define activity types
			{
				ActivityParams dummyAct = new ActivityParams("dummy");
				dummyAct.setTypicalDuration(12 * 3600);
				config.planCalcScore().addActivityParams(dummyAct);
			}
			return config;
		}
		
	}

	private class SignalEventAnalyzer implements SignalGroupStateChangedEventHandler{

		Double lastSignalOffEventTime = null;
		int noOffEvents;
		Double lastPlanStartEventTime = null;
		int noPlanStartEvents;
		Double firstSignalEventTime = null;
		double[] cycleTimesOfFirstCyclePerHour = new double[30];
		int[] noSignalEventsPerHour = new int[30];
		
		Double lastGreenSwitchOfThisCycle = null;
		
		@Override
		public void reset(int iteration) {
		}

		public double getCycleTimeOfFirstCycleInHour(int hourOfDay) {
			return cycleTimesOfFirstCyclePerHour[hourOfDay];
		}

		public Double getFirstSignalEventTime() {
			return firstSignalEventTime;
		}
		
		public Double getLastSignalOffEventTime() {
			return lastSignalOffEventTime;
		}
		
		public int getNumberOfSignalEventsInHour(int hourOfDay){
			return noSignalEventsPerHour[hourOfDay];
		}
		
		public int getNumberOfOffEvents() {
			return noOffEvents;
		}
		
		public Double getLastPlanStartEventTime() {
			return lastPlanStartEventTime;
		}
		
		public int getNumberOfPlanStartEvents() {
			return noPlanStartEvents;
		}

		@Override
		public void handleEvent(SignalGroupStateChangedEvent event) {
			int hourOfDay = (int) (event.getTime() / 3600);
			// count number of signal events per hour
			noSignalEventsPerHour[hourOfDay]++;
			
			if (firstSignalEventTime == null){
				firstSignalEventTime = event.getTime();
			}
			
			if (event.getNewState().equals(SignalGroupState.OFF)){
				lastSignalOffEventTime = event.getTime();
				noOffEvents++;
				return;
			} else if (event.getNewState().equals(SignalGroupState.START_PLAN)){
				lastPlanStartEventTime = event.getTime();
				noPlanStartEvents++;
				return;
			}
			
			// fill cycle time array
			if (cycleTimesOfFirstCyclePerHour[hourOfDay] == 0.0){
				// first cycle time of this hour not yet determined
				if (event.getNewState().equals(SignalGroupState.GREEN)){
					if (lastGreenSwitchOfThisCycle == null){
						// this is the first green switch of this cycle
						lastGreenSwitchOfThisCycle = event.getTime();
					} else {
						// this is the second green switch of this cycle
						cycleTimesOfFirstCyclePerHour[hourOfDay] = event.getTime() - lastGreenSwitchOfThisCycle;
						lastGreenSwitchOfThisCycle = null;
					}
				}
			}
		}
		
	}
	
}
