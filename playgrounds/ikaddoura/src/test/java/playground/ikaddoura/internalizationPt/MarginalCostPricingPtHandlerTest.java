/* *********************************************************************** *
 * project: org.matsim.*
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

/**
 * 
 */
package playground.ikaddoura.internalizationPt;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;

import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControlerConfigGroup.MobsimType;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author lkroeger, ikaddoura
 *
 */
public class MarginalCostPricingPtHandlerTest  {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();
	
	private EventsManager events;
	
	private Id testAgent1 = new IdImpl("testAgent1");
	private Id testAgent2 = new IdImpl("testAgent2");
	private Id testAgent3 = new IdImpl("testAgent3");
	private Id testAgent4 = new IdImpl("testAgent4");
	private Id testAgent5 = new IdImpl("testAgent5");

	//one agent from start (stop 1) to finish (stop 6)
	//another agent gets on (stop 2) and off (stop 3)
	@Test
    public final void testInVehicleDelay01() {
   	 	
		Config config = utils.loadConfig(null);
		
		String networkFile = utils.getInputDirectory() + "multimodalnetwork.xml";
		String populationFile = utils.getInputDirectory() + "population.xml";
		String scheduleFile = utils.getInputDirectory() + "transitschedule.xml";
		String vehiclesFile = utils.getInputDirectory() + "transitVehicles.xml";
		
		config.network().setInputFile(networkFile);
		config.plans().setInputFile(populationFile);
		config.controler().setLastIteration(0);
		config.controler().setMobsim(MobsimType.qsim.toString());
		config.setParam("vspExperimental", "isGeneratingBoardingDeniedEvent", "true");
		
		config.scenario().setUseTransit(true);
		config.scenario().setUseVehicles(true);
		config.transit().setTransitScheduleFile(scheduleFile);
		config.transit().setVehiclesFile(vehiclesFile);
		
		ActivityParams hParams = new ActivityParams("h");
		hParams.setTypicalDuration(3600.);
		config.planCalcScore().addActivityParams(hParams);
		ActivityParams wParams = new ActivityParams("w");
		wParams.setTypicalDuration(3600.);
		config.planCalcScore().addActivityParams(wParams);
		
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		this.events = controler.getEvents();
		
		final List<TransferDelayInVehicleEvent> transferDelayInVehicleEvent = new ArrayList<TransferDelayInVehicleEvent>();
		
		events.addHandler( new TransferDelayInVehicleEventHandler() {
			
			@Override
			public void reset(int iteration) {				
			}

			@Override
			public void handleEvent(TransferDelayInVehicleEvent event) {
				transferDelayInVehicleEvent.add(event);
			}
			
		});
		
		controler.addControlerListener(new InternalizationPtControlerListener(scenario));
		controler.run();
		
		//*******************************************************************************************
		
		Assert.assertEquals("transferDelayInVehicleEvent", 8, transferDelayInVehicleEvent.size());
		
		TransferDelayInVehicleEvent ive1 = transferDelayInVehicleEvent.get(0);
		TransferDelayInVehicleEvent ive2 = transferDelayInVehicleEvent.get(1);
		TransferDelayInVehicleEvent ive3 = transferDelayInVehicleEvent.get(2);
		TransferDelayInVehicleEvent ive4 = transferDelayInVehicleEvent.get(3);
		TransferDelayInVehicleEvent ive5 = transferDelayInVehicleEvent.get(4);
		TransferDelayInVehicleEvent ive6 = transferDelayInVehicleEvent.get(5);
		TransferDelayInVehicleEvent ive7 = transferDelayInVehicleEvent.get(6);
		TransferDelayInVehicleEvent ive8 = transferDelayInVehicleEvent.get(7);
		
		Assert.assertEquals("affected Agents", 0, ive1.getAffectedAgents());
		Assert.assertEquals("causing Agent", testAgent1.toString(), ive1.getCausingAgent().toString());
		Assert.assertEquals("delay", 1.0, ive1.getDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("affected Agents", 0, ive2.getAffectedAgents());
		Assert.assertEquals("causing Agent", testAgent1.toString(), ive2.getCausingAgent().toString());
		Assert.assertEquals("delay", 2.0, ive2.getDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("affected Agents", 1, ive3.getAffectedAgents());
		Assert.assertEquals("causing Agent", testAgent2.toString(), ive3.getCausingAgent().toString());
		Assert.assertEquals("delay", 1.0, ive3.getDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("affected Agents", 1, ive4.getAffectedAgents());
		Assert.assertEquals("causing Agent", testAgent2.toString(), ive4.getCausingAgent().toString());
		Assert.assertEquals("delay", 2.0, ive4.getDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("affected Agents", 1, ive5.getAffectedAgents());
		Assert.assertEquals("causing Agent", testAgent2.toString(), ive5.getCausingAgent().toString());
		Assert.assertEquals("delay", 0.75, ive5.getDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("affected Agents", 1, ive6.getAffectedAgents());
		Assert.assertEquals("causing Agent", testAgent2.toString(), ive6.getCausingAgent().toString());
		Assert.assertEquals("delay", 2.0, ive6.getDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("affected Agents", 0, ive7.getAffectedAgents());
		Assert.assertEquals("causing Agent", testAgent1.toString(), ive7.getCausingAgent().toString());
		Assert.assertEquals("delay", 0.75, ive7.getDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("affected Agents", 0, ive8.getAffectedAgents());
		Assert.assertEquals("causing Agent", testAgent1.toString(), ive8.getCausingAgent().toString());
		Assert.assertEquals("delay", 2.0, ive8.getDelay(), MatsimTestUtils.EPSILON);
		
		//*******************************************************************************************
		
     }

	//one agent from start (stop 1) to finish (stop 6)
	//another agent gets on (stop 2)
	//he gets off and a third agent gets on (stop 3)
	//the third agent gets off (stop 4)
	//the door-opening and -closing time has to be divided
	@Test
	public final void testInVehicleDelay02(){
	
	Config config = utils.loadConfig(null);
	
	String networkFile = utils.getInputDirectory() + "multimodalnetwork.xml";
	String populationFile = utils.getInputDirectory() + "population.xml";
	String scheduleFile = utils.getInputDirectory() + "transitschedule.xml";
	String vehiclesFile = utils.getInputDirectory() + "transitVehicles.xml";
	
	config.network().setInputFile(networkFile);
	config.plans().setInputFile(populationFile);
	config.controler().setLastIteration(0);
	config.controler().setMobsim(MobsimType.qsim.toString());
	config.setParam("vspExperimental", "isGeneratingBoardingDeniedEvent", "true");
	
	config.scenario().setUseTransit(true);
	config.scenario().setUseVehicles(true);
	config.transit().setTransitScheduleFile(scheduleFile);
	config.transit().setVehiclesFile(vehiclesFile);
	
	ActivityParams hParams = new ActivityParams("h");
	hParams.setTypicalDuration(3600.);
	config.planCalcScore().addActivityParams(hParams);
	ActivityParams wParams = new ActivityParams("w");
	wParams.setTypicalDuration(3600.);
	config.planCalcScore().addActivityParams(wParams);
	
	ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.loadScenario(config);
	Controler controler = new Controler(scenario);
	this.events = controler.getEvents();
	
	final List<TransferDelayInVehicleEvent> transferDelayInVehicleEvent = new ArrayList<TransferDelayInVehicleEvent>();
	
	events.addHandler( new TransferDelayInVehicleEventHandler() {
		
		@Override
		public void reset(int iteration) {				
		}

		@Override
		public void handleEvent(TransferDelayInVehicleEvent event) {
			transferDelayInVehicleEvent.add(event);
		}
		
	});
	
		controler.addControlerListener(new InternalizationPtControlerListener(scenario));
		controler.run();
	
		//*******************************************************************************************
	
		Assert.assertEquals("number of TransferDelayInVehicleEvents", 12, transferDelayInVehicleEvent.size());
	
		TransferDelayInVehicleEvent ive1 = transferDelayInVehicleEvent.get(0);
		TransferDelayInVehicleEvent ive2 = transferDelayInVehicleEvent.get(1);
		TransferDelayInVehicleEvent ive3 = transferDelayInVehicleEvent.get(2);
		TransferDelayInVehicleEvent ive4 = transferDelayInVehicleEvent.get(3);
		TransferDelayInVehicleEvent ive5 = transferDelayInVehicleEvent.get(4);
		TransferDelayInVehicleEvent ive6 = transferDelayInVehicleEvent.get(5);
		TransferDelayInVehicleEvent ive7 = transferDelayInVehicleEvent.get(6);
		TransferDelayInVehicleEvent ive8 = transferDelayInVehicleEvent.get(7);
		TransferDelayInVehicleEvent ive9 = transferDelayInVehicleEvent.get(8);
		TransferDelayInVehicleEvent ive10 = transferDelayInVehicleEvent.get(9);
		TransferDelayInVehicleEvent ive11 = transferDelayInVehicleEvent.get(10);
		TransferDelayInVehicleEvent ive12 = transferDelayInVehicleEvent.get(11);
	
		Assert.assertEquals("affected Agents", 0, ive1.getAffectedAgents());
		Assert.assertEquals("causing Agent", testAgent1.toString(), ive1.getCausingAgent().toString());
		Assert.assertEquals("delay", 1.0, ive1.getDelay(), MatsimTestUtils.EPSILON);
	
		Assert.assertEquals("affected Agents", 0, ive2.getAffectedAgents());
		Assert.assertEquals("causing Agent", testAgent1.toString(), ive2.getCausingAgent().toString());
		Assert.assertEquals("delay", 2.0, ive2.getDelay(), MatsimTestUtils.EPSILON);
	
		Assert.assertEquals("affected Agents", 1, ive3.getAffectedAgents());
		Assert.assertEquals("causing Agent", testAgent2.toString(), ive3.getCausingAgent().toString());
		Assert.assertEquals("delay", 1.0, ive3.getDelay(), MatsimTestUtils.EPSILON);
	
		Assert.assertEquals("affected Agents", 1, ive4.getAffectedAgents());
		Assert.assertEquals("causing Agent", testAgent2.toString(), ive4.getCausingAgent().toString());
		Assert.assertEquals("delay", 2.0, ive4.getDelay(), MatsimTestUtils.EPSILON);
	
		Assert.assertEquals("affected Agents", 1, ive5.getAffectedAgents());
		Assert.assertEquals("causing Agent", testAgent2.toString(), ive5.getCausingAgent().toString());
		Assert.assertEquals("delay", 0.75, ive5.getDelay(), MatsimTestUtils.EPSILON);
	
		Assert.assertEquals("affected Agents", 1, ive6.getAffectedAgents());
		Assert.assertEquals("causing Agent", testAgent3.toString(), ive6.getCausingAgent().toString());
		Assert.assertEquals("delay", 1.0, ive6.getDelay(), MatsimTestUtils.EPSILON);
	
		Assert.assertEquals("affected Agents", 1, ive7.getAffectedAgents());
		Assert.assertEquals("causing Agent", testAgent3.toString(), ive7.getCausingAgent().toString());
		Assert.assertEquals("delay", 1.0, ive7.getDelay(), MatsimTestUtils.EPSILON);
	
		Assert.assertEquals("affected Agents", 1, ive8.getAffectedAgents());
		Assert.assertEquals("causing Agent", testAgent2.toString(), ive8.getCausingAgent().toString());
		Assert.assertEquals("delay", 1.0, ive8.getDelay(), MatsimTestUtils.EPSILON);
	
		Assert.assertEquals("affected Agents", 1, ive9.getAffectedAgents());
		Assert.assertEquals("causing Agent", testAgent3.toString(), ive9.getCausingAgent().toString());
		Assert.assertEquals("delay", 0.75, ive9.getDelay(), MatsimTestUtils.EPSILON);

		Assert.assertEquals("affected Agents", 1, ive10.getAffectedAgents());
		Assert.assertEquals("causing Agent", testAgent3.toString(), ive10.getCausingAgent().toString());
		Assert.assertEquals("delay", 2.0, ive10.getDelay(), MatsimTestUtils.EPSILON);
	
		Assert.assertEquals("affected Agents", 0, ive11.getAffectedAgents());
		Assert.assertEquals("causing Agent", testAgent1.toString(), ive11.getCausingAgent().toString());
		Assert.assertEquals("delay", 0.75, ive11.getDelay(), MatsimTestUtils.EPSILON);
	
		Assert.assertEquals("affected Agents", 0, ive12.getAffectedAgents());
		Assert.assertEquals("causing Agent", testAgent1.toString(), ive12.getCausingAgent().toString());
		Assert.assertEquals("delay", 2.0, ive12.getDelay(), MatsimTestUtils.EPSILON);
	
		//*******************************************************************************************

	}

	//agents 1 and 2 from stop 1 to stop 3
	//agent 3 from stop 2 to stop 6
	//agent 4 from stop 4 to stop 5
	//agent 5 from stop 4 to stop 6
	@Test
	public final void testInVehicleDelay03(){
		
		Config config = utils.loadConfig(null);
		
		String networkFile = utils.getInputDirectory() + "multimodalnetwork.xml";
		String populationFile = utils.getInputDirectory() + "population.xml";
		String scheduleFile = utils.getInputDirectory() + "transitschedule.xml";
		String vehiclesFile = utils.getInputDirectory() + "transitVehicles.xml";
		
		config.network().setInputFile(networkFile);
		config.plans().setInputFile(populationFile);
		config.controler().setLastIteration(0);
		config.controler().setMobsim(MobsimType.qsim.toString());
		config.setParam("vspExperimental", "isGeneratingBoardingDeniedEvent", "true");
		
		config.scenario().setUseTransit(true);
		config.scenario().setUseVehicles(true);
		config.transit().setTransitScheduleFile(scheduleFile);
		config.transit().setVehiclesFile(vehiclesFile);
		
		ActivityParams hParams = new ActivityParams("h");
		hParams.setTypicalDuration(3600.);
		config.planCalcScore().addActivityParams(hParams);
		ActivityParams wParams = new ActivityParams("w");
		wParams.setTypicalDuration(3600.);
		config.planCalcScore().addActivityParams(wParams);
		
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		this.events = controler.getEvents();
		
		final List<TransferDelayInVehicleEvent> transferDelayInVehicleEvent = new ArrayList<TransferDelayInVehicleEvent>();
		
		events.addHandler( new TransferDelayInVehicleEventHandler() {
			
			@Override
			public void reset(int iteration) {				
			}

			@Override
			public void handleEvent(TransferDelayInVehicleEvent event) {
				transferDelayInVehicleEvent.add(event);
			}
			
		});
		
			controler.addControlerListener(new InternalizationPtControlerListener(scenario));
			controler.run();
		
			//*******************************************************************************************
		
			Assert.assertEquals("number of TransferDelayInVehicleEvents", 20, transferDelayInVehicleEvent.size());
			
			TransferDelayInVehicleEvent ive1 = transferDelayInVehicleEvent.get(0);
			TransferDelayInVehicleEvent ive2 = transferDelayInVehicleEvent.get(1);
			TransferDelayInVehicleEvent ive3 = transferDelayInVehicleEvent.get(2);
			TransferDelayInVehicleEvent ive4 = transferDelayInVehicleEvent.get(3);
			TransferDelayInVehicleEvent ive5 = transferDelayInVehicleEvent.get(4);
			TransferDelayInVehicleEvent ive6 = transferDelayInVehicleEvent.get(5);
			TransferDelayInVehicleEvent ive7 = transferDelayInVehicleEvent.get(6);
			TransferDelayInVehicleEvent ive8 = transferDelayInVehicleEvent.get(7);
			TransferDelayInVehicleEvent ive9 = transferDelayInVehicleEvent.get(8);
			TransferDelayInVehicleEvent ive10 = transferDelayInVehicleEvent.get(9);
			TransferDelayInVehicleEvent ive11 = transferDelayInVehicleEvent.get(10);
			TransferDelayInVehicleEvent ive12 = transferDelayInVehicleEvent.get(11);
			TransferDelayInVehicleEvent ive13 = transferDelayInVehicleEvent.get(12);
			TransferDelayInVehicleEvent ive14 = transferDelayInVehicleEvent.get(13);
			TransferDelayInVehicleEvent ive15 = transferDelayInVehicleEvent.get(14);
			TransferDelayInVehicleEvent ive16 = transferDelayInVehicleEvent.get(15);
			TransferDelayInVehicleEvent ive17 = transferDelayInVehicleEvent.get(16);
			TransferDelayInVehicleEvent ive18 = transferDelayInVehicleEvent.get(17);
			TransferDelayInVehicleEvent ive19 = transferDelayInVehicleEvent.get(18);
			TransferDelayInVehicleEvent ive20 = transferDelayInVehicleEvent.get(19);
			
			Assert.assertEquals("affected Agents", 0, ive1.getAffectedAgents());
			Assert.assertEquals("causing Agent", testAgent1.toString(), ive1.getCausingAgent().toString());
			Assert.assertEquals("delay", 1.0, ive1.getDelay(), MatsimTestUtils.EPSILON);
			
			Assert.assertEquals("affected Agents", 1, ive2.getAffectedAgents());
			Assert.assertEquals("causing Agent", testAgent2.toString(), ive2.getCausingAgent().toString());
			Assert.assertEquals("delay", 1.0, ive2.getDelay(), MatsimTestUtils.EPSILON);
			
			Assert.assertEquals("affected Agents", 0, ive3.getAffectedAgents());
			Assert.assertEquals("causing Agent", testAgent1.toString(), ive3.getCausingAgent().toString());
			Assert.assertEquals("delay", 1.0, ive3.getDelay(), MatsimTestUtils.EPSILON);
			
			Assert.assertEquals("affected Agents", 0, ive4.getAffectedAgents());
			Assert.assertEquals("causing Agent", testAgent2.toString(), ive4.getCausingAgent().toString());
			Assert.assertEquals("delay", 1.0, ive4.getDelay(), MatsimTestUtils.EPSILON);
			
			Assert.assertEquals("affected Agents", 2, ive5.getAffectedAgents());
			Assert.assertEquals("causing Agent", testAgent3.toString(), ive5.getCausingAgent().toString());
			Assert.assertEquals("delay", 1.0, ive5.getDelay(), MatsimTestUtils.EPSILON);
			
			Assert.assertEquals("affected Agents", 2, ive6.getAffectedAgents());
			Assert.assertEquals("causing Agent", testAgent3.toString(), ive6.getCausingAgent().toString());
			Assert.assertEquals("delay", 2.0, ive6.getDelay(), MatsimTestUtils.EPSILON);
			
			Assert.assertEquals("affected Agents", 2, ive7.getAffectedAgents());
			Assert.assertEquals("causing Agent", testAgent1.toString(), ive7.getCausingAgent().toString());
			Assert.assertEquals("delay", 0.75, ive7.getDelay(), MatsimTestUtils.EPSILON);
			
			Assert.assertEquals("affected Agents", 1, ive8.getAffectedAgents());
			Assert.assertEquals("causing Agent", testAgent2.toString(), ive8.getCausingAgent().toString());
			Assert.assertEquals("delay", 0.75, ive8.getDelay(), MatsimTestUtils.EPSILON);
			
			Assert.assertEquals("affected Agents", 1, ive9.getAffectedAgents());
			Assert.assertEquals("causing Agent", testAgent1.toString(), ive9.getCausingAgent().toString());
			Assert.assertEquals("delay", 1.0, ive9.getDelay(), MatsimTestUtils.EPSILON);

			Assert.assertEquals("affected Agents", 1, ive10.getAffectedAgents());
			Assert.assertEquals("causing Agent", testAgent2.toString(), ive10.getCausingAgent().toString());
			Assert.assertEquals("delay", 1.0, ive10.getDelay(), MatsimTestUtils.EPSILON);
			
			Assert.assertEquals("affected Agents", 1, ive11.getAffectedAgents());
			Assert.assertEquals("causing Agent", testAgent4.toString(), ive11.getCausingAgent().toString());
			Assert.assertEquals("delay", 1.0, ive11.getDelay(), MatsimTestUtils.EPSILON);
			
			Assert.assertEquals("affected Agents", 2, ive12.getAffectedAgents());
			Assert.assertEquals("causing Agent", testAgent5.toString(), ive12.getCausingAgent().toString());
			Assert.assertEquals("delay", 1.0, ive12.getDelay(), MatsimTestUtils.EPSILON);
			
			Assert.assertEquals("affected Agents", 1, ive13.getAffectedAgents());
			Assert.assertEquals("causing Agent", testAgent4.toString(), ive13.getCausingAgent().toString());
			Assert.assertEquals("delay", 1.0, ive13.getDelay(), MatsimTestUtils.EPSILON);
			
			Assert.assertEquals("affected Agents", 1, ive14.getAffectedAgents());
			Assert.assertEquals("causing Agent", testAgent5.toString(), ive14.getCausingAgent().toString());
			Assert.assertEquals("delay", 1.0, ive14.getDelay(), MatsimTestUtils.EPSILON);
			
			Assert.assertEquals("affected Agents", 2, ive15.getAffectedAgents());
			Assert.assertEquals("causing Agent", testAgent4.toString(), ive15.getCausingAgent().toString());
			Assert.assertEquals("delay", 0.75, ive15.getDelay(), MatsimTestUtils.EPSILON);
			
			Assert.assertEquals("affected Agents", 2, ive16.getAffectedAgents());
			Assert.assertEquals("causing Agent", testAgent4.toString(), ive16.getCausingAgent().toString());
			Assert.assertEquals("delay", 2.0, ive16.getDelay(), MatsimTestUtils.EPSILON);
			
			Assert.assertEquals("affected Agents", 1, ive17.getAffectedAgents());
			Assert.assertEquals("causing Agent", testAgent3.toString(), ive17.getCausingAgent().toString());
			Assert.assertEquals("delay", 0.75, ive17.getDelay(), MatsimTestUtils.EPSILON);
			
			Assert.assertEquals("affected Agents", 0, ive18.getAffectedAgents());
			Assert.assertEquals("causing Agent", testAgent5.toString(), ive18.getCausingAgent().toString());
			Assert.assertEquals("delay", 0.75, ive18.getDelay(), MatsimTestUtils.EPSILON);
			
			Assert.assertEquals("affected Agents", 0, ive19.getAffectedAgents());
			Assert.assertEquals("causing Agent", testAgent3.toString(), ive19.getCausingAgent().toString());
			Assert.assertEquals("delay", 1.0, ive19.getDelay(), MatsimTestUtils.EPSILON);
			
			Assert.assertEquals("affected Agents", 0, ive20.getAffectedAgents());
			Assert.assertEquals("causing Agent", testAgent5.toString(), ive20.getCausingAgent().toString());
			Assert.assertEquals("delay", 1.0, ive20.getDelay(), MatsimTestUtils.EPSILON);
			
			//*******************************************************************************************
		
	}
	
	//agent 1 (2 --> 6)
	//agent 2 is waiting (3 --> 4)
	//agent 2 gets on and off the bus while agent 1 is going through
	@Test
	public final void testWaitingDelay01(){
		
		Config config = utils.loadConfig(null);
		
		String networkFile = utils.getInputDirectory() + "multimodalnetwork.xml";
		String populationFile = utils.getInputDirectory() + "population.xml";
		String scheduleFile = utils.getInputDirectory() + "transitschedule.xml";
		String vehiclesFile = utils.getInputDirectory() + "transitVehicles.xml";
		
		config.network().setInputFile(networkFile);
		config.plans().setInputFile(populationFile);
		config.controler().setLastIteration(0);
		config.controler().setMobsim(MobsimType.qsim.toString());
		config.setParam("vspExperimental", "isGeneratingBoardingDeniedEvent", "true");
		
		config.scenario().setUseTransit(true);
		config.scenario().setUseVehicles(true);
		config.transit().setTransitScheduleFile(scheduleFile);
		config.transit().setVehiclesFile(vehiclesFile);
		
		ActivityParams hParams = new ActivityParams("h");
		hParams.setTypicalDuration(3600.);
		config.planCalcScore().addActivityParams(hParams);
		ActivityParams wParams = new ActivityParams("w");
		wParams.setTypicalDuration(3600.);
		config.planCalcScore().addActivityParams(wParams);
		
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		this.events = controler.getEvents();
		
		final List<TransferDelayWaitingEvent> waitingDelayEvents = new ArrayList<TransferDelayWaitingEvent>();
		
		events.addHandler( new TransferDelayWaitingEventHandler() {
			
			@Override
			public void reset(int iteration) {				
			}

			@Override
			public void handleEvent(TransferDelayWaitingEvent event) {
				waitingDelayEvents.add(event);
				
				System.out.println(event.toString());
			}
			
		});
		
			controler.addControlerListener(new InternalizationPtControlerListener(scenario));
			controler.run();
		
				//*******************************************************************************************
		
				Assert.assertEquals("number of WaitingDelayEvents", 8, waitingDelayEvents.size());
						
				TransferDelayWaitingEvent wde1 = waitingDelayEvents.get(0);
				TransferDelayWaitingEvent wde2 = waitingDelayEvents.get(1);
				TransferDelayWaitingEvent wde3 = waitingDelayEvents.get(2);
				TransferDelayWaitingEvent wde4 = waitingDelayEvents.get(3);
				TransferDelayWaitingEvent wde5 = waitingDelayEvents.get(4);
				TransferDelayWaitingEvent wde6 = waitingDelayEvents.get(5);
				TransferDelayWaitingEvent wde7 = waitingDelayEvents.get(6);
				TransferDelayWaitingEvent wde8 = waitingDelayEvents.get(7);
				
				Assert.assertEquals("affected Agents", 1.0, wde1.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
				Assert.assertEquals("causing Agent", testAgent1.toString(), wde1.getCausingAgent().toString());
				Assert.assertEquals("delay", 1.0, wde1.getDelay(), MatsimTestUtils.EPSILON);
				
				Assert.assertEquals("affected Agents", 0.0, wde2.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
				Assert.assertEquals("causing Agent", testAgent2.toString(), wde2.getCausingAgent().toString());
				Assert.assertEquals("delay", 1.0, wde2.getDelay(), MatsimTestUtils.EPSILON);
				
				Assert.assertEquals("affected Agents", 0.0, wde3.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
				Assert.assertEquals("causing Agent", testAgent2.toString(), wde3.getCausingAgent().toString());
				Assert.assertEquals("delay", 0.75, wde3.getDelay(), MatsimTestUtils.EPSILON);
				
				Assert.assertEquals("affected Agents", 0.0, wde4.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
				Assert.assertEquals("causing Agent", testAgent1.toString(), wde4.getCausingAgent().toString());
				Assert.assertEquals("delay", 0.75, wde4.getDelay(), MatsimTestUtils.EPSILON);
				
				Assert.assertEquals("affected Agents", 1.0, wde5.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
				Assert.assertEquals("causing Agent", testAgent1.toString(), wde5.getCausingAgent().toString());
				Assert.assertEquals("delay", 2.0, wde5.getDelay(), MatsimTestUtils.EPSILON);
				
				Assert.assertEquals("affected Agents", 0.0, wde6.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
				Assert.assertEquals("causing Agent", testAgent2.toString(), wde6.getCausingAgent().toString());
				Assert.assertEquals("delay", 2.0, wde6.getDelay(), MatsimTestUtils.EPSILON);
				
				Assert.assertEquals("affected Agents", 0.0, wde7.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
				Assert.assertEquals("causing Agent", testAgent2.toString(), wde7.getCausingAgent().toString());
				Assert.assertEquals("delay", 2.0, wde7.getDelay(), MatsimTestUtils.EPSILON);
				
				Assert.assertEquals("affected Agents", 0.0, wde8.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
				Assert.assertEquals("causing Agent", testAgent1.toString(), wde8.getCausingAgent().toString());
				Assert.assertEquals("delay", 2.0, wde8.getDelay(), MatsimTestUtils.EPSILON);
				
				//*******************************************************************************************
				
			}
	
	//agent 1 (2 --> 4)
	//agent 2 is waiting (3 --> 5)
	//agent 1 gets off the bus before agent 2 gets off the bus
	@Test
	public final void testWaitingDelay02(){
		
		Config config = utils.loadConfig(null);
		
		String networkFile = utils.getInputDirectory() + "multimodalnetwork.xml";
		String populationFile = utils.getInputDirectory() + "population.xml";
		String scheduleFile = utils.getInputDirectory() + "transitschedule.xml";
		String vehiclesFile = utils.getInputDirectory() + "transitVehicles.xml";
		
		config.network().setInputFile(networkFile);
		config.plans().setInputFile(populationFile);
		config.controler().setLastIteration(0);
		config.controler().setMobsim(MobsimType.qsim.toString());
		config.setParam("vspExperimental", "isGeneratingBoardingDeniedEvent", "true");
		
		config.scenario().setUseTransit(true);
		config.scenario().setUseVehicles(true);
		config.transit().setTransitScheduleFile(scheduleFile);
		config.transit().setVehiclesFile(vehiclesFile);
		
		ActivityParams hParams = new ActivityParams("h");
		hParams.setTypicalDuration(3600.);
		config.planCalcScore().addActivityParams(hParams);
		ActivityParams wParams = new ActivityParams("w");
		wParams.setTypicalDuration(3600.);
		config.planCalcScore().addActivityParams(wParams);
		
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		this.events = controler.getEvents();
		
		final List<TransferDelayWaitingEvent> waitingDelayEvents = new ArrayList<TransferDelayWaitingEvent>();
		
		events.addHandler( new TransferDelayWaitingEventHandler() {
			
			@Override
			public void reset(int iteration) {				
			}

			@Override
			public void handleEvent(TransferDelayWaitingEvent event) {
				waitingDelayEvents.add(event);
			}
			
		});
		
			controler.addControlerListener(new InternalizationPtControlerListener(scenario));
			controler.run();
		
		//*******************************************************************************************
		
		Assert.assertEquals("number of WaitingDelayEvents", 8, waitingDelayEvents.size());
		
		TransferDelayWaitingEvent wde1 = waitingDelayEvents.get(0);
		TransferDelayWaitingEvent wde2 = waitingDelayEvents.get(1);
		TransferDelayWaitingEvent wde3 = waitingDelayEvents.get(2);
		TransferDelayWaitingEvent wde4 = waitingDelayEvents.get(3);
		TransferDelayWaitingEvent wde5 = waitingDelayEvents.get(4);
		TransferDelayWaitingEvent wde6 = waitingDelayEvents.get(5);
		TransferDelayWaitingEvent wde7 = waitingDelayEvents.get(6);
		TransferDelayWaitingEvent wde8 = waitingDelayEvents.get(7);
		
		Assert.assertEquals("affected Agents", 1.0, wde1.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("causing Agent", testAgent1.toString(), wde1.getCausingAgent().toString());
		Assert.assertEquals("delay", 1.0, wde1.getDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("affected Agents", 0.0, wde2.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("causing Agent", testAgent2.toString(), wde2.getCausingAgent().toString());
		Assert.assertEquals("delay", 1.0, wde2.getDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("affected Agents", 0.0, wde3.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("causing Agent", testAgent1.toString(), wde3.getCausingAgent().toString());
		Assert.assertEquals("delay", 0.75, wde3.getDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("affected Agents", 0.0, wde4.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("causing Agent", testAgent2.toString(), wde4.getCausingAgent().toString());
		Assert.assertEquals("delay", 0.75, wde4.getDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("affected Agents", 1.0, wde5.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("causing Agent", testAgent1.toString(), wde5.getCausingAgent().toString());
		Assert.assertEquals("delay", 2.0, wde5.getDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("affected Agents", 0.0, wde6.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("causing Agent", testAgent2.toString(), wde6.getCausingAgent().toString());
		Assert.assertEquals("delay", 2.0, wde6.getDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("affected Agents", 0.0, wde7.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("causing Agent", testAgent1.toString(), wde7.getCausingAgent().toString());
		Assert.assertEquals("delay", 2.0, wde7.getDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("affected Agents", 0.0, wde8.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("causing Agent", testAgent2.toString(), wde8.getCausingAgent().toString());
		Assert.assertEquals("delay", 2.0, wde8.getDelay(), MatsimTestUtils.EPSILON);
		
		//*******************************************************************************************
		
	}
	
	//agent 1 (2 --> 3)
	//agent 2 is waiting (4 --> 5)
	//agent 2 waits less than the delay of the bus (2<3 sec) --> consideration of AffectedAgentsUnits
	@Test
	public final void testWaitingDelay03(){
		
		Config config = utils.loadConfig(null);
		
		String networkFile = utils.getInputDirectory() + "multimodalnetwork.xml";
		String populationFile = utils.getInputDirectory() + "population.xml";
		String scheduleFile = utils.getInputDirectory() + "transitschedule.xml";
		String vehiclesFile = utils.getInputDirectory() + "transitVehicles.xml";
		
		config.network().setInputFile(networkFile);
		config.plans().setInputFile(populationFile);
		config.controler().setLastIteration(0);
		config.controler().setMobsim(MobsimType.qsim.toString());
		config.setParam("vspExperimental", "isGeneratingBoardingDeniedEvent", "true");
		
		config.scenario().setUseTransit(true);
		config.scenario().setUseVehicles(true);
		config.transit().setTransitScheduleFile(scheduleFile);
		config.transit().setVehiclesFile(vehiclesFile);
		
		ActivityParams hParams = new ActivityParams("h");
		hParams.setTypicalDuration(3600.);
		config.planCalcScore().addActivityParams(hParams);
		ActivityParams wParams = new ActivityParams("w");
		wParams.setTypicalDuration(3600.);
		config.planCalcScore().addActivityParams(wParams);
		
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		this.events = controler.getEvents();
		
		final List<TransferDelayWaitingEvent> waitingDelayEvents = new ArrayList<TransferDelayWaitingEvent>();
		
		events.addHandler( new TransferDelayWaitingEventHandler() {
			
			@Override
			public void reset(int iteration) {				
			}

			@Override
			public void handleEvent(TransferDelayWaitingEvent event) {
				waitingDelayEvents.add(event);
			}
			
		});
		
			controler.addControlerListener(new InternalizationPtControlerListener(scenario));
			controler.run();
		
		//*******************************************************************************************
		
		Assert.assertEquals("number of WaitingDelayEvents", 8, waitingDelayEvents.size());
		
		TransferDelayWaitingEvent wde1 = waitingDelayEvents.get(0);
		TransferDelayWaitingEvent wde2 = waitingDelayEvents.get(1);
		TransferDelayWaitingEvent wde3 = waitingDelayEvents.get(2);
		TransferDelayWaitingEvent wde4 = waitingDelayEvents.get(3);
		TransferDelayWaitingEvent wde5 = waitingDelayEvents.get(4);
		TransferDelayWaitingEvent wde6 = waitingDelayEvents.get(5);
		TransferDelayWaitingEvent wde7 = waitingDelayEvents.get(6);
		TransferDelayWaitingEvent wde8 = waitingDelayEvents.get(7);
		
		Assert.assertEquals("affected Agents", 0.3333333333333333, wde1.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("causing Agent", testAgent1.toString(), wde1.getCausingAgent().toString());
		Assert.assertEquals("delay", 1.0, wde1.getDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("affected Agents", 0.0, wde2.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("causing Agent", testAgent2.toString(), wde2.getCausingAgent().toString());
		Assert.assertEquals("delay", 1.0, wde2.getDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("affected Agents", 0.3333333333333333, wde3.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("causing Agent", testAgent1.toString(), wde3.getCausingAgent().toString());
		Assert.assertEquals("delay", 0.75, wde3.getDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("affected Agents", 0.0, wde4.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("causing Agent", testAgent2.toString(), wde4.getCausingAgent().toString());
		Assert.assertEquals("delay", 0.75, wde4.getDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("affected Agents", 0.3333333333333333, wde5.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("causing Agent", testAgent1.toString(), wde5.getCausingAgent().toString());
		Assert.assertEquals("delay", 2.0, wde5.getDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("affected Agents", 0.3333333333333333, wde6.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("causing Agent", testAgent1.toString(), wde6.getCausingAgent().toString());
		Assert.assertEquals("delay", 2.0, wde6.getDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("affected Agents", 0.0, wde7.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("causing Agent", testAgent2.toString(), wde7.getCausingAgent().toString());
		Assert.assertEquals("delay", 2.0, wde7.getDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("affected Agents", 0.0, wde8.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("causing Agent", testAgent2.toString(), wde8.getCausingAgent().toString());
		Assert.assertEquals("delay", 2.0, wde8.getDelay(), MatsimTestUtils.EPSILON);
		
		//*******************************************************************************************
		
	}
	
	//agent 1 and agent 2 (2 --> 3)
	//agent 3 is waiting (4 --> 5)
	@Test
	public final void testWaitingDelay04(){
		
		Config config = utils.loadConfig(null);
		
		String networkFile = utils.getInputDirectory() + "multimodalnetwork.xml";
		String populationFile = utils.getInputDirectory() + "population.xml";
		String scheduleFile = utils.getInputDirectory() + "transitschedule.xml";
		String vehiclesFile = utils.getInputDirectory() + "transitVehicles.xml";
		
		config.network().setInputFile(networkFile);
		config.plans().setInputFile(populationFile);
		config.controler().setLastIteration(0);
		config.controler().setMobsim(MobsimType.qsim.toString());
		config.setParam("vspExperimental", "isGeneratingBoardingDeniedEvent", "true");
		
		config.scenario().setUseTransit(true);
		config.scenario().setUseVehicles(true);
		config.transit().setTransitScheduleFile(scheduleFile);
		config.transit().setVehiclesFile(vehiclesFile);
		
		ActivityParams hParams = new ActivityParams("h");
		hParams.setTypicalDuration(3600.);
		config.planCalcScore().addActivityParams(hParams);
		ActivityParams wParams = new ActivityParams("w");
		wParams.setTypicalDuration(3600.);
		config.planCalcScore().addActivityParams(wParams);
		
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		this.events = controler.getEvents();
		
		final List<TransferDelayWaitingEvent> waitingDelayEvents = new ArrayList<TransferDelayWaitingEvent>();
		
		events.addHandler( new TransferDelayWaitingEventHandler() {
			
			@Override
			public void reset(int iteration) {				
			}

			@Override
			public void handleEvent(TransferDelayWaitingEvent event) {
				waitingDelayEvents.add(event);
			}
			
		});
		
			controler.addControlerListener(new InternalizationPtControlerListener(scenario));
			controler.run();
		
		//*******************************************************************************************
		
		Assert.assertEquals("number of WaitingDelayEvents", 12, waitingDelayEvents.size());
		
		TransferDelayWaitingEvent wde1 = waitingDelayEvents.get(0);
		TransferDelayWaitingEvent wde2 = waitingDelayEvents.get(1);
		TransferDelayWaitingEvent wde3 = waitingDelayEvents.get(2);
		TransferDelayWaitingEvent wde4 = waitingDelayEvents.get(3);
		TransferDelayWaitingEvent wde5 = waitingDelayEvents.get(4);
		TransferDelayWaitingEvent wde6 = waitingDelayEvents.get(5);
		TransferDelayWaitingEvent wde7 = waitingDelayEvents.get(6);
		TransferDelayWaitingEvent wde8 = waitingDelayEvents.get(7);
		TransferDelayWaitingEvent wde9 = waitingDelayEvents.get(8);
		TransferDelayWaitingEvent wde10 = waitingDelayEvents.get(9);
		TransferDelayWaitingEvent wde11 = waitingDelayEvents.get(10);
		TransferDelayWaitingEvent wde12 = waitingDelayEvents.get(11);
		
		Assert.assertEquals("affected Agents", 2.0, wde1.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("causing Agent", testAgent1.toString(), wde1.getCausingAgent().toString());
		Assert.assertEquals("delay", 1.0, wde1.getDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("affected Agents", 1.0,  wde2.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("causing Agent", testAgent2.toString(), wde2.getCausingAgent().toString());
		Assert.assertEquals("delay", 1.0, wde2.getDelay(), MatsimTestUtils.EPSILON);

		Assert.assertEquals("affected Agents", 0.0, wde3.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("causing Agent", testAgent3.toString(), wde3.getCausingAgent().toString());
		Assert.assertEquals("delay", 1.0, wde3.getDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("affected Agents", 1.0, wde4.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("causing Agent", testAgent1.toString(), wde4.getCausingAgent().toString());
		Assert.assertEquals("delay", 0.75, wde4.getDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("affected Agents", 1.0, wde5.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("causing Agent", testAgent2.toString(), wde5.getCausingAgent().toString());
		Assert.assertEquals("delay", 0.75, wde5.getDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("affected Agents", 0.0, wde6.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("causing Agent", testAgent3.toString(), wde6.getCausingAgent().toString());
		Assert.assertEquals("delay", 0.75, wde6.getDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("affected Agents", 1.0, wde7.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("causing Agent", testAgent1.toString(), wde7.getCausingAgent().toString());
		Assert.assertEquals("delay", 1.0, wde7.getDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("affected Agents", 1.0, wde8.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("causing Agent", testAgent2.toString(), wde8.getCausingAgent().toString());
		Assert.assertEquals("delay", 1.0, wde8.getDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("affected Agents", 1.0, wde9.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("causing Agent", testAgent1.toString(), wde9.getCausingAgent().toString());
		Assert.assertEquals("delay", 1.0, wde9.getDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("affected Agents", 1.0, wde10.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("causing Agent", testAgent2.toString(), wde10.getCausingAgent().toString());
		Assert.assertEquals("delay", 1.0, wde10.getDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("affected Agents", 0.0, wde11.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("causing Agent", testAgent3.toString(), wde11.getCausingAgent().toString());
		Assert.assertEquals("delay", 2.0, wde11.getDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("affected Agents", 0.0, wde12.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("causing Agent", testAgent3.toString(), wde12.getCausingAgent().toString());
		Assert.assertEquals("delay", 2.0, wde12.getDelay(), MatsimTestUtils.EPSILON);
		
		//*******************************************************************************************
		
	}
	
	//agent 1  (2 --> 3)
	//agent 2 and 3 are waiting (4 --> 5)
	@Test
	public final void testWaitingDelay05(){
		
		Config config = utils.loadConfig(null);
		
		String networkFile = utils.getInputDirectory() + "multimodalnetwork.xml";
		String populationFile = utils.getInputDirectory() + "population.xml";
		String scheduleFile = utils.getInputDirectory() + "transitschedule.xml";
		String vehiclesFile = utils.getInputDirectory() + "transitVehicles.xml";
		
		config.network().setInputFile(networkFile);
		config.plans().setInputFile(populationFile);
		config.controler().setLastIteration(0);
		config.controler().setMobsim(MobsimType.qsim.toString());
		config.setParam("vspExperimental", "isGeneratingBoardingDeniedEvent", "true");
		
		config.scenario().setUseTransit(true);
		config.scenario().setUseVehicles(true);
		config.transit().setTransitScheduleFile(scheduleFile);
		config.transit().setVehiclesFile(vehiclesFile);
		
		ActivityParams hParams = new ActivityParams("h");
		hParams.setTypicalDuration(3600.);
		config.planCalcScore().addActivityParams(hParams);
		ActivityParams wParams = new ActivityParams("w");
		wParams.setTypicalDuration(3600.);
		config.planCalcScore().addActivityParams(wParams);
		
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		this.events = controler.getEvents();
		
		final List<TransferDelayWaitingEvent> waitingDelayEvents = new ArrayList<TransferDelayWaitingEvent>();
		
		events.addHandler( new TransferDelayWaitingEventHandler() {
			
			@Override
			public void reset(int iteration) {				
			}

			@Override
			public void handleEvent(TransferDelayWaitingEvent event) {
				waitingDelayEvents.add(event);
			}
			
		});
		
			controler.addControlerListener(new InternalizationPtControlerListener(scenario));
			controler.run();
		
		//*******************************************************************************************
		
		Assert.assertEquals("number of WaitingDelayEvents", 12, waitingDelayEvents.size());
		
		TransferDelayWaitingEvent wde1 = waitingDelayEvents.get(0);
		TransferDelayWaitingEvent wde2 = waitingDelayEvents.get(1);
		TransferDelayWaitingEvent wde3 = waitingDelayEvents.get(2);
		TransferDelayWaitingEvent wde4 = waitingDelayEvents.get(3);
		TransferDelayWaitingEvent wde5 = waitingDelayEvents.get(4);
		TransferDelayWaitingEvent wde6 = waitingDelayEvents.get(5);
		TransferDelayWaitingEvent wde7 = waitingDelayEvents.get(6);
		TransferDelayWaitingEvent wde8 = waitingDelayEvents.get(7);
		TransferDelayWaitingEvent wde9 = waitingDelayEvents.get(8);
		TransferDelayWaitingEvent wde10 = waitingDelayEvents.get(9);
		TransferDelayWaitingEvent wde11 = waitingDelayEvents.get(10);
		TransferDelayWaitingEvent wde12 = waitingDelayEvents.get(11);
		
		Assert.assertEquals("affected Agents", 2.0, wde1.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("causing Agent", testAgent1.toString(), wde1.getCausingAgent().toString());
		Assert.assertEquals("delay", 1.0, wde1.getDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("affected Agents", 1.0, wde2.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("causing Agent", testAgent2.toString(), wde2.getCausingAgent().toString());
		Assert.assertEquals("delay", 1.0, wde2.getDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("affected Agents", 0.0, wde3.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("causing Agent", testAgent3.toString(), wde3.getCausingAgent().toString());
		Assert.assertEquals("delay", 1.0, wde3.getDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("affected Agents", 2.0, wde4.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("causing Agent", testAgent1.toString(), wde4.getCausingAgent().toString());
		Assert.assertEquals("delay", 0.75, wde4.getDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("affected Agents", 0.0, wde5.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("causing Agent", testAgent2.toString(), wde5.getCausingAgent().toString());
		Assert.assertEquals("delay", 0.75, wde5.getDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("affected Agents", 0.0, wde6.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("causing Agent", testAgent3.toString(), wde6.getCausingAgent().toString());
		Assert.assertEquals("delay", 0.75, wde6.getDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("affected Agents", 2.0,  wde7.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("causing Agent", testAgent1.toString(), wde7.getCausingAgent().toString());
		Assert.assertEquals("delay", 2.0, wde7.getDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("affected Agents", 2.0, wde8.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("causing Agent", testAgent1.toString(), wde8.getCausingAgent().toString());
		Assert.assertEquals("delay", 2.0, wde8.getDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("affected Agents", 0.0, wde9.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("causing Agent", testAgent2.toString(), wde9.getCausingAgent().toString());
		Assert.assertEquals("delay", 1.0, wde9.getDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("affected Agents", 0.0, wde10.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("causing Agent", testAgent3.toString(), wde10.getCausingAgent().toString());
		Assert.assertEquals("delay", 1.0, wde10.getDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("affected Agents", 0.0, wde11.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("causing Agent", testAgent2.toString(), wde11.getCausingAgent().toString());
		Assert.assertEquals("delay", 1.0, wde11.getDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("affected Agents", 0.0, wde12.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("causing Agent", testAgent3.toString(), wde12.getCausingAgent().toString());
		Assert.assertEquals("delay", 1.0, wde12.getDelay(), MatsimTestUtils.EPSILON);
		
		//*******************************************************************************************
		
	}
	
	//capacity: 2
	//two agents from start (stop 1) to finish (stop 6)
	// a third cannot enter, he gets on the next bus (stop 2)
	@Test
	public final void testCapacityDelay01(){
		
		Config config = utils.loadConfig(null);
		
		String networkFile = utils.getInputDirectory() + "multimodalnetwork.xml";
		String populationFile = utils.getInputDirectory() + "population.xml";
		String scheduleFile = utils.getInputDirectory() + "transitschedule.xml";
		String vehiclesFile = utils.getInputDirectory() + "transitVehicles.xml";
		
		config.network().setInputFile(networkFile);
		config.plans().setInputFile(populationFile);
		config.controler().setLastIteration(0);
		config.controler().setMobsim(MobsimType.qsim.toString());
		config.setParam("vspExperimental", "isGeneratingBoardingDeniedEvent", "true");
		
		config.scenario().setUseTransit(true);
		config.scenario().setUseVehicles(true);
		config.transit().setTransitScheduleFile(scheduleFile);
		config.transit().setVehiclesFile(vehiclesFile);
		
		ActivityParams hParams = new ActivityParams("h");
		hParams.setTypicalDuration(3600.);
		config.planCalcScore().addActivityParams(hParams);
		ActivityParams wParams = new ActivityParams("w");
		wParams.setTypicalDuration(3600.);
		config.planCalcScore().addActivityParams(wParams);
		
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		this.events = controler.getEvents();
		
		final List<CapacityDelayEvent> capacityDelayEvents = new ArrayList<CapacityDelayEvent>();
		
		events.addHandler( new CapacityDelayEventHandler() {
			
			@Override
			public void reset(int iteration) {				
			}

			@Override
			public void handleEvent(CapacityDelayEvent event) {
				System.out.println(event.toString());
				capacityDelayEvents.add(event);
			}
			
		});
		
			controler.addControlerListener(new InternalizationPtControlerListener(scenario));
			controler.run();
		
		//*******************************************************************************************
		
		Assert.assertEquals("number of CapacityDelayEvents", 2, capacityDelayEvents.size());
		
		CapacityDelayEvent cde1 = capacityDelayEvents.get(0);
		CapacityDelayEvent cde2 = capacityDelayEvents.get(1);
		
		Assert.assertEquals("affected Agents", testAgent3.toString(), cde1.getAffectedAgentId().toString());
		Assert.assertEquals("causing Agent", testAgent1.toString(), cde1.getCausingAgentId().toString());
		Assert.assertEquals("delay", 198.5, cde1.getDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("affected Agents", testAgent3.toString(), cde2.getAffectedAgentId().toString());
		Assert.assertEquals("causing Agent", testAgent2.toString(), cde2.getCausingAgentId().toString());
		Assert.assertEquals("delay", 198.5, cde2.getDelay(), MatsimTestUtils.EPSILON);
		
		//*******************************************************************************************
		
	}
	
	//capacity: 2
	//one agent from start (stop 1) to finish (stop 6)
	//two agents waiting at stop 2
	//the second can get on the bus
	//the third agent cannot enter, he gets on the next bus
	@Test
	public final void testCapacityDelay02(){
	
		Config config = utils.loadConfig(null);
		
		String networkFile = utils.getInputDirectory() + "multimodalnetwork.xml";
		String populationFile = utils.getInputDirectory() + "population.xml";
		String scheduleFile = utils.getInputDirectory() + "transitschedule.xml";
		String vehiclesFile = utils.getInputDirectory() + "transitVehicles.xml";
		
		config.network().setInputFile(networkFile);
		config.plans().setInputFile(populationFile);
		config.controler().setLastIteration(0);
		config.controler().setMobsim(MobsimType.qsim.toString());
		config.setParam("vspExperimental", "isGeneratingBoardingDeniedEvent", "true");
		
		config.scenario().setUseTransit(true);
		config.scenario().setUseVehicles(true);
		config.transit().setTransitScheduleFile(scheduleFile);
		config.transit().setVehiclesFile(vehiclesFile);
		
		ActivityParams hParams = new ActivityParams("h");
		hParams.setTypicalDuration(3600.);
		config.planCalcScore().addActivityParams(hParams);
		ActivityParams wParams = new ActivityParams("w");
		wParams.setTypicalDuration(3600.);
		config.planCalcScore().addActivityParams(wParams);
		
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		this.events = controler.getEvents();
		
		final List<CapacityDelayEvent> capacityDelayEvents = new ArrayList<CapacityDelayEvent>();
		
		events.addHandler( new CapacityDelayEventHandler() {
			
			@Override
			public void reset(int iteration) {				
			}

			@Override
			public void handleEvent(CapacityDelayEvent event) {
				capacityDelayEvents.add(event);
			}
			
		});
		
			controler.addControlerListener(new InternalizationPtControlerListener(scenario));
			controler.run();	
			
		//*******************************************************************************************
		
		Assert.assertEquals("number of CapacityDelayEvents", 2, capacityDelayEvents.size());	
			
		CapacityDelayEvent cde1 = capacityDelayEvents.get(0);
		CapacityDelayEvent cde2 = capacityDelayEvents.get(1);
		
		Assert.assertEquals("affected Agents", testAgent3.toString(), cde1.getAffectedAgentId().toString());
		Assert.assertEquals("causing Agent", testAgent1.toString(), cde1.getCausingAgentId().toString());
		Assert.assertEquals("delay", 197.5, cde1.getDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("affected Agents", testAgent3.toString(), cde2.getAffectedAgentId().toString());
		Assert.assertEquals("causing Agent", testAgent2.toString(), cde2.getCausingAgentId().toString());
		Assert.assertEquals("delay", 197.5, cde2.getDelay(), MatsimTestUtils.EPSILON);
		
		//*******************************************************************************************
	}
	
	//capacity: 2
	//agent 5 is waiting at stop 4, he misses 2 buses due to capacity-constraints
	//the gap is not the same, the Marginal costs shouldn't be equally distributed
	@Test
	public final void testCapacityDelay03(){
		
		Config config = utils.loadConfig(null);
		
		String networkFile = utils.getInputDirectory() + "multimodalnetwork.xml";
		String populationFile = utils.getInputDirectory() + "population.xml";
		String scheduleFile = utils.getInputDirectory() + "transitschedule.xml";
		String vehiclesFile = utils.getInputDirectory() + "transitVehicles.xml";
		
		config.network().setInputFile(networkFile);
		config.plans().setInputFile(populationFile);
		config.controler().setLastIteration(0);
		config.controler().setMobsim(MobsimType.qsim.toString());
		config.setParam("vspExperimental", "isGeneratingBoardingDeniedEvent", "true");
		
		config.scenario().setUseTransit(true);
		config.scenario().setUseVehicles(true);
		config.transit().setTransitScheduleFile(scheduleFile);
		config.transit().setVehiclesFile(vehiclesFile);
		
		ActivityParams hParams = new ActivityParams("h");
		hParams.setTypicalDuration(3600.);
		config.planCalcScore().addActivityParams(hParams);
		ActivityParams wParams = new ActivityParams("w");
		wParams.setTypicalDuration(3600.);
		config.planCalcScore().addActivityParams(wParams);
		
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		this.events = controler.getEvents();
		
		final List<CapacityDelayEvent> capacityDelayEvents = new ArrayList<CapacityDelayEvent>();
		
		events.addHandler( new CapacityDelayEventHandler() {
			
			@Override
			public void reset(int iteration) {				
			}

			@Override
			public void handleEvent(CapacityDelayEvent event) {
				capacityDelayEvents.add(event);
			}
			
		});
		
			controler.addControlerListener(new InternalizationPtControlerListener(scenario));
			controler.run();	
		
		//*******************************************************************************************
		
		Assert.assertEquals("number of CapacityDelayEvents", 4, capacityDelayEvents.size());
		
		CapacityDelayEvent cde1 = capacityDelayEvents.get(0);
		CapacityDelayEvent cde2 = capacityDelayEvents.get(1);
		CapacityDelayEvent cde3 = capacityDelayEvents.get(2);
		CapacityDelayEvent cde4 = capacityDelayEvents.get(3);
		
		Assert.assertEquals("affected Agents", testAgent5.toString(), cde1.getAffectedAgentId().toString());
		Assert.assertEquals("causing Agent", testAgent1.toString(), cde1.getCausingAgentId().toString());
		Assert.assertEquals("delay", 200.0, cde1.getDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("affected Agents", testAgent5.toString(), cde2.getAffectedAgentId().toString());
		Assert.assertEquals("causing Agent", testAgent2.toString(), cde2.getCausingAgentId().toString());
		Assert.assertEquals("delay", 200.0, cde2.getDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("affected Agents", testAgent5.toString(), cde3.getAffectedAgentId().toString());
		Assert.assertEquals("causing Agent", testAgent3.toString(), cde3.getCausingAgentId().toString());
		Assert.assertEquals("delay", 98.5, cde3.getDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("affected Agents", testAgent5.toString(), cde4.getAffectedAgentId().toString());
		Assert.assertEquals("causing Agent", testAgent4.toString(), cde4.getCausingAgentId().toString());
		Assert.assertEquals("delay", 98.5, cde4.getDelay(), MatsimTestUtils.EPSILON);
		
		//*******************************************************************************************
		
	}
	
	//capacity: 2
	//two agents (agent 3 and 4) waiting at stop 2
	//two agents (agent 1 and 2) are leaving the bus at stop 2
	//there shouldn't be any capacity-constraints
	@Test
	public final void testCapacityDelay04(){
		
		Config config = utils.loadConfig(null);
		
		String networkFile = utils.getInputDirectory() + "multimodalnetwork.xml";
		String populationFile = utils.getInputDirectory() + "population.xml";
		String scheduleFile = utils.getInputDirectory() + "transitschedule.xml";
		String vehiclesFile = utils.getInputDirectory() + "transitVehicles.xml";
		
		config.network().setInputFile(networkFile);
		config.plans().setInputFile(populationFile);
		config.controler().setLastIteration(0);
		config.controler().setMobsim(MobsimType.qsim.toString());
		config.setParam("vspExperimental", "isGeneratingBoardingDeniedEvent", "true");
		
		config.scenario().setUseTransit(true);
		config.scenario().setUseVehicles(true);
		config.transit().setTransitScheduleFile(scheduleFile);
		config.transit().setVehiclesFile(vehiclesFile);
		
		ActivityParams hParams = new ActivityParams("h");
		hParams.setTypicalDuration(3600.);
		config.planCalcScore().addActivityParams(hParams);
		ActivityParams wParams = new ActivityParams("w");
		wParams.setTypicalDuration(3600.);
		config.planCalcScore().addActivityParams(wParams);
		
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		this.events = controler.getEvents();
		
		final List<CapacityDelayEvent> capacityDelayEvents = new ArrayList<CapacityDelayEvent>();
		
		events.addHandler( new CapacityDelayEventHandler() {
			
			@Override
			public void reset(int iteration) {				
			}

			@Override
			public void handleEvent(CapacityDelayEvent event) {
				capacityDelayEvents.add(event);
			}
			
		});
		
			controler.addControlerListener(new InternalizationPtControlerListener(scenario));
			controler.run();	
			
		//*******************************************************************************************
			
		Assert.assertEquals("number of CapacityDelayEvents", 0, capacityDelayEvents.size());
		
		//*******************************************************************************************
	}
	
	//capacity: 2
	//two agents (agent 1 and 2) from stop 1 to stop 2
	//two agents (agent 3 and 4) from stop 2 to stop 6, they get on when agents 1 and 2 get off the bus
	//agent 5 is waiting at stop 3 and can't get on the bus
	//Only agents 3 and 4 should be responsible for the CapacityDelay
	@Test
	public final void testCapacityDelay05(){
		
		Config config = utils.loadConfig(null);
		
		String networkFile = utils.getInputDirectory() + "multimodalnetwork.xml";
		String populationFile = utils.getInputDirectory() + "population.xml";
		String scheduleFile = utils.getInputDirectory() + "transitschedule.xml";
		String vehiclesFile = utils.getInputDirectory() + "transitVehicles.xml";
		
		config.network().setInputFile(networkFile);
		config.plans().setInputFile(populationFile);
		config.controler().setLastIteration(0);
		config.controler().setMobsim(MobsimType.qsim.toString());
		config.setParam("vspExperimental", "isGeneratingBoardingDeniedEvent", "true");
		
		config.scenario().setUseTransit(true);
		config.scenario().setUseVehicles(true);
		config.transit().setTransitScheduleFile(scheduleFile);
		config.transit().setVehiclesFile(vehiclesFile);
		
		ActivityParams hParams = new ActivityParams("h");
		hParams.setTypicalDuration(3600.);
		config.planCalcScore().addActivityParams(hParams);
		ActivityParams wParams = new ActivityParams("w");
		wParams.setTypicalDuration(3600.);
		config.planCalcScore().addActivityParams(wParams);
		
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		this.events = controler.getEvents();
		
		final List<CapacityDelayEvent> capacityDelayEvents = new ArrayList<CapacityDelayEvent>();
		
		events.addHandler( new CapacityDelayEventHandler() {
			
			@Override
			public void reset(int iteration) {				
			}

			@Override
			public void handleEvent(CapacityDelayEvent event) {
				capacityDelayEvents.add(event);
			}
			
		});
		
			controler.addControlerListener(new InternalizationPtControlerListener(scenario));
			controler.run();	
		
		//*******************************************************************************************
		
		Assert.assertEquals("number of CapacityDelayEvents", 2, capacityDelayEvents.size());
		
		CapacityDelayEvent cde1 = capacityDelayEvents.get(0);
		CapacityDelayEvent cde2 = capacityDelayEvents.get(1);
		
		Assert.assertEquals("affected Agents", testAgent5.toString(), cde1.getAffectedAgentId().toString());
		Assert.assertEquals("causing Agent", testAgent3.toString(), cde1.getCausingAgentId().toString());
		Assert.assertEquals("delay", 196.0, cde1.getDelay(), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("affected Agents", testAgent5.toString(), cde2.getAffectedAgentId().toString());
		Assert.assertEquals("causing Agent", testAgent4.toString(), cde2.getCausingAgentId().toString());
		Assert.assertEquals("delay", 196.0, cde2.getDelay(), MatsimTestUtils.EPSILON);
		
		//*******************************************************************************************
		
	}
	
}


