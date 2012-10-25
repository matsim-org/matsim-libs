/* *********************************************************************** *
 * project: org.matsim.*
 * TransitStopHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.qsim.pt;

import java.util.List;

import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * Provides a replaceable behavior for implementing different loading and unloading procedures
 * for transit vehicles.
 *
 * @author mrieser
 */
public interface TransitStopHandler {

	/**
	 * <p>Handles the loading and unloading for a transit vehicle. Given a list of passengers that
	 * want to either board or exit the vehicle, the method can define a behavior for the
	 * process of entering and leaving passengers. The method has to specifically call
	 * {@link PassengerAccessEgress#handlePassengerEntering(PTPassengerAgent, double)} and
	 * {@link PassengerAccessEgress#handlePassengerLeaving(PTPassengerAgent, double)} to let
	 * passengers in and out the vehicle. This allows implementations of this interface to
	 * define different strategies, e.g. a very simple one where everybody enters and leaves at
	 * the same time, or one where first all passengers leave, and at a later time all passengers
	 * enter, etc.</p>
	 * <p>The method must return the time how long the vehicle will have to stay at least at the stop
	 * to let the passengers enter/exit. If the method returns a value > 0, the method will be called
	 * again at a later time for the same stop, as in between some additional people could have arrived
	 * that want to enter. Only if the method returns 0, it may continue on its route to the next stop.
	 * </p>
	 *
	 * @param stop the stop the vehicle is currently at
	 * @param now the current time
	 * @param leavingPassengers the list of passengers that want to exit
	 * @param enteringPassengers the list of passengers that want to board
	 * @param handler provides methods for actually removing/adding passengers to the vehicle
	 * @return the time (in seconds) how long the vehicle will stay at least at this stop.
	 */
	public double handleTransitStop(final TransitStopFacility stop, final double now,
			final List<PTPassengerAgent> leavingPassengers, final List<PTPassengerAgent> enteringPassengers,
			final PassengerAccessEgress handler, MobsimVehicle vehicle);

}
