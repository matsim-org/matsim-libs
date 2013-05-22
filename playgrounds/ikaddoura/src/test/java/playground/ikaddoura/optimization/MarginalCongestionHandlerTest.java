///* *********************************************************************** *
// * project: org.matsim.*
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// * copyright       : (C) 2013 by the members listed in the COPYING,        *
// *                   LICENSE and WARRANTY file.                            *
// * email           : info at matsim dot org                                *
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// *   This program is free software; you can redistribute it and/or modify  *
// *   it under the terms of the GNU General Public License as published by  *
// *   the Free Software Foundation; either version 2 of the License, or     *
// *   (at your option) any later version.                                   *
// *   See also COPYING, LICENSE and WARRANTY file                           *
// *                                                                         *
// * *********************************************************************** */
//
///**
// * 
// */
//package playground.ikaddoura.optimization;
//
//import java.util.ArrayList;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Set;
//
//import org.apache.log4j.Logger;
//import org.junit.Assert;
//import org.junit.Test;
//import org.matsim.api.core.v01.Id;
//import org.matsim.api.core.v01.network.Link;
//import org.matsim.api.core.v01.network.Network;
//import org.matsim.api.core.v01.network.Node;
//import org.matsim.api.core.v01.population.Activity;
//import org.matsim.api.core.v01.population.Leg;
//import org.matsim.api.core.v01.population.Person;
//import org.matsim.api.core.v01.population.Plan;
//import org.matsim.api.core.v01.population.Population;
//import org.matsim.core.api.experimental.events.EventsFactory;
//import org.matsim.core.api.experimental.events.EventsManager;
//import org.matsim.core.basic.v01.IdImpl;
//import org.matsim.core.config.Config;
//import org.matsim.core.config.ConfigUtils;
//import org.matsim.core.events.EventsUtils;
//import org.matsim.core.population.PopulationFactoryImpl;
//import org.matsim.core.population.routes.LinkNetworkRouteFactory;
//import org.matsim.core.population.routes.NetworkRoute;
//import org.matsim.core.scenario.ScenarioImpl;
//import org.matsim.core.scenario.ScenarioUtils;
//import org.matsim.testcases.MatsimTestCase;
//import org.matsim.testcases.MatsimTestUtils;
//
//import playground.ikaddoura.internalizationCar.MarginalCongestionEvent;
//import playground.ikaddoura.internalizationCar.MarginalCongestionEventHandler;
//import playground.ikaddoura.internalizationCar.MarginalCongestionHandler;
//
///**
// * @author ikaddoura
// *
// */
//public class MarginalCongestionHandlerTest extends MatsimTestCase {
//	private final static Logger log = Logger.getLogger(MarginalCongestionHandlerTest.class);
//
//	private EventsManager events;
//	private ScenarioImpl scenario;
//	private EventsFactory ef = new EventsFactory();
//	
//	private Id testAgent1 = new IdImpl("testAgent1");
//	private Id testAgent2 = new IdImpl("testAgent2");
//	private Id testAgent3 = new IdImpl("testAgent3");
//	
//	private Id linkId1 = new IdImpl("link1");
//	private Id linkId2 = new IdImpl("link2");
//	private Id linkId3 = new IdImpl("link3");
//	private Id linkId4 = new IdImpl("link4");
//	
//	// 2 agenten mit 10 sec Verzögerung
//	@Test
//	public final void testFlowCongestion1(){
//		
//		loadScenario();
//		setLinks_noStorageCapacityConstraints();
//		setPopulation();
//		final List<MarginalCongestionEvent> congestionEvents = new ArrayList<MarginalCongestionEvent>();
//							
//		events.addHandler( new MarginalCongestionEventHandler() {
//
//			@Override
//			public void reset(int iteration) {				
//			}
//
//			@Override
//			public void handleEvent(MarginalCongestionEvent event) {
//				congestionEvents.add(event);
//			}
//			
//		});
//						
//		MarginalCongestionHandler congestionHandler = new MarginalCongestionHandler(this.events, this.scenario);
//
//		// starte agent 1 und agent 2 mit 10 sec Verzögerung
//		// erst agent 1...
//		congestionHandler.handleEvent(ef.createAgentDepartureEvent(0, testAgent1, linkId1, "car"));
//		congestionHandler.handleEvent(ef.createLinkLeaveEvent(1, testAgent1, linkId1, testAgent1));
//		congestionHandler.handleEvent(ef.createLinkEnterEvent(1, testAgent1, linkId2, testAgent1));
//		// dann agent 2...
//		congestionHandler.handleEvent(ef.createAgentDepartureEvent(10, testAgent2, linkId1, "car"));
//		congestionHandler.handleEvent(ef.createLinkLeaveEvent(11, testAgent2, linkId1, testAgent2));
//		congestionHandler.handleEvent(ef.createLinkEnterEvent(11, testAgent2, linkId2, testAgent2));
//		
//		// agent 1 kann ohne Probleme link 2 verlassen...
//		congestionHandler.handleEvent(ef.createLinkLeaveEvent(51, testAgent1, linkId2, testAgent1));
//		congestionHandler.handleEvent(ef.createLinkEnterEvent(51, testAgent1, linkId3, testAgent1));
//		
//		// agent 2 muss allerdings durch die flow capacity warten (flow capacity: 1car/60sec)
//		// freeFlowLeaveTime + flowCapacityDelay + 1sec = linkEnterTime + freeTravelTime + flowCapacityDelay + 1sec
//		congestionHandler.handleEvent(ef.createLinkLeaveEvent(11 + 50 + 50 + 1, testAgent2, linkId2, testAgent2));
//		congestionHandler.handleEvent(ef.createLinkEnterEvent(11 + 50 + 50 + 1, testAgent2, linkId3, testAgent2));
//		
//		// *****************
//		
//		Assert.assertEquals("number of congestion events", 1, congestionEvents.size());
//
//		MarginalCongestionEvent congEvent = congestionEvents.get(0);
//		Assert.assertEquals("external delay", 50, congEvent.getDelay(), MatsimTestUtils.EPSILON);
//		Assert.assertEquals("congested link", linkId2.toString(), congEvent.getLinkId().toString());
//		Assert.assertEquals("causing Agent", testAgent1.toString(), congEvent.getCausingAgentId().toString());
//		Assert.assertEquals("affected Agent", testAgent2.toString(), congEvent.getAffectedAgentId().toString());
//	}
//	
//	// 2 agenten mit 59 sec Verzögerung
//	@Test
//	public final void testFlowCongestion2(){
//		
//		loadScenario();
//		setLinks_noStorageCapacityConstraints();
//		setPopulation();
//		final List<MarginalCongestionEvent> congestionEvents = new ArrayList<MarginalCongestionEvent>();
//							
//		events.addHandler( new MarginalCongestionEventHandler() {
//
//			@Override
//			public void reset(int iteration) {				
//			}
//
//			@Override
//			public void handleEvent(MarginalCongestionEvent event) {
//				congestionEvents.add(event);
//			}
//			
//		});
//						
//		MarginalCongestionHandler congestionHandler = new MarginalCongestionHandler(this.events, this.scenario);
//		
//		// starte agent 1 und agent 2 mit 59 sec Verzögerung
//		// erst agent 1...
//		congestionHandler.handleEvent(ef.createAgentDepartureEvent(0, testAgent1, linkId1, "car"));
//		congestionHandler.handleEvent(ef.createLinkLeaveEvent(1, testAgent1, linkId1, testAgent1));
//		congestionHandler.handleEvent(ef.createLinkEnterEvent(1, testAgent1, linkId2, testAgent1));
//		// dann agent 2...
//		congestionHandler.handleEvent(ef.createAgentDepartureEvent(59, testAgent2, linkId1, "car"));
//		congestionHandler.handleEvent(ef.createLinkLeaveEvent(60, testAgent2, linkId1, testAgent2));
//		congestionHandler.handleEvent(ef.createLinkEnterEvent(60, testAgent2, linkId2, testAgent2));
//		
//		// agent 1 kann ohne Probleme link 2 verlassen...
//		congestionHandler.handleEvent(ef.createLinkLeaveEvent(51, testAgent1, linkId2, testAgent1));
//		congestionHandler.handleEvent(ef.createLinkEnterEvent(51, testAgent1, linkId3, testAgent1));
//		
//		// agent 2 muss allerdings durch die flow capacity warten (flow capacity: 1car/60sec)
//		// freeFlowLeaveTime + flowCapacityDelay + 1sec = enterTime + freeTravelTime + flowCapacityDelay + 1sec
//		congestionHandler.handleEvent(ef.createLinkLeaveEvent(60 + 50 + 1 + 1, testAgent2, linkId2, testAgent2));
//		congestionHandler.handleEvent(ef.createLinkEnterEvent(60 + 50 + 1 + 1, testAgent2, linkId3, testAgent2));
//		
//		// *****************
//		
//		Assert.assertEquals("number of congestion events", 1, congestionEvents.size());
//
//		MarginalCongestionEvent congEvent = congestionEvents.get(0);
//		Assert.assertEquals("external delay", 1, congEvent.getDelay(), MatsimTestUtils.EPSILON);
//		Assert.assertEquals("congested link", linkId2.toString(), congEvent.getLinkId().toString());
//		Assert.assertEquals("causing Agent", testAgent1.toString(), congEvent.getCausingAgentId().toString());
//		Assert.assertEquals("affected Agent", testAgent2.toString(), congEvent.getAffectedAgentId().toString());
//	}	
//
//		// 2 agenten mit 60 sec Verzögerung
//		@Test
//		public final void testFlowCongestion3(){
//			
//			loadScenario();
//			setLinks_noStorageCapacityConstraints();
//			setPopulation();
//			final List<MarginalCongestionEvent> congestionEvents = new ArrayList<MarginalCongestionEvent>();
//								
//			events.addHandler( new MarginalCongestionEventHandler() {
//
//				@Override
//				public void reset(int iteration) {				
//				}
//
//				@Override
//				public void handleEvent(MarginalCongestionEvent event) {
//					congestionEvents.add(event);
//				}
//				
//			});
//							
//			MarginalCongestionHandler congestionHandler = new MarginalCongestionHandler(this.events, this.scenario);
//			
//			// starte agent 1 und agent 2 mit 60 sec Verzögerung
//			// erst agent 1...
//			congestionHandler.handleEvent(ef.createAgentDepartureEvent(0, testAgent1, linkId1, "car"));
//			congestionHandler.handleEvent(ef.createLinkLeaveEvent(1, testAgent1, linkId1, testAgent1));
//			congestionHandler.handleEvent(ef.createLinkEnterEvent(1, testAgent1, linkId2, testAgent1));
//			// dann agent 2...
//			congestionHandler.handleEvent(ef.createAgentDepartureEvent(60, testAgent2, linkId1, "car"));
//			congestionHandler.handleEvent(ef.createLinkLeaveEvent(61, testAgent2, linkId1, testAgent2));
//			congestionHandler.handleEvent(ef.createLinkEnterEvent(61, testAgent2, linkId2, testAgent2));
//			
//			// agent 1 kann ohne Probleme link 2 verlassen...
//			congestionHandler.handleEvent(ef.createLinkLeaveEvent(51, testAgent1, linkId2, testAgent1));
//			congestionHandler.handleEvent(ef.createLinkEnterEvent(51, testAgent1, linkId3, testAgent1));
//			
//			// agent 2 muss allerdings durch die flow capacity warten (flow capacity: 1car/60sec)
//			// freeFlowLeaveTime + flowCapacityDelay + 1sec = enterTime + freeTravelTime + flowCapacityDelay + 1sec
//			congestionHandler.handleEvent(ef.createLinkLeaveEvent(61 + 50 + 0 + 1, testAgent2, linkId2, testAgent2));
//			congestionHandler.handleEvent(ef.createLinkEnterEvent(61 + 50 + 0 + 1, testAgent2, linkId3, testAgent2));
//			
//			// *****************
//			
//			Assert.assertEquals("number of congestion events", 0, congestionEvents.size());
//		}
//		
//		// 3 agenten mit je 10 sec Verzögerung
//		@Test
//		public final void testFlowCongestion4(){
//			
//			loadScenario();
//			setLinks_noStorageCapacityConstraints();
//			setPopulation();
//			final List<MarginalCongestionEvent> congestionEvents = new ArrayList<MarginalCongestionEvent>();
//								
//			events.addHandler( new MarginalCongestionEventHandler() {
//
//				@Override
//				public void reset(int iteration) {				
//				}
//
//				@Override
//				public void handleEvent(MarginalCongestionEvent event) {
//					congestionEvents.add(event);
//				}
//				
//			});
//							
//			MarginalCongestionHandler congestionHandler = new MarginalCongestionHandler(this.events, this.scenario);
//			
//			// starte agent 1
//			congestionHandler.handleEvent(ef.createAgentDepartureEvent(0, testAgent1, linkId1, "car"));
//			congestionHandler.handleEvent(ef.createLinkLeaveEvent(1, testAgent1, linkId1, testAgent1));
//			congestionHandler.handleEvent(ef.createLinkEnterEvent(1, testAgent1, linkId2, testAgent1));
//			
//			// starte agent 2 mit 10 sec Verzögerung
//			congestionHandler.handleEvent(ef.createAgentDepartureEvent(10, testAgent2, linkId1, "car"));
//			congestionHandler.handleEvent(ef.createLinkLeaveEvent(11, testAgent2, linkId1, testAgent2));
//			congestionHandler.handleEvent(ef.createLinkEnterEvent(11, testAgent2, linkId2, testAgent2));
//			
//			// starte agent 3 mit nochmal 10 sec Verzögerung
//			congestionHandler.handleEvent(ef.createAgentDepartureEvent(20, testAgent3, linkId1, "car"));
//			congestionHandler.handleEvent(ef.createLinkLeaveEvent(21, testAgent3, linkId1, testAgent3));
//			congestionHandler.handleEvent(ef.createLinkEnterEvent(21, testAgent3, linkId2, testAgent3));
//			
//			// agent 1 kann problemlos link 2 verlassen
//			congestionHandler.handleEvent(ef.createLinkLeaveEvent(51, testAgent1, linkId2, testAgent1));
//			congestionHandler.handleEvent(ef.createLinkEnterEvent(51, testAgent1, linkId3, testAgent1));
//			
//			// agent 2 muss warten
//			// freeFlowLeaveTime + flowCapacityDelay + 1sec = enterTime + freeTravelTime + flowCapacityDelay + 1sec
//			congestionHandler.handleEvent(ef.createLinkLeaveEvent(11 + 50 + 50 + 1, testAgent2, linkId2, testAgent2));
//			congestionHandler.handleEvent(ef.createLinkEnterEvent(11 + 50 + 50 + 1, testAgent2, linkId3, testAgent2));
//			
//			// agent 3 muss noch mehr warten
//			// freeFlowLeaveTime + flowCapacityDelay + 1sec = enterTime + freeTravelTime + flowCapacityDelay + 1sec
//			congestionHandler.handleEvent(ef.createLinkLeaveEvent(21 + 50 + 100 + 1, testAgent3, linkId2, testAgent3));
//			congestionHandler.handleEvent(ef.createLinkEnterEvent(21 + 50 + 100 + 1, testAgent3, linkId3, testAgent3));
//
//			
//			// *****************
//						
//			for (MarginalCongestionEvent event : congestionEvents){
//				
//				if (event.getCausingAgentId().toString().equals(testAgent1.toString())){
//
//					if (event.getAffectedAgentId().toString().equals(testAgent1.toString())){
//						throw new RuntimeException();
//
//					} else if (event.getAffectedAgentId().toString().equals(testAgent2.toString())){
//						Assert.assertEquals("external delay", 50, event.getDelay(), MatsimTestUtils.EPSILON);
//						Assert.assertEquals("congested link", linkId2.toString(), event.getLinkId().toString());
//						
//					} else if (event.getAffectedAgentId().toString().equals(testAgent3.toString())){
//						Assert.assertEquals("external delay", 40, event.getDelay(), MatsimTestUtils.EPSILON);
//						Assert.assertEquals("congested link", linkId2.toString(), event.getLinkId().toString());
//						
//					} else {
//						throw new RuntimeException();
//					}
//					
//				} else if (event.getCausingAgentId().toString().equals(testAgent2.toString())) {
//					if (event.getAffectedAgentId().toString().equals(testAgent1.toString())){
//						throw new RuntimeException();
//						
//					} else if (event.getAffectedAgentId().toString().equals(testAgent2.toString())){
//						throw new RuntimeException();
//										
//					} else if (event.getAffectedAgentId().toString().equals(testAgent3.toString())){
//						Assert.assertEquals("external delay", 60, event.getDelay(), MatsimTestUtils.EPSILON);
//						Assert.assertEquals("congested link", linkId2.toString(), event.getLinkId().toString());
//						
//					} else {
//						throw new RuntimeException();
//					}	
//				
//				} else {
//					throw new RuntimeException();
//				}
//			}
//		}
//		
//		// 3 agenten, agent 2 mit 5 sec, agent 3 mit 120 sec Verzögerung
//		@Test
//		public final void testFlowCongestion5(){
//			
//			loadScenario();
//			setLinks_noStorageCapacityConstraints();
//			setPopulation();
//			final List<MarginalCongestionEvent> congestionEvents = new ArrayList<MarginalCongestionEvent>();
//								
//			events.addHandler( new MarginalCongestionEventHandler() {
//
//				@Override
//				public void reset(int iteration) {				
//				}
//
//				@Override
//				public void handleEvent(MarginalCongestionEvent event) {
//					congestionEvents.add(event);
//				}
//				
//			});
//							
//			MarginalCongestionHandler congestionHandler = new MarginalCongestionHandler(this.events, this.scenario);
//			
//			// starte agent 1
//			congestionHandler.handleEvent(ef.createAgentDepartureEvent(0, testAgent1, linkId1, "car"));
//			congestionHandler.handleEvent(ef.createLinkLeaveEvent(1, testAgent1, linkId1, testAgent1));
//			congestionHandler.handleEvent(ef.createLinkEnterEvent(1, testAgent1, linkId2, testAgent1));
//			
//			// starte agent 2 mit 5 sec Verzögerung
//			congestionHandler.handleEvent(ef.createAgentDepartureEvent(5, testAgent2, linkId1, "car"));
//			congestionHandler.handleEvent(ef.createLinkLeaveEvent(6, testAgent2, linkId1, testAgent2));
//			congestionHandler.handleEvent(ef.createLinkEnterEvent(6, testAgent2, linkId2, testAgent2));
//			
//			// starte agent 3 mit 120 sec Verzögerung
//			congestionHandler.handleEvent(ef.createAgentDepartureEvent(120, testAgent3, linkId1, "car"));
//			congestionHandler.handleEvent(ef.createLinkLeaveEvent(121, testAgent3, linkId1, testAgent3));
//			congestionHandler.handleEvent(ef.createLinkEnterEvent(121, testAgent3, linkId2, testAgent3));
//			
//			// agent 1 kann problemlos link 2 verlassen
//			congestionHandler.handleEvent(ef.createLinkLeaveEvent(51, testAgent1, linkId2, testAgent1));
//			congestionHandler.handleEvent(ef.createLinkEnterEvent(51, testAgent1, linkId3, testAgent1));
//			
//			// agent 2 muss warten
//			// freeFlowLeaveTime + flowCapacityDelay + 1sec = enterTime + freeTravelTime + flowCapacityDelay + 1sec
//			congestionHandler.handleEvent(ef.createLinkLeaveEvent(6 + 50 + 55 + 1, testAgent2, linkId2, testAgent2));
//			congestionHandler.handleEvent(ef.createLinkEnterEvent(6 + 50 + 55 + 1, testAgent2, linkId3, testAgent2));
//			
//			// agent 3 kann problemlos link 2 verlassen (der Stau hat sich bereits aufgelöst)
//			// freeFlowLeaveTime + flowCapacityDelay + 1sec = enterTime + freeTravelTime + flowCapacityDelay + 1sec
//			congestionHandler.handleEvent(ef.createLinkLeaveEvent(121 + 50 + 0 + 1, testAgent3, linkId2, testAgent3));
//			congestionHandler.handleEvent(ef.createLinkEnterEvent(121 + 50 + 0 + 1, testAgent3, linkId3, testAgent3));
//			
//			// *****************
//						
//			for (MarginalCongestionEvent event : congestionEvents){
//				
//				if (event.getCausingAgentId().toString().equals(testAgent1.toString())){
//
//					if (event.getAffectedAgentId().toString().equals(testAgent1.toString())){
//						throw new RuntimeException();
//
//					} else if (event.getAffectedAgentId().toString().equals(testAgent2.toString())){
//						Assert.assertEquals("external delay", 55, event.getDelay(), MatsimTestUtils.EPSILON);
//						Assert.assertEquals("congested link", linkId2.toString(), event.getLinkId().toString());
//						
//					} else if (event.getAffectedAgentId().toString().equals(testAgent3.toString())){
//						throw new RuntimeException();
//						
//					} else {
//						throw new RuntimeException();
//					}
//					
//				} else if (event.getCausingAgentId().toString().equals(testAgent2.toString())) {
//					if (event.getAffectedAgentId().toString().equals(testAgent1.toString())){
//						throw new RuntimeException();
//						
//					} else if (event.getAffectedAgentId().toString().equals(testAgent2.toString())){
//						throw new RuntimeException();
//										
//					} else if (event.getAffectedAgentId().toString().equals(testAgent3.toString())){
//						throw new RuntimeException();
//						
//					} else {
//						throw new RuntimeException();
//					}	
//				
//				} else {
//					throw new RuntimeException();
//				}
//			}
//		}
//		
//		// 3 agenten, agent 2 mit 5 sec, agent 3 mit 120 sec Verzögerung
//				@Test
//				public final void testFlowCongestion5b(){
//					
//					loadScenario();
//					setLinks_noStorageCapacityConstraints();
//					setPopulation();
//					final List<MarginalCongestionEvent> congestionEvents = new ArrayList<MarginalCongestionEvent>();
//										
//					events.addHandler( new MarginalCongestionEventHandler() {
//
//						@Override
//						public void reset(int iteration) {				
//						}
//
//						@Override
//						public void handleEvent(MarginalCongestionEvent event) {
//							congestionEvents.add(event);
//						}
//						
//					});
//									
//					MarginalCongestionHandler congestionHandler = new MarginalCongestionHandler(this.events, this.scenario);
//					
//					// starte agent 1
//					congestionHandler.handleEvent(ef.createAgentDepartureEvent(0, testAgent1, linkId1, "car"));
//					congestionHandler.handleEvent(ef.createLinkLeaveEvent(1, testAgent1, linkId1, testAgent1));
//					congestionHandler.handleEvent(ef.createLinkEnterEvent(1, testAgent1, linkId2, testAgent1));
//					
//					// starte agent 2 mit 5 sec Verzögerung
//					congestionHandler.handleEvent(ef.createAgentDepartureEvent(5, testAgent2, linkId1, "car"));
//					congestionHandler.handleEvent(ef.createLinkLeaveEvent(6, testAgent2, linkId1, testAgent2));
//					congestionHandler.handleEvent(ef.createLinkEnterEvent(6, testAgent2, linkId2, testAgent2));
//					
//					// starte agent 3 mit 120 sec Verzögerung
//					congestionHandler.handleEvent(ef.createAgentDepartureEvent(120, testAgent3, linkId1, "car"));
//					congestionHandler.handleEvent(ef.createLinkLeaveEvent(121, testAgent3, linkId1, testAgent3));
//					congestionHandler.handleEvent(ef.createLinkEnterEvent(121, testAgent3, linkId2, testAgent3));
//					
//					// agent 1 kann problemlos link 2 verlassen
//					congestionHandler.handleEvent(ef.createLinkLeaveEvent(51, testAgent1, linkId2, testAgent1));
//					congestionHandler.handleEvent(ef.createLinkEnterEvent(51, testAgent1, linkId3, testAgent1));
//					
//					// agent 2 muss warten
//					// freeFlowLeaveTime + flowCapacityDelay + 1sec = enterTime + freeTravelTime + flowCapacityDelay + 1sec
//					congestionHandler.handleEvent(ef.createLinkLeaveEvent(6 + 50 + 55 + 1, testAgent2, linkId2, testAgent2));
//					congestionHandler.handleEvent(ef.createLinkEnterEvent(6 + 50 + 55 + 1, testAgent2, linkId3, testAgent2));
//					
//					// agent 3 kann problemlos link 2 verlassen (der Stau hat sich bereits aufgelöst)
//					// freeFlowLeaveTime + flowCapacityDelay + 1sec = enterTime + freeTravelTime + flowCapacityDelay + 1sec
//					congestionHandler.handleEvent(ef.createLinkLeaveEvent(121 + 50 + 0 + 1, testAgent3, linkId2, testAgent3));
//					congestionHandler.handleEvent(ef.createLinkEnterEvent(121 + 50 + 0 + 1, testAgent3, linkId3, testAgent3));
//					
//					// *****************
//								
//					for (MarginalCongestionEvent event : congestionEvents){
//						
//						if (event.getCausingAgentId().toString().equals(testAgent1.toString())){
//
//							if (event.getAffectedAgentId().toString().equals(testAgent1.toString())){
//								throw new RuntimeException();
//
//							} else if (event.getAffectedAgentId().toString().equals(testAgent2.toString())){
//								Assert.assertEquals("external delay", 55, event.getDelay(), MatsimTestUtils.EPSILON);
//								Assert.assertEquals("congested link", linkId2.toString(), event.getLinkId().toString());
//								
//							} else if (event.getAffectedAgentId().toString().equals(testAgent3.toString())){
//								throw new RuntimeException();
//								
//							} else {
//								throw new RuntimeException();
//							}
//							
//						} else if (event.getCausingAgentId().toString().equals(testAgent2.toString())) {
//							if (event.getAffectedAgentId().toString().equals(testAgent1.toString())){
//								throw new RuntimeException();
//								
//							} else if (event.getAffectedAgentId().toString().equals(testAgent2.toString())){
//								throw new RuntimeException();
//												
//							} else if (event.getAffectedAgentId().toString().equals(testAgent3.toString())){
//								throw new RuntimeException();
//								
//							} else {
//								throw new RuntimeException();
//							}	
//						
//						} else {
//							throw new RuntimeException();
//						}
//					}
//				}
//		
//		// 2 agenten, agent 2 20 sec nach agent 1, agent 2 zusätzlich um 100 sec verzögert
//		@Test
//		public final void testStorageCongestion1(){
//			
//			loadScenario();
//			setPopulation();
//			setLinks_storageCapacityConstraints_link3_link4();
//			
//			final List<MarginalCongestionEvent> congestionEvents = new ArrayList<MarginalCongestionEvent>();
//								
//			events.addHandler( new MarginalCongestionEventHandler() {
//
//				@Override
//				public void reset(int iteration) {				
//				}
//
//				@Override
//				public void handleEvent(MarginalCongestionEvent event) {
//					congestionEvents.add(event);
//				}
//				
//			});
//							
//			MarginalCongestionHandler congestionHandler = new MarginalCongestionHandler(this.events, this.scenario);
//			
//			// starte agent 1
//			congestionHandler.handleEvent(ef.createAgentDepartureEvent(0, testAgent1, linkId1, "car"));
//			congestionHandler.handleEvent(ef.createLinkLeaveEvent(1, testAgent1, linkId1, testAgent1));
//			congestionHandler.handleEvent(ef.createLinkEnterEvent(1, testAgent1, linkId2, testAgent1));
//			
//			// starte agent 2 mit 20 sec Verzögerung
//			congestionHandler.handleEvent(ef.createAgentDepartureEvent(20, testAgent2, linkId1, "car"));
//			congestionHandler.handleEvent(ef.createLinkLeaveEvent(21, testAgent2, linkId1, testAgent2));
//			congestionHandler.handleEvent(ef.createLinkEnterEvent(21, testAgent2, linkId2, testAgent2));
//			
//			// agent 1 kann link 2 problemlos verlassen
//			congestionHandler.handleEvent(ef.createLinkLeaveEvent(51, testAgent1, linkId2, testAgent1));
//			congestionHandler.handleEvent(ef.createLinkEnterEvent(51, testAgent1, linkId3, testAgent1));
//			
//			// agent 2 muss durch die flowCapacity 40 sec warten und wird dann nochmal zusätzlich um 100 sec verzögert
//			// freeFlowLeaveTime + flowCapacityDelay + storageCapacityDelay + 1sec = enterTime + freeTravelTime + flowCapacityDelay + storageCapacityDelay + 1sec
//			congestionHandler.handleEvent(ef.createLinkLeaveEvent(21 + 50 + 40 + 100 + 1, testAgent2, linkId2, testAgent2));
//			congestionHandler.handleEvent(ef.createLinkEnterEvent(21 + 50 + 40 + 100 + 1, testAgent2, linkId3, testAgent2));
//
//			// *****************
//			
//			Assert.assertEquals("number of congestion events", 1, congestionEvents.size());
//
//			MarginalCongestionEvent congEvent = congestionEvents.get(0);
//			Assert.assertEquals("capacity constraint", "storageCapacity", congEvent.getCapacityConstraint());
//			Assert.assertEquals("external delay", 100, congEvent.getDelay(), MatsimTestUtils.EPSILON);
//			Assert.assertEquals("congested link", linkId2.toString(), congEvent.getLinkId().toString());
//			Assert.assertEquals("causing Agent", testAgent1.toString(), congEvent.getCausingAgentId().toString());
//			Assert.assertEquals("affected Agent", testAgent2.toString(), congEvent.getAffectedAgentId().toString());
//		}	
//			
//		// 3 agenten, agent 2 20 sec nach agent 1, agent 2 zusätzlich um 100 sec verzögert, agent 3 nach agent 2 auf link 2
//		@Test
//		public final void testStorageCongestion2(){
//			
//			loadScenario();
//			setPopulation();
//			setLinks_storageCapacityConstraints_link3_link4();
//			
//			final List<MarginalCongestionEvent> congestionEvents = new ArrayList<MarginalCongestionEvent>();
//								
//			events.addHandler( new MarginalCongestionEventHandler() {
//
//				@Override
//				public void reset(int iteration) {				
//				}
//
//				@Override
//				public void handleEvent(MarginalCongestionEvent event) {
//					congestionEvents.add(event);
//				}
//				
//			});
//							
//			MarginalCongestionHandler congestionHandler = new MarginalCongestionHandler(this.events, this.scenario);
//			
//			// starte agent 1
//			congestionHandler.handleEvent(ef.createAgentDepartureEvent(0, testAgent1, linkId1, "car"));
//			congestionHandler.handleEvent(ef.createLinkLeaveEvent(1, testAgent1, linkId1, testAgent1));
//			congestionHandler.handleEvent(ef.createLinkEnterEvent(1, testAgent1, linkId2, testAgent1));
//			
//			// starte agent 2 mit 20 sec Verzögerung
//			congestionHandler.handleEvent(ef.createAgentDepartureEvent(20, testAgent2, linkId1, "car"));
//			congestionHandler.handleEvent(ef.createLinkLeaveEvent(21, testAgent2, linkId1, testAgent2));
//			congestionHandler.handleEvent(ef.createLinkEnterEvent(21, testAgent2, linkId2, testAgent2));
//			
//			// agent 3 fährt nach agent 2 auf link2, dort bleibt er die ganze Zeit über
//			congestionHandler.handleEvent(ef.createLinkEnterEvent(30, testAgent3, linkId2, testAgent3));
//			
//			// agent 1 kann link 2 problemlos verlassen
//			congestionHandler.handleEvent(ef.createLinkLeaveEvent(51, testAgent1, linkId2, testAgent1));
//			congestionHandler.handleEvent(ef.createLinkEnterEvent(51, testAgent1, linkId3, testAgent1));
//			
//			// agent 2 muss durch die flowCapacity 40 sec warten und wird dann nochmal zusätzlich um 100 sec verzögert
//			// freeFlowLeaveTime + flowCapacityDelay + storageCapacityDelay + 1sec = enterTime + freeTravelTime + flowCapacityDelay + storageCapacityDelay + 1sec
//			congestionHandler.handleEvent(ef.createLinkLeaveEvent(21 + 50 + 40 + 100 + 1, testAgent2, linkId2, testAgent2));
//			congestionHandler.handleEvent(ef.createLinkEnterEvent(21 + 50 + 40 + 100 + 1, testAgent2, linkId3, testAgent2));
//
//			// *****************
//			
//			Assert.assertEquals("number of congestion events", 1, congestionEvents.size());
//
//			for (MarginalCongestionEvent congEvent : congestionEvents) {
//				
//				if (congEvent.getCausingAgentId().toString().equals(testAgent1.toString())){
//					Assert.assertEquals("capacity constraint", "storageCapacity", congEvent.getCapacityConstraint());
//					Assert.assertEquals("external delay", 100, congEvent.getDelay(), MatsimTestUtils.EPSILON);
//					Assert.assertEquals("congested link", linkId2.toString(), congEvent.getLinkId().toString());
//					Assert.assertEquals("affected Agent", testAgent2.toString(), congEvent.getAffectedAgentId().toString());
//				
//				} else {
//					throw new RuntimeException();
//				}
//			}
//		}	
//		
//		// 3 agenten, agent 2 20 sec nach agent 1, agent 2 zusätzlich um 100 sec verzögert, agent 3 vor agent 1 und 2 auf link 2
//		@Test
//		public final void testStorageCongestion3(){
//			
//			loadScenario();
//			setPopulation();
//			setLinks_storageCapacityConstraints_link3_link4();
//			
//			final List<MarginalCongestionEvent> congestionEvents = new ArrayList<MarginalCongestionEvent>();
//								
//			events.addHandler( new MarginalCongestionEventHandler() {
//
//				@Override
//				public void reset(int iteration) {				
//				}
//
//				@Override
//				public void handleEvent(MarginalCongestionEvent event) {
//					congestionEvents.add(event);
//				}
//				
//			});
//							
//			MarginalCongestionHandler congestionHandler = new MarginalCongestionHandler(this.events, this.scenario);
//			
//			// agent 3 fährt vor agent 2 und 1 auf link 2 und bleibt dort die ganze Zeit über
//			congestionHandler.handleEvent(ef.createLinkEnterEvent(0, testAgent3, linkId2, testAgent3));
//			
//			// starte agent 1
//			congestionHandler.handleEvent(ef.createAgentDepartureEvent(100, testAgent1, linkId1, "car"));
//			congestionHandler.handleEvent(ef.createLinkLeaveEvent(101, testAgent1, linkId1, testAgent1));
//			congestionHandler.handleEvent(ef.createLinkEnterEvent(101, testAgent1, linkId2, testAgent1));
//						
//			// starte agent 2 mit 20 sec Verzögerung
//			congestionHandler.handleEvent(ef.createAgentDepartureEvent(120, testAgent2, linkId1, "car"));
//			congestionHandler.handleEvent(ef.createLinkLeaveEvent(121, testAgent2, linkId1, testAgent2));
//			congestionHandler.handleEvent(ef.createLinkEnterEvent(121, testAgent2, linkId2, testAgent2));
//			
//			// agent 1 kann link 2 problemlos verlassen
//			congestionHandler.handleEvent(ef.createLinkLeaveEvent(151, testAgent1, linkId2, testAgent1));
//			congestionHandler.handleEvent(ef.createLinkEnterEvent(151, testAgent1, linkId3, testAgent1));
//			
//			// agent 2 muss durch die flowCapacity 40 sec warten und wird dann nochmal zusätzlich um 100 sec verzögert
//			// freeFlowLeaveTime + flowCapacityDelay + storageCapacityDelay + 1sec = enterTime + freeTravelTime + flowCapacityDelay + storageCapacityDelay + 1sec
//			congestionHandler.handleEvent(ef.createLinkLeaveEvent(121 + 50 + 40 + 100 + 1, testAgent2, linkId2, testAgent2));
//			congestionHandler.handleEvent(ef.createLinkEnterEvent(121 + 50 + 40 + 100 + 1, testAgent2, linkId3, testAgent2));
//
//			// *****************
//			
//			Assert.assertEquals("number of congestion events", 2, congestionEvents.size());
//
//			for (MarginalCongestionEvent congEvent : congestionEvents) {
//				
//				if (congEvent.getCausingAgentId().toString().equals(testAgent1.toString())){
//					Assert.assertEquals("capacity constraint", "storageCapacity", congEvent.getCapacityConstraint());
//					Assert.assertEquals("external delay", 50, congEvent.getDelay(), MatsimTestUtils.EPSILON);
//					Assert.assertEquals("congested link", linkId2.toString(), congEvent.getLinkId().toString());
//					Assert.assertEquals("affected Agent", testAgent2.toString(), congEvent.getAffectedAgentId().toString());
//				
//				} else if (congEvent.getCausingAgentId().toString().equals(testAgent3.toString())){
//					Assert.assertEquals("capacity constraint", "storageCapacity", congEvent.getCapacityConstraint());
//					Assert.assertEquals("external delay", 50, congEvent.getDelay(), MatsimTestUtils.EPSILON);
//					Assert.assertEquals("congested link", linkId2.toString(), congEvent.getLinkId().toString());
//					Assert.assertEquals("affected Agent", testAgent2.toString(), congEvent.getAffectedAgentId().toString());
//				
//				} else {
//					throw new RuntimeException();
//				}
//			}
//		}
//		
//		// 3 agenten, agent 2 20 sec nach agent 1, agent 2 zusätzlich um 100 sec verzögert, agent 3 vor agent 1 und 2 auf link 4, storage capacity auf link 4 erreicht
//		@Test
//		public final void testStorageCongestion4(){
//			
//			loadScenario();
//			setPopulation();
//			setLinks_storageCapacityConstraints_link3_link4();
//			
//			final List<MarginalCongestionEvent> congestionEvents = new ArrayList<MarginalCongestionEvent>();
//								
//			events.addHandler( new MarginalCongestionEventHandler() {
//
//				@Override
//				public void reset(int iteration) {				
//				}
//
//				@Override
//				public void handleEvent(MarginalCongestionEvent event) {
//					congestionEvents.add(event);
//				}
//				
//			});
//							
//			MarginalCongestionHandler congestionHandler = new MarginalCongestionHandler(this.events, this.scenario);
//			
//			// agent 3 fährt vor agent 2 und 1 auf link 4 und bleibt dort die ganze Zeit über
//			// storage capacity auf link 4 erreicht
//			congestionHandler.handleEvent(ef.createLinkEnterEvent(0, testAgent3, linkId4, testAgent3));
//			
//			// starte agent 1
//			congestionHandler.handleEvent(ef.createAgentDepartureEvent(100, testAgent1, linkId1, "car"));
//			congestionHandler.handleEvent(ef.createLinkLeaveEvent(101, testAgent1, linkId1, testAgent1));
//			congestionHandler.handleEvent(ef.createLinkEnterEvent(101, testAgent1, linkId2, testAgent1));
//						
//			// starte agent 2 mit 20 sec Verzögerung
//			congestionHandler.handleEvent(ef.createAgentDepartureEvent(120, testAgent2, linkId1, "car"));
//			congestionHandler.handleEvent(ef.createLinkLeaveEvent(121, testAgent2, linkId1, testAgent2));
//			congestionHandler.handleEvent(ef.createLinkEnterEvent(121, testAgent2, linkId2, testAgent2));
//			
//			// agent 1 kann link 2 problemlos verlassen
//			congestionHandler.handleEvent(ef.createLinkLeaveEvent(151, testAgent1, linkId2, testAgent1));
//			congestionHandler.handleEvent(ef.createLinkEnterEvent(151, testAgent1, linkId3, testAgent1));
//			
//			// agent 2 muss durch die flowCapacity 40 sec warten und wird dann nochmal zusätzlich um 100 sec verzögert
//			// freeFlowLeaveTime + flowCapacityDelay + storageCapacityDelay + 1sec = enterTime + freeTravelTime + flowCapacityDelay + storageCapacityDelay + 1sec
//			congestionHandler.handleEvent(ef.createLinkLeaveEvent(121 + 50 + 40 + 100 + 1, testAgent2, linkId2, testAgent2));
//			congestionHandler.handleEvent(ef.createLinkEnterEvent(121 + 50 + 40 + 100 + 1, testAgent2, linkId3, testAgent2));
//
//			// *****************
//			
//			Assert.assertEquals("number of congestion events", 2, congestionEvents.size());
//
//			for (MarginalCongestionEvent congEvent : congestionEvents) {
//				
//				if (congEvent.getCausingAgentId().toString().equals(testAgent1.toString())){
//					Assert.assertEquals("capacity constraint", "storageCapacity", congEvent.getCapacityConstraint());
//					Assert.assertEquals("external delay", 50, congEvent.getDelay(), MatsimTestUtils.EPSILON);
//					Assert.assertEquals("congested link", linkId2.toString(), congEvent.getLinkId().toString());
//					Assert.assertEquals("affected Agent", testAgent2.toString(), congEvent.getAffectedAgentId().toString());
//				
//				} else if (congEvent.getCausingAgentId().toString().equals(testAgent3.toString())){
//					Assert.assertEquals("capacity constraint", "storageCapacity", congEvent.getCapacityConstraint());
//					Assert.assertEquals("external delay", 50, congEvent.getDelay(), MatsimTestUtils.EPSILON);
//					Assert.assertEquals("congested link", linkId2.toString(), congEvent.getLinkId().toString());
//					Assert.assertEquals("affected Agent", testAgent2.toString(), congEvent.getAffectedAgentId().toString());
//				
//				} else {
//					throw new RuntimeException();
//				}
//			}
//		}
//		
//		// 3 agenten, agent 2 20 sec nach agent 1, agent 2 zusätzlich um 100 sec verzögert, agent 3 vor agent 1 und 2 auf link 4, link 4 ist nicht voll
//		@Test
//		public final void testStorageCongestion5(){
//			
//			loadScenario();
//			setPopulation();
//			setLinks_storageCapacityConstraints_link3();
//			
//			final List<MarginalCongestionEvent> congestionEvents = new ArrayList<MarginalCongestionEvent>();
//								
//			events.addHandler( new MarginalCongestionEventHandler() {
//
//				@Override
//				public void reset(int iteration) {				
//				}
//
//				@Override
//				public void handleEvent(MarginalCongestionEvent event) {
//					congestionEvents.add(event);
//				}
//				
//			});
//							
//			MarginalCongestionHandler congestionHandler = new MarginalCongestionHandler(this.events, this.scenario);
//			
//			// agent 3 fährt vor agent 2 und 1 auf link 4 und bleibt dort die ganze Zeit über
//			// storage capacity auf link 4 nicht erreicht
//			congestionHandler.handleEvent(ef.createLinkEnterEvent(0, testAgent3, linkId4, testAgent3));
//			
//			// starte agent 1
//			congestionHandler.handleEvent(ef.createAgentDepartureEvent(100, testAgent1, linkId1, "car"));
//			congestionHandler.handleEvent(ef.createLinkLeaveEvent(101, testAgent1, linkId1, testAgent1));
//			congestionHandler.handleEvent(ef.createLinkEnterEvent(101, testAgent1, linkId2, testAgent1));
//						
//			// starte agent 2 mit 20 sec Verzögerung
//			congestionHandler.handleEvent(ef.createAgentDepartureEvent(120, testAgent2, linkId1, "car"));
//			congestionHandler.handleEvent(ef.createLinkLeaveEvent(121, testAgent2, linkId1, testAgent2));
//			congestionHandler.handleEvent(ef.createLinkEnterEvent(121, testAgent2, linkId2, testAgent2));
//			
//			// agent 1 kann link 2 problemlos verlassen
//			congestionHandler.handleEvent(ef.createLinkLeaveEvent(151, testAgent1, linkId2, testAgent1));
//			congestionHandler.handleEvent(ef.createLinkEnterEvent(151, testAgent1, linkId3, testAgent1));
//			
//			// agent 2 muss durch die flowCapacity 40 sec warten und wird dann nochmal zusätzlich um 100 sec verzögert
//			// freeFlowLeaveTime + flowCapacityDelay + storageCapacityDelay + 1sec = enterTime + freeTravelTime + flowCapacityDelay + storageCapacityDelay + 1sec
//			congestionHandler.handleEvent(ef.createLinkLeaveEvent(121 + 50 + 40 + 100 + 1, testAgent2, linkId2, testAgent2));
//			congestionHandler.handleEvent(ef.createLinkEnterEvent(121 + 50 + 40 + 100 + 1, testAgent2, linkId3, testAgent2));
//
//			// *****************
//			
//			Assert.assertEquals("number of congestion events", 1, congestionEvents.size());
//
//			for (MarginalCongestionEvent congEvent : congestionEvents) {
//				
//				if (congEvent.getCausingAgentId().toString().equals(testAgent1.toString())){
//					Assert.assertEquals("capacity constraint", "storageCapacity", congEvent.getCapacityConstraint());
//					Assert.assertEquals("external delay", 100, congEvent.getDelay(), MatsimTestUtils.EPSILON);
//					Assert.assertEquals("congested link", linkId2.toString(), congEvent.getLinkId().toString());
//					Assert.assertEquals("affected Agent", testAgent2.toString(), congEvent.getAffectedAgentId().toString());
//				
//				} else {
//					throw new RuntimeException();
//				}
//			}
//		}
//		
//		@Test
//		public final void testStorageCongestion6(){
//			
//			loadScenario();
//			setPopulation();
//			setLinks_storageCapacityConstraints_link3_link4();
//			
//			final List<MarginalCongestionEvent> congestionEvents = new ArrayList<MarginalCongestionEvent>();
//								
//			events.addHandler( new MarginalCongestionEventHandler() {
//
//				@Override
//				public void reset(int iteration) {				
//				}
//
//				@Override
//				public void handleEvent(MarginalCongestionEvent event) {
//					congestionEvents.add(event);
//				}
//				
//			});
//							
//			MarginalCongestionHandler congestionHandler = new MarginalCongestionHandler(this.events, this.scenario);
//			
//			// agent 2 fährt in der 51. sec auf link 2, leave time = 102 
//			congestionHandler.handleEvent(ef.createLinkEnterEvent(51, testAgent2, linkId2, testAgent2));
//			
//			// agent 1 fährt auf link 3 und blockiert diesen link
//			congestionHandler.handleEvent(ef.createLinkEnterEvent(101, testAgent1, linkId3, testAgent1));
//			// agent 1 verlässt link 3 und gibt den blockierten link wieder frei
//			congestionHandler.handleEvent(ef.createLinkLeaveEvent(103, testAgent1, linkId3, testAgent1));
//			
//			// da agent 1 link 3 verlassen hat, kann agent 2 auf link 3 fahren, statt zur 102. sec zur 104. --> 2 sec Verspätung
//			congestionHandler.handleEvent(ef.createLinkLeaveEvent(104, testAgent2, linkId2, testAgent2));
//			congestionHandler.handleEvent(ef.createLinkEnterEvent(104, testAgent2, linkId3, testAgent2));
//
//			// *****************
//			
//			Assert.assertEquals("number of congestion events", 1, congestionEvents.size());
//
//			for (MarginalCongestionEvent congEvent : congestionEvents) {
//				
//				if (congEvent.getCausingAgentId().toString().equals(testAgent1.toString())){
//					Assert.assertEquals("capacity constraint", "storageCapacity", congEvent.getCapacityConstraint());
//					Assert.assertEquals("external delay", 2., congEvent.getDelay(), MatsimTestUtils.EPSILON);
//					Assert.assertEquals("congested link", linkId2.toString(), congEvent.getLinkId().toString());
//					Assert.assertEquals("affected Agent", testAgent2.toString(), congEvent.getAffectedAgentId().toString());
//				
//				} else {
//					throw new RuntimeException();
//				}
//			}
//		}
//
//		@Test
//		public final void testStorageCongestion7(){
//			
//			loadScenario();
//			setPopulation();
//			setLinks_storageCapacityConstraints_link3_link4();
//			
//			final List<MarginalCongestionEvent> congestionEvents = new ArrayList<MarginalCongestionEvent>();
//								
//			events.addHandler( new MarginalCongestionEventHandler() {
//
//				@Override
//				public void reset(int iteration) {				
//				}
//
//				@Override
//				public void handleEvent(MarginalCongestionEvent event) {
//					congestionEvents.add(event);
//				}
//				
//			});
//							
//			MarginalCongestionHandler congestionHandler = new MarginalCongestionHandler(this.events, this.scenario);
//			
//			// agent 2 fährt in der 51. sec auf link 2, leave time = 102 
//			congestionHandler.handleEvent(ef.createLinkEnterEvent(51, testAgent2, linkId2, testAgent2));
//			
//			// agent 1 fährt auf link 3 und blockiert diesen link
//			congestionHandler.handleEvent(ef.createLinkEnterEvent(101, testAgent1, linkId3, testAgent1));
//			// agent 1 beginnt auf link 3 mit einer Aktivität, der blockierte link wird wieder frei
//			congestionHandler.handleEvent(ef.createAgentArrivalEvent(103, testAgent1, linkId3, "car"));
//			
//			// da agent 1 link 3 verlassen hat, kann agent 2 auf link 3 fahren, statt zur 102. sec zur 104. --> 2 sec Verspätung
//			congestionHandler.handleEvent(ef.createLinkLeaveEvent(104, testAgent2, linkId2, testAgent2));
//			congestionHandler.handleEvent(ef.createLinkEnterEvent(104, testAgent2, linkId3, testAgent2));
//
//			// *****************
//			
//			Assert.assertEquals("number of congestion events", 1, congestionEvents.size());
//
//			for (MarginalCongestionEvent congEvent : congestionEvents) {
//				
//				if (congEvent.getCausingAgentId().toString().equals(testAgent1.toString())){
//					Assert.assertEquals("capacity constraint", "storageCapacity", congEvent.getCapacityConstraint());
//					Assert.assertEquals("external delay", 2., congEvent.getDelay(), MatsimTestUtils.EPSILON);
//					Assert.assertEquals("congested link", linkId2.toString(), congEvent.getLinkId().toString());
//					Assert.assertEquals("affected Agent", testAgent2.toString(), congEvent.getAffectedAgentId().toString());
//				
//				} else {
//					throw new RuntimeException();
//				}
//			}
//		}
//	
//	
//	// *************************************************************************************************************************
//
//		private void setPopulation() {
//			PopulationFactoryImpl popFactory = new PopulationFactoryImpl(scenario);
//		
//			Person person1 = popFactory.createPerson(testAgent1);
//			Plan plan1 = popFactory.createPlan();
//
//			Person person2 = popFactory.createPerson(testAgent2);
//			Plan plan2 = popFactory.createPlan();
//			
//			Person person3 = popFactory.createPerson(testAgent3);
//			Plan plan3 = popFactory.createPlan();			
//			
//			Activity act = popFactory.createActivityFromLinkId("home", linkId1);
//			act.setEndTime(0);
//			Leg leg = popFactory.createLeg("car");
//			LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();
//			List<Id> linkIds = new ArrayList<Id>();
//			linkIds.add(linkId2);
//			linkIds.add(linkId3);
//			NetworkRoute route = (NetworkRoute) routeFactory.createRoute(linkId1, linkId4);
//			route.setLinkIds(linkId1, linkIds, linkId4);
//			leg.setRoute(route);
//			
//			plan1.addActivity(act);
//			plan1.addLeg(leg);
//			plan2.addActivity(act);
//			plan2.addLeg(leg);
//			plan3.addActivity(act);
//			plan3.addLeg(leg);
//	
//			person1.addPlan(plan1);
//			person2.addPlan(plan2);
//			person3.addPlan(plan3);
//			
//			Population population = scenario.getPopulation();
//			population.addPerson(person1);
//			population.addPerson(person2);
//			population.addPerson(person3);
//		}
//
//	private void loadScenario() {
//	
//		this.scenario = null;
//		this.events = null;
//		log.info("************** Loading scenario **************");
//		
//		// (0)-----link1-----(1)-----link2-----(2)-----link3-----(3)-----link4-----(4)
//		
//		Config config = ConfigUtils.createConfig();;
//		this.scenario = (ScenarioImpl)(ScenarioUtils.createScenario(config));
//		
//		Network network = scenario.getNetwork();
//		
//		Node node0 = network.getFactory().createNode(new IdImpl("0"), scenario.createCoord(0., 0.));
//		Node node1 = network.getFactory().createNode(new IdImpl("1"), scenario.createCoord(100., 0.));
//		Node node2 = network.getFactory().createNode(new IdImpl("2"), scenario.createCoord(200., 0.));
//		Node node3 = network.getFactory().createNode(new IdImpl("3"), scenario.createCoord(300., 0.));
//		Node node4 = network.getFactory().createNode(new IdImpl("4"), scenario.createCoord(400., 0.));
//		
//		Link link1 = network.getFactory().createLink(this.linkId1, node0, node1);
//		Link link2 = network.getFactory().createLink(this.linkId2, node1, node2);
//		Link link3 = network.getFactory().createLink(this.linkId3, node2, node3);
//		Link link4 = network.getFactory().createLink(this.linkId4, node3, node4);
//
//		network.addNode(node0);
//		network.addNode(node1);
//		network.addNode(node2);
//		network.addNode(node3);
//		network.addNode(node4);
//
//		network.addLink(link1);
//		network.addLink(link2);
//		network.addLink(link3);
//		network.addLink(link4);
//
//		this.events = EventsUtils.createEventsManager();
//	}
//
//	private void setLinks_noStorageCapacityConstraints(){
//		Set<String> modes = new HashSet<String>();
//		modes.add("car");
//		
//		Link link1 = this.scenario.getNetwork().getLinks().get(linkId1);
//		link1.setAllowedModes(modes);
//		link1.setCapacity(7200); // 7200 --> 2 cars/sec 
//		link1.setFreespeed(10); // 10 --> 36 km/h // 50 sec pro 500 m
//		link1.setNumberOfLanes(1);
//		link1.setLength(500);
//		
//		Link link2 = this.scenario.getNetwork().getLinks().get(linkId2);
//		link2.setAllowedModes(modes);
//		link2.setCapacity(60); // 60 --> 1 car/min // 3600 --> 1 car/sec 
//		link2.setFreespeed(10); // 10 --> 36 km/h // 50 sec pro 500 m
//		link2.setNumberOfLanes(1);
//		link2.setLength(500);
//		
//		Link link3 = this.scenario.getNetwork().getLinks().get(linkId3);
//		link3.setAllowedModes(modes);
//		link3.setCapacity(7200); // 7200 --> 2 cars/sec 
//		link3.setFreespeed(10); // 10 --> 36 km/h // 50 sec pro 500 m
//		link3.setNumberOfLanes(1);
//		link3.setLength(500);
//		
//		Link link4 = this.scenario.getNetwork().getLinks().get(linkId4);
//		link4.setAllowedModes(modes);
//		link4.setCapacity(7200); // 7200 --> 2 cars/sec 
//		link4.setFreespeed(10); // 10 --> 36 km/h // 50 sec pro 500 m
//		link4.setNumberOfLanes(1);
//		link4.setLength(500);
//	}
//	
//	private void setLinks_storageCapacityConstraints_link3_link4() {
//		Set<String> modes = new HashSet<String>();
//		modes.add("car");
//		
//		Link link1 = this.scenario.getNetwork().getLinks().get(linkId1);
//		link1.setAllowedModes(modes);
//		link1.setCapacity(7200); // 7200 --> 2 cars/sec 
//		link1.setFreespeed(10); // 10 --> 36 km/h // 50 sec pro 500 m
//		link1.setNumberOfLanes(1);
//		link1.setLength(500);
//		
//		Link link2 = this.scenario.getNetwork().getLinks().get(linkId2);
//		link2.setAllowedModes(modes);
//		link2.setCapacity(60); // 60 --> 1 car/min // 3600 --> 1 car/sec 
//		link2.setFreespeed(10); // 10 --> 36 km/h // 50 sec pro 500 m
//		link2.setNumberOfLanes(1);
//		link2.setLength(500);
//		
//		Link link3 = this.scenario.getNetwork().getLinks().get(linkId3);
//		link3.setAllowedModes(modes);
//		link3.setCapacity(7200); // 7200 --> 2 cars/sec 
//		link3.setFreespeed(10);
//		link3.setNumberOfLanes(1);
//		link3.setLength(10);
//		
//		Link link4 = this.scenario.getNetwork().getLinks().get(linkId4);
//		link4.setAllowedModes(modes);
//		link4.setCapacity(7200); // 7200 --> 2 cars/sec 
//		link4.setFreespeed(10); // 10 --> 36 km/h // 50 sec pro 500 m
//		link4.setNumberOfLanes(1);
//		link4.setLength(10);
//	}
//	
//	private void setLinks_storageCapacityConstraints_link3() {
//		Set<String> modes = new HashSet<String>();
//		modes.add("car");
//		
//		Link link1 = this.scenario.getNetwork().getLinks().get(linkId1);
//		link1.setAllowedModes(modes);
//		link1.setCapacity(7200); // 7200 --> 2 cars/sec 
//		link1.setFreespeed(10); // 10 --> 36 km/h // 50 sec pro 500 m
//		link1.setNumberOfLanes(1);
//		link1.setLength(500);
//		
//		Link link2 = this.scenario.getNetwork().getLinks().get(linkId2);
//		link2.setAllowedModes(modes);
//		link2.setCapacity(60); // 60 --> 1 car/min // 3600 --> 1 car/sec 
//		link2.setFreespeed(10); // 10 --> 36 km/h // 50 sec pro 500 m
//		link2.setNumberOfLanes(1);
//		link2.setLength(500);
//		
//		Link link3 = this.scenario.getNetwork().getLinks().get(linkId3);
//		link3.setAllowedModes(modes);
//		link3.setCapacity(7200); // 7200 --> 2 cars/sec 
//		link3.setFreespeed(10);
//		link3.setNumberOfLanes(1);
//		link3.setLength(7.5);
//		
//		Link link4 = this.scenario.getNetwork().getLinks().get(linkId4);
//		link4.setAllowedModes(modes);
//		link4.setCapacity(7200); // 7200 --> 2 cars/sec 
//		link4.setFreespeed(10); // 10 --> 36 km/h // 50 sec pro 500 m
//		link4.setNumberOfLanes(1);
//		link4.setLength(500);	
//	}
//
//	
//}
