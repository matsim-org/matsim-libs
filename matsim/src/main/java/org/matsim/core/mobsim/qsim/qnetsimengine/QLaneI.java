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
import org.matsim.api.core.v01.Identifiable;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.lanes.Lane;
import org.matsim.vehicles.Vehicle;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;


/**
 * Interface for QLanes, which (can) make up the QLinks.
 * 
 * @author nagel
 *
 */
public interface QLaneI extends Identifiable<Lane> {
	
	void addFromWait( final QVehicle veh);

	boolean isAcceptingFromWait(QVehicle veh);

	boolean isActive();

	double getSimulatedFlowCapacityPerTimeStep();
	
	void recalcTimeVariantAttributes();
	
	QVehicle getVehicle(final Id<Vehicle> vehicleId);

	double getStorageCapacity();
	
	static interface VisData {
		public Collection<AgentSnapshotInfo> addAgentSnapshotInfo(Collection<AgentSnapshotInfo> positions, double now ) ;
	}

	VisData getVisData();

	/**
	 * Needs to be added _upstream_ of the regular stop location so that a possible second stop on the link can also be served.
	 * <p></p>
	 * Notes:<ul>
	 * <li> is not only used for transit and should thus be renamed.  kai, nov'14
	 * </ul>
	 */
	void addTransitSlightlyUpstreamOfStop(final QVehicle veh);
	
	void changeUnscaledFlowCapacityPerSecond( final double val ) ;

	void changeEffectiveNumberOfLanes( final double val ) ;

	boolean doSimStep();

	void clearVehicles();

	Collection<MobsimVehicle> getAllVehicles();

	void addFromUpstream(final QVehicle veh);

	boolean isNotOfferingVehicle();

	QVehicle popFirstVehicle();

	QVehicle getFirstVehicle();

	double getLastMovementTimeOfFirstVehicle();

	boolean isAcceptingFromUpstream();

//	void changeSpeedMetersPerSecond(double val) ;
	// cannot be consistently implemented with current design, since it is not a parameter of the LinkSpeedCalculator.  kai, feb'18

	/**
	 * When multiple lanes lead to the same next link, the QLinkLanesImpl needs to decide which lane to use.  It uses
	 * the one with the smallest load.
	 */
	double getLoadIndicator() ;
	
	void initBeforeSimStep();
	// yyyy could you please explain why this here was added.  Why can't the same thing be done at the beginning of "doSimStep"?  kai, nov'18

}
