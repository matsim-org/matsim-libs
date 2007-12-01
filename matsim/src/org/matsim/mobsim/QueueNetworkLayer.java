/* *********************************************************************** *
 * project: org.matsim.*
 * QueueNetworkLayer.java
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

package org.matsim.mobsim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ListIterator;
import java.util.Map;

import org.matsim.mobsim.snapshots.PositionInfo;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.utils.identifiers.IdI;

/**
 * @author david
 *
 * QueueNetworkLayer is responsible for creating the QueueLinks/Nodes and for
 * implementing doSim
 */
public class QueueNetworkLayer extends NetworkLayer {
	/* If simulateAllLinks is set to true, then the method "moveLink" will be called for every link in every timestep.
	 * If simulateAllLinks is set to false, the method "moveLink" will only be called for "active" links (links where at least one
	 * car is in one of the many queues).
	 * One should assume, that the result of a simulation is the same, no matter how "simulateAllLinks" is set. But the order how
	 * the links are processed influences the order of events within one time step. Thus, just comparing the event-files will not
	 * work, but first sorting the two event-files by time and agent-id and then comparing them, will work.
	 */
	private boolean simulateAllLinks = false;

	private final ArrayList<QueueLink> simLinksArray = new ArrayList<QueueLink>();
	private ArrayList<QueueNode> simNodesArray = new ArrayList<QueueNode>();
	private final ArrayList<QueueLink> simActivateThis = new ArrayList<QueueLink>();

	// set to true to move vehicles from waitingList before vehQueue
	final static boolean moveWaitFirst = false;

	@Override
	protected Node newNode(final String id, final String x, final String y, final String type) {
		return new QueueNode(id,x,y,type);
	}

	@Override
	protected Link newLink(final NetworkLayer network, final String id, final Node from, final Node to, final String length,
			 final String freespeed, final String capacity, final String permlanes,
			 final String origid, final String type) {
		return new QueueLink(this,id,from,to,length,freespeed,capacity,permlanes,origid,type);
	}

	public void beforeSim() {
		/* This is the  array of links/nodes that have to be moved in the simulation
		 * in the single CPU version this will be ALL links/nodes
		 * but the parallel version put only the relevant nodes here
		 * We still need the whole net on the parallel version, to reconstruct routes
		 * on vehicle-receiving, etc. */
		this.simNodesArray = new ArrayList<QueueNode>(getNodes().values());
		this.simLinksArray.clear();

		if (this.simulateAllLinks) {
			this.simLinksArray.addAll(getLinks().values());
		}

		// finish init for links
		for (QueueLink link : getLinks().values()) {
			link.finishInit();
		}
	}

	/**
	 * Implements one simulation step, called from simulation framework
	 * @param time The current time in the simulation.
	 */
	public void simStep(final double time) {
		for (QueueNode node : this.simNodesArray) {
			node.moveNode(time);
		}
		reactivateLinks();
		moveLinks(time);
	}

	protected void moveLinks(final double time) {
		ListIterator<QueueLink> links = this.simLinksArray.listIterator();
		QueueLink link;
		boolean isActive;

		// TODO [kn] this is in my view unstable code.  Should be
		// while (links.hasNext()) {
		//    link = links.next();
		//    if ( moveWaitFirst ) {
		//        isActive = link.moveLinkWaitFirst(time);
		//    } else {
		//          isActive = ...isActive;
		//    }
		//    if ( !isActive ...
		// kai, nov07
		/* well, we just moved the if (moveWaitFirst) outside of the while-loop,
		 * so we have the if only once and not for every link. marcel, dez07 */

		if (moveWaitFirst) {

			while (links.hasNext()) {
				link = links.next();
				isActive = link.moveLinkWaitFirst(time);
				if (!isActive && !this.simulateAllLinks) {
					links.remove();
				}
			}

		} else {

			while (links.hasNext()) {
				link = links.next();
				isActive = link.moveLink(time);
				if (!isActive && !this.simulateAllLinks) {
					links.remove();
				}
			}

		}
	}

	/**
	 * Called whenever this object should dump a snapshot
	 * @return A collection with the current positions of all vehicles.
	 */
	public Collection<PositionInfo> getVehiclePositions() {
		Collection<PositionInfo> positions = new ArrayList<PositionInfo>();
		for (QueueLink link : getLinks().values()) {
			link.getVehiclePositions(positions);
		}
		return positions;
	}

	/**
	 * @return Returns the simLinksArray.
	 */
	public ArrayList<QueueLink> getSimLinksArray() {
		return this.simLinksArray;
	}

	/**
	 * @return Returns the simNodesArray.
	 */
	public ArrayList<QueueNode> getSimNodesArray() {
		return this.simNodesArray;
	}

	public void afterSim() {
		/* Reset vehicles on ALL links. We cannot iterate only over the active links
		 * (this.simLinksArray), because there may be links that have vehicles only
		 * in the buffer (such links are *not* active, as the buffer gets emptied
		 * when handling the nodes.
		 */
		for (QueueLink link : getLinks().values()) {
			link.linkStatus = QueueLink.LinkStatus.DEFAULT;
			link.clearVehicles();
		}
	}

	public void addActiveLink(final QueueLink link) {
		if (!this.simulateAllLinks) {
			this.simActivateThis.add(link);
		}
	}

	private void reactivateLinks() {
		if (!this.simulateAllLinks && !this.simActivateThis.isEmpty()) {
			this.simLinksArray.addAll(this.simActivateThis);
			this.simActivateThis.clear();
		}
	}

	@Override
	public boolean removeLink(final Link link) {
		((QueueLink)link).clearVehicles();
		// we have to remove the link from both locations and simLinkArray
		this.simLinksArray.remove(link);
		return super.removeLink(link);
	}
	@Override
	public boolean removeNode(final Node node) {
		this.simNodesArray.remove(node);
		return super.removeNode(node);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<IdI, ? extends QueueLink> getLinks() {
		return (Map<IdI, ? extends QueueLink>)super.getLinks();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<IdI, ? extends QueueNode> getNodes() {
		return (Map<IdI, ? extends QueueNode>)super.getNodes();
	}

}
