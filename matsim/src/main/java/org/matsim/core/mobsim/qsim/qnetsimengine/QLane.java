/* *********************************************************************** *
 * project: org.matsim.*
 * QueueLane
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.LaneLeaveEvent;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.lanes.data.v20.LaneData20;
import org.matsim.lanes.vis.VisLane;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;


/**
 * A QueueLane has no own active state and only offers isActive() for a
 * stateless check for activation, a QueueLink is active as long as at least one
 * of its QueueLanes is active.  [[hm.  I my intuition, either it does have a state (active or not), or it doesn't. kai]]
 * <p/>
 *
 * @author dgrether based on prior QueueLink implementations of
 * @author dstrippgen
 * @author aneumann
 * @author mrieser
 */
public final class QLane extends QueueWithBuffer implements Identifiable {
	// this has public material without any kind of interface since it is accessed via qLink.get*Lane*() (in some not-yet-finalized
	// syntax).  kai, aug'10
	// yyyy This should almost certainly use composition/delegation instead of inheritance.  kai, sep'13
	// yyyy The VisDataImpl still has significant code overlap with QueueWithBuffer. kai, sep'13

	private static final Logger log = Logger.getLogger(QLane.class);

	/**
	 * This collection contains all Lanes downstream, if null it is the last lane
	 * within a QueueLink.
	 */
	private List<QLane> toLanes = null;

	/**
	 * Contains all Link instances which are reachable from this lane
	 */
	private final Set<Id> destinationLinkIds = new LinkedHashSet<Id>();

	private final LaneData20 laneData;

	private Map<Id, List<QLane>> toLinkToQLanesMap = null;

	private VisDataImpl visData = new VisDataImpl() ;
	
	/*package*/ QLane(final NetsimLink ql, LaneData20 laneData, boolean isFirstLaneOnLink) {
		super( (AbstractQLink) ql, new FIFOVehicleQ() ) ;
		this.isFirstLane = isFirstLaneOnLink;
		this.laneData = laneData;
	}

	/*package*/ void finishInitialization() {
		//do some indexing
		if (this.toLanes != null){
			this.toLinkToQLanesMap = new HashMap<Id, List<QLane>>(this.getDestinationLinkIds().size());
			for (QLane toLane : this.toLanes){
				for (Id toLinkId : toLane.getDestinationLinkIds()){
					if (!this.toLinkToQLanesMap.containsKey(toLinkId)){
						this.toLinkToQLanesMap.put(toLinkId, new ArrayList<QLane>());
					}
					this.toLinkToQLanesMap.get(toLinkId).add(toLane);
				}
			}
		}
		
		this.flowcap_accumulate = (this.flowCapacityPerTimeStepFractionalPart == 0.0 ? 0.0 : 1.0);
		// (needs to be re-done since flow cap changes after this was done for the first time)
		
		this.visData = new VisDataImpl();
	}

	@Override
	public Id getId(){
		// lane has its own id.  kai, jun'13
		return this.laneData.getId();
	}

	void setEndsAtMetersFromLinkEnd(final double meters) {
		this.endsAtMetersFromLinkEnd = meters;
	}

	double getEndsAtMeterFromLinkEnd(){
		return this.endsAtMetersFromLinkEnd;
	}

	boolean isThisTimeStepGreen(){
		return this.thisTimeStepGreen ;
	}

	/** called from framework, do everything related to link movement here
	 *
	 * @param now current time step
	 * @return true if there is at least one vehicle moved to another lane
	 */
	@Override
	public boolean doSimStep(final double now) {
		updateRemainingFlowCapacity();

		moveLaneToBuffer(now);

		// move vehicles from buffer to next lane if there is one.
		boolean isOtherLaneActive = false;
		if ( this.toLanes != null ) {
			isOtherLaneActive = moveBufferToNextLane( now ) ;
		 }
		return isOtherLaneActive;
	}

	private QLane chooseNextLane(Id toLinkId){
		List<QLane> nextLanes = this.toLinkToQLanesMap.get(toLinkId);
		if (nextLanes.size() == 1){
			return nextLanes.get(0);
		}
		//else chose lane by storage cap
		QLane retLane = nextLanes.get(0);
		for (QLane l : nextLanes) {
			if (l.usedStorageCapacity < retLane.usedStorageCapacity){
				retLane = l;
			}
		}
		return retLane;
	}

	private boolean moveBufferToNextLane(final double now) {
		boolean movedAtLeastOne = false;
		QVehicle veh;
		while ((veh = this.buffer.peek()) != null) {
			Id nextLinkId = veh.getDriver().chooseNextLinkId();
			QLane toQueueLane = null;
			toQueueLane = this.chooseNextLane(nextLinkId);
			if (toQueueLane != null) {
				if (toQueueLane.isAcceptingFromUpstream()) {
					this.buffer.poll();
					this.usedBufferStorageCapacity -= veh.getSizeInEquivalents() ;
					this.qLink.network.simEngine.getMobsim().getEventsManager().processEvent(
							new LaneLeaveEvent(now, veh.getDriver().getId(), this.qLink.getLink().getId(), this.getId()));
//					toQueueLane.addFromPreviousLane(veh);
					toQueueLane.addFromUpstream(veh);
					movedAtLeastOne = true;
				}
				else {
					return movedAtLeastOne;
				}
			}
			else { //error handling
				StringBuilder b = new StringBuilder();
				b.append("Person Id: ");
				b.append(veh.getDriver().getId());
				b.append(" is on Lane Id ");
				b.append(this.getLaneData().getId());
				b.append(" on Link Id ");
				b.append(this.qLink.getLink().getId());
				b.append(" and wants to go on to Link Id ");
				b.append(nextLinkId);
				b.append(" but there is no Lane leading to that Link!");
				log.error(b.toString());
				throw new IllegalStateException(b.toString());
			}
		} // end while
		return movedAtLeastOne;
	}

	 void setGeneratingEvents(final boolean fireLaneEvents) {
		this.generatingEvents = fireLaneEvents;
	}

	 void addAToLane(final QLane lane) {
		 // this is needed since the movement from one lane to the next is done internally. kai, jun'13
		if (this.toLanes == null) {
			this.toLanes = new LinkedList<QLane>();
		}
		this.toLanes.add(lane);
	}

	 List<QLane> getToLanes(){
		 // this is needed to that QLinkLanesImpl can figure out the destination link ... but should be possible to do this
		 // internally inside QLane. kai, jun'13
		return this.toLanes;
	}

	 void addDestinationLink(final Id linkId) {
		 // in middle of link, need to know which turning lane to take.  Need to know this from only knowing the next
		 // link in the route.  thus need to know which lane leads to which link.
		 // yy might want to have this knowledge in the wrapper, rather than here. kai, jun'13
		this.destinationLinkIds.add(linkId);
	}

	Set<Id> getDestinationLinkIds(){
		// see above (somehow, we seem to be getting link ids from lanes and then lanes from link ids--???)
		return Collections.unmodifiableSet(destinationLinkIds);
	}

	public double getLength(){
		// needed once, by OTFVis
		return this.length;
	}

	 LaneData20 getLaneData() {
		return this.laneData;
	}

	/**
	 * Inner class to capsulate visualization methods
	 *
	 * @author dgrether
	 */
	class VisDataImpl extends QueueWithBuffer.VisDataImpl {
		VisLane visLane ;
		
		VisDataImpl(){
		}
		
//		@Override
//		public Collection<AgentSnapshotInfo> getAgentSnapshotInfo( final Collection<AgentSnapshotInfo> positions) {
//			AgentSnapshotInfoBuilder snapshotInfoBuilder = QLane.this.qLink.network.simEngine.getAgentSnapshotInfoBuilder();
//			
//			double numberOfVehiclesDriving = QLane.this.buffer.size() + QLane.this.vehQueue.size();
//			if (numberOfVehiclesDriving > 0) {
//				double now = QLane.this.qLink.network.simEngine.getMobsim().getSimTimer().getTimeOfDay();
//				Link link = QLane.this.qLink.getLink();
//				double spacing = snapshotInfoBuilder.calculateVehicleSpacing(QLane.this.length, numberOfVehiclesDriving,
//						QLane.this.storageCapacity, QLane.this.bufferStorageCapacity); 
//				double freespeedTraveltime = QLane.this.freespeedTravelTime;
//				
//				double lastDistanceFromFromNode = Double.NaN;
//				for (QVehicle veh : QLane.this.buffer){
//					lastDistanceFromFromNode = this.createAndAddVehiclePositionAndReturnDistance(positions, snapshotInfoBuilder, now, 
//							lastDistanceFromFromNode, link, spacing, freespeedTraveltime, veh);
//				}
//				for (QVehicle veh : QLane.this.vehQueue) {
//					lastDistanceFromFromNode = this.createAndAddVehiclePositionAndReturnDistance(positions, snapshotInfoBuilder, now, 
//							lastDistanceFromFromNode, link, spacing, freespeedTraveltime, veh);
//				}
//			}
//			return positions;
//		}
		
		@Override
		double createAndAddVehiclePositionAndReturnDistance(final Collection<AgentSnapshotInfo> positions,
				AgentSnapshotInfoBuilder snapshotInfoBuilder, double now, double lastDistanceFromFromNode, Link link,
				double spacing, double freespeedTraveltime, QVehicle veh){
			double remainingTravelTime = veh.getEarliestLinkExitTime() - now ;
			lastDistanceFromFromNode = snapshotInfoBuilder.calculateDistanceOnVectorFromFromNode2(QLane.this.length, spacing, 
					lastDistanceFromFromNode, now, freespeedTraveltime, remainingTravelTime);
			double speedValue = snapshotInfoBuilder.calcSpeedValueBetweenZeroAndOne(veh, 
					QLane.this.getInverseSimulatedFlowCapacity(), now, link.getFreespeed());
			if (this.visLane.getNumberOfLanes() < 2.0){
				snapshotInfoBuilder.createAndAddVehiclePosition(positions, this.visLane.getStartCoord(), this.visLane.getEndCoord(), 
						QLane.this.length, this.visLane.getEuklideanDistance(), veh, 
						lastDistanceFromFromNode, null, speedValue);
			}
			else {
				int noLanes = (int) this.visLane.getNumberOfLanes();
				int lane = snapshotInfoBuilder.guessLane(veh, noLanes);
				Tuple<Coord, Coord> startEndCoord = this.visLane.getDrivingLaneStartEndCoord(lane);
				snapshotInfoBuilder.createAndAddVehiclePosition(positions, startEndCoord.getFirst(), startEndCoord.getSecond(), 
						QLane.this.length, this.visLane.getEuklideanDistance(), veh, 
						lastDistanceFromFromNode, null, speedValue);
			}
			return lastDistanceFromFromNode;
		}
		
	}

	double getInverseSimulatedFlowCapacity() {
		return this.inverseFlowCapacityPerTimeStep ;
	}

	void setOTFLane(VisLane otfLane) {
		this.visData.visLane = otfLane;
	}

}



