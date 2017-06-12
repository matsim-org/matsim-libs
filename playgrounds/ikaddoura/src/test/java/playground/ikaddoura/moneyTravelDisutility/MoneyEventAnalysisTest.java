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
package playground.ikaddoura.moneyTravelDisutility;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.noise.personLinkMoneyEvents.PersonLinkMoneyEvent;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

import playground.ikaddoura.moneyTravelDisutility.data.AgentFilter;
import playground.ikaddoura.moneyTravelDisutility.data.BerlinAgentFilter;

/**
 * 
 * Testing the functionality of {@link MoneyEventAnalysis}.
 * 
 * @author ikaddoura
 *
 */
public class MoneyEventAnalysisTest {
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();
	
	@Test
	public final void test1() {
		
		final Id<Person> personId1 = Id.createPersonId("person1");
		final Id<Person> personId2 = Id.createPersonId("lkw2");
		final Id<Person> personId3 = Id.createPersonId("person3");
		final Id<Person> personId4 = Id.createPersonId("person4");
		final Id<Person> personId5 = Id.createPersonId("lkw5");

		final Id<Vehicle> vehicleId1 = Id.createVehicleId("vehicle1");
		final Id<Vehicle> vehicleId2 = Id.createVehicleId("vehicle2");
		final Id<Vehicle> vehicleId3 = Id.createVehicleId("vehicle3");
		final Id<Vehicle> vehicleId4 = Id.createVehicleId("vehicle4");
		final Id<Vehicle> vehicleId5 = Id.createVehicleId("vehicle5");
		
		Id<Link> linkId1 = Id.createLinkId("link1");
		Id<Link> linkId2 = Id.createLinkId("link2");
		Id<Link> linkId3 = Id.createLinkId("link3");

		Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
		Controler controler = new Controler(scenario);
		
		MoneyEventAnalysis moneyAnalysis = new MoneyEventAnalysis();

		final MoneyTimeDistanceTravelDisutilityFactory factory = new MoneyTimeDistanceTravelDisutilityFactory(
				new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.car, controler.getConfig().planCalcScore()));
				
		controler.addOverridingModule(new AbstractModule(){
			@Override
			public void install() {
								
				// agent filter
				this.bind(AgentFilter.class).to(BerlinAgentFilter.class);

				// travel disutility
				this.bindCarTravelDisutilityFactory().toInstance(factory);
				this.bind(MoneyEventAnalysis.class).asEagerSingleton();
				
				// person money event handler + controler listener
				this.addControlerListenerBinding().toInstance(moneyAnalysis);
				this.addEventHandlerBinding().toInstance(moneyAnalysis);
			}
		});
		
		controler.getConfig().controler().setLastIteration(0);
		controler.getConfig().controler().setOutputDirectory(testUtils.getOutputDirectory() + "test1/");
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler.run();
				
		// add a fake iteration
		
		moneyAnalysis.reset(1);
		
		moneyAnalysis.handleEvent(new PersonEntersVehicleEvent(0., personId1, vehicleId1));
		moneyAnalysis.handleEvent(new PersonEntersVehicleEvent(0., personId2, vehicleId2));
		moneyAnalysis.handleEvent(new PersonEntersVehicleEvent(0., personId3, vehicleId3));
		moneyAnalysis.handleEvent(new PersonEntersVehicleEvent(0., personId4, vehicleId4));
		moneyAnalysis.handleEvent(new PersonEntersVehicleEvent(0., personId5, vehicleId5));

		moneyAnalysis.handleEvent(new LinkEnterEvent(60., vehicleId1, linkId1));
		moneyAnalysis.handleEvent(new LinkEnterEvent(60., vehicleId2, linkId1));
		moneyAnalysis.handleEvent(new LinkEnterEvent(60., vehicleId3, linkId1));
		
		moneyAnalysis.handleEvent(new LinkEnterEvent(960., vehicleId1, linkId2));
		moneyAnalysis.handleEvent(new LinkEnterEvent(960., vehicleId2, linkId2));
		moneyAnalysis.handleEvent(new LinkEnterEvent(960., vehicleId3, linkId2));
		moneyAnalysis.handleEvent(new PersonLinkMoneyEvent(966., personId2, linkId2, -99., 966., "test"));
		
		moneyAnalysis.handleEvent(new LinkEnterEvent(3660., vehicleId1, linkId3));
		moneyAnalysis.handleEvent(new LinkEnterEvent(3660., vehicleId2, linkId3));
		moneyAnalysis.handleEvent(new LinkEnterEvent(3660., vehicleId3, linkId3));
		moneyAnalysis.handleEvent(new LinkEnterEvent(3660., vehicleId4, linkId3));
		moneyAnalysis.handleEvent(new LinkEnterEvent(3660., vehicleId5, linkId3));
		moneyAnalysis.handleEvent(new PersonLinkMoneyEvent(3661., personId2, linkId3, -123., 3661., "test"));
		moneyAnalysis.handleEvent(new PersonLinkMoneyEvent(3662., personId3, linkId3, -8., 3662., "test"));

		moneyAnalysis.notifyIterationEnds(new IterationEndsEvent(controler, 1));
		
		// test null intervals for linkId1
		Assert.assertNotNull("time bin should not be null", moneyAnalysis.getLinkId2info().get(linkId1).getTimeBinNr2timeBin().get(0));
		Assert.assertNull("time bin should be null", moneyAnalysis.getLinkId2info().get(linkId1).getTimeBinNr2timeBin().get(1));
		Assert.assertNull("time bin should be null", moneyAnalysis.getLinkId2info().get(linkId1).getTimeBinNr2timeBin().get(2));
		Assert.assertNull("time bin should be null", moneyAnalysis.getLinkId2info().get(linkId1).getTimeBinNr2timeBin().get(3));
		Assert.assertNull("time bin should be null", moneyAnalysis.getLinkId2info().get(linkId1).getTimeBinNr2timeBin().get(4));
		Assert.assertNull("time bin should be null", moneyAnalysis.getLinkId2info().get(linkId1).getTimeBinNr2timeBin().get(5));

		// test null intervals for linkId2
		Assert.assertNull("time bin should be null", moneyAnalysis.getLinkId2info().get(linkId2).getTimeBinNr2timeBin().get(0));
		Assert.assertNotNull("time bin should not be null", moneyAnalysis.getLinkId2info().get(linkId2).getTimeBinNr2timeBin().get(1));
		Assert.assertNull("time bin should be null", moneyAnalysis.getLinkId2info().get(linkId2).getTimeBinNr2timeBin().get(2));
		Assert.assertNull("time bin should be null", moneyAnalysis.getLinkId2info().get(linkId2).getTimeBinNr2timeBin().get(3));
		Assert.assertNull("time bin should be null", moneyAnalysis.getLinkId2info().get(linkId2).getTimeBinNr2timeBin().get(4));
		Assert.assertNull("time bin should be null", moneyAnalysis.getLinkId2info().get(linkId2).getTimeBinNr2timeBin().get(5));
		
		// test null intervals for linkId3
		Assert.assertNull("time bin should be null", moneyAnalysis.getLinkId2info().get(linkId3).getTimeBinNr2timeBin().get(0));
		Assert.assertNull("time bin should be null", moneyAnalysis.getLinkId2info().get(linkId3).getTimeBinNr2timeBin().get(1));
		Assert.assertNull("time bin should be null", moneyAnalysis.getLinkId2info().get(linkId3).getTimeBinNr2timeBin().get(2)); 
		Assert.assertNull("time bin should be null", moneyAnalysis.getLinkId2info().get(linkId3).getTimeBinNr2timeBin().get(3));
		Assert.assertNotNull("time bin should not be null", moneyAnalysis.getLinkId2info().get(linkId3).getTimeBinNr2timeBin().get(4)); 
		Assert.assertNull("time bin should be null", moneyAnalysis.getLinkId2info().get(linkId3).getTimeBinNr2timeBin().get(5)); 
		
		// test the number of entering agents for each link
		Assert.assertEquals("wrong number of entering agents", 3, moneyAnalysis.getLinkId2info().get(linkId1).getTimeBinNr2timeBin().get(0).getEnteringAgents().size());
		Assert.assertEquals("wrong number of entering agents", 3, moneyAnalysis.getLinkId2info().get(linkId2).getTimeBinNr2timeBin().get(1).getEnteringAgents().size());
		Assert.assertEquals("wrong number of entering agents", 5, moneyAnalysis.getLinkId2info().get(linkId3).getTimeBinNr2timeBin().get(4).getEnteringAgents().size());
		
		// test the average amount (all agent types) for each link
		Assert.assertEquals("wrong average amount", 0., moneyAnalysis.getLinkId2info().get(linkId1).getTimeBinNr2timeBin().get(0).getAverageAmount(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong average amount", -99./3., moneyAnalysis.getLinkId2info().get(linkId2).getTimeBinNr2timeBin().get(1).getAverageAmount(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong average amount", (-123 - 8.) / 5., moneyAnalysis.getLinkId2info().get(linkId3).getTimeBinNr2timeBin().get(4).getAverageAmount(), MatsimTestUtils.EPSILON);
		
		// test the average amount per agent type for each link with agent money events
		Assert.assertNull("should be null (no agent of type 'other' pays a toll)", moneyAnalysis.getLinkId2info().get(linkId2).getTimeBinNr2timeBin().get(1).getAgentTypeId2avgAmount().get("other"));
		Assert.assertEquals("wrong average amount", -99., moneyAnalysis.getLinkId2info().get(linkId2).getTimeBinNr2timeBin().get(1).getAgentTypeId2avgAmount().get("lkw"), MatsimTestUtils.EPSILON);

		Assert.assertEquals("wrong average amount", - 8. / 3., moneyAnalysis.getLinkId2info().get(linkId3).getTimeBinNr2timeBin().get(4).getAgentTypeId2avgAmount().get("other"), MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong average amount", - 123. / 2, moneyAnalysis.getLinkId2info().get(linkId3).getTimeBinNr2timeBin().get(4).getAgentTypeId2avgAmount().get("lkw"), MatsimTestUtils.EPSILON);
	}
	
	// one vehicle type for which there are no money events
	@Test
	public final void test2() {
		
		final Id<Person> personId1 = Id.createPersonId("person1");
		final Id<Person> personId2 = Id.createPersonId("person2");
		final Id<Person> personId3 = Id.createPersonId("person3");
		final Id<Person> personId4 = Id.createPersonId("person4");
		final Id<Person> personId5 = Id.createPersonId("person5");
		final Id<Person> personId6 = Id.createPersonId("lkw6");

		final Id<Vehicle> vehicleId1 = Id.createVehicleId("vehicle1");
		final Id<Vehicle> vehicleId2 = Id.createVehicleId("vehicle2");
		final Id<Vehicle> vehicleId3 = Id.createVehicleId("vehicle3");
		final Id<Vehicle> vehicleId4 = Id.createVehicleId("vehicle4");
		final Id<Vehicle> vehicleId5 = Id.createVehicleId("vehicle5");
		final Id<Vehicle> vehicleId6 = Id.createVehicleId("vehicle6");

		final Id<Link> linkId1 = Id.createLinkId("link1");
		final Id<Link> linkId2 = Id.createLinkId("link2");
		final Id<Link> linkId3 = Id.createLinkId("link3");

		Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
		Controler controler = new Controler(scenario);
		
		MoneyEventAnalysis moneyAnalysis = new MoneyEventAnalysis();

		final MoneyTimeDistanceTravelDisutilityFactory factory = new MoneyTimeDistanceTravelDisutilityFactory(
				new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.car, controler.getConfig().planCalcScore()));
				
		controler.addOverridingModule(new AbstractModule(){
			@Override
			public void install() {
								
				// agent filter
				this.bind(AgentFilter.class).to(BerlinAgentFilter.class);

				// travel disutility
				this.bindCarTravelDisutilityFactory().toInstance(factory);
				this.bind(MoneyEventAnalysis.class).asEagerSingleton();
				
				// person money event handler + controler listener
				this.addControlerListenerBinding().toInstance(moneyAnalysis);
				this.addEventHandlerBinding().toInstance(moneyAnalysis);
			}
		});
		
		controler.getConfig().controler().setLastIteration(0);
		controler.getConfig().controler().setOutputDirectory(testUtils.getOutputDirectory() + "test2/");
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler.run();
				
		// add a fake iteration
		
		moneyAnalysis.reset(0);
		
		moneyAnalysis.handleEvent(new PersonEntersVehicleEvent(0., personId1, vehicleId1));
		moneyAnalysis.handleEvent(new PersonEntersVehicleEvent(0., personId2, vehicleId2));
		moneyAnalysis.handleEvent(new PersonEntersVehicleEvent(0., personId3, vehicleId3));
		moneyAnalysis.handleEvent(new PersonEntersVehicleEvent(0., personId4, vehicleId4));
		moneyAnalysis.handleEvent(new PersonEntersVehicleEvent(0., personId5, vehicleId5));
		moneyAnalysis.handleEvent(new PersonEntersVehicleEvent(0., personId6, vehicleId6));

		moneyAnalysis.handleEvent(new LinkEnterEvent(60., vehicleId1, linkId1));
		moneyAnalysis.handleEvent(new LinkEnterEvent(60., vehicleId2, linkId1));
		moneyAnalysis.handleEvent(new LinkEnterEvent(60., vehicleId3, linkId1));
		
		moneyAnalysis.handleEvent(new LinkEnterEvent(960., vehicleId1, linkId2));
		moneyAnalysis.handleEvent(new LinkEnterEvent(960., vehicleId2, linkId2));
		moneyAnalysis.handleEvent(new LinkEnterEvent(960., vehicleId3, linkId2));
		moneyAnalysis.handleEvent(new PersonLinkMoneyEvent(966., personId2, linkId2, -99., 966., "test"));
		
		moneyAnalysis.handleEvent(new LinkEnterEvent(3660., vehicleId1, linkId3));
		moneyAnalysis.handleEvent(new LinkEnterEvent(3660., vehicleId2, linkId3));
		moneyAnalysis.handleEvent(new LinkEnterEvent(3660., vehicleId3, linkId3));
		moneyAnalysis.handleEvent(new LinkEnterEvent(3660., vehicleId4, linkId3));
		moneyAnalysis.handleEvent(new LinkEnterEvent(3660., vehicleId5, linkId3));
		moneyAnalysis.handleEvent(new LinkEnterEvent(3660., vehicleId6, linkId3));
		moneyAnalysis.handleEvent(new PersonLinkMoneyEvent(3661., personId2, linkId3, -123., 3661., "test"));
		moneyAnalysis.handleEvent(new PersonLinkMoneyEvent(3662., personId3, linkId3, -8., 3662., "test"));

		moneyAnalysis.notifyIterationEnds(new IterationEndsEvent(controler, 0));
		
		// test null intervals for linkId1
		Assert.assertNotNull("time bin should not be null", moneyAnalysis.getLinkId2info().get(linkId1).getTimeBinNr2timeBin().get(0));
		Assert.assertNull("time bin should be null", moneyAnalysis.getLinkId2info().get(linkId1).getTimeBinNr2timeBin().get(1));
		Assert.assertNull("time bin should be null", moneyAnalysis.getLinkId2info().get(linkId1).getTimeBinNr2timeBin().get(2));
		Assert.assertNull("time bin should be null", moneyAnalysis.getLinkId2info().get(linkId1).getTimeBinNr2timeBin().get(3));
		Assert.assertNull("time bin should be null", moneyAnalysis.getLinkId2info().get(linkId1).getTimeBinNr2timeBin().get(4));
		Assert.assertNull("time bin should be null", moneyAnalysis.getLinkId2info().get(linkId1).getTimeBinNr2timeBin().get(5));

		// test null intervals for linkId2
		Assert.assertNull("time bin should be null", moneyAnalysis.getLinkId2info().get(linkId2).getTimeBinNr2timeBin().get(0));
		Assert.assertNotNull("time bin should not be null", moneyAnalysis.getLinkId2info().get(linkId2).getTimeBinNr2timeBin().get(1));
		Assert.assertNull("time bin should be null", moneyAnalysis.getLinkId2info().get(linkId2).getTimeBinNr2timeBin().get(2));
		Assert.assertNull("time bin should be null", moneyAnalysis.getLinkId2info().get(linkId2).getTimeBinNr2timeBin().get(3));
		Assert.assertNull("time bin should be null", moneyAnalysis.getLinkId2info().get(linkId2).getTimeBinNr2timeBin().get(4));
		Assert.assertNull("time bin should be null", moneyAnalysis.getLinkId2info().get(linkId2).getTimeBinNr2timeBin().get(5));
		
		// test null intervals for linkId3
		Assert.assertNull("time bin should be null", moneyAnalysis.getLinkId2info().get(linkId3).getTimeBinNr2timeBin().get(0));
		Assert.assertNull("time bin should be null", moneyAnalysis.getLinkId2info().get(linkId3).getTimeBinNr2timeBin().get(1));
		Assert.assertNull("time bin should be null", moneyAnalysis.getLinkId2info().get(linkId3).getTimeBinNr2timeBin().get(2)); 
		Assert.assertNull("time bin should be null", moneyAnalysis.getLinkId2info().get(linkId3).getTimeBinNr2timeBin().get(3));
		Assert.assertNotNull("time bin should not be null", moneyAnalysis.getLinkId2info().get(linkId3).getTimeBinNr2timeBin().get(4)); 
		Assert.assertNull("time bin should be null", moneyAnalysis.getLinkId2info().get(linkId3).getTimeBinNr2timeBin().get(5)); 
		
		// test the number of entering agents for each link
		Assert.assertEquals("wrong number of entering agents", 3, moneyAnalysis.getLinkId2info().get(linkId1).getTimeBinNr2timeBin().get(0).getEnteringAgents().size());
		Assert.assertEquals("wrong number of entering agents", 3, moneyAnalysis.getLinkId2info().get(linkId2).getTimeBinNr2timeBin().get(1).getEnteringAgents().size());
		Assert.assertEquals("wrong number of entering agents", 6, moneyAnalysis.getLinkId2info().get(linkId3).getTimeBinNr2timeBin().get(4).getEnteringAgents().size());
		
		// test the average amount (all agent types) for each link
		Assert.assertEquals("wrong average amount", 0., moneyAnalysis.getLinkId2info().get(linkId1).getTimeBinNr2timeBin().get(0).getAverageAmount(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong average amount", -99./3., moneyAnalysis.getLinkId2info().get(linkId2).getTimeBinNr2timeBin().get(1).getAverageAmount(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong average amount", (-123 - 8.) / 6., moneyAnalysis.getLinkId2info().get(linkId3).getTimeBinNr2timeBin().get(4).getAverageAmount(), MatsimTestUtils.EPSILON);
		
		// test the average amount per agent type for each link with agent money events
		Assert.assertNull("should be null (no agent of type 'lkw' pays a toll)", moneyAnalysis.getLinkId2info().get(linkId2).getTimeBinNr2timeBin().get(1).getAgentTypeId2avgAmount().get("lkw"));
		Assert.assertEquals("wrong average amount", -99./3, moneyAnalysis.getLinkId2info().get(linkId2).getTimeBinNr2timeBin().get(1).getAgentTypeId2avgAmount().get("other"), MatsimTestUtils.EPSILON);

		Assert.assertNull("should be null (no agent of type 'lkw' pays a toll)", moneyAnalysis.getLinkId2info().get(linkId3).getTimeBinNr2timeBin().get(4).getAgentTypeId2avgAmount().get("lkw"));
		Assert.assertEquals("wrong average amount", (-123 - 8.) / 5., moneyAnalysis.getLinkId2info().get(linkId3).getTimeBinNr2timeBin().get(4).getAgentTypeId2avgAmount().get("other"), MatsimTestUtils.EPSILON);
	}
	
	// money events before link enter event
	@Test
	public final void test3() {
		
		final Id<Person> personId1 = Id.createPersonId("person1");
		
		final Id<Vehicle> vehicleId1 = Id.createVehicleId("vehicle1");
		
		final Id<Link> linkId1 = Id.createLinkId("link1");
		final Id<Link> linkId2 = Id.createLinkId("link2");
		final Id<Link> linkId3 = Id.createLinkId("link3");

		Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
		Controler controler = new Controler(scenario);
		
		MoneyEventAnalysis moneyAnalysis = new MoneyEventAnalysis();

		final MoneyTimeDistanceTravelDisutilityFactory factory = new MoneyTimeDistanceTravelDisutilityFactory(
				new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.car, controler.getConfig().planCalcScore()));
				
		controler.addOverridingModule(new AbstractModule(){
			@Override
			public void install() {
								
				// agent filter
				this.bind(AgentFilter.class).to(BerlinAgentFilter.class);

				// travel disutility
				this.bindCarTravelDisutilityFactory().toInstance(factory);
				this.bind(MoneyEventAnalysis.class).asEagerSingleton();
				
				// person money event handler + controler listener
				this.addControlerListenerBinding().toInstance(moneyAnalysis);
				this.addEventHandlerBinding().toInstance(moneyAnalysis);
			}
		});
		
		controler.getConfig().controler().setLastIteration(0);
		controler.getConfig().controler().setOutputDirectory(testUtils.getOutputDirectory() + "test3/");
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler.run();
				
		// add a fake iteration
		
		moneyAnalysis.reset(0);
		
		moneyAnalysis.handleEvent(new PersonEntersVehicleEvent(0., personId1, vehicleId1));
		
		moneyAnalysis.handleEvent(new PersonLinkMoneyEvent(60., personId1, linkId1, -99., 60., "test"));
		
		moneyAnalysis.handleEvent(new LinkEnterEvent(60., vehicleId1, linkId1));
		
		moneyAnalysis.handleEvent(new LinkEnterEvent(960., vehicleId1, linkId2));
		
		moneyAnalysis.handleEvent(new LinkEnterEvent(3660., vehicleId1, linkId3));
		
		moneyAnalysis.notifyIterationEnds(new IterationEndsEvent(controler, 0));
	}
	
	// money events without any link enter event (link enter event was in previous time bin)
	@Test
	public final void test4() {
		
		final Id<Person> personId1 = Id.createPersonId("person1");
		
		final Id<Vehicle> vehicleId1 = Id.createVehicleId("vehicle1");
		
		final Id<Link> linkId1 = Id.createLinkId("link1");

		Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
		Controler controler = new Controler(scenario);
		
		MoneyEventAnalysis moneyAnalysis = new MoneyEventAnalysis();

		final MoneyTimeDistanceTravelDisutilityFactory factory = new MoneyTimeDistanceTravelDisutilityFactory(
				new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.car, controler.getConfig().planCalcScore()));
				
		controler.addOverridingModule(new AbstractModule(){
			@Override
			public void install() {
								
				// agent filter
				this.bind(AgentFilter.class).to(BerlinAgentFilter.class);

				// travel disutility
				this.bindCarTravelDisutilityFactory().toInstance(factory);
				this.bind(MoneyEventAnalysis.class).asEagerSingleton();
				
				// person money event handler + controler listener
				this.addControlerListenerBinding().toInstance(moneyAnalysis);
				this.addEventHandlerBinding().toInstance(moneyAnalysis);
			}
		});
		
		controler.getConfig().controler().setLastIteration(0);
		controler.getConfig().controler().setOutputDirectory(testUtils.getOutputDirectory() + "test4/");
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler.run();
				
		// add a fake iteration
		
		moneyAnalysis.reset(0);
		
		moneyAnalysis.handleEvent(new PersonEntersVehicleEvent(0., personId1, vehicleId1));
		moneyAnalysis.handleEvent(new LinkEnterEvent(60., vehicleId1, linkId1));
		
		moneyAnalysis.handleEvent(new PersonLinkMoneyEvent(960., personId1, linkId1, -99., 960., "test"));
		
		moneyAnalysis.notifyIterationEnds(new IterationEndsEvent(controler, 0));
	}
	
	// money events for taxi vehicles
	@Test
	public final void test5() {
		
		final Id<Person> personId1 = Id.createPersonId("person1");
		
		final Id<Person> taxiDriverPersonId = Id.createPersonId("taxi1");
		final Id<Vehicle> vehicleId1 = Id.createVehicleId("taxi1");
		final Id<Link> linkId1 = Id.createLinkId("link1");

		Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
		Controler controler = new Controler(scenario);
		
		MoneyEventAnalysis moneyAnalysis = new MoneyEventAnalysis();

		final MoneyTimeDistanceTravelDisutilityFactory factory = new MoneyTimeDistanceTravelDisutilityFactory(
				new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.car, controler.getConfig().planCalcScore()));
				
		controler.addOverridingModule(new AbstractModule(){
			@Override
			public void install() {
								
				// travel disutility
				this.bindCarTravelDisutilityFactory().toInstance(factory);
				this.bind(MoneyEventAnalysis.class).asEagerSingleton();
				
				// person money event handler + controler listener
				this.addControlerListenerBinding().toInstance(moneyAnalysis);
				this.addEventHandlerBinding().toInstance(moneyAnalysis);
			}
		});
		
		controler.getConfig().controler().setLastIteration(0);
		controler.getConfig().controler().setOutputDirectory(testUtils.getOutputDirectory() + "test5/");
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler.run();
				
		// add a fake iteration
		
		moneyAnalysis.reset(0);
		
		moneyAnalysis.handleEvent(new PersonEntersVehicleEvent(0., taxiDriverPersonId, vehicleId1));
		moneyAnalysis.handleEvent(new PersonEntersVehicleEvent(1., personId1, vehicleId1));
		moneyAnalysis.handleEvent(new LinkEnterEvent(60., vehicleId1, linkId1));
		
		moneyAnalysis.handleEvent(new PersonLinkMoneyEvent(360., taxiDriverPersonId, linkId1, -99., 360., "test"));
		
		moneyAnalysis.notifyIterationEnds(new IterationEndsEvent(controler, 0));
		
		System.out.println("average amount: " + moneyAnalysis.getLinkId2info().get(linkId1).getTimeBinNr2timeBin().get(0).getAverageAmount());
		Assert.assertEquals("wrong average amount", -99., moneyAnalysis.getLinkId2info().get(linkId1).getTimeBinNr2timeBin().get(0).getAverageAmount(), MatsimTestUtils.EPSILON);
	}
	
}
