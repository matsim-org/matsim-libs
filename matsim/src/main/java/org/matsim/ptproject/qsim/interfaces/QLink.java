/* *********************************************************************** *
 * project: org.matsim.*
 * QueueLink
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
package org.matsim.ptproject.qsim.interfaces;

import java.util.Collection;
import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.ptproject.qsim.netsimengine.QNode;
import org.matsim.vis.snapshots.writers.VisData;
import org.matsim.vis.snapshots.writers.VisLink;

public interface QLink extends VisLink {

	// ######################
	// to clarify:
	
//	public QSimEngine getQSimEngine();
//	// yyyy nearly all uses of this are within the package --???
//	// The useage outside the package seems in order to get to the qsim. Might make sense to provide this directly:
//	// even with the parallel qsim, this is still a well-defined operation. kai, aug'10

	/**
	 * @deprecated
	 */
	@Deprecated // yyyyyy I would say that this should not be accessible since it exposes internal structure
	// which should not be necessary outside.  kai, may'10
	public LinkedList<QVehicle> getVehQueue();

	
	//	######################
	// not so good, but no idea:
	
	public void recalcTimeVariantAttributes(double time);
	// necessary (called from networkChangeEvents)

	// yyyy these two functions should not be public since it exposes the internal mechanics that the departure logic is central
	// while the visualization logic is link-based.  But it needs to be public as long as the network engine is a separate package. 
	// kai, aug'10
	public void registerAgentOnLink(PersonAgent agent);
	public void unregisterAgentOnLink(PersonAgent agent);

	
	//	######################
	// ok:
	
	public QSimI getQSim() ;


	/**
	 * In the end, it really seems much easier to get from there the exact info when the link is full.  kai, aug'10
	 */
	public double getSpaceCap();

	public Link getLink();
	// (underlying data)

	public void addParkedVehicle(QVehicle vehicle);
	// necessary (ini)

	public QVehicle getVehicle(Id vehicleId);
	// not terribly efficient, but a possible method also for general mobsims

	public Collection<QVehicle> getAllVehicles();
	// not terribly efficient, but a possible method also for general mobsims

	Collection<QVehicle> getAllNonParkedVehicles();
	// not terribly efficient, but a possible method also for general mobsims

	public QNode getToQueueNode();
	// I think this is essentially ok since you can't do that much with a QNode.  yyyy Should probably be called getMobsimNode, 
	// though, and for that reason it should be behind an interface.

	public void addDepartingVehicle(QVehicle vehicle);
	// necessary (called from the "facilities"engine)

//	void reinsertBus(QVehicle bus);


}