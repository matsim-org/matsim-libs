/* *********************************************************************** *
 * project: org.matsim.*
 * LanesUtils
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package org.matsim.lanes.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.lanes.ModelLane;
import org.matsim.lanes.data.v11.LaneData11;
import org.matsim.lanes.data.v11.LaneDefinitionsFactory11;
import org.matsim.lanes.data.v11.LanesToLinkAssignment11;
import org.matsim.lanes.data.v20.Lane;
import org.matsim.lanes.data.v20.LaneData20MeterFromLinkEndComparator;
import org.matsim.lanes.data.v20.LaneDefinitionsFactory20;
import org.matsim.lanes.data.v20.LanesToLinkAssignment20;

/**
 * @author dgrether
 * @author tthunig
 * 
 */
public final class LanesUtils {

	private static final LaneFromLinkEndComparator fromLinkEndComparator = new LaneFromLinkEndComparator();
	
	/**
	 * Convenience method to create a format 11 lane with the given Id, the
	 * given length, the given number of represented lanes and the given Ids of
	 * the downstream links the lane leads to. The Lane is added to the
	 * LanesToLinkAssignment given as parameter.
	 * 
	 * @param l2l
	 * @param factory
	 * @param laneId
	 * @param length
	 * @param noLanes
	 * @param toLinkIds
	 */
	public static void createAndAddLane11(LanesToLinkAssignment11 l2l, LaneDefinitionsFactory11 factory, Id<Lane> laneId, 
			double length, 	double noLanes, Id<Link>... toLinkIds) {
		LaneData11 lane = factory.createLane(laneId);
		for (Id<Link> toLinkId : toLinkIds) {
			lane.addToLinkId(toLinkId);
		}
		lane.setStartsAtMeterFromLinkEnd(length);
		lane.setNumberOfRepresentedLanes(noLanes);
		l2l.addLane(lane);
	}
	
	/**
	 * Convenience method to create a format 20 lane with the given Id, the given length,
	 * the given capacity, the given number of represented lanes, the given
	 * alignment and the given Ids of the downstream links or lanes, respectively,
	 * the lane leads to. The lane is added to the LanesToLinkAssignment given
	 * as parameter.
	 * 
	 * @param l2l
	 *            the LanesToLinkAssignment to that the created lane is added
	 * @param factory
	 *            a LaneDefinitionsFactory20 to create the lane
	 * @param laneId
	 * @param capacity
	 * @param startsAtMeterFromLinkEnd
	 * @param alignment
	 * @param numberOfRepresentedLanes
	 * @param toLinkIds
	 * @param toLaneIds
	 */
	public static void createAndAddLane20(LanesToLinkAssignment20 l2l,
			LaneDefinitionsFactory20 factory, Id<Lane> laneId, double capacity,
			double startsAtMeterFromLinkEnd, int alignment,
			int numberOfRepresentedLanes, List<Id<Link>> toLinkIds, List<Id<Lane>> toLaneIds) {
		
		Lane lane = factory.createLane(laneId);
		if (toLinkIds != null){
			for (Id<Link> toLinkId : toLinkIds) {
				lane.addToLinkId(toLinkId);
			}
		}
		if (toLaneIds != null){
			for (Id<Lane> toLaneId : toLaneIds) {
				lane.addToLaneId(toLaneId);
			}
		}
		lane.setCapacityVehiclesPerHour(capacity);
		lane.setStartsAtMeterFromLinkEnd(startsAtMeterFromLinkEnd);
		lane.setNumberOfRepresentedLanes(numberOfRepresentedLanes);
		lane.setAlignment(alignment); 
		l2l.addLane(lane);
	}
	
	/**
	 * Creates a sorted list of lanes for a link. 
	 * @param link
	 * @param lanesToLinkAssignment
	 * @return sorted list with the most upstream lane at the first position. 
	 */
	public static List<ModelLane> createLanes(Link link, LanesToLinkAssignment20 lanesToLinkAssignment) {
		List<ModelLane> queueLanes = new ArrayList<ModelLane>();
		List<Lane> sortedLanes =  new ArrayList<Lane>(lanesToLinkAssignment.getLanes().values());
		Collections.sort(sortedLanes, new LaneData20MeterFromLinkEndComparator());
		Collections.reverse(sortedLanes);

		List<ModelLane> laneList = new LinkedList<ModelLane>();
		Lane firstLane = sortedLanes.remove(0);
		if (firstLane.getStartsAtMeterFromLinkEnd() != link.getLength()) {
			throw new IllegalStateException("First Lane Id " + firstLane.getId() + " on Link Id " + link.getId() +
			"isn't starting at the beginning of the link!");
		}
		ModelLane firstQLane = new ModelLane(firstLane);
		laneList.add(firstQLane);
		Stack<ModelLane> laneStack = new Stack<ModelLane>();

		while (!laneList.isEmpty()){
			ModelLane lastQLane = laneList.remove(0);
			laneStack.push(lastQLane);
			queueLanes.add(lastQLane);

			//if existing create the subsequent lanes
			List<Id<Lane>> toLaneIds = lastQLane.getLaneData().getToLaneIds();
			double nextMetersFromLinkEnd = 0.0;
			double laneLength = 0.0;
			if (toLaneIds != null 	&& (!toLaneIds.isEmpty())) {
				for (Id<Lane> toLaneId : toLaneIds){
					Lane currentLane = lanesToLinkAssignment.getLanes().get(toLaneId);
					nextMetersFromLinkEnd = currentLane.getStartsAtMeterFromLinkEnd();
					ModelLane currentQLane = new ModelLane(currentLane);
					laneList.add(currentQLane);
					lastQLane.addAToLane(currentQLane);
				}
				laneLength = lastQLane.getLaneData().getStartsAtMeterFromLinkEnd() - nextMetersFromLinkEnd;
				lastQLane.setEndsAtMetersFromLinkEnd(nextMetersFromLinkEnd);
			}
			//there are no subsequent lanes
			else {
				laneLength = lastQLane.getLaneData().getStartsAtMeterFromLinkEnd();
				lastQLane.setEndsAtMetersFromLinkEnd(0.0);
			}
			lastQLane.changeLength(laneLength);
		}

		//fill toLinks
		while (! laneStack.isEmpty()){
			ModelLane qLane = laneStack.pop();
			if (qLane.getToLanes() == null || (qLane.getToLanes().isEmpty())) {
				for (Id<Link> toLinkId : qLane.getLaneData().getToLinkIds()){
					qLane.addDestinationLink(toLinkId);
				}
			}
			else {
				for (ModelLane subsequentLane : qLane.getToLanes()){
					for (Id<Link> toLinkId : subsequentLane.getDestinationLinkIds()){
						qLane.addDestinationLink(toLinkId);
					}
				}
			}
		}

		Collections.sort(queueLanes, fromLinkEndComparator);
		return queueLanes;
	}

}
