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
import java.util.Iterator;
import java.util.ListIterator;

import org.matsim.mobsim.snapshots.PositionInfo;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;

/**
 * @author david
 * 
 * QueueNetworkLayer is responsible for creating the QueueLinks/Nodes and for 
 * implementing doSim ( TODO [DS] which should be moved into an Simulatable interface)
 * 
 */
public class QueueNetworkLayer extends NetworkLayer 
//implements MobsimNetI
{
	/* If simulateAllLinks is set to true, then the method "moveLink" will be called for every link in every timestep.
	 * If simulateAllLinks is set to false, the method "moveLink" will only be called for "active" links (links where at least one 
	 * car is in one of the many queues). 
	 * One should assume, that the result of a simulation is the same, no matter how "simulateAllLinks" is set. But the order how
	 * the links are processed influences the order of events within one time step. Thus, just comparing the event-files will not
	 * work, but first sorting the two event-files by time and agent-id and then comparing them, will work.
	 */
	private boolean simulateAllLinks = false;
	
	private ArrayList<QueueLink> simLinksArray = new ArrayList<QueueLink>();
	private ArrayList<QueueNode> simNodesArray = new ArrayList<QueueNode>();
	private ArrayList<QueueLink> simActivateThis = new ArrayList<QueueLink>();
	
	// set to true to move vehicles from waitingList before vehQueue
	final static boolean moveWaitFirst = false; 

	protected Node newNode(final String id, final String x, final String y, final String type) {
		return new QueueNode(id,x,y,type);
	}
	protected Link newLink(final NetworkLayer network, final String id, final Node from, final Node to, final String length,
			 final String freespeed, final String capacity, final String permlanes,
			 final String origid, final String type) {
		return new QueueLink(this,id,from,to,length,freespeed,capacity,permlanes,origid,type);
	}

	@SuppressWarnings("unchecked")
	public void beforeSim()
	{
		// This is the  array of links/nodes that have to be moved in the simulation
		// in the single CPU version this will be ALL links/nodes
		// but the parallel version put only the relevant nodes here
		// We still need the whole net on the parallel version, to reconstruct routes
		// on vehicle-receiving, etc
		simNodesArray = new ArrayList<QueueNode>(this.nodes);
		simLinksArray.clear();

		if (this.simulateAllLinks) {
			simLinksArray.addAll(this.links);
		}
		
		// finish init for links
		for (Iterator iter = this.links.iterator(); iter.hasNext(); ) {
			QueueLink link = (QueueLink) iter.next();
			link.finishInit();
		}
	}

	/**
	 * implements one simulation step, called from simulation framework
	 */
	public void simStep(double t) {
	// Included this for implementing SimNetI as we might later want 
	// to use the SimController, but then we need to implement SimPopulation as well
	// it means we have to wrap Plans in some meaningful way
				
		Iterator<QueueNode> nodes = simNodesArray.iterator();
		while(nodes.hasNext()) {
			nodes.next().moveNode(t);
		}
		
		reactivateLinks();
		
		moveLinks(t);
		
	}
	
	protected void moveLinks(double time) {
		boolean isActive = true;
		
		ListIterator<QueueLink> links = simLinksArray.listIterator();
		QueueLink link = null;
		
		// TODO [kn] this is in my view unstable code.  Should be
		// while (links.hasNext()) {
		//    link = links.next() ;
		//    if ( moveWaitFirst ) {
		//        isActive = link.moveLinkWaitFirst(time) ;
		//    } else {
		//          isActive = ...isActive ;
		//    }
		//    if ( !isActive ...
		
		if (moveWaitFirst) {
		
			while(links.hasNext()) {
				link = links.next();
				isActive = link.moveLinkWaitFirst(time);
				if (!isActive && !this.simulateAllLinks) {
					links.remove();
//					QueueNode toNode = (QueueNode) link.getToNode() ;
//					toNode.checkNodeForDeActivation() ;
				}
			}

		} else {
			
			while(links.hasNext()) {
				link = links.next();
				isActive = link.moveLink(time);
				if (!isActive && !this.simulateAllLinks) {
					links.remove();
//					QueueNode toNode = (QueueNode) link.getToNode() ;
//					toNode.checkNodeForDeActivation() ;
				}
			}
			
		}
	}
	
	/**
	 * Called whenever this object should dump a snapshot
	 */
	public Collection<PositionInfo> getVehiclePositions() {
		Collection<PositionInfo> positions = new ArrayList<PositionInfo>();
		for (Object obj : getLinks()) {
			QueueLink l = (QueueLink)obj;
			l.getVehiclePositions(positions);
		}
		return positions;
	}

	/**
	 * @return Returns the simLinksArray.
	 */
	public ArrayList<QueueLink> getSimLinksArray() {
		return simLinksArray;
	}

	/**
	 * @return Returns the simNodesArray.
	 */
	public ArrayList getSimNodesArray() {
		return simNodesArray;
	}

	//////////////////////////////////////////////////////////////////////
	// method for the MobsimNetI interface
	//////////////////////////////////////////////////////////////////////

//	public void addAgent(MobsimAgentI agent) {
//		QueueLink link = (QueueLink)agent.getDepartureLink();
//		link.addParking((Vehicle)agent);
//	}

//	public Set getArrivedAgents() {
//		// _TODO Arrived Agents must be registered somewhere to be returned here 
//		return null;
//	}

	public void afterSim() {
		/* Reset vehicles on ALL links. We cannot iterate only over the active links
		 * (this.simLinksArray), because there may be links that have vehicles only 
		 * in the buffer (such links are *not* active, as the buffer gets emptied
		 * when handling the nodes.
		 */
		for (Object obj : getLinks()) {
			QueueLink link = (QueueLink) obj;
			link.linkStatus = QueueLink.LinkStatus.DEFAULT;
			link.clearVehicles();
		}
	}

	public void addActiveLink(QueueLink link) {
		if (!this.simulateAllLinks) {
			simActivateThis.add(link);
		}
	}

	private void reactivateLinks() {
		if (!this.simulateAllLinks && !simActivateThis.isEmpty()) {
			simLinksArray.addAll(simActivateThis);
			simActivateThis.clear();
		}
	}
			
	@Override
	public boolean removeLink(Link link) {
		((QueueLink)link).clearVehicles();
		// we have to remove the link from both locations and simLinkArray
		simLinksArray.remove(link);
		return super.removeLink(link);
	}
	@Override
	public boolean removeNode(Node node) {
		simNodesArray.remove(node);
		return super.removeNode(node);
	}


}
