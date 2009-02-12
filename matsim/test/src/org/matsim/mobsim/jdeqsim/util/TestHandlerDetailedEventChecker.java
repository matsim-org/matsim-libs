package org.matsim.mobsim.jdeqsim.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import org.matsim.basic.v01.BasicLeg;
import org.matsim.basic.v01.IdImpl;
import org.matsim.basic.v01.BasicPlanImpl.ActIterator;
import org.matsim.basic.v01.BasicPlanImpl.LegIterator;
import org.matsim.events.ActEndEvent;
import org.matsim.events.ActStartEvent;
import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.AgentStuckEvent;
import org.matsim.events.AgentWait2LinkEvent;
import org.matsim.events.LinkEnterEvent;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.events.PersonEvent;
import org.matsim.events.handler.ActEndEventHandler;
import org.matsim.events.handler.ActStartEventHandler;
import org.matsim.events.handler.AgentArrivalEventHandler;
import org.matsim.events.handler.AgentDepartureEventHandler;
import org.matsim.events.handler.AgentStuckEventHandler;
import org.matsim.events.handler.AgentWait2LinkEventHandler;
import org.matsim.events.handler.EventHandler;
import org.matsim.events.handler.LinkEnterEventHandler;
import org.matsim.events.handler.LinkLeaveEventHandler;
import org.matsim.mobsim.jdeqsim.JDEQSimStarterWithoutController;
import org.matsim.mobsim.jdeqsim.SimulationParameters;
import org.matsim.mobsim.jdeqsim.util.testable.PopulationModifier;
import org.matsim.mobsim.jdeqsim.util.testable.TestHandler;
import org.matsim.network.Link;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.routes.CarRoute;
import org.matsim.testcases.MatsimTestCase;

public class TestHandlerDetailedEventChecker extends MatsimTestCase implements TestHandler,
		ActEndEventHandler, ActStartEventHandler, AgentDepartureEventHandler, AgentStuckEventHandler,
		AgentWait2LinkEventHandler, AgentArrivalEventHandler, EventHandler, LinkEnterEventHandler,
		LinkLeaveEventHandler {

	protected HashMap<String, LinkedList<PersonEvent>> events = new HashMap<String, LinkedList<PersonEvent>>();
	public LinkedList<PersonEvent> allEvents = new LinkedList<PersonEvent>();
	private HashMap<String, ExpectedNumberOfEvents> expectedNumberOfMessages = new HashMap<String, ExpectedNumberOfEvents>();
	protected boolean printEvent = true;
	protected Population population;

	public TestHandlerDetailedEventChecker() {

	}

	public void checkAssertions() {

		// at least one event
		assertEquals(true, events.size() > 0);

		// all events of one agent must have ascending time stamps
		double lastTimeStamp;
		for (LinkedList<PersonEvent> list : events.values()) {
			lastTimeStamp = Double.NEGATIVE_INFINITY;
			for (int i = 0; i < list.size(); i++) {
				if (lastTimeStamp > list.get(i).time) {
					for (int j = 0; j < list.size(); j++) {
						System.out.println(list.get(j).toString());
					}
					System.out.println(lastTimeStamp);
					System.out.println(list.get(i).time);

					assertEquals(true, false); // in this case, something is
					// wrong (messages are not
					// arriving in a consistent
					// manner)
				}

				assertEquals(true, lastTimeStamp <= list.get(i).time);
				lastTimeStamp = list.get(i).time;
			}
		}

		// compare plan and events for each agent
		// compare: type of events, linkId
		for (LinkedList<PersonEvent> list : events.values()) {
			Person p = population.getPersons().get(new IdImpl(list.get(0).agentId));
			// printEvents(list.get(0).agentId);
			Plan plan = p.getSelectedPlan();
			int index = 0;

			ActIterator actIter = plan.getIteratorAct();
			LegIterator legIter = plan.getIteratorLeg();

			Act act = (Act) actIter.next();
			while (legIter.hasNext()) {

				Leg leg = (Leg) legIter.next();

				// act end event
				assertEquals(true, list.get(index) instanceof ActEndEvent);
				assertEquals(true, act.getLinkId().toString().equalsIgnoreCase(
						((ActEndEvent) list.get(index)).linkId));
				index++;

				// each leg starts with departure on act link
				assertEquals(true, list.get(index) instanceof AgentDepartureEvent);
				assertEquals(true, act.getLinkId().toString().equalsIgnoreCase(
						((AgentDepartureEvent) list.get(index)).linkId));
				index++;

				// each CAR leg must enter/leave act link
				if (leg.getMode().equals(BasicLeg.Mode.car)) {
					// the first LinkEnterEvent is a AgentWait2LinkEvent
					assertEquals(true, list.get(index) instanceof AgentWait2LinkEvent);
					assertEquals(true, act.getLinkId().toString().equalsIgnoreCase(
							((AgentWait2LinkEvent) list.get(index)).linkId));
					index++;

					assertEquals(true, list.get(index) instanceof LinkLeaveEvent);
					assertEquals(true, act.getLinkId().toString().equalsIgnoreCase(
							((LinkLeaveEvent) list.get(index)).linkId));
					index++;

					for (Link link : ((CarRoute) leg.getRoute()).getLinks()) {
						// enter link and leave each link on route
						assertEquals(true, list.get(index) instanceof LinkEnterEvent);
						assertEquals(true, link.getId().toString().equalsIgnoreCase(
								((LinkEnterEvent) list.get(index)).linkId));
						index++;

						assertEquals(true, list.get(index) instanceof LinkLeaveEvent);
						assertEquals(true, link.getId().toString().equalsIgnoreCase(
								((LinkLeaveEvent) list.get(index)).linkId));
						index++;
					}
				}

				// get next act
				act = (Act) actIter.next();

				// each leg ends with arrival on act link
				assertEquals(true, list.get(index) instanceof AgentArrivalEvent);
				assertEquals(true, act.getLinkId().toString().equalsIgnoreCase(
						((AgentArrivalEvent) list.get(index)).linkId));
				index++;

				// each leg ends with arrival on act link
				assertEquals(true, list.get(index) instanceof ActStartEvent);
				assertEquals(true, act.getLinkId().toString().equalsIgnoreCase(
						((ActStartEvent) list.get(index)).linkId));
				index++;
			}
		}

	}

	public void handleEvent(ActEndEvent event) {
		if (!events.containsKey(event.agentId)) {
			events.put(event.agentId, new LinkedList<PersonEvent>());
		}
		events.get(event.agentId).add(event);
		if (printEvent) {
			System.out.println(event.toString());
		}
		allEvents.add(event);
	}

	public void reset(int iteration) {

	}

	public void handleEvent(AgentDepartureEvent event) {
		if (!events.containsKey(event.agentId)) {
			events.put(event.agentId, new LinkedList<PersonEvent>());
		}
		events.get(event.agentId).add(event);
		if (printEvent) {
			System.out.println(event.toString());
		}
		allEvents.add(event);
	}

	public void handleEvent(AgentWait2LinkEvent event) {
		if (!events.containsKey(event.agentId)) {
			events.put(event.agentId, new LinkedList<PersonEvent>());
		}
		events.get(event.agentId).add(event);
		if (printEvent) {
			System.out.println(event.toString());
		}
		allEvents.add(event);
	}

	public void handleEvent(LinkLeaveEvent event) {
		if (!events.containsKey(event.agentId)) {
			events.put(event.agentId, new LinkedList<PersonEvent>());
		}
		events.get(event.agentId).add(event);
		if (printEvent) {
			System.out.println(event.toString());
		}
		allEvents.add(event);
	}

	public void handleEvent(LinkEnterEvent event) {
		if (!events.containsKey(event.agentId)) {
			events.put(event.agentId, new LinkedList<PersonEvent>());
		}
		events.get(event.agentId).add(event);
		if (printEvent) {
			System.out.println(event.toString());
		}
		allEvents.add(event);
	}

	public void handleEvent(AgentArrivalEvent event) {
		if (!events.containsKey(event.agentId)) {
			events.put(event.agentId, new LinkedList<PersonEvent>());
		}
		events.get(event.agentId).add(event);
		if (printEvent) {
			System.out.println(event.toString());
		}
		allEvents.add(event);
	}

	public void handleEvent(ActStartEvent event) {
		if (!events.containsKey(event.agentId)) {
			events.put(event.agentId, new LinkedList<PersonEvent>());
		}
		events.get(event.agentId).add(event);
		if (printEvent) {
			System.out.println(event.toString());
		}
		allEvents.add(event);
	}

	public void handleEvent(AgentStuckEvent event) {
		if (!events.containsKey(event.agentId)) {
			events.put(event.agentId, new LinkedList<PersonEvent>());
		}
		events.get(event.agentId).add(event);
		if (printEvent) {
			System.out.println(event.toString());
		}
		allEvents.add(event);
	}

	// if populationModifier == null, then the DummyPopulationModifier is used
	// if planFilePath == null, then the plan specified in the config file is
	// used
	public void startTestDES(String configFilePath, boolean printEvent, String planFilePath,
			PopulationModifier populationModifier) {
		String[] args = new String[1];
		args[0] = configFilePath;
		this.printEvent = printEvent;
		SimulationParameters.setTestEventHandler(this);

		if (planFilePath != null) {
			SimulationParameters.setTestPlanPath(planFilePath);
		} else {
			SimulationParameters.setTestPlanPath(null);
		}

		if (populationModifier != null) {
			SimulationParameters.setTestPopulationModifier(populationModifier);
		} else {
			SimulationParameters.setTestPopulationModifier(new DummyPopulationModifier());
		}

		JDEQSimStarterWithoutController.main(args);
		this
				.calculateExpectedNumberOfEvents(SimulationParameters.getTestPopulationModifier()
						.getPopulation());
		SimulationParameters.getTestEventHandler().checkAssertions();
	}

	public void calculateExpectedNumberOfEvents(Population population) {
		this.population = population;

		for (Person p : population.getPersons().values()) {
			Plan plan = p.getSelectedPlan();
			ExpectedNumberOfEvents expected = new ExpectedNumberOfEvents();
			ArrayList<Object> actsLegs = plan.getActsLegs();
			expected.expectedDepartureEvents += actsLegs.size() / 2;

			LegIterator iter = plan.getIteratorLeg();
			while (iter.hasNext()) {
				Leg leg = (Leg) iter.next();
				// at the moment only cars are simulated on the road
				if (leg.getMode().equals(BasicLeg.Mode.car)) {
					expected.expectedLinkEnterEvents += ((CarRoute) leg.getRoute()).getLinks().size() + 1;
				}
			}

			expected.expectedArrivalEvents = expected.expectedDepartureEvents;
			expected.expectedLinkLeaveEvents = expected.expectedLinkEnterEvents;

			expectedNumberOfMessages.put(p.getId().toString(), expected);

		}
	}

	private class ExpectedNumberOfEvents {
		public int expectedLinkEnterEvents;
		public int expectedLinkLeaveEvents;
		public int expectedDepartureEvents;
		public int expectedArrivalEvents;
	}

}
