/* *********************************************************************** *
 * project: org.matsim.*
 * QueueRemoteLink.java
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

package playground.david.mobsim.distributed;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import org.matsim.mobsim.QueueLink;
import org.matsim.mobsim.Simulation;
import org.matsim.mobsim.Vehicle;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.utils.misc.Time;

public class QueueRemoteLink extends QueueLink implements RemoteLinkI {

	protected RemoteLinkI linkI = null;
	protected QueueRemoteLink(final NetworkLayer network, final String id, final Node from, final Node to, final String length, final String freespeed, final String capacity, final String permlanes, final String origid, final String type) {
		super(network, id, from, to, length, freespeed, capacity, permlanes, origid,
				type);
	}

	public QueueRemoteLink(final QueueLink link, final NetworkLayer network) {
		super(network, link.getId().toString(), link.getFromNode(), link.getToNode(),
			Double.toString(link.getLength()), Double.toString(link.getFreespeed(Time.UNDEFINED_TIME)),
			Double.toString(link.getCapacity()), Integer.toString(link.getLanes()),
			link.getOrigId(), link.getType());

	}

	protected boolean updateActiveStatus() {
		// Activte/ Deactivate does not work for parallel version
		// DS TODO repair this
		return true;
	}

	// add a vehicle to the link (used by QNode.moveVehicleOverNode)
	@Override
	synchronized public void add(final Vehicle veh) {
		if( this.linkI != null) {
			try {
				Simulation.decLiving();
				this.linkI.transmit(veh);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		} else super.add(veh);
	}

	@Override
	synchronized public boolean hasSpace() {
		if (this.linkI != null)
			try {
				return this.linkI.hasSpace();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		return super.hasSpace();
	}

	public void initRemoteVisibility()
	{
		DistributedQueueSimulation.registerWithRMI("RLink" + this.getId(), this);
	}
	public void exitRemoteVisibility()
	{
		DistributedQueueSimulation.unregisterWithRMI("RLink" + this.getId(), this);
	}

	public void connectToRemoteLink(final String hostname) {

		try {
			String fullname = "rmi://"+hostname + ":/RLink" + this.getId();
//			Gbl.debugMsg(0,this.getClass(),"RMI connect local link with remote Link " + fullname + "]");
			this.linkI = (RemoteLinkI)Naming.lookup(fullname);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		}
	}

	public void transmit(final Vehicle veh) throws RemoteException {
		veh.rebuildVeh(this);
		super.add(veh);
        Simulation.incLiving();
	}

	@Override
	public String toString() {
	if(this.linkI != null) return "RMI Stub link for " + this.id;
	return "RMI Server " + super.toString();
	}

}

