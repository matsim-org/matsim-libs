/* *********************************************************************** *
 * project: org.matsim.*
 * QueueNode.java
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

package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.core.mobsim.qsim.interfaces.NetsimLink;
import org.matsim.core.mobsim.qsim.interfaces.NetsimNode;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine.NetsimInternalInterface;

/**
 * Represents a node in the QSimulation.
 */
public class QNode implements NetsimNode {
	private static final Logger log = Logger.getLogger(QNode.class);
	public static class Builder {
		private final NetsimInternalInterface netsimEngine;
		private NetsimEngineContext context;
		public Builder( NetsimInternalInterface netsimEngine2, NetsimEngineContext context ) {
			this.netsimEngine = netsimEngine2;
			this.context = context;
		}
		public QNode build( Node n ) {
			return new QNode( n, context, netsimEngine ) ;
		}
	}

	private final QLinkI[] inLinksArrayCache;
	private final QLinkI[] tempLinks;
	
	/*
	 * This needs to be atomic since this allows us to ensure that an node which is
	 * already active is not activated again. This could happen if multiple thread call
	 * activateNode() concurrently.
	 * cdobler, sep'14
	 */
	private final AtomicBoolean active = new AtomicBoolean(false);

	private final Node node;

	// necessary if Nodes are (de)activated
	private NetElementActivationRegistry activator = null;

	// for Customizable
	private final Map<String, Object> customAttributes = new HashMap<>();

	private final Random random;
	private final NetsimEngineContext context;
	private final NetsimInternalInterface netsimEngine;

	private QNode(final Node n, NetsimEngineContext context, NetsimInternalInterface netsimEngine2) {
		this.node = n;
		this.netsimEngine = netsimEngine2 ;
		this.context = context ;
		int nofInLinks = this.node.getInLinks().size();
		this.inLinksArrayCache = new QLinkI[nofInLinks];
		this.tempLinks = new QLinkI[nofInLinks];
		if (this.context.qsimConfig.getNumberOfThreads() > 1) { 
			// This could just as well be the "normal" case. The second alternative
			// is just there so some scenarios / test cases stay
			// "event-file-compatible". Consider removing the second alternative.
			this.random = MatsimRandom.getLocalInstance();
		} else {
			this.random = MatsimRandom.getRandom();
		}
	}
	
	/**
	 * Loads the inLinks-array with the corresponding links.
	 * Cannot be called in constructor, as the queueNetwork does not yet know
	 * the queueLinks. Should be called by QueueNetwork, after creating all
	 * QueueNodes and QueueLinks.
	 */
	/*package*/ void init() {
		int i = 0;
		for (Link l : this.node.getInLinks().values()) {
			QNetwork network = netsimEngine.getNetsimNetwork() ;
			this.inLinksArrayCache[i] = network.getNetsimLinks().get(l.getId());
			i++;
		}
		/* As the order of links has an influence on the simulation results,
		 * the nodes are sorted to avoid indeterministic simulations. dg[april08]
		 */
		Arrays.sort(this.inLinksArrayCache, new Comparator<NetsimLink>() {
			@Override
			public int compare(NetsimLink o1, NetsimLink o2) {
				return o1.getLink().getId().compareTo(o2.getLink().getId());
			}
		});
	}

	@Override
	public Node getNode() {
		return this.node;
	}

	/*
	 * The ParallelQSim replaces the activator with the QSimEngineRunner 
	 * that handles this node.
	 */
	/*package*/ void setNetElementActivationRegistry(NetElementActivationRegistry activator) {
		this.activator = activator;
	}

	/*
	 * This method is called from QueueWithBuffer.addToBuffer(...) which is triggered at 
	 * some placed, but always initially by a QLink's doSomStep(...) method. I.e. QNodes
	 * are only activated while moveNodes(...) is performed. However, multiple threads
	 * could try to activate the same node at a time, therefore this has to be thread-safe.
	 * cdobler, sep'14 
	 */
	/*package*/ final void activateNode() {	
		/*
		 * this.active.compareAndSet(boolean expected, boolean update)
		 * We expect the value to be false, i.e. the node is de-activated. If this is
		 * true, the value is changed to true and the activator is informed.
		 */
		if (this.active.compareAndSet(false, true)) {
			this.activator.registerNodeAsActive(this);
		}
	}

	final boolean isActive() {
		return this.active.get();
	}

	/**
	 * Moves vehicles from the inlinks' buffer to the outlinks where possible.<br>
	 * The inLinks are randomly chosen, and for each link all vehicles in the
	 * buffer are moved to their desired outLink as long as there is space. If the
	 * front most vehicle in a buffer cannot move across the node because there is
	 * no free space on its destination link, the work on this inLink is finished
	 * and the next inLink's buffer is handled (this means, that at the node, all
	 * links have only like one lane, and there are no separate lanes for the
	 * different outLinks. Thus if the front most vehicle cannot drive further,
	 * all other vehicles behind must wait, too, even if their links would be
	 * free).
	 *
	 * @param now
	 *          The current time in seconds from midnight.
	 * @return 
	 * 		Whether the QNode stays active or not.
	 */
	/*package*/ boolean doSimStep(final double now) {
		
		int inLinksCounter = 0;
		double inLinksCapSum = 0.0;
		// Check all incoming links for buffered agents
		for (QLinkI link : this.inLinksArrayCache) {
			if (!link.isNotOfferingVehicle()) {
				this.tempLinks[inLinksCounter] = link;
				inLinksCounter++;
				inLinksCapSum += link.getLink().getCapacity(now);
			}
		}

		if (inLinksCounter == 0) {
			this.active.set(false);
			return false; // Nothing to do
		}

		// randomize based on capacity
        for (int auxCounter = 0; auxCounter < inLinksCounter; auxCounter++) {
			double rndNum = random.nextDouble() * inLinksCapSum;
			double selCap = 0.0;
			for (int i = 0; i < inLinksCounter; i++) {
				QLinkI link = this.tempLinks[i];
				if (link != null) {
                    selCap += link.getLink().getCapacity(now);
                    if (selCap >= rndNum) {
                        inLinksCapSum -= link.getLink().getCapacity(now);
                        this.tempLinks[i] = null;
                        this.moveLink(link, now);
                        break;
                    }
                }
			}
		}
		
		return true;
	}

	private void moveLink(final QLinkI link, final double now){
//		if ( link instanceof QLinkLanesImpl ) {
			// This cannot be moved to QLinkLanesImpl since we want to be able to serve other lanes if one lane is blocked.
			// kai, feb'12
			// yyyy but somehow I think this should be solved "under the hood" in QLinkLanesImpl.  kai, sep'13
			// zzzz the above yyyy solution would get really ugly with the current interface and the code in the
			// else {} branch: link.isNotOfferingVehicle():boolean would return sequentially the state of the single
			//lanes. Each call would have side effects on a state machine within QLinkLanesImpl. Proposal: think
			//about a better interface first, then solve under the hood. dg, mar'14
			for (QLaneI lane : link.getOfferingQLanes()) {
				while (! lane.isNotOfferingVehicle()) {
					QVehicle veh = lane.getFirstVehicle();
					Id<Link> nextLink = veh.getDriver().chooseNextLinkId();
					if (! (lane.hasGreenForToLink(nextLink) && moveVehicleOverNode(veh, lane, now))) {
						break;
					}
				}
			}
//		} 
//		else {
//			while (!link.isNotOfferingVehicle()) {
//				QVehicle veh = link.getFirstVehicle();
//				if (!moveVehicleOverNode(veh, link, now)) {
//					break;
//				}
//			}
//		}
	}


	private static boolean checkNextLinkSemantics(Link currentLink, Id<Link> nextLinkId, QLinkI nextQLink, QVehicle veh){
		if (nextQLink == null){
			//throw new IllegalStateException
			log.warn("The link id " + nextLinkId + " is not available in the simulation network, but vehicle " + veh.getId() + 
					" plans to travel on that link from link " + veh.getCurrentLink().getId());
			return false ;
		}
		if (currentLink.getToNode() != nextQLink.getLink().getFromNode()) {
			//throw new RuntimeException
			log.warn("Cannot move vehicle " + veh.getId() + " from link " + currentLink.getId() + " to link " + nextQLink.getLink().getId());
			return false ;
		}
		return true ;
	}

	// ////////////////////////////////////////////////////////////////////
	// Queue related movement code
	// ////////////////////////////////////////////////////////////////////
	/**
	 * @return <code>true</code> if the vehicle was successfully moved over the node, <code>false</code>
	 * otherwise (e.g. in case where the next link is jammed)
	 */
	private boolean moveVehicleOverNode(final QVehicle veh, final QLaneI fromLaneBuffer, final double now) {
		Id<Link> nextLinkId = veh.getDriver().chooseNextLinkId();
		Link currentLink = veh.getCurrentLink();

		if ((! fromLaneBuffer.hasGreenForToLink(nextLinkId))) {
			//there is no longer a stuck check for red links. This means that
			//in case of an infinite red time the simulation will not stop automatically because
			//vehicles waiting in front of the red signal will never reach their destination. dg, mar'14
				return false;
		}

		if (nextLinkId == null) {
			log.error( "Agent has no or wrong route! agentId=" + veh.getDriver().getId()
					+ " currentLink=" + currentLink.getId().toString()
					+ ". The agent is removed from the simulation.");
			moveVehicleFromInlinkToAbort(veh, fromLaneBuffer, now, currentLink.getId());
			return true;
		}
		
		QNetwork network = (QNetwork) this.netsimEngine.getNetsimNetwork() ;
		QLinkI nextQueueLink = network.getNetsimLinks().get(nextLinkId);
		if ( !checkNextLinkSemantics(currentLink, nextLinkId, nextQueueLink, veh) ) {
			moveVehicleFromInlinkToAbort( veh, fromLaneBuffer, now, currentLink.getId() ) ;
			return true ;
		}
		
		QLaneI nextQueueLane = nextQueueLink.getAcceptingQLane() ;

		if (nextQueueLane.isAcceptingFromUpstream()) {
			moveVehicleFromInlinkToOutlink(veh, currentLink.getId(), fromLaneBuffer, nextLinkId, nextQueueLane);
			return true;
		}

		if (vehicleIsStuck(fromLaneBuffer, now)) {
			/* We just push the vehicle further after stucktime is over, regardless
			 * of if there is space on the next link or not.. optionally we let them
			 * die here, we have a config setting for that!
			 */
			if (this.context.qsimConfig.isRemoveStuckVehicles()) {
				moveVehicleFromInlinkToAbort(veh, fromLaneBuffer, now, currentLink.getId());
				return false ;
			} else {
				moveVehicleFromInlinkToOutlink(veh, currentLink.getId(), fromLaneBuffer, nextLinkId, nextQueueLane);
				return true; 
				// (yyyy why is this returning `true'?  Since this is a fix to avoid gridlock, this should proceed in small steps. 
				// kai, feb'12) 
			}
		}

		return false;

	}

	private static int wrnCnt = 0 ;
	private void moveVehicleFromInlinkToAbort(final QVehicle veh, final QLaneI fromLane, final double now, Id<Link> currentLinkId) {
		fromLane.popFirstVehicle();
		// -->
		this.context.getEventsManager().processEvent(new LinkLeaveEvent(now, veh.getId(), currentLinkId));
		// <--
		
		// first treat the passengers:
		for ( PassengerAgent pp : veh.getPassengers() ) {
			if ( pp instanceof MobsimAgent ) {
				((MobsimAgent)pp).setStateToAbort(now);
				netsimEngine.arrangeNextAgentState((MobsimAgent)pp) ;
			} else if ( wrnCnt < 1 ) {
				wrnCnt++ ; 
				log.warn("encountering PassengerAgent that cannot be cast into a MobsimAgent; cannot say if this is a problem" ) ;
				log.warn(Gbl.ONLYONCE) ;
			}
		}

		// now treat the driver:
		veh.getDriver().setStateToAbort(now) ;
		netsimEngine.arrangeNextAgentState(veh.getDriver()) ;
	
	}

	private void moveVehicleFromInlinkToOutlink(final QVehicle veh, Id<Link> currentLinkId, final QLaneI fromLane, Id<Link> nextLinkId, QLaneI nextQueueLane) {
		double now = this.context.getSimTimer().getTimeOfDay() ;

		fromLane.popFirstVehicle();
		// -->
//		network.simEngine.getMobsim().getEventsManager().processEvent(new LaneLeaveEvent(now, veh.getId(), currentLinkId, fromLane.getId()));
		this.context.getEventsManager().processEvent(new LinkLeaveEvent(now, veh.getId(), currentLinkId));
		// <--

		veh.getDriver().notifyMoveOverNode( nextLinkId );

		nextQueueLane.addFromUpstream(veh);
		// -->
		this.context.getEventsManager().processEvent(new LinkEnterEvent(now, veh.getId(), nextLinkId ));
		// <--
	}

	private boolean vehicleIsStuck(final QLaneI fromLaneBuffer, final double now) {
//		final double stuckTime = network.simEngine.getStuckTime();
		final double stuckTime = this.context.qsimConfig.getStuckTime() ;
		return (now - fromLaneBuffer.getLastMovementTimeOfFirstVehicle()) > stuckTime;
	}

	@Override
	public Map<String, Object> getCustomAttributes() {
		return customAttributes;
	}

}
