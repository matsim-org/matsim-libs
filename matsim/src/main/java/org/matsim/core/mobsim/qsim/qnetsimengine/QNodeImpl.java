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

import java.util.*;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.NetsimLink;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineI.NetsimInternalInterface;
import org.matsim.core.mobsim.qsim.qnetsimengine.TurnAcceptanceLogic.AcceptTurn;
import org.matsim.lanes.Lane;

/**
 * Represents a node in the QSimulation.
 */
final class QNodeImpl extends AbstractQNode {
	private static final Logger log = Logger.getLogger(QNodeImpl.class);
	public static class Builder {
		private final NetsimInternalInterface netsimEngine;
		private final NetsimEngineContext context;
		public Builder( NetsimInternalInterface netsimEngine2, NetsimEngineContext context ) {
			this.netsimEngine = netsimEngine2;
			this.context = context;
		}
		private TurnAcceptanceLogic turnAcceptanceLogic = new DefaultTurnAcceptanceLogic() ;
		public final void setTurnAcceptanceLogic( TurnAcceptanceLogic turnAcceptanceLogic ) {
			this.turnAcceptanceLogic = turnAcceptanceLogic ;
		}
		public QNodeImpl build( Node n ) {
			return new QNodeImpl( n, context, netsimEngine, turnAcceptanceLogic ) ;
		}
	}
	
	private final QLinkI[] inLinksArrayCache;
	private final QLinkI[] tempLinks;
	
	
	private final Random random;
	private final NetsimEngineContext context;
	private final NetsimInternalInterface netsimEngine;
	
	private final TurnAcceptanceLogic turnAcceptanceLogic ;

	private double now;
	
	private QNodeImpl(final Node n, NetsimEngineContext context, NetsimInternalInterface netsimEngine2, TurnAcceptanceLogic turnAcceptanceLogic) {
		super(n) ;
		this.netsimEngine = netsimEngine2 ;
		this.context = context ;
		this.turnAcceptanceLogic = turnAcceptanceLogic;
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
		this.now = now;

		int inLinksCounter = 0;
		double inLinksCapSum = 0.0;
		// Check all incoming links for buffered agents
		//??? info od reszty? to są incomming, czyli te powinny być lokalnie?
		for (QLinkI link : this.inLinksArrayCache) {
			if (!link.isNotOfferingVehicle()) {
				this.tempLinks[inLinksCounter] = link;
				inLinksCounter++;
				inLinksCapSum += link.getLink().getCapacity(now);
			}
		}
		
		if (inLinksCounter == 0) {
			this.setActive(false);
			return false; // Nothing to do
		}

		List<MoveVehicleDto> moveVehicleDtos = new LinkedList<>();
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
						myMoveLink(link, now);
						break;
					}
				}
			}
		}
		return true;
	}
	
	private void moveLink(final QLinkI link, final double now){
		for (QLaneI lane : link.getOfferingQLanes()) {
			while (! lane.isNotOfferingVehicle()) {
				QVehicle veh = lane.getFirstVehicle();
				if (! moveVehicleOverNode(veh, link, lane, now )) {
					break;
				}
			}
		}
	}

	private void myMoveLink(final QLinkI link, final double now){
		for (QLaneI lane : link.getOfferingQLanes()) {
			moveLane(link, lane, now);
		}
	}

	private void moveLane(QLinkI fromLink, final QLaneI fromLane, final double now ) {
		Queue<QVehicle> buffer = fromLane.getBuffer();
		while (!buffer.isEmpty()) {
			if (!tryMoveBatch(buffer, fromLink, fromLane, now))
				break;
		}
		if (!fromLane.isNotOfferingVehicle()) {
			QVehicle veh = fromLane.getFirstVehicle();
			if (vehicleIsStuck(fromLane, now)) {
				moveVehicleFromInlinkToAbort(veh, fromLane, now, fromLink.getLink().getId());
			}
		}
	}

	private boolean tryMoveBatch(Queue<QVehicle> buffer, QLinkI fromLink, final QLaneI fromLane, final double now) {
		QVehicle veh = buffer.peek();
		if (veh == null)
			return false;
		Id<Link> batchLinkId = veh.getDriver().chooseNextLinkId();
		List<MoveVehicleDto> batch = new LinkedList<>();

		while (!buffer.isEmpty()) {
			MoveVehicleDto moveVehicleDto = addToBatch(buffer, fromLink, fromLane, now, batchLinkId);
			if (moveVehicleDto == null) {
				if (batch.isEmpty())
					return false;
				else {
//					Logger.getRootLogger().info("moving batch " + batch);
					moveBatch(batchLinkId, batch);
					return true;
				}
			} else {
				if (!moveVehicleDto.isAborted())
					batch.add(moveVehicleDto);
			}
		}

		if (!batch.isEmpty()) {
			moveBatch(batchLinkId, batch);
			return true;
		}
		return false;
	}

	private void moveBatch(Id<Link> linkId, List<MoveVehicleDto> batch) {
		Id<Node> toNodeId = this.netsimEngine.getNetsimNetwork().getNetsimLinks().get(linkId).getToNode().getNode().getId();
		Integer workerId = netsimEngine.getQSim().getNodesWorkerIds().get(toNodeId);
		if (workerId.equals(this.netsimEngine.getQSim().getWorkerId())) {
			acceptVehicles(batch, true);
		} else {
			List<AcceptedVehiclesDto> accepted = netsimEngine.getQSim().getWorkerDelegate().update(workerId, batch, now);
			handleAccepted(accepted);
		}
	}

	private MoveVehicleDto addToBatch(Queue<QVehicle> buffer, QLinkI fromLink, final QLaneI fromLane, final double now, Id<Link> batchLinkId) {
		QVehicle veh = buffer.peek();
		if (veh == null)
			return null;
		Id<Link> nextLinkId = veh.getDriver().chooseNextLinkId();
		if (!nextLinkId.equals(batchLinkId))
			return null;
		Link currentLink = fromLink.getLink() ;

		AcceptTurn turn = turnAcceptanceLogic.isAcceptingTurn(currentLink, fromLane, nextLinkId, veh, this.netsimEngine.getNetsimNetwork(), now);
		if ( turn.equals(AcceptTurn.ABORT) ) {
			moveVehicleFromInlinkToAbort( veh, fromLane, now, currentLink.getId() ) ;
			buffer.poll();
			return MoveVehicleDto.aborted() ;
		} else if ( turn.equals(AcceptTurn.WAIT) ) {
			return null;
		}
//		Logger.getRootLogger().info("polling from buffer " + buffer);
		buffer.poll();
		MobsimDriverAgent driver = veh.getDriver();
		Id<Node> toNodeId = this.netsimEngine.getNetsimNetwork().getNetsimLinks().get(nextLinkId).getToNode().getNode().getId();
		return new MoveVehicleDto(node.getId(), toNodeId, fromLink.getLink().getId(), fromLane.getId(), veh.getId(), driver.getId(), driver.getLinkIndex(), driver.getPlanIndex(), nextLinkId);
	}

	private boolean myMoveVehicleOverNode( final QVehicle veh, QLinkI fromLink, final QLaneI fromLane, final double now ) {
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

		///łatwiej byłoby remove, trzeba dodatkowych sprawdzeń
		if (vehicleIsStuck(fromLane, now)) {
			/* We just push the vehicle further after stucktime is over, regardless
			 * of if there is space on the next link or not.. optionally we let them
			 * die here, we have a config setting for that!
			 */
			if (this.context.qsimConfig.isRemoveStuckVehicles()) {
				moveVehicleFromInlinkToAbort(veh, fromLane, now, currentLink.getId());
				return false ;
			} else {
				moveVehicleFromInlinkToOutlink(veh, currentLink.getId(), fromLane, nextLinkId, nextQueueLane);
				return true;
				// (yyyy why is this returning `true'?  Since this is a fix to avoid gridlock, this should proceed in small steps.
				// kai, feb'12)
			}
		}

		return false;

	}

	private List<MoveVehicleDto> getVehiclesReadyToLeave(final QLinkI link, final double now){
		List<MoveVehicleDto> moveVehicleDtos = new LinkedList<>();
		for (QLaneI lane : link.getOfferingQLanes()) {
			Queue<QVehicle> buffer = lane.getBuffer();
			while (!buffer.isEmpty()) {
				QVehicle veh = buffer.poll();
				if (!getVehicleOverNode(veh, link, lane, now)) {
					break;
				}
				MobsimDriverAgent driver = veh.getDriver();
				Id<Link> nextLinkId = driver.chooseNextLinkId();
				Id<Node> toNodeId = this.netsimEngine.getNetsimNetwork().getNetsimLinks().get(nextLinkId).getToNode().getNode().getId();
				MoveVehicleDto moveVehicleDto = new MoveVehicleDto(node.getId(), toNodeId, link.getLink().getId(), lane.getId(), veh.getId(), driver.getId(), driver.getLinkIndex(), driver.getPlanIndex(), nextLinkId);
				moveVehicleDtos.add(moveVehicleDto);
			}
		}
		return moveVehicleDtos;
	}

	private boolean getVehicleOverNode(QVehicle veh, QLinkI fromLink, QLaneI fromLane, double now) {
		Id<Link> nextLinkId = veh.getDriver().chooseNextLinkId();
		Link currentLink = fromLink.getLink() ;

		AcceptTurn turn = turnAcceptanceLogic.isAcceptingTurn(currentLink, fromLane, nextLinkId, veh, this.netsimEngine.getNetsimNetwork(), now);
		if ( turn.equals(AcceptTurn.ABORT) ) {
			//todo moveToAbort potem
			return true ;
		} else if ( turn.equals(AcceptTurn.WAIT) ) {
			return false;
		}

//		QLinkI nextQueueLink = this.netsimEngine.getNetsimNetwork().getNetsimLinks().get(nextLinkId);
//		QLaneI nextQueueLane = nextQueueLink.getAcceptingQLane() ;
		return true;
		//todo stuck vehicles
		//tutaj jest problem, jeśli je abortujemy to potem inny node musi je wprowadzić z powrotem, chyba trzeba przesyłać wszystkie z flagą stuck żeby puścic mimo capacity
	}

	@Override
	public List<AcceptedVehiclesDto> acceptVehicles(List<MoveVehicleDto> moveVehicleDtos, boolean local) {
//		Logger.getRootLogger().info("accepting vehicles");
		List<AcceptedVehiclesDto> acceptedVehicles = new LinkedList<>();
		for (MoveVehicleDto moveVehicleDto : moveVehicleDtos) {
			if (!acceptVehicle(moveVehicleDto, local))
				break;
			acceptedVehicles.add(moveVehicleDto.toAcceptedVehiclesDto());
		}
		return acceptedVehicles;
	}

	@Override
	public Map<Id<Link>, List<AcceptedVehiclesDto>> acceptVehiclesLocal(List<MoveVehicleDto> moveVehicleDtos) {
		//todo to tak samo na wątki
		//może da się to lepiej zrobić?
		List<AcceptedVehiclesDto> accepted = acceptVehicles(moveVehicleDtos, true);
		return accepted.stream()
				.collect(Collectors.groupingBy(AcceptedVehiclesDto::getLinkId));
	}

	public boolean acceptVehicle(MoveVehicleDto moveVehicleDto, boolean local) {
		Id<Link> nextLinkId = moveVehicleDto.getToLinkId();
		QLinkI nextQueueLink = this.netsimEngine.getNetsimNetwork().getNetsimLinks().get(nextLinkId);
		QLaneI nextQueueLane = nextQueueLink.getAcceptingQLane() ;

		MobsimVehicle veh = netsimEngine.getQSim().getVehicles().get(moveVehicleDto.getVehicleId());

		if (nextQueueLane.isAcceptingFromUpstream()) {
			acceptVehicleFromInlinkToOutlink((QVehicle) veh, moveVehicleDto.getFromLinkId(), moveVehicleDto.getFromLaneId(), nextLinkId, nextQueueLane, moveVehicleDto.getPersonId(), moveVehicleDto.getPersonLinkIndex(), moveVehicleDto.getPlanIndex(), local);
			return true;
		}

		return false;
	}

	private void acceptVehicleFromInlinkToOutlink(final QVehicle veh, Id<Link> fromLinkId, Id<Lane> fromLaneId, Id<Link> nextLinkId, QLaneI nextQueueLane, Id<Person> personId, int personLinkIndex, int planIndex, boolean local) {
//		Logger.getRootLogger().info("accepting vehicle, local: " + local + " " + veh.getId());
		double now = this.context.getSimTimer().getTimeOfDay() ;

		// -->
		//		network.simEngine.getMobsim().getEventsManager().processEvent(new LaneLeaveEvent(now, veh.getId(), currentLinkId, fromLane.getId()));
		this.context.getEventsManager().processEvent(new LinkLeaveEvent(now, veh.getId(), fromLinkId));
		// <--

		MobsimDriverAgent mobsimDriverAgent = (MobsimDriverAgent) netsimEngine.getQSim().getAllAgents().get(personId);

		//todo to da się lepiej zrobić ?
		if (local) {
			QLinkI link = netsimEngine.getNetsimNetwork().getNetsimLinks().get(fromLinkId);
			QLaneI lane = link.getOfferingQLanes().stream().filter(l -> l.getId().equals(fromLaneId)).findFirst().get();
			QVehicle popped = lane.popFirstVehicle();
//			Logger.getRootLogger().info("popping local " + popped);
			mobsimDriverAgent.notifyMoveOverNode(nextLinkId);
		} else {
			QLinkI toLink = netsimEngine.getNetsimNetwork().getNetsimLinks().get(nextLinkId);
			veh.setCurrentLink(toLink.getLink());
			veh.setDriver(mobsimDriverAgent);
			mobsimDriverAgent.setVehicle(veh) ;
			netsimEngine.getQSim().insertAgentIntoMobsimFromUpdate(mobsimDriverAgent);
			mobsimDriverAgent.myNotifyMoveOverNode(nextLinkId, personLinkIndex, planIndex);
		}
		// -->
		this.context.getEventsManager().processEvent(new LinkEnterEvent(now, veh.getId(), nextLinkId ));
		// <--
		nextQueueLane.addFromUpstream(veh);
	}

	@Override
	public void handleAccepted(List<AcceptedVehiclesDto> accepted) {
		if (accepted.isEmpty())
			return;
		//todo do mapy to
		AcceptedVehiclesDto firstAccepted = accepted.get(0);
		Id<Link> linkId = firstAccepted.getLinkId();
		Id<Lane> laneId = firstAccepted.getLaneId();

		//todo mapa
		QLinkI link = netsimEngine.getNetsimNetwork().getNetsimLinks().get(linkId);
		QLaneI lane = link.getOfferingQLanes().stream().filter(l -> l.getId().equals(laneId)).findFirst().get();

		for (AcceptedVehiclesDto acceptedVehicle : accepted) {

			//todo to tylko sprawdzenie czy działa
			QVehicle firstVehicle = lane.getFirstVehicle();
			if (!firstVehicle.getId().equals(acceptedVehicle.getVehicleId())) {
				throw new RuntimeException("Invalid id");
			}
			QVehicle poppedVeh = lane.popFirstVehicle();
			netsimEngine.getQSim().removeAgent(poppedVeh.getDriver().getId());
			poppedVeh.getDriver();
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


		//////




		if (nextQueueLane.isAcceptingFromUpstream()) {
			moveVehicleFromInlinkToOutlink(veh, currentLink.getId(), fromLane, nextLinkId, nextQueueLane);
			return true;
		}

		///łatwiej byłoby remove, trzeba dodatkowych sprawdzeń
		if (vehicleIsStuck(fromLane, now)) {
			/* We just push the vehicle further after stucktime is over, regardless
			 * of if there is space on the next link or not.. optionally we let them
			 * die here, we have a config setting for that!
			 */
			if (this.context.qsimConfig.isRemoveStuckVehicles()) {
				moveVehicleFromInlinkToAbort(veh, fromLane, now, currentLink.getId());
				return false ;
			} else {
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
//		Logger.getRootLogger().info("Aborting vehicle " + veh.getId());
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

		////todo
//		te. The score is calculated and stored in the plan aer its execution
//		by the mobility simulation during the scoring stag
		veh.getDriver().notifyMoveOverNode( nextLinkId );
		
		// -->
		this.context.getEventsManager().processEvent(new LinkEnterEvent(now, veh.getId(), nextLinkId ));
		// <--
		nextQueueLane.addFromUpstream(veh);
	}
	
	private boolean vehicleIsStuck(final QLaneI fromLaneBuffer, final double now) {
		//		final double stuckTime = network.simEngine.getStuckTime();
		final double stuckTime = this.context.qsimConfig.getStuckTime() ;
		return (now - fromLaneBuffer.getLastMovementTimeOfFirstVehicle()) > stuckTime;
	}
	

}
