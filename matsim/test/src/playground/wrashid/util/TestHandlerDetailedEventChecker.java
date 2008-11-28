package playground.wrashid.util;

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
import org.matsim.network.Link;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.routes.CarRoute;
import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.DES.SimulationParameters;
import playground.wrashid.DES.util.testable.PopulationModifier;
import playground.wrashid.DES.util.testable.TestHandler;
import playground.wrashid.PDES2.Road;
import playground.wrashid.deqsim.DEQSimStarter;
import playground.wrashid.deqsim.PDESStarter2;

public class TestHandlerDetailedEventChecker extends MatsimTestCase implements
		TestHandler, ActEndEventHandler, ActStartEventHandler,
		AgentDepartureEventHandler, AgentStuckEventHandler,
		AgentWait2LinkEventHandler, AgentArrivalEventHandler, EventHandler,
		LinkEnterEventHandler, LinkLeaveEventHandler {

	protected HashMap<String, LinkedList<PersonEvent>> events = new HashMap<String, LinkedList<PersonEvent>>();
	public LinkedList<PersonEvent> allEvents = new LinkedList<PersonEvent>();
	private HashMap<String, ExpectedNumberOfEvents> expectedNumberOfMessages = new HashMap<String, ExpectedNumberOfEvents>();
	private boolean printEvent = true;
	private Population population;

	public TestHandlerDetailedEventChecker() {

	}

	public void checkAssertions() {
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
				}
				assertEquals(true, lastTimeStamp <= list.get(i).time);
				lastTimeStamp = list.get(i).time;
			}
		}

		/*
		 * // checks not needed anymore, because covered by other checks
		 * 
		 *  // compare with expected number of events per agent, per event type
		 * for (LinkedList<BasicEvent> list : events.values()) { int
		 * linkEnterEventCounter = 0; int linkLeaveEventCounter = 0; int
		 * departureEventCounter = 0; int arrivalEventCounter = 0; for (int i =
		 * 0; i < list.size(); i++) { if (list.get(i) instanceof LinkEnterEvent) {
		 * linkEnterEventCounter++; } if (list.get(i) instanceof LinkLeaveEvent) {
		 * linkLeaveEventCounter++; } if (list.get(i) instanceof
		 * AgentDepartureEvent) { departureEventCounter++; } if (list.get(i)
		 * instanceof AgentArrivalEvent) { arrivalEventCounter++; }
		 *  } ExpectedNumberOfEvents expected =
		 * expectedNumberOfMessages.get(list .get(0).agentId); // if
		 * (estimate.expectedLinkEnterEvents!=linkEnterEventCounter){ // for
		 * (int j=0;j<list.size();j++){ //
		 * System.out.println(list.get(j).toString()); // } // } else { //
		 * System.out.println("ok"); // }
		 * 
		 * assertEquals(expected.expectedLinkEnterEvents,
		 * linkEnterEventCounter);
		 * assertEquals(expected.expectedLinkLeaveEvents,
		 * linkLeaveEventCounter);
		 * assertEquals(expected.expectedDepartureEvents,
		 * departureEventCounter); assertEquals(expected.expectedArrivalEvents,
		 * arrivalEventCounter); }
		 *  // check, that each enter event is followed by a leave event //
		 * check, that the same road is left, which is entered for (LinkedList<BasicEvent>
		 * list : events.values()) { for (int i = 0; i < list.size(); i++) { if
		 * (list.get(i) instanceof LinkEnterEvent) {
		 * 
		 * try { assertEquals(true, list.get(i + 1) instanceof LinkLeaveEvent); }
		 * catch (Exception e) { for (int j = 0; j < list.size(); j++) {
		 * System.out.println(list.get(j)); } e.printStackTrace();
		 * System.exit(0); }
		 * 
		 * LinkEnterEvent enterEvent = (LinkEnterEvent) list.get(i);
		 * LinkLeaveEvent leaveEvent = (LinkLeaveEvent) list .get(i + 1); //
		 * System.out.println(enterEvent); if
		 * (!enterEvent.linkId.equalsIgnoreCase(leaveEvent.linkId)) { for (int j =
		 * 0; j < list.size(); j++) { System.out.println(list.get(j)); }
		 * System.out.println("===========error===========");
		 * System.out.println(enterEvent.toString());
		 * System.out.println(leaveEvent.toString()); }
		 * 
		 * assertEquals(true, enterEvent.linkId
		 * .equalsIgnoreCase(leaveEvent.linkId)); } } }
		 * 
		 */

		// compare plan and events for each agent
		// compare: type of events, linkId
		for (LinkedList<PersonEvent> list : events.values()) {
			Person p = population.getPersons().get(
					new IdImpl(list.get(0).agentId));

			Plan plan = p.getSelectedPlan();
			int index = 0;

			ActIterator actIter = plan.getIteratorAct();
			LegIterator legIter = plan.getIteratorLeg();

			Act act = (Act) actIter.next();
			while (legIter.hasNext()) {

				Leg leg = (Leg) legIter.next();

				// each leg starts with departure on act link
				assertEquals(true,
						list.get(index) instanceof AgentDepartureEvent);
				assertEquals(true, act.getLinkId().toString().equalsIgnoreCase(
						((AgentDepartureEvent) list.get(index)).linkId));
				index++;

				// each leg must enter/leave act link
				assertEquals(true, list.get(index) instanceof LinkEnterEvent);
				assertEquals(true, act.getLinkId().toString().equalsIgnoreCase(
						((LinkEnterEvent) list.get(index)).linkId));
				index++;

				assertEquals(true, list.get(index) instanceof LinkLeaveEvent);
				assertEquals(true, act.getLinkId().toString().equalsIgnoreCase(
						((LinkLeaveEvent) list.get(index)).linkId));
				index++;

				for (Link link : ((CarRoute) leg.getRoute()).getLinks()) {
					// enter link and leave each link on route
					assertEquals(true,
							list.get(index) instanceof LinkEnterEvent);
					assertEquals(true, link.getId().toString()
							.equalsIgnoreCase(
									((LinkEnterEvent) list.get(index)).linkId));
					index++;

					assertEquals(true,
							list.get(index) instanceof LinkLeaveEvent);
					assertEquals(true, link.getId().toString()
							.equalsIgnoreCase(
									((LinkLeaveEvent) list.get(index)).linkId));
					index++;
				}

				// get next act
				act = (Act) actIter.next();

				// each leg ends with arrival on act link
				assertEquals(true, list.get(index) instanceof AgentArrivalEvent);
				assertEquals(true, act.getLinkId().toString().equalsIgnoreCase(
						((AgentArrivalEvent) list.get(index)).linkId));
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
	public void startTestDES(String configFilePath, boolean printEvent,
			String planFilePath, PopulationModifier populationModifier) {
		String[] args = new String[1];
		args[0] = configFilePath;
		this.printEvent = printEvent;
		SimulationParameters.testEventHandler = this;

		if (planFilePath != null) {
			SimulationParameters.testPlanPath = planFilePath;
		} else {
			SimulationParameters.testPlanPath = null;
		}

		if (populationModifier != null) {
			SimulationParameters.testPopulationModifier = populationModifier;
		} else {
			SimulationParameters.testPopulationModifier = new DummyPopulationModifier();
		}

		DEQSimStarter.main(args);
		this
				.calculateExpectedNumberOfEvents(SimulationParameters.testPopulationModifier
						.getPopulation());
		SimulationParameters.testEventHandler.checkAssertions();
	}

	// if populationModifier == null, then the DummyPopulationModifier is used
	// if planFilePath == null, then the plan specified in the config file is
	// used
	public void startTestPDES2(String configFilePath, boolean printEvent,
			String planFilePath, PopulationModifier populationModifier) {
		String[] args = new String[1];
		args[0] = configFilePath;
		this.printEvent = printEvent;
		playground.wrashid.PDES2.SimulationParameters.testEventHandler = this;

		if (planFilePath != null) {
			playground.wrashid.PDES2.SimulationParameters.testPlanPath = planFilePath;
		} else {
			playground.wrashid.PDES2.SimulationParameters.testPlanPath = null;
		}

		if (populationModifier != null) {
			playground.wrashid.PDES2.SimulationParameters.testPopulationModifier = populationModifier;
		} else {
			playground.wrashid.PDES2.SimulationParameters.testPopulationModifier = new DummyPopulationModifier();
		}

		PDESStarter2.main(args);
		this
				.calculateExpectedNumberOfEvents(playground.wrashid.PDES2.SimulationParameters.testPopulationModifier
						.getPopulation());
		playground.wrashid.PDES2.SimulationParameters.testEventHandler
				.checkAssertions();
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
					expected.expectedLinkEnterEvents += ((CarRoute) leg.getRoute()).getLinks().length + 1;
				}
			}

			expected.expectedArrivalEvents = expected.expectedDepartureEvents;
			expected.expectedLinkLeaveEvents = expected.expectedLinkEnterEvents;

			expectedNumberOfMessages.put(p.getId().toString(), expected);

			if (p.getId().toString().equalsIgnoreCase("1")) {
				// printPlan(plan);
			}

		}
	}

	private class ExpectedNumberOfEvents {
		public int expectedLinkEnterEvents;
		public int expectedLinkLeaveEvents;
		public int expectedDepartureEvents;
		public int expectedArrivalEvents;
	}

	private void printPlan(Plan plan) {
		LegIterator iter = plan.getIteratorLeg();
		while (iter.hasNext()) {
			Leg leg = (Leg) iter.next();
			for (Link link : ((CarRoute) leg.getRoute()).getLinks()) {
				System.out.print(link.getId()
						+ "("
						+ Road.allRoads.get(link.getId().toString())
								.getZoneId() + ")" + "-");
			}
			System.out.println();
		}
	}

}
