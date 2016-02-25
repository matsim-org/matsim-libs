/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.vehicles.Vehicle;
import org.matsim.vis.snapshotwriters.VisData;

/**
 * Essentially an interface, but since I do not want the methods public for the time being, it is incarnated as an abstract class.
 * <p/>
 * Interface for QLanes, which (can) make up the QLinks.
 * 
 * @author nagel
 *
 */
abstract class QLaneI {
	
	abstract void addFromWait( final QVehicle veh, final double now);

	abstract boolean isAcceptingFromWait();

	abstract void updateRemainingFlowCapacity();

	abstract boolean isActive();

	abstract double getSimulatedFlowCapacity();

	abstract void recalcTimeVariantAttributes(final double now);

	abstract QVehicle getVehicle( final Id<Vehicle> vehicleId);

	abstract double getStorageCapacity();

	abstract VisData getVisData();

	/**
	 * Needs to be added _upstream_ of the regular stop location so that a possible second stop on the link can also be served.
	 * <p/>
	 * Notes:<ul>
	 * <li> is not only used for transit and should thus be renamed.  kai, nov'14
	 * </ul>
	 */
	abstract void addTransitSlightlyUpstreamOfStop(final QVehicle veh);
	
	abstract void changeUnscaledFlowCapacityPerSecond( final double val, final double now ) ;

	abstract void changeEffectiveNumberOfLanes( final double val, final double now ) ;

	/**
	 * Seems ok as public interface function. kai, aug'15
	 */
	abstract boolean doSimStep(final double now);

	/**
	 * Seems ok as public interface function. kai, aug'15 
	 */
	abstract void clearVehicles();

	abstract Collection<MobsimVehicle> getAllVehicles();

	/**
	 * <br>
	 * seems ok as public interface function. kai, aug'15
	 */
	abstract void addFromUpstream(final QVehicle veh);

	abstract boolean isNotOfferingVehicle();

	abstract QVehicle popFirstVehicle();

	abstract QVehicle getFirstVehicle();

	abstract double getLastMovementTimeOfFirstVehicle();

	abstract boolean hasGreenForToLink(final Id<Link> toLinkId);

	abstract boolean isAcceptingFromUpstream();

}