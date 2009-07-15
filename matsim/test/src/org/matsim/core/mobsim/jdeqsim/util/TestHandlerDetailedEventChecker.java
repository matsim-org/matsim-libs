package org.matsim.core.mobsim.jdeqsim.util;

import java.util.HashMap;
import java.util.LinkedList;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.basic.v01.events.BasicPersonEvent;
import org.matsim.api.basic.v01.events.handler.BasicPersonEventHandler;
import org.matsim.api.basic.v01.population.PlanElement;
import org.matsim.core.api.experimental.ScenarioImpl;
import org.matsim.core.config.Config;
import org.matsim.core.events.ActivityEndEvent;
import org.matsim.core.events.ActivityStartEvent;
import org.matsim.core.events.AgentArrivalEvent;
import org.matsim.core.events.AgentDepartureEvent;
import org.matsim.core.events.AgentWait2LinkEvent;
import org.matsim.core.events.Events;
import org.matsim.core.events.LinkEnterEvent;
import org.matsim.core.events.LinkLeaveEvent;
import org.matsim.core.events.parallelEventsHandler.ParallelEvents;
import org.matsim.core.mobsim.jdeqsim.JDEQSimulation;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioLoader;
import org.matsim.testcases.MatsimTestCase;

public class TestHandlerDetailedEventChecker extends MatsimTestCase implements BasicPersonEventHandler {

	protected HashMap<Id, LinkedList<BasicPersonEvent>> events = new HashMap<Id, LinkedList<BasicPersonEvent>>();
	public LinkedList<BasicPersonEvent> allEvents = new LinkedList<BasicPersonEvent>();
//	private HashMap<Id, ExpectedNumberOfEvents> expectedNumberOfMessages = new HashMap<Id, ExpectedNumberOfEvents>();
	protected boolean printEvent = true;

	public void checkAssertions(final PopulationImpl population) {

		// at least one event
		assertTrue(events.size() > 0);

		// all events of one agent must have ascending time stamps
		double lastTimeStamp;
		for (LinkedList<BasicPersonEvent> list : events.values()) {
			lastTimeStamp = Double.NEGATIVE_INFINITY;
			for (int i = 0; i < list.size(); i++) {
				if (lastTimeStamp > list.get(i).getTime()) {
					for (int j = 0; j < list.size(); j++) {
						System.out.println(list.get(j).toString());
					}
					System.out.println(lastTimeStamp);
					System.out.println(list.get(i).getTime());
					fail("Messages are not arriving in a consistent manner.");
				}

				assertTrue(lastTimeStamp <= list.get(i).getTime());
				lastTimeStamp = list.get(i).getTime();
			}
		}

		// compare plan and events for each agent
		// compare: type of events, linkId
		for (LinkedList<BasicPersonEvent> list : events.values()) {
			PersonImpl p = population.getPersons().get(list.get(0).getPersonId());
			// printEvents(list.get(0).agentId);
			PlanImpl plan = p.getSelectedPlan();
			int index = 0;

			ActivityImpl act = null;
			LegImpl leg = null;
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof ActivityImpl) {
					act = (ActivityImpl) pe;
					
					if (leg != null) {
						// each leg ends with arrival on act link
						assertTrue(list.get(index) instanceof AgentArrivalEvent);
						assertTrue(act.getLinkId().toString().equalsIgnoreCase(
								((AgentArrivalEvent) list.get(index)).getLinkId().toString()));
						index++;

						// each leg ends with arrival on act link
						assertTrue(list.get(index) instanceof ActivityStartEvent);
						assertEquals(act.getLinkId(), ((ActivityStartEvent) list.get(index)).getLinkId());
						index++;
					}
				} else if (pe instanceof LegImpl) {
					leg = (LegImpl) pe;

					// act end event
					assertTrue(list.get(index) instanceof ActivityEndEvent);
					assertEquals(act.getLinkId(), ((ActivityEndEvent) list.get(index)).getLinkId());
					index++;
					
					// each leg starts with departure on act link
					assertTrue(list.get(index) instanceof AgentDepartureEvent);
					assertTrue(act.getLinkId().toString().equalsIgnoreCase(
							((AgentDepartureEvent) list.get(index)).getLinkId().toString()));
					index++;
					
					// each CAR leg must enter/leave act link
					if (leg.getMode().equals(TransportMode.car)) {
						// the first LinkEnterEvent is a AgentWait2LinkEvent
						assertTrue(list.get(index) instanceof AgentWait2LinkEvent);
						assertTrue(act.getLinkId().toString().equalsIgnoreCase(
								((AgentWait2LinkEvent) list.get(index)).getLinkId().toString()));
						index++;
						
						assertTrue(list.get(index) instanceof LinkLeaveEvent);
						assertTrue(act.getLinkId().toString().equalsIgnoreCase(
								((LinkLeaveEvent) list.get(index)).getLinkId().toString()));
						index++;
						
						for (LinkImpl link : ((NetworkRoute) leg.getRoute()).getLinks()) {
							// enter link and leave each link on route
							assertTrue(list.get(index) instanceof LinkEnterEvent);
							assertTrue(link.getId().toString().equalsIgnoreCase(
									((LinkEnterEvent) list.get(index)).getLinkId().toString()));
							index++;
							
							assertTrue(list.get(index) instanceof LinkLeaveEvent);
							assertTrue(link.getId().toString().equalsIgnoreCase(
									((LinkLeaveEvent) list.get(index)).getLinkId().toString()));
							index++;
						}
					}
					
				}
			}
		}
	}

	public void handleEvent(BasicPersonEvent event) {
		if (!events.containsKey(event.getPersonId())) {
			events.put(event.getPersonId(), new LinkedList<BasicPersonEvent>());
		}
		events.get(event.getPersonId()).add(event);
		if (printEvent) {
			System.out.println(event.toString());
		}
		allEvents.add(event);
	}

	public void reset(int iteration) {
	}

	// if populationModifier == null, then the DummyPopulationModifier is used
	// if planFilePath == null, then the plan specified in the config file is
	// used
	public void startTestDES(String configFilePath, boolean printEvent, String planFilePath,
			PopulationModifier populationModifier) {
		Config config = loadConfig(configFilePath);
		if (planFilePath != null) {
			config.plans().setInputFile(planFilePath);
		}
		this.printEvent = printEvent;
		ScenarioLoader loader = new ScenarioLoader(config);
		loader.loadScenario();
		ScenarioImpl data = loader.getScenario();
		NetworkLayer network = (NetworkLayer) data.getNetwork();
		PopulationImpl population = data.getPopulation();
		if (populationModifier != null) {
			population = populationModifier.modifyPopulation(population);
		}
		Events events = new ParallelEvents(1);
		events.addHandler(this);
		events.initProcessing();
		new JDEQSimulation(network, population, events).run();
		events.finishProcessing();

//		this.calculateExpectedNumberOfEvents(population); // this method doesn't do anything useful/stateful
		this.checkAssertions(population);
	}

//	public void calculateExpectedNumberOfEvents(Population population) {
//
//		for (Person p : population.getPersons().values()) {
//			Plan plan = p.getSelectedPlan();
//			ExpectedNumberOfEvents expected = new ExpectedNumberOfEvents();
//			List<? extends BasicPlanElement> actsLegs = plan.getPlanElements();
//			expected.expectedDepartureEvents += actsLegs.size() / 2;
//
//			LegIterator iter = plan.getIteratorLeg();
//			while (iter.hasNext()) {
//				Leg leg = (Leg) iter.next();
//				// at the moment only cars are simulated on the road
//				if (leg.getMode().equals(TransportMode.car)) {
//					expected.expectedLinkEnterEvents += ((NetworkRoute) leg.getRoute()).getLinks().size() + 1;
//				}
//			}
//
//			expected.expectedArrivalEvents = expected.expectedDepartureEvents;
//			expected.expectedLinkLeaveEvents = expected.expectedLinkEnterEvents;
//
//			expectedNumberOfMessages.put(p.getId(), expected);
//
//		}
//	}

//	/*package*/ static class ExpectedNumberOfEvents {
//		public int expectedLinkEnterEvents;
//		public int expectedLinkLeaveEvents;
//		public int expectedDepartureEvents;
//		public int expectedArrivalEvents;
//	}

}
