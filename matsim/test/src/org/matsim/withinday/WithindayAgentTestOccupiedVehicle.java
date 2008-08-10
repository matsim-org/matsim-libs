/* *********************************************************************** *
 * project: org.matsim.*
 * WithindayAgentTestOccupiedVehicle.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.withinday;

import java.util.ArrayList;

import org.matsim.mobsim.queuesim.SimulationTimer;
import org.matsim.network.Link;
import org.matsim.network.Node;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.withinday.mobsim.OccupiedVehicle;

/**
 * @author dgrether
 *
 */
public class WithindayAgentTestOccupiedVehicle extends OccupiedVehicle {

	private final Link destinationLink;

	public WithindayAgentTestOccupiedVehicle(final Leg currentLeg, final Link currentLink, final Link destinationLink, final ArrayList<Object> arrayList) {
		this.currentLeg = currentLeg;
		this.currentLink = currentLink;
		this.destinationLink = destinationLink;
		this.actslegs = arrayList;
	}

	/**
	 * @see org.matsim.mobsim.queuesim.Vehicle#getCurrentLeg()
	 */
	@Override
	public Leg getCurrentLeg() {
		return (Leg) this.currentLeg;
	}

	/**
	 * @see org.matsim.mobsim.queuesim.Vehicle#getDestinationLink()
	 */
	@Override
	public Link getDestinationLink() {
		return this.destinationLink;
	}

	/**
	 * @see org.matsim.withinday.mobsim.OccupiedVehicle#exchangeActsLegs(java.util.ArrayList)
	 */
	@Override
	public void exchangeActsLegs(final ArrayList<Object> actslegs) {
		super.exchangeActsLegs(actslegs);
	}

	@Override
	public Node getCurrentNode() {
		return super.getCurrentNode();
	}

	/**
	 * @see org.matsim.mobsim.queuesim.Vehicle#getDepartureTime_s()
	 */
	@Override
	public double getDepartureTime_s() {
		return SimulationTimer.getTime();
	}


	//############################################################
	//for security reasons overwritten but not implemented methods

	@Override
	public Link chooseNextLink() {
		throw new UnsupportedOperationException("This method should not be used for this test!");
	}

	/**
	 * @see org.matsim.mobsim.queuesim.Vehicle#getDriver()
	 */
	@Override
	public Person getDriver() {
		throw new UnsupportedOperationException("This method should not be used for this test!");
	}

	/**
	 * @see org.matsim.mobsim.queuesim.Vehicle#getID()
	 */
	@Override
	public int getID() {
		throw new UnsupportedOperationException("This method should not be used for this test!");
	}

	/**
	 * @see org.matsim.mobsim.queuesim.Vehicle#getLane()
	 */
	@Override
	public int getLane() {
		throw new UnsupportedOperationException("This method should not be used for this test!");
	}

	/**
	 * @see org.matsim.mobsim.queuesim.Vehicle#getLastMovedTime()
	 */
	@Override
	public double getLastMovedTime() {
		throw new UnsupportedOperationException("This method should not be used for this test!");
	}

	/**
	 * @see org.matsim.mobsim.queuesim.Vehicle#getPosInLink_m()
	 */
	@Override
	public double getPosInLink_m() {
		throw new UnsupportedOperationException("This method should not be used for this test!");
	}

	/**
	 * @see org.matsim.mobsim.queuesim.Vehicle#incCurrentNode()
	 */
	@Override
	public void incCurrentNode() {
		throw new UnsupportedOperationException("This method should not be used for this test!");
	}

	/**
	 * @see org.matsim.mobsim.queuesim.Vehicle#initVeh()
	 */
	@Override
	public boolean initVeh() {
		throw new UnsupportedOperationException("This method should not be used for this test!");
	}

	/**
	 * @see org.matsim.mobsim.queuesim.Vehicle#leaveActivity()
	 */
	@Override
	public void leaveActivity(final double now) {
		throw new UnsupportedOperationException("This method should not be used for this test!");
	}




}
