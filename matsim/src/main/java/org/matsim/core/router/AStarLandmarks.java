/* *********************************************************************** *
 * project: org.matsim.*
 * AStarLandmarks.java
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

package org.matsim.core.router;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.AStarNodeData;
import org.matsim.core.router.util.PreProcessLandmarks;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.RouterPriorityQueue;
import org.matsim.vehicles.Vehicle;

/**
 * Implements the A* router algorithm for a given NetworkLayer
 * using some so called landmark nodes that are used to get a
 * better estimation of the remaining travel cost. This
 * way we can accelerate the routing speed, and the routes we find
 * are still guaranteed to be the best ones with respect to the travel cost.
 *
 * <p>For every router, there exists a class which computes some
 * preprocessing data and is passed to the router class
 * constructor in order to accelerate the routing procedure.
 * The one used for AStarLandmarks is org.matsim.demandmodeling.router.util.PreProcessLandmarks.<br>
 *
 * AStarLandmarks is about double as fast as AStarEuclidean. PreProcessLandmarks.run() takes
 * about 1 minute on 400'000 nodes on a AMD Opteron processor 275 with 2.2GHz and
 * requires 2*X double values per node, where X is the number of landmarks. Currently, it is
 * set to 16 (but can be set to another value, as 12 or 8 for example), so with 400'000
 * nodes we would need 2*8*16*400'000 bytes = about 100MB of additional memory.<br>
 * Conditions: The same as for AStarEuclidean.<br>
 * Code example:<br>
 * <code><br> TravelMinCost costFunction = ...<br>
 * PreProcessLandmarks preProcessData = new PreProcessLandmarks(costFunction);<br>
 * preProcessData.run(network);<br>...<br>
 * LeastCostPathCalculator routingAlgo = new AStarLandmarks(network, preProcessData);
 * <br>...</code></p>
 * @see org.matsim.core.router.AStarEuclidean
 * @see org.matsim.core.router.util.PreProcessLandmarks
 * @see org.matsim.core.router.Dijkstra
 * @author lnicolas
 */
public class AStarLandmarks extends AStarEuclidean {

	protected int[] activeLandmarkIndexes;

	protected final Node[] landmarks;

	/*package*/ static final int controlInterval = 40;
	/*package*/ int controlCounter = 0;

	/**
	 * Default constructor; sets the overdo factor to 1.
	 * @param network Where we do the routing.
	 * @param preProcessData The pre-process data (containing the landmarks etc.).
	 * @param timeFunction Calculates the travel time on links.
	 * @param costFunction Calculates the travel cost on links.
	 */
	AStarLandmarks(final Network network, final PreProcessLandmarks preProcessData,
			final TravelDisutility costFunction, final TravelTime timeFunction) {
		this(network, preProcessData, costFunction, timeFunction, 1);
	}

	/**
	 * Default constructor; sets the overdo factor to 1.
	 * @param network Where we do the routing.
	 * @param preProcessData The pre-process data (containing the landmarks etc.).
	 * @param timeFunction Calculates the travel time on links.
	 */
	AStarLandmarks(final Network network, final PreProcessLandmarks preProcessData, final TravelTime timeFunction) {
		this(network, preProcessData, preProcessData.getCostFunction(), timeFunction, 1);
	}

	/**
	 * @param network Where we do the routing.
	 * @param preProcessData The pre-process data (containing the landmarks etc.).
	 * @param costFunction
	 * @param timeFunction Calculates the travel time on links.
	 * @param overdoFactor The factor which is multiplied with the output of the A* heuristic function. 
	 * The higher the overdo factor the greedier the router, i.e. it visits less nodes during routing and 
	 * is thus faster, but for an overdo factor > 1, it is not guaranteed that the router returns the 
	 * least-cost paths. Rather it tends to return distance-minimal paths.
	 * @see #AStarLandmarks(Network, PreProcessLandmarks, TravelTime)
	 */
	AStarLandmarks(final Network network, final PreProcessLandmarks preProcessData,
			final TravelDisutility costFunction, final TravelTime timeFunction, final double overdoFactor) {
		super(network, preProcessData, costFunction, timeFunction, overdoFactor);

		this.landmarks = preProcessData.getLandmarks();
	}

	@Override
	public Path calcLeastCostPath(final Node fromNode, final Node toNode, final double startTime, final Person person, final Vehicle vehicle) {
		this.controlCounter = 0;	// reset counter for each calculated path!
		
		if (this.landmarks.length >= 2) {
			initializeActiveLandmarks(fromNode, toNode, 2);
		} else {
			initializeActiveLandmarks(fromNode, toNode, this.landmarks.length);
		}
		return super.calcLeastCostPath(fromNode, toNode, startTime, person, vehicle);
	}

	@Override
	protected void relaxNode(final Node outNode, final Node toNode, final RouterPriorityQueue<Node> pendingNodes) {
		this.controlCounter++;
		if (this.controlCounter == controlInterval) {
			int newLandmarkIndex = checkToAddLandmark(outNode, toNode);
			if (newLandmarkIndex > 0) {
				updatePendingNodes(newLandmarkIndex, toNode, pendingNodes);
			}
			this.controlCounter = 0;
		}
		super.relaxNode(outNode, toNode, pendingNodes);
	}

	/**
	 * Inspect all landmarks and determines the actLandmarkCount best ones that will be used for routing (so-called active landmarks).
	 * 
	 * @param fromNode The node for which we estimate the travel time to the toNode in order to rank the landmarks.
	 * @param toNode The node to which we estimate the travel time from the fromNode in order to rank the landmarks.
	 * @param actLandmarkCount The number of active landmarks landmarks to set.
	 */
	/*package*/ void initializeActiveLandmarks(final Node fromNode, final Node toNode, final int actLandmarkCount) {
		final PreProcessLandmarks.LandmarksData fromData = getPreProcessData(fromNode);
		final PreProcessLandmarks.LandmarksData toData = getPreProcessData(toNode);

		// Sort the landmarks according to the accuracy of their distance estimation they yield.
		double[] estTravelTimes = new double[actLandmarkCount];
		this.activeLandmarkIndexes = new int[actLandmarkCount];
		for (int i = 0; i < estTravelTimes.length; i++) {
			estTravelTimes[i] = Double.NEGATIVE_INFINITY;
		}
		double tmpTravTime;
		for (int i = 0; i < this.landmarks.length; i++) {
			tmpTravTime = estimateRemainingTravelCost(fromData, toData, i);
			for (int j = 0; j < estTravelTimes.length; j++) {
				if (tmpTravTime > estTravelTimes[j]) {
					for (int k = estTravelTimes.length - 1; k > j; k--) {
						estTravelTimes[k] = estTravelTimes[k - 1];
						this.activeLandmarkIndexes[k] = this.activeLandmarkIndexes[k - 1];
					}
					estTravelTimes[j] = tmpTravTime;
					this.activeLandmarkIndexes[j] = i;
					break;
				}
			}
		}
	}

	@Override
	protected PreProcessLandmarks.LandmarksData getPreProcessData(final Node n) {
		return (PreProcessLandmarks.LandmarksData) super.getPreProcessData(n);
	}

	/**
	 * Estimates the remaining travel cost from fromNode to toNode using the landmarks on the network.
	 * 
	 * @param fromNode The first node.
	 * @param toNode The second node.
	 * @return The travel cost when traveling between the two given nodes.
	 */
	@Override
	protected double estimateRemainingTravelCost(final Node fromNode, final Node toNode) {

		PreProcessLandmarks.LandmarksData fromRole = getPreProcessData(fromNode);
		PreProcessLandmarks.LandmarksData toRole = getPreProcessData(toNode);
		double tmpTravCost;
		double travCost = 0;
		for (int i = 0, n = this.activeLandmarkIndexes.length; i < n; i++) {
			tmpTravCost = estimateRemainingTravelCost(fromRole, toRole, this.activeLandmarkIndexes[i]);
			if (tmpTravCost > travCost) {
				travCost = tmpTravCost;
			}
		}
		tmpTravCost = super.estimateRemainingTravelCost(fromNode, toNode);
		if (travCost > tmpTravCost) {
			return travCost;
		}
		/* else */
		return tmpTravCost;
	}

	/**
	 * If a landmark has been added to the set of the active landmarks, this function re-evaluates the estimated 
	 * remaining travel time based on the new set of active landmarks of the nodes contained in pendingNodes. 
	 * If this estimation improved, the node's position in the pendingNodes queue is updated.
	 * 
	 * @param newLandmarkIndex The index of the new active landmark.
	 * @param toNode The target node of the route to be calculated.
	 * @param pendingNodes The nodes visited so far.
	 */
	/*package*/ void updatePendingNodes(final int newLandmarkIndex, final Node toNode, final RouterPriorityQueue<Node> pendingNodes) {
		Iterator<Node> it = pendingNodes.iterator();
		PreProcessLandmarks.LandmarksData toRole = getPreProcessData(toNode);
		List<Double> newEstRemTravCosts = new ArrayList<>();
		List<Node> nodesToBeUpdated = new ArrayList<>();
		while (it.hasNext()) {
			Node node = it.next();
			AStarNodeData data = getData(node);
			PreProcessLandmarks.LandmarksData ppRole = getPreProcessData(node);
			double estRemTravCost = data.getExpectedRemainingCost();
			double newEstRemTravCost = estimateRemainingTravelCost(ppRole, toRole, newLandmarkIndex);
			if (newEstRemTravCost > estRemTravCost) {
				nodesToBeUpdated.add(node);
				newEstRemTravCosts.add(newEstRemTravCost);
			}
		}
		for (Node node : nodesToBeUpdated) {
			pendingNodes.remove(node);
		}
		for (int i = 0; i < nodesToBeUpdated.size(); i++) {
			Node node = nodesToBeUpdated.get(i);
			AStarNodeData data = getData(node);
			data.setExpectedRemainingCost(newEstRemTravCosts.get(i));
			pendingNodes.add(node, getPriority(data));
		}
	}

	/**
	 * Checks whether there is a landmark in the set of the non-active landmarks that yields a better estimation than 
	 * the best active landmark. If there is, this landmark is added to the set of active landmark and its index is returned.
	 * 
	 * @param fromNode The node for which we estimate the remaining travel time to the toNode.
	 * @param toNode The target node. 
	 * @return The index of the landmark that has been added to the set of active landmarks, or -1 if no landmark was added.
	 */
	/*package*/ int checkToAddLandmark(final Node fromNode, final Node toNode) {
		double bestTravCostEst = estimateRemainingTravelCost(fromNode, toNode);
		PreProcessLandmarks.LandmarksData fromRole = getPreProcessData(fromNode);
		PreProcessLandmarks.LandmarksData toRole = getPreProcessData(toNode);
		int bestIndex = -1;
		for (int i = 0; i < this.landmarks.length; i++) {
			double tmpTravTime = estimateRemainingTravelCost(fromRole, toRole, i);
			if (tmpTravTime > bestTravCostEst) {
				bestIndex = i;
				bestTravCostEst = tmpTravTime;
			}
		}
		if (bestIndex != -1) {
			int[] newActiveLandmarks = new int[this.activeLandmarkIndexes.length + 1];
			System.arraycopy(this.activeLandmarkIndexes, 0, newActiveLandmarks, 0, this.activeLandmarkIndexes.length);
			newActiveLandmarks[this.activeLandmarkIndexes.length] = bestIndex;
			this.activeLandmarkIndexes = newActiveLandmarks;
		}
		return bestIndex;
	}

	/**
	 * Estimates the remaining travel cost from fromNode to toNode using the landmark given by index.
	 * 
	 * @param fromRole The first node/role.
	 * @param toRole The second node/role.
	 * @param index The index of the landmarks that should be used for
	 * the estimation of the travel cost.
	 * @return The travel cost when traveling between the two given nodes.
	 */
	protected double estimateRemainingTravelCost(final PreProcessLandmarks.LandmarksData fromRole,
			final PreProcessLandmarks.LandmarksData toRole, final int index) {
		double tmpTravTime;
		final double fromMinLandmarkTravelTime = fromRole.getMinLandmarkTravelTime(index);
		final double toMaxLandmarkTravelTime = toRole.getMaxLandmarkTravelTime(index);
		tmpTravTime = fromMinLandmarkTravelTime - toMaxLandmarkTravelTime;
		if (tmpTravTime < 0) {
			tmpTravTime = toRole.getMinLandmarkTravelTime(index) - fromRole.getMaxLandmarkTravelTime(index);
			if (tmpTravTime <= 0) {
				return 0;
			}
		}
		return tmpTravTime * this.overdoFactor;
	}
}