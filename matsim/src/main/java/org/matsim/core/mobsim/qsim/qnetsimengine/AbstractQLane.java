/* *********************************************************************** *
 * project: org.matsim.*
 * QBufferItem
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
package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.matsim.api.core.v01.Id;


/**
 * Abstract class representing the functionality common for all Queue-Logic Links and Lanes, i.e.
 * providing selected, decorated methods for access and additional methods needed for
 * the logic implemented.
 * <p/>
 * Thoughts/comments:<ul>
 * <li> There is the "lane" functionality (e.g. "addToLane", "popFirst"), and the "link" 
 * functionality (e.g. "addToParking").  For the normal qsim, they are combined into
 * the QLinkImpl.  For the qsim with lanes, those are split into QLane and QLinkLanesImpl.
 * This has led to a lot of code replication, and some of the code has diverged 
 * (QLinkImpl is more modern with respect to pt and with respect to vehicle conservation).
 * kai, nov'11
 * <li> Triggered by a recent bug fix, I started moving some of the joint material up to the present class. kai, jun'13
 * </ul>
 * Please read the docu of QBufferItem, QLane, QLinkInternalI (arguably to be renamed
 * into something like AbstractQLink) and QLinkImpl jointly. kai, nov'11
 * 
 * @author dgrether
 */
abstract class AbstractQLane {
	
	/**
	 * The remaining integer part of the flow capacity available in one time step to move vehicles into the
	 * buffer. This value is updated each time step by a call to
	 * {@link #updateBufferCapacity(double)}.
	 */
	double remainingflowCap = 0.0;
	/**
	 * Stores the accumulated fractional parts of the flow capacity. See also
	 * flowCapFraction.
	 */
	double flowcap_accumulate = 1.0;
	/**
	 * true, i.e. green, if the link is not signalized
	 */
	boolean thisTimeStepGreen = true;
	double inverseFlowCapacityPerTimeStep;
	double flowCapacityPerTimeStepFractionalPart;
	/**
	 * The number of vehicles able to leave the buffer in one time step (usually 1s).
	 */
	double flowCapacityPerTimeStep;
	int bufferStorageCapacity;
	double usedBufferStorageCapacity = 0.0;


	/**
	 * upstream add
	 */
	abstract void addFromUpstream(final QVehicle veh);
	

	/**
	 * equivalent to a Buffer.isEmpty() operation
	 */
	abstract boolean isNotOfferingVehicle();

	/**
	 * equivalent to a Buffer.pop() operation
	 */
	abstract QVehicle popFirstVehicle();
	/**
	 * equivalent to a Buffer.peek() operation
	 */
	abstract QVehicle getFirstVehicle();

	abstract double getLastMovementTimeOfFirstVehicle();

	abstract boolean hasGreenForToLink(Id toLinkId);
	
	abstract boolean hasSpace();


	final void updateRemainingFlowCapacity() {
		this.remainingflowCap = this.flowCapacityPerTimeStep;
//				if (this.thisTimeStepGreen && this.flowcap_accumulate < 1.0 && this.hasBufferSpaceLeft()) {
		if (this.thisTimeStepGreen && this.flowcap_accumulate < 1.0 && this.isNotOfferingVehicle() ) {
			this.flowcap_accumulate += this.flowCapacityPerTimeStepFractionalPart;
		}
	}


	final boolean hasFlowCapacityLeftAndBufferSpace() {
		return (
				hasBufferSpaceLeft() 
				&& 
				((this.remainingflowCap >= 1.0) || (this.flowcap_accumulate >= 1.0))
				);
	}


	private boolean hasBufferSpaceLeft() {
		return usedBufferStorageCapacity < this.bufferStorageCapacity;
	}
	
}
