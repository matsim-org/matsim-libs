/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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

package org.matsim.contrib.parking.parkingproxy;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.contrib.parking.parkingproxy.AccessEgressFinder.LegActPair;

/**
 * <b>For some reason not working at the moment</b></br>
 * 
 * Tracks how many vehicles are in a given area at any time based on the selected Plans of the Persons
 * in the Population.</br>
 * 
 * The functionality of the {@linkplain PenaltyGenerator} interface are delegated to the {@linkplain MovingEntityCounter}
 * received in the constructor.
 * 
 * @author tkohl / Senozon
 *
 */
@Deprecated
class ParkingCounterByPlans implements IterationStartsListener, PenaltyGenerator {
	
	public static final String CARMODE = "car";	
	
	private final MovingEntityCounter carCounter;
	private final int carWeight;
	private final AccessEgressFinder egressFinder = new AccessEgressFinder(CARMODE);
	
	/**
	 * 
	 * @param carCounter
	 * @param carWeight
	 */
	public ParkingCounterByPlans(MovingEntityCounter carCounter, int carWeight) {
		this.carCounter = carCounter;
		this.carWeight = carWeight;
	}

	@Override
	public PenaltyCalculator generatePenaltyCalculator() {
		return carCounter.generatePenaltyCalculator();
	}

	@Override
	public void reset() {
		carCounter.reset();
	}

	/**
	 * Gets the current population from the event services and calls {@link #calculateByPopulation(Population)}.
	 */
	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		calculateByPopulation(event.getServices().getScenario().getPopulation(), event.getServices().getScenario().getNetwork());
	}
	
	/**
	 * Iterates over all plans and calls the {@linkplain MovingEntityCounter#handleArrival(int, double, double, int)}
	 * and {@linkplain MovingEntityCounter#handleDeparture(int, double, double, int)} functions whenever an
	 * arrival or departure is detected. More precisely, the functions are called whenever an egress leg
	 * after a car leg starts and whenever an access leg followed by a car leg ends. The Coordinate used is
	 * the to-coord of the link the activity is at which is consistent with how {@linkplain ParkingVehiclesCountEventHandler}
	 * currently does things.
	 */
	public void calculateByPopulation(Population pop, Network network) {
		for (Person p : pop.getPersons().values()) {
			for (LegActPair walkActPair : this.egressFinder.findEgressWalks(p.getSelectedPlan())) {
				carCounter.handleArrival(
						(int) walkActPair.leg.getDepartureTime().seconds(),
						network.getLinks().get(walkActPair.act.getLinkId()).getToNode().getCoord(),
						carWeight
						);
			}
			for (LegActPair walkActPair : this.egressFinder.findAccessWalks(p.getSelectedPlan())) {
				carCounter.handleDeparture(
						(int) (walkActPair.leg.getDepartureTime().seconds() + walkActPair.leg.getTravelTime().seconds()),
						network.getLinks().get(walkActPair.act.getLinkId()).getToNode().getCoord(),
						carWeight
						);
			}
		}
	}

}
