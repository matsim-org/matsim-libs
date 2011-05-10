/* *********************************************************************** *
 * project: org.matsim.*
 * AgentTopology.java
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
package playground.thibautd.agentsmating.greedysavings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.thibautd.jointtripsoptimizer.population.JointLeg;

/**
 * Defines a topology an agents, based on the distances between home and work
 * locations.
 * 
 * @author thibautd
 */
public class AgentTopology {
	private static final Logger log =
		Logger.getLogger(AgentTopology.class);

	//private final static String HOME_REGEXP = "home.*";
	private final static String WORK_REGEXP = "work.*";

	private final QuadTree<Person> homeQuadTree;
	private final QuadTree<Person> workQuadTree;
	private double acceptableDistance;

	private final Map<Person, Tuple<Coord, Coord>> homeWorkLocations =
		new TreeMap<Person, Tuple<Coord, Coord>>();

	/*
	 * =========================================================================
	 * Constructor and related helpers
	 * =========================================================================
	 */
	public AgentTopology(
			final Network network,
			final Population population,
			final double acceptableDistance) {
		this.acceptableDistance = acceptableDistance;
		//this.network = network.getLinks();
		double maxX = Double.NEGATIVE_INFINITY;
		double minX = Double.POSITIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;

		Coord fromNode, toNode;
		
		//construct quadTree
		//use other thing than network (this is the only place where it is used)
		log.info("   constructing agent topology QuadTree...");
		for (Map.Entry<Id, ? extends Link> link :
				network.getLinks().entrySet()) {
			fromNode = link.getValue().getFromNode().getCoord();
			toNode = link.getValue().getToNode().getCoord();

			maxX = Math.max(fromNode.getX(), maxX);
			minX = Math.min(fromNode.getX(), minX);
			maxX = Math.max(toNode.getX(), maxX);
			minX = Math.min(toNode.getX(), minX);

			maxY = Math.max(fromNode.getY(), maxY);
			minY = Math.min(fromNode.getY(), minY);
			maxY = Math.max(toNode.getY(), maxY);
			minY = Math.min(toNode.getY(), minY);
		}

		this.homeQuadTree = new QuadTree<Person>(minX, minY, maxX, maxY);
		this.workQuadTree = new QuadTree<Person>(minX, minY, maxX, maxY);
		log.info("   constructing agent topology QT... DONE");
		log.info("   minX: "+minX+", minY: "+minY+", maxX: "+maxX+", maxY: "+maxY);

		//fill the quadTree
		this.fillQuadTree(population);
	}

	private void fillQuadTree(final Population population) {
		log.info("   filling QuadTree...");

		Tuple<Coord, Coord> homeWorkCoords;
		Coord coord;

		for (Person person : population.getPersons().values()) {
			homeWorkCoords = getHomeWorkLocations(person.getSelectedPlan());

			if ( !(homeWorkCoords == null) ) {
				coord = homeWorkCoords.getFirst();
				this.homeQuadTree.put(coord.getX(), coord.getY(), person);

				coord = homeWorkCoords.getSecond();
				this.workQuadTree.put(coord.getX(), coord.getY(), person);
			}
		}
		log.info("   filling QuadTree... DONE");
	}

	/**
	 * @return the coordinates of home and work locations if the first trip
	 * of the plan is a H-W car trip; null otherwise.
	 */
	private Tuple<Coord, Coord> getHomeWorkLocations(final Plan plan) {
		//TODO: identify all H-W trips, even if not first leg? (very improbable)
		List<PlanElement> planElements = plan.getPlanElements();

		if ( ((JointLeg) planElements.get(1)).getMode() != TransportMode.car) {
			return null;
		}

		Coord home = ((Activity) planElements.get(0)).getCoord();
		Coord work = null;
		Activity workAct = (Activity) planElements.get(2);

		if (workAct.getType().matches(WORK_REGEXP)) {
			work = workAct.getCoord();
		}

		return (work == null ?
				null :
				new Tuple<Coord, Coord>(home, work));
	}

	/*
	 * =========================================================================
	 * public methods
	 * =========================================================================
	 */

	/**
	 * @return a Tuple defining a pair (agent , saving value) for all "neighbors"
	 *
	 * @throws UnknownPersonException if the person is not part of the topology
	 */
	public List<Tuple<Person, Double>> getNeighbors(final Person person) {
		List<Tuple<Person, Double>> output = new ArrayList<Tuple<Person, Double>>();
		Tuple<Coord, Coord> homeWorkCoords = this.homeWorkLocations.get(person);
		Coord currentCoord;
		Collection<Person> homeNeighbors;
		Collection<Person> workNeighbors;

		if (homeWorkCoords == null) {
			throw new UnknownPersonException();
		}

		currentCoord = homeWorkCoords.getFirst();
		homeNeighbors = this.homeQuadTree.get(
				currentCoord.getX(),
				currentCoord.getY(),
				this.acceptableDistance);

		currentCoord = homeWorkCoords.getSecond();
		workNeighbors = this.workQuadTree.get(
				currentCoord.getX(),
				currentCoord.getY(),
				this.acceptableDistance);

		for (Person neighbor : homeNeighbors) {
			// assume the remove method is implemented.
			if (workNeighbors.remove(neighbor)) {
				output.add(
						new Tuple<Person, Double>(
							neighbor,
							computeSavings(homeWorkCoords, neighbor)));
			}
		}

		return output;
	}

	/**
	 * @return the savings value, based on the euclidean distance (not the
	 * network one).
	 */
	private double computeSavings(
			final Tuple<Coord, Coord> driverCoords,
			final Person passenger) {
		Coord homeDriver = driverCoords.getFirst();
		Coord workDriver = driverCoords.getSecond();
		Tuple<Coord, Coord> passengerCoords = this.homeWorkLocations.get(passenger);
		Coord homePassenger = passengerCoords.getFirst();
		Coord workPassenger = passengerCoords.getSecond();

		double saving = CoordUtils.calcDistance(homeDriver, workDriver);
		saving -= CoordUtils.calcDistance(homeDriver, homePassenger);
		saving -= CoordUtils.calcDistance(workDriver, workPassenger);

		return saving;
	}

	/**
	 * removes the agent from the topology.
	 */
	public void remove(Person person) {
		Tuple<Coord, Coord> homeWorkCoords = this.homeWorkLocations.get(person);

		Coord currentCoord = homeWorkCoords.getFirst();
		this.homeQuadTree.remove(currentCoord.getX(), currentCoord.getY(), person);

		currentCoord = homeWorkCoords.getSecond();
		this.workQuadTree.remove(currentCoord.getX(), currentCoord.getY(), person);

		this.homeWorkLocations.remove(person);
	}

	/*
	 * =========================================================================
	 * classes
	 * =========================================================================
	 */
	public class UnknownPersonException extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}
}

