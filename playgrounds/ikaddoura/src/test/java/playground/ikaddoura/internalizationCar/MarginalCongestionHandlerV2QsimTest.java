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
package playground.ikaddoura.internalizationCar;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.model.FrameworkMethod;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineModule;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author ikaddoura
 *
 */

public class MarginalCongestionHandlerV2QsimTest {
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();
	
	private EventsManager events;
	
	private Id testAgent1 = new IdImpl("testAgent1");
	private Id testAgent2 = new IdImpl("testAgent2");
	private Id testAgent3 = new IdImpl("testAgent3");
	private Id testAgent4 = new IdImpl("testAgent4");
	private Id testAgent5 = new IdImpl("testAgent5");
	private Id testAgent6 = new IdImpl("testAgent6");
	private Id testAgent7 = new IdImpl("testAgent7");
	
	private Id linkId1 = new IdImpl("link1");
	private Id linkId2 = new IdImpl("link2");
	private Id linkId3 = new IdImpl("link3");
	private Id linkId4 = new IdImpl("link4");
	private Id linkId5 = new IdImpl("link5");
	private Id linkId6 = new IdImpl("link6");
	private Id linkId7 = new IdImpl("link7");
	private Id linkId8 = new IdImpl("link8");
	
	//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	
	// 3 agents start at the same time on the first link
	// on the second link the second and the third agent are delayed due to flow congestion constraints (1 car / 10 seconds)
	// the third agent is affected by both other agents (he has to wait "two waiting-intervals")		
	@Test
	public final void testFlowCongestion_3agents_sameTime(){
		
		testUtils.starting(new FrameworkMethod(MarginalCongestionHandlerV2QsimTest.class.getMethods()[0]));
		Scenario sc = loadScenario2();
		setPopulation01(sc);
		
		final List<MarginalCongestionEvent> congestionEvents = new ArrayList<MarginalCongestionEvent>();
		
		events.addHandler( new MarginalCongestionEventHandler() {				
			@Override
			public void reset(int iteration) {				
			}
			@Override
			public void handleEvent(MarginalCongestionEvent event) {
				congestionEvents.add(event);
			}	
		});
			
		events.addHandler(new MarginalCongestionHandlerImplV2(events, (ScenarioImpl) sc));
					
		QSim sim = createQSim(sc, events);
		sim.run();
			
		Assert.assertEquals("wrong number of congestion events" , 3, congestionEvents.size());
			
		for (MarginalCongestionEvent event : congestionEvents) {
							
			if (event.getCausingAgentId().toString().equals(this.testAgent3.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent2.toString())) {
				Assert.assertEquals("wrong delay.", 10.0, event.getDelay(), MatsimTestUtils.EPSILON);
				
			} else if (event.getCausingAgentId().toString().equals(this.testAgent2.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent1.toString())) {
				Assert.assertEquals("wrong delay.", 10.0, event.getDelay(), MatsimTestUtils.EPSILON);
				
			} else if (event.getCausingAgentId().toString().equals(this.testAgent3.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent1.toString())) {
				Assert.assertEquals("wrong delay.", 10.0, event.getDelay(), MatsimTestUtils.EPSILON);
			}
		}
			
	}
	
	// two agents start on link 1 with a gap of almost the flow congestion constraint (1 car / 10 seconds) on link 2.
	// the second agent is delayed by just one second,
	// a third agent follows, is not delayed and should not be considered for the calculations
	@Test
	public final void testFlowCongestion_3agents_differentTimes(){
			
		testUtils.starting(new FrameworkMethod(MarginalCongestionHandlerV2QsimTest.class.getMethods()[0]));
		Scenario sc = loadScenario2();
		setPopulation02(sc);
			
		final List<MarginalCongestionEvent> congestionEvents = new ArrayList<MarginalCongestionEvent>();
			
		events.addHandler( new MarginalCongestionEventHandler() {

			@Override
			public void reset(int iteration) {				
			}

			@Override
			public void handleEvent(MarginalCongestionEvent event) {
				congestionEvents.add(event);
			}
				
		});
			
		events.addHandler(new MarginalCongestionHandlerImplV2(events, (ScenarioImpl) sc));
					
		QSim sim = createQSim(sc, events);
		sim.run();
					
		for (MarginalCongestionEvent event : congestionEvents) {
			if (event.getCausingAgentId().toString().equals(this.testAgent1.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent2.toString())) {
				Assert.assertEquals("wrong delay.", 1.0, event.getDelay(), MatsimTestUtils.EPSILON);
			}
		}
		Assert.assertEquals("wrong number of congestion events" , 1, congestionEvents.size());
			
	}
	
	// the flow capacity on link 3 (1car / 60 seconds) is activated by the first agent,
	// then the storage capacity on link 3 (only one car) is reached, too
	// finally, two cars on the link before are delayed, the second one additional because of the flow capacity on this link.
	@Ignore
	@Test
	public final void testFlowAndStorageCongestion_4agents(){
		
		Scenario sc = loadScenario1();
		setPopulation03(sc);
		
		final List<MarginalCongestionEvent> congestionEvents = new ArrayList<MarginalCongestionEvent>();
		
		events.addHandler( new MarginalCongestionEventHandler() {

			@Override
			public void reset(int iteration) {				
			}

			@Override
			public void handleEvent(MarginalCongestionEvent event) {
				congestionEvents.add(event);
			}	
		});
		
		events.addHandler(new MarginalCongestionHandlerImplV2(events, (ScenarioImpl) sc));
				
		QSim sim = createQSim(sc, events);
		sim.run();
		
		double delayEffectSum = 0;
		
		System.out.println(events.toString());
		
		for (MarginalCongestionEvent event : congestionEvents) {
		
			System.out.println(events.toString());
			
			if (event.getTime() == 160.0) {
				
				if (event.getCausingAgentId().toString().equals(this.testAgent2.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent3.toString())) {
					Assert.assertEquals("wrong delay.", 9.0, event.getDelay(), MatsimTestUtils.EPSILON);
				} else if (event.getCausingAgentId().toString().equals(this.testAgent1.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent2.toString())) {
					Assert.assertEquals("wrong delay.", 59.0, event.getDelay(), MatsimTestUtils.EPSILON);
				} 
				
			} else if (event.getTime() == 170.0) {
				
				if (event.getCausingAgentId().toString().equals(this.testAgent3.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent4.toString())) {
					delayEffectSum = delayEffectSum + event.getDelay();
					// the delay of 18 seconds results from storage and flow capacity constraints
				} else {
					Assert.fail("Unexpected causing agent or affected agent.");
				}
			}
		}
		Assert.assertEquals("wrong delay sum.", 18.0, delayEffectSum, MatsimTestUtils.EPSILON);
	}
	
	// agent 1 stops on link 3 and starts an activity
	// agent 2 is delayed due to flow constraints on link 2
	// the MarginalCongestionEvent is thrown, while agent 1 has already started the activity
	@Test
	public final void testFlowCongestion_11(){
		
		testUtils.starting(new FrameworkMethod(MarginalCongestionHandlerV2QsimTest.class.getMethods()[0]));
		Scenario sc = loadScenario1();
		setPopulation11(sc);
		
		final List<MarginalCongestionEvent> congestionEvents = new ArrayList<MarginalCongestionEvent>();
		
		events.addHandler( new MarginalCongestionEventHandler() {

			@Override
			public void reset(int iteration) {				
			}

			@Override
			public void handleEvent(MarginalCongestionEvent event) {
				congestionEvents.add(event);
				System.out.println(event.toString());
			}
			
		});
		
		events.addHandler(new MarginalCongestionHandlerImplV2(events, (ScenarioImpl) sc));
				
		QSim sim = createQSim(sc, events);
		sim.run();
		
		Assert.assertEquals("wrong number of congestion events" , 1, congestionEvents.size());
		
		for (MarginalCongestionEvent event : congestionEvents) {
			if (event.getCausingAgentId().toString().equals(this.testAgent1.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent2.toString())) {
				Assert.assertEquals("wrong delay.", 6.0, event.getDelay(), MatsimTestUtils.EPSILON);
			}
		}
	
	}
	
	// agent 2 finishes an activity on link 3 just before agent 1 passes this link
	// later on agent 4 finishes an activity on link 3 just after agent 3 passes the link
	// in both cases an agent should be delayed due to flow capacity constraints (1 car / 60 seconds)
	@Test
	public final void testFlowCongestion_activityEnding(){
		
		testUtils.starting(new FrameworkMethod(MarginalCongestionHandlerV2QsimTest.class.getMethods()[0]));
		Scenario sc = loadScenario1();
		setPopulation12(sc);
		
		final List<MarginalCongestionEvent> congestionEvents = new ArrayList<MarginalCongestionEvent>();
		
		events.addHandler( new MarginalCongestionEventHandler() {

			@Override
			public void reset(int iteration) {				
			}

			@Override
			public void handleEvent(MarginalCongestionEvent event) {
				congestionEvents.add(event);
			}
			
		});
		
		events.addHandler(new MarginalCongestionHandlerImplV2(events, (ScenarioImpl) sc));
				
		QSim sim = createQSim(sc, events);
		sim.run();
		
		System.out.println("test_flowCongestion12: "+events.toString());
		
		for (MarginalCongestionEvent event : congestionEvents) {
		
			System.out.println("test_flowCongestion12: "+event.toString());
		
			if (event.getCausingAgentId().toString().equals(this.testAgent2.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent1.toString())) {
				Assert.assertEquals("wrong delay.", 50.0, event.getDelay(), MatsimTestUtils.EPSILON);
			}else if (event.getCausingAgentId().toString().equals(this.testAgent3.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent4.toString())) {
				Assert.assertEquals("wrong delay.", 30.0, event.getDelay(), MatsimTestUtils.EPSILON);
			}
		}
		
		Assert.assertEquals("wrong number of congestion events" , 2, congestionEvents.size());
	
	}

	// link 3 is blocked at first due to flow capacity constraints, and then due to storage capacity constraints
	// both effects should be considered,
	// later on an agent passes this link only delayed due to flow capacity constraints
	@Test
	public final void testStorageCongestion_13(){
		
		testUtils.starting(new FrameworkMethod(MarginalCongestionHandlerV2QsimTest.class.getMethods()[0]));
		Scenario sc = loadScenario1();
		setPopulation13(sc);
		
		final List<MarginalCongestionEvent> congestionEvents = new ArrayList<MarginalCongestionEvent>();
		
		events.addHandler( new MarginalCongestionEventHandler() {

			@Override
			public void reset(int iteration) {				
			}

			@Override
			public void handleEvent(MarginalCongestionEvent event) {
				congestionEvents.add(event);
			}
			
		});

		events.addHandler(new MarginalCongestionHandlerImplV2(events, (ScenarioImpl) sc));
				
		QSim sim = createQSim(sc, events);
		sim.run();
		
		Assert.assertEquals("wrong number of congestion events" , 7, congestionEvents.size());
		
		for (MarginalCongestionEvent event : congestionEvents) {
			if (event.getCausingAgentId().toString().equals(this.testAgent1.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent2.toString())) {
				Assert.assertEquals("wrong delay.", 8.0, event.getDelay(), MatsimTestUtils.EPSILON);
			} else if (event.getCausingAgentId().toString().equals(this.testAgent2.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent3.toString())) {
					Assert.assertEquals("wrong delay.", 35.0, event.getDelay(), MatsimTestUtils.EPSILON);
			} else if (event.getCapacityConstraint().toString().equals("storageCapacity") && event.getCausingAgentId().toString().equals(this.testAgent3.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent4.toString())) {
					Assert.assertEquals("wrong delay.", 3.0, event.getDelay(), MatsimTestUtils.EPSILON);
			} else if (event.getCapacityConstraint().toString().equals("flowCapacity") && event.getCausingAgentId().toString().equals(this.testAgent3.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent4.toString())) {
					Assert.assertEquals("wrong delay.", 59.0, event.getDelay(), MatsimTestUtils.EPSILON);
			} else if (event.getCapacityConstraint().toString().equals("storageCapacity") && event.getCausingAgentId().toString().equals(this.testAgent4.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent5.toString())) {
					Assert.assertEquals("wrong delay.", 40.0, event.getDelay(), MatsimTestUtils.EPSILON);
			} else if (event.getCapacityConstraint().toString().equals("flowCapacity") && event.getCausingAgentId().toString().equals(this.testAgent4.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent5.toString())) {
					Assert.assertEquals("wrong delay.", 59.0, event.getDelay(), MatsimTestUtils.EPSILON);
			} else if (event.getCausingAgentId().toString().equals(this.testAgent5.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent6.toString())) {
					Assert.assertEquals("wrong delay.", 28.0, event.getDelay(), MatsimTestUtils.EPSILON);		
			}
		}
	
	}

	// testing the clearing of the lists
	// distribution of the time on flow and storage capacity constraints
	// six agents pass the bottleneck link 3 with a flow capacity of 1 car / 60 sec and a storage capacity of 1 car
	// the flow capacity constraints on link 2 (1 car / 10 sec) should be irrelevant and taken into account by the storage capacity constraints of link 3
	// otherwise there would be thrown 12 MarginalCongestionEvents
	@Test
	public final void testClearing(){
				
		testUtils.starting(new FrameworkMethod(MarginalCongestionHandlerV2QsimTest.class.getMethods()[0]));
		Scenario sc = loadScenario3();
		setPopulation14(sc);
				
		final List<MarginalCongestionEvent> congestionEvents = new ArrayList<MarginalCongestionEvent>();
			
		events.addHandler( new MarginalCongestionEventHandler() {

			@Override
			public void reset(int iteration) {				
			}

			@Override
			public void handleEvent(MarginalCongestionEvent event) {
				congestionEvents.add(event);
//				System.out.println(event.toString());
			}	
		});
				
		events.addHandler(new MarginalCongestionHandlerImplV2(events, (ScenarioImpl) sc));
						
		QSim sim = createQSim(sc, events);
		sim.run();
			
//		System.out.println("test_Clearing: "+events.toString());
						
		for (MarginalCongestionEvent event : congestionEvents) {
				
			if (event.getTime() == 212.0) {
				if (event.getCausingAgentId().toString().equals(this.testAgent1.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent2.toString())) {
						Assert.assertEquals("wrong delay.", 40.0, event.getDelay(), MatsimTestUtils.EPSILON);
				} else if (event.getCausingAgentId().toString().equals(this.testAgent2.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent3.toString())) {
						Assert.assertEquals("wrong delay.", 36.0, event.getDelay(), MatsimTestUtils.EPSILON);
				}
			}
			else if (event.getTime() == 272.0) {
				if (event.getCausingAgentId().toString().equals(this.testAgent2.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent3.toString())) {
						Assert.assertEquals("wrong delay.", 59.0, event.getDelay(), MatsimTestUtils.EPSILON);
				} else if (event.getCausingAgentId().toString().equals(this.testAgent3.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent4.toString())) {
						Assert.assertEquals("wrong delay.", 11.0, event.getDelay(), MatsimTestUtils.EPSILON);
				}
			}
			else if (event.getTime() == 332.0) {
				if (event.getCausingAgentId().toString().equals(this.testAgent3.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent4.toString())) {
						Assert.assertEquals("wrong delay.", 59.0, event.getDelay(), MatsimTestUtils.EPSILON);
				} else if (event.getCausingAgentId().toString().equals(this.testAgent4.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent5.toString())) {
							Assert.assertEquals("wrong delay.", 70.0, event.getDelay(), MatsimTestUtils.EPSILON);
				}
			}
			else if (event.getTime() == 392.0) {
				if (event.getCausingAgentId().toString().equals(this.testAgent4.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent5.toString())) {
					Assert.assertEquals("wrong delay.", 59.0, event.getDelay(), MatsimTestUtils.EPSILON);
				} else if (event.getCausingAgentId().toString().equals(this.testAgent5.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent6.toString())) {
						Assert.assertEquals("wrong delay.", 129.0, event.getDelay(), MatsimTestUtils.EPSILON);
				}
			}
			else if (event.getTime() == 452.0) {
				if (event.getCausingAgentId().toString().equals(this.testAgent5.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent6.toString())) {
						Assert.assertEquals("wrong delay.", 59.0, event.getDelay(), MatsimTestUtils.EPSILON);
				} 
			}
			
		Assert.assertEquals("wrong number of congestion events" , 9, congestionEvents.size());
		}	
	}
			
		// the storage capacity of 3 cars on link 3 is reached,
		// the fifth agent has to wait at first 20 seconds due to storage capacity constraints and then 240 seconds due to flow capacity constraints,
		// later on, the sixth agent is not delayed anymore
		@Test
		public final void testClearing2(){
				
			testUtils.starting(new FrameworkMethod(MarginalCongestionHandlerV2QsimTest.class.getMethods()[0]));
			Scenario sc = loadScenario4();
			setPopulation15(sc);
				
			final List<MarginalCongestionEvent> congestionEvents = new ArrayList<MarginalCongestionEvent>();
			
			events.addHandler( new MarginalCongestionEventHandler() {

				@Override
				public void reset(int iteration) {				
				}

				@Override
				public void handleEvent(MarginalCongestionEvent event) {
					congestionEvents.add(event);
				}
					
			});
				
			events.addHandler(new MarginalCongestionHandlerImplV2(events, (ScenarioImpl) sc));
						
			QSim sim = createQSim(sc, events);
			sim.run();
			
			System.out.println("test_Clearing2: "+events.toString());
						
			for (MarginalCongestionEvent event : congestionEvents) {
				
				System.out.println("test_Clearing2: "+event.toString());
				
			
			
				if (event.getTime() == 233.0) {
					if (event.getCausingAgentId().toString().equals(this.testAgent1.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent2.toString())) {
						Assert.assertEquals("wrong delay.", 66.0, event.getDelay(), MatsimTestUtils.EPSILON);
					} else if (event.getCausingAgentId().toString().equals(this.testAgent4.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent5.toString())) {
						Assert.assertEquals("wrong delay.", 22.0, event.getDelay(), MatsimTestUtils.EPSILON);
					}
				}
				else if (event.getTime() == 313.0) {
					if (event.getCausingAgentId().toString().equals(this.testAgent2.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent3.toString())) {
						Assert.assertEquals("wrong delay.", 80.0, event.getDelay(), MatsimTestUtils.EPSILON);
					} else if (event.getCausingAgentId().toString().equals(this.testAgent1.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent3.toString())) {
						Assert.assertEquals("wrong delay.", 51.0, event.getDelay(), MatsimTestUtils.EPSILON);
					}
				}
				else if (event.getTime() == 393.0) {
					if (event.getCausingAgentId().toString().equals(this.testAgent3.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent4.toString())) {
						Assert.assertEquals("wrong delay.", 80.0, event.getDelay(), MatsimTestUtils.EPSILON);
					} else if (event.getCausingAgentId().toString().equals(this.testAgent2.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent4.toString())) {
						Assert.assertEquals("wrong delay.", 80.0, event.getDelay(), MatsimTestUtils.EPSILON);
					} else if (event.getCausingAgentId().toString().equals(this.testAgent1.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent4.toString())) {
						Assert.assertEquals("wrong delay.", 36.0, event.getDelay(), MatsimTestUtils.EPSILON);
					}
				}
				else if (event.getTime() == 473.0) {
					if (event.getCausingAgentId().toString().equals(this.testAgent4.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent5.toString())) {
						Assert.assertEquals("wrong delay.", 80.0, event.getDelay(), MatsimTestUtils.EPSILON);
					} else if (event.getCausingAgentId().toString().equals(this.testAgent3.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent5.toString())) {
						Assert.assertEquals("wrong delay.", 80.0, event.getDelay(), MatsimTestUtils.EPSILON);
					} else if (event.getCausingAgentId().toString().equals(this.testAgent2.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent5.toString())) {
						Assert.assertEquals("wrong delay.", 79.0, event.getDelay(), MatsimTestUtils.EPSILON);
					}
				}
				
				Assert.assertEquals("wrong number of congestion events" , 10, congestionEvents.size());
				
			}
				
		}
	
		// the storage capacity on link 3 (3 cars) is reached, 3 agents end an activity on the link while another agent (agent 5) is already in the buffer (flow capacity 1 car / 80 seconds!)
		// agent 5 should leave the link as the last, he is delayed by all other agents
		@Test
		public final void testStorageCapacity_reached(){
				
			testUtils.starting(new FrameworkMethod(MarginalCongestionHandlerV2QsimTest.class.getMethods()[0]));
			Scenario sc = loadScenario4();
			setPopulation17(sc);
				
			final List<MarginalCongestionEvent> congestionEvents = new ArrayList<MarginalCongestionEvent>();
			
			events.addHandler( new MarginalCongestionEventHandler() {

				@Override
				public void reset(int iteration) {				
				}

				@Override
				public void handleEvent(MarginalCongestionEvent event) {
					congestionEvents.add(event);
				}
					
			});

			events.addHandler(new MarginalCongestionHandlerImplV2(events, (ScenarioImpl) sc));
						
			QSim sim = createQSim(sc, events);
			sim.run();
						
			for (MarginalCongestionEvent event : congestionEvents) {
				
				if (event.getTime() == 233.0) {
					if (event.getCausingAgentId().toString().equals(this.testAgent1.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent2.toString())) {
						Assert.assertEquals("wrong delay.", 70.0, event.getDelay(), MatsimTestUtils.EPSILON);
					} else if (event.getCausingAgentId().toString().equals(this.testAgent4.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent5.toString())) {
						Assert.assertEquals("wrong delay.", 33.0, event.getDelay(), MatsimTestUtils.EPSILON);
					}
				}
				
				else if (event.getTime() == 313.0) {
					if (event.getCausingAgentId().toString().equals(this.testAgent2.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent3.toString())) {
						Assert.assertEquals("wrong delay.", 80.0, event.getDelay(), MatsimTestUtils.EPSILON);
					} else if (event.getCausingAgentId().toString().equals(this.testAgent1.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent3.toString())) {
						Assert.assertEquals("wrong delay.", 59.0, event.getDelay(), MatsimTestUtils.EPSILON);
					}
				}
				
				else if (event.getTime() == 393.0) {
					if (event.getCausingAgentId().toString().equals(this.testAgent3.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent4.toString())) {
						Assert.assertEquals("wrong delay.", 80.0, event.getDelay(), MatsimTestUtils.EPSILON);
					} else if (event.getCausingAgentId().toString().equals(this.testAgent2.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent4.toString())) {
						Assert.assertEquals("wrong delay.", 80.0, event.getDelay(), MatsimTestUtils.EPSILON);
					} else if (event.getCausingAgentId().toString().equals(this.testAgent1.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent4.toString())) {
						Assert.assertEquals("wrong delay.", 48.0, event.getDelay(), MatsimTestUtils.EPSILON);
					}
				}
				else if (event.getTime() == 473.0) {
					if (event.getCausingAgentId().toString().equals(this.testAgent4.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent5.toString())) {
						Assert.assertEquals("wrong delay.", 80.0, event.getDelay(), MatsimTestUtils.EPSILON);
					} else if (event.getCausingAgentId().toString().equals(this.testAgent3.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent5.toString())) {
						Assert.assertEquals("wrong delay.", 80.0, event.getDelay(), MatsimTestUtils.EPSILON);
					} else if (event.getCausingAgentId().toString().equals(this.testAgent2.toString()) && event.getAffectedAgentId().toString().equals(this.testAgent5.toString())) {
						Assert.assertEquals("wrong delay.", 79.0, event.getDelay(), MatsimTestUtils.EPSILON);
					} 
				}
			Assert.assertEquals("wrong number of congestion events" , 10, congestionEvents.size());
			
			}
			
		}
	
	// ################################################################################################################################

	private void setPopulation01(Scenario scenario) {
			
		Population population = scenario.getPopulation();
        PopulationFactoryImpl popFactory = (PopulationFactoryImpl) scenario.getPopulation().getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();

		Activity lastActLink3 = popFactory.createActivityFromLinkId("work", linkId3);
			
		// leg: 1,2,3
		Leg leg_1_3 = popFactory.createLeg("car");
		List<Id<Link>> linkIds3 = new ArrayList<Id<Link>>();
		linkIds3.add(linkId2);
		NetworkRoute route3 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId3);
		route3.setLinkIds(linkId1, linkIds3, linkId3);
		leg_1_3.setRoute(route3);		
			
		// ################################################################
		// first agent (1 --> 3)
		Person person1 = popFactory.createPerson(testAgent1);
		Plan plan1 = popFactory.createPlan();
		Activity act1 = popFactory.createActivityFromLinkId("home", linkId1);
		act1.setEndTime(99);
		plan1.addActivity(act1);
		plan1.addLeg(leg_1_3);
		plan1.addActivity(lastActLink3);
		person1.addPlan(plan1);
		population.addPerson(person1);

		// ################################################################
		// second agent (1 --> 3)		
		Person person2 = popFactory.createPerson(testAgent2);
		Plan plan2 = popFactory.createPlan();
		Activity act2 = popFactory.createActivityFromLinkId("home", linkId1);
		act2.setEndTime(99);
		plan2.addActivity(act2);
		plan2.addLeg(leg_1_3);
		plan2.addActivity(lastActLink3);	
		person2.addPlan(plan2);
		population.addPerson(person2);
			
		// ################################################################
		// third agent (1 --> 3)		
		Person person3 = popFactory.createPerson(testAgent3);
		Plan plan3 = popFactory.createPlan();
		Activity act3 = popFactory.createActivityFromLinkId("home", linkId1);
		act3.setEndTime(99);
		plan3.addActivity(act3);
		plan3.addLeg(leg_1_3);
		plan3.addActivity(lastActLink3);	
		person3.addPlan(plan3);
		population.addPerson(person3);

	}
	
	private void setPopulation02(Scenario scenario) {
		
		Population population = scenario.getPopulation();
        PopulationFactoryImpl popFactory = (PopulationFactoryImpl) scenario.getPopulation().getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();

		Activity lastActLink3 = popFactory.createActivityFromLinkId("work", linkId3);
		
		// leg: 1,2,3
		Leg leg_1_3 = popFactory.createLeg("car");
		List<Id<Link>> linkIds3 = new ArrayList<Id<Link>>();
		linkIds3.add(linkId2);
		NetworkRoute route3 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId3);
		route3.setLinkIds(linkId1, linkIds3, linkId3);
		leg_1_3.setRoute(route3);		
		
		// ################################################################
		// first agent (1 --> 3)
		Person person1 = popFactory.createPerson(testAgent1);
		Plan plan1 = popFactory.createPlan();
		Activity act1 = popFactory.createActivityFromLinkId("home", linkId1);
		act1.setEndTime(99);
		plan1.addActivity(act1);
		plan1.addLeg(leg_1_3);
		plan1.addActivity(lastActLink3);
		person1.addPlan(plan1);
		population.addPerson(person1);

		// ################################################################
		// second agent (1 --> 3)		
		Person person2 = popFactory.createPerson(testAgent2);
		Plan plan2 = popFactory.createPlan();
		Activity act2 = popFactory.createActivityFromLinkId("home", linkId1);
		act2.setEndTime(109);
		plan2.addActivity(act2);
		plan2.addLeg(leg_1_3);
		plan2.addActivity(lastActLink3);	
		person2.addPlan(plan2);
		population.addPerson(person2);
		
		// ################################################################
		// third agent (1 --> 3)		
		Person person3 = popFactory.createPerson(testAgent3);
		Plan plan3 = popFactory.createPlan();
		Activity act3 = popFactory.createActivityFromLinkId("home", linkId1);
		act3.setEndTime(124);
		plan3.addActivity(act3);
		plan3.addLeg(leg_1_3);
		plan3.addActivity(lastActLink3);	
		person3.addPlan(plan3);
		population.addPerson(person3);

	}
		
	private void setPopulation03(Scenario scenario) {
		
		Population population = scenario.getPopulation();
        PopulationFactoryImpl popFactory = (PopulationFactoryImpl) scenario.getPopulation().getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();

		Activity lastActLink3 = popFactory.createActivityFromLinkId("work", linkId3);
		Activity lastActLink4 = popFactory.createActivityFromLinkId("work", linkId4);
		
		// leg: 3,4
		Leg leg_3_4 = popFactory.createLeg("car");
		List<Id<Link>> linkIds1 = new ArrayList<Id<Link>>();
		NetworkRoute route1 = (NetworkRoute) routeFactory.createRoute(linkId3, linkId4);
		route1.setLinkIds(linkId3, linkIds1, linkId4);
		leg_3_4.setRoute(route1);
		
		// leg: 2,3,4
		Leg leg_2_4 = popFactory.createLeg("car");
		List<Id<Link>> linkIds2 = new ArrayList<Id<Link>>();
		linkIds2.add(linkId3);
		NetworkRoute route2 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId4);
		route2.setLinkIds(linkId2, linkIds2, linkId4);
		leg_2_4.setRoute(route2);
		
		// leg: 1,2,3
		Leg leg_1_3 = popFactory.createLeg("car");
		List<Id<Link>> linkIds3 = new ArrayList<Id<Link>>();
		linkIds3.add(linkId2);
		NetworkRoute route3 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId3);
		route3.setLinkIds(linkId1, linkIds3, linkId3);
		leg_1_3.setRoute(route3);
		
		// ################################################################
		// first agent activating the flow capacity on link3 (3 --> 4)
		Person person1 = popFactory.createPerson(testAgent1);
		Plan plan1 = popFactory.createPlan();
		Activity act1 = popFactory.createActivityFromLinkId("home", linkId3);
		act1.setEndTime(99);
		plan1.addActivity(act1);
		plan1.addLeg(leg_3_4);
		plan1.addActivity(lastActLink4);
		person1.addPlan(plan1);
		population.addPerson(person1);
		
		// ################################################################
		// second agent blocking link3 for 1 min (2 --> 4)
		Person person2 = popFactory.createPerson(testAgent2);
		Plan plan2 = popFactory.createPlan();
		Activity act2 = popFactory.createActivityFromLinkId("home", linkId2);
		act2.setEndTime(99);
		plan2.addActivity(act2);
		plan2.addLeg(leg_2_4);
		plan2.addActivity(lastActLink4);
		person2.addPlan(plan2);
		population.addPerson(person2);			
		
		// ################################################################
		// third agent: in buffer of link2 (1 --> 3)
		Person person3 = popFactory.createPerson(testAgent3);
		Plan plan3 = popFactory.createPlan();
		Activity act3 = popFactory.createActivityFromLinkId("home", linkId1);
//		act3.setEndTime(104);
		act3.setEndTime(99);
		plan3.addActivity(act3);
		plan3.addLeg(leg_1_3);
		plan3.addActivity(lastActLink3);
		person3.addPlan(plan3);
		population.addPerson(person3);

		// ################################################################
		// last agent causing the troubles... (1 --> 3)		
		Person person4 = popFactory.createPerson(testAgent4);
		Plan plan4 = popFactory.createPlan();
		Activity act4 = popFactory.createActivityFromLinkId("home", linkId1);
//		act4.setEndTime(105);
		act4.setEndTime(100);
		plan4.addActivity(act4);
		plan4.addLeg(leg_1_3);
		plan4.addActivity(lastActLink3);	
		person4.addPlan(plan4);
		population.addPerson(person4);

	}
	
	
	
	private void setPopulation11(Scenario scenario) {
		
		Population population = scenario.getPopulation();
        PopulationFactoryImpl popFactory = (PopulationFactoryImpl) scenario.getPopulation().getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();

		Activity lastActLink3 = popFactory.createActivityFromLinkId("work", linkId3);
		Activity lastActLink4 = popFactory.createActivityFromLinkId("work", linkId4);
	
		// leg: 1,2,3,4
		Leg leg_1_4 = popFactory.createLeg("car");
		List<Id<Link>> linkIds4 = new ArrayList<Id<Link>>();
		linkIds4.add(linkId2);
		linkIds4.add(linkId3);
		NetworkRoute route4 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId4);
		route4.setLinkIds(linkId1, linkIds4, linkId4);
		leg_1_4.setRoute(route4);

		// leg: 1,2,3
		Leg leg_1_3 = popFactory.createLeg("car");
		List<Id<Link>> linkIds3 = new ArrayList<Id<Link>>();
		linkIds3.add(linkId2);
		NetworkRoute route3 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId3);
		route3.setLinkIds(linkId1, linkIds3, linkId3);
		leg_1_3.setRoute(route3);		
		
		// ################################################################
		// first agent (1 --> 3)
		Person person1 = popFactory.createPerson(testAgent1);
		Plan plan1 = popFactory.createPlan();
		Activity act1 = popFactory.createActivityFromLinkId("home", linkId1);
		act1.setEndTime(99);
		plan1.addActivity(act1);
		plan1.addLeg(leg_1_3);
		plan1.addActivity(lastActLink3);
		person1.addPlan(plan1);
		population.addPerson(person1);

		// ################################################################
		// second agent (1 --> 4)		
		Person person2 = popFactory.createPerson(testAgent2);
		Plan plan2 = popFactory.createPlan();
		Activity act2 = popFactory.createActivityFromLinkId("home", linkId1);
		act2.setEndTime(104);
		plan2.addActivity(act2);
		plan2.addLeg(leg_1_4);
		plan2.addActivity(lastActLink4);	
		person2.addPlan(plan2);
		population.addPerson(person2);
	
	}
	
	private void setPopulation12(Scenario scenario) {
		
		Population population = scenario.getPopulation();
        PopulationFactoryImpl popFactory = (PopulationFactoryImpl) scenario.getPopulation().getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();

		Activity lastActLink3 = popFactory.createActivityFromLinkId("work", linkId3);
		Activity lastActLink4 = popFactory.createActivityFromLinkId("work", linkId4);
	
		// leg: 1,2,3,4
		Leg leg_1_4 = popFactory.createLeg("car");
		List<Id<Link>> linkIds4 = new ArrayList<Id<Link>>();
		linkIds4.add(linkId2);
		linkIds4.add(linkId3);
		NetworkRoute route4 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId4);
		route4.setLinkIds(linkId1, linkIds4, linkId4);
		leg_1_4.setRoute(route4);

		// leg: 3,4
		Leg leg_3_4 = popFactory.createLeg("car");
		List<Id<Link>> linkIds34 = new ArrayList<Id<Link>>();
		NetworkRoute route34 = (NetworkRoute) routeFactory.createRoute(linkId3, linkId4);
		route34.setLinkIds(linkId3, linkIds34, linkId4);
		leg_3_4.setRoute(route34);		
		
		// ################################################################
		// first agent (1 --> 4)
		Person person1 = popFactory.createPerson(testAgent1);
		Plan plan1 = popFactory.createPlan();
		Activity act1 = popFactory.createActivityFromLinkId("home", linkId1);
		act1.setEndTime(99);
		plan1.addActivity(act1);
		plan1.addLeg(leg_1_4);
		plan1.addActivity(lastActLink4);
		person1.addPlan(plan1);
		population.addPerson(person1);

		// ################################################################
		// second agent (3 --> 4)		
		Person person2 = popFactory.createPerson(testAgent2);
		Plan plan2 = popFactory.createPlan();
		Activity act2 = popFactory.createActivityFromLinkId("home", linkId3);
		act2.setEndTime(141);
		plan2.addActivity(act2);
		plan2.addLeg(leg_3_4);
		plan2.addActivity(lastActLink4);	
		person2.addPlan(plan2);
		population.addPerson(person2);

		// ################################################################
		// third agent (1 --> 4)		
		Person person3 = popFactory.createPerson(testAgent3);
		Plan plan3 = popFactory.createPlan();
		Activity act3 = popFactory.createActivityFromLinkId("home", linkId1);
		act3.setEndTime(299);
		plan3.addActivity(act3);
		plan3.addLeg(leg_1_4);
		plan3.addActivity(lastActLink4);	
		person3.addPlan(plan3);
		population.addPerson(person3);
		
		// ################################################################
		// fourth agent (3 --> 4)		
		Person person4 = popFactory.createPerson(testAgent4);
		Plan plan4 = popFactory.createPlan();
		Activity act4 = popFactory.createActivityFromLinkId("home", linkId3);
		act4.setEndTime(381);
		plan4.addActivity(act4);
		plan4.addLeg(leg_3_4);
		plan4.addActivity(lastActLink4);	
		person4.addPlan(plan4);
		population.addPerson(person4);
		
	}

	private void setPopulation13(Scenario scenario) {
		
		Population population = scenario.getPopulation();
        PopulationFactoryImpl popFactory = (PopulationFactoryImpl) scenario.getPopulation().getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();

		Activity lastActLink4 = popFactory.createActivityFromLinkId("work", linkId4);
	
		// leg: 1,2,3,4
		Leg leg_1_4 = popFactory.createLeg("car");
		List<Id<Link>> linkIds4 = new ArrayList<Id<Link>>();
		linkIds4.add(linkId2);
		linkIds4.add(linkId3);
		NetworkRoute route4 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId4);
		route4.setLinkIds(linkId1, linkIds4, linkId4);
		leg_1_4.setRoute(route4);

		// leg: 3,4
		Leg leg_3_4 = popFactory.createLeg("car");
		List<Id<Link>> linkIds34 = new ArrayList<Id<Link>>();
		NetworkRoute route34 = (NetworkRoute) routeFactory.createRoute(linkId3, linkId4);
		route34.setLinkIds(linkId3, linkIds34, linkId4);
		leg_3_4.setRoute(route34);		
		
		// ################################################################
		// first agent (3 --> 4)
		Person person1 = popFactory.createPerson(testAgent1);
		Plan plan1 = popFactory.createPlan();
		Activity act1 = popFactory.createActivityFromLinkId("home", linkId3);
		act1.setEndTime(99);
		plan1.addActivity(act1);
		plan1.addLeg(leg_3_4);
		plan1.addActivity(lastActLink4);
		person1.addPlan(plan1);
		population.addPerson(person1);

		// ################################################################
		// second agent (1 --> 4)		
		Person person2 = popFactory.createPerson(testAgent2);
		Plan plan2 = popFactory.createPlan();
		Activity act2 = popFactory.createActivityFromLinkId("home", linkId1);
		act2.setEndTime(99);
		plan2.addActivity(act2);
		plan2.addLeg(leg_1_4);
		plan2.addActivity(lastActLink4);	
		person2.addPlan(plan2);
		population.addPerson(person2);

		// ################################################################
		// third agent (1 --> 4)		
		Person person3 = popFactory.createPerson(testAgent3);
		Plan plan3 = popFactory.createPlan();
		Activity act3 = popFactory.createActivityFromLinkId("home", linkId1);
		act3.setEndTime(132);
		plan3.addActivity(act3);
		plan3.addLeg(leg_1_4);
		plan3.addActivity(lastActLink4);	
		person3.addPlan(plan3);
		population.addPerson(person3);
		
		// ################################################################
		// fourth agent (1 --> 4)		
		Person person4 = popFactory.createPerson(testAgent4);
		Plan plan4 = popFactory.createPlan();
		Activity act4 = popFactory.createActivityFromLinkId("home", linkId1);
		act4.setEndTime(165);
		plan4.addActivity(act4);
		plan4.addLeg(leg_1_4);
		plan4.addActivity(lastActLink4);	
		person4.addPlan(plan4);
		population.addPerson(person4);
		
		// ################################################################
		// fifth agent (1 --> 4)		
		Person person5 = popFactory.createPerson(testAgent5);
		Plan plan5 = popFactory.createPlan();
		Activity act5 = popFactory.createActivityFromLinkId("home", linkId1);
		act5.setEndTime(188);
		plan5.addActivity(act5);
		plan5.addLeg(leg_1_4);
		plan5.addActivity(lastActLink4);	
		person5.addPlan(plan5);
		population.addPerson(person5);
		
		// ################################################################
		// sixth agent (1 --> 4)		
		Person person6 = popFactory.createPerson(testAgent6);
		Plan plan6 = popFactory.createPlan();
		Activity act6 = popFactory.createActivityFromLinkId("home", linkId1);
		act6.setEndTime(319);
		plan6.addActivity(act6);
		plan6.addLeg(leg_1_4);
		plan6.addActivity(lastActLink4);	
		person6.addPlan(plan6);
		population.addPerson(person6);
		
		
	}
	
	private void setPopulation14(Scenario scenario) {
		
		Population population = scenario.getPopulation();
        PopulationFactoryImpl popFactory = (PopulationFactoryImpl) scenario.getPopulation().getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();

		Activity lastActLink4 = popFactory.createActivityFromLinkId("work", linkId4);
		
		// leg: 1,2,3,4
		Leg leg_1_4 = popFactory.createLeg("car");
		List<Id<Link>> linkIds4 = new ArrayList<Id<Link>>();
		linkIds4.add(linkId2);
		linkIds4.add(linkId3);
		NetworkRoute route4 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId4);
		route4.setLinkIds(linkId1, linkIds4, linkId4);
		leg_1_4.setRoute(route4);		
		
		// ################################################################
		// first agent (1 --> 4)
		Person person1 = popFactory.createPerson(testAgent1);
		Plan plan1 = popFactory.createPlan();
		Activity act1 = popFactory.createActivityFromLinkId("home", linkId1);
		act1.setEndTime(99);
		plan1.addActivity(act1);
		plan1.addLeg(leg_1_4);
		plan1.addActivity(lastActLink4);
		person1.addPlan(plan1);
		population.addPerson(person1);

		// ################################################################
		// second agent (1 --> 4)		
		Person person2 = popFactory.createPerson(testAgent2);
		Plan plan2 = popFactory.createPlan();
		Activity act2 = popFactory.createActivityFromLinkId("home", linkId1);
		act2.setEndTime(119);
		plan2.addActivity(act2);
		plan2.addLeg(leg_1_4);
		plan2.addActivity(lastActLink4);	
		person2.addPlan(plan2);
		population.addPerson(person2);
		
		// ################################################################
		// third agent (1 --> 4)		
		Person person3 = popFactory.createPerson(testAgent3);
		Plan plan3 = popFactory.createPlan();
		Activity act3 = popFactory.createActivityFromLinkId("home", linkId1);
		act3.setEndTime(124);
		plan3.addActivity(act3);
		plan3.addLeg(leg_1_4);
		plan3.addActivity(lastActLink4);	
		person3.addPlan(plan3);
		population.addPerson(person3);
		
		// ################################################################
		// fourth agent (1 --> 4)		
		Person person4 = popFactory.createPerson(testAgent4);
		Plan plan4 = popFactory.createPlan();
		Activity act4 = popFactory.createActivityFromLinkId("home", linkId1);				
		act4.setEndTime(209);
		plan4.addActivity(act4);
		plan4.addLeg(leg_1_4);
		plan4.addActivity(lastActLink4);	
		person4.addPlan(plan4);
		population.addPerson(person4);
		
		// ################################################################
		// fifth agent (1 --> 4)		
		Person person5 = popFactory.createPerson(testAgent5);
		Plan plan5 = popFactory.createPlan();
		Activity act5 = popFactory.createActivityFromLinkId("home", linkId1);				
		act5.setEndTime(210);
		plan5.addActivity(act5);
		plan5.addLeg(leg_1_4);
		plan5.addActivity(lastActLink4);	
		person5.addPlan(plan5);
		population.addPerson(person5);
		
		// ################################################################
		// sixth agent (1 --> 4)		
		Person person6 = popFactory.createPerson(testAgent6);
		Plan plan6 = popFactory.createPlan();
		Activity act6 = popFactory.createActivityFromLinkId("home", linkId1);				
		act6.setEndTime(211);
		plan6.addActivity(act6);
		plan6.addLeg(leg_1_4);
		plan6.addActivity(lastActLink4);	
		person6.addPlan(plan6);
		population.addPerson(person6);
	
	}
	
	private void setPopulation15(Scenario scenario) {
		
		Population population = scenario.getPopulation();
        PopulationFactoryImpl popFactory = (PopulationFactoryImpl) scenario.getPopulation().getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();

		Activity lastActLink4 = popFactory.createActivityFromLinkId("work", linkId4);
		
		// leg: 1,2,3,4
		Leg leg_1_4 = popFactory.createLeg("car");
		List<Id<Link>> linkIds4 = new ArrayList<Id<Link>>();
		linkIds4.add(linkId2);
		linkIds4.add(linkId3);
		NetworkRoute route4 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId4);
		route4.setLinkIds(linkId1, linkIds4, linkId4);
		leg_1_4.setRoute(route4);		
		
		// ################################################################
		// first agent (1 --> 4)
		Person person1 = popFactory.createPerson(testAgent1);
		Plan plan1 = popFactory.createPlan();
		Activity act1 = popFactory.createActivityFromLinkId("home", linkId1);
		act1.setEndTime(99);
		plan1.addActivity(act1);
		plan1.addLeg(leg_1_4);
		plan1.addActivity(lastActLink4);
		person1.addPlan(plan1);
		population.addPerson(person1);

		// ################################################################
		// second agent (1 --> 4)		
		Person person2 = popFactory.createPerson(testAgent2);
		Plan plan2 = popFactory.createPlan();
		Activity act2 = popFactory.createActivityFromLinkId("home", linkId1);
		act2.setEndTime(114);
		plan2.addActivity(act2);
		plan2.addLeg(leg_1_4);
		plan2.addActivity(lastActLink4);	
		person2.addPlan(plan2);
		population.addPerson(person2);
		
		// ################################################################
		// third agent (1 --> 4)		
		Person person3 = popFactory.createPerson(testAgent3);
		Plan plan3 = popFactory.createPlan();
		Activity act3 = popFactory.createActivityFromLinkId("home", linkId1);
		act3.setEndTime(129);
		plan3.addActivity(act3);
		plan3.addLeg(leg_1_4);
		plan3.addActivity(lastActLink4);	
		person3.addPlan(plan3);
		population.addPerson(person3);
		
		// ################################################################
		// fourth agent (1 --> 4)		
		Person person4 = popFactory.createPerson(testAgent4);
		Plan plan4 = popFactory.createPlan();
		Activity act4 = popFactory.createActivityFromLinkId("home", linkId1);				
		act4.setEndTime(144);
		plan4.addActivity(act4);
		plan4.addLeg(leg_1_4);
		plan4.addActivity(lastActLink4);	
		person4.addPlan(plan4);
		population.addPerson(person4);
		
		// ################################################################
		// fifth agent (1 --> 4)		
		Person person5 = popFactory.createPerson(testAgent5);
		Plan plan5 = popFactory.createPlan();
		Activity act5 = popFactory.createActivityFromLinkId("home", linkId1);				
		act5.setEndTime(159);
		plan5.addActivity(act5);
		plan5.addLeg(leg_1_4);
		plan5.addActivity(lastActLink4);	
		person5.addPlan(plan5);
		population.addPerson(person5);
		
		// ################################################################
		// sixth agent (1 --> 4)		
		Person person6 = popFactory.createPerson(testAgent6);
		Plan plan6 = popFactory.createPlan();
		Activity act6 = popFactory.createActivityFromLinkId("home", linkId1);				
		act6.setEndTime(599);
		plan6.addActivity(act6);
		plan6.addLeg(leg_1_4);
		plan6.addActivity(lastActLink4);	
		person6.addPlan(plan6);
		population.addPerson(person6);
	
	}
	
	private void setPopulation16(Scenario scenario) {
		
		Population population = scenario.getPopulation();
        PopulationFactoryImpl popFactory = (PopulationFactoryImpl) scenario.getPopulation().getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();

		Activity lastActLink4 = popFactory.createActivityFromLinkId("work", linkId4);
		Activity lastActLink3 = popFactory.createActivityFromLinkId("work", linkId3);
		
		// leg: 1,2,3,4
		Leg leg_1_4 = popFactory.createLeg("car");
		List<Id<Link>> linkIds4 = new ArrayList<Id<Link>>();
		linkIds4.add(linkId2);
		linkIds4.add(linkId3);
		NetworkRoute route4 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId4);
		route4.setLinkIds(linkId1, linkIds4, linkId4);
		leg_1_4.setRoute(route4);

		// leg: 1,2,3
		Leg leg_1_3 = popFactory.createLeg("car");
		List<Id<Link>> linkIds3 = new ArrayList<Id<Link>>();
		linkIds3.add(linkId2);
		NetworkRoute route3 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId3);
		route3.setLinkIds(linkId1, linkIds3, linkId3);
		leg_1_3.setRoute(route3);		
		
		// ################################################################
		// first agent (1 --> 4)
		Person person1 = popFactory.createPerson(testAgent1);
		Plan plan1 = popFactory.createPlan();
		Activity act1 = popFactory.createActivityFromLinkId("home", linkId1);
		act1.setEndTime(99);
		plan1.addActivity(act1);
		plan1.addLeg(leg_1_4);
		plan1.addActivity(lastActLink4);
		person1.addPlan(plan1);
		population.addPerson(person1);

		// ################################################################
		// second agent (1 --> 4)		
		Person person2 = popFactory.createPerson(testAgent2);
		Plan plan2 = popFactory.createPlan();
		Activity act2 = popFactory.createActivityFromLinkId("home", linkId1);
		act2.setEndTime(114);
		plan2.addActivity(act2);
		plan2.addLeg(leg_1_4);
		plan2.addActivity(lastActLink4);	
		person2.addPlan(plan2);
		population.addPerson(person2);
		
		// ################################################################
		// third agent (1 --> 3)		
		Person person3 = popFactory.createPerson(testAgent3);
		Plan plan3 = popFactory.createPlan();
		Activity act3 = popFactory.createActivityFromLinkId("home", linkId1);
		act3.setEndTime(129);
		plan3.addActivity(act3);
		plan3.addLeg(leg_1_3);
		plan3.addActivity(lastActLink3);	
		person3.addPlan(plan3);
		population.addPerson(person3);
		
		// ################################################################
		// fourth agent (1 --> 4)		
		Person person4 = popFactory.createPerson(testAgent4);
		Plan plan4 = popFactory.createPlan();
		Activity act4 = popFactory.createActivityFromLinkId("home", linkId1);				
		act4.setEndTime(144);
		plan4.addActivity(act4);
		plan4.addLeg(leg_1_4);
		plan4.addActivity(lastActLink4);	
		person4.addPlan(plan4);
		population.addPerson(person4);
		
		// ################################################################
		// fifth agent (1 --> 4)		
		Person person5 = popFactory.createPerson(testAgent5);
		Plan plan5 = popFactory.createPlan();
		Activity act5 = popFactory.createActivityFromLinkId("home", linkId1);				
		act5.setEndTime(159);
		plan5.addActivity(act5);
		plan5.addLeg(leg_1_4);
		plan5.addActivity(lastActLink4);	
		person5.addPlan(plan5);
		population.addPerson(person5);
		
		// ################################################################
		// sixth agent (1 --> 4)		
		Person person6 = popFactory.createPerson(testAgent6);
		Plan plan6 = popFactory.createPlan();
		Activity act6 = popFactory.createActivityFromLinkId("home", linkId1);				
		act6.setEndTime(164);
		plan6.addActivity(act6);
		plan6.addLeg(leg_1_4);
		plan6.addActivity(lastActLink4);	
		person6.addPlan(plan6);
		population.addPerson(person6);
		
		// ################################################################
		// seventh agent (1 --> 4)		
		Person person7 = popFactory.createPerson(testAgent7);
		Plan plan7 = popFactory.createPlan();
		Activity act7 = popFactory.createActivityFromLinkId("home", linkId1);				
		act6.setEndTime(799);
		plan7.addActivity(act7);
		plan7.addLeg(leg_1_4);
		plan7.addActivity(lastActLink4);	
		person7.addPlan(plan7);
		population.addPerson(person7);
	
	}
	
	private void setPopulation17(Scenario scenario) {
		
		Population population = scenario.getPopulation();
        PopulationFactoryImpl popFactory = (PopulationFactoryImpl) scenario.getPopulation().getFactory();
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();

		Activity lastActLink4 = popFactory.createActivityFromLinkId("work", linkId4);
	
		// leg: 1,2,3,4
		Leg leg_1_4 = popFactory.createLeg("car");
		List<Id<Link>> linkIds4 = new ArrayList<Id<Link>>();
		linkIds4.add(linkId2);
		linkIds4.add(linkId3);
		NetworkRoute route4 = (NetworkRoute) routeFactory.createRoute(linkId1, linkId4);
		route4.setLinkIds(linkId1, linkIds4, linkId4);
		leg_1_4.setRoute(route4);

		// leg: 2,4
		Leg leg_2_4 = popFactory.createLeg("car");
		List<Id<Link>> linkIds24 = new ArrayList<Id<Link>>();
		linkIds24.add(linkId3);
		NetworkRoute route24 = (NetworkRoute) routeFactory.createRoute(linkId2, linkId4);
		route24.setLinkIds(linkId2, linkIds24, linkId4);
		leg_2_4.setRoute(route24);		
		
		// ################################################################
		// first agent (1 --> 4)
		Person person1 = popFactory.createPerson(testAgent1);
		Plan plan1 = popFactory.createPlan();
		Activity act1 = popFactory.createActivityFromLinkId("home", linkId1);
		act1.setEndTime(99);
		plan1.addActivity(act1);
		plan1.addLeg(leg_1_4);
		plan1.addActivity(lastActLink4);
		person1.addPlan(plan1);
		population.addPerson(person1);

		// ################################################################
		// second agent (2 --> 4)		
		Person person2 = popFactory.createPerson(testAgent2);
		Plan plan2 = popFactory.createPlan();
		Activity act2 = popFactory.createActivityFromLinkId("home", linkId2);
		act2.setEndTime(161);
		plan2.addActivity(act2);
		plan2.addLeg(leg_2_4);
		plan2.addActivity(lastActLink4);	
		person2.addPlan(plan2);
		population.addPerson(person2);
		
		// ################################################################
		// third agent (3 --> 4)		
		Person person3 = popFactory.createPerson(testAgent3);
		Plan plan3 = popFactory.createPlan();
		Activity act3 = popFactory.createActivityFromLinkId("home", linkId2);
		act3.setEndTime(172);
		plan3.addActivity(act3);
		plan3.addLeg(leg_2_4);
		plan3.addActivity(lastActLink4);	
		person3.addPlan(plan3);
		population.addPerson(person3);
		
		// ################################################################
		// fourth agent (3 --> 4)		
		Person person4 = popFactory.createPerson(testAgent4);
		Plan plan4 = popFactory.createPlan();
		Activity act4 = popFactory.createActivityFromLinkId("home", linkId2);				
		act4.setEndTime(183);
		plan4.addActivity(act4);
		plan4.addLeg(leg_2_4);
		plan4.addActivity(lastActLink4);	
		person4.addPlan(plan4);
		population.addPerson(person4);
		
		// ################################################################
		// fifth agent (1 --> 4)		
		Person person5 = popFactory.createPerson(testAgent5);
		Plan plan5 = popFactory.createPlan();
		Activity act5 = popFactory.createActivityFromLinkId("home", linkId1);				
		act5.setEndTime(148);
		plan5.addActivity(act5);
		plan5.addLeg(leg_1_4);
		plan5.addActivity(lastActLink4);	
		person5.addPlan(plan5);
		population.addPerson(person5);
	
	}
	
	private Scenario loadScenario1() {
			
		//    -----link8----   ----link7----   ----link6----   ----link5----   
		// (0)				(1)				(2)				(3)				(4)
		//    -----link1----   ----link2----   ----link3----   ----link4----   
		
		Config config = testUtils.loadConfig(null);
		QSimConfigGroup qSimConfigGroup = config.qsim();
		qSimConfigGroup.setFlowCapFactor(1.0);
		qSimConfigGroup.setStorageCapFactor(1.0);
		qSimConfigGroup.setInsertingWaitingVehiclesBeforeDrivingVehicles(true);
		qSimConfigGroup.setRemoveStuckVehicles(true);
		qSimConfigGroup.setStuckTime(100.0);
		Scenario scenario = (ScenarioUtils.createScenario(config));
	
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		network.setEffectiveCellSize(7.5);
		network.setCapacityPeriod(3600.);
		
		Node node0 = network.getFactory().createNode(new IdImpl("0"), scenario.createCoord(0., 0.));
		Node node1 = network.getFactory().createNode(new IdImpl("1"), scenario.createCoord(100., 0.));
		Node node2 = network.getFactory().createNode(new IdImpl("2"), scenario.createCoord(200., 0.));
		Node node3 = network.getFactory().createNode(new IdImpl("3"), scenario.createCoord(300., 0.));
		Node node4 = network.getFactory().createNode(new IdImpl("4"), scenario.createCoord(400., 0.));
		
		Link link1 = network.getFactory().createLink(this.linkId1, node0, node1);
		Link link2 = network.getFactory().createLink(this.linkId2, node1, node2);
		Link link3 = network.getFactory().createLink(this.linkId3, node2, node3);
		Link link4 = network.getFactory().createLink(this.linkId4, node3, node4);
		Link link5 = network.getFactory().createLink(this.linkId5, node4, node3);
		Link link6 = network.getFactory().createLink(this.linkId6, node3, node2);
		Link link7 = network.getFactory().createLink(this.linkId7, node2, node1);
		Link link8 = network.getFactory().createLink(this.linkId8, node1, node0);

		Set<String> modes = new HashSet<String>();
		modes.add("car");
		
		// link without capacity restrictions
		link1.setAllowedModes(modes);
		link1.setCapacity(10800);
		link1.setFreespeed(500);
		link1.setNumberOfLanes(100);
		link1.setLength(500);
		
		// link with low flow capacity: one car every 10 sec
		link2.setAllowedModes(modes);
		link2.setCapacity(360);
		link2.setFreespeed(10);
		link2.setNumberOfLanes(100);
		link2.setLength(500);
		
		// link meant to reach storage capacity: space for one car, flow capacity: one car every 60 sec
		link3.setAllowedModes(modes);
		link3.setCapacity(60);
		link3.setFreespeed(500);
		link3.setNumberOfLanes(1);
		link3.setLength(7.5);
		
		// link without capacity restrictions
		link4.setAllowedModes(modes);
		link4.setCapacity(10800);
		link4.setFreespeed(500);
		link4.setNumberOfLanes(100);
		link4.setLength(500);
		
		// links 5 to 8 should be there, to make sure that the calculations work if there are more outgoing links
		// links 5 to 8 without capacity restrictions
		link5.setAllowedModes(modes);
		link5.setCapacity(10800);
		link5.setFreespeed(500);
		link5.setNumberOfLanes(100);
		link5.setLength(500);
		link6.setAllowedModes(modes);
		link6.setCapacity(10800);
		link6.setFreespeed(500);
		link6.setNumberOfLanes(100);
		link6.setLength(500);
		link7.setAllowedModes(modes);
		link7.setCapacity(10800);
		link7.setFreespeed(500);
		link7.setNumberOfLanes(100);
		link7.setLength(500);
		link8.setAllowedModes(modes);
		link8.setCapacity(10800);
		link8.setFreespeed(500);
		link8.setNumberOfLanes(100);
		link8.setLength(500);
		
		network.addNode(node0);
		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);
		network.addNode(node4);

		network.addLink(link1);
		network.addLink(link2);
		network.addLink(link3);
		network.addLink(link4);
		network.addLink(link5);
		network.addLink(link6);
		network.addLink(link7);
		network.addLink(link8);

		this.events = EventsUtils.createEventsManager();
		return scenario;
	}
	
	private Scenario loadScenario2() {
		
		// (0)-----link1-----(1)-----link2-----(2)-----link3-----(3)-----link4-----(4)
		
		Config config = testUtils.loadConfig(null);
		QSimConfigGroup qSimConfigGroup = config.qsim();
		qSimConfigGroup.setFlowCapFactor(1.0);
		qSimConfigGroup.setStorageCapFactor(1.0);
		qSimConfigGroup.setInsertingWaitingVehiclesBeforeDrivingVehicles(true);
		qSimConfigGroup.setRemoveStuckVehicles(true);
		qSimConfigGroup.setStuckTime(100.0);
		Scenario scenario = (ScenarioUtils.createScenario(config));
	
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		network.setEffectiveCellSize(7.5);
		network.setCapacityPeriod(3600.);
		
		Node node0 = network.getFactory().createNode(new IdImpl("0"), scenario.createCoord(0., 0.));
		Node node1 = network.getFactory().createNode(new IdImpl("1"), scenario.createCoord(100., 0.));
		Node node2 = network.getFactory().createNode(new IdImpl("2"), scenario.createCoord(200., 0.));
		Node node3 = network.getFactory().createNode(new IdImpl("3"), scenario.createCoord(300., 0.));
		Node node4 = network.getFactory().createNode(new IdImpl("4"), scenario.createCoord(400., 0.));
		
		Link link1 = network.getFactory().createLink(this.linkId1, node0, node1);
		Link link2 = network.getFactory().createLink(this.linkId2, node1, node2);
		Link link3 = network.getFactory().createLink(this.linkId3, node2, node3);
		Link link4 = network.getFactory().createLink(this.linkId4, node3, node4);

		Set<String> modes = new HashSet<String>();
		modes.add("car");
		
		// link without capacity restrictions
		link1.setAllowedModes(modes);
		link1.setCapacity(10800);
		link1.setFreespeed(500);
		link1.setNumberOfLanes(100);
		link1.setLength(500);
		
		// link with low flow capacity: one car every 10 sec
		link2.setAllowedModes(modes);
		link2.setCapacity(360);
		link2.setFreespeed(10);
		link2.setNumberOfLanes(100);
		link2.setLength(500);
		
		// link without capacity restrictions
		link3.setAllowedModes(modes);
		link3.setCapacity(10800);
		link3.setFreespeed(500);
		link3.setNumberOfLanes(100);
		link3.setLength(500);
		
		// link without capacity restrictions
		link4.setAllowedModes(modes);
		link4.setCapacity(10800);
		link4.setFreespeed(500);
		link4.setNumberOfLanes(100);
		link4.setLength(500);
		
		network.addNode(node0);
		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);
		network.addNode(node4);

		network.addLink(link1);
		network.addLink(link2);
		network.addLink(link3);
		network.addLink(link4);

		this.events = EventsUtils.createEventsManager();
		return scenario;
	}
	
	private Scenario loadScenario3() {
		
		// (0)-----link1-----(1)-----link2-----(2)-----link3-----(3)-----link4-----(4)
		
		Config config = testUtils.loadConfig(null);
		QSimConfigGroup qSimConfigGroup = config.qsim();
		qSimConfigGroup.setFlowCapFactor(1.0);
		qSimConfigGroup.setStorageCapFactor(1.0);
		qSimConfigGroup.setInsertingWaitingVehiclesBeforeDrivingVehicles(true);
		qSimConfigGroup.setRemoveStuckVehicles(true);
		qSimConfigGroup.setStuckTime(100.0);
		Scenario scenario = (ScenarioUtils.createScenario(config));
	
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		network.setEffectiveCellSize(7.5);
		network.setCapacityPeriod(3600.);
		
		Node node0 = network.getFactory().createNode(new IdImpl("0"), scenario.createCoord(0., 0.));
		Node node1 = network.getFactory().createNode(new IdImpl("1"), scenario.createCoord(100., 0.));
		Node node2 = network.getFactory().createNode(new IdImpl("2"), scenario.createCoord(200., 0.));
		Node node3 = network.getFactory().createNode(new IdImpl("3"), scenario.createCoord(300., 0.));
		Node node4 = network.getFactory().createNode(new IdImpl("4"), scenario.createCoord(400., 0.));
		
		Link link1 = network.getFactory().createLink(this.linkId1, node0, node1);
		Link link2 = network.getFactory().createLink(this.linkId2, node1, node2);
		Link link3 = network.getFactory().createLink(this.linkId3, node2, node3);
		Link link4 = network.getFactory().createLink(this.linkId4, node3, node4);
		Link link5 = network.getFactory().createLink(this.linkId5, node2, node4);

		Set<String> modes = new HashSet<String>();
		modes.add("car");
		
		// link without capacity restrictions
		link1.setAllowedModes(modes);
		link1.setCapacity(10800);
		link1.setFreespeed(500);
		link1.setNumberOfLanes(100);
		link1.setLength(500);
		
		// link with low flow capacity: one car every 10 sec
		link2.setAllowedModes(modes);
		link2.setCapacity(360);
		link2.setFreespeed(10);
		link2.setNumberOfLanes(100);
		link2.setLength(500);
		
		// link meant to reach storage capacity: space for one car, one car every 60 sec
		link3.setAllowedModes(modes);
		link3.setCapacity(60);
		link3.setFreespeed(500);
		link3.setNumberOfLanes(1);
		link3.setLength(7.5);
		
		// link without capacity restrictions
		link4.setAllowedModes(modes);
		link4.setCapacity(10800);
		link4.setFreespeed(500);
		link4.setNumberOfLanes(100);
		link4.setLength(500);
		
		// link without capacity restrictions
		link5.setAllowedModes(modes);
		link5.setCapacity(10800);
		link5.setFreespeed(500);
		link5.setNumberOfLanes(100);
		link5.setLength(1100);
		
		network.addNode(node0);
		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);
		network.addNode(node4);

		network.addLink(link1);
		network.addLink(link2);
		network.addLink(link3);
		network.addLink(link4);
		network.addLink(link5);

		this.events = EventsUtils.createEventsManager();
		return scenario;
	}

	private Scenario loadScenario4() {
	
		// (0)-----link1-----(1)-----link2-----(2)-----link3-----(3)-----link4-----(4)
		
		Config config = testUtils.loadConfig(null);
		QSimConfigGroup qSimConfigGroup = config.qsim();
		qSimConfigGroup.setFlowCapFactor(1.0);
		qSimConfigGroup.setStorageCapFactor(1.0);
		qSimConfigGroup.setInsertingWaitingVehiclesBeforeDrivingVehicles(true);
		qSimConfigGroup.setRemoveStuckVehicles(true);
		qSimConfigGroup.setStuckTime(100.0);
		Scenario scenario = (ScenarioUtils.createScenario(config));
	
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		network.setEffectiveCellSize(7.5);
		network.setCapacityPeriod(3600.);
		
		Node node0 = network.getFactory().createNode(new IdImpl("0"), scenario.createCoord(0., 0.));
		Node node1 = network.getFactory().createNode(new IdImpl("1"), scenario.createCoord(100., 0.));
		Node node2 = network.getFactory().createNode(new IdImpl("2"), scenario.createCoord(200., 0.));
		Node node3 = network.getFactory().createNode(new IdImpl("3"), scenario.createCoord(300., 0.));
		Node node4 = network.getFactory().createNode(new IdImpl("4"), scenario.createCoord(400., 0.));
		
		Link link1 = network.getFactory().createLink(this.linkId1, node0, node1);
		Link link2 = network.getFactory().createLink(this.linkId2, node1, node2);
		Link link3 = network.getFactory().createLink(this.linkId3, node2, node3);
		Link link4 = network.getFactory().createLink(this.linkId4, node3, node4);
	
		Set<String> modes = new HashSet<String>();
		modes.add("car");
		
		// link without capacity restrictions
		link1.setAllowedModes(modes);
		link1.setCapacity(10800);
		link1.setFreespeed(500);
		link1.setNumberOfLanes(100);
		link1.setLength(500);
		
		// link with low flow capacity: one car every 10 sec
		link2.setAllowedModes(modes);
		link2.setCapacity(360);
		link2.setFreespeed(10);
		link2.setNumberOfLanes(100);
		link2.setLength(500);
		
		// link meant to reach storage capacity: space for three cars, one car every 80 sec
		link3.setAllowedModes(modes);
		link3.setCapacity(45);
		link3.setFreespeed(25);
		link3.setNumberOfLanes(1);
		link3.setLength(22.5);
		
		// link without capacity restrictions
		link4.setAllowedModes(modes);
		link4.setCapacity(10800);
		link4.setFreespeed(500);
		link4.setNumberOfLanes(100);
		link4.setLength(500);
		
		network.addNode(node0);
		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);
		network.addNode(node4);
	
		network.addLink(link1);
		network.addLink(link2);
		network.addLink(link3);
		network.addLink(link4);
	
		this.events = EventsUtils.createEventsManager();
		return scenario;
	}
	
	private QSim createQSim(Scenario sc, EventsManager events) {
		QSim qSim1 = new QSim(sc, events);
		ActivityEngine activityEngine = new ActivityEngine();
		qSim1.addMobsimEngine(activityEngine);
		qSim1.addActivityHandler(activityEngine);
        QNetsimEngineModule.configure(qSim1);
		TeleportationEngine teleportationEngine = new TeleportationEngine();
		qSim1.addMobsimEngine(teleportationEngine);
		QSim qSim = qSim1;
		AgentFactory agentFactory = new DefaultAgentFactory(qSim);
		PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory, qSim);
		qSim.addAgentSource(agentSource);
		return qSim;
	}
	
}
