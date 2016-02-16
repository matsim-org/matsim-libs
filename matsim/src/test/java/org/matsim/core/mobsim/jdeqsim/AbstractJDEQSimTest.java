package org.matsim.core.mobsim.jdeqsim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.mobsim.jdeqsim.util.CppEventFileParser;
import org.matsim.core.mobsim.jdeqsim.util.EventLibrary;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.vehicles.Vehicle;

public abstract class AbstractJDEQSimTest extends MatsimTestCase {

	protected Map<Id<Vehicle>, Id<Person>> vehicleToDriver = null;
	protected Map<Id<Person>, List<Event>> eventsByPerson = null;
	public LinkedList<Event> allEvents = null;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.eventsByPerson = new HashMap<Id<Person>, List<Event>>();
		this.vehicleToDriver = new HashMap<>();
		this.allEvents = new LinkedList<Event>();
	}

	@Override
	protected void tearDown() throws Exception {
		this.eventsByPerson = null;
		this.vehicleToDriver = null;
		this.allEvents = null;
		Road.getAllRoads().clear(); // SimulationParameter contains a Map containing Links which refer to the Network, give that free for GC
		super.tearDown();
	}

	public void runJDEQSim(Scenario scenario) {
		EventsManagerImpl events = new EventsManagerImpl();
		events.addHandler(new PersonEventCollector());
		events.initProcessing();
		new JDEQSimulation(ConfigUtils.addOrGetModule(scenario.getConfig(), JDEQSimConfigGroup.NAME, JDEQSimConfigGroup.class), scenario, events).run();
		events.finishProcessing();
	}

	protected void checkAscendingTimeStamps() {
		// all events of one agent must have ascending time stamps
		double lastTimeStamp;
		for (List<Event> list : eventsByPerson.values()) {
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
	}

	/**
	 * Compares plan and events for each agent.
	 * Checks the type of the event and the linkId.
	 */
	protected void checkEventsCorrespondToPlans(final Population population) {
		for (Entry<Id<Person>, List<Event>> entry : eventsByPerson.entrySet()) {
			List<Event> list = entry.getValue();
			Person p = population.getPersons().get(entry.getKey());
			// printEvents(list.get(0).agentId);
			Plan plan = p.getSelectedPlan();
			int index = 0;

			Activity act = null;
			Leg leg = null;
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					act = (Activity) pe;

					if (leg != null) {
						// each leg ends with enter on act link
						// => only for non empty car legs and non-cars legs this
						// statement is true
						if (leg.getMode().equals(TransportMode.car) && ((NetworkRoute) leg.getRoute()).getLinkIds().size() > 0) {
							assertTrue(list.get(index) instanceof LinkEnterEvent);
							assertTrue(act.getLinkId().toString().equalsIgnoreCase(
									((LinkEnterEvent) list.get(index)).getLinkId().toString()));
							index++;
						}

						// each leg ends with arrival on act link
						assertTrue(list.get(index) instanceof PersonArrivalEvent);
						assertTrue(act.getLinkId().toString().equalsIgnoreCase(
								((PersonArrivalEvent) list.get(index)).getLinkId().toString()));
						index++;

						// each leg ends with arrival on act link
						assertTrue(list.get(index) instanceof ActivityStartEvent);
						assertEquals(act.getLinkId(), ((ActivityStartEvent) list.get(index)).getLinkId());
						index++;
					}
				} else if (pe instanceof Leg) {
					leg = (Leg) pe;

					// act end event
					assertTrue(list.get(index) instanceof ActivityEndEvent);
					assertEquals(act.getLinkId(), ((ActivityEndEvent) list.get(index)).getLinkId());
					index++;

					// each leg starts with departure on act link
					assertTrue(list.get(index) instanceof PersonDepartureEvent);
					assertTrue(act.getLinkId().toString().equalsIgnoreCase(
							((PersonDepartureEvent) list.get(index)).getLinkId().toString()));
					index++;

					// each CAR leg must enter/leave act link
					if (leg.getMode().equals(TransportMode.car)) {

						// if car leg contains empty route, then this check is
						// not applicable
						if (((NetworkRoute) leg.getRoute()).getLinkIds().size() > 0) {
							// the first LinkEnterEvent is a AgentWait2LinkEvent
							assertTrue(list.get(index) instanceof VehicleEntersTrafficEvent);
							assertTrue(act.getLinkId().toString().equalsIgnoreCase(
									((VehicleEntersTrafficEvent) list.get(index)).getLinkId().toString()));
							index++;

							assertTrue(list.get(index) instanceof LinkLeaveEvent);
							assertTrue(act.getLinkId().toString().equalsIgnoreCase(
									((LinkLeaveEvent) list.get(index)).getLinkId().toString()));
							index++;
						}

						for (Id<Link> linkId : ((NetworkRoute) leg.getRoute()).getLinkIds()) {
							// enter link and leave each link on route
							assertTrue(list.get(index) instanceof LinkEnterEvent);
							assertTrue(linkId.equals(	((LinkEnterEvent) list.get(index)).getLinkId()) );
							index++;

							assertTrue(list.get(index) instanceof LinkLeaveEvent);
							assertTrue(linkId.equals( ((LinkLeaveEvent) list.get(index)).getLinkId()));
							index++;
						}
					}

				}
			}
		}
	}

	/**
	 * Compare events to deq event file. The order of events must also be the
	 * same. (this test will only succeed for simple tests with one car
	 * often!!!) => reason: at junctions the order of cars can change + stuck
	 * vehicles are dealt with in different ways
	 */
	protected void compareToDEQSimEvents(final String deqsimEventsFile) {
 		LinkedList<Event> copyEventList=new LinkedList<Event>();

 		// remove ActStartEvent and ActEndEvent, because this does not exist in
		// c++ DEQSim
 		for (int i=0;i<allEvents.size();i++){
	 		if (!(allEvents.get(i) instanceof ActivityStartEvent || allEvents.get(i) instanceof ActivityEndEvent)){
				copyEventList.add(allEvents.get(i));
			}
 		}

		ArrayList<EventLog> deqSimLog=CppEventFileParser.parseFile(deqsimEventsFile);
		for (int i=0;i<copyEventList.size();i++){
			assertTrue("events not equal.", CppEventFileParser.equals(copyEventList.get(i), deqSimLog.get(i)));
		}
	}

	/**
	 * Compares the sum of all travel times with the sum of all travel times generated by the C++DEQSim.
	 * As {@link #compareToDEQSimEvents(String)} does not function for most comparisons of the JavaDEQSim and C++DEQSim model,
	 * we need to compare the time each car was on the road and take its average. This figure should with in a small interval
	 * for both simulations.
	 * Attention: Still when vehicles are stuck, this comparison can be off by larger number, because unstucking the vehicles is
	 * done in different ways by the two simulations
	 */
	protected void compareToDEQSimTravelTimes(final String deqsimEventsFile, final double tolerancePercentValue) {
		ArrayList<EventLog> deqSimLog = CppEventFileParser.parseFile(deqsimEventsFile);

		double deqSimTravelSum=EventLog.getSumTravelTime(deqSimLog);
		double javaSimTravelSum=EventLibrary.getSumTravelTime(allEvents);
		assertTrue ((Math.abs(deqSimTravelSum - javaSimTravelSum)/deqSimTravelSum) < tolerancePercentValue);
	}


	private class PersonEventCollector implements ActivityStartEventHandler, ActivityEndEventHandler, LinkEnterEventHandler, 
			LinkLeaveEventHandler, PersonDepartureEventHandler, PersonArrivalEventHandler, VehicleEntersTrafficEventHandler {

		@Override
		public void reset(int iteration) {
		}

		@Override
		public void handleEvent(VehicleEntersTrafficEvent event) {
			// save drivers
			vehicleToDriver.put(event.getVehicleId(), event.getPersonId());
			
			if (!eventsByPerson.containsKey(event.getPersonId())) {
				eventsByPerson.put(event.getPersonId(), new LinkedList<Event>());
			}
			eventsByPerson.get(event.getPersonId()).add(event);
			allEvents.add(event);
		}

		@Override
		public void handleEvent(PersonArrivalEvent event) {
			if (!eventsByPerson.containsKey(event.getPersonId())) {
				eventsByPerson.put(event.getPersonId(), new LinkedList<Event>());
			}
			eventsByPerson.get(event.getPersonId()).add(event);
			allEvents.add(event);
		}

		@Override
		public void handleEvent(PersonDepartureEvent event) {
			if (!eventsByPerson.containsKey(event.getPersonId())) {
				eventsByPerson.put(event.getPersonId(), new LinkedList<Event>());
			}
			eventsByPerson.get(event.getPersonId()).add(event);
			allEvents.add(event);
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			Id<Person> driverId = vehicleToDriver.get(event.getVehicleId());
			if (!eventsByPerson.containsKey(driverId)) {
				eventsByPerson.put(driverId, new LinkedList<Event>());
			}
			eventsByPerson.get(driverId).add(event);
			
			allEvents.add(event);
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			Id<Person> driverId = vehicleToDriver.get(event.getVehicleId());
			if (!eventsByPerson.containsKey(driverId)) {
				eventsByPerson.put(driverId, new LinkedList<Event>());
			}
			eventsByPerson.get(driverId).add(event);
			
			allEvents.add(event);
		}

		@Override
		public void handleEvent(ActivityEndEvent event) {
			if (!eventsByPerson.containsKey(event.getPersonId())) {
				eventsByPerson.put(event.getPersonId(), new LinkedList<Event>());
			}
			eventsByPerson.get(event.getPersonId()).add(event);
			allEvents.add(event);
		}

		@Override
		public void handleEvent(ActivityStartEvent event) {
			if (!eventsByPerson.containsKey(event.getPersonId())) {
				eventsByPerson.put(event.getPersonId(), new LinkedList<Event>());
			}
			eventsByPerson.get(event.getPersonId()).add(event);
			allEvents.add(event);
		}
	}



}
