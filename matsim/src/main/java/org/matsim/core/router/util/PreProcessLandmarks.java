/* *********************************************************************** *
 * project: org.matsim.*
 * PreProcessLandmarks.java
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

package org.matsim.core.router.util;

import java.awt.geom.Rectangle2D;
import java.util.Comparator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.internal.MatsimComparator;

/**
 * Pre-processes a given network, gathering information which can be used by
 * {@link org.matsim.core.router.AStarLandmarks} when computing least-cost paths
 * between a start and an end node. Specifically, designates some
 * nodes in the network that act as landmarks and computes the last-cost-path
 * from and to each node in the network to each of the landmarks.
 *
 * @author lnicolas
 */
public class PreProcessLandmarks extends PreProcessEuclidean {

	private final int landmarkCount;

	private final Landmarker landmarker;

	private Node[] landmarks;
	
	private int numberOfThreads = 8;

	private static final Logger log = LogManager.getLogger(PreProcessLandmarks.class);

	public PreProcessLandmarks(final TravelDisutility costFunction) {
		this(costFunction, new Rectangle2D.Double());
	}

	public PreProcessLandmarks(final TravelDisutility costFunction, final int landmarkCount) {
		this(costFunction, new Rectangle2D.Double(), landmarkCount);
	}

	/**
	 * @param costFunction
	 * @param travelZone The area within which the landmarks should lie. Narrowing the zone where the landmarks should
	 * be put normally improves the routing speed of {@link org.matsim.core.router.AStarLandmarks}.
	 */
	public PreProcessLandmarks(final TravelDisutility costFunction,
			final Rectangle2D.Double travelZone) {
		this(costFunction, travelZone, 16);
	}

	/**
	 * Sets the number of threads that will be used to calculate the distances to/from landmarks.
	 * Default is 8.
	 * 
	 * @param numberOfThreads
	 */
	public void setNumberOfThreads(int numberOfThreads) {
		this.numberOfThreads = numberOfThreads;
	}
	
	/**
	 * @param costFunction
	 * @param travelZone The area within which the landmarks should lie. Narrowing the zone where the landmarks should
	 * be put normally improves the routing speed of {@link org.matsim.core.router.AStarLandmarks}.
	 * @param landmarkCount
	 */
	public PreProcessLandmarks(final TravelDisutility costFunction, final Rectangle2D.Double travelZone, final int landmarkCount) {
		this( costFunction ,
				new PieSlicesLandmarker( travelZone ),
				landmarkCount );
	}

	public PreProcessLandmarks(
			final TravelDisutility costFunction,
			final Landmarker landmarker,
			final int landmarkCount) {
		super(costFunction);

		this.landmarkCount = landmarkCount;
		this.landmarker = landmarker;
	}

	@Override
	public void run(final Network network) {
		super.run(network);
		
		log.info("Putting landmarks on network...");
		long now = System.currentTimeMillis();
		landmarks = landmarker.identifyLandmarks( landmarkCount , network );
		log.info("done in " + (System.currentTimeMillis() - now) + " ms");

		log.info("Initializing landmarks data");
		for (Node node : network.getNodes().values()) {
			this.nodeData.put(node, new LandmarksData(this.landmarkCount));
		}
		
		int nOfThreads = this.numberOfThreads;
		if (nOfThreads > this.landmarks.length) {
			nOfThreads = this.landmarks.length;
		}
		if (nOfThreads < 2) {
			nOfThreads = 2; // always use at least two threads
		}
 		log.info("Calculating distance from each node to each of the " + this.landmarkCount + " landmarks using " + nOfThreads + " threads...");
		now = System.currentTimeMillis();

		
		ExecutorService executor = Executors.newFixedThreadPool(nOfThreads);
		for (int i = 0; i < this.landmarks.length; i++) {
			executor.execute(new Calculator(i, this.landmarks[i], this.nodeData, this.costFunction));
		}
		executor.shutdown();
		while (!executor.isTerminated()) {
			log.info("wait for landmarks Calculator to finish...");
			try {
				if (!executor.awaitTermination(10, TimeUnit.MINUTES)) {
					throw new RuntimeException("Landmarks pre-processing timeout.");
				}
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

		for (Node node : network.getNodes().values()) {
			LandmarksData r = getNodeData(node);
			r.updateMinMaxTravelTimes();
		}

		for (Node node : network.getNodes().values()) {
			LandmarksData r = getNodeData(node);
			for (int i = 0; i < this.landmarks.length; i++) {
				if (r.getMinLandmarkTravelTime(i) > r.getMaxLandmarkTravelTime(i)) {
					log.info("Min > max for node " + node.getId() + " and landmark " + i);
				}
			}
		}

		log.info("done in " + (System.currentTimeMillis() - now) + " ms");
	}

	private static class Calculator implements Runnable {
		
		private final int landmarkIdx;
		private final Node landmark;
		private final Map<Node, DeadEndData> nodeData;
		private final TravelDisutility costFunction;
		
		public Calculator(final int landmarkIdx, final Node landmark, final Map<Node, DeadEndData> nodeData, final TravelDisutility costFunction) {
			this.landmarkIdx = landmarkIdx;
			this.landmark = landmark;
			this.nodeData = nodeData;
			this.costFunction = costFunction;
		}
		
		@Override
		public void run() {
			expandLandmarkFrom();
			expandLandmarkTo();
		}
	
		private void expandLandmarkFrom() {
			LandmarksFromTravelTimeComparator comparator = new LandmarksFromTravelTimeComparator(this.nodeData, this.landmarkIdx);
			PriorityQueue<Node> pendingNodes = new PriorityQueue<>(100, comparator);
			LandmarksData role = (LandmarksData) this.nodeData.get(this.landmark);
			role.setToLandmarkTravelTime(this.landmarkIdx, 0.0);
			role.setFromLandmarkTravelTime(this.landmarkIdx, 0.0);
			pendingNodes.add(this.landmark);
			while (!pendingNodes.isEmpty()) {
				Node node = pendingNodes.poll();
				double fromTravTime = ((LandmarksData) this.nodeData.get(node)).getFromLandmarkTravelTime(this.landmarkIdx);
				LandmarksData role2;
				for (Link l : node.getOutLinks().values()) {
					Node n;
					n = l.getToNode();
					double linkTravTime = this.costFunction.getLinkMinimumTravelDisutility(l);
					role2 = (LandmarksData) this.nodeData.get(n);
					double totalTravelTime = fromTravTime + linkTravTime;
					if (role2.getFromLandmarkTravelTime(this.landmarkIdx) > totalTravelTime) {
						role2.setFromLandmarkTravelTime(this.landmarkIdx, totalTravelTime);
						pendingNodes.add(n);
					}
				}
			}
		}

		private void expandLandmarkTo() {
			LandmarksToTravelTimeComparator comparator = new LandmarksToTravelTimeComparator(this.nodeData, this.landmarkIdx);
			PriorityQueue<Node> pendingNodes = new PriorityQueue<>(100, comparator);
			LandmarksData role = (LandmarksData) this.nodeData.get(this.landmark);
			role.setToLandmarkTravelTime(this.landmarkIdx, 0.0);
			role.setFromLandmarkTravelTime(this.landmarkIdx, 0.0);
			pendingNodes.add(this.landmark);
			while (!pendingNodes.isEmpty()) {
				Node node = pendingNodes.poll();
				double toTravTime = ((LandmarksData) this.nodeData.get(node)).getToLandmarkTravelTime(this.landmarkIdx);
				LandmarksData role2;
				for (Link l : node.getInLinks().values()) {
					Node n = l.getFromNode();
					double linkTravTime = this.costFunction.getLinkMinimumTravelDisutility(l);
					role2 = (LandmarksData) this.nodeData.get(n);
					double totalTravelTime = toTravTime + linkTravTime;
					if (role2.getToLandmarkTravelTime(this.landmarkIdx) > totalTravelTime) {
						role2.setToLandmarkTravelTime(this.landmarkIdx, totalTravelTime);
						pendingNodes.add(n);
					}
				}
			}
		}
	
	}

	public Node[] getLandmarks() {
		return this.landmarks.clone();
	}

	@Override
	public LandmarksData getNodeData(final Node n) {
		DeadEndData r = this.nodeData.get(n);
		if (r == null) {
			r = new LandmarksData(this.landmarkCount);
			this.nodeData.put(n, r);
		}
		// would be better to work with a Map<Node,LandmarksData>, but for some reason the implementor of this class
		// decided to inherit from PreProcessEuclidean, which inherits from PreprocessDijkstra, which is wehre the field
		// is..
		// Before I casted here, the cast was done from whithin AStarLandmarks algorithm, which is even worse.
		// td dec 15
		return (LandmarksData) r;
	}

	public static class LandmarksData extends DeadEndData {

		private final double[] landmarkTravelTime1;
		private final double[] landmarkTravelTime2;

		LandmarksData(final int landmarkCount) {
			this.landmarkTravelTime2 = new double[landmarkCount];
			this.landmarkTravelTime1 = new double[landmarkCount];
			for (int i = 0; i < this.landmarkTravelTime2.length; i++) {
				this.landmarkTravelTime2[i] = Double.POSITIVE_INFINITY;
				this.landmarkTravelTime1[i] = Double.POSITIVE_INFINITY;
			}
		}

		void setToLandmarkTravelTime(final int landmarkIndex, final double travelTime) {
			this.landmarkTravelTime2[landmarkIndex] = travelTime;
		}

		void setFromLandmarkTravelTime(final int landmarkIndex, final double travelTime) {
			this.landmarkTravelTime1[landmarkIndex] = travelTime;
		}

		double getToLandmarkTravelTime(final int landmarkIndex) {
			return this.landmarkTravelTime2[landmarkIndex];
		}

		double getFromLandmarkTravelTime(final int landmarkIndex) {
			return this.landmarkTravelTime1[landmarkIndex];
		}

		void updateMinMaxTravelTimes() {
			for (int i = 0; i < this.landmarkTravelTime1.length; i++) {
				setTravelTimes(i, this.landmarkTravelTime2[i], this.landmarkTravelTime1[i]);
			}
		}

		private void setTravelTimes(final int landmarkIndex, final double travelTime1,
				final double travelTime2) {
			if (travelTime1 > travelTime2) {
				this.landmarkTravelTime2[landmarkIndex] = travelTime1;
				this.landmarkTravelTime1[landmarkIndex] = travelTime2;
			} else {
				this.landmarkTravelTime1[landmarkIndex] = travelTime1;
				this.landmarkTravelTime2[landmarkIndex] = travelTime2;
			}
		}

		public double getMinLandmarkTravelTime(final int landmarkIndex) {
			return this.landmarkTravelTime1[landmarkIndex];
		}

		public double getMaxLandmarkTravelTime(final int landmarkIndex) {
			return this.landmarkTravelTime2[landmarkIndex];
		}
	}

    /**
	 * Sorts the Nodes ascending according to their ToLandmarkTravelTime.
	 *
	 * @author lnicolas
	 * @author mrieser
	 */
	private static class LandmarksToTravelTimeComparator implements Comparator<Node>, MatsimComparator {
		private final Map<Node, DeadEndData> roleData;
		private final int landmarkIndex;

		protected LandmarksToTravelTimeComparator(final Map<Node, DeadEndData> roleData, final int landmarkIndex) {
			this.roleData = roleData;
			this.landmarkIndex = landmarkIndex;
		}

		@Override
		public int compare(final Node n1, final Node n2) {

			double c1 = ((LandmarksData) this.roleData.get(n1)).getToLandmarkTravelTime(this.landmarkIndex);
			double c2 = ((LandmarksData) this.roleData.get(n2)).getToLandmarkTravelTime(this.landmarkIndex);

			if (c1 < c2) {
				return -1;
			}
			if (c1 > c2) {
				return +1;
			}
			return n1.getId().compareTo(n2.getId());
		}
	}

	/**
	 * Sorts the Nodes ascending according to their FromLandmarkTravelTime.
	 *
	 * @author lnicolas
	 * @author mrieser
	 */
	private static class LandmarksFromTravelTimeComparator implements Comparator<Node>, MatsimComparator {
		private final Map<Node, DeadEndData> roleData;
		private final int landmarkIndex;
		
		protected LandmarksFromTravelTimeComparator(final Map<Node, DeadEndData> roleData, final int landmarkIndex) {
			this.roleData = roleData;
			this.landmarkIndex = landmarkIndex;
		}
		
		@Override
		public int compare(final Node n1, final Node n2) {
			
			double c1 = ((LandmarksData) this.roleData.get(n1)).getFromLandmarkTravelTime(this.landmarkIndex);
			double c2 = ((LandmarksData) this.roleData.get(n2)).getFromLandmarkTravelTime(this.landmarkIndex);
			
			if (c1 < c2) {
				return -1;
			}
			if (c1 > c2) {
				return +1;
			}
			return n1.getId().compareTo(n2.getId());
		}
	}
	
}
