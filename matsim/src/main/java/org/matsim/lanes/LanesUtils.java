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
package org.matsim.lanes;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.*;

/**
 * @author dgrether
 * @author tthunig
 * 
 */
public final class LanesUtils {

	public static Lanes createLanesContainer(){
		return new LanesImpl();
	}

	/**
	 * Convenience method to create a lane with the given Id, the given length,
	 * the given capacity, the given number of represented lanes, the given
	 * alignment and the given Ids of the downstream links or lanes, respectively,
	 * the lane leads to. The lane is added to the LanesToLinkAssignment given
	 * as parameter.
	 * 
	 * @param l2l
	 *            the LanesToLinkAssignment to that the created lane is added
	 * @param factory
	 *            a LaneDefinitionsFactory to create the lane
	 * @param laneId
	 * @param capacity
	 * @param startsAtMeterFromLinkEnd
	 * @param alignment
	 * @param numberOfRepresentedLanes
	 * @param toLinkIds
	 * @param toLaneIds
	 */
	public static void createAndAddLane(LanesToLinkAssignment l2l,
			LanesFactory factory, Id<Lane> laneId, double capacity,
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
	 * Replaces the method that converted a lane from format 11 to format 20.
	 * Use this when you have not defined an original lane of the link and when you have not set lane capacities yet.
	 */
	public static void createOriginalLanesAndSetLaneCapacities(Network network, Lanes lanes){
		LanesFactory factory = lanes.getFactory();
		for (LanesToLinkAssignment l2l : lanes.getLanesToLinkAssignments().values()){
			Link link = network.getLinks().get(l2l.getLinkId());

			Lane olLane = factory.createLane(Id.create(l2l.getLinkId().toString() + ".ol", Lane.class));
			l2l.addLane(olLane);
			for (Lane lane : l2l.getLanes().values()) {
				olLane.addToLaneId(lane.getId());

				//set capacity of the lane depending on link capacity and number of representative lanes
				LanesUtils.calculateAndSetCapacity(lane, true, link, network);
			}
			olLane.setNumberOfRepresentedLanes(link.getNumberOfLanes());
			olLane.setStartsAtMeterFromLinkEnd(link.getLength());
		}
	}
	
	/**
	 * Creates a sorted list of lanes for a link. 
	 * @param link
	 * @param lanesToLinkAssignment
	 * @return sorted list with the most upstream lane at the first position. 
	 */
	public static List<ModelLane> createLanes(Link link, LanesToLinkAssignment lanesToLinkAssignment) {
		List<ModelLane> queueLanes = new ArrayList<>();
		List<Lane> sortedLanes =  new ArrayList<>(lanesToLinkAssignment.getLanes().values());

		// orders lanes by start on link an whether they are outgoing or not o the start is the same
		sortedLanes.sort(Comparator.comparingDouble(Lane::getStartsAtMeterFromLinkEnd).thenComparing(
				(l1, l2) -> {
					boolean l1Outgoing = l1.getToLinkIds() != null && !l1.getToLinkIds().isEmpty();
					boolean l2Outgoing = l2.getToLinkIds() != null && !l2.getToLinkIds().isEmpty();
					if (l1Outgoing && !l2Outgoing)
						return -1;
					else if(l2Outgoing && !l1Outgoing)
						return 1;
					else
						return 0;
				}
		));
		Collections.reverse(sortedLanes);

		List<ModelLane> laneList = new LinkedList<>();
		Lane firstLane = sortedLanes.remove(0);
		if (firstLane.getStartsAtMeterFromLinkEnd() != link.getLength()) {
			throw new IllegalStateException("First Lane Id " + firstLane.getId() + " on Link Id " + link.getId() +
			"isn't starting at the beginning of the link!");
		}
		ModelLane firstQLane = new ModelLane(firstLane);
		laneList.add(firstQLane);
		Stack<ModelLane> laneStack = new Stack<>();

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
			lastQLane.setLength(laneLength);
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

		Collections.sort(queueLanes, new Comparator<ModelLane>() {
			@Override
			public int compare(ModelLane o1, ModelLane o2) {
				if (o1.getEndsAtMeterFromLinkEnd() < o2.getEndsAtMeterFromLinkEnd()) {
					return -1;
				} else if (o1.getEndsAtMeterFromLinkEnd() > o2.getEndsAtMeterFromLinkEnd()) {
					return 1;
				} else {
					return 0;
				}
			}
		});
		return queueLanes;
	}

	/**
	 * Calculate capacity by formular from Neumann2008DA:
	 *
	 * Flow of a Lane is given by the flow of the link divided by the number of lanes represented by the link.
	 *
	 * A Lane may represent one or more lanes in reality. This is given by the attribute numberOfRepresentedLanes of
	 * the Lane definition. The flow of a lane is scaled by this number.
	 */
	public static void calculateAndSetCapacity(Lane lane, boolean isLaneAtLinkEnd, Link link, Network network){
		if (isLaneAtLinkEnd){
			double noLanesLink = link.getNumberOfLanes();
			double linkFlowCapPerSecondPerLane = link.getCapacity() / network.getCapacityPeriod()
					/ noLanesLink;
			double laneFlowCapPerHour = lane.getNumberOfRepresentedLanes()
					* linkFlowCapPerSecondPerLane * 3600.0;
			lane.setCapacityVehiclesPerHour(laneFlowCapPerHour);
		}
		else {
			double capacity = link.getCapacity() / network.getCapacityPeriod() * 3600.0;
			lane.setCapacityVehiclesPerHour(capacity);
		}
	}

	public static void calculateMissingCapacitiesForLanes20(String networkInputFilename, String lanes20InputFilename, String lanes20OutputFilename){
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(networkInputFilename);
		config.qsim().setUseLanes(true);
		config.network().setLaneDefinitionsFile(lanes20InputFilename);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Network network = scenario.getNetwork();
		Lanes lanes = scenario.getLanes();
		for (LanesToLinkAssignment l2l : lanes.getLanesToLinkAssignments().values()){
			Link link = network.getLinks().get(l2l.getLinkId());
			for (Lane lane : l2l.getLanes().values()){
				if (lane.getToLaneIds() == null || lane.getToLaneIds().isEmpty()){
					calculateAndSetCapacity(lane, true, link, network);
				}
				else {
					calculateAndSetCapacity(lane, false, link, network);
				}
			}
		}
		LanesWriter writerDelegate = new LanesWriter(lanes);
		writerDelegate.write(lanes20OutputFilename);
	}
	
	public static void overwriteLaneCapacitiesByNetworkCapacities(Network net, Lanes lanes) {
		for (LanesToLinkAssignment linkLanes : lanes.getLanesToLinkAssignments().values()) {
			double linkCap = net.getLinks().get(linkLanes.getLinkId()).getCapacity();
			for (Lane lane : linkLanes.getLanes().values()) {
				lane.setCapacityVehiclesPerHour(linkCap);
			}
		}
	}
}
