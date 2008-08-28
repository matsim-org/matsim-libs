/* *********************************************************************** *
 * project: org.matsim.*
 * Vehicle.java
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

package org.matsim.mobsim.queuesim;

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.network.Link;
import org.matsim.population.Leg;
import org.matsim.utils.vis.netvis.DrawableAgentI;

public class Vehicle implements DrawableAgentI {

//	private final static Logger log = Logger.getLogger(Vehicle.class);

	static private int globalID = 0;
	private double currentDepartureTime = 0;
	private double lastMovedTime = 0;

	private PersonAgent driver = null;

	private final Id id = new IdImpl(globalID++);

	public Vehicle() {}


	public double getDepartureTime_s() {
		return this.currentDepartureTime;
	}

	public void setDepartureTime_s(final double time) {
		this.currentDepartureTime = time;
	}

	/**
	 * @return Returns the currentLink.
	 */
	public Link getCurrentLink() {
		return this.driver.getCurrentLink();
	}

	public Leg getCurrentLeg() {
		return this.driver.getCurrentLeg();
	}

	/**
	 * @return Returns the driver.
	 */
	public PersonAgent getDriver() {
		return this.driver;
	}

	/**
	 * @param driver The driver to set.
	 */
	public void setDriver(final PersonAgent driver) {
		this.driver = driver;
	}

	/**
	 * @return Returns the Id of the vehicle.
	 */
	public Id getId() {
		return this.id;
	}

	/**
	 * @return Returns the time the vehicle moved last.
	 */
	public double getLastMovedTime() {
		return this.lastMovedTime;
	}

	/**
	 * @param lastMovedTime The lastMovedTime to set.
	 */
	public void setLastMovedTime(final double lastMovedTime) {
		this.lastMovedTime = lastMovedTime;
	}


	@Override
	public String toString() {
		return "Vehicle Id " + getId() + ", driven by (personId) " + this.driver.getPerson().getId()
				+ ", on link " + this.driver.getCurrentLink().getId() + ", routeindex: " + this.driver.getCurrentNodeIndex()
				+ ", next activity#: " + this.driver.getNextActivity();
	}



	public double getPosInLink_m() {

		double dur = this.driver.getCurrentLink().getFreespeedTravelTime(SimulationTimer.getTime());
		double mytime = getDepartureTime_s() - SimulationTimer.getTime();
		if (mytime<0) {
			mytime = 0.;
		}
		mytime/= dur;
		mytime = (1.-mytime)*this.driver.getCurrentLink().getLength();
		return mytime;
	}

	/** @return Always returns the value 1. */
	public int getLane() {
		return 1;
	}

}
