package playground.wrashid.util;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.basic.v01.population.BasicPlanElement;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.NetworkRoute;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.PlanElement;
import org.matsim.core.api.population.Population;
import org.matsim.core.events.PersonEvent;
import org.matsim.core.mobsim.jdeqsim.util.DummyPopulationModifier;
import org.matsim.core.mobsim.jdeqsim.util.TestHandlerDetailedEventChecker;
import org.matsim.core.mobsim.jdeqsim.util.testable.PopulationModifier;

import playground.wrashid.PDES2.Road;
import playground.wrashid.PDES2.TestHandler;
import playground.wrashid.deqsim.PDESStarter2;


public class TestHandlerDetailedEventChecker2 extends TestHandlerDetailedEventChecker implements TestHandler {

	protected HashMap<String, LinkedList<PersonEvent>> events = new HashMap<String, LinkedList<PersonEvent>>();
	public LinkedList<PersonEvent> allEvents = new LinkedList<PersonEvent>();
	private HashMap<String, ExpectedNumberOfEvents> expectedNumberOfMessages = new HashMap<String, ExpectedNumberOfEvents>();

	Population population = null;

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

	public void checkAssertions() {
		super.checkAssertions(this.population);
	}
	
	public void calculateExpectedNumberOfEvents(Population population) {
		this.population = population;
		for (Person p : population.getPersons().values()) {
			Plan plan = p.getSelectedPlan();
			ExpectedNumberOfEvents expected = new ExpectedNumberOfEvents();
			List<? extends BasicPlanElement> actsLegs = plan.getPlanElements();
			expected.expectedDepartureEvents += actsLegs.size() / 2;

			for (BasicPlanElement pe : actsLegs) {
				if (pe instanceof Leg) {
					Leg leg = (Leg) pe;
					// at the moment only cars are simulated on the road
					if (leg.getMode().equals(TransportMode.car)) {
						expected.expectedLinkEnterEvents += ((NetworkRoute) leg.getRoute()).getLinks().size() + 1;
					}
				}
			}

			expected.expectedArrivalEvents = expected.expectedDepartureEvents;
			expected.expectedLinkLeaveEvents = expected.expectedLinkEnterEvents;

			expectedNumberOfMessages.put(p.getId().toString(), expected);

//			if (p.getId().toString().equalsIgnoreCase("225055")) {
				//printPlanPDES2(plan);
				//printPlanDES(plan);
//			}

		}
	}

	private class ExpectedNumberOfEvents {
		public int expectedLinkEnterEvents;
		public int expectedLinkLeaveEvents;
		public int expectedDepartureEvents;
		public int expectedArrivalEvents;
	}

	
	private void printPlanPDES2(Plan plan) {
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Leg) {
				Leg leg = (Leg) pe;
				for (Link link : ((NetworkRoute) leg.getRoute()).getLinks()) {
					System.out.print(link.getId()
							+ "("
							+ Road.allRoads.get(link.getId().toString())
							.getZoneId() + ")" + "-");
				}
				System.out.println();
			}
		}
	}
	
	private void printPlanDES(Plan plan) {
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Leg) {
				Leg leg = (Leg) pe;
				for (Link link : ((NetworkRoute) leg.getRoute()).getLinks()) {
					System.out.print(link.getId()+ "-");
				}
				System.out.println();
			}
		}
	}

	private void printEvents(String personId) {
		LinkedList<PersonEvent> list = events.get(personId);
		for (int i = 0; i < list.size(); i++) {
			System.out.println(list.get(i).toString());
		}
	}

}
