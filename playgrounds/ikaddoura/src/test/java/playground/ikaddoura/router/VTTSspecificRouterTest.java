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
package playground.ikaddoura.router;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.otfvis.OTFVisFileWriterModule;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutility.Builder;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

import playground.ikaddoura.analysis.vtts.VTTSHandler;
import playground.ikaddoura.analysis.vtts.VTTScomputation;

/**
 * 
 * Tests a router which accounts for the agent's real VTTS.
 * The VTTS is computed by {@link VTTSHandler}. 
 * 
 * @author ikaddoura
 *
 */

public class VTTSspecificRouterTest {
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	/**
	 * There are two agents. One agent is more pressed for time than the other one which results in different VTTS.
	 * There are two routes. One expensive but fast route. One cheap but slow route.
	 * 
	 * Accounting for the different VTTS results in a different outcome than assuming the default VTTS (as it is the case in the default router).
	 * 
	 */
	@Test
	public final void test1(){
		
		// starts the VTTS-specific router
		
		final String configFile1 = testUtils.getPackageInputDirectory() + "vttsSpecificRouter/configVTTS.xml";
		final Scenario scenario = ScenarioUtils.loadScenario( testUtils.loadConfig( configFile1 ) );
		final Controler controler = new Controler( scenario );
		final VTTSHandler vttsHandler = new VTTSHandler(controler.getScenario());
		final VTTSTimeDistanceTravelDisutilityFactory factory = new VTTSTimeDistanceTravelDisutilityFactory(vttsHandler, controler.getConfig().planCalcScore()) ;
		factory.setSigma(0.); // no randomness
		
		controler.addOverridingModule(new AbstractModule(){
			@Override
			public void install() {
				this.bindCarTravelDisutilityFactory().toInstance( factory );
			}
		}); 		
		
		final Map<Id<Vehicle>, Set<Id<Link>>> vehicleId2linkIds = new HashMap<>();

		controler.addControlerListener(new VTTScomputation(vttsHandler));
		
		controler.addControlerListener( new StartupListener() {

			@Override
			public void notifyStartup(StartupEvent event) {
				event.getServices().getEvents().addHandler(new LinkLeaveEventHandler() {
					
					@Override
					public void reset(int iteration) {
						vehicleId2linkIds.clear();
					}
					
					@Override
					public void handleEvent(LinkLeaveEvent event) {
						if (vehicleId2linkIds.containsKey(event.getVehicleId())) {
							vehicleId2linkIds.get(event.getVehicleId()).add(event.getLinkId());
						} else {
							Set<Id<Link>> linkIds = new HashSet<Id<Link>>();
							linkIds.add(event.getLinkId());
							vehicleId2linkIds.put(event.getVehicleId(), linkIds);
						}
					}
				});		
			}		
		});
		
		
		controler.addOverridingModule(new OTFVisFileWriterModule());
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler.run();
		
		// high VTTS person's VTTS: 17.21 EUR/h (this is a value provided by the VTTSHandler)
		// low VTTS person's VTTS: 10.37 EUR/h (this is a value provided by the VTTSHandler)
		
		// high speed and costly route: 15 km; 50 km/h (travel time: 0.3h)
		// high VTTS person: 17.21 EUR/km * 0.3 km + 15 km * 0.002 EUR/m --> 35.16 EUR
		// low VTTS person: 10.37 * 0.3 + 15 * 2 --> 33.11 EUR		
		
		// low speed and cheap route: 10 km; 10 km/h (travel time: 1h)
		// high VTTS person: 17.21 * 1 + 10 * 2 --> 37.21 EUR
		// low VTTS person: 10.37 * 1 + 10 * 2 --> 30.37 EUR
		
		// ==> the high VTTS person will choose the high speed but expensive route (35.16 < 37.21)
		// ==> the low VTTS person will choose the low speed but cheap route (30.37 < 33.11)
		
		Id<Link> longDistanceShortTimeLinkId = Id.createLinkId("link_1_2");
		Id<Link> shortDistanceLongTimeLinkId = Id.createLinkId("link_3_6");
		
		for (Id<Vehicle> id : vehicleId2linkIds.keySet()) {

			if (id.toString().contains("highVTTS")) {
				
				// the high VTTS person should use the fast but expensive route
				Assert.assertEquals(true, vehicleId2linkIds.get(id).contains(longDistanceShortTimeLinkId));
			}
			
			if (id.toString().contains("lowVTTS")) {

				// the low VTTS person should use the slow but cheap route
				Assert.assertEquals(true, vehicleId2linkIds.get(id).contains(shortDistanceLongTimeLinkId));
			}
		}		
	}
	
	/**
	 * Now do same as before but use the default router...
	 * 
	 */
	@Test
	public final void test2(){
				
		final String configFile1 = testUtils.getPackageInputDirectory() + "vttsSpecificRouter/configVTTS.xml";
		final Scenario scenario = ScenarioUtils.loadScenario( testUtils.loadConfig( configFile1 ) );
		final Controler controler = new Controler( scenario );

		// current default:
//		final Builder factory = new Builder( TransportMode.car );
//		factory.setSigma(0.0);
//
//		services.addOverridingModule(new AbstractModule(){
//			@Override
//			public void install() {
//				this.bindCarTravelDisutilityFactory().toInstance( factory );
//			}
//		});
		
		final Map<Id<Vehicle>, Set<Id<Link>>> vehicleId2linkIds = new HashMap<>();

		controler.addControlerListener( new StartupListener() {

			@Override
			public void notifyStartup(StartupEvent event) {
				event.getServices().getEvents().addHandler(new LinkLeaveEventHandler() {
					
					@Override
					public void reset(int iteration) {
						vehicleId2linkIds.clear();
					}
					
					@Override
					public void handleEvent(LinkLeaveEvent event) {
						if (vehicleId2linkIds.containsKey(event.getVehicleId())) {
							vehicleId2linkIds.get(event.getVehicleId()).add(event.getLinkId());
						} else {
							Set<Id<Link>> linkIds = new HashSet<Id<Link>>();
							linkIds.add(event.getLinkId());
							vehicleId2linkIds.put(event.getVehicleId(), linkIds);
						}
					}
				});		
			}		
		});
		
		
		controler.addOverridingModule(new OTFVisFileWriterModule());
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler.run();
				
		// high speed and costly route: 15 km; 50 km/h (travel time: 0.3h)
		// for both persons: 12 EUR/h * 0.3 h + 15 km * 0.002 EUR/m --> 33.6 EUR
		
		// low speed and cheap route: 10 km; 10 km/h (travel time: 1h)
		// for both person: 12 * 1 + 10 * 2 --> 32 EUR
		
		// ==> both persons will choose the low speed but cheap route (32 < 33.6)
		
		Id<Link> shortDistanceLongTimeLinkId = Id.createLinkId("link_3_6");
		
		for (Id<Vehicle> id : vehicleId2linkIds.keySet()) {

			if (id.toString().contains("highVTTS")) {
				
				// both persons use the slow but cheap route
				Assert.assertEquals(true, vehicleId2linkIds.get(id).contains(shortDistanceLongTimeLinkId));
				
//				System.out.println(vehicleId2linkIds.get(id).toString());
			}
			
			if (id.toString().contains("lowVTTS")) {

				// both persons use the slow but cheap route
				Assert.assertEquals(true, vehicleId2linkIds.get(id).contains(shortDistanceLongTimeLinkId));
				
//				System.out.println(vehicleId2linkIds.get(id).toString());
			}
		}		
	}
	
	/**
	 * use the VTTS specific router but set any distance cost to zero, the outcome should be the same as for the standard router
	 * 
	 */
	@Test
	public final void test3(){
		
		// starts the VTTS-specific router
		
		final String configFile = testUtils.getPackageInputDirectory() + "vttsSpecificRouter/configVTTS_noDistanceCost.xml";
		final Scenario scenario = ScenarioUtils.loadScenario( testUtils.loadConfig( configFile ) );
		final Controler controler = new Controler( scenario );
		final VTTSHandler vttsHandler = new VTTSHandler(controler.getScenario());
		final VTTSTimeDistanceTravelDisutilityFactory factory = new VTTSTimeDistanceTravelDisutilityFactory(vttsHandler, controler.getConfig().planCalcScore()) ;
		factory.setSigma(0.); // no randomness
		
		controler.addOverridingModule(new AbstractModule(){
			@Override
			public void install() {
				this.bindCarTravelDisutilityFactory().toInstance( factory );
			}
		}); 		
		
		final Map<Id<Vehicle>, Set<Id<Link>>> vehicleId2linkIds = new HashMap<>();

		controler.addControlerListener(new VTTScomputation(vttsHandler));
		
		controler.addControlerListener( new StartupListener() {

			@Override
			public void notifyStartup(StartupEvent event) {
				event.getServices().getEvents().addHandler(new LinkLeaveEventHandler() {
					
					@Override
					public void reset(int iteration) {
						vehicleId2linkIds.clear();
					}
					
					@Override
					public void handleEvent(LinkLeaveEvent event) {
						if (vehicleId2linkIds.containsKey(event.getVehicleId())) {
							vehicleId2linkIds.get(event.getVehicleId()).add(event.getLinkId());
						} else {
							Set<Id<Link>> linkIds = new HashSet<Id<Link>>();
							linkIds.add(event.getLinkId());
							vehicleId2linkIds.put(event.getVehicleId(), linkIds);
						}
					}
				});		
			}		
		});
		
		
		controler.addOverridingModule(new OTFVisFileWriterModule());
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler.run();
		
		// high VTTS person's VTTS: 17.21 EUR/h (this is a value provided by the VTTSHandler)
		// low VTTS person's VTTS: 10.37 EUR/h (this is a value provided by the VTTSHandler)
		
		// high speed and costly route: 15 km; 50 km/h (travel time: 0.3h)
		// high VTTS person: 17.21 EUR/h * 0.3 h --> 5.163
		// low VTTS person: 10.37 * 0.3	--> 3.111
		
		// low speed and cheap route: 10 km; 10 km/h (travel time: 1h)
		// high VTTS person: 17.21 * 1 --> 17.21
		// low VTTS person: 10.37 * 1 --> 10.37
		
		// ==> the high VTTS person will choose the high speed route
		// ==> the low VTTS person will choose the high speed route
		
		double scoreSum = 0.;
		for (Person person : controler.getScenario().getPopulation().getPersons().values()) {
			scoreSum += person.getSelectedPlan().getScore();
		}
		Assert.assertEquals("not the same score sum", 147.36748899537443, scoreSum, MatsimTestUtils.EPSILON);
		
		Id<Link> longDistanceShortTimeLinkId = Id.createLinkId("link_1_2");
		
		for (Id<Vehicle> id : vehicleId2linkIds.keySet()) {

			if (id.toString().contains("highVTTS")) {
				
				// both persons use the fast route
				Assert.assertEquals(true, vehicleId2linkIds.get(id).contains(longDistanceShortTimeLinkId));
			}
			
			if (id.toString().contains("lowVTTS")) {

				// both persons use the fast route
				Assert.assertEquals(true, vehicleId2linkIds.get(id).contains(longDistanceShortTimeLinkId));
			}
		}		
	}
	
	/**
	 * use the standard router but set any distance cost to zero, the outcome should be the same as for the VTTS specific router
	 * 
	 */
	@Test
	public final void test4(){
				
		final String configFile = testUtils.getPackageInputDirectory() + "vttsSpecificRouter/configVTTS_noDistanceCost.xml";
		final Scenario scenario = ScenarioUtils.loadScenario( testUtils.loadConfig( configFile ) );
		final Controler controler = new Controler( scenario );
		final Map<Id<Vehicle>, Set<Id<Link>>> vehicleId2linkIds = new HashMap<>();
		
		controler.addControlerListener( new StartupListener() {

			@Override
			public void notifyStartup(StartupEvent event) {
				event.getServices().getEvents().addHandler(new LinkLeaveEventHandler() {
					
					@Override
					public void reset(int iteration) {
						vehicleId2linkIds.clear();
					}
					
					@Override
					public void handleEvent(LinkLeaveEvent event) {
						if (vehicleId2linkIds.containsKey(event.getVehicleId())) {
							vehicleId2linkIds.get(event.getVehicleId()).add(event.getLinkId());
						} else {
							Set<Id<Link>> linkIds = new HashSet<Id<Link>>();
							linkIds.add(event.getLinkId());
							vehicleId2linkIds.put(event.getVehicleId(), linkIds);
						}
					}
				});		
			}		
		});
		
		
		controler.addOverridingModule(new OTFVisFileWriterModule());
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler.run();
		
		double scoreSum = 0.;
		for (Person person : controler.getScenario().getPopulation().getPersons().values()) {
			scoreSum += person.getSelectedPlan().getScore();
		}
		Assert.assertEquals("not the same score sum", 147.36748899537443, scoreSum, MatsimTestUtils.EPSILON);
		
		// both persons use the fast route
		
		Id<Link> longDistanceShortTimeLinkId = Id.createLinkId("link_1_2");
		
		for (Id<Vehicle> id : vehicleId2linkIds.keySet()) {

			if (id.toString().contains("highVTTS")) {
				
				// both persons use the fast route
				Assert.assertEquals(true, vehicleId2linkIds.get(id).contains(longDistanceShortTimeLinkId));
			}
			
			if (id.toString().contains("lowVTTS")) {

				// both persons use the fast route
				Assert.assertEquals(true, vehicleId2linkIds.get(id).contains(longDistanceShortTimeLinkId));
			}
		}		
	}
	
	/**
	 * compares the default with the VTTS specific router with distance cost = 0
	 * 
	 */
	@Test
	public final void test5(){
		
		// starts the VTTS-specific router
		
		final String configFile1 = testUtils.getPackageInputDirectory() + "vttsSpecificRouter/configVTTS_noDistanceCost_largePopulation_1.xml";
		final Scenario scenario1 = ScenarioUtils.loadScenario( testUtils.loadConfig( configFile1 ) );
		final Controler controler1 = new Controler( scenario1 );
		final VTTSHandler vttsHandler1 = new VTTSHandler(controler1.getScenario());
		final VTTSTimeDistanceTravelDisutilityFactory factory1 = new VTTSTimeDistanceTravelDisutilityFactory(vttsHandler1, controler1.getConfig().planCalcScore()) ;
		factory1.setSigma(0.); // no randomness
		
		controler1.addOverridingModule(new AbstractModule(){
			@Override
			public void install() {
				this.bindCarTravelDisutilityFactory().toInstance( factory1 );
			}
		}); 		
		
		controler1.addControlerListener(new VTTScomputation(vttsHandler1));
		
		controler1.addOverridingModule(new OTFVisFileWriterModule());
		controler1.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler1.run();
		
		double scoreSum1 = 0.;
		for (Person person : controler1.getScenario().getPopulation().getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				scoreSum1 += plan.getScore();
			}
		}
		
		
		// default run
		
		final String configFile2 = testUtils.getPackageInputDirectory() + "vttsSpecificRouter/configVTTS_noDistanceCost_largePopulation_2.xml";
		final Scenario scenario2 = ScenarioUtils.loadScenario( testUtils.loadConfig( configFile2 ) );
		final Controler controler2 = new Controler( scenario2 );

		controler2.addOverridingModule(new OTFVisFileWriterModule());
		controler2.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler2.run();
		
		double scoreSum2 = 0.;
		for (Person person : controler2.getScenario().getPopulation().getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				scoreSum2 += plan.getScore();
			}
		}
		System.out.println(scoreSum1 + " / " + scoreSum2);
		Assert.assertEquals("not the same score sum", scoreSum1, scoreSum2, MatsimTestUtils.EPSILON);
			
	}
	
	/**
	 * compares the default with the VTTS specific router with distance cost = -0.002
	 */
	@Test
	public final void test6(){
		
		// starts the VTTS-specific router
		
		final String configFile1 = testUtils.getPackageInputDirectory() + "vttsSpecificRouter/configVTTS_withDistanceCost_largePopulation_1.xml";
		final Scenario scenario1 = ScenarioUtils.loadScenario( testUtils.loadConfig( configFile1 ) );
		final Controler controler1 = new Controler( scenario1 );
		final VTTSHandler vttsHandler1 = new VTTSHandler(controler1.getScenario());
		final VTTSTimeDistanceTravelDisutilityFactory factory1 = new VTTSTimeDistanceTravelDisutilityFactory(vttsHandler1, controler1.getConfig().planCalcScore()) ;
		factory1.setSigma(0.); // no randomness
		
		controler1.addOverridingModule(new AbstractModule(){
			@Override
			public void install() {
				this.bindCarTravelDisutilityFactory().toInstance( factory1 );
			}
		}); 		
		
		controler1.addControlerListener(new VTTScomputation(vttsHandler1));
		
		controler1.addOverridingModule(new OTFVisFileWriterModule());
		controler1.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler1.run();
		
		double scoreSum1 = 0.;
		for (Person person : controler1.getScenario().getPopulation().getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				scoreSum1 += plan.getScore();
			}
		}
		
		// default run
		
		final String configFile2 = testUtils.getPackageInputDirectory() + "vttsSpecificRouter/configVTTS_withDistanceCost_largePopulation_2.xml";
		final Scenario scenario2 = ScenarioUtils.loadScenario( testUtils.loadConfig( configFile2 ) );
		final Controler controler2 = new Controler( scenario2 );

		controler2.addOverridingModule(new OTFVisFileWriterModule());
		controler2.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler2.run();
		
		double scoreSum2 = 0.;
		for (Person person : controler2.getScenario().getPopulation().getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				scoreSum2 += plan.getScore();
			}
		}
		System.out.println(scoreSum1 + " / " + scoreSum2);
		Assert.assertTrue(scoreSum1 - scoreSum2 > 1.0);
	}
	
	/**
	 * 
	 * Setting sigma to 3.0, the rest is the same as in test1.
	 * 
	 * There are two agents. One agent is more pressed for time than the other one which results in different VTTS.
	 * There are two routes. One expensive but fast route. One cheap but slow route.
	 * 
	 * Accounting for the different VTTS results in a different outcome than assuming the default VTTS (as it is the case in the default router).	 * 
	 */
	@Test
	public final void test7(){
		
		// starts the VTTS-specific router
		
		final String configFile = testUtils.getPackageInputDirectory() + "vttsSpecificRouter/configVTTS.xml";
		final Scenario scenario = ScenarioUtils.loadScenario( testUtils.loadConfig( configFile ) );
		final Controler controler = new Controler( scenario );
		final VTTSHandler vttsHandler = new VTTSHandler(controler.getScenario());
		final VTTSTimeDistanceTravelDisutilityFactory factory = new VTTSTimeDistanceTravelDisutilityFactory(vttsHandler, controler.getConfig().planCalcScore()) ;
		factory.setSigma(3.0); // no randomness
		
		controler.addOverridingModule(new AbstractModule(){
			@Override
			public void install() {
				this.bindCarTravelDisutilityFactory().toInstance( factory );
			}
		}); 		
		
		final Map<Id<Vehicle>, Set<Id<Link>>> vehicleId2linkIds = new HashMap<>();

		controler.addControlerListener(new VTTScomputation(vttsHandler));
		
		controler.addControlerListener( new StartupListener() {

			@Override
			public void notifyStartup(StartupEvent event) {
				event.getServices().getEvents().addHandler(new LinkLeaveEventHandler() {
					
					@Override
					public void reset(int iteration) {
						vehicleId2linkIds.clear();
					}
					
					@Override
					public void handleEvent(LinkLeaveEvent event) {
						if (vehicleId2linkIds.containsKey(event.getVehicleId())) {
							vehicleId2linkIds.get(event.getVehicleId()).add(event.getLinkId());
						} else {
							Set<Id<Link>> linkIds = new HashSet<Id<Link>>();
							linkIds.add(event.getLinkId());
							vehicleId2linkIds.put(event.getVehicleId(), linkIds);
						}
					}
				});		
			}		
		});
		
		
		controler.addOverridingModule(new OTFVisFileWriterModule());
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler.run();
		
		
		Id<Link> longDistanceShortTimeLinkId = Id.createLinkId("link_1_2");
		Id<Link> shortDistanceLongTimeLinkId = Id.createLinkId("link_3_6");
		
		for (Id<Vehicle> id : vehicleId2linkIds.keySet()) {

			if (id.toString().contains("highVTTS")) {
				
				// the high VTTS person should use the fast but expensive route
				Assert.assertEquals(true, vehicleId2linkIds.get(id).contains(longDistanceShortTimeLinkId));
			}
			
			if (id.toString().contains("lowVTTS")) {

				// the low VTTS person should use the slow but cheap route
				Assert.assertEquals(true, vehicleId2linkIds.get(id).contains(shortDistanceLongTimeLinkId));
			}
		}		
	}
	
	
	/**
	 * Use the default router but set sigma to 3.0
	 * 
	 */
	@Test
	public final void test8(){
				
		final String configFile1 = testUtils.getPackageInputDirectory() + "vttsSpecificRouter/configVTTS.xml";
		final Scenario scenario = ScenarioUtils.loadScenario( testUtils.loadConfig( configFile1 ) );
		final Controler controler = new Controler( scenario );

		final Builder factory = new Builder( TransportMode.car, controler.getConfig().planCalcScore() );
		factory.setSigma(3.0);

		controler.addOverridingModule(new AbstractModule(){
			@Override
			public void install() {
				this.bindCarTravelDisutilityFactory().toInstance( factory );
			}
		}); 	
		
		final Map<Id<Vehicle>, Set<Id<Link>>> vehicleId2linkIds = new HashMap<>();

		controler.addControlerListener( new StartupListener() {

			@Override
			public void notifyStartup(StartupEvent event) {
				event.getServices().getEvents().addHandler(new LinkLeaveEventHandler() {
					
					@Override
					public void reset(int iteration) {
						vehicleId2linkIds.clear();
					}
					
					@Override
					public void handleEvent(LinkLeaveEvent event) {
						if (vehicleId2linkIds.containsKey(event.getVehicleId())) {
							vehicleId2linkIds.get(event.getVehicleId()).add(event.getLinkId());
						} else {
							Set<Id<Link>> linkIds = new HashSet<Id<Link>>();
							linkIds.add(event.getLinkId());
							vehicleId2linkIds.put(event.getVehicleId(), linkIds);
						}
					}
				});		
			}		
		});
		
		
		controler.addOverridingModule(new OTFVisFileWriterModule());
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler.run();
		
		Id<Link> longDistanceShortTimeLinkId = Id.createLinkId("link_1_2");
		Id<Link> shortDistanceLongTimeLinkId = Id.createLinkId("link_3_6");
		
		for (Id<Vehicle> id : vehicleId2linkIds.keySet()) {

			if (id.toString().contains("highVTTS")) {
				
				// the high VTTS person should use the fast but expensive route
				Assert.assertEquals(true, vehicleId2linkIds.get(id).contains(longDistanceShortTimeLinkId));
			}
			
			if (id.toString().contains("lowVTTS")) {

				// the low VTTS person should use the slow but cheap route
				Assert.assertEquals(true, vehicleId2linkIds.get(id).contains(shortDistanceLongTimeLinkId));
			}
		}		
	}
	
}
