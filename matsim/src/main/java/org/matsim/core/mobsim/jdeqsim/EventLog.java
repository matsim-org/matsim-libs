/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.core.mobsim.jdeqsim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * This class is used for comparing results with the historical C++
 * implementation results of DEQSim.
 * 
 * @author rashid_waraich
 */
public class EventLog {
	double time = 0.0;
	int vehicleId = 0;
	int legNo = 0;
	int linkId = 0;
	int fromNodeId = 0;
	int toNodeId = 0;
	String type = null;

	public EventLog(double time, int vehicleId, int legNo, int linkId, int fromNodeId, int toNodeId, String type) {
		super();
		this.time = time;
		this.vehicleId = vehicleId;
		this.legNo = legNo;
		this.linkId = linkId;
		this.fromNodeId = fromNodeId;
		this.toNodeId = toNodeId;
		this.type = type;
	}

	public void print() {
		System.out.print("time: " + time);
		System.out.print(";vehicleId: " + vehicleId);
		System.out.print(";legNo: " + legNo);
		System.out.print(";linkId: " + linkId);
		System.out.print(";fromNodeId: " + fromNodeId);
		System.out.print(";toNodeId: " + toNodeId);
		System.out.print(";type: " + type);
		System.out.println();
	}

	public double getTime() {
		return time;
	}

	public void setTime(double time) {
		this.time = time;
	}

	public int getVehicleId() {
		return vehicleId;
	}

	public void setVehicleId(int vehicleId) {
		this.vehicleId = vehicleId;
	}

	public int getLegNo() {
		return legNo;
	}

	public void setLegNo(int legNo) {
		this.legNo = legNo;
	}

	public int getLinkId() {
		return linkId;
	}

	public void setLinkId(int linkId) {
		this.linkId = linkId;
	}

	public int getFromNodeId() {
		return fromNodeId;
	}

	public void setFromNodeId(int fromNodeId) {
		this.fromNodeId = fromNodeId;
	}

	public int getToNodeId() {
		return toNodeId;
	}

	public void setToNodeId(int toNodeId) {
		this.toNodeId = toNodeId;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public static boolean compare(ArrayList<EventLog> eventLog1, ArrayList<EventLog> eventLog2) {
		int NoOfNotEqualEvents = 0;

		assert eventLog1.size() == eventLog2.size() : "The size of both eventLogs must be the same!";
		for (int i = 0; i < eventLog1.size(); i++) {
			// System.out.println("=========");
			// eventLog1.get(i).print();
			// eventLog2.get(i).print();
			// System.out.println("=========");

			if (!equals(eventLog1.get(i), eventLog2.get(i))) {
				// NoOfNotEqualEvents++;
				return false;
			}
		}

		System.out.println("# Events Java: " + eventLog1.size());
		System.out.println("# Events C++: " + eventLog2.size());
		System.out.println("NoOfNotEqualEvents: " + NoOfNotEqualEvents);

		return true;
	}

	/**
	 * the time must be the same (compared up to 4 digits after the floating
	 * point) and the link the event type is ignored for the moment, because in
	 * the beginning it might be different
	 * 
	 * @param eventLog1
	 * @param eventLog2
	 * @return
	 */
	public static boolean equals(EventLog eventLog1, EventLog eventLog2) {

		if (Math.rint(eventLog1.getTime() * 10000) == Math.rint(eventLog2.getTime() * 10000)
				&& eventLog1.getLinkId() == eventLog2.getLinkId()) {
			return true;
		} else {
			System.out.println("====PROBLEM=====");
			eventLog1.print();
			eventLog2.print();
			System.out.println("=========");
		}
		return false;
	}

	public static void print(ArrayList<EventLog> eventLog) {
		for (int i = 0; i < eventLog.size(); i++) {
			eventLog.get(i).print();
		}
		// 
	}

	/**
	 * For each link in the event list, find out how long a car has been on that
	 * link. Then compare the usage time of each link for the two different
	 * Event logs. Print the average(absolute difference): absSumLink and the
	 * sum (absolute difference) in seconds: absAverageLinkDiff
	 * 
	 * @param eventLog1
	 * @param eventLog2
	 * @return
	 */
	public static double absAverageLinkDiff(ArrayList<EventLog> eventLog1, ArrayList<EventLog> eventLog2) {
		HashMap<Integer, Double[]> hm = new HashMap<Integer, Double[]>();
		/*
		 * key: int (linkId), value: double[4], array contains:
		 * (startCurrentLink1,totalUsageDurationLink1,startCurrentLink2,totalUsageDurationLink2)
		 */

		assert eventLog1.size() == eventLog2.size() : "The size of both eventLogs must be the same!" + eventLog1.size() + " - "
				+ eventLog2.size();

		for (int i = 0; i < eventLog1.size(); i++) {

			int link1 = eventLog1.get(i).getLinkId();
			if (!hm.containsKey(link1)) {
				Double[] d = new Double[4];
				d[0] = 0d;
				d[1] = 0d;
				d[2] = 0d;
				d[3] = 0d;
				hm.put(link1, d);
			}
			hm.get(link1)[1] = eventLog1.get(i).time - hm.get(link1)[0];

			int link2 = eventLog2.get(i).getLinkId();
			if (!hm.containsKey(link2)) {
				Double[] d = new Double[4];
				d[0] = 0d;
				d[1] = 0d;
				d[2] = 0d;
				d[3] = 0d;
				hm.put(link2, d);
			}
			hm.get(link2)[3] = eventLog2.get(i).time - hm.get(link2)[2];

		}

		double absSum = 0;
		double absAverage = 0;
		for (Double[] d : hm.values()) {
			absSum += Math.abs(d[1] - d[3]);
		}

		absAverage = absSum / hm.size();
		System.out.println("absSumLink:" + absSum);
		System.out.println("absAverageLinkDiff:" + absAverage);
		return absAverage;
	}

	public static void filterEvents(int linkId, ArrayList<EventLog> eventLog1, ArrayList<EventLog> eventLog2) {
		LinkedList<EventLog> list1, list2;
		list1 = new LinkedList<EventLog>();
		list2 = new LinkedList<EventLog>();
		assert eventLog1.size() == eventLog2.size() : "The size of both eventLogs must be the same!";
		for (int i = 0; i < eventLog1.size(); i++) {
			if (eventLog1.get(i).linkId == linkId) {
				list1.add(eventLog1.get(i));
			}
			if (eventLog2.get(i).linkId == linkId) {
				list2.add(eventLog2.get(i));
			}
		}
		assert list1.size() == list2.size() : "Inconsistent list size!";
		int noOfDifferentTimes = 0;
		for (int i = 0; i < list1.size(); i++) {
			if (list1.get(i).time != list2.get(i).time) {
				noOfDifferentTimes++;
			}
		}
		System.out.println("noOfDifferentTimes:" + noOfDifferentTimes);
	}

	/**
	 * 
	 * Get the travel time of one person. This means the time, between starting
	 * each leg end its end.
	 * 
	 * @param eventLog1
	 * @param vehicleId
	 * @return
	 */
	public static double getTravelTime(ArrayList<EventLog> eventLog1, int vehicleId) {
		double travelTime = 0;
		double startLegTime = 0;

		for (int i = 0; i < eventLog1.size(); i++) {
			if (eventLog1.get(i).vehicleId == vehicleId) {
				if (eventLog1.get(i).type.equalsIgnoreCase(JDEQSimConfigGroup.START_LEG)) {
					startLegTime = eventLog1.get(i).time;
				} else if (eventLog1.get(i).type.equalsIgnoreCase(JDEQSimConfigGroup.END_LEG)) {
					travelTime += eventLog1.get(i).time - startLegTime;
				}
			}
		}

		return travelTime;
	}

	/**
	 * 
	 * Get sum of Travel time of all vehicles. This means the sum of all leg
	 * times.
	 * 
	 * @param eventLog1
	 * @return
	 */
	public static double getSumTravelTime(ArrayList<EventLog> eventLog1) {
		double travelTime = 0;
		// key=vehicleId, value=starting time of last leg
		HashMap<Integer, Double> startingTime = new HashMap<Integer, Double>();

		for (int i = 0; i < eventLog1.size(); i++) {
			if (eventLog1.get(i).type.equalsIgnoreCase(JDEQSimConfigGroup.START_LEG)) {
				startingTime.put(eventLog1.get(i).vehicleId, eventLog1.get(i).time);
			} else if (eventLog1.get(i).type.equalsIgnoreCase(JDEQSimConfigGroup.END_LEG)) {
				travelTime += eventLog1.get(i).time - startingTime.get(eventLog1.get(i).vehicleId);
			}
		}

		return travelTime;
	}

}
