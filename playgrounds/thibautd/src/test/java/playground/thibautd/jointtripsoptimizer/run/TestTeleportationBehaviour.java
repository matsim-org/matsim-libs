/* *********************************************************************** *
 * project: org.matsim.*
 * TestTeleportationBehaviour.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.jointtripsoptimizer.run;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.testcases.MatsimTestUtils;

import playground.thibautd.jointtripsoptimizer.population.JointActingTypes;
import playground.thibautd.jointtripsoptimizer.population.JointLeg;
import playground.thibautd.jointtripsoptimizer.utils.JointControlerUtils;

/**
 * Tests the teleportation behaviour of car_passenger mode.
 *
 * For the moment, only JDEQSim tested: this mobsim may anyway be the only one
 * compatible with the approach.
 *
 * @author thibautd
 */
public class TestTeleportationBehaviour {
	private String inputPath;
	private String outputPath;
	private Controler jdeqsimControler;
	private Controler qsimControler;

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	// /////////////////////////////////////////////////////////////////////////
	// Fixtures preparation
	// /////////////////////////////////////////////////////////////////////////
	@Before
	public void initializePaths() {
		this.inputPath = getParentDirectory(utils.getPackageInputDirectory());
		this.outputPath = utils.getOutputDirectory();

		jdeqsimControler = JointControlerUtils.createControler(inputPath+"/config-jdeqsim.xml");
		jdeqsimControler.getConfig().controler().setOutputDirectory(outputPath);
		jdeqsimControler.getConfig().controler().setMobsim("jdeqsim");

		qsimControler = JointControlerUtils.createControler(inputPath+"/config-qsim.xml");
		qsimControler.getConfig().controler().setOutputDirectory(outputPath);
		qsimControler.getConfig().controler().setMobsim("qsim");
	}

	private String getParentDirectory(final String path) {
		String[] pathArray = path.split("/");
		String output = "";

		for (int i=0; i < pathArray.length - 1; i++) {
			output += pathArray[i] + "/";
		}

		return output;
	}

	//@After
	//public void clearOutputFiles() {
	//}

	// /////////////////////////////////////////////////////////////////////////
	// Test methods
	// /////////////////////////////////////////////////////////////////////////
	
	/**
	 * Tests whether the passengers are teleported accoding to the leg travel
	 * time when using JDEQSim
	 */
	@Test
	public void testJDEQSimTeleportationTime() {
		testTeleportationTime(jdeqsimControler);
	}

	/**
	 * Tests whether the passengers are teleported accoding to the leg travel
	 * time when using QSim
	 */
	@Test
	public void testQSimTeleportationTime() {
		testTeleportationTime(qsimControler);
	}

	public void testTeleportationTime(final Controler controler) {
		PassengerListener listener = new PassengerListener();
		controler.getConfig().controler().setFirstIteration(0);
		controler.getConfig().controler().setLastIteration(0);
		controler.addControlerListener(listener);
		controler.run();

		Map<Id, ? extends Person> persons = controler.getPopulation().getPersons();
		double duration;
		double expectedDuration;
	
		for (Map.Entry<Id, Tuple<Double, Double>> times :
				listener.getTimes().entrySet()) {
			duration = times.getValue().getSecond() - times.getValue().getFirst();
			expectedDuration = getFirstPassengerLegDuration(persons.get(times.getKey()));

			Assert.assertEquals(
					"teleportation time different from leg travel time",
					expectedDuration,
					duration,
					MatsimTestUtils.EPSILON);
		}
	}

	private double getFirstPassengerLegDuration(final Person person) {
		List<PlanElement> pes = person.getSelectedPlan().getPlanElements();

		for (PlanElement pe : pes) {
			if ((pe instanceof Leg) && (((Leg) pe).getMode().equals(JointActingTypes.PASSENGER))) {
				return ((Leg) pe).getTravelTime();
				//return ((Leg) pe).getRoute().getTravelTime();
			}
		}
		return Double.NaN;
	}

	/**
	 * tests the "relationships" between the driver and the passenger routes:
	 * <ul>
	 * <li> they must reference the same instance
	 * <li> they must be identical
	 * </ul>
	 */
	@Test
	public void testPassengerDriverRouteRelationship() {
		jdeqsimControler.getConfig().controler().setFirstIteration(0);
		jdeqsimControler.getConfig().controler().setLastIteration(0);
		jdeqsimControler.run();

		NetworkRoute driverRoute;
		NetworkRoute passengerRoute;

		for (Person person : jdeqsimControler.getPopulation().getPersons().values()) {
			for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
				if ((pe instanceof Leg) && (((JointLeg) pe).getIsDriver())) {
					driverRoute = (NetworkRoute) ((JointLeg) pe).getRoute();

					for (JointLeg passengerLeg : ((JointLeg) pe).getLinkedElements().values()) {
						passengerRoute = (NetworkRoute) passengerLeg.getRoute();
						assertRoutesAreCorrect(passengerRoute, driverRoute);
					}
				}
			}
		}
	}

	private void assertRoutesAreCorrect(
			final NetworkRoute passengerRoute,
			final NetworkRoute driverRoute) {
		Assert.assertNotSame(
				"the driver and passenger route point toward the same instance!",
				passengerRoute,
				driverRoute);

		Assert.assertEquals(
				"the driver and passenger route do not have the same durations",
				passengerRoute.getTravelTime(),
				driverRoute.getTravelTime(),
				MatsimTestUtils.EPSILON);
		
		Assert.assertEquals(
				"the driver and the passenger rout do not have the same origin",
				passengerRoute.getStartLinkId(),
				driverRoute.getStartLinkId());

		// is it necessary?
		Assert.assertEquals(
				"the driver and the passenger rout do not have the same path",
				passengerRoute.getLinkIds(),
				driverRoute.getLinkIds());

		Assert.assertEquals(
				"the driver and the passenger rout do not have the same destination",
				passengerRoute.getEndLinkId(),
				driverRoute.getEndLinkId());
	}

	// /////////////////////////////////////////////////////////////////////////
	// helpers
	// ////////////////////////////////////////////////////////////////////////
	// only handles the first car passenger leg.
	private class PassengerListener implements
			StartupListener,
			AgentDepartureEventHandler,
			AgentArrivalEventHandler {
		private final Map<Id, Tuple<Double, Double>> passengerTimes =
			new HashMap<Id, Tuple<Double, Double>>();
		private final Map<Id, Double> departures = new HashMap<Id, Double>();

		@Override
		public void notifyStartup(final StartupEvent event) {
			event.getControler().getEvents().addHandler(this);
		}

		@Override
		public void reset(final int iteration) {}

		@Override
		public void handleEvent(final AgentDepartureEvent event) {
			if (event.getLegMode().equals(JointActingTypes.PASSENGER)) {
				this.departures.put(event.getPersonId(), event.getTime());
			}
		}

		@Override
		public void handleEvent(final AgentArrivalEvent event) {
			Id id = event.getPersonId();
			if (!this.passengerTimes.containsKey(id) &&
					event.getLegMode().equals(JointActingTypes.PASSENGER)) {
				this.passengerTimes.put(
						id,
						new Tuple<Double, Double>(
							departures.get(id),
							event.getTime()));
			}
		}

		public Map<Id, Tuple<Double, Double>> getTimes() {
			return this.passengerTimes;
		}
	}
}


