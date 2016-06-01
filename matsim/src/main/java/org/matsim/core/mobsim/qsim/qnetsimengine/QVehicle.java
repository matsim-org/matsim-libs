/* *********************************************************************** *
 * project: org.matsim.*
 * SimVehicle.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.DriverAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * The ``Q'' implementation of the MobsimVehicle.
 * <p/>
 * Design thoughts:<ul>
 * <li> This needs to be public since the ``Q'' version of the
 * vehicle is used by more than one package.  This interfaces should, however, not be used outside the relevant
 * netsimengines.  In particular, the information should not be used for visualization.  kai, nov'11
 * <li> Might be possible to make all methods in this hierarchy protected and just inherit to a QueueVehicle.  kai, nov'11
 * </ul>
 *
 * @author nagel
 */

public class QVehicle extends QItem implements MobsimVehicle {

	private static final Logger log = Logger.getLogger(QVehicle.class);

	private static int warnCount = 0;

	private double earliestLinkExitTime = 0;
	private DriverAgent driver = null;
	private Collection<PassengerAgent> passengers = null;
	private final Id<Vehicle> id;
	private Link currentLink = null;
	private final Vehicle vehicle;
	private final int passengerCapacity;

	public QVehicle(final Vehicle basicVehicle) {
		this.id = basicVehicle.getId();
		this.vehicle = basicVehicle;
		this.passengers = new ArrayList<>();

		VehicleCapacity capacity = basicVehicle.getType().getCapacity();
		if (capacity == null) {
			this.passengerCapacity = 4;
			if (warnCount < 10) {
				log.warn("No VehicleCapacity (= maximum number of passengers) set in Vehicle. "
						+ "Using default value of 4.  This is only a problem if you need vehicles with different "
						+ "capacities, e.g. for minibuses.");
				warnCount++;
				if ( warnCount == 10 ) {
					log.warn( Gbl.FUTURE_SUPPRESSED ) ;
				}
			}
		} else {
			// do *not* subtract one for the driver! Most pt vehicles define the capacity without the driver.
			// for private cars, think about if we should subtract one from the capacity if the driver is set?
			// But if we do, change the number of seats of the default vehicle from 4 to 5.
			this.passengerCapacity = capacity.getSeats() +
					(capacity.getStandingRoom() == null ? 0 : capacity.getStandingRoom());
		}
	}

	public void setCurrentLink(final Link link) {
		this.currentLink = link;
	}
	// yy not sure if this needs to be publicly exposed

	/**Design thoughts:<ul>
	 * <li> I am fairly sure that this should not be publicly exposed.  As far as I can tell, it is used in order to
	 * figure out of a visualizer should make a vehicle "green" or "red".  But green or red should be related to
	 * vehicle speed, and the mobsim should figure that out, not the visualizer.  So something like "getCurrentSpeed"
	 * seems to be a more useful option. kai, nov'11
	 * <li> The problem is not only the speed, but also the positioning of the vehicle in the "asQueue" plotting method.
	 * (Although, when thinking about it: Should be possible to obtain same result by using "getEarliestLinkExitTime()".
	 * But I am not sure if that would really be a conceptual improvement ... linkEnterTime is, after all, a much more
	 * "physical" quantity.)  kai, nov'11
	 * <li> But maybe it should then go into MobsimVehicle?  kai, nov'11
	 * <li> Also see comment under setLinkEnterTime().  kai, nov'11
	 * <li>removed this while refactoring the visualizer computations, dg jan'12</li>
	 * </ul>
	 */
//	public double getLinkEnterTime() {
//		return this.linkEnterTime;
//	}

	/**Design thoughts:<ul>
	 * <li> This has to remain public as long as QVehicle/QVehicleImpl is both used by QueueSimulation and QSim.  At best,
	 * we could say that there should also be a MobsimVehicle interface that does not expose this.  kai, nov'11.
	 * (This is there now.  kai, nov'11)
	 * <li>removed this while refactoring the visualizer computations, dg jan'12</li>
	 * </ul>
	 */
//	public void setLinkEnterTime(final double time) {
//		this.linkEnterTime = time;
//	}

	@Override
	public double getEarliestLinkExitTime() {
		return this.earliestLinkExitTime;
	}

	@Override
	public void setEarliestLinkExitTime(final double time) {
		this.earliestLinkExitTime = time;
	}

	@Override
	public Link getCurrentLink() {
		return this.currentLink;
	}

	@Override
	public MobsimDriverAgent getDriver() {
		if ( this.driver instanceof MobsimDriverAgent ) {
			return (MobsimDriverAgent) this.driver;
		} else if ( this.driver==null ) {
			return null ;
		} else {
			throw new RuntimeException( "error (downstream methods need to be made to accept DriverAgent)") ;
		}
	}

	public void setDriver(final DriverAgent driver) {
		if (driver != null) {
			if (this.driver != null && !this.driver.getId().equals(driver.getId())) {
				throw new RuntimeException( "A driver (" + this.driver.getId() +") " +
						"is already set in vehicle " + this.getId() + ". " +
						"Setting agent " + driver.getId().toString() + " is not possible!");
			}
		}
		// TODO: To make this check possible, we would need something like removeDriver().
//		else {
//			throw new RuntimeException( "Driver to be set in vehicle " + this.getId() +
//					" is null!");
//		}

		this.driver = driver;
	}

	@Override
	public Id<Vehicle> getId() {
		return this.id;
	}

	@Override
	public double getSizeInEquivalents() {
		return vehicle.getType().getPcuEquivalents();
	}

	@Override
	public Vehicle getVehicle() {
		return this.vehicle;
	}

	@Override
	public String toString() {
		return "Vehicle Id " + getId() + ", driven by (personId) " + this.driver.getId()
				+ ", on link " + this.currentLink.getId();
	}

	public double getMaximumVelocity() {
		return vehicle.getType().getMaximumVelocity();
	}

	@Override
	public Collection<? extends PassengerAgent> getPassengers() {
		return Collections.unmodifiableCollection(this.passengers);
	}

	@Override
	public boolean addPassenger(PassengerAgent passenger) {
		if (this.passengers.size() < this.passengerCapacity) {
			return this.passengers.add(passenger);
		}
		return false;
	}

	@Override
	public boolean removePassenger(PassengerAgent passenger) {
		return this.passengers.remove(passenger);
	}

	@Override
	public int getPassengerCapacity() {
		return this.passengerCapacity;
	}
}
