/* *********************************************************************** *
 * project: org.matsim.*
 * FromToSummary.java
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

package org.matsim.plans.algorithms;

import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.network.Node;
import org.matsim.plans.Act;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;

/**
 * @author lnicolas
 *
 */
public class FromToSummary extends PersonAlgorithm implements PlanAlgorithmI {

	public FromToSummary() {
		super();

		this.comparator = new NodePairComparator();

		this.fromToMap = new TreeMap<NodePair, StartTimeOccurrence>(
				this.comparator);
	}

	public FromToSummary(Node fromNode, Node toNode, int startTime) {
		this();

		addStartTimeOccurrence(fromNode, toNode, startTime);
	}

	private NodePairComparator comparator;

	private TreeMap<NodePair, StartTimeOccurrence> fromToMap;

	Rectangle2D.Double travelZone = new Rectangle2D.Double();

	/**
	 * Prints the trip details (number of trips with the same fromNode, toNode and startTime)
	 * to the console.
	 */
	public void printSummary() {
		ArrayList<String> stringArray = new ArrayList<String>();
		ArrayList<Integer> countArray = new ArrayList<Integer>();
		double avgDistance = 0;
		int cnt = 0;
		Iterator<Map.Entry<NodePair, StartTimeOccurrence>> it = this.fromToMap.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<NodePair, StartTimeOccurrence> me = it.next();
			StartTimeOccurrence occ = me.getValue();
			NodePair np = me.getKey();
			int count = occ.getOccurrenceCnt();
			String out = "Trips from " + np.getFirst().getId() + " to "
					+ np.getSecond().getId() + ": " + count + " (";
			double dist = np.getFirst().getCoord().calcDistance(
					np.getSecond().getCoord());
			avgDistance = (avgDistance * cnt + dist) / (cnt + 1);
			cnt++;
			Iterator<Map.Entry<Double, Integer>> sIt = occ.getStartTimeOccurrenceCnt().entrySet().iterator();
			while (sIt.hasNext()) {
				Map.Entry<Double, Integer> sMe = sIt.next();
				int sOcc = sMe.getValue().intValue();
				double sTime = sMe.getKey().doubleValue();
				out += sOcc + "x" + sTime;
				if (sIt.hasNext()) {
					out += ", ";
				}
			}
			out += ")";

			boolean inserted = false;
			for (int i = 0; i < countArray.size() && inserted == false; i++) {
				if (countArray.get(i).intValue() >= count) {
					countArray.add(i, count);
					stringArray.add(i, out);
					inserted = true;
				}
			}
			if (inserted == false) {
				countArray.add(count);
				stringArray.add(out);
			}
		}
		for (String out : stringArray) {
			System.out.println(out);
		}
		System.out.println();
		System.out.println("Avg from-to distance : " + avgDistance);
		System.out.println("Total number of trips: " + stringArray.size());
	}

	/**
	 * @see org.matsim.plans.algorithms.PlanAlgorithmI#run(org.matsim.plans.Plan)
	 */
	public void run(Plan plan) {
		ArrayList actslegs = plan.getActsLegs();
		Act fromAct = (Act) actslegs.get(0);
		Node fromNode = fromAct.getLink().getToNode();

		for (int j = 2; j < actslegs.size(); j = j + 2) {
			Act toAct = (Act) actslegs.get(j);

			if (fromAct.getEndTime() >= 0) {
				Node toNode = toAct.getLink().getFromNode();
				addStartTimeOccurrence(fromNode, toNode, fromAct.getEndTime());
				this.travelZone.add(fromNode.getCoord().getX(), fromNode.getCoord()
						.getY());
				this.travelZone.add(toNode.getCoord().getX(), toNode.getCoord()
						.getY());
			} else if (fromAct.getDur() < 0) {
				System.out.println("act " + (j - 2)
						+ " has neither end-time nor duration.");
			}

			fromAct = toAct;
		}
	}

	private void addStartTimeOccurrence(Node fromNode, Node toNode,
			double startTime) {
		NodePair np = new NodePair(fromNode, toNode);
		StartTimeOccurrence occ = this.fromToMap.get(np);
		if (occ == null) {
			occ = new StartTimeOccurrence();
		}
		occ.addOccurrence(startTime);
		this.fromToMap.put(np, occ);
	}

	@Override
	public void run(Person person) {
		for (Plan plan : person.getPlans()) {
			run(plan);
		}
	}

	/**
	 * @return the fromToMap
	 */
	public TreeMap<NodePair, StartTimeOccurrence> getFromToMap() {
		return this.fromToMap;
	}

	/**
	 * @author lnicolas
	 * Stores a pair of Nodes.
	 */
	static public class NodePair {

		Node first;
		Node second;

		public NodePair(Node first, Node second) {
			setFirst(first);
			setSecond(second);
		}

		/**
		 * @return the first
		 */
		public Node getFirst() {
			return this.first;
		}

		/**
		 * @param first the first to set
		 */
		private void setFirst(Node first) {
			this.first = first;
		}

		/**
		 * @return the second
		 */
		public Node getSecond() {
			return this.second;
		}

		/**
		 * @param second the second to set
		 */
		private void setSecond(Node second) {
			this.second = second;
		}

		/**
		 * Compares two NodePairs
		 * @param other
		 * @return true if the Nodes in other are the same as in this and appear in the same order
		 */
		public boolean equals(Object other) {
			if (other instanceof NodePair) {
				if (getFirst().equals(((NodePair)other).getFirst())
						&& getSecond().equals(((NodePair)other).getSecond())) {
					return true;
				}
			}
			return false;
		}
		
		public int hashCode() {
			return getFirst().hashCode() & getSecond().hashCode();
		}
	}

	/**
	 * @author lnicolas
	 * Store a number together with a given start time of a trip.
	 * (Yet, this number corresponds to the number of existing trips with the same
	 * start and end nodes and start time)
	 */
	static public class StartTimeOccurrence {

		private int occurrenceCnt = 0;

		private TreeMap<Double, Integer> startTimeOccurrenceCnt = new TreeMap<Double, Integer>();

		public void addOccurrence(double startTime) {
			Double time = Double.valueOf(startTime);
			this.occurrenceCnt++;

			Integer s = this.startTimeOccurrenceCnt.get(time);
			if (s == null) {
				s = 1;
			} else {
				s++;
			}

			this.startTimeOccurrenceCnt.put(time, s);
		}

		/**
		 * @return the startTimeOccurrenceCnt
		 */
		public TreeMap<Double, Integer> getStartTimeOccurrenceCnt() {
			return this.startTimeOccurrenceCnt;
		}

		/**
		 * @return the occurrenceCnt
		 */
		public int getOccurrenceCnt() {
			return this.occurrenceCnt;
		}
	}

	public Rectangle2D.Double getTravelZone() {
		return this.travelZone;
	}
}

/**
 * @author lnicolas
 * Compares two NodePairs.
 */
class NodePairComparator implements Comparator<FromToSummary.NodePair>, Serializable {

	private static final long serialVersionUID = 1L;

	public int compare(FromToSummary.NodePair n1, FromToSummary.NodePair n2) {
		Node n1First = n1.getFirst();
		Node n1Second = n1.getSecond();
		Node n2First = n2.getFirst();
		Node n2Second = n2.getSecond();

		if (n1First.equals(n2First) && n1Second.equals(n2Second)) {
			return 0;
		} else if (n1First.equals(n2First)) {
			return n1Second.compareTo(n2Second);
		} else {
			return n1First.compareTo(n2First);
		}
	}
}
