/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package scenarios.illustrative.parallel.createInput;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to create a population for the parallel scenario.
 * 
 * You can modify 
 * 	- the number of persons you like to simulate, 
 * 	- whether they should get an initial route and 
 *  - the score of the initial route.
 * 
 * @author gthunig
 */
public final class TtCreateParallelPopulation {
	
	private static final Logger log = Logger
			.getLogger(TtCreateParallelPopulation.class);

	private Population population;
	private Network network;

	private int numberOfPersons;
	private boolean initRoutes = false;
	private Double initPlanScore = null;

	public TtCreateParallelPopulation(Population pop, Network net) {
		this.population = pop;
		this.network = net;
	}

	/**
	 * Fills a population container with the given number of persons per OD
	 * Pair. All persons travel from all cardinal directions to the opposite
	 * cardinal direction.
	 * 
	 * All agents start at 8am.
	 * 
	 * @param numberOfPersons
	 *            the number of persons per OD pair
	 * @param initRoutes
	 *            flag that determines whether agents are initialized with or
	 *            without initial routes. If it is false, all agents are
	 *            initialized with no initial routes. If it is true, they are
	 *            initialized with both routes for their OD Pair, whereby every
	 *            second agent gets the first and every other agent the other
	 *            route as initial selected route.
	 * @param initPlanScore
	 *            initial score for all plans the persons will get. Use null for
	 *            no scores.
	 */
	public void createPersons(int numberOfPersons, boolean initRoutes, Double initPlanScore) {
		this.numberOfPersons = numberOfPersons;
		this.initRoutes = initRoutes;
		this.initPlanScore = initPlanScore;
		
		log.info("Create population ...");
		
		createWestEastDemand();
		createEastWestDemand();
		if (TtCreateParallelNetworkAndLanes.checkNetworkForSecondODPair(this.network)) {
			createNorthSouthDemand();
			createSouthNorthDemand();
		}
	}

	public void writePopFile(String pathToPopFile) {
		PopulationWriter popWriter = new PopulationWriter(population);
		popWriter.write(pathToPopFile);
	}

	public void writePopFileToDefaultPath() {
		writePopFile("../../runs-svn/parallel/");
	}

	private void createWestEastDemand() {
		for (int i = 0; i < this.numberOfPersons; i++) {

			// create a person
			Person person = population.getFactory().createPerson(
					Id.createPersonId(Integer.toString(i) + "_ab"));

			// create a start activity
			Activity startAct = population.getFactory()
					.createActivityFromLinkId("dummy", Id.createLinkId("a_1"));
			// distribute agents uniformly between 8 and 9 am.
			startAct.setEndTime(8 * 3600 + (double)(i)/numberOfPersons * 3600);

			// create a drain activity
			Activity drainAct = population.getFactory().createActivityFromLinkId(
					"dummy", Id.createLinkId("6_b"));

			if (initRoutes) {
				Leg leg = createWestEastLeg(true);
				Plan planNorth = createPlan(startAct, leg, drainAct, initPlanScore);
				leg = createWestEastLeg(false);
				Plan planSouth = createPlan(startAct, leg, drainAct, initPlanScore);
				person.addPlan(planNorth);
				person.addPlan(planSouth);
				if (i % 2 == 0) {
					person.setSelectedPlan(planNorth);
				} else {
					person.setSelectedPlan(planSouth);
				}
			} else {
				Leg leg = population.getFactory().createLeg(TransportMode.car);
				Plan plan = createPlan(startAct, leg, drainAct, initPlanScore);
				person.addPlan(plan);
			}

			// store information in population
			population.addPerson(person);
		}
	}

	private Leg createWestEastLeg(boolean takeNorthernPath) {
		Leg leg = population.getFactory().createLeg(TransportMode.car);

		List<Id<Link>> path = new ArrayList<>();
		path.add(Id.createLinkId("1_2"));
		if (takeNorthernPath) {
			path.add(Id.createLinkId("2_3"));
			path.add(Id.createLinkId("3_4"));
			path.add(Id.createLinkId("4_5"));
		} else {
			path.add(Id.createLinkId("2_7"));
			path.add(Id.createLinkId("7_8"));
			path.add(Id.createLinkId("8_5"));
		}

		path.add(Id.createLinkId("5_6"));

		Route route = new LinkNetworkRouteImpl(Id.createLinkId("a_1"), path, Id.createLinkId("6_b"));

		leg.setRoute(route);
		return leg;
	}

	private void createEastWestDemand() {
		for (int i = 0; i < this.numberOfPersons; i++) {

			// create a person
			Person person = population.getFactory().createPerson(
					Id.createPersonId(Integer.toString(i) + "_ba"));

			// create a start activity
			Activity startAct = population.getFactory()
					.createActivityFromLinkId("dummy", Id.createLinkId("b_6"));
			// distribute agents uniformly between 8 and 9 am.
			startAct.setEndTime(8 * 3600 + (double)(i)/numberOfPersons * 3600);

			// create a drain activity
			Activity drainAct = population.getFactory().createActivityFromLinkId(
					"dummy", Id.createLinkId("1_a"));

			if (initRoutes) {
				Leg leg = createEastWestLeg(true);
				Plan planNorth = createPlan(startAct, leg, drainAct, initPlanScore);
				leg = createEastWestLeg(false);
				Plan planSouth = createPlan(startAct, leg, drainAct, initPlanScore);
				person.addPlan(planNorth);
				person.addPlan(planSouth);
				if (i % 2 == 0) {
					person.setSelectedPlan(planNorth);
				} else {
					person.setSelectedPlan(planSouth);
				}
			} else {
				Leg leg = population.getFactory().createLeg(TransportMode.car);
				Plan plan = createPlan(startAct, leg, drainAct, initPlanScore);
				person.addPlan(plan);
			}

			// store information in population
			population.addPerson(person);
		}
	}

	private Leg createEastWestLeg(boolean takeNorthernPath) {
		Leg leg = population.getFactory().createLeg(TransportMode.car);

		List<Id<Link>> path = new ArrayList<>();
		path.add(Id.createLinkId("6_5"));
		if (takeNorthernPath) {
			path.add(Id.createLinkId("5_4"));
			path.add(Id.createLinkId("4_3"));
			path.add(Id.createLinkId("3_2"));
		} else {
			path.add(Id.createLinkId("5_8"));
			path.add(Id.createLinkId("8_7"));
			path.add(Id.createLinkId("7_2"));
		}
		path.add(Id.createLinkId("2_1"));

		Route route = new LinkNetworkRouteImpl(Id.createLinkId("b_6"), path, Id.createLinkId("1_a"));

		leg.setRoute(route);
		return leg;
	}

	private void createNorthSouthDemand() {
		for (int i = 0; i < this.numberOfPersons; i++) {

			// create a person
			Person person = population.getFactory().createPerson(
					Id.createPersonId(Integer.toString(i) + "_cd"));

			// create a start activity
			Activity startAct = population.getFactory()
					.createActivityFromLinkId("dummy", Id.createLinkId("c_9"));
			// distribute agents uniformly between 8 and 9 am.
			startAct.setEndTime(8 * 3600 + (double)(i)/numberOfPersons * 3600);

			// create a drain activity
			Activity drainAct = population.getFactory().createActivityFromLinkId(
					"dummy", Id.createLinkId("12_d"));

			if (initRoutes) {
				Leg leg = createNorthSouthLeg(true);
				Plan planWest = createPlan(startAct, leg, drainAct, initPlanScore);
				leg = createNorthSouthLeg(false);
				Plan planEast = createPlan(startAct, leg, drainAct, initPlanScore);
				person.addPlan(planWest);
				person.addPlan(planEast);
				if (i % 2 == 0) {
					person.setSelectedPlan(planWest);
				} else {
					person.setSelectedPlan(planEast);
				}
			} else {
				Leg leg = population.getFactory().createLeg(TransportMode.car);
				Plan plan = createPlan(startAct, leg, drainAct, initPlanScore);
				person.addPlan(plan);
			}

			// store information in population
			population.addPerson(person);
		}
	}

	private Leg createNorthSouthLeg(boolean takeWesternPath) {
		Leg leg = population.getFactory().createLeg(TransportMode.car);

		List<Id<Link>> path = new ArrayList<>();
		path.add(Id.createLinkId("9_10"));
		if (takeWesternPath) {
			path.add(Id.createLinkId("10_3"));
			path.add(Id.createLinkId("3_7"));
			path.add(Id.createLinkId("7_11"));
		} else {
			path.add(Id.createLinkId("10_4"));
			path.add(Id.createLinkId("4_8"));
			path.add(Id.createLinkId("8_11"));
		}
		path.add(Id.createLinkId("11_12"));

		Route route = new LinkNetworkRouteImpl(Id.createLinkId("c_9"), path, Id.createLinkId("12_d"));

		leg.setRoute(route);
		return leg;
	}

	private void createSouthNorthDemand() {
		for (int i = 0; i < this.numberOfPersons; i++) {

			// create a person
			Person person = population.getFactory().createPerson(
					Id.createPersonId(Integer.toString(i) + "_dc"));

			// create a start activity
			Activity startAct = population.getFactory()
					.createActivityFromLinkId("dummy", Id.createLinkId("d_12"));
			// distribute agents uniformly between 8 and 9 am.
			startAct.setEndTime(8 * 3600 + (double)(i)/numberOfPersons * 3600);

			// create a drain activity
			Activity drainAct = population.getFactory().createActivityFromLinkId(
					"dummy", Id.createLinkId("9_c"));

			if (initRoutes) {
				Leg leg = createSouthNorthLeg(true);
				Plan planWest = createPlan(startAct, leg, drainAct, initPlanScore);
				leg = createSouthNorthLeg(false);
				Plan planEast = createPlan(startAct, leg, drainAct, initPlanScore);
				person.addPlan(planWest);
				person.addPlan(planEast);
				if (i % 2 == 0) {
					person.setSelectedPlan(planWest);
				} else {
					person.setSelectedPlan(planEast);
				}
			} else {
				Leg leg = population.getFactory().createLeg(TransportMode.car);
				Plan plan = createPlan(startAct, leg, drainAct, initPlanScore);
				person.addPlan(plan);
			}

			// store information in population
			population.addPerson(person);
		}
	}

	private Leg createSouthNorthLeg(boolean takeWesternPath) {
		Leg leg = population.getFactory().createLeg(TransportMode.car);

		List<Id<Link>> path = new ArrayList<>();
		path.add(Id.createLinkId("12_11"));
		if (takeWesternPath) {
			path.add(Id.createLinkId("11_7"));
			path.add(Id.createLinkId("7_3"));
			path.add(Id.createLinkId("3_10"));
		} else {
			path.add(Id.createLinkId("11_8"));
			path.add(Id.createLinkId("8_4"));
			path.add(Id.createLinkId("4_10"));
		}
		path.add(Id.createLinkId("10_9"));

		Route route = new LinkNetworkRouteImpl(Id.createLinkId("d_12"), path, Id.createLinkId("9_c"));

		leg.setRoute(route);
		return leg;
	}

	private Plan createPlan(Activity startAct, Leg leg, Activity drainAct,
			Double initPlanScore) {
		
		Plan plan = population.getFactory().createPlan();

		plan.addActivity(startAct);
		plan.addLeg(leg);
		plan.addActivity(drainAct);
		plan.setScore(initPlanScore);
		
		return plan;
	}
}
