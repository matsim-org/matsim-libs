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

import java.io.File;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
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
import org.matsim.core.utils.collections.Tuple;
import org.matsim.testcases.MatsimTestUtils;

import playground.thibautd.jointtripsoptimizer.population.JointActingTypes;
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

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	// /////////////////////////////////////////////////////////////////////////
	// Fixtures preparation
	// /////////////////////////////////////////////////////////////////////////
	@Before
	public void initializePaths() {
		this.inputPath = getParentDirectory(utils.getPackageInputDirectory());
		this.outputPath = utils.getOutputDirectory();

		jdeqsimControler = JointControlerUtils.createControler(inputPath+"/config.xml");
		jdeqsimControler.getConfig().controler().setOutputDirectory(outputPath);
		jdeqsimControler.getConfig().controler().setMobsim("jdeqsim");
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
		PassengerListener listener = new PassengerListener();
		jdeqsimControler.getConfig().controler().setFirstIteration(0);
		jdeqsimControler.getConfig().controler().setLastIteration(0);
		jdeqsimControler.addControlerListener(listener);
		jdeqsimControler.run();

		Map<Id, ? extends Person> persons = jdeqsimControler.getPopulation().getPersons();
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
		public void reset(final int iteration) {
			// TODO Auto-generated method stub
			
		}

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


