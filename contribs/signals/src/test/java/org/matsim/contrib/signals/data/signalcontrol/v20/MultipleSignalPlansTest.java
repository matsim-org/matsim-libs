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
package org.matsim.contrib.signals.data.signalcontrol.v20;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Ignore;
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
import org.matsim.contrib.signals.controler.SignalsModule;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
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
import org.matsim.contrib.signals.model.DefaultPlanbasedSignalSystemController;
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
 * @author tthunig
 *
 */
public class MultipleSignalPlansTest {

	private static final Logger log = Logger.getLogger(MultipleSignalPlansTest.class);
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	@Test
	public void test2SequentialPlansCompleteDay(){
		ScenarioRunner runner = new ScenarioRunner(0, 3600*1, 3600*1, 3600*24);
		runner.setNoPersons(3600);
		SignalEventAnalyzer signalAnalyzer = runner.run();
		
		log.info("First signal event at time " + signalAnalyzer.getFirstSignalEventTime());
		log.info("Last signal off event at time " + signalAnalyzer.getLastSignalOffEventTime());
		log.info("Number of signal off events " + signalAnalyzer.getNumberOfOffEvents());
		log.info("First cycle time after 0am " + signalAnalyzer.getCycleTimeOfFirstCycleInHour(0));
		log.info("First cycle time after 1am " + signalAnalyzer.getCycleTimeOfFirstCycleInHour(1));
		// test time when signal plans are switched on and off
		Assert.assertEquals("First signal state event unexpected.", 0.0, signalAnalyzer.getFirstSignalEventTime(), MatsimTestUtils.EPSILON);
		Assert.assertNull("There was an unexpected event that switches off signals.", signalAnalyzer.getLastSignalOffEventTime());
		Assert.assertEquals("Number of signal off events is wrong.", 0, signalAnalyzer.getNumberOfOffEvents());		
		// test if signal plans are both running
		Assert.assertEquals("Cycle time of first signal plan wrong.", 120, signalAnalyzer.getCycleTimeOfFirstCycleInHour(0), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Cycle time of second signal plan wrong.", 60, signalAnalyzer.getCycleTimeOfFirstCycleInHour(1), MatsimTestUtils.EPSILON);
	}
	
	@Test
	public void test2SequentialPlansUncompleteDayEnd(){
		SignalEventAnalyzer signalAnalyzer = (new ScenarioRunner(0, 3600*1, 3600*1, 3600*2)).run();
		
		log.info("First signal event at time " + signalAnalyzer.getFirstSignalEventTime());
		log.info("Last signal off event at time " + signalAnalyzer.getLastSignalOffEventTime());
		log.info("Number of signal off events " + signalAnalyzer.getNumberOfOffEvents());
		log.info("First cycle time after 0am " + signalAnalyzer.getCycleTimeOfFirstCycleInHour(0));
		log.info("First cycle time after 1am " + signalAnalyzer.getCycleTimeOfFirstCycleInHour(1));
		log.info("First cycle time after 2am " + signalAnalyzer.getCycleTimeOfFirstCycleInHour(2));
		log.info("Number of signal events after 2am " + signalAnalyzer.getNumberOfSignalEventsInHour(2));
		// test time when signal plans are switched on and off
		Assert.assertEquals("First signal state event unexpected.", 0.0, signalAnalyzer.getFirstSignalEventTime(), MatsimTestUtils.EPSILON);
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
	@Ignore
	// TODO debug why first signal plan starts at time 0
	public void test2SequentialPlansUncompleteDayStart(){
		SignalEventAnalyzer signalAnalyzer = (new ScenarioRunner(3600*1, 3600*2, 3600*2, 3600*24)).run();
		
		log.info("First signal event at time " + signalAnalyzer.getFirstSignalEventTime());
		log.info("Last signal off event at time " + signalAnalyzer.getLastSignalOffEventTime());
		log.info("Number of signal off events " + signalAnalyzer.getNumberOfOffEvents());
		log.info("First cycle time after 0am " + signalAnalyzer.getCycleTimeOfFirstCycleInHour(0));
		log.info("First cycle time after 1am " + signalAnalyzer.getCycleTimeOfFirstCycleInHour(1));
		log.info("First cycle time after 2am " + signalAnalyzer.getCycleTimeOfFirstCycleInHour(2));
		log.info("Number of signal events between 0am and 1am " + signalAnalyzer.getNumberOfSignalEventsInHour(0));		
		// test time when signal plans are switched on and off
		Assert.assertEquals("First signal state event unexpected.", 3600*1, signalAnalyzer.getFirstSignalEventTime(), MatsimTestUtils.EPSILON);
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
		ScenarioRunner runner = new ScenarioRunner(0, 3600*1, 3600*1+1, 3600*24);
		runner.setNoPersons(3600);
		SignalEventAnalyzer signalAnalyzer = runner.run();
		
		log.info("First signal event at time " + signalAnalyzer.getFirstSignalEventTime());
		log.info("Last signal off event at time " + signalAnalyzer.getLastSignalOffEventTime());
		log.info("Number of signal off events " + signalAnalyzer.getNumberOfOffEvents());
		log.info("First cycle time after 0am " + signalAnalyzer.getCycleTimeOfFirstCycleInHour(0));
		log.info("First cycle time after 1am " + signalAnalyzer.getCycleTimeOfFirstCycleInHour(1));
		// test time when signal plans are switched on and off
		Assert.assertEquals("First signal state event unexpected.", 0.0, signalAnalyzer.getFirstSignalEventTime(), MatsimTestUtils.EPSILON);
		Assert.assertNull("There was an unexpected event that switches off signals.", signalAnalyzer.getLastSignalOffEventTime());
		Assert.assertEquals("Number of signal off events is wrong.", 0, signalAnalyzer.getNumberOfOffEvents());		
		// test if signal plans are both running
		Assert.assertEquals("Cycle time of first signal plan wrong.", 120, signalAnalyzer.getCycleTimeOfFirstCycleInHour(0), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Cycle time of second signal plan wrong.", 60, signalAnalyzer.getCycleTimeOfFirstCycleInHour(1), MatsimTestUtils.EPSILON);
	}
	
	@Test
	public void test2SequentialPlans1HourGap(){
		SignalEventAnalyzer signalAnalyzer = (new ScenarioRunner(3600*0, 3600*1, 3600*2, 3600*24)).run();
		
		log.info("First signal event at time " + signalAnalyzer.getFirstSignalEventTime());
		log.info("Last signal off event at time " + signalAnalyzer.getLastSignalOffEventTime());
		log.info("Number of signal off events " + signalAnalyzer.getNumberOfOffEvents());
		log.info("First cycle time after 0am " + signalAnalyzer.getCycleTimeOfFirstCycleInHour(0));
		log.info("First cycle time after 1am " + signalAnalyzer.getCycleTimeOfFirstCycleInHour(1));
		log.info("First cycle time after 2am " + signalAnalyzer.getCycleTimeOfFirstCycleInHour(2));
		log.info("Number of signal events between 1am and 2am " + signalAnalyzer.getNumberOfSignalEventsInHour(1));
		// test time when signal plans are switched on and off
		Assert.assertEquals("First signal state event unexpected.", 0.0, signalAnalyzer.getFirstSignalEventTime(), MatsimTestUtils.EPSILON);
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
		ScenarioRunner runner = new ScenarioRunner(3600*0, 3600*1, 3600*2, 3600*3);
		runner.setNoPersons(3*3600);
		SignalEventAnalyzer signalAnalyzer = runner.run();
		
		log.info("First signal event at time " + signalAnalyzer.getFirstSignalEventTime());
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
	@Ignore
	// TODO debug: first signal plan is ignored (or only would be switched on at 10pm); instead second plan is switched on to early
	public void test2SequentialPlansOverMidnight(){
		SignalEventAnalyzer signalAnalyzer = (new ScenarioRunner(3600*22, 3600*1, 3600*1, 3600*22)).run();
		
		log.info("First signal event at time " + signalAnalyzer.getFirstSignalEventTime());
		log.info("Last signal off event at time " + signalAnalyzer.getLastSignalOffEventTime());
		log.info("Number of signal off events " + signalAnalyzer.getNumberOfOffEvents());
		log.info("First cycle time after 0am " + signalAnalyzer.getCycleTimeOfFirstCycleInHour(0));
		log.info("First cycle time after 1am " + signalAnalyzer.getCycleTimeOfFirstCycleInHour(1));
		// test time when signal plans are switched on and off
		Assert.assertEquals("First signal state event unexpected.", 0.0, signalAnalyzer.getFirstSignalEventTime(), MatsimTestUtils.EPSILON);
		Assert.assertNull("There was an unexpected event that switches off signals.", signalAnalyzer.getLastSignalOffEventTime());
		Assert.assertEquals("Number of signal off events is wrong.", 0, signalAnalyzer.getNumberOfOffEvents());
		// test if signal plans are both running
		Assert.assertEquals("Cycle time of first signal plan wrong.", 120, signalAnalyzer.getCycleTimeOfFirstCycleInHour(0), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Cycle time of second signal plan wrong.", 60, signalAnalyzer.getCycleTimeOfFirstCycleInHour(1), MatsimTestUtils.EPSILON);
	}
	
	private class ScenarioRunner{
		
		private double plan1StartTime;
		private double plan1EndTime;
		private double plan2StartTime;
		private double plan2EndTime;
		private int noPersons = 2*3600;
		
		private Scenario scenario;
		
		/* package */ ScenarioRunner(double plan1StartTime, double plan1EndTime, double plan2StartTime, double plan2EndTime) {
			this.plan1StartTime = plan1StartTime;
			this.plan1EndTime = plan1EndTime;
			this.plan2StartTime = plan2StartTime;
			this.plan2EndTime = plan2EndTime;
		}
		
		/* package */ void setNoPersons(int noPersons) {
			this.noPersons = noPersons;
		}
		
		/* package */ SignalEventAnalyzer run() {
			Config config = defineConfig();
			
			scenario = ScenarioUtils.loadScenario(config);	
			// add missing scenario elements
			scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());
			
			createScenarioElements(scenario);
			
			Controler controler = new Controler(scenario);
			// add missing modules
			SignalsModule signalsModule = new SignalsModule();
			controler.addOverridingModule(signalsModule);
			
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
			
			for (int i = 0; i < noPersons; i++) {
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
			
			// create a first plan for the signal system (with cycle time 120) valid from 0am to 1am
			SignalPlanData signalPlan1 = SignalUtils.createSignalPlan(conFac, 120, 0, Id.create("SignalPlan1", SignalPlan.class));
			signalPlan1.setStartTime(plan1StartTime);
			signalPlan1.setEndTime(plan1EndTime);
			signalSystemControl.addSignalPlanData(signalPlan1);
			signalPlan1.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(conFac, signalGroupId1, 0, 60));
			signalPlan1.setOffset(0);
			
			// create a second plan for the signal system (with cycle time 60) valid from 1am to 24pm
			SignalPlanData signalPlan2 = SignalUtils.createSignalPlan(conFac, 60, 0, Id.create("SignalPlan2", SignalPlan.class));
			signalPlan2.setStartTime(plan2StartTime);
			signalPlan2.setEndTime(plan2EndTime);
			signalSystemControl.addSignalPlanData(signalPlan2);
			signalPlan2.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(conFac, signalGroupId1, 0, 30));
			signalPlan2.setOffset(0);
		}

		private Config defineConfig() {
			Config config = ConfigUtils.createConfig();
			config.controler().setOutputDirectory(testUtils.getOutputDirectory());
			
			// set number of iterations
			config.controler().setLastIteration(0);
		
			// able or enable signals and lanes
			SignalSystemsConfigGroup signalConfigGroup = ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
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

	class SignalEventAnalyzer implements SignalGroupStateChangedEventHandler{

		Double lastSignalOffEventTime = null;
		int noOffEvents;
		Double firstSignalEventTime = null;
		double[] cycleTimesOfFirstCyclePerHour = new double[24];
		int[] noSignalEventsPerHour = new int[24];
		
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
