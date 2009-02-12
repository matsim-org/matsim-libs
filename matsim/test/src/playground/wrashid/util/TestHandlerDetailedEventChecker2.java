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
import org.matsim.mobsim.jdeqsim.JDEQSimStarterWithoutController;
import org.matsim.mobsim.jdeqsim.SimulationParameters;
import org.matsim.mobsim.jdeqsim.util.DummyPopulationModifier;
import org.matsim.mobsim.jdeqsim.util.TestHandlerDetailedEventChecker;
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

import playground.wrashid.PDES2.Road;
import playground.wrashid.deqsim.PDESStarter2;


public class TestHandlerDetailedEventChecker2 extends TestHandlerDetailedEventChecker {

	protected HashMap<String, LinkedList<PersonEvent>> events = new HashMap<String, LinkedList<PersonEvent>>();
	public LinkedList<PersonEvent> allEvents = new LinkedList<PersonEvent>();
	private HashMap<String, ExpectedNumberOfEvents> expectedNumberOfMessages = new HashMap<String, ExpectedNumberOfEvents>();

	public TestHandlerDetailedEventChecker2() {

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
					expected.expectedLinkEnterEvents += ((CarRoute) leg
							.getRoute()).getLinks().size() + 1;
				}
			}

			expected.expectedArrivalEvents = expected.expectedDepartureEvents;
			expected.expectedLinkLeaveEvents = expected.expectedLinkEnterEvents;

			expectedNumberOfMessages.put(p.getId().toString(), expected);

			if (p.getId().toString().equalsIgnoreCase("225055")) {
				//printPlanPDES2(plan);
				//printPlanDES(plan);
			}

		}
	}

	private class ExpectedNumberOfEvents {
		public int expectedLinkEnterEvents;
		public int expectedLinkLeaveEvents;
		public int expectedDepartureEvents;
		public int expectedArrivalEvents;
	}

	
	private void printPlanPDES2(Plan plan) {
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
	
	private void printPlanDES(Plan plan) {
		LegIterator iter = plan.getIteratorLeg();
		while (iter.hasNext()) {
			Leg leg = (Leg) iter.next();
			for (Link link : ((CarRoute) leg.getRoute()).getLinks()) {
				System.out.print(link.getId()+ "-");
			}
			System.out.println();
		}
	}

	private void printEvents(String personId) {
		LinkedList<PersonEvent> list = events.get(personId);
		for (int i = 0; i < list.size(); i++) {
			System.out.println(list.get(i).toString());
		}
	}

}
