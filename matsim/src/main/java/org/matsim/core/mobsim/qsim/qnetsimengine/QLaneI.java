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
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.vis.snapshotwriters.VisData;

/**
 * Interface for the "QLanes" which make up the "QLinks".
 * <p/>
 * Design comments:<ul>
 * <li> This is now public as all java interfaces.  But it should not be possible to access the QLane from outside this package. kai, jun'13
 * <li> This has some commonalities with the QLink methods such as adding from upstream and removing to downstream.  This used to 
 * be standardized by AbstractQLane, but the interface cannot derive from the Abstract class, and I do not (yet) want to make the methods
 * in AbstractQLane public because then anyone who has access to the QLink can add vehicles which I do not want.  kai, jun'13
 * </ul> 
 * 
 * @author nagel
 *
 */
interface QLaneI {
	
	boolean doSimStep( final double now ) ;
	
	void addFromWait( final QVehicle veh, final double now);

	boolean isAcceptingFromWait();

	void updateRemainingFlowCapacity();

//	int vehInQueueCount();

	boolean isActive();

	double getSimulatedFlowCapacity();

	boolean isAcceptingFromUpstream();

	void recalcTimeVariantAttributes(final double now);

	QVehicle getVehicle( final Id vehicleId);

	Collection<MobsimVehicle> getAllVehicles();

	QVehicle popFirstVehicle();

	boolean hasGreenForToLink( final Id toLinkId);

	double getStorageCapacity();

	boolean isNotOfferingVehicle();

	void clearVehicles();

	void addFromUpstream(final QVehicle veh);

	VisData getVisData();

	QVehicle getFirstVehicle();

	double getLastMovementTimeOfFirstVehicle();

	/**
	 * Needs to be added _upstream_ of the regular stop location so that a possible second stop on the link can also be served.
	 */
	void addTransitSlightlyUpstreamOfStop(final QVehicle veh);

}