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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControlerConfigGroup.MobsimType;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleType.DoorOperationMode;
import org.matsim.vehicles.Vehicles;

public class MarginalCostPricingPtHandlerIT {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();
	
	private EventsManager events;
	
	private Id<Person> testAgent1 = Id.create("testAgent1", Person.class);
	private Id<Person> testAgent2 = Id.create("testAgent2", Person.class);
	private Id<Person> testAgent3 = Id.create("testAgent3", Person.class);
	private Id<Person> testAgent4 = Id.create("testAgent4", Person.class);
	private Id<Person> testAgent5 = Id.create("testAgent5", Person.class);
	
	private Id<Link> linkId1 = Id.create("1", Link.class);
	private Id<Link> linkId2 = Id.create("2", Link.class);
	private Id<Link> linkId3 = Id.create("3", Link.class);
	private Id<Link> linkId4 = Id.create("4", Link.class);
	private Id<Link> linkId5 = Id.create("5", Link.class);
	private Id<Link> linkId6 = Id.create("6", Link.class);
	private Id<Link> linkId7 = Id.create("7", Link.class);
	private Id<Link> linkId8 = Id.create("8", Link.class);
	private Id<Link> linkId9 = Id.create("9", Link.class);
	private Id<Link> linkId10 = Id.create("10", Link.class);
	private Id<Link> linkId11 = Id.create("11", Link.class);
	private Id<Link> linkId12 = Id.create("12", Link.class);
	
	//one agent from start (stop 1) to finish (stop 6)
	//another agent gets on (stop 2) and off (stop 3)
	@Test
    public final void testInVehicleDelay01() {
   	 	
		Config config = utils.createConfig();
		
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(0);
		config.controler().setMobsim(MobsimType.qsim.toString());
		config.setParam("vspExperimental", "isGeneratingBoardingDeniedEvent", "true");
		
		config.transit().setUseTransit(true);
		
		ActivityParams hParams = new ActivityParams("h");
		hParams.setTypicalDuration(3600.);
		config.planCalcScore().addActivityParams(hParams);
		ActivityParams wParams = new ActivityParams("w");
		wParams.setTypicalDuration(3600.);
		config.planCalcScore().addActivityParams(wParams);
		
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);

		setPopulationTestInVehicleDelay01(scenario);
		fillScheduleTestInVehicleDelay01(scenario);
		fillVehicleTestInVehicleDelay01(scenario);
		setNetworkOneWay(scenario);
		
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
	
	Config config = utils.createConfig();
	
	config.controler().setOutputDirectory(utils.getOutputDirectory());
	config.controler().setLastIteration(0);
	config.controler().setMobsim(MobsimType.qsim.toString());
	config.setParam("vspExperimental", "isGeneratingBoardingDeniedEvent", "true");
	
	config.transit().setUseTransit(true);
	
	ActivityParams hParams = new ActivityParams("h");
	hParams.setTypicalDuration(3600.);
	config.planCalcScore().addActivityParams(hParams);
	ActivityParams wParams = new ActivityParams("w");
	wParams.setTypicalDuration(3600.);
	config.planCalcScore().addActivityParams(wParams);
	
	MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);
	
	setPopulationTestInVehicleDelay02(scenario);
	fillScheduleTestInVehicleDelay02(scenario);
	fillVehicleTestInVehicleDelay02(scenario);
	setNetworkOneWay(scenario);
	
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
		
		Config config = utils.createConfig();
		
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(0);
		config.controler().setMobsim(MobsimType.qsim.toString());
		config.setParam("vspExperimental", "isGeneratingBoardingDeniedEvent", "true");
		
		config.transit().setUseTransit(true);
		
		ActivityParams hParams = new ActivityParams("h");
		hParams.setTypicalDuration(3600.);
		config.planCalcScore().addActivityParams(hParams);
		ActivityParams wParams = new ActivityParams("w");
		wParams.setTypicalDuration(3600.);
		config.planCalcScore().addActivityParams(wParams);
		
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);
		
		setPopulationTestInVehicleDelay03(scenario);
		fillScheduleTestInVehicleDelay03(scenario);
		fillVehicleTestInVehicleDelay03(scenario);
		setNetworkOneWay(scenario);
		
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
		
		Config config = utils.createConfig();
		
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(0);
		config.controler().setMobsim(MobsimType.qsim.toString());
		config.setParam("vspExperimental", "isGeneratingBoardingDeniedEvent", "true");
		
		config.transit().setUseTransit(true);
		
		ActivityParams hParams = new ActivityParams("h");
		hParams.setTypicalDuration(3600.);
		config.planCalcScore().addActivityParams(hParams);
		ActivityParams wParams = new ActivityParams("w");
		wParams.setTypicalDuration(3600.);
		config.planCalcScore().addActivityParams(wParams);
		
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);
		
		setPopulationTestWaitingDelay01(scenario);
		fillScheduleTestWaitingDelay01(scenario);
		fillVehicleTestWaitingDelay01(scenario);
		setNetworkOneWay(scenario);
		
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
		
		Config config = utils.createConfig();
		
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(0);
		config.controler().setMobsim(MobsimType.qsim.toString());
		config.setParam("vspExperimental", "isGeneratingBoardingDeniedEvent", "true");
		
		config.transit().setUseTransit(true);
		
		ActivityParams hParams = new ActivityParams("h");
		hParams.setTypicalDuration(3600.);
		config.planCalcScore().addActivityParams(hParams);
		ActivityParams wParams = new ActivityParams("w");
		wParams.setTypicalDuration(3600.);
		config.planCalcScore().addActivityParams(wParams);
		
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);

		setPopulationTestWaitingDelay02(scenario);
		fillScheduleTestWaitingDelay02(scenario);
		fillVehicleTestWaitingDelay02(scenario);
		setNetworkOneWay(scenario);
		
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
	//agent 2 waits less than the delay of the bus (2.0<6.0 sec) --> consideration of AffectedAgentsUnits
	//It is important to define a bus in the schedule which leaves later on,
	//even if agent 2 will nevertheless get the first bus.
	@Test
	public final void testWaitingDelay03(){
		
		Config config = utils.createConfig();
		
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(0);
		config.controler().setMobsim(MobsimType.qsim.toString());
		config.setParam("vspExperimental", "isGeneratingBoardingDeniedEvent", "true");
		
		config.transit().setUseTransit(true);
		
		ActivityParams hParams = new ActivityParams("h");
		hParams.setTypicalDuration(3600.);
		config.planCalcScore().addActivityParams(hParams);
		ActivityParams wParams = new ActivityParams("w");
		wParams.setTypicalDuration(3600.);
		config.planCalcScore().addActivityParams(wParams);
		
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);

		setPopulationTestWaitingDelay03(scenario);
		fillScheduleTestWaitingDelay03(scenario);
		fillVehicleTestWaitingDelay03(scenario);
		setNetworkOneWay(scenario);
		
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
			
			Assert.assertEquals("affected Agents", 0.5, wde1.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
			Assert.assertEquals("causing Agent", testAgent1.toString(), wde1.getCausingAgent().toString());
			Assert.assertEquals("delay", 1.0, wde1.getDelay(), MatsimTestUtils.EPSILON);
			
			Assert.assertEquals("affected Agents", 0.0, wde2.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
			Assert.assertEquals("causing Agent", testAgent2.toString(), wde2.getCausingAgent().toString());
			Assert.assertEquals("delay", 1.0, wde2.getDelay(), MatsimTestUtils.EPSILON);
			
			Assert.assertEquals("affected Agents", 0.5, wde3.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
			Assert.assertEquals("causing Agent", testAgent1.toString(), wde3.getCausingAgent().toString());
			Assert.assertEquals("delay", 0.75, wde3.getDelay(), MatsimTestUtils.EPSILON);
			
			Assert.assertEquals("affected Agents", 0.0, wde4.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
			Assert.assertEquals("causing Agent", testAgent2.toString(), wde4.getCausingAgent().toString());
			Assert.assertEquals("delay", 0.75, wde4.getDelay(), MatsimTestUtils.EPSILON);
			
			Assert.assertEquals("affected Agents", 0.5, wde5.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
			Assert.assertEquals("causing Agent", testAgent1.toString(), wde5.getCausingAgent().toString());
			Assert.assertEquals("delay", 2.0, wde5.getDelay(), MatsimTestUtils.EPSILON);
			
			Assert.assertEquals("affected Agents", 0.5, wde6.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
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

	// Difficulties if an agent arrives 
	// 1.) at the stop in the time step when the bus would already close the doors or
	// 2.) at the stop in the time step when the bus would pass the 
	//
	//agent 1 (2 --> 4)
	//agent 2 is waiting (4 --> 5)
	//agent 2 arrives in the time step of (/ after) the arrival of the bus at the station,
	@Ignore
	@Test
	public final void testWaitingDelay03b(){
			
		Config config = utils.createConfig();
			
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(0);
		config.controler().setMobsim(MobsimType.qsim.toString());
		config.setParam("vspExperimental", "isGeneratingBoardingDeniedEvent", "true");
			
		config.transit().setUseTransit(true);

		ActivityParams hParams = new ActivityParams("h");
		hParams.setTypicalDuration(3600.);
		config.planCalcScore().addActivityParams(hParams);
		ActivityParams wParams = new ActivityParams("w");
		wParams.setTypicalDuration(3600.);
		config.planCalcScore().addActivityParams(wParams);
			
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);

		setPopulationTestWaitingDelay03b(scenario);
		fillScheduleTestWaitingDelay03b(scenario);
		fillVehicleTestWaitingDelay03b(scenario);
		setNetworkOneWay(scenario);
			
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
				
			Assert.assertEquals("affected Agents", 0.42105263157894735, wde1.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
			Assert.assertEquals("causing Agent", testAgent1.toString(), wde1.getCausingAgent().toString());
			Assert.assertEquals("delay", 1.0, wde1.getDelay(), MatsimTestUtils.EPSILON);
				
			Assert.assertEquals("affected Agents", 0.0, wde2.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
			Assert.assertEquals("causing Agent", testAgent2.toString(), wde2.getCausingAgent().toString());
			Assert.assertEquals("delay", 1.0, wde2.getDelay(), MatsimTestUtils.EPSILON);
				
			Assert.assertEquals("affected Agents", 0.42105263157894735, wde3.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
			Assert.assertEquals("causing Agent", testAgent1.toString(), wde3.getCausingAgent().toString());
			Assert.assertEquals("delay", 0.75, wde3.getDelay(), MatsimTestUtils.EPSILON);
				
			Assert.assertEquals("affected Agents", 0.0, wde4.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
			Assert.assertEquals("causing Agent", testAgent2.toString(), wde4.getCausingAgent().toString());
			Assert.assertEquals("delay", 0.75, wde4.getDelay(), MatsimTestUtils.EPSILON);
				
			Assert.assertEquals("affected Agents", 0.42105263157894735, wde5.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
			Assert.assertEquals("causing Agent", testAgent1.toString(), wde5.getCausingAgent().toString());
			Assert.assertEquals("delay", 2.0, wde5.getDelay(), MatsimTestUtils.EPSILON);
				
			Assert.assertEquals("affected Agents", 0, wde6.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
			Assert.assertEquals("causing Agent", testAgent1.toString(), wde6.getCausingAgent().toString());
			Assert.assertEquals("delay", 1.0, wde6.getDelay(), MatsimTestUtils.EPSILON);
				
			Assert.assertEquals("affected Agents", 0.0, wde7.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
			Assert.assertEquals("causing Agent", testAgent2.toString(), wde7.getCausingAgent().toString());
			Assert.assertEquals("delay", 1.0, wde7.getDelay(), MatsimTestUtils.EPSILON);
				
			Assert.assertEquals("affected Agents", 0.0, wde8.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
			Assert.assertEquals("causing Agent", testAgent2.toString(), wde8.getCausingAgent().toString());
			Assert.assertEquals("delay", 2.0, wde8.getDelay(), MatsimTestUtils.EPSILON);
				
			//*******************************************************************************************
			
	}

	//agent 1 and agent 2 (2 --> 3)
	//agent 3 is waiting (4 --> 5)
	@Test
	public final void testWaitingDelay04(){
		
		Config config = utils.createConfig();
		
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(0);
		config.controler().setMobsim(MobsimType.qsim.toString());
		config.setParam("vspExperimental", "isGeneratingBoardingDeniedEvent", "true");
		
		config.transit().setUseTransit(true);
		
		ActivityParams hParams = new ActivityParams("h");
		hParams.setTypicalDuration(3600.);
		config.planCalcScore().addActivityParams(hParams);
		ActivityParams wParams = new ActivityParams("w");
		wParams.setTypicalDuration(3600.);
		config.planCalcScore().addActivityParams(wParams);
		
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);

		setPopulationTestWaitingDelay04(scenario);
		fillScheduleTestWaitingDelay04(scenario);
		fillVehicleTestWaitingDelay04(scenario);
		setNetworkOneWay(scenario);
		
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
		
		Config config = utils.createConfig();
		
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(0);
		config.controler().setMobsim(MobsimType.qsim.toString());
		config.setParam("vspExperimental", "isGeneratingBoardingDeniedEvent", "true");
		
		config.transit().setUseTransit(true);
		
		ActivityParams hParams = new ActivityParams("h");
		hParams.setTypicalDuration(3600.);
		config.planCalcScore().addActivityParams(hParams);
		ActivityParams wParams = new ActivityParams("w");
		wParams.setTypicalDuration(3600.);
		config.planCalcScore().addActivityParams(wParams);
		
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);

		setPopulationTestWaitingDelay05(scenario);
		fillScheduleTestWaitingDelay05(scenario);
		fillVehicleTestWaitingDelay05(scenario);
		setNetworkOneWay(scenario);
		
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
	
	//different network, suitable for round trips
	//Testing over several departures:
	//agent 1 (2 --> 5)
	//agent 2 (5 --> 2)
	//agent 3 (2 --> 5)
	@Test
	public final void testWaitingDelay06(){
		
		Config config = utils.createConfig();
		
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(0);
		config.controler().setMobsim(MobsimType.qsim.toString());
		config.setParam("vspExperimental", "isGeneratingBoardingDeniedEvent", "true");
		
		config.transit().setUseTransit(true);
		
		ActivityParams hParams = new ActivityParams("h");
		hParams.setTypicalDuration(3600.);
		config.planCalcScore().addActivityParams(hParams);
		ActivityParams wParams = new ActivityParams("w");
		wParams.setTypicalDuration(3600.);
		config.planCalcScore().addActivityParams(wParams);
		
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);

		setPopulationTestWaitingDelay06(scenario);
		fillScheduleTestWaitingDelay06(scenario);
		fillVehicleTestWaitingDelay06(scenario);
		setNetworkRoundTrip(scenario);
		
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
			
			Assert.assertEquals("affected Agents", 0.0, wde1.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
			Assert.assertEquals("causing Agent", testAgent1.toString(), wde1.getCausingAgent().toString());
			Assert.assertEquals("delay", 1.0, wde1.getDelay(), MatsimTestUtils.EPSILON);
	
			Assert.assertEquals("affected Agents", 0.0, wde2.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
			Assert.assertEquals("causing Agent", testAgent1.toString(), wde2.getCausingAgent().toString());
			Assert.assertEquals("delay", 0.75, wde2.getDelay(), MatsimTestUtils.EPSILON);
			
			Assert.assertEquals("affected Agents", 0.0, wde3.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
			Assert.assertEquals("causing Agent", testAgent1.toString(), wde3.getCausingAgent().toString());
			Assert.assertEquals("delay", 2.0, wde3.getDelay(), MatsimTestUtils.EPSILON);
			
			Assert.assertEquals("affected Agents", 0.0, wde4.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
			Assert.assertEquals("causing Agent", testAgent1.toString(), wde4.getCausingAgent().toString());
			Assert.assertEquals("delay", 2.0, wde4.getDelay(), MatsimTestUtils.EPSILON);
			
			Assert.assertEquals("affected Agents", 0.0, wde5.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
			Assert.assertEquals("causing Agent", testAgent2.toString(), wde5.getCausingAgent().toString());
			Assert.assertEquals("delay", 1.0, wde5.getDelay(), MatsimTestUtils.EPSILON);
			
			Assert.assertEquals("affected Agents", 0.0, wde6.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
			Assert.assertEquals("causing Agent", testAgent2.toString(), wde6.getCausingAgent().toString());
			Assert.assertEquals("delay", 0.75, wde6.getDelay(), MatsimTestUtils.EPSILON);
			
			Assert.assertEquals("affected Agents", 0.0, wde7.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
			Assert.assertEquals("causing Agent", testAgent2.toString(), wde7.getCausingAgent().toString());
			Assert.assertEquals("delay", 2.0, wde7.getDelay(), MatsimTestUtils.EPSILON);
	
			Assert.assertEquals("affected Agents", 0.0, wde8.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
			Assert.assertEquals("causing Agent", testAgent2.toString(), wde8.getCausingAgent().toString());
			Assert.assertEquals("delay", 2.0, wde8.getDelay(), MatsimTestUtils.EPSILON);
			
			Assert.assertEquals("affected Agents", 0.0, wde9.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
			Assert.assertEquals("causing Agent", testAgent3.toString(), wde9.getCausingAgent().toString());
			Assert.assertEquals("delay", 1.0, wde9.getDelay(), MatsimTestUtils.EPSILON);
			
			Assert.assertEquals("affected Agents", 0.0, wde10.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
			Assert.assertEquals("causing Agent", testAgent3.toString(), wde10.getCausingAgent().toString());
			Assert.assertEquals("delay", 0.75, wde10.getDelay(), MatsimTestUtils.EPSILON);
			
			Assert.assertEquals("affected Agents", 0.0, wde11.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
			Assert.assertEquals("causing Agent", testAgent3.toString(), wde11.getCausingAgent().toString());
			Assert.assertEquals("delay", 2.0, wde11.getDelay(), MatsimTestUtils.EPSILON);
	
			Assert.assertEquals("affected Agents", 0.0, wde12.getAffectedAgentUnits(), MatsimTestUtils.EPSILON);
			Assert.assertEquals("causing Agent", testAgent3.toString(), wde12.getCausingAgent().toString());
			Assert.assertEquals("delay", 2.0, wde12.getDelay(), MatsimTestUtils.EPSILON);
			
			//*******************************************************************************************
		
	}
	
	//capacity: 2
	//two agents from start (stop 1) to finish (stop 6)
	// a third cannot enter, he gets on the next bus (stop 2)
	@Test
	public final void testCapacityDelay01(){
		
		Config config = utils.createConfig();
		
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(0);
		config.controler().setMobsim(MobsimType.qsim.toString());
		config.setParam("vspExperimental", "isGeneratingBoardingDeniedEvent", "true");
		
		config.transit().setUseTransit(true);
		
		ActivityParams hParams = new ActivityParams("h");
		hParams.setTypicalDuration(3600.);
		config.planCalcScore().addActivityParams(hParams);
		ActivityParams wParams = new ActivityParams("w");
		wParams.setTypicalDuration(3600.);
		config.planCalcScore().addActivityParams(wParams);
		
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);

		setPopulationTestCapacityDelay01(scenario);
		fillScheduleTestCapacityDelay01(scenario);
		fillVehicleTestCapacityDelay01(scenario);
		setNetworkOneWay(scenario);
		
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
	
		Config config = utils.createConfig();
		
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(0);
		config.controler().setMobsim(MobsimType.qsim.toString());
		config.setParam("vspExperimental", "isGeneratingBoardingDeniedEvent", "true");
		
		config.transit().setUseTransit(true);
		
		ActivityParams hParams = new ActivityParams("h");
		hParams.setTypicalDuration(3600.);
		config.planCalcScore().addActivityParams(hParams);
		ActivityParams wParams = new ActivityParams("w");
		wParams.setTypicalDuration(3600.);
		config.planCalcScore().addActivityParams(wParams);
		
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);

		setPopulationTestCapacityDelay02(scenario);
		fillScheduleTestCapacityDelay02(scenario);
		fillVehicleTestCapacityDelay02(scenario);
		setNetworkOneWay(scenario);
		
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
		
		Config config = utils.createConfig();
		
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(0);
		config.controler().setMobsim(MobsimType.qsim.toString());
		config.setParam("vspExperimental", "isGeneratingBoardingDeniedEvent", "true");
		
		config.transit().setUseTransit(true);
		
		ActivityParams hParams = new ActivityParams("h");
		hParams.setTypicalDuration(3600.);
		config.planCalcScore().addActivityParams(hParams);
		ActivityParams wParams = new ActivityParams("w");
		wParams.setTypicalDuration(3600.);
		config.planCalcScore().addActivityParams(wParams);
		
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);

		setPopulationTestCapacityDelay03(scenario);
		fillScheduleTestCapacityDelay03(scenario);
		fillVehicleTestCapacityDelay03(scenario);
		setNetworkOneWay(scenario);
		
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
		
		Config config = utils.createConfig();
		
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(0);
		config.controler().setMobsim(MobsimType.qsim.toString());
		config.setParam("vspExperimental", "isGeneratingBoardingDeniedEvent", "true");
		
		config.transit().setUseTransit(true);
		
		ActivityParams hParams = new ActivityParams("h");
		hParams.setTypicalDuration(3600.);
		config.planCalcScore().addActivityParams(hParams);
		ActivityParams wParams = new ActivityParams("w");
		wParams.setTypicalDuration(3600.);
		config.planCalcScore().addActivityParams(wParams);
		
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);

		setPopulationTestCapacityDelay04(scenario);
		fillScheduleTestCapacityDelay04(scenario);
		fillVehicleTestCapacityDelay04(scenario);
		setNetworkOneWay(scenario);
		
		Controler controler = new Controler(scenario);
		this.events = controler.getEvents();
		
		final List<CapacityDelayEvent> capacityDelayEvents = new ArrayList<CapacityDelayEvent>();
		final List<ActivityStartEvent> activityStartEvents = new ArrayList<ActivityStartEvent>();
		
		events.addHandler( new CapacityDelayEventHandler() {
			
			@Override
			public void reset(int iteration) {				
			}

			@Override
			public void handleEvent(CapacityDelayEvent event) {
				capacityDelayEvents.add(event);
			}
			
		});
		
		events.addHandler( new ActivityStartEventHandler() {
			
			@Override
			public void reset(int iteration) {
				
			}
			
			@Override
			public void handleEvent(ActivityStartEvent event) {
				
				if(event.getActType().toString().equals("w")){
					activityStartEvents.add(event);
				}else{}
			}
		});
		
			controler.addControlerListener(new InternalizationPtControlerListener(scenario));
			controler.run();	
			
			//*******************************************************************************************
				
			Assert.assertEquals("number of CapacityDelayEvents", 0, capacityDelayEvents.size());
			Assert.assertEquals("number of ActivityStartEvents", 4, activityStartEvents.size()); //to make sure that the four agents arrive at their destinations
			
			//*******************************************************************************************
	}
	
	//capacity: 2
	//two agents (agent 1 and 2) from stop 1 to stop 2
	//two agents (agent 3 and 4) from stop 2 to stop 6, they get on when agents 1 and 2 get off the bus
	//agent 5 is waiting at stop 3 and can't get on the bus
	//Only agents 3 and 4 should be responsible for the CapacityDelay
	@Test
	public final void testCapacityDelay05(){
		
		Config config = utils.createConfig();
		
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(0);
		config.controler().setMobsim(MobsimType.qsim.toString());
		config.setParam("vspExperimental", "isGeneratingBoardingDeniedEvent", "true");
		
		config.transit().setUseTransit(true);
		
		ActivityParams hParams = new ActivityParams("h");
		hParams.setTypicalDuration(3600.);
		config.planCalcScore().addActivityParams(hParams);
		ActivityParams wParams = new ActivityParams("w");
		wParams.setTypicalDuration(3600.);
		config.planCalcScore().addActivityParams(wParams);
		
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);

		setPopulationTestCapacityDelay05(scenario);
		fillScheduleTestCapacityDelay05(scenario);
		fillVehicleTestCapacityDelay05(scenario);
		setNetworkOneWay(scenario);
		
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
	
	//	The tests for the marginal operator costs can be added later.
	//
	//	//one agent gets on (stop 2) and gets off (stop 3)
	//	@Test
	//	public final void testOperatorCost1(){
	//	
	//	}
	//	
	//	//two agents get on and off (agent 1: 2 --> 3, agent 2: 4 --> 5
	//	@Test
	//	public final void testOperatorCost2(){
	//		
	//	}
	//	
	//	//two agents get on and off at the same stops (2 --> 3)
	//	@Test
	//	public final void testOperatorCost3(){
	//		
	//	}
	//	
	//	//two agents get on and off (agent 1: 2 --> 3, agent 2: 3 --> 4)
	//	//agent 1 gets off and agent 2 gets on at the same stop
	//	@Test
	//	public final void testOperatorCost4(){
	//		
	//	}
	//	
	//	//two agents get on and off (agent 1: 2 --> 3, agent 2: 2 --> 4)
	//	//both get on the bus at the same stop
	//	@Test
	//	public final void testOperatorCost5(){
	//		
	//	}
	
	//*************************************************************************	
	
	private void setNetworkOneWay(Scenario scenario){
		
		Network network = (Network) scenario.getNetwork();
		network.setEffectiveCellSize(7.5);
		network.setCapacityPeriod(3600.);

		Node node0 = network.getFactory().createNode(Id.create("0", Node.class), new Coord(0., 0.));
		Node node1 = network.getFactory().createNode(Id.create("1", Node.class), new Coord(500., 0.));
		Node node2 = network.getFactory().createNode(Id.create("2", Node.class), new Coord(1000., 0.));
		Node node3 = network.getFactory().createNode(Id.create("3", Node.class), new Coord(1500., 0.));
		Node node4 = network.getFactory().createNode(Id.create("4", Node.class), new Coord(2000., 0.));
		Node node5 = network.getFactory().createNode(Id.create("5", Node.class), new Coord(2500., 0.));
		Node node6 = network.getFactory().createNode(Id.create("6", Node.class), new Coord(3000., 0.));
		
		Link link1 = network.getFactory().createLink(Id.create("1", Link.class), node0, node1);
		Link link2 = network.getFactory().createLink(Id.create("2", Link.class), node1, node2);
		Link link3 = network.getFactory().createLink(Id.create("3", Link.class), node2, node3);
		Link link4 = network.getFactory().createLink(Id.create("4", Link.class), node3, node4);
		Link link5 = network.getFactory().createLink(Id.create("5", Link.class), node4, node5);
		Link link6 = network.getFactory().createLink(Id.create("6", Link.class), node5, node6);
		
		Set<String> modes = new HashSet<String>();
		modes.add("bus");
		modes.add("car");
		
		link1.setLength(500.0);
		link1.setCapacity(7200);
		link1.setFreespeed(8.4);
		link1.setAllowedModes(modes);
		link1.setNumberOfLanes(1);
		
		link2.setLength(500.0);
		link2.setCapacity(7200);
		link2.setFreespeed(8.4);
		link2.setAllowedModes(modes);
		link2.setNumberOfLanes(1);
		
		link3.setLength(500.0);
		link3.setCapacity(7200);
		link3.setFreespeed(8.4);
		link3.setAllowedModes(modes);
		link3.setNumberOfLanes(1);
		
		link4.setLength(500.0);
		link4.setCapacity(7200);
		link4.setFreespeed(8.4);
		link4.setAllowedModes(modes);
		link4.setNumberOfLanes(1);
		
		link5.setLength(500.0);
		link5.setCapacity(7200);
		link5.setFreespeed(8.4);
		link5.setAllowedModes(modes);
		link5.setNumberOfLanes(1);
		
		link6.setLength(500.0);
		link6.setCapacity(7200);
		link6.setFreespeed(8.4);
		link6.setAllowedModes(modes);
		link6.setNumberOfLanes(1);
		
		network.addNode(node0);
		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);
		network.addNode(node4);
		network.addNode(node5);
		network.addNode(node6);

		network.addLink(link1);
		network.addLink(link2);
		network.addLink(link3);
		network.addLink(link4);
		network.addLink(link5);
		network.addLink(link6);
		
	}
	
	private void setNetworkRoundTrip(Scenario scenario){
		
		Network network = (Network) scenario.getNetwork();
		network.setEffectiveCellSize(7.5);
		network.setCapacityPeriod(3600.);

		Node node0 = network.getFactory().createNode(Id.create("0", Node.class), new Coord(0., 0.));
		Node node1 = network.getFactory().createNode(Id.create("1", Node.class), new Coord(500., 0.));
		Node node2 = network.getFactory().createNode(Id.create("2", Node.class), new Coord(1000., 0.));
		Node node3 = network.getFactory().createNode(Id.create("3", Node.class), new Coord(1500., 0.));
		Node node4 = network.getFactory().createNode(Id.create("4", Node.class), new Coord(2000., 0.));
		Node node5 = network.getFactory().createNode(Id.create("5", Node.class), new Coord(2500., 0.));
		Node node6 = network.getFactory().createNode(Id.create("6", Node.class), new Coord(3000., 0.));
		
		Link link1 = network.getFactory().createLink(Id.create("1", Link.class), node0, node1);
		Link link2 = network.getFactory().createLink(Id.create("2", Link.class), node1, node2);
		Link link3 = network.getFactory().createLink(Id.create("3", Link.class), node2, node3);
		Link link4 = network.getFactory().createLink(Id.create("4", Link.class), node3, node4);
		Link link5 = network.getFactory().createLink(Id.create("5", Link.class), node4, node5);
		Link link6 = network.getFactory().createLink(Id.create("6", Link.class), node5, node6);
		Link link7 = network.getFactory().createLink(Id.create("7", Link.class), node6, node5);
		Link link8 = network.getFactory().createLink(Id.create("8", Link.class), node5, node4);
		Link link9 = network.getFactory().createLink(Id.create("9", Link.class), node4, node3);
		Link link10 = network.getFactory().createLink(Id.create("10", Link.class), node3, node2);
		Link link11 = network.getFactory().createLink(Id.create("11", Link.class), node2, node1);
		Link link12 = network.getFactory().createLink(Id.create("12", Link.class), node1, node0);
		
		Set<String> modes = new HashSet<String>();
		modes.add("bus");
		modes.add("car");
		
		link1.setLength(500.0);
		link1.setCapacity(7200);
		link1.setFreespeed(8.4);
		link1.setAllowedModes(modes);
		link1.setNumberOfLanes(1);
		
		link2.setLength(500.0);
		link2.setCapacity(7200);
		link2.setFreespeed(8.4);
		link2.setAllowedModes(modes);
		link2.setNumberOfLanes(1);
		
		link3.setLength(500.0);
		link3.setCapacity(7200);
		link3.setFreespeed(8.4);
		link3.setAllowedModes(modes);
		link3.setNumberOfLanes(1);
		
		link4.setLength(500.0);
		link4.setCapacity(7200);
		link4.setFreespeed(8.4);
		link4.setAllowedModes(modes);
		link4.setNumberOfLanes(1);
		
		link5.setLength(500.0);
		link5.setCapacity(7200);
		link5.setFreespeed(8.4);
		link5.setAllowedModes(modes);
		link5.setNumberOfLanes(1);
		
		link6.setLength(500.0);
		link6.setCapacity(7200);
		link6.setFreespeed(8.4);
		link6.setAllowedModes(modes);
		link6.setNumberOfLanes(1);
		
		link7.setLength(500.0);
		link7.setCapacity(7200);
		link7.setFreespeed(8.4);
		link7.setAllowedModes(modes);
		link7.setNumberOfLanes(1);
		
		link8.setLength(500.0);
		link8.setCapacity(7200);
		link8.setFreespeed(8.4);
		link8.setAllowedModes(modes);
		link8.setNumberOfLanes(1);
		
		link9.setLength(500.0);
		link9.setCapacity(7200);
		link9.setFreespeed(8.4);
		link9.setAllowedModes(modes);
		link9.setNumberOfLanes(1);
		
		link10.setLength(500.0);
		link10.setCapacity(7200);
		link10.setFreespeed(8.4);
		link10.setAllowedModes(modes);
		link10.setNumberOfLanes(1);
		
		link11.setLength(500.0);
		link11.setCapacity(7200);
		link11.setFreespeed(8.4);
		link11.setAllowedModes(modes);
		link11.setNumberOfLanes(1);
		
		link12.setLength(500.0);
		link12.setCapacity(7200);
		link12.setFreespeed(8.4);
		link12.setAllowedModes(modes);
		link12.setNumberOfLanes(1);
		
		network.addNode(node0);
		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);
		network.addNode(node4);
		network.addNode(node5);
		network.addNode(node6);

		network.addLink(link1);
		network.addLink(link2);
		network.addLink(link3);
		network.addLink(link4);
		network.addLink(link5);
		network.addLink(link6);
		network.addLink(link7);
		network.addLink(link8);
		network.addLink(link9);
		network.addLink(link10);
		network.addLink(link11);
		network.addLink(link12);
		
	}
	
	private void setPopulationTestInVehicleDelay01(Scenario scenario) {
		
		Population population = scenario.getPopulation();
        PopulationFactory popFactory = (PopulationFactory) scenario.getPopulation().getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();

		Coord coord1 = new Coord(500.0, 0.0);
		Coord coord2 = new Coord(1000.0, 0.0);
		Coord coord3 = new Coord(1500.0, 0.0);
		Coord coord4 = new Coord(2000.0, 0.0);
		Coord coord5 = new Coord(2500.0, 0.0);
		Coord coord6 = new Coord(3000.0, 0.0);
		
		Activity lastActLink2 = popFactory.createActivityFromCoord("w", coord2);
		Activity lastActLink3 = popFactory.createActivityFromCoord("w", coord3);
		Activity lastActLink4 = popFactory.createActivityFromCoord("w", coord4);
		Activity lastActLink5 = popFactory.createActivityFromCoord("w", coord5);
		Activity lastActLink6 = popFactory.createActivityFromCoord("w", coord6);
		
		Leg leg_1_2 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds12 = new ArrayList<Id<Link>>();
		NetworkRoute route12 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId2);
		route12.setLinkIds(linkId1, linkIds12, linkId2);
		leg_1_2.setRoute(route12);
		
		Leg leg_1_3 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds13 = new ArrayList<Id<Link>>();
		linkIds13.add(linkId2);
		NetworkRoute route13 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId3);
		route13.setLinkIds(linkId1, linkIds13, linkId3);
		leg_1_3.setRoute(route13);
		
		Leg leg_1_4 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds14 = new ArrayList<Id<Link>>();
		linkIds14.add(linkId2);
		linkIds14.add(linkId3);
		NetworkRoute route14 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId4);
		route14.setLinkIds(linkId1, linkIds14, linkId4);
		leg_1_4.setRoute(route14);
		
		Leg leg_1_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds15 = new ArrayList<Id<Link>>();
		linkIds15.add(linkId2);
		linkIds15.add(linkId3);
		linkIds15.add(linkId4);
		NetworkRoute route15 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId5);
		route15.setLinkIds(linkId1, linkIds15, linkId5);
		leg_1_5.setRoute(route15);
		
		Leg leg_1_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds16 = new ArrayList<Id<Link>>();
		linkIds16.add(linkId2);
		linkIds16.add(linkId3);
		linkIds16.add(linkId4);
		linkIds16.add(linkId5);
		NetworkRoute route16 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId6);
		route16.setLinkIds(linkId1, linkIds16, linkId6);
		leg_1_6.setRoute(route16);
		
		Leg leg_2_3 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds23 = new ArrayList<Id<Link>>();
		NetworkRoute route23 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId3);
		route23.setLinkIds(linkId2, linkIds23, linkId3);
		leg_2_3.setRoute(route23);
		
		Leg leg_2_4 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds24 = new ArrayList<Id<Link>>();
		linkIds24.add(linkId3);
		NetworkRoute route24 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId4);
		route24.setLinkIds(linkId2, linkIds24, linkId4);
		leg_2_4.setRoute(route24);
		
		Leg leg_2_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds25 = new ArrayList<Id<Link>>();
		linkIds25.add(linkId3);
		linkIds25.add(linkId4);
		NetworkRoute route25 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId5);
		route25.setLinkIds(linkId2, linkIds25, linkId5);
		leg_2_5.setRoute(route25);
		
		Leg leg_2_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds26 = new ArrayList<Id<Link>>();
		linkIds26.add(linkId3);
		linkIds26.add(linkId4);
		linkIds26.add(linkId5);
		NetworkRoute route26 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId6);
		route26.setLinkIds(linkId2, linkIds26, linkId6);
		leg_2_6.setRoute(route26);
		
		Leg leg_3_4 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds34 = new ArrayList<Id<Link>>();
		NetworkRoute route34 = (NetworkRoute) routeFactory.createRoute(linkId3, linkId4);
		route34.setLinkIds(linkId3, linkIds34, linkId4);
		leg_3_4.setRoute(route34);
		
		Leg leg_3_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds35 = new ArrayList<Id<Link>>();
		linkIds35.add(linkId4);
		NetworkRoute route35 = (NetworkRoute) routeFactory.createRoute(linkId3, linkId5);
		route35.setLinkIds(linkId3, linkIds35, linkId5);
		leg_3_5.setRoute(route35);
		
		Leg leg_3_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds36 = new ArrayList<Id<Link>>();
		linkIds36.add(linkId4);
		linkIds36.add(linkId5);
		NetworkRoute route36 = (NetworkRoute) routeFactory.createRoute(linkId3, linkId6);
		route36.setLinkIds(linkId3, linkIds36, linkId6);
		leg_3_6.setRoute(route36);
		
		Leg leg_4_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds45 = new ArrayList<Id<Link>>();
		NetworkRoute route45 = (NetworkRoute) routeFactory.createRoute(linkId4, linkId5);
		route45.setLinkIds(linkId4, linkIds45, linkId5);
		leg_4_5.setRoute(route45);
		
		Leg leg_4_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds46 = new ArrayList<Id<Link>>();
		linkIds46.add(linkId5);
		NetworkRoute route46 = (NetworkRoute) routeFactory.createRoute(linkId4, linkId6);
		route46.setLinkIds(linkId4, linkIds46, linkId6);
		leg_4_6.setRoute(route46);
		
		Leg leg_5_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds56 = new ArrayList<Id<Link>>();
		NetworkRoute route56 = (NetworkRoute) routeFactory.createRoute(linkId5, linkId6);
		route56.setLinkIds(linkId5, linkIds56, linkId6);
		leg_5_6.setRoute(route56);
		
		Person person1 = popFactory.createPerson(testAgent1);
		Plan plan1 = popFactory.createPlan();
		Activity act1 = popFactory.createActivityFromCoord("h", coord1);
		act1.setEndTime(99);
		plan1.addActivity(act1);
//		Leg leg1 = popFactory.createLeg("pt");
		plan1.addLeg(leg_1_6);
//		lastActLinky.getCoord().setXY(3000.0, 0.0);
		lastActLink6.setType("w");
		plan1.addActivity(lastActLink6);
		person1.addPlan(plan1);
		
		Person person2 = popFactory.createPerson(testAgent2);
		Plan plan2 = popFactory.createPlan();
		Activity act2 = popFactory.createActivityFromCoord("h", coord2);
		act2.setEndTime(99);
		plan2.addActivity(act2);
//		Leg leg1 = popFactory.createLeg("pt");
		plan2.addLeg(leg_2_3);
//		lastActLinky.getCoord().setXY(1500.0, 0.0);
		lastActLink3.setType("w");
		plan2.addActivity(lastActLink3);
		person2.addPlan(plan2);
		
		population.addPerson(person1);
		population.addPerson(person2);
		
	}
	
	private void fillScheduleTestInVehicleDelay01(MutableScenario scenario) {
		TransitSchedule schedule = scenario.getTransitSchedule();
		
		TransitScheduleFactory sf = schedule.getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();
		
		Id<TransitStopFacility> Stop1 = Id.create("Stop1Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop2 = Id.create("Stop2Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop3 = Id.create("Stop3Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop4 = Id.create("Stop4Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop5 = Id.create("Stop5Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop6 = Id.create("Stop6Id", TransitStopFacility.class);
		TransitStopFacility StopFacility1;
		TransitStopFacility StopFacility2;
		TransitStopFacility StopFacility3;
		TransitStopFacility StopFacility4;
		TransitStopFacility StopFacility5;
		TransitStopFacility StopFacility6;
		StopFacility1 = sf.createTransitStopFacility(Stop1, new Coord(500., 0.), true);
		StopFacility1.setLinkId(linkId1);
		StopFacility2 = sf.createTransitStopFacility(Stop2, new Coord(1000., 0.), true);
		StopFacility2.setLinkId(linkId2);
		StopFacility3 = sf.createTransitStopFacility(Stop3, new Coord(1500., 0.), true);
		StopFacility3.setLinkId(linkId3);
		StopFacility4 = sf.createTransitStopFacility(Stop4, new Coord(2000., 0.), true);
		StopFacility4.setLinkId(linkId4);
		StopFacility5 = sf.createTransitStopFacility(Stop5, new Coord(2500., 0.), true);
		StopFacility5.setLinkId(linkId5);
		StopFacility6 = sf.createTransitStopFacility(Stop6, new Coord(3000., 0.), true);
		StopFacility6.setLinkId(linkId6);
		
		TransitLine TransitLine1 = null;
		Id<TransitLine> Line1 = Id.create("line1Id", TransitLine.class);
		TransitLine1 = sf.createTransitLine(Line1);
		
		List<Id<TransitLine>> lineIds = new ArrayList<Id<TransitLine>>();
		lineIds.add(Line1);
		
		Id<TransitRoute> Route1 = Id.create("Route1Id", TransitRoute.class);
		NetworkRoute NetworkRoute1 = null;
		NetworkRoute1 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId6);
		List<Id<Link>> LinkIds = new ArrayList<Id<Link>>();
		LinkIds.add(linkId2);
		LinkIds.add(linkId3);
		LinkIds.add(linkId4);
		LinkIds.add(linkId5);
		NetworkRoute1.setLinkIds(linkId1, LinkIds, linkId6);
		
		TransitRouteStop TRStop1;
		TransitRouteStop TRStop2;
		TransitRouteStop TRStop3;
		TransitRouteStop TRStop4;
		TransitRouteStop TRStop5;
		TransitRouteStop TRStop6;
		TRStop1 = sf.createTransitRouteStop(StopFacility1, 0.0, 0.0);
		TRStop2 = sf.createTransitRouteStop(StopFacility2, 60.0, 60.0);
		TRStop3 = sf.createTransitRouteStop(StopFacility3, 120.0, 120.0);
		TRStop4 = sf.createTransitRouteStop(StopFacility4, 180.0, 180.0);
		TRStop5 = sf.createTransitRouteStop(StopFacility5, 240.0, 240.0);
		TRStop6 = sf.createTransitRouteStop(StopFacility6, 300.0, 300.0);
		
		List<TransitRouteStop> Stops = new ArrayList<TransitRouteStop>();
		Stops.add(TRStop1);
		Stops.add(TRStop2);
		Stops.add(TRStop3);
		Stops.add(TRStop4);
		Stops.add(TRStop5);
		Stops.add(TRStop6);
		
		TransitRoute TransitRoute1 = null;
		TransitRoute1 = sf.createTransitRoute(Route1, NetworkRoute1, Stops, "pt");
		
		Departure Departure1 = null;
		Id<Departure> DepartureId1 = Id.create("DepartureId_1", Departure.class);
		Departure1 = sf.createDeparture(DepartureId1, 200);
		Id<Vehicle> veh_1 = Id.create("veh_1", Vehicle.class);
		Departure1.setVehicleId(veh_1);
		
		TransitRoute1.addDeparture(Departure1);
		
		TransitLine1.addRoute(TransitRoute1);
		
		schedule.addTransitLine(TransitLine1);
		schedule.addStopFacility(StopFacility1);
		schedule.addStopFacility(StopFacility2);
		schedule.addStopFacility(StopFacility3);
		schedule.addStopFacility(StopFacility4);
		schedule.addStopFacility(StopFacility5);
		schedule.addStopFacility(StopFacility6);
		
	}
	
	private void fillVehicleTestInVehicleDelay01(MutableScenario scenario) {
		Vehicles veh = scenario.getTransitVehicles();

		Id<VehicleType> vehTypeId1 = Id.create("type_1", VehicleType.class);
		Id<Vehicle> vehId1 = Id.create("veh_1", Vehicle.class);
		VehicleType type = veh.getFactory().createVehicleType(vehTypeId1);
		VehicleCapacity cap = veh.getFactory().createVehicleCapacity();
		cap.setSeats(50);
		cap.setStandingRoom(0);
		type.setCapacity(cap);
		type.setLength(10);
		type.setAccessTime(1.0);
		type.setEgressTime(0.75);
		type.setDoorOperationMode(DoorOperationMode.serial);
		
		type.setMaximumVelocity(8.4);
		type.setPcuEquivalents(7.5);
		
		veh.addVehicleType(type); 
		
		Vehicle vehicle1 = veh.getFactory().createVehicle(vehId1, veh.getVehicleTypes().get(vehTypeId1));
		veh.addVehicle( vehicle1);
	}
	
	private void setPopulationTestInVehicleDelay02(Scenario scenario) {
		
		Population population = scenario.getPopulation();
        PopulationFactory popFactory = (PopulationFactory) scenario.getPopulation().getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();

		Coord coord1 = new Coord(500.0, 0.0);
		Coord coord2 = new Coord(1000.0, 0.0);
		Coord coord3 = new Coord(1500.0, 0.0);
		Coord coord4 = new Coord(2000.0, 0.0);
		Coord coord5 = new Coord(2500.0, 0.0);
		Coord coord6 = new Coord(3000.0, 0.0);
		
		Activity lastActLink2 = popFactory.createActivityFromCoord("w", coord2);
		Activity lastActLink3 = popFactory.createActivityFromCoord("w", coord3);
		Activity lastActLink4 = popFactory.createActivityFromCoord("w", coord4);
		Activity lastActLink5 = popFactory.createActivityFromCoord("w", coord5);
		Activity lastActLink6 = popFactory.createActivityFromCoord("w", coord6);
			
		Leg leg_1_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds16 = new ArrayList<Id<Link>>();
		linkIds16.add(linkId2);
		linkIds16.add(linkId3);
		linkIds16.add(linkId4);
		linkIds16.add(linkId5);
		NetworkRoute route16 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId6);
		route16.setLinkIds(linkId1, linkIds16, linkId6);
		leg_1_6.setRoute(route16);
		
		Leg leg_3_4 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds34 = new ArrayList<Id<Link>>();
		NetworkRoute route34 = (NetworkRoute) routeFactory.createRoute(linkId3, linkId4);
		route34.setLinkIds(linkId3, linkIds34, linkId4);
		leg_3_4.setRoute(route34);

		Leg leg_2_3 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds23 = new ArrayList<Id<Link>>();
		NetworkRoute route23 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId3);
		route23.setLinkIds(linkId2, linkIds23, linkId3);
		leg_2_3.setRoute(route23);
		
		Person person1 = popFactory.createPerson(testAgent1);
		Plan plan1 = popFactory.createPlan();
		Activity act1 = popFactory.createActivityFromCoord("h", coord1);
		act1.setEndTime(99);
		plan1.addActivity(act1);
//		Leg leg1 = popFactory.createLeg("pt");
		plan1.addLeg(leg_1_6);
//		lastActLink6.getCoord().setXY(3000.0, 0.0);
		lastActLink6.setType("w");
		plan1.addActivity(lastActLink6);
		person1.addPlan(plan1);
		
		Person person2 = popFactory.createPerson(testAgent2);
		Plan plan2 = popFactory.createPlan();
		Activity act2 = popFactory.createActivityFromCoord("h", coord2);
		act2.setEndTime(99);
		plan2.addActivity(act2);
//		Leg leg2 = popFactory.createLeg("pt");
		plan2.addLeg(leg_2_3);
//		lastActLink3.getCoord().setXY(1500.0, 0.0);
		lastActLink3.setType("w");
		plan2.addActivity(lastActLink3);
		person2.addPlan(plan2);
		
		Person person3 = popFactory.createPerson(testAgent3);
		Plan plan3 = popFactory.createPlan();
		Activity act3 = popFactory.createActivityFromCoord("h", coord3);
		act3.setEndTime(99);
		plan3.addActivity(act3);
//		Leg leg3 = popFactory.createLeg("pt");
		plan3.addLeg(leg_3_4);
//		lastActLink4.getCoord().setXY(2000.0, 0.0);
		lastActLink4.setType("w");
		plan3.addActivity(lastActLink4);
		person3.addPlan(plan3);
		
		population.addPerson(person1);
		population.addPerson(person2);
		population.addPerson(person3);
		
	}
		
	private void fillScheduleTestInVehicleDelay02(MutableScenario scenario) {
		TransitSchedule schedule = scenario.getTransitSchedule();
		
		TransitScheduleFactory sf = schedule.getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();
		
		Id<TransitStopFacility> Stop1 = Id.create("Stop1Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop2 = Id.create("Stop2Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop3 = Id.create("Stop3Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop4 = Id.create("Stop4Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop5 = Id.create("Stop5Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop6 = Id.create("Stop6Id", TransitStopFacility.class);
		TransitStopFacility StopFacility1;
		TransitStopFacility StopFacility2;
		TransitStopFacility StopFacility3;
		TransitStopFacility StopFacility4;
		TransitStopFacility StopFacility5;
		TransitStopFacility StopFacility6;
		StopFacility1 = sf.createTransitStopFacility(Stop1, new Coord(500., 0.), true);
		StopFacility1.setLinkId(linkId1);
		StopFacility2 = sf.createTransitStopFacility(Stop2, new Coord(1000., 0.), true);
		StopFacility2.setLinkId(linkId2);
		StopFacility3 = sf.createTransitStopFacility(Stop3, new Coord(1500., 0.), true);
		StopFacility3.setLinkId(linkId3);
		StopFacility4 = sf.createTransitStopFacility(Stop4, new Coord(2000., 0.), true);
		StopFacility4.setLinkId(linkId4);
		StopFacility5 = sf.createTransitStopFacility(Stop5, new Coord(2500., 0.), true);
		StopFacility5.setLinkId(linkId5);
		StopFacility6 = sf.createTransitStopFacility(Stop6, new Coord(3000., 0.), true);
		StopFacility6.setLinkId(linkId6);
		
		TransitLine TransitLine1 = null;
		Id<TransitLine> Line1 = Id.create("line1Id", TransitLine.class);
		TransitLine1 = sf.createTransitLine(Line1);
		
		List<Id<TransitLine>> lineIds = new ArrayList<Id<TransitLine>>();
		lineIds.add(Line1);
		
		Id<TransitRoute> Route1 = Id.create("Route1Id", TransitRoute.class);
		NetworkRoute NetworkRoute1 = null;
		NetworkRoute1 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId6);
		List<Id<Link>> LinkIds = new ArrayList<Id<Link>>();
		LinkIds.add(linkId2);
		LinkIds.add(linkId3);
		LinkIds.add(linkId4);
		LinkIds.add(linkId5);
		NetworkRoute1.setLinkIds(linkId1, LinkIds, linkId6);
		
		TransitRouteStop TRStop1;
		TransitRouteStop TRStop2;
		TransitRouteStop TRStop3;
		TransitRouteStop TRStop4;
		TransitRouteStop TRStop5;
		TransitRouteStop TRStop6;
		TRStop1 = sf.createTransitRouteStop(StopFacility1, 0.0, 0.0);
		TRStop2 = sf.createTransitRouteStop(StopFacility2, 60.0, 60.0);
		TRStop3 = sf.createTransitRouteStop(StopFacility3, 120.0, 120.0);
		TRStop4 = sf.createTransitRouteStop(StopFacility4, 180.0, 180.0);
		TRStop5 = sf.createTransitRouteStop(StopFacility5, 240.0, 240.0);
		TRStop6 = sf.createTransitRouteStop(StopFacility6, 300.0, 300.0);
		
		List<TransitRouteStop> Stops = new ArrayList<TransitRouteStop>();
		Stops.add(TRStop1);
		Stops.add(TRStop2);
		Stops.add(TRStop3);
		Stops.add(TRStop4);
		Stops.add(TRStop5);
		Stops.add(TRStop6);
		
		TransitRoute TransitRoute1 = null;
		TransitRoute1 = sf.createTransitRoute(Route1, NetworkRoute1, Stops, "pt");
		
		Departure Departure1 = null;
		Id<Departure> DepartureId1 = Id.create("DepartureId_1", Departure.class);
		Departure1 = sf.createDeparture(DepartureId1, 200);
		Id<Vehicle> veh_1 = Id.create("veh_1", Vehicle.class);
		Departure1.setVehicleId(veh_1);
		
		TransitRoute1.addDeparture(Departure1);
		
		TransitLine1.addRoute(TransitRoute1);
		
		schedule.addTransitLine(TransitLine1);
		schedule.addStopFacility(StopFacility1);
		schedule.addStopFacility(StopFacility2);
		schedule.addStopFacility(StopFacility3);
		schedule.addStopFacility(StopFacility4);
		schedule.addStopFacility(StopFacility5);
		schedule.addStopFacility(StopFacility6);
		
	}
	
	private void fillVehicleTestInVehicleDelay02(MutableScenario scenario) {
		Vehicles veh = scenario.getTransitVehicles();
	
		Id<VehicleType> vehTypeId1 = Id.create("type_1", VehicleType.class);
		Id<Vehicle> vehId1 = Id.create("veh_1", Vehicle.class);
	
		VehicleType type = veh.getFactory().createVehicleType(vehTypeId1);
		VehicleCapacity cap = veh.getFactory().createVehicleCapacity();
		cap.setSeats(50);
		cap.setStandingRoom(0);
		type.setCapacity(cap);
		type.setLength(10);
		type.setAccessTime(1.0);
		type.setEgressTime(0.75);
		type.setDoorOperationMode(DoorOperationMode.serial);
		
		type.setMaximumVelocity(8.4);
		type.setPcuEquivalents(7.5);
		
		veh.addVehicleType(type); 
		
		Vehicle vehicle1 = veh.getFactory().createVehicle(vehId1, veh.getVehicleTypes().get(vehTypeId1));
		veh.addVehicle( vehicle1);
		
	}
		
	private void setPopulationTestInVehicleDelay03(Scenario scenario) {
		
		Population population = scenario.getPopulation();
        PopulationFactory popFactory = (PopulationFactory) scenario.getPopulation().getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();

		Coord coord1 = new Coord(500.0, 0.0);
		Coord coord2 = new Coord(1000.0, 0.0);
		Coord coord3 = new Coord(1500.0, 0.0);
		Coord coord4 = new Coord(2000.0, 0.0);
		Coord coord5 = new Coord(2500.0, 0.0);
		Coord coord6 = new Coord(3000.0, 0.0);
		
		Activity lastActLink2 = popFactory.createActivityFromCoord("w", coord2);
		Activity lastActLink3 = popFactory.createActivityFromCoord("w", coord3);
		Activity lastActLink4 = popFactory.createActivityFromCoord("w", coord4);
		Activity lastActLink5 = popFactory.createActivityFromCoord("w", coord5);
		Activity lastActLink6 = popFactory.createActivityFromCoord("w", coord6);
		
		Leg leg_1_2 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds12 = new ArrayList<Id<Link>>();
		NetworkRoute route12 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId2);
		route12.setLinkIds(linkId1, linkIds12, linkId2);
		leg_1_2.setRoute(route12);
		
		Leg leg_1_3 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds13 = new ArrayList<Id<Link>>();
		linkIds13.add(linkId2);
		NetworkRoute route13 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId3);
		route13.setLinkIds(linkId1, linkIds13, linkId3);
		leg_1_3.setRoute(route13);
		
		Leg leg_1_4 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds14 = new ArrayList<Id<Link>>();
		linkIds14.add(linkId2);
		linkIds14.add(linkId3);
		NetworkRoute route14 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId4);
		route14.setLinkIds(linkId1, linkIds14, linkId4);
		leg_1_4.setRoute(route14);
		
		Leg leg_1_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds15 = new ArrayList<Id<Link>>();
		linkIds15.add(linkId2);
		linkIds15.add(linkId3);
		linkIds15.add(linkId4);
		NetworkRoute route15 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId5);
		route15.setLinkIds(linkId1, linkIds15, linkId5);
		leg_1_5.setRoute(route15);
		
		Leg leg_1_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds16 = new ArrayList<Id<Link>>();
		linkIds16.add(linkId2);
		linkIds16.add(linkId3);
		linkIds16.add(linkId4);
		linkIds16.add(linkId5);
		NetworkRoute route16 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId6);
		route16.setLinkIds(linkId1, linkIds16, linkId6);
		leg_1_6.setRoute(route16);
		
		Leg leg_2_3 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds23 = new ArrayList<Id<Link>>();
		NetworkRoute route23 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId3);
		route23.setLinkIds(linkId2, linkIds23, linkId3);
		leg_2_3.setRoute(route23);
		
		Leg leg_2_4 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds24 = new ArrayList<Id<Link>>();
		linkIds24.add(linkId3);
		NetworkRoute route24 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId4);
		route24.setLinkIds(linkId2, linkIds24, linkId4);
		leg_2_4.setRoute(route24);
		
		Leg leg_2_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds25 = new ArrayList<Id<Link>>();
		linkIds25.add(linkId3);
		linkIds25.add(linkId4);
		NetworkRoute route25 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId5);
		route25.setLinkIds(linkId2, linkIds25, linkId5);
		leg_2_5.setRoute(route25);
		
		Leg leg_2_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds26 = new ArrayList<Id<Link>>();
		linkIds26.add(linkId3);
		linkIds26.add(linkId4);
		linkIds26.add(linkId5);
		NetworkRoute route26 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId6);
		route26.setLinkIds(linkId2, linkIds26, linkId6);
		leg_2_6.setRoute(route26);
		
		Leg leg_3_4 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds34 = new ArrayList<Id<Link>>();
		NetworkRoute route34 = (NetworkRoute) routeFactory.createRoute(linkId3, linkId4);
		route34.setLinkIds(linkId3, linkIds34, linkId4);
		leg_3_4.setRoute(route34);
		
		Leg leg_3_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds35 = new ArrayList<Id<Link>>();
		linkIds35.add(linkId4);
		NetworkRoute route35 = (NetworkRoute) routeFactory.createRoute(linkId3, linkId5);
		route35.setLinkIds(linkId3, linkIds35, linkId5);
		leg_3_5.setRoute(route35);
		
		Leg leg_3_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds36 = new ArrayList<Id<Link>>();
		linkIds36.add(linkId4);
		linkIds36.add(linkId5);
		NetworkRoute route36 = (NetworkRoute) routeFactory.createRoute(linkId3, linkId6);
		route36.setLinkIds(linkId3, linkIds36, linkId6);
		leg_3_6.setRoute(route36);
		
		Leg leg_4_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds45 = new ArrayList<Id<Link>>();
		NetworkRoute route45 = (NetworkRoute) routeFactory.createRoute(linkId4, linkId5);
		route45.setLinkIds(linkId4, linkIds45, linkId5);
		leg_4_5.setRoute(route45);
		
		Leg leg_4_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds46 = new ArrayList<Id<Link>>();
		linkIds46.add(linkId5);
		NetworkRoute route46 = (NetworkRoute) routeFactory.createRoute(linkId4, linkId6);
		route46.setLinkIds(linkId4, linkIds46, linkId6);
		leg_4_6.setRoute(route46);
		
		Leg leg_5_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds56 = new ArrayList<Id<Link>>();
		NetworkRoute route56 = (NetworkRoute) routeFactory.createRoute(linkId5, linkId6);
		route56.setLinkIds(linkId5, linkIds56, linkId6);
		leg_5_6.setRoute(route56);
		
		Person person1 = popFactory.createPerson(testAgent1);
		Plan plan1 = popFactory.createPlan();
		Activity act1 = popFactory.createActivityFromCoord("h", coord1);
		act1.setEndTime(99);
		plan1.addActivity(act1);
//		Leg leg1 = popFactory.createLeg("pt");
		plan1.addLeg(leg_1_3);
//		lastActLink3.getCoord().setXY(1500.0, 0.0);
		lastActLink3.setType("w");
		plan1.addActivity(lastActLink3);
		person1.addPlan(plan1);

		Person person2 = popFactory.createPerson(testAgent2);
		Plan plan2 = popFactory.createPlan();
		Activity act2 = popFactory.createActivityFromCoord("h", coord1);
		act2.setEndTime(100);
		plan2.addActivity(act2);
//		Leg leg2 = popFactory.createLeg("pt");
		plan2.addLeg(leg_1_3);
//		lastActLink3.getCoord().setXY(1500.0, 0.0);
		lastActLink3.setType("w");
		plan2.addActivity(lastActLink3);
		person2.addPlan(plan2);
		
		Person person3 = popFactory.createPerson(testAgent3);
		Plan plan3 = popFactory.createPlan();
		Activity act3 = popFactory.createActivityFromCoord("h", coord2);
		act3.setEndTime(99);
		plan3.addActivity(act3);
//		Leg leg3 = popFactory.createLeg("pt");
		plan3.addLeg(leg_2_6);
//		lastActLink6.getCoord().setXY(3000.0, 0.0);
		lastActLink6.setType("w");
		plan3.addActivity(lastActLink6);
		person3.addPlan(plan3);
		
		Person person4 = popFactory.createPerson(testAgent4);
		Plan plan4 = popFactory.createPlan();
		Activity act4 = popFactory.createActivityFromCoord("h", coord4);
		act4.setEndTime(99);
		plan4.addActivity(act4);
//		Leg leg4 = popFactory.createLeg("pt");
		plan4.addLeg(leg_4_5);
//		lastActLink5.getCoord().setXY(2500.0, 0.0);
		lastActLink5.setType("w");
		plan4.addActivity(lastActLink5);
		person4.addPlan(plan4);
		
		Person person5 = popFactory.createPerson(testAgent5);
		Plan plan5 = popFactory.createPlan();
		Activity act5 = popFactory.createActivityFromCoord("h", coord4);
		act5.setEndTime(100);
		plan5.addActivity(act5);
//		Leg leg5 = popFactory.createLeg("pt");
		plan5.addLeg(leg_4_6);
//		lastActLink6.getCoord().setXY(3000.0, 0.0);
		lastActLink6.setType("w");
		plan5.addActivity(lastActLink6);
		person5.addPlan(plan5);
		
		population.addPerson(person1);
		population.addPerson(person2);
		population.addPerson(person3);
		population.addPerson(person4);
		population.addPerson(person5);
		
	}

	private void fillScheduleTestInVehicleDelay03(MutableScenario scenario) {
		TransitSchedule schedule = scenario.getTransitSchedule();
		
		TransitScheduleFactory sf = schedule.getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();
		
		Id<TransitStopFacility> Stop1 = Id.create("Stop1Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop2 = Id.create("Stop2Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop3 = Id.create("Stop3Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop4 = Id.create("Stop4Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop5 = Id.create("Stop5Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop6 = Id.create("Stop6Id", TransitStopFacility.class);
		TransitStopFacility StopFacility1;
		TransitStopFacility StopFacility2;
		TransitStopFacility StopFacility3;
		TransitStopFacility StopFacility4;
		TransitStopFacility StopFacility5;
		TransitStopFacility StopFacility6;
		StopFacility1 = sf.createTransitStopFacility(Stop1, new Coord(500., 0.), true);
		StopFacility1.setLinkId(linkId1);
		StopFacility2 = sf.createTransitStopFacility(Stop2, new Coord(1000., 0.), true);
		StopFacility2.setLinkId(linkId2);
		StopFacility3 = sf.createTransitStopFacility(Stop3, new Coord(1500., 0.), true);
		StopFacility3.setLinkId(linkId3);
		StopFacility4 = sf.createTransitStopFacility(Stop4, new Coord(2000., 0.), true);
		StopFacility4.setLinkId(linkId4);
		StopFacility5 = sf.createTransitStopFacility(Stop5, new Coord(2500., 0.), true);
		StopFacility5.setLinkId(linkId5);
		StopFacility6 = sf.createTransitStopFacility(Stop6, new Coord(3000., 0.), true);
		StopFacility6.setLinkId(linkId6);
		
		TransitLine TransitLine1 = null;
		Id<TransitLine> Line1 = Id.create("line1Id", TransitLine.class);
		TransitLine1 = sf.createTransitLine(Line1);
		
		List<Id<TransitLine>> lineIds = new ArrayList<Id<TransitLine>>();
		lineIds.add(Line1);
		
		Id<TransitRoute> Route1 = Id.create("Route1Id", TransitRoute.class);
		NetworkRoute NetworkRoute1 = null;
		NetworkRoute1 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId6);
		List<Id<Link>> LinkIds = new ArrayList<Id<Link>>();
		LinkIds.add(linkId2);
		LinkIds.add(linkId3);
		LinkIds.add(linkId4);
		LinkIds.add(linkId5);
		NetworkRoute1.setLinkIds(linkId1, LinkIds, linkId6);
		
		TransitRouteStop TRStop1;
		TransitRouteStop TRStop2;
		TransitRouteStop TRStop3;
		TransitRouteStop TRStop4;
		TransitRouteStop TRStop5;
		TransitRouteStop TRStop6;
		TRStop1 = sf.createTransitRouteStop(StopFacility1, 0.0, 0.0);
		TRStop2 = sf.createTransitRouteStop(StopFacility2, 60.0, 60.0);
		TRStop3 = sf.createTransitRouteStop(StopFacility3, 120.0, 120.0);
		TRStop4 = sf.createTransitRouteStop(StopFacility4, 180.0, 180.0);
		TRStop5 = sf.createTransitRouteStop(StopFacility5, 240.0, 240.0);
		TRStop6 = sf.createTransitRouteStop(StopFacility6, 300.0, 300.0);
		
		List<TransitRouteStop> Stops = new ArrayList<TransitRouteStop>();
		Stops.add(TRStop1);
		Stops.add(TRStop2);
		Stops.add(TRStop3);
		Stops.add(TRStop4);
		Stops.add(TRStop5);
		Stops.add(TRStop6);
		
		TransitRoute TransitRoute1 = null;
		TransitRoute1 = sf.createTransitRoute(Route1, NetworkRoute1, Stops, "pt");
		
		Departure Departure1 = null;
		Id<Departure> DepartureId1 = Id.create("DepartureId_1", Departure.class);
		Departure1 = sf.createDeparture(DepartureId1, 200);
		Id<Vehicle> veh_1 = Id.create("veh_1", Vehicle.class);
		Departure1.setVehicleId(veh_1);
		
		TransitRoute1.addDeparture(Departure1);
		
		TransitLine1.addRoute(TransitRoute1);
		
		schedule.addTransitLine(TransitLine1);
		schedule.addStopFacility(StopFacility1);
		schedule.addStopFacility(StopFacility2);
		schedule.addStopFacility(StopFacility3);
		schedule.addStopFacility(StopFacility4);
		schedule.addStopFacility(StopFacility5);
		schedule.addStopFacility(StopFacility6);
		
	}
	
	private void fillVehicleTestInVehicleDelay03(MutableScenario scenario) {
		Vehicles veh = scenario.getTransitVehicles();

		Id<VehicleType> vehTypeId1 = Id.create("type_1", VehicleType.class);
		Id<Vehicle> vehId1 = Id.create("veh_1", Vehicle.class);

		VehicleType type = veh.getFactory().createVehicleType(vehTypeId1);
		VehicleCapacity cap = veh.getFactory().createVehicleCapacity();
		cap.setSeats(50);
		cap.setStandingRoom(0);
		type.setCapacity(cap);
		type.setLength(10);
		type.setAccessTime(1.0);
		type.setEgressTime(0.75);
		type.setDoorOperationMode(DoorOperationMode.serial);
		
		type.setMaximumVelocity(8.4);
		type.setPcuEquivalents(7.5);
		
		veh.addVehicleType(type); 
		
		Vehicle vehicle1 = veh.getFactory().createVehicle(vehId1, veh.getVehicleTypes().get(vehTypeId1));
		veh.addVehicle( vehicle1);
	}
	
	private void setPopulationTestWaitingDelay01(Scenario scenario) {
		
		Population population = scenario.getPopulation();
        PopulationFactory popFactory = (PopulationFactory) scenario.getPopulation().getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();

		Coord coord1 = new Coord(500.0, 0.0);
		Coord coord2 = new Coord(1000.0, 0.0);
		Coord coord3 = new Coord(1500.0, 0.0);
		Coord coord4 = new Coord(2000.0, 0.0);
		Coord coord5 = new Coord(2500.0, 0.0);
		Coord coord6 = new Coord(3000.0, 0.0);
		
		Activity lastActLink2 = popFactory.createActivityFromCoord("w", coord2);
		Activity lastActLink3 = popFactory.createActivityFromCoord("w", coord3);
		Activity lastActLink4 = popFactory.createActivityFromCoord("w", coord4);
		Activity lastActLink5 = popFactory.createActivityFromCoord("w", coord5);
		Activity lastActLink6 = popFactory.createActivityFromCoord("w", coord6);
		
		Leg leg_1_2 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds12 = new ArrayList<Id<Link>>();
		NetworkRoute route12 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId2);
		route12.setLinkIds(linkId1, linkIds12, linkId2);
		leg_1_2.setRoute(route12);
		
		Leg leg_1_3 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds13 = new ArrayList<Id<Link>>();
		linkIds13.add(linkId2);
		NetworkRoute route13 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId3);
		route13.setLinkIds(linkId1, linkIds13, linkId3);
		leg_1_3.setRoute(route13);
		
		Leg leg_1_4 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds14 = new ArrayList<Id<Link>>();
		linkIds14.add(linkId2);
		linkIds14.add(linkId3);
		NetworkRoute route14 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId4);
		route14.setLinkIds(linkId1, linkIds14, linkId4);
		leg_1_4.setRoute(route14);
		
		Leg leg_1_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds15 = new ArrayList<Id<Link>>();
		linkIds15.add(linkId2);
		linkIds15.add(linkId3);
		linkIds15.add(linkId4);
		NetworkRoute route15 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId5);
		route15.setLinkIds(linkId1, linkIds15, linkId5);
		leg_1_5.setRoute(route15);
		
		Leg leg_1_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds16 = new ArrayList<Id<Link>>();
		linkIds16.add(linkId2);
		linkIds16.add(linkId3);
		linkIds16.add(linkId4);
		linkIds16.add(linkId5);
		NetworkRoute route16 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId6);
		route16.setLinkIds(linkId1, linkIds16, linkId6);
		leg_1_6.setRoute(route16);
		
		Leg leg_2_3 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds23 = new ArrayList<Id<Link>>();
		NetworkRoute route23 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId3);
		route23.setLinkIds(linkId2, linkIds23, linkId3);
		leg_2_3.setRoute(route23);
		
		Leg leg_2_4 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds24 = new ArrayList<Id<Link>>();
		linkIds24.add(linkId3);
		NetworkRoute route24 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId4);
		route24.setLinkIds(linkId2, linkIds24, linkId4);
		leg_2_4.setRoute(route24);
		
		Leg leg_2_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds25 = new ArrayList<Id<Link>>();
		linkIds25.add(linkId3);
		linkIds25.add(linkId4);
		NetworkRoute route25 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId5);
		route25.setLinkIds(linkId2, linkIds25, linkId5);
		leg_2_5.setRoute(route25);
		
		Leg leg_2_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds26 = new ArrayList<Id<Link>>();
		linkIds26.add(linkId3);
		linkIds26.add(linkId4);
		linkIds26.add(linkId5);
		NetworkRoute route26 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId6);
		route26.setLinkIds(linkId2, linkIds26, linkId6);
		leg_2_6.setRoute(route26);
		
		Leg leg_3_4 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds34 = new ArrayList<Id<Link>>();
		NetworkRoute route34 = (NetworkRoute) routeFactory.createRoute(linkId3, linkId4);
		route34.setLinkIds(linkId3, linkIds34, linkId4);
		leg_3_4.setRoute(route34);
		
		Leg leg_3_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds35 = new ArrayList<Id<Link>>();
		linkIds35.add(linkId4);
		NetworkRoute route35 = (NetworkRoute) routeFactory.createRoute(linkId3, linkId5);
		route35.setLinkIds(linkId3, linkIds35, linkId5);
		leg_3_5.setRoute(route35);
		
		Leg leg_3_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds36 = new ArrayList<Id<Link>>();
		linkIds36.add(linkId4);
		linkIds36.add(linkId5);
		NetworkRoute route36 = (NetworkRoute) routeFactory.createRoute(linkId3, linkId6);
		route36.setLinkIds(linkId3, linkIds36, linkId6);
		leg_3_6.setRoute(route36);
		
		Leg leg_4_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds45 = new ArrayList<Id<Link>>();
		NetworkRoute route45 = (NetworkRoute) routeFactory.createRoute(linkId4, linkId5);
		route45.setLinkIds(linkId4, linkIds45, linkId5);
		leg_4_5.setRoute(route45);
		
		Leg leg_4_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds46 = new ArrayList<Id<Link>>();
		linkIds46.add(linkId5);
		NetworkRoute route46 = (NetworkRoute) routeFactory.createRoute(linkId4, linkId6);
		route46.setLinkIds(linkId4, linkIds46, linkId6);
		leg_4_6.setRoute(route46);
		
		Leg leg_5_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds56 = new ArrayList<Id<Link>>();
		NetworkRoute route56 = (NetworkRoute) routeFactory.createRoute(linkId5, linkId6);
		route56.setLinkIds(linkId5, linkIds56, linkId6);
		leg_5_6.setRoute(route56);
		
		Person person1 = popFactory.createPerson(testAgent1);
		Plan plan1 = popFactory.createPlan();
		Activity act1 = popFactory.createActivityFromCoord("h", coord2);
		act1.setEndTime(99);
		plan1.addActivity(act1);
	//	Leg leg1 = popFactory.createLeg("pt");
		plan1.addLeg(leg_2_6);
	//	lastActLink6.getCoord().setXY(3000.0, 0.0);
		lastActLink6.setType("w");
		plan1.addActivity(lastActLink6);
		person1.addPlan(plan1);
		
		Person person2 = popFactory.createPerson(testAgent2);
		Plan plan2 = popFactory.createPlan();
		Activity act2 = popFactory.createActivityFromCoord("h", coord3);
		act2.setEndTime(99);
		plan2.addActivity(act2);
	//	Leg leg2 = popFactory.createLeg("pt");
		plan2.addLeg(leg_3_4);
	//	lastActLink4.getCoord().setXY(2000.0, 0.0);
		lastActLink4.setType("w");
		plan2.addActivity(lastActLink4);
		person2.addPlan(plan2);
		
		population.addPerson(person1);
		population.addPerson(person2);
	
	}
	
	private void fillScheduleTestWaitingDelay01(MutableScenario scenario) {
		TransitSchedule schedule = scenario.getTransitSchedule();
		
		TransitScheduleFactory sf = schedule.getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();
		
		Id<TransitStopFacility> Stop1 = Id.create("Stop1Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop2 = Id.create("Stop2Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop3 = Id.create("Stop3Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop4 = Id.create("Stop4Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop5 = Id.create("Stop5Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop6 = Id.create("Stop6Id", TransitStopFacility.class);
		TransitStopFacility StopFacility1;
		TransitStopFacility StopFacility2;
		TransitStopFacility StopFacility3;
		TransitStopFacility StopFacility4;
		TransitStopFacility StopFacility5;
		TransitStopFacility StopFacility6;
		StopFacility1 = sf.createTransitStopFacility(Stop1, new Coord(500., 0.), true);
		StopFacility1.setLinkId(linkId1);
		StopFacility2 = sf.createTransitStopFacility(Stop2, new Coord(1000., 0.), true);
		StopFacility2.setLinkId(linkId2);
		StopFacility3 = sf.createTransitStopFacility(Stop3, new Coord(1500., 0.), true);
		StopFacility3.setLinkId(linkId3);
		StopFacility4 = sf.createTransitStopFacility(Stop4, new Coord(2000., 0.), true);
		StopFacility4.setLinkId(linkId4);
		StopFacility5 = sf.createTransitStopFacility(Stop5, new Coord(2500., 0.), true);
		StopFacility5.setLinkId(linkId5);
		StopFacility6 = sf.createTransitStopFacility(Stop6, new Coord(3000., 0.), true);
		StopFacility6.setLinkId(linkId6);
		
		TransitLine TransitLine1 = null;
		Id<TransitLine> Line1 = Id.create("line1Id", TransitLine.class);
		TransitLine1 = sf.createTransitLine(Line1);
		
		List<Id<TransitLine>> lineIds = new ArrayList<Id<TransitLine>>();
		lineIds.add(Line1);
		
		Id<TransitRoute> Route1 = Id.create("Route1Id", TransitRoute.class);
		NetworkRoute NetworkRoute1 = null;
		NetworkRoute1 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId6);
		List<Id<Link>> LinkIds = new ArrayList<Id<Link>>();
		LinkIds.add(linkId2);
		LinkIds.add(linkId3);
		LinkIds.add(linkId4);
		LinkIds.add(linkId5);
		NetworkRoute1.setLinkIds(linkId1, LinkIds, linkId6);
		
		TransitRouteStop TRStop1;
		TransitRouteStop TRStop2;
		TransitRouteStop TRStop3;
		TransitRouteStop TRStop4;
		TransitRouteStop TRStop5;
		TransitRouteStop TRStop6;
		TRStop1 = sf.createTransitRouteStop(StopFacility1, 0.0, 0.0);
		TRStop2 = sf.createTransitRouteStop(StopFacility2, 60.0, 60.0);
		TRStop3 = sf.createTransitRouteStop(StopFacility3, 120.0, 120.0);
		TRStop4 = sf.createTransitRouteStop(StopFacility4, 180.0, 180.0);
		TRStop5 = sf.createTransitRouteStop(StopFacility5, 240.0, 240.0);
		TRStop6 = sf.createTransitRouteStop(StopFacility6, 300.0, 300.0);
		
		List<TransitRouteStop> Stops = new ArrayList<TransitRouteStop>();
		Stops.add(TRStop1);
		Stops.add(TRStop2);
		Stops.add(TRStop3);
		Stops.add(TRStop4);
		Stops.add(TRStop5);
		Stops.add(TRStop6);
		
		TransitRoute TransitRoute1 = null;
		TransitRoute1 = sf.createTransitRoute(Route1, NetworkRoute1, Stops, "pt");
		
		Departure Departure1 = null;
		Id<Departure> DepartureId1 = Id.create("DepartureId_1", Departure.class);
		Departure1 = sf.createDeparture(DepartureId1, 200);
		Id<Vehicle> veh_1 = Id.create("veh_1", Vehicle.class);
		Departure1.setVehicleId(veh_1);
		
		TransitRoute1.addDeparture(Departure1);
		
		TransitLine1.addRoute(TransitRoute1);
		
		schedule.addTransitLine(TransitLine1);
		schedule.addStopFacility(StopFacility1);
		schedule.addStopFacility(StopFacility2);
		schedule.addStopFacility(StopFacility3);
		schedule.addStopFacility(StopFacility4);
		schedule.addStopFacility(StopFacility5);
		schedule.addStopFacility(StopFacility6);
		
	}
	
	private void fillVehicleTestWaitingDelay01(MutableScenario scenario) {
		Vehicles veh = scenario.getTransitVehicles();

		Id<VehicleType> vehTypeId1 = Id.create("type_1", VehicleType.class);
		Id<Vehicle> vehId1 = Id.create("veh_1", Vehicle.class);

		VehicleType type = veh.getFactory().createVehicleType(vehTypeId1);
		VehicleCapacity cap = veh.getFactory().createVehicleCapacity();
		cap.setSeats(50);
		cap.setStandingRoom(0);
		type.setCapacity(cap);
		type.setLength(10);
		type.setAccessTime(1.0);
		type.setEgressTime(0.75);
		type.setDoorOperationMode(DoorOperationMode.serial);
		
		type.setMaximumVelocity(8.4);
		type.setPcuEquivalents(7.5);
		
		veh.addVehicleType(type); 
		
		Vehicle vehicle1 = veh.getFactory().createVehicle(vehId1, veh.getVehicleTypes().get(vehTypeId1));
		veh.addVehicle( vehicle1);
	}
	
	private void setPopulationTestWaitingDelay02(Scenario scenario) {
		
		Population population = scenario.getPopulation();
        PopulationFactory popFactory = (PopulationFactory) scenario.getPopulation().getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();

		Coord coord1 = new Coord(500.0, 0.0);
		Coord coord2 = new Coord(1000.0, 0.0);
		Coord coord3 = new Coord(1500.0, 0.0);
		Coord coord4 = new Coord(2000.0, 0.0);
		Coord coord5 = new Coord(2500.0, 0.0);
		Coord coord6 = new Coord(3000.0, 0.0);
		
		Activity lastActLink2 = popFactory.createActivityFromCoord("w", coord2);
		Activity lastActLink3 = popFactory.createActivityFromCoord("w", coord3);
		Activity lastActLink4 = popFactory.createActivityFromCoord("w", coord4);
		Activity lastActLink5 = popFactory.createActivityFromCoord("w", coord5);
		Activity lastActLink6 = popFactory.createActivityFromCoord("w", coord6);
		
		Leg leg_1_2 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds12 = new ArrayList<Id<Link>>();
		NetworkRoute route12 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId2);
		route12.setLinkIds(linkId1, linkIds12, linkId2);
		leg_1_2.setRoute(route12);
		
		Leg leg_1_3 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds13 = new ArrayList<Id<Link>>();
		linkIds13.add(linkId2);
		NetworkRoute route13 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId3);
		route13.setLinkIds(linkId1, linkIds13, linkId3);
		leg_1_3.setRoute(route13);
		
		Leg leg_1_4 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds14 = new ArrayList<Id<Link>>();
		linkIds14.add(linkId2);
		linkIds14.add(linkId3);
		NetworkRoute route14 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId4);
		route14.setLinkIds(linkId1, linkIds14, linkId4);
		leg_1_4.setRoute(route14);
		
		Leg leg_1_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds15 = new ArrayList<Id<Link>>();
		linkIds15.add(linkId2);
		linkIds15.add(linkId3);
		linkIds15.add(linkId4);
		NetworkRoute route15 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId5);
		route15.setLinkIds(linkId1, linkIds15, linkId5);
		leg_1_5.setRoute(route15);
		
		Leg leg_1_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds16 = new ArrayList<Id<Link>>();
		linkIds16.add(linkId2);
		linkIds16.add(linkId3);
		linkIds16.add(linkId4);
		linkIds16.add(linkId5);
		NetworkRoute route16 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId6);
		route16.setLinkIds(linkId1, linkIds16, linkId6);
		leg_1_6.setRoute(route16);
		
		Leg leg_2_3 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds23 = new ArrayList<Id<Link>>();
		NetworkRoute route23 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId3);
		route23.setLinkIds(linkId2, linkIds23, linkId3);
		leg_2_3.setRoute(route23);
		
		Leg leg_2_4 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds24 = new ArrayList<Id<Link>>();
		linkIds24.add(linkId3);
		NetworkRoute route24 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId4);
		route24.setLinkIds(linkId2, linkIds24, linkId4);
		leg_2_4.setRoute(route24);
		
		Leg leg_2_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds25 = new ArrayList<Id<Link>>();
		linkIds25.add(linkId3);
		linkIds25.add(linkId4);
		NetworkRoute route25 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId5);
		route25.setLinkIds(linkId2, linkIds25, linkId5);
		leg_2_5.setRoute(route25);
		
		Leg leg_2_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds26 = new ArrayList<Id<Link>>();
		linkIds26.add(linkId3);
		linkIds26.add(linkId4);
		linkIds26.add(linkId5);
		NetworkRoute route26 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId6);
		route26.setLinkIds(linkId2, linkIds26, linkId6);
		leg_2_6.setRoute(route26);
		
		Leg leg_3_4 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds34 = new ArrayList<Id<Link>>();
		NetworkRoute route34 = (NetworkRoute) routeFactory.createRoute(linkId3, linkId4);
		route34.setLinkIds(linkId3, linkIds34, linkId4);
		leg_3_4.setRoute(route34);
		
		Leg leg_3_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds35 = new ArrayList<Id<Link>>();
		linkIds35.add(linkId4);
		NetworkRoute route35 = (NetworkRoute) routeFactory.createRoute(linkId3, linkId5);
		route35.setLinkIds(linkId3, linkIds35, linkId5);
		leg_3_5.setRoute(route35);
		
		Leg leg_3_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds36 = new ArrayList<Id<Link>>();
		linkIds36.add(linkId4);
		linkIds36.add(linkId5);
		NetworkRoute route36 = (NetworkRoute) routeFactory.createRoute(linkId3, linkId6);
		route36.setLinkIds(linkId3, linkIds36, linkId6);
		leg_3_6.setRoute(route36);
		
		Leg leg_4_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds45 = new ArrayList<Id<Link>>();
		NetworkRoute route45 = (NetworkRoute) routeFactory.createRoute(linkId4, linkId5);
		route45.setLinkIds(linkId4, linkIds45, linkId5);
		leg_4_5.setRoute(route45);
		
		Leg leg_4_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds46 = new ArrayList<Id<Link>>();
		linkIds46.add(linkId5);
		NetworkRoute route46 = (NetworkRoute) routeFactory.createRoute(linkId4, linkId6);
		route46.setLinkIds(linkId4, linkIds46, linkId6);
		leg_4_6.setRoute(route46);
		
		Leg leg_5_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds56 = new ArrayList<Id<Link>>();
		NetworkRoute route56 = (NetworkRoute) routeFactory.createRoute(linkId5, linkId6);
		route56.setLinkIds(linkId5, linkIds56, linkId6);
		leg_5_6.setRoute(route56);
		
		Person person1 = popFactory.createPerson(testAgent1);
		Plan plan1 = popFactory.createPlan();
		Activity act1 = popFactory.createActivityFromCoord("h", coord2);
		act1.setEndTime(99);
		plan1.addActivity(act1);
	//	Leg leg1 = popFactory.createLeg("pt");
		plan1.addLeg(leg_2_4);
	//	lastActLink4.getCoord().setXY(2000.0, 0.0);
		lastActLink4.setType("w");
		plan1.addActivity(lastActLink4);
		person1.addPlan(plan1);
		
		Person person2 = popFactory.createPerson(testAgent2);
		Plan plan2 = popFactory.createPlan();
		Activity act2 = popFactory.createActivityFromCoord("h", coord3);
		act2.setEndTime(99);
		plan2.addActivity(act2);
	//	Leg leg2 = popFactory.createLeg("pt");
		plan2.addLeg(leg_3_5);
	//	lastActLink5.getCoord().setXY(2500.0, 0.0);
		lastActLink5.setType("w");
		plan2.addActivity(lastActLink5);
		person2.addPlan(plan2);
		
		population.addPerson(person1);
		population.addPerson(person2);
		
	}
	
	private void fillScheduleTestWaitingDelay02(MutableScenario scenario) {
		TransitSchedule schedule = scenario.getTransitSchedule();
		
		TransitScheduleFactory sf = schedule.getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();
		
		Id<TransitStopFacility> Stop1 = Id.create("Stop1Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop2 = Id.create("Stop2Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop3 = Id.create("Stop3Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop4 = Id.create("Stop4Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop5 = Id.create("Stop5Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop6 = Id.create("Stop6Id", TransitStopFacility.class);
		TransitStopFacility StopFacility1;
		TransitStopFacility StopFacility2;
		TransitStopFacility StopFacility3;
		TransitStopFacility StopFacility4;
		TransitStopFacility StopFacility5;
		TransitStopFacility StopFacility6;
		StopFacility1 = sf.createTransitStopFacility(Stop1, new Coord(500., 0.), true);
		StopFacility1.setLinkId(linkId1);
		StopFacility2 = sf.createTransitStopFacility(Stop2, new Coord(1000., 0.), true);
		StopFacility2.setLinkId(linkId2);
		StopFacility3 = sf.createTransitStopFacility(Stop3, new Coord(1500., 0.), true);
		StopFacility3.setLinkId(linkId3);
		StopFacility4 = sf.createTransitStopFacility(Stop4, new Coord(2000., 0.), true);
		StopFacility4.setLinkId(linkId4);
		StopFacility5 = sf.createTransitStopFacility(Stop5, new Coord(2500., 0.), true);
		StopFacility5.setLinkId(linkId5);
		StopFacility6 = sf.createTransitStopFacility(Stop6, new Coord(3000., 0.), true);
		StopFacility6.setLinkId(linkId6);
		
		TransitLine TransitLine1 = null;
		Id<TransitLine> Line1 = Id.create("line1Id", TransitLine.class);
		TransitLine1 = sf.createTransitLine(Line1);
		
		List<Id<TransitLine>> lineIds = new ArrayList<Id<TransitLine>>();
		lineIds.add(Line1);
		
		Id<TransitRoute> Route1 = Id.create("Route1Id", TransitRoute.class);
		NetworkRoute NetworkRoute1 = null;
		NetworkRoute1 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId6);
		List<Id<Link>> LinkIds = new ArrayList<Id<Link>>();
		LinkIds.add(linkId2);
		LinkIds.add(linkId3);
		LinkIds.add(linkId4);
		LinkIds.add(linkId5);
		NetworkRoute1.setLinkIds(linkId1, LinkIds, linkId6);
		
		TransitRouteStop TRStop1;
		TransitRouteStop TRStop2;
		TransitRouteStop TRStop3;
		TransitRouteStop TRStop4;
		TransitRouteStop TRStop5;
		TransitRouteStop TRStop6;
		TRStop1 = sf.createTransitRouteStop(StopFacility1, 0.0, 0.0);
		TRStop2 = sf.createTransitRouteStop(StopFacility2, 60.0, 60.0);
		TRStop3 = sf.createTransitRouteStop(StopFacility3, 120.0, 120.0);
		TRStop4 = sf.createTransitRouteStop(StopFacility4, 180.0, 180.0);
		TRStop5 = sf.createTransitRouteStop(StopFacility5, 240.0, 240.0);
		TRStop6 = sf.createTransitRouteStop(StopFacility6, 300.0, 300.0);
		
		List<TransitRouteStop> Stops = new ArrayList<TransitRouteStop>();
		Stops.add(TRStop1);
		Stops.add(TRStop2);
		Stops.add(TRStop3);
		Stops.add(TRStop4);
		Stops.add(TRStop5);
		Stops.add(TRStop6);
		
		TransitRoute TransitRoute1 = null;
		TransitRoute1 = sf.createTransitRoute(Route1, NetworkRoute1, Stops, "pt");
		
		Departure Departure1 = null;
		Id<Departure> DepartureId1 = Id.create("DepartureId_1", Departure.class);
		Departure1 = sf.createDeparture(DepartureId1, 200);
		Id<Vehicle> veh_1 = Id.create("veh_1", Vehicle.class);
		Departure1.setVehicleId(veh_1);
		
		TransitRoute1.addDeparture(Departure1);
		
		TransitLine1.addRoute(TransitRoute1);
		
		schedule.addTransitLine(TransitLine1);
		schedule.addStopFacility(StopFacility1);
		schedule.addStopFacility(StopFacility2);
		schedule.addStopFacility(StopFacility3);
		schedule.addStopFacility(StopFacility4);
		schedule.addStopFacility(StopFacility5);
		schedule.addStopFacility(StopFacility6);
		
	}
	
	private void fillVehicleTestWaitingDelay02(MutableScenario scenario) {
		Vehicles veh = scenario.getTransitVehicles();

		Id<VehicleType> vehTypeId1 = Id.create("type_1", VehicleType.class);
		Id<Vehicle> vehId1 = Id.create("veh_1", Vehicle.class);

		VehicleType type = veh.getFactory().createVehicleType(vehTypeId1);
		VehicleCapacity cap = veh.getFactory().createVehicleCapacity();
		cap.setSeats(50);
		cap.setStandingRoom(0);
		type.setCapacity(cap);
		type.setLength(10);
		type.setAccessTime(1.0);
		type.setEgressTime(0.75);
		type.setDoorOperationMode(DoorOperationMode.serial);
		
		type.setMaximumVelocity(8.4);
		type.setPcuEquivalents(7.5);
		
		veh.addVehicleType(type); 
		
		Vehicle vehicle1 = veh.getFactory().createVehicle(vehId1, veh.getVehicleTypes().get(vehTypeId1));
		veh.addVehicle( vehicle1);
	}
	
	private void setPopulationTestWaitingDelay03(Scenario scenario) {
		
		Population population = scenario.getPopulation();
        PopulationFactory popFactory = (PopulationFactory) scenario.getPopulation().getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();

		Coord coord1 = new Coord(500.0, 0.0);
		Coord coord2 = new Coord(1000.0, 0.0);
		Coord coord3 = new Coord(1500.0, 0.0);
		Coord coord4 = new Coord(2000.0, 0.0);
		Coord coord5 = new Coord(2500.0, 0.0);
		Coord coord6 = new Coord(3000.0, 0.0);
		
		Activity lastActLink2 = popFactory.createActivityFromCoord("w", coord2);
		Activity lastActLink3 = popFactory.createActivityFromCoord("w", coord3);
		Activity lastActLink4 = popFactory.createActivityFromCoord("w", coord4);
		Activity lastActLink5 = popFactory.createActivityFromCoord("w", coord5);
		Activity lastActLink6 = popFactory.createActivityFromCoord("w", coord6);
		
		Leg leg_1_2 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds12 = new ArrayList<Id<Link>>();
		NetworkRoute route12 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId2);
		route12.setLinkIds(linkId1, linkIds12, linkId2);
		leg_1_2.setRoute(route12);
		
		Leg leg_1_3 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds13 = new ArrayList<Id<Link>>();
		linkIds13.add(linkId2);
		NetworkRoute route13 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId3);
		route13.setLinkIds(linkId1, linkIds13, linkId3);
		leg_1_3.setRoute(route13);
		
		Leg leg_1_4 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds14 = new ArrayList<Id<Link>>();
		linkIds14.add(linkId2);
		linkIds14.add(linkId3);
		NetworkRoute route14 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId4);
		route14.setLinkIds(linkId1, linkIds14, linkId4);
		leg_1_4.setRoute(route14);
		
		Leg leg_1_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds15 = new ArrayList<Id<Link>>();
		linkIds15.add(linkId2);
		linkIds15.add(linkId3);
		linkIds15.add(linkId4);
		NetworkRoute route15 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId5);
		route15.setLinkIds(linkId1, linkIds15, linkId5);
		leg_1_5.setRoute(route15);
		
		Leg leg_1_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds16 = new ArrayList<Id<Link>>();
		linkIds16.add(linkId2);
		linkIds16.add(linkId3);
		linkIds16.add(linkId4);
		linkIds16.add(linkId5);
		NetworkRoute route16 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId6);
		route16.setLinkIds(linkId1, linkIds16, linkId6);
		leg_1_6.setRoute(route16);
		
		Leg leg_2_3 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds23 = new ArrayList<Id<Link>>();
		NetworkRoute route23 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId3);
		route23.setLinkIds(linkId2, linkIds23, linkId3);
		leg_2_3.setRoute(route23);
		
		Leg leg_2_4 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds24 = new ArrayList<Id<Link>>();
		linkIds24.add(linkId3);
		NetworkRoute route24 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId4);
		route24.setLinkIds(linkId2, linkIds24, linkId4);
		leg_2_4.setRoute(route24);
		
		Leg leg_2_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds25 = new ArrayList<Id<Link>>();
		linkIds25.add(linkId3);
		linkIds25.add(linkId4);
		NetworkRoute route25 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId5);
		route25.setLinkIds(linkId2, linkIds25, linkId5);
		leg_2_5.setRoute(route25);
		
		Leg leg_2_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds26 = new ArrayList<Id<Link>>();
		linkIds26.add(linkId3);
		linkIds26.add(linkId4);
		linkIds26.add(linkId5);
		NetworkRoute route26 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId6);
		route26.setLinkIds(linkId2, linkIds26, linkId6);
		leg_2_6.setRoute(route26);
		
		Leg leg_3_4 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds34 = new ArrayList<Id<Link>>();
		NetworkRoute route34 = (NetworkRoute) routeFactory.createRoute(linkId3, linkId4);
		route34.setLinkIds(linkId3, linkIds34, linkId4);
		leg_3_4.setRoute(route34);
		
		Leg leg_3_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds35 = new ArrayList<Id<Link>>();
		linkIds35.add(linkId4);
		NetworkRoute route35 = (NetworkRoute) routeFactory.createRoute(linkId3, linkId5);
		route35.setLinkIds(linkId3, linkIds35, linkId5);
		leg_3_5.setRoute(route35);
		
		Leg leg_3_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds36 = new ArrayList<Id<Link>>();
		linkIds36.add(linkId4);
		linkIds36.add(linkId5);
		NetworkRoute route36 = (NetworkRoute) routeFactory.createRoute(linkId3, linkId6);
		route36.setLinkIds(linkId3, linkIds36, linkId6);
		leg_3_6.setRoute(route36);
		
		Leg leg_4_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds45 = new ArrayList<Id<Link>>();
		NetworkRoute route45 = (NetworkRoute) routeFactory.createRoute(linkId4, linkId5);
		route45.setLinkIds(linkId4, linkIds45, linkId5);
		leg_4_5.setRoute(route45);
		
		Leg leg_4_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds46 = new ArrayList<Id<Link>>();
		linkIds46.add(linkId5);
		NetworkRoute route46 = (NetworkRoute) routeFactory.createRoute(linkId4, linkId6);
		route46.setLinkIds(linkId4, linkIds46, linkId6);
		leg_4_6.setRoute(route46);
		
		Leg leg_5_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds56 = new ArrayList<Id<Link>>();
		NetworkRoute route56 = (NetworkRoute) routeFactory.createRoute(linkId5, linkId6);
		route56.setLinkIds(linkId5, linkIds56, linkId6);
		leg_5_6.setRoute(route56);
		
		Person person1 = popFactory.createPerson(testAgent1);
		Plan plan1 = popFactory.createPlan();
		Activity act1 = popFactory.createActivityFromCoord("h", coord2);
		act1.setEndTime(99);
		plan1.addActivity(act1);
	//	Leg leg1 = popFactory.createLeg("pt");
		plan1.addLeg(leg_2_3);
	//	lastActLink3.getCoord().setXY(1500.0, 0.0);
		lastActLink3.setType("w");
		plan1.addActivity(lastActLink3);
		person1.addPlan(plan1);
		
		Person person2 = popFactory.createPerson(testAgent2);
		Plan plan2 = popFactory.createPlan();
		Activity act2 = popFactory.createActivityFromCoord("h", coord4);
		act2.setEndTime(384);
		plan2.addActivity(act2);
	//	Leg leg2 = popFactory.createLeg("pt");
		plan2.addLeg(leg_4_6);
	//	lastActLink6.getCoord().setXY(3000.0, 0.0);
		lastActLink6.setType("w");
		plan2.addActivity(lastActLink6);
		person2.addPlan(plan2);
		
		population.addPerson(person1);
		population.addPerson(person2);
		
	}
	
	private void fillScheduleTestWaitingDelay03(MutableScenario scenario) {
		TransitSchedule schedule = scenario.getTransitSchedule();
		
		TransitScheduleFactory sf = schedule.getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();
		
		Id<TransitStopFacility> Stop1 = Id.create("Stop1Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop2 = Id.create("Stop2Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop3 = Id.create("Stop3Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop4 = Id.create("Stop4Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop5 = Id.create("Stop5Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop6 = Id.create("Stop6Id", TransitStopFacility.class);
		TransitStopFacility StopFacility1;
		TransitStopFacility StopFacility2;
		TransitStopFacility StopFacility3;
		TransitStopFacility StopFacility4;
		TransitStopFacility StopFacility5;
		TransitStopFacility StopFacility6;
		StopFacility1 = sf.createTransitStopFacility(Stop1, new Coord(500., 0.), true);
		StopFacility1.setLinkId(linkId1);
		StopFacility2 = sf.createTransitStopFacility(Stop2, new Coord(1000., 0.), true);
		StopFacility2.setLinkId(linkId2);
		StopFacility3 = sf.createTransitStopFacility(Stop3, new Coord(1500., 0.), true);
		StopFacility3.setLinkId(linkId3);
		StopFacility4 = sf.createTransitStopFacility(Stop4, new Coord(2000., 0.), true);
		StopFacility4.setLinkId(linkId4);
		StopFacility5 = sf.createTransitStopFacility(Stop5, new Coord(2500., 0.), true);
		StopFacility5.setLinkId(linkId5);
		StopFacility6 = sf.createTransitStopFacility(Stop6, new Coord(3000., 0.), true);
		StopFacility6.setLinkId(linkId6);
		
		TransitLine TransitLine1 = null;
		Id<TransitLine> Line1 = Id.create("line1Id", TransitLine.class);
		TransitLine1 = sf.createTransitLine(Line1);
		
		List<Id<TransitLine>> lineIds = new ArrayList<Id<TransitLine>>();
		lineIds.add(Line1);
		
		Id<TransitRoute> Route1 = Id.create("Route1Id", TransitRoute.class);
		NetworkRoute NetworkRoute1 = null;
		NetworkRoute1 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId6);
		List<Id<Link>> LinkIds = new ArrayList<Id<Link>>();
		LinkIds.add(linkId2);
		LinkIds.add(linkId3);
		LinkIds.add(linkId4);
		LinkIds.add(linkId5);
		NetworkRoute1.setLinkIds(linkId1, LinkIds, linkId6);
		
		TransitRouteStop TRStop1;
		TransitRouteStop TRStop2;
		TransitRouteStop TRStop3;
		TransitRouteStop TRStop4;
		TransitRouteStop TRStop5;
		TransitRouteStop TRStop6;
		TRStop1 = sf.createTransitRouteStop(StopFacility1, 0.0, 0.0);
		TRStop2 = sf.createTransitRouteStop(StopFacility2, 60.0, 60.0);
		TRStop3 = sf.createTransitRouteStop(StopFacility3, 120.0, 120.0);
		TRStop4 = sf.createTransitRouteStop(StopFacility4, 180.0, 180.0);
		TRStop5 = sf.createTransitRouteStop(StopFacility5, 240.0, 240.0);
		TRStop6 = sf.createTransitRouteStop(StopFacility6, 300.0, 300.0);
		
		List<TransitRouteStop> Stops = new ArrayList<TransitRouteStop>();
		Stops.add(TRStop1);
		Stops.add(TRStop2);
		Stops.add(TRStop3);
		Stops.add(TRStop4);
		Stops.add(TRStop5);
		Stops.add(TRStop6);
		
		TransitRoute TransitRoute1 = null;
		TransitRoute1 = sf.createTransitRoute(Route1, NetworkRoute1, Stops, "pt");
		
		Departure Departure1 = null;
		Id<Departure> DepartureId1 = Id.create("DepartureId_1", Departure.class);
		Departure1 = sf.createDeparture(DepartureId1, 200);
		Id<Vehicle> veh_1 = Id.create("veh_1", Vehicle.class);
		Departure1.setVehicleId(veh_1);
		
		Departure Departure2 = null;
		Id<Departure> DepartureId2 = Id.create("DepartureId_2", Departure.class);
		Departure2 = sf.createDeparture(DepartureId2, 800);
		Id<Vehicle> veh_2 = Id.create("veh_2", Vehicle.class);
		Departure2.setVehicleId(veh_2);
		
		TransitRoute1.addDeparture(Departure1);
		TransitRoute1.addDeparture(Departure2);
		
		TransitLine1.addRoute(TransitRoute1);
		
		schedule.addTransitLine(TransitLine1);
		schedule.addStopFacility(StopFacility1);
		schedule.addStopFacility(StopFacility2);
		schedule.addStopFacility(StopFacility3);
		schedule.addStopFacility(StopFacility4);
		schedule.addStopFacility(StopFacility5);
		schedule.addStopFacility(StopFacility6);
		
	}
	
	private void fillVehicleTestWaitingDelay03(MutableScenario scenario) {
		Vehicles veh = scenario.getTransitVehicles();

		Id<VehicleType> vehTypeId1 = Id.create("type_1", VehicleType.class);
		Id<Vehicle> vehId1 = Id.create("veh_1", Vehicle.class);
		Id<Vehicle> vehId2 = Id.create("veh_2", Vehicle.class);

		VehicleType type = veh.getFactory().createVehicleType(vehTypeId1);
		VehicleCapacity cap = veh.getFactory().createVehicleCapacity();
		cap.setSeats(50);
		cap.setStandingRoom(0);
		type.setCapacity(cap);
		type.setLength(10);
		type.setAccessTime(1.0);
		type.setEgressTime(0.75);
		type.setDoorOperationMode(DoorOperationMode.serial);
		
		type.setMaximumVelocity(8.4);
		type.setPcuEquivalents(7.5);
		
		veh.addVehicleType(type); 
		
		Vehicle vehicle1 = veh.getFactory().createVehicle(vehId1, veh.getVehicleTypes().get(vehTypeId1));
		veh.addVehicle( vehicle1);
		
		Vehicle vehicle2 = veh.getFactory().createVehicle(vehId2, veh.getVehicleTypes().get(vehTypeId1));
		veh.addVehicle( vehicle2);
	}

	private void setPopulationTestWaitingDelay03b(Scenario scenario) {
		
		Population population = scenario.getPopulation();
        PopulationFactory popFactory = (PopulationFactory) scenario.getPopulation().getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();

		Coord coord1 = new Coord(500.0, 0.0);
		Coord coord2 = new Coord(1000.0, 0.0);
		Coord coord3 = new Coord(1500.0, 0.0);
		Coord coord4 = new Coord(2000.0, 0.0);
		Coord coord5 = new Coord(2500.0, 0.0);
		Coord coord6 = new Coord(3000.0, 0.0);
		
		Activity lastActLink2 = popFactory.createActivityFromCoord("w", coord2);
		Activity lastActLink3 = popFactory.createActivityFromCoord("w", coord3);
		Activity lastActLink4 = popFactory.createActivityFromCoord("w", coord4);
		Activity lastActLink5 = popFactory.createActivityFromCoord("w", coord5);
		Activity lastActLink6 = popFactory.createActivityFromCoord("w", coord6);
		
		Leg leg_1_2 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds12 = new ArrayList<Id<Link>>();
		NetworkRoute route12 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId2);
		route12.setLinkIds(linkId1, linkIds12, linkId2);
		leg_1_2.setRoute(route12);
		
		Leg leg_1_3 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds13 = new ArrayList<Id<Link>>();
		linkIds13.add(linkId2);
		NetworkRoute route13 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId3);
		route13.setLinkIds(linkId1, linkIds13, linkId3);
		leg_1_3.setRoute(route13);
		
		Leg leg_1_4 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds14 = new ArrayList<Id<Link>>();
		linkIds14.add(linkId2);
		linkIds14.add(linkId3);
		NetworkRoute route14 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId4);
		route14.setLinkIds(linkId1, linkIds14, linkId4);
		leg_1_4.setRoute(route14);
		
		Leg leg_1_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds15 = new ArrayList<Id<Link>>();
		linkIds15.add(linkId2);
		linkIds15.add(linkId3);
		linkIds15.add(linkId4);
		NetworkRoute route15 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId5);
		route15.setLinkIds(linkId1, linkIds15, linkId5);
		leg_1_5.setRoute(route15);
		
		Leg leg_1_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds16 = new ArrayList<Id<Link>>();
		linkIds16.add(linkId2);
		linkIds16.add(linkId3);
		linkIds16.add(linkId4);
		linkIds16.add(linkId5);
		NetworkRoute route16 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId6);
		route16.setLinkIds(linkId1, linkIds16, linkId6);
		leg_1_6.setRoute(route16);
		
		Leg leg_2_3 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds23 = new ArrayList<Id<Link>>();
		NetworkRoute route23 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId3);
		route23.setLinkIds(linkId2, linkIds23, linkId3);
		leg_2_3.setRoute(route23);
		
		Leg leg_2_4 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds24 = new ArrayList<Id<Link>>();
		linkIds24.add(linkId3);
		NetworkRoute route24 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId4);
		route24.setLinkIds(linkId2, linkIds24, linkId4);
		leg_2_4.setRoute(route24);
		
		Leg leg_2_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds25 = new ArrayList<Id<Link>>();
		linkIds25.add(linkId3);
		linkIds25.add(linkId4);
		NetworkRoute route25 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId5);
		route25.setLinkIds(linkId2, linkIds25, linkId5);
		leg_2_5.setRoute(route25);
		
		Leg leg_2_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds26 = new ArrayList<Id<Link>>();
		linkIds26.add(linkId3);
		linkIds26.add(linkId4);
		linkIds26.add(linkId5);
		NetworkRoute route26 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId6);
		route26.setLinkIds(linkId2, linkIds26, linkId6);
		leg_2_6.setRoute(route26);
		
		Leg leg_3_4 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds34 = new ArrayList<Id<Link>>();
		NetworkRoute route34 = (NetworkRoute) routeFactory.createRoute(linkId3, linkId4);
		route34.setLinkIds(linkId3, linkIds34, linkId4);
		leg_3_4.setRoute(route34);
		
		Leg leg_3_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds35 = new ArrayList<Id<Link>>();
		linkIds35.add(linkId4);
		NetworkRoute route35 = (NetworkRoute) routeFactory.createRoute(linkId3, linkId5);
		route35.setLinkIds(linkId3, linkIds35, linkId5);
		leg_3_5.setRoute(route35);
		
		Leg leg_3_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds36 = new ArrayList<Id<Link>>();
		linkIds36.add(linkId4);
		linkIds36.add(linkId5);
		NetworkRoute route36 = (NetworkRoute) routeFactory.createRoute(linkId3, linkId6);
		route36.setLinkIds(linkId3, linkIds36, linkId6);
		leg_3_6.setRoute(route36);
		
		Leg leg_4_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds45 = new ArrayList<Id<Link>>();
		NetworkRoute route45 = (NetworkRoute) routeFactory.createRoute(linkId4, linkId5);
		route45.setLinkIds(linkId4, linkIds45, linkId5);
		leg_4_5.setRoute(route45);
		
		Leg leg_4_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds46 = new ArrayList<Id<Link>>();
		linkIds46.add(linkId5);
		NetworkRoute route46 = (NetworkRoute) routeFactory.createRoute(linkId4, linkId6);
		route46.setLinkIds(linkId4, linkIds46, linkId6);
		leg_4_6.setRoute(route46);
		
		Leg leg_5_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds56 = new ArrayList<Id<Link>>();
		NetworkRoute route56 = (NetworkRoute) routeFactory.createRoute(linkId5, linkId6);
		route56.setLinkIds(linkId5, linkIds56, linkId6);
		leg_5_6.setRoute(route56);
		
		Person person1 = popFactory.createPerson(testAgent1);
		Plan plan1 = popFactory.createPlan();
		Activity act1 = popFactory.createActivityFromCoord("h", coord2);
		act1.setEndTime(99);
		plan1.addActivity(act1);
	//	Leg leg1 = popFactory.createLeg("pt");
		plan1.addLeg(leg_2_4);
	//	lastActLink4.getCoord().setXY(2000.0, 0.0);
		lastActLink4.setType("w");
		plan1.addActivity(lastActLink4);
		person1.addPlan(plan1);
		
		Person person2 = popFactory.createPerson(testAgent2);
		Plan plan2 = popFactory.createPlan();
		Activity act2 = popFactory.createActivityFromCoord("h", coord4);
		act2.setEndTime(383);
		plan2.addActivity(act2);
	//	Leg leg2 = popFactory.createLeg("pt");
		plan2.addLeg(leg_4_6);
	//	lastActLink6.getCoord().setXY(3000.0, 0.0);
		lastActLink6.setType("w");
		plan2.addActivity(lastActLink6);
		person2.addPlan(plan2);
		
		population.addPerson(person1);
		population.addPerson(person2);
		
	}
	
	private void fillScheduleTestWaitingDelay03b(MutableScenario scenario) {
		TransitSchedule schedule = scenario.getTransitSchedule();
		
		TransitScheduleFactory sf = schedule.getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();
		
		Id<TransitStopFacility> Stop1 = Id.create("Stop1Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop2 = Id.create("Stop2Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop3 = Id.create("Stop3Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop4 = Id.create("Stop4Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop5 = Id.create("Stop5Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop6 = Id.create("Stop6Id", TransitStopFacility.class);
		TransitStopFacility StopFacility1;
		TransitStopFacility StopFacility2;
		TransitStopFacility StopFacility3;
		TransitStopFacility StopFacility4;
		TransitStopFacility StopFacility5;
		TransitStopFacility StopFacility6;
		StopFacility1 = sf.createTransitStopFacility(Stop1, new Coord(500., 0.), true);
		StopFacility1.setLinkId(linkId1);
		StopFacility2 = sf.createTransitStopFacility(Stop2, new Coord(1000., 0.), true);
		StopFacility2.setLinkId(linkId2);
		StopFacility3 = sf.createTransitStopFacility(Stop3, new Coord(1500., 0.), true);
		StopFacility3.setLinkId(linkId3);
		StopFacility4 = sf.createTransitStopFacility(Stop4, new Coord(2000., 0.), true);
		StopFacility4.setLinkId(linkId4);
		StopFacility5 = sf.createTransitStopFacility(Stop5, new Coord(2500., 0.), true);
		StopFacility5.setLinkId(linkId5);
		StopFacility6 = sf.createTransitStopFacility(Stop6, new Coord(3000., 0.), true);
		StopFacility6.setLinkId(linkId6);
		
		TransitLine TransitLine1 = null;
		Id<TransitLine> Line1 = Id.create("line1Id", TransitLine.class);
		TransitLine1 = sf.createTransitLine(Line1);
		
		List<Id<TransitLine>> lineIds = new ArrayList<Id<TransitLine>>();
		lineIds.add(Line1);
		
		Id<TransitRoute> Route1 = Id.create("Route1Id", TransitRoute.class);
		NetworkRoute NetworkRoute1 = null;
		NetworkRoute1 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId6);
		List<Id<Link>> LinkIds = new ArrayList<Id<Link>>();
		LinkIds.add(linkId2);
		LinkIds.add(linkId3);
		LinkIds.add(linkId4);
		LinkIds.add(linkId5);
		NetworkRoute1.setLinkIds(linkId1, LinkIds, linkId6);
		
		TransitRouteStop TRStop1;
		TransitRouteStop TRStop2;
		TransitRouteStop TRStop3;
		TransitRouteStop TRStop4;
		TransitRouteStop TRStop5;
		TransitRouteStop TRStop6;
		TRStop1 = sf.createTransitRouteStop(StopFacility1, 0.0, 0.0);
		TRStop2 = sf.createTransitRouteStop(StopFacility2, 60.0, 60.0);
		TRStop3 = sf.createTransitRouteStop(StopFacility3, 120.0, 120.0);
		TRStop4 = sf.createTransitRouteStop(StopFacility4, 180.0, 180.0);
		TRStop5 = sf.createTransitRouteStop(StopFacility5, 240.0, 240.0);
		TRStop6 = sf.createTransitRouteStop(StopFacility6, 300.0, 300.0);
		
		List<TransitRouteStop> Stops = new ArrayList<TransitRouteStop>();
		Stops.add(TRStop1);
		Stops.add(TRStop2);
		Stops.add(TRStop3);
		Stops.add(TRStop4);
		Stops.add(TRStop5);
		Stops.add(TRStop6);
		
		TransitRoute TransitRoute1 = null;
		TransitRoute1 = sf.createTransitRoute(Route1, NetworkRoute1, Stops, "pt");
		
		Departure Departure1 = null;
		Id<Departure> DepartureId1 = Id.create("DepartureId_1", Departure.class);
		Departure1 = sf.createDeparture(DepartureId1, 200);
		Id<Vehicle> veh_1 = Id.create("veh_1", Vehicle.class);
		Departure1.setVehicleId(veh_1);
		
		Departure Departure2 = null;
		Id<Departure> DepartureId2 = Id.create("DepartureId_2", Departure.class);
		Departure2 = sf.createDeparture(DepartureId2, 800);
		Id<Vehicle> veh_2 = Id.create("veh_2", Vehicle.class);
		Departure2.setVehicleId(veh_2);
		
		TransitRoute1.addDeparture(Departure1);
		TransitRoute1.addDeparture(Departure2);
		
		TransitLine1.addRoute(TransitRoute1);
		
		schedule.addTransitLine(TransitLine1);
		schedule.addStopFacility(StopFacility1);
		schedule.addStopFacility(StopFacility2);
		schedule.addStopFacility(StopFacility3);
		schedule.addStopFacility(StopFacility4);
		schedule.addStopFacility(StopFacility5);
		schedule.addStopFacility(StopFacility6);
		
	}
	
	private void fillVehicleTestWaitingDelay03b(MutableScenario scenario) {
		Vehicles veh = scenario.getTransitVehicles();

		Id<VehicleType> vehTypeId1 = Id.create("type_1", VehicleType.class);
		Id<Vehicle> vehId1 = Id.create("veh_1", Vehicle.class);
		Id<Vehicle> vehId2 = Id.create("veh_2", Vehicle.class);

		VehicleType type = veh.getFactory().createVehicleType(vehTypeId1);
		VehicleCapacity cap = veh.getFactory().createVehicleCapacity();
		cap.setSeats(50);
		cap.setStandingRoom(0);
		type.setCapacity(cap);
		type.setLength(10);
		type.setAccessTime(1.0);
		type.setEgressTime(0.75);
		type.setDoorOperationMode(DoorOperationMode.serial);
		
		type.setMaximumVelocity(8.4);
		type.setPcuEquivalents(7.5);
		
		veh.addVehicleType( type); 
		
		Vehicle vehicle1 = veh.getFactory().createVehicle(vehId1, veh.getVehicleTypes().get(vehTypeId1));
		veh.addVehicle( vehicle1);
		
		Vehicle vehicle2 = veh.getFactory().createVehicle(vehId2, veh.getVehicleTypes().get(vehTypeId1));
		veh.addVehicle( vehicle2);
	}
	
	private void setPopulationTestWaitingDelay04(Scenario scenario) {
		
		Population population = scenario.getPopulation();
        PopulationFactory popFactory = (PopulationFactory) scenario.getPopulation().getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();

		Coord coord1 = new Coord(500.0, 0.0);
		Coord coord2 = new Coord(1000.0, 0.0);
		Coord coord3 = new Coord(1500.0, 0.0);
		Coord coord4 = new Coord(2000.0, 0.0);
		Coord coord5 = new Coord(2500.0, 0.0);
		Coord coord6 = new Coord(3000.0, 0.0);
		
		Activity lastActLink2 = popFactory.createActivityFromCoord("w", coord2);
		Activity lastActLink3 = popFactory.createActivityFromCoord("w", coord3);
		Activity lastActLink4 = popFactory.createActivityFromCoord("w", coord4);
		Activity lastActLink5 = popFactory.createActivityFromCoord("w", coord5);
		Activity lastActLink6 = popFactory.createActivityFromCoord("w", coord6);
		
		Leg leg_1_2 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds12 = new ArrayList<Id<Link>>();
		NetworkRoute route12 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId2);
		route12.setLinkIds(linkId1, linkIds12, linkId2);
		leg_1_2.setRoute(route12);
		
		Leg leg_1_3 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds13 = new ArrayList<Id<Link>>();
		linkIds13.add(linkId2);
		NetworkRoute route13 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId3);
		route13.setLinkIds(linkId1, linkIds13, linkId3);
		leg_1_3.setRoute(route13);
		
		Leg leg_1_4 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds14 = new ArrayList<Id<Link>>();
		linkIds14.add(linkId2);
		linkIds14.add(linkId3);
		NetworkRoute route14 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId4);
		route14.setLinkIds(linkId1, linkIds14, linkId4);
		leg_1_4.setRoute(route14);
		
		Leg leg_1_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds15 = new ArrayList<Id<Link>>();
		linkIds15.add(linkId2);
		linkIds15.add(linkId3);
		linkIds15.add(linkId4);
		NetworkRoute route15 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId5);
		route15.setLinkIds(linkId1, linkIds15, linkId5);
		leg_1_5.setRoute(route15);
		
		Leg leg_1_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds16 = new ArrayList<Id<Link>>();
		linkIds16.add(linkId2);
		linkIds16.add(linkId3);
		linkIds16.add(linkId4);
		linkIds16.add(linkId5);
		NetworkRoute route16 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId6);
		route16.setLinkIds(linkId1, linkIds16, linkId6);
		leg_1_6.setRoute(route16);
		
		Leg leg_2_3 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds23 = new ArrayList<Id<Link>>();
		NetworkRoute route23 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId3);
		route23.setLinkIds(linkId2, linkIds23, linkId3);
		leg_2_3.setRoute(route23);
		
		Leg leg_2_4 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds24 = new ArrayList<Id<Link>>();
		linkIds24.add(linkId3);
		NetworkRoute route24 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId4);
		route24.setLinkIds(linkId2, linkIds24, linkId4);
		leg_2_4.setRoute(route24);
		
		Leg leg_2_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds25 = new ArrayList<Id<Link>>();
		linkIds25.add(linkId3);
		linkIds25.add(linkId4);
		NetworkRoute route25 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId5);
		route25.setLinkIds(linkId2, linkIds25, linkId5);
		leg_2_5.setRoute(route25);
		
		Leg leg_2_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds26 = new ArrayList<Id<Link>>();
		linkIds26.add(linkId3);
		linkIds26.add(linkId4);
		linkIds26.add(linkId5);
		NetworkRoute route26 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId6);
		route26.setLinkIds(linkId2, linkIds26, linkId6);
		leg_2_6.setRoute(route26);
		
		Leg leg_3_4 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds34 = new ArrayList<Id<Link>>();
		NetworkRoute route34 = (NetworkRoute) routeFactory.createRoute(linkId3, linkId4);
		route34.setLinkIds(linkId3, linkIds34, linkId4);
		leg_3_4.setRoute(route34);
		
		Leg leg_3_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds35 = new ArrayList<Id<Link>>();
		linkIds35.add(linkId4);
		NetworkRoute route35 = (NetworkRoute) routeFactory.createRoute(linkId3, linkId5);
		route35.setLinkIds(linkId3, linkIds35, linkId5);
		leg_3_5.setRoute(route35);
		
		Leg leg_3_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds36 = new ArrayList<Id<Link>>();
		linkIds36.add(linkId4);
		linkIds36.add(linkId5);
		NetworkRoute route36 = (NetworkRoute) routeFactory.createRoute(linkId3, linkId6);
		route36.setLinkIds(linkId3, linkIds36, linkId6);
		leg_3_6.setRoute(route36);
		
		Leg leg_4_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds45 = new ArrayList<Id<Link>>();
		NetworkRoute route45 = (NetworkRoute) routeFactory.createRoute(linkId4, linkId5);
		route45.setLinkIds(linkId4, linkIds45, linkId5);
		leg_4_5.setRoute(route45);
		
		Leg leg_4_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds46 = new ArrayList<Id<Link>>();
		linkIds46.add(linkId5);
		NetworkRoute route46 = (NetworkRoute) routeFactory.createRoute(linkId4, linkId6);
		route46.setLinkIds(linkId4, linkIds46, linkId6);
		leg_4_6.setRoute(route46);
		
		Leg leg_5_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds56 = new ArrayList<Id<Link>>();
		NetworkRoute route56 = (NetworkRoute) routeFactory.createRoute(linkId5, linkId6);
		route56.setLinkIds(linkId5, linkIds56, linkId6);
		leg_5_6.setRoute(route56);
		
		Person person1 = popFactory.createPerson(testAgent1);
		Plan plan1 = popFactory.createPlan();
		Activity act1 = popFactory.createActivityFromCoord("h", coord2);
		act1.setEndTime(99);
		plan1.addActivity(act1);
	//	Leg leg1 = popFactory.createLeg("pt");
		plan1.addLeg(leg_2_3);
	//	lastActLink3.getCoord().setXY(1500.0, 0.0);
		lastActLink3.setType("w");
		plan1.addActivity(lastActLink3);
		person1.addPlan(plan1);
		
		Person person2 = popFactory.createPerson(testAgent2);
		Plan plan2 = popFactory.createPlan();
		Activity act2 = popFactory.createActivityFromCoord("h", coord2);
		act2.setEndTime(100);
		plan2.addActivity(act2);
	//	Leg leg2 = popFactory.createLeg("pt");
		plan2.addLeg(leg_2_3);
	//	lastActLink3.getCoord().setXY(1500.0, 0.0);
		lastActLink3.setType("w");
		plan2.addActivity(lastActLink3);
		person2.addPlan(plan2);
		
		Person person3 = popFactory.createPerson(testAgent3);
		Plan plan3 = popFactory.createPlan();
		Activity act3 = popFactory.createActivityFromCoord("h", coord4);
		act3.setEndTime(99);
		plan3.addActivity(act3);
	//	Leg leg3 = popFactory.createLeg("pt");
		plan3.addLeg(leg_4_5);
	//	lastActLink5.getCoord().setXY(2500.0, 0.0);
		lastActLink5.setType("w");
		plan3.addActivity(lastActLink5);
		person3.addPlan(plan3);
		
		population.addPerson(person1);
		population.addPerson(person2);
		population.addPerson(person3);
		
	}
	
	private void fillScheduleTestWaitingDelay04(MutableScenario scenario) {
		TransitSchedule schedule = scenario.getTransitSchedule();
		
		TransitScheduleFactory sf = schedule.getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();
		
		Id<TransitStopFacility> Stop1 = Id.create("Stop1Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop2 = Id.create("Stop2Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop3 = Id.create("Stop3Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop4 = Id.create("Stop4Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop5 = Id.create("Stop5Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop6 = Id.create("Stop6Id", TransitStopFacility.class);
		TransitStopFacility StopFacility1;
		TransitStopFacility StopFacility2;
		TransitStopFacility StopFacility3;
		TransitStopFacility StopFacility4;
		TransitStopFacility StopFacility5;
		TransitStopFacility StopFacility6;
		StopFacility1 = sf.createTransitStopFacility(Stop1, new Coord(500., 0.), true);
		StopFacility1.setLinkId(linkId1);
		StopFacility2 = sf.createTransitStopFacility(Stop2, new Coord(1000., 0.), true);
		StopFacility2.setLinkId(linkId2);
		StopFacility3 = sf.createTransitStopFacility(Stop3, new Coord(1500., 0.), true);
		StopFacility3.setLinkId(linkId3);
		StopFacility4 = sf.createTransitStopFacility(Stop4, new Coord(2000., 0.), true);
		StopFacility4.setLinkId(linkId4);
		StopFacility5 = sf.createTransitStopFacility(Stop5, new Coord(2500., 0.), true);
		StopFacility5.setLinkId(linkId5);
		StopFacility6 = sf.createTransitStopFacility(Stop6, new Coord(3000., 0.), true);
		StopFacility6.setLinkId(linkId6);
		
		TransitLine TransitLine1 = null;
		Id<TransitLine> Line1 = Id.create("line1Id", TransitLine.class);
		TransitLine1 = sf.createTransitLine(Line1);
		
		List<Id<TransitLine>> lineIds = new ArrayList<Id<TransitLine>>();
		lineIds.add(Line1);
		
		Id<TransitRoute> Route1 = Id.create("Route1Id", TransitRoute.class);
		NetworkRoute NetworkRoute1 = null;
		NetworkRoute1 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId6);
		List<Id<Link>> LinkIds = new ArrayList<Id<Link>>();
		LinkIds.add(linkId2);
		LinkIds.add(linkId3);
		LinkIds.add(linkId4);
		LinkIds.add(linkId5);
		NetworkRoute1.setLinkIds(linkId1, LinkIds, linkId6);
		
		TransitRouteStop TRStop1;
		TransitRouteStop TRStop2;
		TransitRouteStop TRStop3;
		TransitRouteStop TRStop4;
		TransitRouteStop TRStop5;
		TransitRouteStop TRStop6;
		TRStop1 = sf.createTransitRouteStop(StopFacility1, 0.0, 0.0);
		TRStop2 = sf.createTransitRouteStop(StopFacility2, 60.0, 60.0);
		TRStop3 = sf.createTransitRouteStop(StopFacility3, 120.0, 120.0);
		TRStop4 = sf.createTransitRouteStop(StopFacility4, 180.0, 180.0);
		TRStop5 = sf.createTransitRouteStop(StopFacility5, 240.0, 240.0);
		TRStop6 = sf.createTransitRouteStop(StopFacility6, 300.0, 300.0);
		
		List<TransitRouteStop> Stops = new ArrayList<TransitRouteStop>();
		Stops.add(TRStop1);
		Stops.add(TRStop2);
		Stops.add(TRStop3);
		Stops.add(TRStop4);
		Stops.add(TRStop5);
		Stops.add(TRStop6);
		
		TransitRoute TransitRoute1 = null;
		TransitRoute1 = sf.createTransitRoute(Route1, NetworkRoute1, Stops, "pt");
		
		Departure Departure1 = null;
		Id<Departure> DepartureId1 = Id.create("DepartureId_1", Departure.class);
		Departure1 = sf.createDeparture(DepartureId1, 200);
		Id<Vehicle> veh_1 = Id.create("veh_1", Vehicle.class);
		Departure1.setVehicleId(veh_1);
		
		TransitRoute1.addDeparture(Departure1);
		
		TransitLine1.addRoute(TransitRoute1);
		
		schedule.addTransitLine(TransitLine1);
		schedule.addStopFacility(StopFacility1);
		schedule.addStopFacility(StopFacility2);
		schedule.addStopFacility(StopFacility3);
		schedule.addStopFacility(StopFacility4);
		schedule.addStopFacility(StopFacility5);
		schedule.addStopFacility(StopFacility6);
		
	}
	
	private void fillVehicleTestWaitingDelay04(MutableScenario scenario) {
		Vehicles veh = scenario.getTransitVehicles();

		Id<VehicleType> vehTypeId1 = Id.create("type_1", VehicleType.class);
		Id<Vehicle> vehId1 = Id.create("veh_1", Vehicle.class);

		VehicleType type = veh.getFactory().createVehicleType(vehTypeId1);
		VehicleCapacity cap = veh.getFactory().createVehicleCapacity();
		cap.setSeats(50);
		cap.setStandingRoom(0);
		type.setCapacity(cap);
		type.setLength(10);
		type.setAccessTime(1.0);
		type.setEgressTime(0.75);
		type.setDoorOperationMode(DoorOperationMode.serial);
		
		type.setMaximumVelocity(8.4);
		type.setPcuEquivalents(7.5);
		
		veh.addVehicleType(type); 
		
		Vehicle vehicle1 = veh.getFactory().createVehicle(vehId1, veh.getVehicleTypes().get(vehTypeId1));
		veh.addVehicle( vehicle1);
	}
	
	private void setPopulationTestWaitingDelay05(Scenario scenario) {
		
		Population population = scenario.getPopulation();
        PopulationFactory popFactory = (PopulationFactory) scenario.getPopulation().getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();

		Coord coord1 = new Coord(499.0, 0.0);
		Coord coord2 = new Coord(999.0, 0.0);
		Coord coord3 = new Coord(1499.0, 0.0);
		Coord coord4 = new Coord(1999.0, 0.0);
		Coord coord5 = new Coord(2499.0, 0.0);
		Coord coord6 = new Coord(2999.0, 0.0);
		
		Activity lastActLink2 = popFactory.createActivityFromCoord("w", coord2);
		Activity lastActLink3 = popFactory.createActivityFromCoord("w", coord3);
		Activity lastActLink4 = popFactory.createActivityFromCoord("w", coord4);
		Activity lastActLink5 = popFactory.createActivityFromCoord("w", coord5);
		Activity lastActLink6 = popFactory.createActivityFromCoord("w", coord6);
		
		Leg leg_1_2 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds12 = new ArrayList<Id<Link>>();
		NetworkRoute route12 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId2);
		route12.setLinkIds(linkId1, linkIds12, linkId2);
		leg_1_2.setRoute(route12);
		
		Leg leg_1_3 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds13 = new ArrayList<Id<Link>>();
		linkIds13.add(linkId2);
		NetworkRoute route13 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId3);
		route13.setLinkIds(linkId1, linkIds13, linkId3);
		leg_1_3.setRoute(route13);
		
		Leg leg_1_4 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds14 = new ArrayList<Id<Link>>();
		linkIds14.add(linkId2);
		linkIds14.add(linkId3);
		NetworkRoute route14 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId4);
		route14.setLinkIds(linkId1, linkIds14, linkId4);
		leg_1_4.setRoute(route14);
		
		Leg leg_1_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds15 = new ArrayList<Id<Link>>();
		linkIds15.add(linkId2);
		linkIds15.add(linkId3);
		linkIds15.add(linkId4);
		NetworkRoute route15 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId5);
		route15.setLinkIds(linkId1, linkIds15, linkId5);
		leg_1_5.setRoute(route15);
		
		Leg leg_1_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds16 = new ArrayList<Id<Link>>();
		linkIds16.add(linkId2);
		linkIds16.add(linkId3);
		linkIds16.add(linkId4);
		linkIds16.add(linkId5);
		NetworkRoute route16 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId6);
		route16.setLinkIds(linkId1, linkIds16, linkId6);
		leg_1_6.setRoute(route16);
		
		Leg leg_2_3 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds23 = new ArrayList<Id<Link>>();
		NetworkRoute route23 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId3);
		route23.setLinkIds(linkId2, linkIds23, linkId3);
		leg_2_3.setRoute(route23);
		
		Leg leg_2_4 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds24 = new ArrayList<Id<Link>>();
		linkIds24.add(linkId3);
		NetworkRoute route24 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId4);
		route24.setLinkIds(linkId2, linkIds24, linkId4);
		leg_2_4.setRoute(route24);
		
		Leg leg_2_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds25 = new ArrayList<Id<Link>>();
		linkIds25.add(linkId3);
		linkIds25.add(linkId4);
		NetworkRoute route25 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId5);
		route25.setLinkIds(linkId2, linkIds25, linkId5);
		leg_2_5.setRoute(route25);
		
		Leg leg_2_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds26 = new ArrayList<Id<Link>>();
		linkIds26.add(linkId3);
		linkIds26.add(linkId4);
		linkIds26.add(linkId5);
		NetworkRoute route26 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId6);
		route26.setLinkIds(linkId2, linkIds26, linkId6);
		leg_2_6.setRoute(route26);
		
		Leg leg_3_4 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds34 = new ArrayList<Id<Link>>();
		NetworkRoute route34 = (NetworkRoute) routeFactory.createRoute(linkId3, linkId4);
		route34.setLinkIds(linkId3, linkIds34, linkId4);
		leg_3_4.setRoute(route34);
		
		Leg leg_3_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds35 = new ArrayList<Id<Link>>();
		linkIds35.add(linkId4);
		NetworkRoute route35 = (NetworkRoute) routeFactory.createRoute(linkId3, linkId5);
		route35.setLinkIds(linkId3, linkIds35, linkId5);
		leg_3_5.setRoute(route35);
		
		Leg leg_3_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds36 = new ArrayList<Id<Link>>();
		linkIds36.add(linkId4);
		linkIds36.add(linkId5);
		NetworkRoute route36 = (NetworkRoute) routeFactory.createRoute(linkId3, linkId6);
		route36.setLinkIds(linkId3, linkIds36, linkId6);
		leg_3_6.setRoute(route36);
		
		Leg leg_4_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds45 = new ArrayList<Id<Link>>();
		NetworkRoute route45 = (NetworkRoute) routeFactory.createRoute(linkId4, linkId5);
		route45.setLinkIds(linkId4, linkIds45, linkId5);
		leg_4_5.setRoute(route45);
		
		Leg leg_4_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds46 = new ArrayList<Id<Link>>();
		linkIds46.add(linkId5);
		NetworkRoute route46 = (NetworkRoute) routeFactory.createRoute(linkId4, linkId6);
		route46.setLinkIds(linkId4, linkIds46, linkId6);
		leg_4_6.setRoute(route46);
		
		Leg leg_5_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds56 = new ArrayList<Id<Link>>();
		NetworkRoute route56 = (NetworkRoute) routeFactory.createRoute(linkId5, linkId6);
		route56.setLinkIds(linkId5, linkIds56, linkId6);
		leg_5_6.setRoute(route56);
		
		Person person1 = popFactory.createPerson(testAgent1);
		Plan plan1 = popFactory.createPlan();
		Activity act1 = popFactory.createActivityFromCoord("h", coord2);
		act1.setEndTime(99);
		plan1.addActivity(act1);
	//	Leg leg1 = popFactory.createLeg("pt");
		plan1.addLeg(leg_2_3);
	//	lastActLink3.getCoord().setXY(1500.0, 0.0);
		lastActLink3.setType("w");
		plan1.addActivity(lastActLink3);
		person1.addPlan(plan1);
		
		Person person2 = popFactory.createPerson(testAgent2);
		Plan plan2 = popFactory.createPlan();
		Activity act2 = popFactory.createActivityFromCoord("h", coord4);
		act2.setEndTime(99);
		plan2.addActivity(act2);
	//	Leg leg2 = popFactory.createLeg("pt");
		plan2.addLeg(leg_4_5);
	//	lastActLink5.getCoord().setXY(2500.0, 0.0);
		lastActLink5.setType("w");
		plan2.addActivity(lastActLink5);
		person2.addPlan(plan2);
		
		Person person3 = popFactory.createPerson(testAgent3);
		Plan plan3 = popFactory.createPlan();
		Activity act3 = popFactory.createActivityFromCoord("h", coord4);
		act3.setEndTime(100);
		plan3.addActivity(act3);
	//	Leg leg3 = popFactory.createLeg("pt");
		plan3.addLeg(leg_4_5);
	//	lastActLink5.getCoord().setXY(2500.0, 0.0);
		lastActLink5.setType("w");
		plan3.addActivity(lastActLink5);
		person3.addPlan(plan3);
		
		population.addPerson(person1);
		population.addPerson(person2);
		population.addPerson(person3);
		
	}
	
	private void fillScheduleTestWaitingDelay05(MutableScenario scenario) {
		TransitSchedule schedule = scenario.getTransitSchedule();
		
		TransitScheduleFactory sf = schedule.getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();
		
		Id<TransitStopFacility> Stop1 = Id.create("Stop1Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop2 = Id.create("Stop2Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop3 = Id.create("Stop3Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop4 = Id.create("Stop4Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop5 = Id.create("Stop5Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop6 = Id.create("Stop6Id", TransitStopFacility.class);
		TransitStopFacility StopFacility1;
		TransitStopFacility StopFacility2;
		TransitStopFacility StopFacility3;
		TransitStopFacility StopFacility4;
		TransitStopFacility StopFacility5;
		TransitStopFacility StopFacility6;
		StopFacility1 = sf.createTransitStopFacility(Stop1, new Coord(500., 0.), true);
		StopFacility1.setLinkId(linkId1);
		StopFacility2 = sf.createTransitStopFacility(Stop2, new Coord(1000., 0.), true);
		StopFacility2.setLinkId(linkId2);
		StopFacility3 = sf.createTransitStopFacility(Stop3, new Coord(1500., 0.), true);
		StopFacility3.setLinkId(linkId3);
		StopFacility4 = sf.createTransitStopFacility(Stop4, new Coord(2000., 0.), true);
		StopFacility4.setLinkId(linkId4);
		StopFacility5 = sf.createTransitStopFacility(Stop5, new Coord(2500., 0.), true);
		StopFacility5.setLinkId(linkId5);
		StopFacility6 = sf.createTransitStopFacility(Stop6, new Coord(3000., 0.), true);
		StopFacility6.setLinkId(linkId6);
		
		TransitLine TransitLine1 = null;
		Id<TransitLine> Line1 = Id.create("line1Id", TransitLine.class);
		TransitLine1 = sf.createTransitLine(Line1);
		
		List<Id<TransitLine>> lineIds = new ArrayList<Id<TransitLine>>();
		lineIds.add(Line1);
		
		Id<TransitRoute> Route1 = Id.create("Route1Id", TransitRoute.class);
		NetworkRoute NetworkRoute1 = null;
		NetworkRoute1 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId6);
		List<Id<Link>> LinkIds = new ArrayList<Id<Link>>();
		LinkIds.add(linkId2);
		LinkIds.add(linkId3);
		LinkIds.add(linkId4);
		LinkIds.add(linkId5);
		NetworkRoute1.setLinkIds(linkId1, LinkIds, linkId6);
		
		TransitRouteStop TRStop1;
		TransitRouteStop TRStop2;
		TransitRouteStop TRStop3;
		TransitRouteStop TRStop4;
		TransitRouteStop TRStop5;
		TransitRouteStop TRStop6;
		TRStop1 = sf.createTransitRouteStop(StopFacility1, 0.0, 0.0);
		TRStop2 = sf.createTransitRouteStop(StopFacility2, 60.0, 60.0);
		TRStop3 = sf.createTransitRouteStop(StopFacility3, 120.0, 120.0);
		TRStop4 = sf.createTransitRouteStop(StopFacility4, 180.0, 180.0);
		TRStop5 = sf.createTransitRouteStop(StopFacility5, 240.0, 240.0);
		TRStop6 = sf.createTransitRouteStop(StopFacility6, 300.0, 300.0);
		
		List<TransitRouteStop> Stops = new ArrayList<TransitRouteStop>();
		Stops.add(TRStop1);
		Stops.add(TRStop2);
		Stops.add(TRStop3);
		Stops.add(TRStop4);
		Stops.add(TRStop5);
		Stops.add(TRStop6);
		
		TransitRoute TransitRoute1 = null;
		TransitRoute1 = sf.createTransitRoute(Route1, NetworkRoute1, Stops, "pt");
		
		Departure Departure1 = null;
		Id<Departure> DepartureId1 = Id.create("DepartureId_1", Departure.class);
		Departure1 = sf.createDeparture(DepartureId1, 200);
		Id<Vehicle> veh_1 = Id.create("veh_1", Vehicle.class);
		Departure1.setVehicleId(veh_1);
		
		TransitRoute1.addDeparture(Departure1);
		
		TransitLine1.addRoute(TransitRoute1);
		
		schedule.addTransitLine(TransitLine1);
		schedule.addStopFacility(StopFacility1);
		schedule.addStopFacility(StopFacility2);
		schedule.addStopFacility(StopFacility3);
		schedule.addStopFacility(StopFacility4);
		schedule.addStopFacility(StopFacility5);
		schedule.addStopFacility(StopFacility6);
		
	}
	
	private void fillVehicleTestWaitingDelay05(MutableScenario scenario) {
		Vehicles veh = scenario.getTransitVehicles();

		Id<VehicleType> vehTypeId1 = Id.create("type_1", VehicleType.class);
		Id<Vehicle> vehId1 = Id.create("veh_1", Vehicle.class);

		VehicleType type = veh.getFactory().createVehicleType(vehTypeId1);
		VehicleCapacity cap = veh.getFactory().createVehicleCapacity();
		cap.setSeats(50);
		cap.setStandingRoom(0);
		type.setCapacity(cap);
		type.setLength(10);
		type.setAccessTime(1.0);
		type.setEgressTime(0.75);
		type.setDoorOperationMode(DoorOperationMode.serial);
		
		type.setMaximumVelocity(8.4);
		type.setPcuEquivalents(7.5);
		
		veh.addVehicleType(type); 
		
		Vehicle vehicle1 = veh.getFactory().createVehicle(vehId1, veh.getVehicleTypes().get(vehTypeId1));
		veh.addVehicle( vehicle1);
	}
	
	private void setPopulationTestWaitingDelay06(Scenario scenario) {
		
		Population population = scenario.getPopulation();
        PopulationFactory popFactory = (PopulationFactory) scenario.getPopulation().getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();
		
		//The network, coordinates and activities are adjusted here
		//for simulating the outward journey and return journey
		//and the operation over several trips of a car

		Coord coord1 = new Coord(500.0, 0.0);
		Coord coord2 = new Coord(1000.0, 0.0);
		Coord coord3 = new Coord(1500.0, 0.0);
		Coord coord4 = new Coord(2000.0, 0.0);
		Coord coord5 = new Coord(2500.0, 0.0);
		Coord coord6 = new Coord(3000.0, 0.0);
		Coord coord1back = new Coord(500.0, 0.0);
		Coord coord2back = new Coord(1000.0, 0.0);
		Coord coord3back = new Coord(1500.0, 0.0);
		Coord coord4back = new Coord(2000.0, 0.0);
		Coord coord5back = new Coord(2500.0, 0.0);
		Coord coord6back = new Coord(3000.0, 0.0);
		
		Activity lastActLink2 = popFactory.createActivityFromCoord("w", coord2);
		Activity lastActLink3 = popFactory.createActivityFromCoord("w", coord3);
		Activity lastActLink4 = popFactory.createActivityFromCoord("w", coord4);
		Activity lastActLink5 = popFactory.createActivityFromCoord("w", coord5);
		Activity lastActLink6 = popFactory.createActivityFromCoord("w", coord6);
		Activity lastActLink1back = popFactory.createActivityFromCoord("w", coord1back);
		Activity lastActLink2back = popFactory.createActivityFromCoord("w", coord2back);
		Activity lastActLink3back = popFactory.createActivityFromCoord("w", coord3back);
		Activity lastActLink4back = popFactory.createActivityFromCoord("w", coord4back);
		Activity lastActLink5back = popFactory.createActivityFromCoord("w", coord5back);
		
		Leg leg_1_2 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds12 = new ArrayList<Id<Link>>();
		NetworkRoute route12 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId2);
		route12.setLinkIds(linkId1, linkIds12, linkId2);
		leg_1_2.setRoute(route12);
		
		Leg leg_1_3 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds13 = new ArrayList<Id<Link>>();
		linkIds13.add(linkId2);
		NetworkRoute route13 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId3);
		route13.setLinkIds(linkId1, linkIds13, linkId3);
		leg_1_3.setRoute(route13);
		
		Leg leg_1_4 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds14 = new ArrayList<Id<Link>>();
		linkIds14.add(linkId2);
		linkIds14.add(linkId3);
		NetworkRoute route14 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId4);
		route14.setLinkIds(linkId1, linkIds14, linkId4);
		leg_1_4.setRoute(route14);
		
		Leg leg_1_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds15 = new ArrayList<Id<Link>>();
		linkIds15.add(linkId2);
		linkIds15.add(linkId3);
		linkIds15.add(linkId4);
		NetworkRoute route15 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId5);
		route15.setLinkIds(linkId1, linkIds15, linkId5);
		leg_1_5.setRoute(route15);
		
		Leg leg_1_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds16 = new ArrayList<Id<Link>>();
		linkIds16.add(linkId2);
		linkIds16.add(linkId3);
		linkIds16.add(linkId4);
		linkIds16.add(linkId5);
		NetworkRoute route16 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId6);
		route16.setLinkIds(linkId1, linkIds16, linkId6);
		leg_1_6.setRoute(route16);
		
		Leg leg_2_3 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds23 = new ArrayList<Id<Link>>();
		NetworkRoute route23 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId3);
		route23.setLinkIds(linkId2, linkIds23, linkId3);
		leg_2_3.setRoute(route23);
		
		Leg leg_2_4 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds24 = new ArrayList<Id<Link>>();
		linkIds24.add(linkId3);
		NetworkRoute route24 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId4);
		route24.setLinkIds(linkId2, linkIds24, linkId4);
		leg_2_4.setRoute(route24);
		
		Leg leg_2_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds25 = new ArrayList<Id<Link>>();
		linkIds25.add(linkId3);
		linkIds25.add(linkId4);
		NetworkRoute route25 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId5);
		route25.setLinkIds(linkId2, linkIds25, linkId5);
		leg_2_5.setRoute(route25);
		
		Leg leg_2_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds26 = new ArrayList<Id<Link>>();
		linkIds26.add(linkId3);
		linkIds26.add(linkId4);
		linkIds26.add(linkId5);
		NetworkRoute route26 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId6);
		route26.setLinkIds(linkId2, linkIds26, linkId6);
		leg_2_6.setRoute(route26);
		
		Leg leg_3_4 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds34 = new ArrayList<Id<Link>>();
		NetworkRoute route34 = (NetworkRoute) routeFactory.createRoute(linkId3, linkId4);
		route34.setLinkIds(linkId3, linkIds34, linkId4);
		leg_3_4.setRoute(route34);
		
		Leg leg_3_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds35 = new ArrayList<Id<Link>>();
		linkIds35.add(linkId4);
		NetworkRoute route35 = (NetworkRoute) routeFactory.createRoute(linkId3, linkId5);
		route35.setLinkIds(linkId3, linkIds35, linkId5);
		leg_3_5.setRoute(route35);
		
		Leg leg_3_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds36 = new ArrayList<Id<Link>>();
		linkIds36.add(linkId4);
		linkIds36.add(linkId5);
		NetworkRoute route36 = (NetworkRoute) routeFactory.createRoute(linkId3, linkId6);
		route36.setLinkIds(linkId3, linkIds36, linkId6);
		leg_3_6.setRoute(route36);
		
		Leg leg_4_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds45 = new ArrayList<Id<Link>>();
		NetworkRoute route45 = (NetworkRoute) routeFactory.createRoute(linkId4, linkId5);
		route45.setLinkIds(linkId4, linkIds45, linkId5);
		leg_4_5.setRoute(route45);
		
		Leg leg_4_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds46 = new ArrayList<Id<Link>>();
		linkIds46.add(linkId5);
		NetworkRoute route46 = (NetworkRoute) routeFactory.createRoute(linkId4, linkId6);
		route46.setLinkIds(linkId4, linkIds46, linkId6);
		leg_4_6.setRoute(route46);
		
		Leg leg_5_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds56 = new ArrayList<Id<Link>>();
		NetworkRoute route56 = (NetworkRoute) routeFactory.createRoute(linkId5, linkId6);
		route56.setLinkIds(linkId5, linkIds56, linkId6);
		leg_5_6.setRoute(route56);
		
		Leg leg_5_2 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds710 = new ArrayList<Id<Link>>();
		linkIds710.add(linkId8);
		linkIds710.add(linkId9);
		NetworkRoute route52 = (NetworkRoute) routeFactory.createRoute(linkId7, linkId10);
		route52.setLinkIds(linkId7, linkIds710, linkId10);
		leg_5_2.setRoute(route52);
		
		Person person1 = popFactory.createPerson(testAgent1);
		Plan plan1 = popFactory.createPlan();
		Activity act1 = popFactory.createActivityFromCoord("h", coord2);
		act1.setEndTime(99);
		plan1.addActivity(act1);
	//	Leg leg1 = popFactory.createLeg("pt");
		plan1.addLeg(leg_2_5);
	//	lastActLink5.getCoord().setXY(2500.0, 0.0);
		lastActLink5.setType("w");
		plan1.addActivity(lastActLink5);
		person1.addPlan(plan1);
		
		Person person2 = popFactory.createPerson(testAgent2);
		Plan plan2 = popFactory.createPlan();
		Activity act2 = popFactory.createActivityFromCoord("h", coord5back);
		act2.setEndTime(699);
		plan2.addActivity(act2);
	//	Leg leg2 = popFactory.createLeg("pt");
		plan2.addLeg(leg_5_2);
	//	lastActLink2back.getCoord().setXY(1000.0, 0.0);
		lastActLink2back.setType("w");
		plan2.addActivity(lastActLink2back);
		person2.addPlan(plan2);
		
		Person person3 = popFactory.createPerson(testAgent3);
		Plan plan3 = popFactory.createPlan();
		Activity act3 = popFactory.createActivityFromCoord("h", coord2);
		act3.setEndTime(1299);
		plan3.addActivity(act3);
	//	Leg leg3 = popFactory.createLeg("pt");
		plan3.addLeg(leg_2_5);
	//	lastActLink5.getCoord().setXY(2500.0, 0.0);
		lastActLink5.setType("w");
		plan3.addActivity(lastActLink5);
		person3.addPlan(plan3);
		
		population.addPerson(person1);
		population.addPerson(person2);
		population.addPerson(person3);
		
	}
	
	private void fillScheduleTestWaitingDelay06(MutableScenario scenario) {
		TransitSchedule schedule = scenario.getTransitSchedule();
		
		TransitScheduleFactory sf = schedule.getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();
		
		Id<TransitStopFacility> Stop1 = Id.create("Stop1Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop2 = Id.create("Stop2Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop3 = Id.create("Stop3Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop4 = Id.create("Stop4Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop5 = Id.create("Stop5Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop6 = Id.create("Stop6Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop7 = Id.create("Stop7Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop8 = Id.create("Stop8Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop9 = Id.create("Stop9Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop10 = Id.create("Stop10Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop11 = Id.create("Stop11Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop12 = Id.create("Stop12Id", TransitStopFacility.class);
		
		TransitStopFacility StopFacility1;
		TransitStopFacility StopFacility2;
		TransitStopFacility StopFacility3;
		TransitStopFacility StopFacility4;
		TransitStopFacility StopFacility5;
		TransitStopFacility StopFacility6;
		
		TransitStopFacility StopFacility7;
		TransitStopFacility StopFacility8;
		TransitStopFacility StopFacility9;
		TransitStopFacility StopFacility10;
		TransitStopFacility StopFacility11;
		TransitStopFacility StopFacility12;

		StopFacility1 = sf.createTransitStopFacility(Stop1, new Coord(500., 0.), true);
		StopFacility1.setLinkId(linkId1);
		StopFacility2 = sf.createTransitStopFacility(Stop2, new Coord(1000., 0.), true);
		StopFacility2.setLinkId(linkId2);
		StopFacility3 = sf.createTransitStopFacility(Stop3, new Coord(1500., 0.), true);
		StopFacility3.setLinkId(linkId3);
		StopFacility4 = sf.createTransitStopFacility(Stop4, new Coord(2000., 0.), true);
		StopFacility4.setLinkId(linkId4);
		StopFacility5 = sf.createTransitStopFacility(Stop5, new Coord(2500., 0.), true);
		StopFacility5.setLinkId(linkId5);
		StopFacility6 = sf.createTransitStopFacility(Stop6, new Coord(3000., 0.), true);
		StopFacility6.setLinkId(linkId6);

		StopFacility7 = sf.createTransitStopFacility(Stop7, new Coord(2501., 0.), true);
		StopFacility7.setLinkId(linkId7);
		StopFacility8 = sf.createTransitStopFacility(Stop8, new Coord(2001., 0.), true);
		StopFacility8.setLinkId(linkId8);
		StopFacility9 = sf.createTransitStopFacility(Stop9, new Coord(1501., 0.), true);
		StopFacility9.setLinkId(linkId9);
		StopFacility10 = sf.createTransitStopFacility(Stop10, new Coord(1001., 0.), true);
		StopFacility10.setLinkId(linkId10);
		StopFacility11 = sf.createTransitStopFacility(Stop11, new Coord(501., 0.), true);
		StopFacility11.setLinkId(linkId11);
		StopFacility12 = sf.createTransitStopFacility(Stop12, new Coord(1., 0.), true);
		StopFacility12.setLinkId(linkId12);
		
		TransitLine TransitLine1 = null;
		Id<TransitLine> Line1 = Id.create("line1Id", TransitLine.class);
		TransitLine1 = sf.createTransitLine(Line1);
		
		TransitLine TransitLine2 = null;
		Id<TransitLine> Line2 = Id.create("line2Id", TransitLine.class);
		TransitLine2 = sf.createTransitLine(Line2);
		
		Id<TransitRoute> Route1 = Id.create("Route1Id", TransitRoute.class);
		NetworkRoute NetworkRoute1 = null;
		NetworkRoute1 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId7);
		List<Id<Link>> LinkIds1 = new ArrayList<Id<Link>>();
		LinkIds1.add(linkId2);
		LinkIds1.add(linkId3);
		LinkIds1.add(linkId4);
		LinkIds1.add(linkId5);
		LinkIds1.add(linkId6);
		NetworkRoute1.setLinkIds(linkId1, LinkIds1, linkId7);
		
		Id<TransitRoute> Route2 = Id.create("Route2Id", TransitRoute.class);
		NetworkRoute NetworkRoute2 = null;
		NetworkRoute2 = (NetworkRoute) routeFactory.createRoute(linkId7, linkId1);
		List<Id<Link>> LinkIds2 = new ArrayList<Id<Link>>();
		LinkIds2.add(linkId8);
		LinkIds2.add(linkId9);
		LinkIds2.add(linkId10);
		LinkIds2.add(linkId11);
		LinkIds2.add(linkId12);
		NetworkRoute2.setLinkIds(linkId7, LinkIds2, linkId1);
		
		TransitRouteStop TRStop1;
		TransitRouteStop TRStop2;
		TransitRouteStop TRStop3;
		TransitRouteStop TRStop4;
		TransitRouteStop TRStop5;
		TransitRouteStop TRStop6;
		
		TransitRouteStop TRStop7;
		TransitRouteStop TRStop8;
		TransitRouteStop TRStop9;
		TransitRouteStop TRStop10;
		TransitRouteStop TRStop11;
		TransitRouteStop TRStop12;
		
		TRStop1 = sf.createTransitRouteStop(StopFacility1, 0.0, 0.0);
		TRStop2 = sf.createTransitRouteStop(StopFacility2, 60.0, 60.0);
		TRStop3 = sf.createTransitRouteStop(StopFacility3, 120.0, 120.0);
		TRStop4 = sf.createTransitRouteStop(StopFacility4, 180.0, 180.0);
		TRStop5 = sf.createTransitRouteStop(StopFacility5, 240.0, 240.0);
		TRStop6 = sf.createTransitRouteStop(StopFacility6, 300.0, 300.0);
		
		TRStop7 = sf.createTransitRouteStop(StopFacility7, 0.0, 0.0);
		TRStop8 = sf.createTransitRouteStop(StopFacility8, 60.0, 60.0);
		TRStop9 = sf.createTransitRouteStop(StopFacility9, 120.0, 120.0);
		TRStop10 = sf.createTransitRouteStop(StopFacility10, 180.0, 180.0);
		TRStop11 = sf.createTransitRouteStop(StopFacility11, 240.0, 240.0);
		TRStop12 = sf.createTransitRouteStop(StopFacility12, 300.0, 300.0);
		
		List<TransitRouteStop> Stops1 = new ArrayList<TransitRouteStop>();
		Stops1.add(TRStop1);
		Stops1.add(TRStop2);
		Stops1.add(TRStop3);
		Stops1.add(TRStop4);
		Stops1.add(TRStop5);
		Stops1.add(TRStop6);
		
		List<TransitRouteStop> Stops2 = new ArrayList<TransitRouteStop>();
		Stops2.add(TRStop7);
		Stops2.add(TRStop8);
		Stops2.add(TRStop9);
		Stops2.add(TRStop10);
		Stops2.add(TRStop11);
		Stops2.add(TRStop12);
		
		TransitRoute TransitRoute1 = null;
		TransitRoute1 = sf.createTransitRoute(Route1, NetworkRoute1, Stops1, "pt");
		
		TransitRoute TransitRoute2 = null;
		TransitRoute2 = sf.createTransitRoute(Route2, NetworkRoute2, Stops2, "pt");
		
		Departure Departure1 = null;
		Id<Departure> DepartureId1 = Id.create("DepartureId_1", Departure.class);
		Departure1 = sf.createDeparture(DepartureId1, 200);
		Id<Vehicle> veh_1 = Id.create("veh_1", Vehicle.class);
		Departure1.setVehicleId(veh_1);
		
		Departure Departure2 = null;
		Id<Departure> DepartureId2 = Id.create("DepartureId_2", Departure.class);
		Departure2 = sf.createDeparture(DepartureId2, 800);
		Departure2.setVehicleId(veh_1);
		
		Departure Departure3 = null;
		Id<Departure> DepartureId3 = Id.create("DepartureId_3", Departure.class);
		Departure3 = sf.createDeparture(DepartureId3, 1400);
		Departure3.setVehicleId(veh_1);
		
		TransitRoute1.addDeparture(Departure1);
		TransitRoute2.addDeparture(Departure2);
		TransitRoute1.addDeparture(Departure3);
		
		TransitLine1.addRoute(TransitRoute1);
		TransitLine2.addRoute(TransitRoute2);
		
		schedule.addTransitLine(TransitLine1);
		schedule.addTransitLine(TransitLine2);
		
		schedule.addStopFacility(StopFacility1);
		schedule.addStopFacility(StopFacility2);
		schedule.addStopFacility(StopFacility3);
		schedule.addStopFacility(StopFacility4);
		schedule.addStopFacility(StopFacility5);
		schedule.addStopFacility(StopFacility6);
		
		schedule.addStopFacility(StopFacility7);
		schedule.addStopFacility(StopFacility8);
		schedule.addStopFacility(StopFacility9);
		schedule.addStopFacility(StopFacility10);
		schedule.addStopFacility(StopFacility11);
		schedule.addStopFacility(StopFacility12);
		
	}
	
	private void fillVehicleTestWaitingDelay06(MutableScenario scenario) {
		Vehicles veh = scenario.getTransitVehicles();

		Id<VehicleType> vehTypeId1 = Id.create("type_1", VehicleType.class);
		Id<Vehicle> vehId1 = Id.create("veh_1", Vehicle.class);
		Id<Vehicle> vehId2 = Id.create("veh_2", Vehicle.class);
		Id<Vehicle> vehId3 = Id.create("veh_3", Vehicle.class);

		VehicleType type = veh.getFactory().createVehicleType(vehTypeId1);
		VehicleCapacity cap = veh.getFactory().createVehicleCapacity();
		cap.setSeats(50);
		cap.setStandingRoom(0);
		type.setCapacity(cap);
		type.setLength(10);
		type.setAccessTime(1.0);
		type.setEgressTime(0.75);
		type.setDoorOperationMode(DoorOperationMode.serial);
		
		type.setMaximumVelocity(8.4);
		type.setPcuEquivalents(7.5);
		
		veh.addVehicleType( type); 
		
		Vehicle vehicle1 = veh.getFactory().createVehicle(vehId1, veh.getVehicleTypes().get(vehTypeId1));
		veh.addVehicle( vehicle1);
		
		Vehicle vehicle2 = veh.getFactory().createVehicle(vehId2, veh.getVehicleTypes().get(vehTypeId1));
		veh.addVehicle( vehicle2);
		
		Vehicle vehicle3 = veh.getFactory().createVehicle(vehId3, veh.getVehicleTypes().get(vehTypeId1));
		veh.addVehicle( vehicle3);
	}
	
	private void setPopulationTestCapacityDelay01(Scenario scenario) {
		
		Population population = scenario.getPopulation();
        PopulationFactory popFactory = (PopulationFactory) scenario.getPopulation().getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();

		Coord coord1 = new Coord(500.0, 0.0);
		Coord coord2 = new Coord(1000.0, 0.0);
		Coord coord3 = new Coord(1500.0, 0.0);
		Coord coord4 = new Coord(2000.0, 0.0);
		Coord coord5 = new Coord(2500.0, 0.0);
		Coord coord6 = new Coord(3000.0, 0.0);
		
		Activity lastActLink2 = popFactory.createActivityFromCoord("w", coord2);
		Activity lastActLink3 = popFactory.createActivityFromCoord("w", coord3);
		Activity lastActLink4 = popFactory.createActivityFromCoord("w", coord4);
		Activity lastActLink5 = popFactory.createActivityFromCoord("w", coord5);
		Activity lastActLink6 = popFactory.createActivityFromCoord("w", coord6);
		
		Leg leg_1_2 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds12 = new ArrayList<Id<Link>>();
		NetworkRoute route12 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId2);
		route12.setLinkIds(linkId1, linkIds12, linkId2);
		leg_1_2.setRoute(route12);
		
		Leg leg_1_3 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds13 = new ArrayList<Id<Link>>();
		linkIds13.add(linkId2);
		NetworkRoute route13 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId3);
		route13.setLinkIds(linkId1, linkIds13, linkId3);
		leg_1_3.setRoute(route13);
		
		Leg leg_1_4 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds14 = new ArrayList<Id<Link>>();
		linkIds14.add(linkId2);
		linkIds14.add(linkId3);
		NetworkRoute route14 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId4);
		route14.setLinkIds(linkId1, linkIds14, linkId4);
		leg_1_4.setRoute(route14);
		
		Leg leg_1_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds15 = new ArrayList<Id<Link>>();
		linkIds15.add(linkId2);
		linkIds15.add(linkId3);
		linkIds15.add(linkId4);
		NetworkRoute route15 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId5);
		route15.setLinkIds(linkId1, linkIds15, linkId5);
		leg_1_5.setRoute(route15);
		
		Leg leg_1_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds16 = new ArrayList<Id<Link>>();
		linkIds16.add(linkId2);
		linkIds16.add(linkId3);
		linkIds16.add(linkId4);
		linkIds16.add(linkId5);
		NetworkRoute route16 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId6);
		route16.setLinkIds(linkId1, linkIds16, linkId6);
		leg_1_6.setRoute(route16);
		
		Leg leg_2_3 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds23 = new ArrayList<Id<Link>>();
		NetworkRoute route23 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId3);
		route23.setLinkIds(linkId2, linkIds23, linkId3);
		leg_2_3.setRoute(route23);
		
		Leg leg_2_4 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds24 = new ArrayList<Id<Link>>();
		linkIds24.add(linkId3);
		NetworkRoute route24 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId4);
		route24.setLinkIds(linkId2, linkIds24, linkId4);
		leg_2_4.setRoute(route24);
		
		Leg leg_2_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds25 = new ArrayList<Id<Link>>();
		linkIds25.add(linkId3);
		linkIds25.add(linkId4);
		NetworkRoute route25 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId5);
		route25.setLinkIds(linkId2, linkIds25, linkId5);
		leg_2_5.setRoute(route25);
		
		Leg leg_2_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds26 = new ArrayList<Id<Link>>();
		linkIds26.add(linkId3);
		linkIds26.add(linkId4);
		linkIds26.add(linkId5);
		NetworkRoute route26 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId6);
		route26.setLinkIds(linkId2, linkIds26, linkId6);
		leg_2_6.setRoute(route26);
		
		Leg leg_3_4 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds34 = new ArrayList<Id<Link>>();
		NetworkRoute route34 = (NetworkRoute) routeFactory.createRoute(linkId3, linkId4);
		route34.setLinkIds(linkId3, linkIds34, linkId4);
		leg_3_4.setRoute(route34);
		
		Leg leg_3_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds35 = new ArrayList<Id<Link>>();
		linkIds35.add(linkId4);
		NetworkRoute route35 = (NetworkRoute) routeFactory.createRoute(linkId3, linkId5);
		route35.setLinkIds(linkId3, linkIds35, linkId5);
		leg_3_5.setRoute(route35);
		
		Leg leg_3_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds36 = new ArrayList<Id<Link>>();
		linkIds36.add(linkId4);
		linkIds36.add(linkId5);
		NetworkRoute route36 = (NetworkRoute) routeFactory.createRoute(linkId3, linkId6);
		route36.setLinkIds(linkId3, linkIds36, linkId6);
		leg_3_6.setRoute(route36);
		
		Leg leg_4_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds45 = new ArrayList<Id<Link>>();
		NetworkRoute route45 = (NetworkRoute) routeFactory.createRoute(linkId4, linkId5);
		route45.setLinkIds(linkId4, linkIds45, linkId5);
		leg_4_5.setRoute(route45);
		
		Leg leg_4_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds46 = new ArrayList<Id<Link>>();
		linkIds46.add(linkId5);
		NetworkRoute route46 = (NetworkRoute) routeFactory.createRoute(linkId4, linkId6);
		route46.setLinkIds(linkId4, linkIds46, linkId6);
		leg_4_6.setRoute(route46);
		
		Leg leg_5_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds56 = new ArrayList<Id<Link>>();
		NetworkRoute route56 = (NetworkRoute) routeFactory.createRoute(linkId5, linkId6);
		route56.setLinkIds(linkId5, linkIds56, linkId6);
		leg_5_6.setRoute(route56);
		
		Person person1 = popFactory.createPerson(testAgent1);
		Plan plan1 = popFactory.createPlan();
		Activity act1 = popFactory.createActivityFromCoord("h", coord1);
		act1.setEndTime(99);
		plan1.addActivity(act1);
	//	Leg leg1 = popFactory.createLeg("pt");
		plan1.addLeg(leg_1_6);
	//	lastActLink6.getCoord().setXY(3000.0, 0.0);
		lastActLink6.setType("w");
		plan1.addActivity(lastActLink6);
		person1.addPlan(plan1);
		
		Person person2 = popFactory.createPerson(testAgent2);
		Plan plan2 = popFactory.createPlan();
		Activity act2 = popFactory.createActivityFromCoord("h", coord1);
		act2.setEndTime(100);
		plan2.addActivity(act2);
	//	Leg leg2 = popFactory.createLeg("pt");
		plan2.addLeg(leg_1_6);
	//	lastActLink6.getCoord().setXY(3000.0, 0.0);
		lastActLink6.setType("w");
		plan2.addActivity(lastActLink6);
		person2.addPlan(plan2);
		
		Person person3 = popFactory.createPerson(testAgent3);
		Plan plan3 = popFactory.createPlan();
		Activity act3 = popFactory.createActivityFromCoord("h", coord2);
		act3.setEndTime(99);
		plan3.addActivity(act3);
	//	Leg leg3 = popFactory.createLeg("pt");
		plan3.addLeg(leg_2_6);
	//	lastActLink6.getCoord().setXY(3000.0, 0.0);
		lastActLink6.setType("w");
		plan3.addActivity(lastActLink6);
		person3.addPlan(plan3);
		
		population.addPerson(person1);
		population.addPerson(person2);
		population.addPerson(person3);
		
	}

	private void fillScheduleTestCapacityDelay01(MutableScenario scenario) {
		TransitSchedule schedule = scenario.getTransitSchedule();
		
		TransitScheduleFactory sf = schedule.getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();
		
		Id<TransitStopFacility> Stop1 = Id.create("Stop1Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop2 = Id.create("Stop2Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop3 = Id.create("Stop3Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop4 = Id.create("Stop4Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop5 = Id.create("Stop5Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop6 = Id.create("Stop6Id", TransitStopFacility.class);
		TransitStopFacility StopFacility1;
		TransitStopFacility StopFacility2;
		TransitStopFacility StopFacility3;
		TransitStopFacility StopFacility4;
		TransitStopFacility StopFacility5;
		TransitStopFacility StopFacility6;
		StopFacility1 = sf.createTransitStopFacility(Stop1, new Coord(500., 0.), true);
		StopFacility1.setLinkId(linkId1);
		StopFacility2 = sf.createTransitStopFacility(Stop2, new Coord(1000., 0.), true);
		StopFacility2.setLinkId(linkId2);
		StopFacility3 = sf.createTransitStopFacility(Stop3, new Coord(1500., 0.), true);
		StopFacility3.setLinkId(linkId3);
		StopFacility4 = sf.createTransitStopFacility(Stop4, new Coord(2000., 0.), true);
		StopFacility4.setLinkId(linkId4);
		StopFacility5 = sf.createTransitStopFacility(Stop5, new Coord(2500., 0.), true);
		StopFacility5.setLinkId(linkId5);
		StopFacility6 = sf.createTransitStopFacility(Stop6, new Coord(3000., 0.), true);
		StopFacility6.setLinkId(linkId6);
		
		TransitLine TransitLine1 = null;
		Id<TransitLine> Line1 = Id.create("line1Id", TransitLine.class);
		TransitLine1 = sf.createTransitLine(Line1);
		
		List<Id<TransitLine>> lineIds = new ArrayList<Id<TransitLine>>();
		lineIds.add(Line1);
		
		Id<TransitRoute> Route1 = Id.create("Route1Id", TransitRoute.class);
		NetworkRoute NetworkRoute1 = null;
		NetworkRoute1 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId6);
		List<Id<Link>> LinkIds = new ArrayList<Id<Link>>();
		LinkIds.add(linkId2);
		LinkIds.add(linkId3);
		LinkIds.add(linkId4);
		LinkIds.add(linkId5);
		NetworkRoute1.setLinkIds(linkId1, LinkIds, linkId6);
		
		TransitRouteStop TRStop1;
		TransitRouteStop TRStop2;
		TransitRouteStop TRStop3;
		TransitRouteStop TRStop4;
		TransitRouteStop TRStop5;
		TransitRouteStop TRStop6;
		TRStop1 = sf.createTransitRouteStop(StopFacility1, 0.0, 0.0);
		TRStop2 = sf.createTransitRouteStop(StopFacility2, 60.0, 60.0);
		TRStop3 = sf.createTransitRouteStop(StopFacility3, 120.0, 120.0);
		TRStop4 = sf.createTransitRouteStop(StopFacility4, 180.0, 180.0);
		TRStop5 = sf.createTransitRouteStop(StopFacility5, 240.0, 240.0);
		TRStop6 = sf.createTransitRouteStop(StopFacility6, 300.0, 300.0);
		
		List<TransitRouteStop> Stops = new ArrayList<TransitRouteStop>();
		Stops.add(TRStop1);
		Stops.add(TRStop2);
		Stops.add(TRStop3);
		Stops.add(TRStop4);
		Stops.add(TRStop5);
		Stops.add(TRStop6);
		
		TransitRoute TransitRoute1 = null;
		TransitRoute1 = sf.createTransitRoute(Route1, NetworkRoute1, Stops, "pt");
		
		Departure Departure1 = null;
		Id<Departure> DepartureId1 = Id.create("DepartureId_1", Departure.class);
		Departure1 = sf.createDeparture(DepartureId1, 200);
		Id<Vehicle> veh_1 = Id.create("veh_1", Vehicle.class);
		Departure1.setVehicleId(veh_1);
		
		Departure Departure2 = null;
		Id<Departure> DepartureId2 = Id.create("DepartureId_2", Departure.class);
		Departure2 = sf.createDeparture(DepartureId2, 600);
		Id<Vehicle> veh_2 = Id.create("veh_2", Vehicle.class);
		Departure2.setVehicleId(veh_2);
		
		Departure Departure3 = null;
		Id<Departure> DepartureId3 = Id.create("DepartureId_3", Departure.class);
		Departure3 = sf.createDeparture(DepartureId3, 800);
		Id<Vehicle> veh_3 = Id.create("veh_3", Vehicle.class);
		Departure3.setVehicleId(veh_3);
		
		TransitRoute1.addDeparture(Departure1);
		TransitRoute1.addDeparture(Departure2);
		TransitRoute1.addDeparture(Departure3);
		
		TransitLine1.addRoute(TransitRoute1);
		
		schedule.addTransitLine(TransitLine1);
		schedule.addStopFacility(StopFacility1);
		schedule.addStopFacility(StopFacility2);
		schedule.addStopFacility(StopFacility3);
		schedule.addStopFacility(StopFacility4);
		schedule.addStopFacility(StopFacility5);
		schedule.addStopFacility(StopFacility6);
		
	}
	
	private void fillVehicleTestCapacityDelay01(MutableScenario scenario) {
		Vehicles veh = scenario.getTransitVehicles();

		Id<VehicleType> vehTypeId1 = Id.create("type_1", VehicleType.class);
		Id<Vehicle> vehId1 = Id.create("veh_1", Vehicle.class);
		Id<Vehicle> vehId2 = Id.create("veh_2", Vehicle.class);
		Id<Vehicle> vehId3 = Id.create("veh_3", Vehicle.class);

		VehicleType type = veh.getFactory().createVehicleType(vehTypeId1);
		VehicleCapacity cap = veh.getFactory().createVehicleCapacity();
		cap.setSeats(2);
		cap.setStandingRoom(0);
		type.setCapacity(cap);
		type.setLength(10);
		type.setAccessTime(1.0);
		type.setEgressTime(0.75);
		type.setDoorOperationMode(DoorOperationMode.serial);
		
		type.setMaximumVelocity(8.4);
		type.setPcuEquivalents(7.5);
		
		veh.addVehicleType(type); 
		
		Vehicle vehicle1 = veh.getFactory().createVehicle(vehId1, veh.getVehicleTypes().get(vehTypeId1));
		veh.addVehicle( vehicle1);
		
		Vehicle vehicle2 = veh.getFactory().createVehicle(vehId2, veh.getVehicleTypes().get(vehTypeId1));
		veh.addVehicle( vehicle2);
		
		Vehicle vehicle3 = veh.getFactory().createVehicle(vehId3, veh.getVehicleTypes().get(vehTypeId1));
		veh.addVehicle( vehicle3);
	}
	
	private void setPopulationTestCapacityDelay02(Scenario scenario) {
		
		Population population = scenario.getPopulation();
        PopulationFactory popFactory = (PopulationFactory) scenario.getPopulation().getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();

		Coord coord1 = new Coord(500.0, 0.0);
		Coord coord2 = new Coord(1000.0, 0.0);
		Coord coord3 = new Coord(1500.0, 0.0);
		Coord coord4 = new Coord(2000.0, 0.0);
		Coord coord5 = new Coord(2500.0, 0.0);
		Coord coord6 = new Coord(3000.0, 0.0);
		
		Activity lastActLink2 = popFactory.createActivityFromCoord("w", coord2);
		Activity lastActLink3 = popFactory.createActivityFromCoord("w", coord3);
		Activity lastActLink4 = popFactory.createActivityFromCoord("w", coord4);
		Activity lastActLink5 = popFactory.createActivityFromCoord("w", coord5);
		Activity lastActLink6 = popFactory.createActivityFromCoord("w", coord6);
		
		Leg leg_1_2 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds12 = new ArrayList<Id<Link>>();
		NetworkRoute route12 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId2);
		route12.setLinkIds(linkId1, linkIds12, linkId2);
		leg_1_2.setRoute(route12);
		
		Leg leg_1_3 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds13 = new ArrayList<Id<Link>>();
		linkIds13.add(linkId2);
		NetworkRoute route13 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId3);
		route13.setLinkIds(linkId1, linkIds13, linkId3);
		leg_1_3.setRoute(route13);
		
		Leg leg_1_4 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds14 = new ArrayList<Id<Link>>();
		linkIds14.add(linkId2);
		linkIds14.add(linkId3);
		NetworkRoute route14 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId4);
		route14.setLinkIds(linkId1, linkIds14, linkId4);
		leg_1_4.setRoute(route14);
		
		Leg leg_1_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds15 = new ArrayList<Id<Link>>();
		linkIds15.add(linkId2);
		linkIds15.add(linkId3);
		linkIds15.add(linkId4);
		NetworkRoute route15 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId5);
		route15.setLinkIds(linkId1, linkIds15, linkId5);
		leg_1_5.setRoute(route15);
		
		Leg leg_1_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds16 = new ArrayList<Id<Link>>();
		linkIds16.add(linkId2);
		linkIds16.add(linkId3);
		linkIds16.add(linkId4);
		linkIds16.add(linkId5);
		NetworkRoute route16 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId6);
		route16.setLinkIds(linkId1, linkIds16, linkId6);
		leg_1_6.setRoute(route16);
		
		Leg leg_2_3 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds23 = new ArrayList<Id<Link>>();
		NetworkRoute route23 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId3);
		route23.setLinkIds(linkId2, linkIds23, linkId3);
		leg_2_3.setRoute(route23);
		
		Leg leg_2_4 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds24 = new ArrayList<Id<Link>>();
		linkIds24.add(linkId3);
		NetworkRoute route24 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId4);
		route24.setLinkIds(linkId2, linkIds24, linkId4);
		leg_2_4.setRoute(route24);
		
		Leg leg_2_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds25 = new ArrayList<Id<Link>>();
		linkIds25.add(linkId3);
		linkIds25.add(linkId4);
		NetworkRoute route25 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId5);
		route25.setLinkIds(linkId2, linkIds25, linkId5);
		leg_2_5.setRoute(route25);
		
		Leg leg_2_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds26 = new ArrayList<Id<Link>>();
		linkIds26.add(linkId3);
		linkIds26.add(linkId4);
		linkIds26.add(linkId5);
		NetworkRoute route26 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId6);
		route26.setLinkIds(linkId2, linkIds26, linkId6);
		leg_2_6.setRoute(route26);
		
		Leg leg_3_4 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds34 = new ArrayList<Id<Link>>();
		NetworkRoute route34 = (NetworkRoute) routeFactory.createRoute(linkId3, linkId4);
		route34.setLinkIds(linkId3, linkIds34, linkId4);
		leg_3_4.setRoute(route34);
		
		Leg leg_3_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds35 = new ArrayList<Id<Link>>();
		linkIds35.add(linkId4);
		NetworkRoute route35 = (NetworkRoute) routeFactory.createRoute(linkId3, linkId5);
		route35.setLinkIds(linkId3, linkIds35, linkId5);
		leg_3_5.setRoute(route35);
		
		Leg leg_3_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds36 = new ArrayList<Id<Link>>();
		linkIds36.add(linkId4);
		linkIds36.add(linkId5);
		NetworkRoute route36 = (NetworkRoute) routeFactory.createRoute(linkId3, linkId6);
		route36.setLinkIds(linkId3, linkIds36, linkId6);
		leg_3_6.setRoute(route36);
		
		Leg leg_4_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds45 = new ArrayList<Id<Link>>();
		NetworkRoute route45 = (NetworkRoute) routeFactory.createRoute(linkId4, linkId5);
		route45.setLinkIds(linkId4, linkIds45, linkId5);
		leg_4_5.setRoute(route45);
		
		Leg leg_4_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds46 = new ArrayList<Id<Link>>();
		linkIds46.add(linkId5);
		NetworkRoute route46 = (NetworkRoute) routeFactory.createRoute(linkId4, linkId6);
		route46.setLinkIds(linkId4, linkIds46, linkId6);
		leg_4_6.setRoute(route46);
		
		Leg leg_5_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds56 = new ArrayList<Id<Link>>();
		NetworkRoute route56 = (NetworkRoute) routeFactory.createRoute(linkId5, linkId6);
		route56.setLinkIds(linkId5, linkIds56, linkId6);
		leg_5_6.setRoute(route56);
		
		Person person1 = popFactory.createPerson(testAgent1);
		Plan plan1 = popFactory.createPlan();
		Activity act1 = popFactory.createActivityFromCoord("h", coord1);
		act1.setEndTime(99);
		plan1.addActivity(act1);
	//	Leg leg1 = popFactory.createLeg("pt");
		plan1.addLeg(leg_1_6);
	//	lastActLink6.getCoord().setXY(3000.0, 0.0);
		lastActLink6.setType("w");
		plan1.addActivity(lastActLink6);
		person1.addPlan(plan1);
		
		Person person2 = popFactory.createPerson(testAgent2);
		Plan plan2 = popFactory.createPlan();
		Activity act2 = popFactory.createActivityFromCoord("h", coord2);
		act2.setEndTime(99);
		plan2.addActivity(act2);
	//	Leg leg2 = popFactory.createLeg("pt");
		plan2.addLeg(leg_2_6);
	//	lastActLink6.getCoord().setXY(3000.0, 0.0);
		lastActLink6.setType("w");
		plan2.addActivity(lastActLink6);
		person2.addPlan(plan2);
		
		Person person3 = popFactory.createPerson(testAgent3);
		Plan plan3 = popFactory.createPlan();
		Activity act3 = popFactory.createActivityFromCoord("h", coord2);
		act3.setEndTime(100);
		plan3.addActivity(act3);
	//	Leg leg3 = popFactory.createLeg("pt");
		plan3.addLeg(leg_2_6);
	//	lastActLink6.getCoord().setXY(3000.0, 0.0);
		lastActLink6.setType("w");
		plan3.addActivity(lastActLink6);
		person3.addPlan(plan3);
		
		population.addPerson(person1);
		population.addPerson(person2);
		population.addPerson(person3);
		
	}
	
	private void fillScheduleTestCapacityDelay02(MutableScenario scenario) {
		TransitSchedule schedule = scenario.getTransitSchedule();
		
		TransitScheduleFactory sf = schedule.getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();
		
		Id<TransitStopFacility> Stop1 = Id.create("Stop1Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop2 = Id.create("Stop2Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop3 = Id.create("Stop3Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop4 = Id.create("Stop4Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop5 = Id.create("Stop5Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop6 = Id.create("Stop6Id", TransitStopFacility.class);
		TransitStopFacility StopFacility1;
		TransitStopFacility StopFacility2;
		TransitStopFacility StopFacility3;
		TransitStopFacility StopFacility4;
		TransitStopFacility StopFacility5;
		TransitStopFacility StopFacility6;
		StopFacility1 = sf.createTransitStopFacility(Stop1, new Coord(500., 0.), true);
		StopFacility1.setLinkId(linkId1);
		StopFacility2 = sf.createTransitStopFacility(Stop2, new Coord(1000., 0.), true);
		StopFacility2.setLinkId(linkId2);
		StopFacility3 = sf.createTransitStopFacility(Stop3, new Coord(1500., 0.), true);
		StopFacility3.setLinkId(linkId3);
		StopFacility4 = sf.createTransitStopFacility(Stop4, new Coord(2000., 0.), true);
		StopFacility4.setLinkId(linkId4);
		StopFacility5 = sf.createTransitStopFacility(Stop5, new Coord(2500., 0.), true);
		StopFacility5.setLinkId(linkId5);
		StopFacility6 = sf.createTransitStopFacility(Stop6, new Coord(3000., 0.), true);
		StopFacility6.setLinkId(linkId6);
		
		TransitLine TransitLine1 = null;
		Id<TransitLine> Line1 = Id.create("line1Id", TransitLine.class);
		TransitLine1 = sf.createTransitLine(Line1);
		
		List<Id<TransitLine>> lineIds = new ArrayList<Id<TransitLine>>();
		lineIds.add(Line1);
		
		Id<TransitRoute> Route1 = Id.create("Route1Id", TransitRoute.class);
		NetworkRoute NetworkRoute1 = null;
		NetworkRoute1 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId6);
		List<Id<Link>> LinkIds = new ArrayList<Id<Link>>();
		LinkIds.add(linkId2);
		LinkIds.add(linkId3);
		LinkIds.add(linkId4);
		LinkIds.add(linkId5);
		NetworkRoute1.setLinkIds(linkId1, LinkIds, linkId6);
		
		TransitRouteStop TRStop1;
		TransitRouteStop TRStop2;
		TransitRouteStop TRStop3;
		TransitRouteStop TRStop4;
		TransitRouteStop TRStop5;
		TransitRouteStop TRStop6;
		TRStop1 = sf.createTransitRouteStop(StopFacility1, 0.0, 0.0);
		TRStop2 = sf.createTransitRouteStop(StopFacility2, 60.0, 60.0);
		TRStop3 = sf.createTransitRouteStop(StopFacility3, 120.0, 120.0);
		TRStop4 = sf.createTransitRouteStop(StopFacility4, 180.0, 180.0);
		TRStop5 = sf.createTransitRouteStop(StopFacility5, 240.0, 240.0);
		TRStop6 = sf.createTransitRouteStop(StopFacility6, 300.0, 300.0);
		
		List<TransitRouteStop> Stops = new ArrayList<TransitRouteStop>();
		Stops.add(TRStop1);
		Stops.add(TRStop2);
		Stops.add(TRStop3);
		Stops.add(TRStop4);
		Stops.add(TRStop5);
		Stops.add(TRStop6);
		
		TransitRoute TransitRoute1 = null;
		TransitRoute1 = sf.createTransitRoute(Route1, NetworkRoute1, Stops, "pt");
		
		Departure Departure1 = null;
		Id<Departure> DepartureId1 = Id.create("DepartureId_1", Departure.class);
		Departure1 = sf.createDeparture(DepartureId1, 200);
		Id<Vehicle> veh_1 = Id.create("veh_1", Vehicle.class);
		Departure1.setVehicleId(veh_1);
		
		Departure Departure2 = null;
		Id<Departure> DepartureId2 = Id.create("DepartureId_2", Departure.class);
		Departure2 = sf.createDeparture(DepartureId2, 600);
		Id<Vehicle> veh_2 = Id.create("veh_2", Vehicle.class);
		Departure2.setVehicleId(veh_2);
		
		Departure Departure3 = null;
		Id<Departure> DepartureId3 = Id.create("DepartureId_3", Departure.class);
		Departure3 = sf.createDeparture(DepartureId3, 800);
		Id<Vehicle> veh_3 = Id.create("veh_3", Vehicle.class);
		Departure3.setVehicleId(veh_3);
		
		TransitRoute1.addDeparture(Departure1);
		TransitRoute1.addDeparture(Departure2);
		TransitRoute1.addDeparture(Departure3);
		
		TransitLine1.addRoute(TransitRoute1);
		
		schedule.addTransitLine(TransitLine1);
		schedule.addStopFacility(StopFacility1);
		schedule.addStopFacility(StopFacility2);
		schedule.addStopFacility(StopFacility3);
		schedule.addStopFacility(StopFacility4);
		schedule.addStopFacility(StopFacility5);
		schedule.addStopFacility(StopFacility6);
		
	}
	
	private void fillVehicleTestCapacityDelay02(MutableScenario scenario) {
		Vehicles veh = scenario.getTransitVehicles();

		Id<VehicleType> vehTypeId1 = Id.create("type_1", VehicleType.class);
		Id<Vehicle> vehId1 = Id.create("veh_1", Vehicle.class);
		Id<Vehicle> vehId2 = Id.create("veh_2", Vehicle.class);
		Id<Vehicle> vehId3 = Id.create("veh_3", Vehicle.class);

		VehicleType type = veh.getFactory().createVehicleType(vehTypeId1);
		VehicleCapacity cap = veh.getFactory().createVehicleCapacity();
		cap.setSeats(2);
		cap.setStandingRoom(0);
		type.setCapacity(cap);
		type.setLength(10);
		type.setAccessTime(1.0);
		type.setEgressTime(0.75);
		type.setDoorOperationMode(DoorOperationMode.serial);
		
		type.setMaximumVelocity(8.);
		type.setPcuEquivalents(7.5);
		
		veh.addVehicleType(type); 
		
		Vehicle vehicle1 = veh.getFactory().createVehicle(vehId1, veh.getVehicleTypes().get(vehTypeId1));
		veh.addVehicle( vehicle1);
		
		Vehicle vehicle2 = veh.getFactory().createVehicle(vehId2, veh.getVehicleTypes().get(vehTypeId1));
		veh.addVehicle( vehicle2);
		
		Vehicle vehicle3 = veh.getFactory().createVehicle(vehId3, veh.getVehicleTypes().get(vehTypeId1));
		veh.addVehicle( vehicle3);
	}

	private void setPopulationTestCapacityDelay03(Scenario scenario) {
		
		Population population = scenario.getPopulation();
        PopulationFactory popFactory = (PopulationFactory) scenario.getPopulation().getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();

		Coord coord1 = new Coord(500.0, 0.0);
		Coord coord2 = new Coord(1000.0, 0.0);
		Coord coord3 = new Coord(1500.0, 0.0);
		Coord coord4 = new Coord(2000.0, 0.0);
		Coord coord5 = new Coord(2500.0, 0.0);
		Coord coord6 = new Coord(3000.0, 0.0);
		
		Activity lastActLink2 = popFactory.createActivityFromCoord("w", coord2);
		Activity lastActLink3 = popFactory.createActivityFromCoord("w", coord3);
		Activity lastActLink4 = popFactory.createActivityFromCoord("w", coord4);
		Activity lastActLink5 = popFactory.createActivityFromCoord("w", coord5);
		Activity lastActLink6 = popFactory.createActivityFromCoord("w", coord6);
		
		Leg leg_1_2 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds12 = new ArrayList<Id<Link>>();
		NetworkRoute route12 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId2);
		route12.setLinkIds(linkId1, linkIds12, linkId2);
		leg_1_2.setRoute(route12);
		
		Leg leg_1_3 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds13 = new ArrayList<Id<Link>>();
		linkIds13.add(linkId2);
		NetworkRoute route13 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId3);
		route13.setLinkIds(linkId1, linkIds13, linkId3);
		leg_1_3.setRoute(route13);
		
		Leg leg_1_4 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds14 = new ArrayList<Id<Link>>();
		linkIds14.add(linkId2);
		linkIds14.add(linkId3);
		NetworkRoute route14 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId4);
		route14.setLinkIds(linkId1, linkIds14, linkId4);
		leg_1_4.setRoute(route14);
		
		Leg leg_1_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds15 = new ArrayList<Id<Link>>();
		linkIds15.add(linkId2);
		linkIds15.add(linkId3);
		linkIds15.add(linkId4);
		NetworkRoute route15 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId5);
		route15.setLinkIds(linkId1, linkIds15, linkId5);
		leg_1_5.setRoute(route15);
		
		Leg leg_1_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds16 = new ArrayList<Id<Link>>();
		linkIds16.add(linkId2);
		linkIds16.add(linkId3);
		linkIds16.add(linkId4);
		linkIds16.add(linkId5);
		NetworkRoute route16 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId6);
		route16.setLinkIds(linkId1, linkIds16, linkId6);
		leg_1_6.setRoute(route16);
		
		Leg leg_2_3 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds23 = new ArrayList<Id<Link>>();
		NetworkRoute route23 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId3);
		route23.setLinkIds(linkId2, linkIds23, linkId3);
		leg_2_3.setRoute(route23);
		
		Leg leg_2_4 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds24 = new ArrayList<Id<Link>>();
		linkIds24.add(linkId3);
		NetworkRoute route24 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId4);
		route24.setLinkIds(linkId2, linkIds24, linkId4);
		leg_2_4.setRoute(route24);
		
		Leg leg_2_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds25 = new ArrayList<Id<Link>>();
		linkIds25.add(linkId3);
		linkIds25.add(linkId4);
		NetworkRoute route25 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId5);
		route25.setLinkIds(linkId2, linkIds25, linkId5);
		leg_2_5.setRoute(route25);
		
		Leg leg_2_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds26 = new ArrayList<Id<Link>>();
		linkIds26.add(linkId3);
		linkIds26.add(linkId4);
		linkIds26.add(linkId5);
		NetworkRoute route26 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId6);
		route26.setLinkIds(linkId2, linkIds26, linkId6);
		leg_2_6.setRoute(route26);
		
		Leg leg_3_4 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds34 = new ArrayList<Id<Link>>();
		NetworkRoute route34 = (NetworkRoute) routeFactory.createRoute(linkId3, linkId4);
		route34.setLinkIds(linkId3, linkIds34, linkId4);
		leg_3_4.setRoute(route34);
		
		Leg leg_3_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds35 = new ArrayList<Id<Link>>();
		linkIds35.add(linkId4);
		NetworkRoute route35 = (NetworkRoute) routeFactory.createRoute(linkId3, linkId5);
		route35.setLinkIds(linkId3, linkIds35, linkId5);
		leg_3_5.setRoute(route35);
		
		Leg leg_3_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds36 = new ArrayList<Id<Link>>();
		linkIds36.add(linkId4);
		linkIds36.add(linkId5);
		NetworkRoute route36 = (NetworkRoute) routeFactory.createRoute(linkId3, linkId6);
		route36.setLinkIds(linkId3, linkIds36, linkId6);
		leg_3_6.setRoute(route36);
		
		Leg leg_4_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds45 = new ArrayList<Id<Link>>();
		NetworkRoute route45 = (NetworkRoute) routeFactory.createRoute(linkId4, linkId5);
		route45.setLinkIds(linkId4, linkIds45, linkId5);
		leg_4_5.setRoute(route45);
		
		Leg leg_4_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds46 = new ArrayList<Id<Link>>();
		linkIds46.add(linkId5);
		NetworkRoute route46 = (NetworkRoute) routeFactory.createRoute(linkId4, linkId6);
		route46.setLinkIds(linkId4, linkIds46, linkId6);
		leg_4_6.setRoute(route46);
		
		Leg leg_5_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds56 = new ArrayList<Id<Link>>();
		NetworkRoute route56 = (NetworkRoute) routeFactory.createRoute(linkId5, linkId6);
		route56.setLinkIds(linkId5, linkIds56, linkId6);
		leg_5_6.setRoute(route56);
		
		Person person1 = popFactory.createPerson(testAgent1);
		Plan plan1 = popFactory.createPlan();
		Activity act1 = popFactory.createActivityFromCoord("h", coord1);
		act1.setEndTime(99);
		plan1.addActivity(act1);
	//	Leg leg1 = popFactory.createLeg("pt");
		plan1.addLeg(leg_1_6);
	//	lastActLink6.getCoord().setXY(3000.0, 0.0);
		lastActLink6.setType("w");
		plan1.addActivity(lastActLink6);
		person1.addPlan(plan1);
		
		Person person2 = popFactory.createPerson(testAgent2);
		Plan plan2 = popFactory.createPlan();
		Activity act2 = popFactory.createActivityFromCoord("h", coord1);
		act2.setEndTime(100);
		plan2.addActivity(act2);
	//	Leg leg2 = popFactory.createLeg("pt");
		plan2.addLeg(leg_1_6);
	//	lastActLink6.getCoord().setXY(3000.0, 0.0);
		lastActLink6.setType("w");
		plan2.addActivity(lastActLink6);
		person2.addPlan(plan2);
		
		Person person3 = popFactory.createPerson(testAgent3);
		Plan plan3 = popFactory.createPlan();
		Activity act3 = popFactory.createActivityFromCoord("h", coord1);
		act3.setEndTime(300);
		plan3.addActivity(act3);
	//	Leg leg3 = popFactory.createLeg("pt");
		plan3.addLeg(leg_1_6);
	//	lastActLink6.getCoord().setXY(3000.0, 0.0);
		lastActLink6.setType("w");
		plan3.addActivity(lastActLink6);
		person3.addPlan(plan3);
		
		Person person4 = popFactory.createPerson(testAgent4);
		Plan plan4 = popFactory.createPlan();
		Activity act4 = popFactory.createActivityFromCoord("h", coord1);
		act4.setEndTime(301);
		plan4.addActivity(act4);
	//	Leg leg4 = popFactory.createLeg("pt");
		plan4.addLeg(leg_1_6);
	//	lastActLink6.getCoord().setXY(3000.0, 0.0);
		lastActLink6.setType("w");
		plan4.addActivity(lastActLink6);
		person4.addPlan(plan4);
	
		Person person5 = popFactory.createPerson(testAgent5);
		Plan plan5 = popFactory.createPlan();
		Activity act5 = popFactory.createActivityFromCoord("h", coord2);
		act5.setEndTime(99);
		plan5.addActivity(act5);
	//	Leg leg5 = popFactory.createLeg("pt");
		plan5.addLeg(leg_2_6);
	//	lastActLink6.getCoord().setXY(3000.0, 0.0);
		lastActLink6.setType("w");
		plan5.addActivity(lastActLink6);
		person5.addPlan(plan5);
		
		population.addPerson(person1);
		population.addPerson(person2);
		population.addPerson(person3);
		population.addPerson(person4);
		population.addPerson(person5);
		
	}

	private void fillScheduleTestCapacityDelay03(MutableScenario scenario) {
		TransitSchedule schedule = scenario.getTransitSchedule();
		
		TransitScheduleFactory sf = schedule.getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();
		
		Id<TransitStopFacility> Stop1 = Id.create("Stop1Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop2 = Id.create("Stop2Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop3 = Id.create("Stop3Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop4 = Id.create("Stop4Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop5 = Id.create("Stop5Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop6 = Id.create("Stop6Id", TransitStopFacility.class);
		TransitStopFacility StopFacility1;
		TransitStopFacility StopFacility2;
		TransitStopFacility StopFacility3;
		TransitStopFacility StopFacility4;
		TransitStopFacility StopFacility5;
		TransitStopFacility StopFacility6;
		StopFacility1 = sf.createTransitStopFacility(Stop1, new Coord(500., 0.), true);
		StopFacility1.setLinkId(linkId1);
		StopFacility2 = sf.createTransitStopFacility(Stop2, new Coord(1000., 0.), true);
		StopFacility2.setLinkId(linkId2);
		StopFacility3 = sf.createTransitStopFacility(Stop3, new Coord(1500., 0.), true);
		StopFacility3.setLinkId(linkId3);
		StopFacility4 = sf.createTransitStopFacility(Stop4, new Coord(2000., 0.), true);
		StopFacility4.setLinkId(linkId4);
		StopFacility5 = sf.createTransitStopFacility(Stop5, new Coord(2500., 0.), true);
		StopFacility5.setLinkId(linkId5);
		StopFacility6 = sf.createTransitStopFacility(Stop6, new Coord(3000., 0.), true);
		StopFacility6.setLinkId(linkId6);
		
		TransitLine TransitLine1 = null;
		Id<TransitLine> Line1 = Id.create("line1Id", TransitLine.class);
		TransitLine1 = sf.createTransitLine(Line1);
		
		List<Id<TransitLine>> lineIds = new ArrayList<Id<TransitLine>>();
		lineIds.add(Line1);
		
		Id<TransitRoute> Route1 = Id.create("Route1Id", TransitRoute.class);
		NetworkRoute NetworkRoute1 = null;
		NetworkRoute1 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId6);
		List<Id<Link>> LinkIds = new ArrayList<Id<Link>>();
		LinkIds.add(linkId2);
		LinkIds.add(linkId3);
		LinkIds.add(linkId4);
		LinkIds.add(linkId5);
		NetworkRoute1.setLinkIds(linkId1, LinkIds, linkId6);
		
		TransitRouteStop TRStop1;
		TransitRouteStop TRStop2;
		TransitRouteStop TRStop3;
		TransitRouteStop TRStop4;
		TransitRouteStop TRStop5;
		TransitRouteStop TRStop6;
		TRStop1 = sf.createTransitRouteStop(StopFacility1, 0.0, 0.0);
		TRStop2 = sf.createTransitRouteStop(StopFacility2, 60.0, 60.0);
		TRStop3 = sf.createTransitRouteStop(StopFacility3, 120.0, 120.0);
		TRStop4 = sf.createTransitRouteStop(StopFacility4, 180.0, 180.0);
		TRStop5 = sf.createTransitRouteStop(StopFacility5, 240.0, 240.0);
		TRStop6 = sf.createTransitRouteStop(StopFacility6, 300.0, 300.0);
		
		List<TransitRouteStop> Stops = new ArrayList<TransitRouteStop>();
		Stops.add(TRStop1);
		Stops.add(TRStop2);
		Stops.add(TRStop3);
		Stops.add(TRStop4);
		Stops.add(TRStop5);
		Stops.add(TRStop6);
		
		TransitRoute TransitRoute1 = null;
		TransitRoute1 = sf.createTransitRoute(Route1, NetworkRoute1, Stops, "pt");
		
		Departure Departure1 = null;
		Id<Departure> DepartureId1 = Id.create("DepartureId_1", Departure.class);
		Departure1 = sf.createDeparture(DepartureId1, 200);
		Id<Vehicle> veh_1 = Id.create("veh_1", Vehicle.class);
		Departure1.setVehicleId(veh_1);
		
		Departure Departure2 = null;
		Id<Departure> DepartureId2 = Id.create("DepartureId_2", Departure.class);
		Departure2 = sf.createDeparture(DepartureId2, 600);
		Id<Vehicle> veh_2 = Id.create("veh_2", Vehicle.class);
		Departure2.setVehicleId(veh_2);
		
		Departure Departure3 = null;
		Id<Departure> DepartureId3 = Id.create("DepartureId_3", Departure.class);
		Departure3 = sf.createDeparture(DepartureId3, 800);
		Id<Vehicle> veh_3 = Id.create("veh_3", Vehicle.class);
		Departure3.setVehicleId(veh_3);
		
		TransitRoute1.addDeparture(Departure1);
		TransitRoute1.addDeparture(Departure2);
		TransitRoute1.addDeparture(Departure3);
		
		TransitLine1.addRoute(TransitRoute1);
		
		schedule.addTransitLine(TransitLine1);
		schedule.addStopFacility(StopFacility1);
		schedule.addStopFacility(StopFacility2);
		schedule.addStopFacility(StopFacility3);
		schedule.addStopFacility(StopFacility4);
		schedule.addStopFacility(StopFacility5);
		schedule.addStopFacility(StopFacility6);
		
	}
	
	private void fillVehicleTestCapacityDelay03(MutableScenario scenario) {
		Vehicles veh = scenario.getTransitVehicles();

		Id<VehicleType> vehTypeId1 = Id.create("type_1", VehicleType.class);
		Id<Vehicle> vehId1 = Id.create("veh_1", Vehicle.class);
		Id<Vehicle> vehId2 = Id.create("veh_2", Vehicle.class);
		Id<Vehicle> vehId3 = Id.create("veh_3", Vehicle.class);

		VehicleType type = veh.getFactory().createVehicleType(vehTypeId1);
		VehicleCapacity cap = veh.getFactory().createVehicleCapacity();
		cap.setSeats(2);
		cap.setStandingRoom(0);
		type.setCapacity(cap);
		type.setLength(10);
		type.setAccessTime(1.0);
		type.setEgressTime(0.75);
		type.setDoorOperationMode(DoorOperationMode.serial);
		
		type.setMaximumVelocity(8.4);
		type.setPcuEquivalents(7.5);
		
		veh.addVehicleType(type); 
		
		Vehicle vehicle1 = veh.getFactory().createVehicle(vehId1, veh.getVehicleTypes().get(vehTypeId1));
		veh.addVehicle( vehicle1);
		
		Vehicle vehicle2 = veh.getFactory().createVehicle(vehId2, veh.getVehicleTypes().get(vehTypeId1));
		veh.addVehicle( vehicle2);
		
		Vehicle vehicle3 = veh.getFactory().createVehicle(vehId3, veh.getVehicleTypes().get(vehTypeId1));
		veh.addVehicle( vehicle3);
	}

	private void setPopulationTestCapacityDelay04(Scenario scenario) {
		
		Population population = scenario.getPopulation();
        PopulationFactory popFactory = (PopulationFactory) scenario.getPopulation().getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();

		Coord coord1 = new Coord(500.0, 0.0);
		Coord coord2 = new Coord(1000.0, 0.0);
		Coord coord3 = new Coord(1500.0, 0.0);
		Coord coord4 = new Coord(2000.0, 0.0);
		Coord coord5 = new Coord(2500.0, 0.0);
		Coord coord6 = new Coord(3000.0, 0.0);
		
		Activity lastActLink2 = popFactory.createActivityFromCoord("w", coord2);
		Activity lastActLink3 = popFactory.createActivityFromCoord("w", coord3);
		Activity lastActLink4 = popFactory.createActivityFromCoord("w", coord4);
		Activity lastActLink5 = popFactory.createActivityFromCoord("w", coord5);
		Activity lastActLink6 = popFactory.createActivityFromCoord("w", coord6);
		
		Leg leg_1_2 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds12 = new ArrayList<Id<Link>>();
		NetworkRoute route12 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId2);
		route12.setLinkIds(linkId1, linkIds12, linkId2);
		leg_1_2.setRoute(route12);
		
		Leg leg_1_3 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds13 = new ArrayList<Id<Link>>();
		linkIds13.add(linkId2);
		NetworkRoute route13 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId3);
		route13.setLinkIds(linkId1, linkIds13, linkId3);
		leg_1_3.setRoute(route13);
		
		Leg leg_1_4 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds14 = new ArrayList<Id<Link>>();
		linkIds14.add(linkId2);
		linkIds14.add(linkId3);
		NetworkRoute route14 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId4);
		route14.setLinkIds(linkId1, linkIds14, linkId4);
		leg_1_4.setRoute(route14);
		
		Leg leg_1_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds15 = new ArrayList<Id<Link>>();
		linkIds15.add(linkId2);
		linkIds15.add(linkId3);
		linkIds15.add(linkId4);
		NetworkRoute route15 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId5);
		route15.setLinkIds(linkId1, linkIds15, linkId5);
		leg_1_5.setRoute(route15);
		
		Leg leg_1_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds16 = new ArrayList<Id<Link>>();
		linkIds16.add(linkId2);
		linkIds16.add(linkId3);
		linkIds16.add(linkId4);
		linkIds16.add(linkId5);
		NetworkRoute route16 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId6);
		route16.setLinkIds(linkId1, linkIds16, linkId6);
		leg_1_6.setRoute(route16);
		
		Leg leg_2_3 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds23 = new ArrayList<Id<Link>>();
		NetworkRoute route23 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId3);
		route23.setLinkIds(linkId2, linkIds23, linkId3);
		leg_2_3.setRoute(route23);
		
		Leg leg_2_4 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds24 = new ArrayList<Id<Link>>();
		linkIds24.add(linkId3);
		NetworkRoute route24 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId4);
		route24.setLinkIds(linkId2, linkIds24, linkId4);
		leg_2_4.setRoute(route24);
		
		Leg leg_2_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds25 = new ArrayList<Id<Link>>();
		linkIds25.add(linkId3);
		linkIds25.add(linkId4);
		NetworkRoute route25 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId5);
		route25.setLinkIds(linkId2, linkIds25, linkId5);
		leg_2_5.setRoute(route25);
		
		Leg leg_2_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds26 = new ArrayList<Id<Link>>();
		linkIds26.add(linkId3);
		linkIds26.add(linkId4);
		linkIds26.add(linkId5);
		NetworkRoute route26 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId6);
		route26.setLinkIds(linkId2, linkIds26, linkId6);
		leg_2_6.setRoute(route26);
		
		Leg leg_3_4 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds34 = new ArrayList<Id<Link>>();
		NetworkRoute route34 = (NetworkRoute) routeFactory.createRoute(linkId3, linkId4);
		route34.setLinkIds(linkId3, linkIds34, linkId4);
		leg_3_4.setRoute(route34);
		
		Leg leg_3_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds35 = new ArrayList<Id<Link>>();
		linkIds35.add(linkId4);
		NetworkRoute route35 = (NetworkRoute) routeFactory.createRoute(linkId3, linkId5);
		route35.setLinkIds(linkId3, linkIds35, linkId5);
		leg_3_5.setRoute(route35);
		
		Leg leg_3_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds36 = new ArrayList<Id<Link>>();
		linkIds36.add(linkId4);
		linkIds36.add(linkId5);
		NetworkRoute route36 = (NetworkRoute) routeFactory.createRoute(linkId3, linkId6);
		route36.setLinkIds(linkId3, linkIds36, linkId6);
		leg_3_6.setRoute(route36);
		
		Leg leg_4_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds45 = new ArrayList<Id<Link>>();
		NetworkRoute route45 = (NetworkRoute) routeFactory.createRoute(linkId4, linkId5);
		route45.setLinkIds(linkId4, linkIds45, linkId5);
		leg_4_5.setRoute(route45);
		
		Leg leg_4_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds46 = new ArrayList<Id<Link>>();
		linkIds46.add(linkId5);
		NetworkRoute route46 = (NetworkRoute) routeFactory.createRoute(linkId4, linkId6);
		route46.setLinkIds(linkId4, linkIds46, linkId6);
		leg_4_6.setRoute(route46);
		
		Leg leg_5_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds56 = new ArrayList<Id<Link>>();
		NetworkRoute route56 = (NetworkRoute) routeFactory.createRoute(linkId5, linkId6);
		route56.setLinkIds(linkId5, linkIds56, linkId6);
		leg_5_6.setRoute(route56);
		
		Person person1 = popFactory.createPerson(testAgent1);
		Plan plan1 = popFactory.createPlan();
		Activity act1 = popFactory.createActivityFromCoord("h", coord1);
		act1.setEndTime(99);
		plan1.addActivity(act1);
	//	Leg leg1 = popFactory.createLeg("pt");
		plan1.addLeg(leg_1_2);
	//	lastActLink2.getCoord().setXY(1000.0, 0.0);
		lastActLink2.setType("w");
		plan1.addActivity(lastActLink2);
		person1.addPlan(plan1);
		
		Person person2 = popFactory.createPerson(testAgent2);
		Plan plan2 = popFactory.createPlan();
		Activity act2 = popFactory.createActivityFromCoord("h", coord1);
		act2.setEndTime(100);
		plan2.addActivity(act2);
	//	Leg leg2 = popFactory.createLeg("pt");
		plan2.addLeg(leg_1_2);
	//	lastActLink2.getCoord().setXY(1000.0, 0.0);
		lastActLink2.setType("w");
		plan2.addActivity(lastActLink2);
		person2.addPlan(plan2);
		
		Person person3 = popFactory.createPerson(testAgent3);
		Plan plan3 = popFactory.createPlan();
		Activity act3 = popFactory.createActivityFromCoord("h", coord2);
		act3.setEndTime(99);
		plan3.addActivity(act3);
	//	Leg leg3 = popFactory.createLeg("pt");
		plan3.addLeg(leg_2_6);
	//	lastActLink6.getCoord().setXY(3000.0, 0.0);
		lastActLink6.setType("w");
		plan3.addActivity(lastActLink6);
		person3.addPlan(plan3);
		
		Person person4 = popFactory.createPerson(testAgent4);
		Plan plan4 = popFactory.createPlan();
		Activity act4 = popFactory.createActivityFromCoord("h", coord2);
		act4.setEndTime(100);
		plan4.addActivity(act4);
	//	Leg leg4 = popFactory.createLeg("pt");
		plan4.addLeg(leg_2_6);
	//	lastActLink6.getCoord().setXY(3000.0, 0.0);
		lastActLink6.setType("w");
		plan4.addActivity(lastActLink6);
		person4.addPlan(plan4);
		
		population.addPerson(person1);
		population.addPerson(person2);
		population.addPerson(person3);
		population.addPerson(person4);
		
	}

	private void fillScheduleTestCapacityDelay04(MutableScenario scenario) {
		TransitSchedule schedule = scenario.getTransitSchedule();
		
		TransitScheduleFactory sf = schedule.getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();
		
		Id<TransitStopFacility> Stop1 = Id.create("Stop1Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop2 = Id.create("Stop2Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop3 = Id.create("Stop3Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop4 = Id.create("Stop4Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop5 = Id.create("Stop5Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop6 = Id.create("Stop6Id", TransitStopFacility.class);
		TransitStopFacility StopFacility1;
		TransitStopFacility StopFacility2;
		TransitStopFacility StopFacility3;
		TransitStopFacility StopFacility4;
		TransitStopFacility StopFacility5;
		TransitStopFacility StopFacility6;
		StopFacility1 = sf.createTransitStopFacility(Stop1, new Coord(500., 0.), true);
		StopFacility1.setLinkId(linkId1);
		StopFacility2 = sf.createTransitStopFacility(Stop2, new Coord(1000., 0.), true);
		StopFacility2.setLinkId(linkId2);
		StopFacility3 = sf.createTransitStopFacility(Stop3, new Coord(1500., 0.), true);
		StopFacility3.setLinkId(linkId3);
		StopFacility4 = sf.createTransitStopFacility(Stop4, new Coord(2000., 0.), true);
		StopFacility4.setLinkId(linkId4);
		StopFacility5 = sf.createTransitStopFacility(Stop5, new Coord(2500., 0.), true);
		StopFacility5.setLinkId(linkId5);
		StopFacility6 = sf.createTransitStopFacility(Stop6, new Coord(3000., 0.), true);
		StopFacility6.setLinkId(linkId6);
		
		TransitLine TransitLine1 = null;
		Id<TransitLine> Line1 = Id.create("line1Id", TransitLine.class);
		TransitLine1 = sf.createTransitLine(Line1);
		
		List<Id<TransitLine>> lineIds = new ArrayList<Id<TransitLine>>();
		lineIds.add(Line1);
		
		Id<TransitRoute> Route1 = Id.create("Route1Id", TransitRoute.class);
		NetworkRoute NetworkRoute1 = null;
		NetworkRoute1 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId6);
		List<Id<Link>> LinkIds = new ArrayList<Id<Link>>();
		LinkIds.add(linkId2);
		LinkIds.add(linkId3);
		LinkIds.add(linkId4);
		LinkIds.add(linkId5);
		NetworkRoute1.setLinkIds(linkId1, LinkIds, linkId6);
		
		TransitRouteStop TRStop1;
		TransitRouteStop TRStop2;
		TransitRouteStop TRStop3;
		TransitRouteStop TRStop4;
		TransitRouteStop TRStop5;
		TransitRouteStop TRStop6;
		TRStop1 = sf.createTransitRouteStop(StopFacility1, 0.0, 0.0);
		TRStop2 = sf.createTransitRouteStop(StopFacility2, 60.0, 60.0);
		TRStop3 = sf.createTransitRouteStop(StopFacility3, 120.0, 120.0);
		TRStop4 = sf.createTransitRouteStop(StopFacility4, 180.0, 180.0);
		TRStop5 = sf.createTransitRouteStop(StopFacility5, 240.0, 240.0);
		TRStop6 = sf.createTransitRouteStop(StopFacility6, 300.0, 300.0);
		
		List<TransitRouteStop> Stops = new ArrayList<TransitRouteStop>();
		Stops.add(TRStop1);
		Stops.add(TRStop2);
		Stops.add(TRStop3);
		Stops.add(TRStop4);
		Stops.add(TRStop5);
		Stops.add(TRStop6);
		
		TransitRoute TransitRoute1 = null;
		TransitRoute1 = sf.createTransitRoute(Route1, NetworkRoute1, Stops, "pt");
		
		Departure Departure1 = null;
		Id<Departure> DepartureId1 = Id.create("DepartureId_1", Departure.class);
		Departure1 = sf.createDeparture(DepartureId1, 200);
		Id<Vehicle> veh_1 = Id.create("veh_1", Vehicle.class);
		Departure1.setVehicleId(veh_1);
		
		Departure Departure2 = null;
		Id<Departure> DepartureId2 = Id.create("DepartureId_2", Departure.class);
		Departure2 = sf.createDeparture(DepartureId2, 600);
		Id<Vehicle> veh_2 = Id.create("veh_2", Vehicle.class);
		Departure2.setVehicleId(veh_2);
		
		Departure Departure3 = null;
		Id<Departure> DepartureId3 = Id.create("DepartureId_3", Departure.class);
		Departure3 = sf.createDeparture(DepartureId3, 800);
		Id<Vehicle> veh_3 = Id.create("veh_3", Vehicle.class);
		Departure3.setVehicleId(veh_3);
		
		TransitRoute1.addDeparture(Departure1);
		TransitRoute1.addDeparture(Departure2);
		TransitRoute1.addDeparture(Departure3);
		
		TransitLine1.addRoute(TransitRoute1);
		
		schedule.addTransitLine(TransitLine1);
		schedule.addStopFacility(StopFacility1);
		schedule.addStopFacility(StopFacility2);
		schedule.addStopFacility(StopFacility3);
		schedule.addStopFacility(StopFacility4);
		schedule.addStopFacility(StopFacility5);
		schedule.addStopFacility(StopFacility6);
		
	}
	
	private void fillVehicleTestCapacityDelay04(MutableScenario scenario) {
		Vehicles veh = scenario.getTransitVehicles();

		Id<VehicleType> vehTypeId1 = Id.create("type_1", VehicleType.class);
		Id<Vehicle> vehId1 = Id.create("veh_1", Vehicle.class);
		Id<Vehicle> vehId2 = Id.create("veh_2", Vehicle.class);
		Id<Vehicle> vehId3 = Id.create("veh_3", Vehicle.class);

		VehicleType type = veh.getFactory().createVehicleType(vehTypeId1);
		VehicleCapacity cap = veh.getFactory().createVehicleCapacity();
		cap.setSeats(2);
		cap.setStandingRoom(0);
		type.setCapacity(cap);
		type.setLength(10);
		type.setAccessTime(1.0);
		type.setEgressTime(0.75);
		type.setDoorOperationMode(DoorOperationMode.serial);
		
		type.setMaximumVelocity(8.4);
		type.setPcuEquivalents(7.5);
		
		veh.addVehicleType(type); 
		
		Vehicle vehicle1 = veh.getFactory().createVehicle(vehId1, veh.getVehicleTypes().get(vehTypeId1));
		veh.addVehicle( vehicle1);
		
		Vehicle vehicle2 = veh.getFactory().createVehicle(vehId2, veh.getVehicleTypes().get(vehTypeId1));
		veh.addVehicle( vehicle2);
		
		Vehicle vehicle3 = veh.getFactory().createVehicle(vehId3, veh.getVehicleTypes().get(vehTypeId1));
		veh.addVehicle( vehicle3);
	}

	private void setPopulationTestCapacityDelay05(Scenario scenario) {
		
		Population population = scenario.getPopulation();
        PopulationFactory popFactory = (PopulationFactory) scenario.getPopulation().getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();

		Coord coord1 = new Coord(500.0, 0.0);
		Coord coord2 = new Coord(1000.0, 0.0);
		Coord coord3 = new Coord(1500.0, 0.0);
		Coord coord4 = new Coord(2000.0, 0.0);
		Coord coord5 = new Coord(2500.0, 0.0);
		Coord coord6 = new Coord(3000.0, 0.0);
		
		Activity lastActLink2 = popFactory.createActivityFromCoord("w", coord2);
		Activity lastActLink3 = popFactory.createActivityFromCoord("w", coord3);
		Activity lastActLink4 = popFactory.createActivityFromCoord("w", coord4);
		Activity lastActLink5 = popFactory.createActivityFromCoord("w", coord5);
		Activity lastActLink6 = popFactory.createActivityFromCoord("w", coord6);
		
		Leg leg_1_2 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds12 = new ArrayList<Id<Link>>();
		NetworkRoute route12 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId2);
		route12.setLinkIds(linkId1, linkIds12, linkId2);
		leg_1_2.setRoute(route12);
		
		Leg leg_1_3 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds13 = new ArrayList<Id<Link>>();
		linkIds13.add(linkId2);
		NetworkRoute route13 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId3);
		route13.setLinkIds(linkId1, linkIds13, linkId3);
		leg_1_3.setRoute(route13);
		
		Leg leg_1_4 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds14 = new ArrayList<Id<Link>>();
		linkIds14.add(linkId2);
		linkIds14.add(linkId3);
		NetworkRoute route14 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId4);
		route14.setLinkIds(linkId1, linkIds14, linkId4);
		leg_1_4.setRoute(route14);
		
		Leg leg_1_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds15 = new ArrayList<Id<Link>>();
		linkIds15.add(linkId2);
		linkIds15.add(linkId3);
		linkIds15.add(linkId4);
		NetworkRoute route15 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId5);
		route15.setLinkIds(linkId1, linkIds15, linkId5);
		leg_1_5.setRoute(route15);
		
		Leg leg_1_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds16 = new ArrayList<Id<Link>>();
		linkIds16.add(linkId2);
		linkIds16.add(linkId3);
		linkIds16.add(linkId4);
		linkIds16.add(linkId5);
		NetworkRoute route16 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId6);
		route16.setLinkIds(linkId1, linkIds16, linkId6);
		leg_1_6.setRoute(route16);
		
		Leg leg_2_3 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds23 = new ArrayList<Id<Link>>();
		NetworkRoute route23 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId3);
		route23.setLinkIds(linkId2, linkIds23, linkId3);
		leg_2_3.setRoute(route23);
		
		Leg leg_2_4 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds24 = new ArrayList<Id<Link>>();
		linkIds24.add(linkId3);
		NetworkRoute route24 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId4);
		route24.setLinkIds(linkId2, linkIds24, linkId4);
		leg_2_4.setRoute(route24);
		
		Leg leg_2_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds25 = new ArrayList<Id<Link>>();
		linkIds25.add(linkId3);
		linkIds25.add(linkId4);
		NetworkRoute route25 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId5);
		route25.setLinkIds(linkId2, linkIds25, linkId5);
		leg_2_5.setRoute(route25);
		
		Leg leg_2_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds26 = new ArrayList<Id<Link>>();
		linkIds26.add(linkId3);
		linkIds26.add(linkId4);
		linkIds26.add(linkId5);
		NetworkRoute route26 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId6);
		route26.setLinkIds(linkId2, linkIds26, linkId6);
		leg_2_6.setRoute(route26);
		
		Leg leg_3_4 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds34 = new ArrayList<Id<Link>>();
		NetworkRoute route34 = (NetworkRoute) routeFactory.createRoute(linkId3, linkId4);
		route34.setLinkIds(linkId3, linkIds34, linkId4);
		leg_3_4.setRoute(route34);
		
		Leg leg_3_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds35 = new ArrayList<Id<Link>>();
		linkIds35.add(linkId4);
		NetworkRoute route35 = (NetworkRoute) routeFactory.createRoute(linkId3, linkId5);
		route35.setLinkIds(linkId3, linkIds35, linkId5);
		leg_3_5.setRoute(route35);
		
		Leg leg_3_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds36 = new ArrayList<Id<Link>>();
		linkIds36.add(linkId4);
		linkIds36.add(linkId5);
		NetworkRoute route36 = (NetworkRoute) routeFactory.createRoute(linkId3, linkId6);
		route36.setLinkIds(linkId3, linkIds36, linkId6);
		leg_3_6.setRoute(route36);
		
		Leg leg_4_5 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds45 = new ArrayList<Id<Link>>();
		NetworkRoute route45 = (NetworkRoute) routeFactory.createRoute(linkId4, linkId5);
		route45.setLinkIds(linkId4, linkIds45, linkId5);
		leg_4_5.setRoute(route45);
		
		Leg leg_4_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds46 = new ArrayList<Id<Link>>();
		linkIds46.add(linkId5);
		NetworkRoute route46 = (NetworkRoute) routeFactory.createRoute(linkId4, linkId6);
		route46.setLinkIds(linkId4, linkIds46, linkId6);
		leg_4_6.setRoute(route46);
		
		Leg leg_5_6 = popFactory.createLeg("pt");
		List<Id<Link>> linkIds56 = new ArrayList<Id<Link>>();
		NetworkRoute route56 = (NetworkRoute) routeFactory.createRoute(linkId5, linkId6);
		route56.setLinkIds(linkId5, linkIds56, linkId6);
		leg_5_6.setRoute(route56);
		
		Person person1 = popFactory.createPerson(testAgent1);
		Plan plan1 = popFactory.createPlan();
		Activity act1 = popFactory.createActivityFromCoord("h", coord1);
		act1.setEndTime(99);
		plan1.addActivity(act1);
	//	Leg leg1 = popFactory.createLeg("pt");
		plan1.addLeg(leg_1_2);
	//	lastActLink2.getCoord().setXY(1000.0, 0.0);
		lastActLink2.setType("w");
		plan1.addActivity(lastActLink2);
		person1.addPlan(plan1);
		
		Person person2 = popFactory.createPerson(testAgent2);
		Plan plan2 = popFactory.createPlan();
		Activity act2 = popFactory.createActivityFromCoord("h", coord1);
		act2.setEndTime(100);
		plan2.addActivity(act2);
	//	Leg leg2 = popFactory.createLeg("pt");
		plan2.addLeg(leg_1_2);
	//	lastActLink2.getCoord().setXY(1000.0, 0.0);
		lastActLink2.setType("w");
		plan2.addActivity(lastActLink2);
		person2.addPlan(plan2);
		
		Person person3 = popFactory.createPerson(testAgent3);
		Plan plan3 = popFactory.createPlan();
		Activity act3 = popFactory.createActivityFromCoord("h", coord2);
		act3.setEndTime(99);
		plan3.addActivity(act3);
	//	Leg leg3 = popFactory.createLeg("pt");
		plan3.addLeg(leg_2_6);
	//	lastActLink6.getCoord().setXY(3000.0, 0.0);
		lastActLink6.setType("w");
		plan3.addActivity(lastActLink6);
		person3.addPlan(plan3);
		
		Person person4 = popFactory.createPerson(testAgent4);
		Plan plan4 = popFactory.createPlan();
		Activity act4 = popFactory.createActivityFromCoord("h", coord2);
		act4.setEndTime(100);
		plan4.addActivity(act4);
	//	Leg leg4 = popFactory.createLeg("pt");
		plan4.addLeg(leg_2_6);
	//	lastActLink6.getCoord().setXY(3000.0, 0.0);
		lastActLink6.setType("w");
		plan4.addActivity(lastActLink6);
		person4.addPlan(plan4);
	
		Person person5 = popFactory.createPerson(testAgent5);
		Plan plan5 = popFactory.createPlan();
		Activity act5 = popFactory.createActivityFromCoord("h", coord3);
		act5.setEndTime(99);
		plan5.addActivity(act5);
	//	Leg leg5 = popFactory.createLeg("pt");
		plan5.addLeg(leg_3_6);
	//	lastActLink6.getCoord().setXY(3000.0, 0.0);
		lastActLink6.setType("w");
		plan5.addActivity(lastActLink6);
		person5.addPlan(plan5);
		
		population.addPerson(person1);
		population.addPerson(person2);
		population.addPerson(person3);
		population.addPerson(person4);
		population.addPerson(person5);
		
	}

	private void fillScheduleTestCapacityDelay05(MutableScenario scenario) {
		TransitSchedule schedule = scenario.getTransitSchedule();
		
		TransitScheduleFactory sf = schedule.getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();
		
		Id<TransitStopFacility> Stop1 = Id.create("Stop1Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop2 = Id.create("Stop2Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop3 = Id.create("Stop3Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop4 = Id.create("Stop4Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop5 = Id.create("Stop5Id", TransitStopFacility.class);
		Id<TransitStopFacility> Stop6 = Id.create("Stop6Id", TransitStopFacility.class);
		TransitStopFacility StopFacility1;
		TransitStopFacility StopFacility2;
		TransitStopFacility StopFacility3;
		TransitStopFacility StopFacility4;
		TransitStopFacility StopFacility5;
		TransitStopFacility StopFacility6;
		StopFacility1 = sf.createTransitStopFacility(Stop1, new Coord(500., 0.), true);
		StopFacility1.setLinkId(linkId1);
		StopFacility2 = sf.createTransitStopFacility(Stop2, new Coord(1000., 0.), true);
		StopFacility2.setLinkId(linkId2);
		StopFacility3 = sf.createTransitStopFacility(Stop3, new Coord(1500., 0.), true);
		StopFacility3.setLinkId(linkId3);
		StopFacility4 = sf.createTransitStopFacility(Stop4, new Coord(2000., 0.), true);
		StopFacility4.setLinkId(linkId4);
		StopFacility5 = sf.createTransitStopFacility(Stop5, new Coord(2500., 0.), true);
		StopFacility5.setLinkId(linkId5);
		StopFacility6 = sf.createTransitStopFacility(Stop6, new Coord(3000., 0.), true);
		StopFacility6.setLinkId(linkId6);
		
		TransitLine TransitLine1 = null;
		Id<TransitLine> Line1 = Id.create("line1Id", TransitLine.class);
		TransitLine1 = sf.createTransitLine(Line1);
		
		List<Id<TransitLine>> lineIds = new ArrayList<Id<TransitLine>>();
		lineIds.add(Line1);
		
		Id<TransitRoute> Route1 = Id.create("Route1Id", TransitRoute.class);
		NetworkRoute NetworkRoute1 = null;
		NetworkRoute1 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId6);
		List<Id<Link>> LinkIds = new ArrayList<Id<Link>>();
		LinkIds.add(linkId2);
		LinkIds.add(linkId3);
		LinkIds.add(linkId4);
		LinkIds.add(linkId5);
		NetworkRoute1.setLinkIds(linkId1, LinkIds, linkId6);
		
		TransitRouteStop TRStop1;
		TransitRouteStop TRStop2;
		TransitRouteStop TRStop3;
		TransitRouteStop TRStop4;
		TransitRouteStop TRStop5;
		TransitRouteStop TRStop6;
		TRStop1 = sf.createTransitRouteStop(StopFacility1, 0.0, 0.0);
		TRStop2 = sf.createTransitRouteStop(StopFacility2, 60.0, 60.0);
		TRStop3 = sf.createTransitRouteStop(StopFacility3, 120.0, 120.0);
		TRStop4 = sf.createTransitRouteStop(StopFacility4, 180.0, 180.0);
		TRStop5 = sf.createTransitRouteStop(StopFacility5, 240.0, 240.0);
		TRStop6 = sf.createTransitRouteStop(StopFacility6, 300.0, 300.0);
		
		List<TransitRouteStop> Stops = new ArrayList<TransitRouteStop>();
		Stops.add(TRStop1);
		Stops.add(TRStop2);
		Stops.add(TRStop3);
		Stops.add(TRStop4);
		Stops.add(TRStop5);
		Stops.add(TRStop6);
		
		TransitRoute TransitRoute1 = null;
		TransitRoute1 = sf.createTransitRoute(Route1, NetworkRoute1, Stops, "pt");
		
		Departure Departure1 = null;
		Id<Departure> DepartureId1 = Id.create("DepartureId_1", Departure.class);
		Departure1 = sf.createDeparture(DepartureId1, 200);
		Id<Vehicle> veh_1 = Id.create("veh_1", Vehicle.class);
		Departure1.setVehicleId(veh_1);
		
		Departure Departure2 = null;
		Id<Departure> DepartureId2 = Id.create("DepartureId_2", Departure.class);
		Departure2 = sf.createDeparture(DepartureId2, 600);
		Id<Vehicle> veh_2 = Id.create("veh_2", Vehicle.class);
		Departure2.setVehicleId(veh_2);
		
		Departure Departure3 = null;
		Id<Departure> DepartureId3 = Id.create("DepartureId_3", Departure.class);
		Departure3 = sf.createDeparture(DepartureId3, 800);
		Id<Vehicle> veh_3 = Id.create("veh_3", Vehicle.class);
		Departure3.setVehicleId(veh_3);
		
		TransitRoute1.addDeparture(Departure1);
		TransitRoute1.addDeparture(Departure2);
		TransitRoute1.addDeparture(Departure3);
		
		TransitLine1.addRoute(TransitRoute1);
		
		schedule.addTransitLine(TransitLine1);
		schedule.addStopFacility(StopFacility1);
		schedule.addStopFacility(StopFacility2);
		schedule.addStopFacility(StopFacility3);
		schedule.addStopFacility(StopFacility4);
		schedule.addStopFacility(StopFacility5);
		schedule.addStopFacility(StopFacility6);
		
	}
	
	private void fillVehicleTestCapacityDelay05(MutableScenario scenario) {
		Vehicles veh = scenario.getTransitVehicles();

		Id<VehicleType> vehTypeId1 = Id.create("type_1", VehicleType.class);
		Id<Vehicle> vehId1 = Id.create("veh_1", Vehicle.class);
		Id<Vehicle> vehId2 = Id.create("veh_2", Vehicle.class);
		Id<Vehicle> vehId3 = Id.create("veh_3", Vehicle.class);

		VehicleType type = veh.getFactory().createVehicleType(vehTypeId1);
		VehicleCapacity cap = veh.getFactory().createVehicleCapacity();
		cap.setSeats(2);
		cap.setStandingRoom(0);
		type.setCapacity(cap);
		type.setLength(10);
		type.setAccessTime(1.0);
		type.setEgressTime(0.75);
		type.setDoorOperationMode(DoorOperationMode.serial);
		
		type.setMaximumVelocity(8.4);
		type.setPcuEquivalents(7.5);
		
		veh.addVehicleType(type); 
		
		Vehicle vehicle1 = veh.getFactory().createVehicle(vehId1, veh.getVehicleTypes().get(vehTypeId1));
		veh.addVehicle( vehicle1);
		
		Vehicle vehicle2 = veh.getFactory().createVehicle(vehId2, veh.getVehicleTypes().get(vehTypeId1));
		veh.addVehicle( vehicle2);
		
		Vehicle vehicle3 = veh.getFactory().createVehicle(vehId3, veh.getVehicleTypes().get(vehTypeId1));
		veh.addVehicle( vehicle3);
	}

}
