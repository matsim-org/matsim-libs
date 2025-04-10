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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonStuckAndContinueEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.NodeTransition;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.core.mobsim.qsim.interfaces.NetsimLink;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineI.NetsimInternalInterface;
import org.matsim.core.mobsim.qsim.qnetsimengine.TurnAcceptanceLogic.AcceptTurn;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

/**
 * Represents a node in the QSimulation.
 */
final class QNodeImpl extends AbstractQNode {
	private static final Logger log = LogManager.getLogger(QNodeImpl.class);
	public static class Builder {
		private final NetsimInternalInterface netsimEngine;
		private final NetsimEngineContext context;
		private final QSimConfigGroup qsimConfig;
		public Builder( NetsimInternalInterface netsimEngine2, NetsimEngineContext context, QSimConfigGroup qsimConfig ) {
			this.netsimEngine = netsimEngine2;
			this.context = context;
			this.qsimConfig = qsimConfig;
		}
		private TurnAcceptanceLogic turnAcceptanceLogic = new DefaultTurnAcceptanceLogic() ;
		public final void setTurnAcceptanceLogic( TurnAcceptanceLogic turnAcceptanceLogic ) {
			this.turnAcceptanceLogic = turnAcceptanceLogic ;
		}
		public QNodeImpl build( Node n ) {
			return new QNodeImpl( n, context, netsimEngine, turnAcceptanceLogic, qsimConfig ) ;
		}
	}

	private final QLinkI[] inLinksArrayCache;
	private final QLinkI[] tempLinks;
	private Double[] inLinkPriorities;

	private final Random random;
	private final NetsimEngineContext context;
	private final NetsimInternalInterface netsimEngine;

	private final TurnAcceptanceLogic turnAcceptanceLogic ;
	private NodeTransition nodeTransitionLogic;
	private boolean stopMoveNodeWhenSingleOutlinkFull;
	private boolean atLeastOneOutgoingLaneIsJammed;

	/**
	 * Checks if capacity is significant positive.
	 * This allows for a small eps unequal to zero to mitigate rounding errors that can happen
	 * with non integer capacities.
	 */
	private static boolean capacityLeft(double x) {
		return x > 1E-10;
	}

	private QNodeImpl(final Node n, NetsimEngineContext context, NetsimInternalInterface netsimEngine2,
			TurnAcceptanceLogic turnAcceptanceLogic, QSimConfigGroup qsimConfig) {
		super(n) ;
		this.netsimEngine = netsimEngine2 ;
		this.context = context ;
		this.turnAcceptanceLogic = turnAcceptanceLogic;
		this.nodeTransitionLogic = qsimConfig.getNodeTransitionLogic();

		switch (nodeTransitionLogic) {
		case emptyBufferAfterBufferRandomDistribution_nodeBlockedWhenSingleOutlinkFull:
		case moveVehByVehRandomDistribution_nodeBlockedWhenSingleOutlinkFull:
		case moveVehByVehDeterministicPriorities_nodeBlockedWhenSingleOutlinkFull:
			this.stopMoveNodeWhenSingleOutlinkFull = true;
			break;
		case emptyBufferAfterBufferRandomDistribution_dontBlockNode:
		case moveVehByVehRandomDistribution_dontBlockNode:
			this.stopMoveNodeWhenSingleOutlinkFull = false;
			break;
		default:
			throw new UnsupportedOperationException("Node transition logic " + nodeTransitionLogic + " is not implemented.");
		}

		int nofInLinks = n.getInLinks().size();
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
	@Override
	public void init() {
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
	@Override
	public boolean doSimStep(final double now) {

		if (inLinkPriorities == null) {
			/* initialize inLink priorities.
			 * unfortunately, this can't be done in init() because when capacities are changed
			 * before controler.run() init() uses the old capacities. theresa, may'20 */
			inLinkPriorities = new Double[inLinksArrayCache.length];
			for (int inLinkCounter=0; inLinkCounter<this.inLinksArrayCache.length; inLinkCounter++) {
				double linkCap = this.inLinksArrayCache[inLinkCounter].getLink().getCapacity(now);
				inLinkPriorities[inLinkCounter] = 1. / linkCap;
			}
		}

		// reset congestion flag
		this.atLeastOneOutgoingLaneIsJammed = false;

		double inLinksCapSum = 0.0;
		// Check all incoming links for buffered agents
		for (int inLinkIndex = 0; inLinkIndex < this.inLinksArrayCache.length; inLinkIndex++) {
			QLinkI link = this.inLinksArrayCache[inLinkIndex];
			if (!link.isNotOfferingVehicle()) {
				this.tempLinks[inLinkIndex] = link;
				inLinksCapSum += link.getLink().getCapacity(now);
			}
		}

		if (!capacityLeft(inLinksCapSum)) {
			this.setActive(false);
			return false; // Nothing to do
		}

		// select vehicles to be moved over the node. The order of vehicles selected depends on the chosen node transition logic.
		switch (nodeTransitionLogic) {
		case emptyBufferAfterBufferRandomDistribution_dontBlockNode:
		case emptyBufferAfterBufferRandomDistribution_nodeBlockedWhenSingleOutlinkFull:
			// randomize based on link capacity: select a link; if next links have enough space all vehicles from the buffer are allowed to pass the node
			while (capacityLeft(inLinksCapSum)) {
				double rndNum = random.nextDouble() * inLinksCapSum;
				double selCap = 0.0;
				for (int i = 0; i < this.inLinksArrayCache.length; i++) {
					QLinkI link = this.tempLinks[i];
					if (link != null) {
						// link is offering vehicles
						selCap += link.getLink().getCapacity(now);
						if (selCap >= rndNum) {
							// try to move vehicles from this link over the node
							inLinksCapSum -= link.getLink().getCapacity(now);
							this.tempLinks[i] = null;
							this.moveLink(link, now);
							if (this.stopMoveNodeWhenSingleOutlinkFull && this.atLeastOneOutgoingLaneIsJammed) {
								// consider intersection as blocked; stop this node sim step
								return true;
							}
							break;
						}
					}
				}
			}
			break;
		case moveVehByVehRandomDistribution_dontBlockNode:
		case moveVehByVehRandomDistribution_nodeBlockedWhenSingleOutlinkFull:
			// randomize based on link capacity: select the vehicles (one by one) that are allowed to pass the node
			while (capacityLeft(inLinksCapSum)) {
				double rndNum = random.nextDouble() * inLinksCapSum;
				double selCap = 0.0;
				for (int i = 0; i < this.inLinksArrayCache.length; i++) {
					QLinkI link = this.tempLinks[i];
					if (link != null) {
						// link is offering vehicles
						selCap += link.getLink().getCapacity(now);
						if (selCap >= rndNum) {
							// try to move a vehicle from this link over the node
							if ( ! moveFirstVehicleOnLink(now, link)) {
								// the link is not able to move (more) vehicles in this time step
								inLinksCapSum -= link.getLink().getCapacity(now);
								this.tempLinks[i] = null;
							} else {
								// a vehicle has been moved
							}
							if (this.stopMoveNodeWhenSingleOutlinkFull && this.atLeastOneOutgoingLaneIsJammed) {
								// consider intersection as blocked; stop this node sim step
								return true;
							}
							// select the next link (could be the same again) that is allowed to move a vehicle. I.e. start again with the beginning of the while-loop
							break;
						}
					}
				}
			}
			break;
		case moveVehByVehDeterministicPriorities_nodeBlockedWhenSingleOutlinkFull:
			// deterministically choose the inLinks vehicle by vehicle based on their capacity and also account for decisions made in previous time steps (i.e. update priorities) to approximate the correct distribution over time.
			double prioWithWhichTheLastVehWasSent = 0.;
			while (capacityLeft(inLinksCapSum)) {
				// look for the inLink with minimal priority that has vehicles in the buffer (use first one when equal, because links are sorted by ID)
				double minPrio = Float.MAX_VALUE;
				int prioInLinkIndex = -1;
				for (int i = 0; i < this.inLinksArrayCache.length; i++) {
					if (tempLinks[i] != null &&
							/* compare link priorities with some tolerance (done by comparing the float part of the double).
							 * otherwise, priorities that should be equal are different when compared as double because its fixed precision
							 * (e.g. 1/7200 + 1/7200 as double is greater than 1/3600).
							 * for equal priorities the tie breaking rule is applied (lower link id, see above)
							 */
							(float) this.inLinkPriorities[i].doubleValue() < (float) minPrio) {
						// link is offering vehicles and has lowest priority so far
						minPrio = this.inLinkPriorities[i];
						prioInLinkIndex = i;
					}
				}
				QLinkI selectedLink = this.inLinksArrayCache[prioInLinkIndex];

				// try to move a vehicle from this selected link over the node
				if ( ! moveFirstVehicleOnLink(now, selectedLink)) {
					// the link is not able to move (more) vehicles in this time step
					inLinksCapSum -= selectedLink.getLink().getCapacity(now);
					this.tempLinks[prioInLinkIndex] = null;
				} else {
					// a vehicle has been moved; update priority of the selected link
					prioWithWhichTheLastVehWasSent = minPrio;
					this.inLinkPriorities[prioInLinkIndex] += 1. / selectedLink.getLink().getCapacity(now);
				}
				if (this.atLeastOneOutgoingLaneIsJammed) {
					// stopMoveNodeWhenSingleOutlinkFull is always true for this node transition
					// consider intersection as blocked; stop this node sim step; (*)
					updatePriorities(now, prioWithWhichTheLastVehWasSent, prioInLinkIndex);
					return true;
				}

				// select the next link (could be the same again) that is allowed to move a vehicle. I.e. start again with the beginning of the while-loop
			}
			// vehicle moving done for this time step. update priorities
			updatePriorities(now, prioWithWhichTheLastVehWasSent, -1);
			break;
		default:
			throw new UnsupportedOperationException("Node transition logic " + nodeTransitionLogic + " is not implemented.");
		}

		return true;
	}

	private void updatePriorities(final double now, double prioWithWhichTheLastVehWasSent, int linkIndexToBeExcluded) {

		for (int linkIndex=0; linkIndex < this.inLinkPriorities.length; linkIndex++) {
			// when the node was blocked (see * above) no priority updating is necessary for the blocked links
			if (linkIndex != linkIndexToBeExcluded && tempLinks[linkIndex] == null) {
				// shift priorities of links that where disabled earlier because of other reasons, e.g. because their buffer was empty or a traffic light showed red,
				// to the level of the other priorities such that they are not overprioritized in next time steps
				inLinkPriorities[linkIndex] = prioWithWhichTheLastVehWasSent
						+ 1. / inLinksArrayCache[linkIndex].getLink().getCapacity(now);
			}
			// shift all priorities around zero accordingly to avoid overflow of double at some time step
			inLinkPriorities[linkIndex] -= prioWithWhichTheLastVehWasSent;
		}
	}

	/**
	 * @return <code>true</code> if a vehicle was successfully moved over the node, <code>false</code>
	 * otherwise (e.g. in case all next links are jammed)
	 *
	 * This logic is only implemented for the case without lanes. When multiple lanes exist for a link, an exception is thrown.
	 */
	private boolean moveFirstVehicleOnLink(final double now, QLinkI link) {
		if (link.getOfferingQLanes().size() > 1) {
			throw new RuntimeException("The qsim node transition parameter " + NodeTransition.moveVehByVehRandomDistribution_dontBlockNode + ", "
					+ NodeTransition.moveVehByVehRandomDistribution_nodeBlockedWhenSingleOutlinkFull + " and "
						+ NodeTransition.moveVehByVehDeterministicPriorities_nodeBlockedWhenSingleOutlinkFull
							+ " are only implemented for the case without lanes. But link " + link.getLink().getId()
								+ " in your scenario has more than one lane. "
									+ "Use the default node transiton " + NodeTransition.emptyBufferAfterBufferRandomDistribution_dontBlockNode
										+ " or " + NodeTransition.emptyBufferAfterBufferRandomDistribution_nodeBlockedWhenSingleOutlinkFull
											+ " or adapt the implementation such that it also works for lanes.");
		}
		for (QLaneI lane : link.getOfferingQLanes()) {
			if (! lane.isNotOfferingVehicle()) {
				QVehicle veh = lane.getFirstVehicle();
				if (moveVehicleOverNode(veh, link, lane, now)) {
					// vehicle was moved. stop here
					return true;
				} else {
					// vehicle was not moved, e.g. because the next link is jammed or a traffic light on this link shows red.
					if (this.stopMoveNodeWhenSingleOutlinkFull && this.atLeastOneOutgoingLaneIsJammed) {
						// next link/lane is jammed. stop vehicle moving for the whole node
						return false;
					} else {
						// try next lane
						continue;
					}
				}
			}
		}
		return false;
	}

	private void moveLink(final QLinkI link, final double now){
		for (QLaneI lane : link.getOfferingQLanes()) {
			while (! lane.isNotOfferingVehicle()) {
				QVehicle veh = lane.getFirstVehicle();
				if (! moveVehicleOverNode(veh, link, lane, now )) {
					// vehicle was not moved, e.g. because the next link is jammed or a traffic light on this link shows red.
					if (this.stopMoveNodeWhenSingleOutlinkFull && this.atLeastOneOutgoingLaneIsJammed) {
						// next link/lane is jammed. stop vehicle moving for the whole node
						return;
					} else {
						// try next lane
						break;
					}
				}
			}
		}
	}



	// ////////////////////////////////////////////////////////////////////
	// Queue related movement code
	// ////////////////////////////////////////////////////////////////////
	/**
	 * @return <code>true</code> if the vehicle was successfully moved over the node, <code>false</code>
	 * otherwise (e.g. in case where the next link is jammed)
	 */
	private boolean moveVehicleOverNode( final QVehicle veh, QLinkI fromLink, final QLaneI fromLane, final double now ) {
		Id<Link> nextLinkId = veh.getDriver().chooseNextLinkId();
		Link currentLink = fromLink.getLink() ;

		AcceptTurn turn = turnAcceptanceLogic.isAcceptingTurn(currentLink, fromLane, nextLinkId, veh, this.netsimEngine.getNetsimNetwork(), now);
		if ( turn.equals(AcceptTurn.ABORT) ) {
			moveVehicleFromInlinkToAbort( veh, fromLane, now, currentLink.getId() ) ;
			return true ;
		} else if ( turn.equals(AcceptTurn.WAIT) ) {
			return false;
		}

		QLinkI nextQueueLink = this.netsimEngine.getNetsimNetwork().getNetsimLinks().get(nextLinkId);
		QLaneI nextQueueLane = nextQueueLink.getAcceptingQLane() ;
		if (nextQueueLane.isAcceptingFromUpstream()) {
			moveVehicleFromInlinkToOutlink(veh, currentLink.getId(), fromLane, nextLinkId, nextQueueLane);
			return true;
		}
		// else, i.e. next link or lane is jammed
		this.atLeastOneOutgoingLaneIsJammed = true;

		if (vehicleIsStuck(fromLane, now)) {
			/* We just push the vehicle further after stucktime is over, regardless
			 * of if there is space on the next link or not.. optionally we let them
			 * die here, we have a config setting for that!
			 */
			if (this.context.qsimConfig.isRemoveStuckVehicles()) {
				moveVehicleFromInlinkToAbort(veh, fromLane, now, currentLink.getId());
				return false ;
			} else {
				if (this.context.qsimConfig.isNotifyAboutStuckVehicles()) {
					// first treat the passengers:
					for ( PassengerAgent pp : veh.getPassengers() ) {
						this.context.getEventsManager().processEvent(new PersonStuckAndContinueEvent(now, pp.getId(), fromLink.getLink().getId(), veh.getDriver().getMode()));
					}
					// now treat the driver:
					this.context.getEventsManager().processEvent(new PersonStuckAndContinueEvent(now, veh.getDriver().getId(), fromLink.getLink().getId(), veh.getDriver().getMode()));
				}
				moveVehicleFromInlinkToOutlink(veh, currentLink.getId(), fromLane, nextLinkId, nextQueueLane);
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

		// -->
		this.context.getEventsManager().processEvent(new LinkEnterEvent(now, veh.getId(), nextLinkId));
		// <--
		nextQueueLane.addFromUpstream(veh);
	}

	private boolean vehicleIsStuck(final QLaneI fromLaneBuffer, final double now) {
		//		final double stuckTime = network.simEngine.getStuckTime();
		final double stuckTime = this.context.qsimConfig.getStuckTime() ;
		return (now - fromLaneBuffer.getLastMovementTimeOfFirstVehicle()) > stuckTime;
	}


}
