/* *********************************************************************** *
 * project: org.matsim.*
 * LaneDefinitionsV11ToV20Conversion
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package org.matsim.lanes.data.v11;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.internal.MatsimComparator;
import org.matsim.core.network.NetworkUtils;
import org.matsim.lanes.LanesUtils;
import org.matsim.lanes.data.v20.Lane;
import org.matsim.lanes.data.v20.Lanes;
import org.matsim.lanes.data.v20.LaneDefinitions20Impl;
import org.matsim.lanes.data.v20.LaneDefinitionsFactory20;

import java.io.Serializable;
import java.util.*;


/**
 * Converts LaneDefinitions that have been read from a xml file in the lanedefinitions_v1.1.xsd
 * to LaneDefinitions that have all attributes set used in the lanedefinitions_v2.0.xsd file format.
 * 
 * In the v1.1 format only the Lanes at the end of a link are specified but not the ones at the 
 * beginning of the link that lead to the Lanes at the end of the link. Furthermore there is no
 * explicit U-Turn functionality expected to be modeled in the v1.1 format. Also not existing in
 * v1.1 is information about the topological order of the Lanes on the link. All this information is 
 * computed heuristically by this converter.
 * 
 * This means:
 * <ul>
 *   <li>One or more Lanes are created that lead from the beginning of the link to the Lanes at
 *   the end of the link.</li>
 *   <li>Based on the geometry information in the network graph topology information is added.</li>
 *   <li>Optional: To the lane which is the most left one (looking south to north on the link) a additional out 
 *   link is added to enable U-Turn functionality.</li>
 * </ul>
 * 
 * @author dgrether
 * @author aneumann
 */
public abstract class LaneDefinitionsV11ToV20Conversion {
	
//	private static final Logger log = Logger.getLogger(LaneDefinitionsV11ToV20Conversion.class);

	/**
	 * Specifies which type of UTurn is created. The interpretation of 
	 * left or right lane depends on the coordinate system and the
	 * behavior of NetworkUtils.getOutLinksSortedByAngle()
	 * 
	 * @author dgrether
	 *
	 */
	public enum UTurnCreation {OFF, ON_LEFT_LANE, ON_RIGHT_LANE}
	
	/**
	 * Syntactic sugar to keep old code running. 
	 */
	public static void convertTo20(LaneDefinitions11 in, Lanes out, Network network) {
		convertTo20(in, out, network, UTurnCreation.ON_LEFT_LANE);
	}	
	
	public static void convertTo20(LaneDefinitions11 in, Lanes out, Network network, UTurnCreation uturn) {
		LaneDefinitionsFactory20 lanedefs20fac = out.getFactory();
		org.matsim.lanes.data.v20.LanesToLinkAssignment20 l2lnew;
		Lane lanev20;
		Link link;
		for (LanesToLinkAssignment11 l2lv11 : in.getLanesToLinkAssignments().values()){
			//create the lane2linkassignment
			l2lnew = lanedefs20fac.createLanesToLinkAssignment(l2lv11.getLinkId());
			link = network.getLinks().get(l2lv11.getLinkId());
			out.addLanesToLinkAssignment(l2lnew);
			//create the already in 1.1 defined lanes and add them to the 2.0 format objects
			for (LaneData11 lanev11 : l2lv11.getLanes().values()){
				lanev20 = lanedefs20fac.createLane(lanev11.getId());
				l2lnew.addLane(lanev20);
				//copy values
				lanev20.setNumberOfRepresentedLanes(lanev11.getNumberOfRepresentedLanes());
				lanev20.setStartsAtMeterFromLinkEnd(lanev11.getStartsAtMeterFromLinkEnd());
				for (Id<Link> toLinkId : lanev11.getToLinkIds()){
					lanev20.addToLinkId(toLinkId);
				}
				LanesUtils.calculateAndSetCapacity(lanev20, true, link, network);
			}
			//further processing of not defined lanes in 1.1 format
			//add original lane
			List<Lane> sortedLanes =  new ArrayList<Lane>(l2lnew.getLanes().values());
			Collections.sort(sortedLanes, new LaneData20MeterFromLinkEndComparator());
			Lane longestLane = sortedLanes.get(sortedLanes.size()-1);
//			double originalLaneLength = link.getLength() - longestLane.getStartsAtMeterFromLinkEnd();
			String originalLaneIdString = link.getId().toString() + ".ol";
			Lane originalLane = lanedefs20fac.createLane(Id.create(originalLaneIdString, Lane.class));
			originalLane.setNumberOfRepresentedLanes(link.getNumberOfLanes());
			originalLane.setStartsAtMeterFromLinkEnd(link.getLength());
			originalLane.addToLaneId(longestLane.getId());
			LanesUtils.calculateAndSetCapacity(originalLane, false, link, network);
			l2lnew.addLane(originalLane);

			//add other lanes
			Lane lastLane = originalLane;
			Lane secondLongestLane;
			Lane intermediateLane;
			Id<Lane> intermediateLaneId;
			int intermediateLanesCounter = 1;
			for (int i = sortedLanes.size() - 2; i >= 0; i--){ //sortedLanes.size() and sortedLanes.size()-1 are already used, so start at -2
				secondLongestLane = sortedLanes.get(i);
				if (longestLane.getStartsAtMeterFromLinkEnd() > secondLongestLane.getStartsAtMeterFromLinkEnd()){
					//create intermediate lane
					intermediateLaneId = Id.create(intermediateLanesCounter + ".cl", Lane.class);
					intermediateLanesCounter++;
					intermediateLane = lanedefs20fac.createLane(intermediateLaneId);
					//intermdiateLane needs values as startsAt and represented number of lanes
					intermediateLane.setStartsAtMeterFromLinkEnd(longestLane.getStartsAtMeterFromLinkEnd());
					intermediateLane.setNumberOfRepresentedLanes(link.getNumberOfLanes());
					intermediateLane.addToLaneId(secondLongestLane.getId());
					LanesUtils.calculateAndSetCapacity(intermediateLane, false, link, network);
					l2lnew.addLane(intermediateLane);
					lastLane.addToLaneId(intermediateLaneId);
					lastLane = intermediateLane;
					longestLane = secondLongestLane;
				}
				else if (longestLane.getStartsAtMeterFromLinkEnd() == secondLongestLane.getStartsAtMeterFromLinkEnd()){
					//this case is rather easy, just add the toLaneId and proceed
					lastLane.addToLaneId(secondLongestLane.getId());
				}
				else {
					throw new RuntimeException("Illegal sort order");
				}
			}


			//calculate the alignment and uturn
			int mostRight = l2lv11.getLanes().size() / 2;
			SortedMap<Double, Link> outLinksByAngle = NetworkUtils.getOutLinksSortedByAngle(link);
			Lane newLane;
			Set<LaneData11> assignedLanes = new HashSet<LaneData11>();
			for (Link outlink : outLinksByAngle.values()){
//				log.info("Outlink: " + outlink.getId());
				for (LaneData11 oldLane : l2lv11.getLanes().values()){
//					log.info("lane: " + oldLane.getId());
					if (assignedLanes.contains(oldLane)){
						continue;
					}
					newLane = l2lnew.getLanes().get(oldLane.getId());

					//add uturn functionality if the first lane is processed, i.e. the most left lane that is indicated by an empty set of assignedLanes
					if (UTurnCreation.ON_LEFT_LANE.equals(uturn) && assignedLanes.isEmpty()){
						addUTurn(link, newLane);
					}
					else if (UTurnCreation.ON_RIGHT_LANE.equals(uturn) && 
							(assignedLanes.size() == (l2lv11.getLanes().size() - 1))) {
						addUTurn(outlink, newLane);
					}

					if (newLane.getToLinkIds().contains(outlink.getId())){
//						log.info("lane " + newLane.getId() + "  alignment: " + mostRight);
						newLane.setAlignment(mostRight);
						assignedLanes.add(oldLane);
						//decrement mostRight skip 0 if number of lanes is even
						mostRight--;
						if ((mostRight == 0) && (l2lv11.getLanes().size() % 2  == 0)){
							mostRight--;
						}
					}
				}
			}
		}
	}

	/**
	 * Syntactic sugar to keep old code running. 
	 */
	public static Lanes convertTo20(LaneDefinitions11 lanedefs11, Network network) {
		return convertTo20(lanedefs11, network, UTurnCreation.ON_LEFT_LANE);
	}
	
	public static Lanes convertTo20(LaneDefinitions11 lanedefs11, Network network, UTurnCreation uturns) {
		Lanes lanedefs20 = new LaneDefinitions20Impl();
		convertTo20(lanedefs11, lanedefs20, network, uturns);
		return lanedefs20;
	}
	
	private static void addUTurn(Link link, Lane newLane) {
		for (Link outLink : link.getToNode().getOutLinks().values()) {
			if ((outLink.getToNode().equals(link.getFromNode()))) {
//				log.info("Added uturn, i.e. turning move from link " + link.getId() + " lane " + newLane.getId() + " to link " + outLink.getId());
				newLane.addToLinkId(outLink.getId());
			}
		}
	}

	static class LaneData20MeterFromLinkEndComparator implements Comparator<Lane>, Serializable, MatsimComparator {

		private static final long serialVersionUID = 1L;

		@Override
		public int compare(Lane o1, Lane o2) {
			if (o1.getStartsAtMeterFromLinkEnd() < o2.getStartsAtMeterFromLinkEnd()) {
				return -1;
			} else if (o1.getStartsAtMeterFromLinkEnd() > o2.getStartsAtMeterFromLinkEnd()) {
				return 1;
			} else {
				return 0;
			}
		}
	}
	
}
