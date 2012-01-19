/* *********************************************************************** *
 * project: org.matsim.*
 * WholesAgentSnapshotInfoBuilder
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package org.matsim.ptproject.qsim.qnetsimengine;

import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.internal.MatsimComparator;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;


/**
 * @deprecated seems to be really experimental code that was never completely finished, dg sep. 2011
 */
@Deprecated
public class WholesAgentSnapshotInfoBuilder extends QueueAgentSnapshotInfoBuilder {

	/*package*/ static class TupleDoubleComparator implements Comparator<Tuple<Double, QItem>>, Serializable, MatsimComparator {

		private static final long serialVersionUID = 1L;

		@Override
		public int compare(final Tuple<Double, QItem> o1, final Tuple<Double, QItem> o2) {
			int ret = - o1.getFirst().compareTo(o2.getFirst()); // the minus should, in theory, sort by decreasing "Double"
			if (ret == 0) {
				ret = o2.getSecond().toString().compareTo(o1.getSecond().toString());
			}
			return ret;
		}
	}

	
	public WholesAgentSnapshotInfoBuilder(Scenario scenario) {
		super(scenario);
	}
	
	public void addVehiclePositions(VisLane visLane, Collection<AgentSnapshotInfo> positions,
			Collection<QVehicle> buffer, Collection<QVehicle> vehQueue, Collection<QItem> holes,
			double linkLength, double offset, Integer laneNumber) {
		double storageCapacity = visLane.getStorageCapacity() ;
		double bufferStorageCapacity = visLane.getBufferStorage() ;

		double currentQueueEnd = linkLength; // queue end initialized at end of link

		// for holes: using the "queue" method, but with different vehSpacing:
//		float vehSpacing = (float) calculateVehicleSpacingAsQueue(linkLength, storageCapacity, bufferStorageCapacity);
		//		float vehSpacingWithHoles = (float) calculateVehicleSpacingWithHoles(linkLength, storageCapacity, bufferStorageCapacity,
		//				congestedDensity);

		// treat vehicles from buffer:
//		currentQueueEnd = positionVehiclesFromBufferAsQueue(positions, currentQueueEnd, vehSpacing, buffer, offset,
//				laneNumber, visLane);
//
//		// treat other driving vehicles:
//		positionOtherDrivingVehiclesWithHoles(positions, currentQueueEnd, vehSpacing, vehQueue, holes,
//				offset, laneNumber, visLane);

	}

	private void positionOtherDrivingVehiclesWithHoles(final Collection<AgentSnapshotInfo> positions, double queueEnd,
			double vehSpacing, Collection<QVehicle> vehQueue, Collection<QItem> holes, double offset, Integer laneNumber,
			VisLane visLane)
	{
//		if ( visLane instanceof QLane ) {
//			throw new RuntimeException("holes visualization is not implemented for lanes since I don't understand which "
//					+ "quantities refer to the link and which to the lane.  kai, nov'10" ) ;
//		}
//
//		double now = visLane.getQLink().network.simEngine.getMobsim().getSimTimer().getTimeOfDay() ;
//		Link  link = visLane.getQLink().getLink() ;
//
//		Queue<Tuple<Double, QItem>> qItemList = new PriorityQueue<Tuple<Double, QItem>>(30, new TupleDoubleComparator() );
//		for ( QVehicle veh : vehQueue ) {
//			double distanceOnLink_m = (now - veh.getLinkEnterTime() ) * link.getFreespeed(now) ;
//			qItemList.add( new Tuple<Double,QItem>( distanceOnLink_m, veh ) ) ;
//		}
//		for ( QItem hole : holes ) {
//			double distanceOnLink_m = ( hole.getEarliestLinkExitTime() - now ) * 15.*1000./3600. ;
//			// holes come from the end of the link; the remaining distance is the same as distanceOnLink_m
//			qItemList.add( new Tuple<Double,QItem>( distanceOnLink_m, hole ) ) ;
//		}
//
//		for (QVehicle veh : vehQueue) {
//			boolean inQueue = false ;
//			double distanceOnLink_m = (now - veh.getLinkEnterTime()) * link.getFreespeed(now) ;
//			if (distanceOnLink_m > queueEnd) { // vehicle is already in queue
//				distanceOnLink_m = queueEnd;
//				queueEnd -= vehSpacing;
//				inQueue = true ;
//			}
//
//			double speedValueBetweenZeroAndOne = 1. ;
//			if ( inQueue ) {
//				speedValueBetweenZeroAndOne = 0. ; // yy could be something more realistic than 0.  kai, nov'10
//			}
//
//			int lane ;
//			if (laneNumber == null){
//				lane  = calculateLane(veh, NetworkUtils.getNumberOfLanesAsInt(Time.UNDEFINED_TIME, link));
//			} else {
//				lane = laneNumber ;
//			}
//
//			List<MobsimAgent> peopleInVehicle = getPeopleInVehicle(veh);
//
//			this.createAndAddSnapshotInfoForPeopleInMovingVehicle(positions, peopleInVehicle, distanceOnLink_m, link, lane,
//					speedValueBetweenZeroAndOne, offset);
//		}
//		throw new RuntimeException("this is not (yet) finished; aborting ...") ;

	}
	//	private double calculateVehicleSpacingWithHoles(double linkLength, double storageCapacity, double bufferStorageCapacity,
	//			double congestedDensity_veh_m ) {
	//		// yyyyyy abusing congDens as nHolesMax!
	//		return 1.*(linkLength / (storageCapacity + bufferStorageCapacity - congestedDensity_veh_m )) ;
	//	}

}
