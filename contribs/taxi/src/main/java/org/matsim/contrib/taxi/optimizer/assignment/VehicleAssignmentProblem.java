/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.taxi.optimizer.assignment;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.common.util.StraightLineKnnFinder;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch.PathData;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.taxi.optimizer.BestDispatchFinder.Dispatch;
import org.matsim.contrib.taxi.optimizer.VehicleData;
import org.matsim.contrib.taxi.optimizer.assignment.AssignmentDestinationData.DestEntry;
import org.matsim.core.router.speedy.SpeedyGraph;
import org.matsim.core.router.speedy.SpeedyGraphBuilder;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

/**
 * @author michalm
 */
public class VehicleAssignmentProblem<D> {
	public interface AssignmentCost<D> {
		double calc(VehicleData.Entry departure, DestEntry<D> dest, PathData pathData);
	}

	private final TravelTime travelTime;
	private final LeastCostPathCalculator router;

	private final OneToManyPathSearch pathSearch;

	private final StraightLineKnnFinder<VehicleData.Entry, DestEntry<D>> destinationFinder;
	private final StraightLineKnnFinder<DestEntry<D>, VehicleData.Entry> vehicleFinder;

	private AssignmentCost<D> assignmentCost;
	private VehicleData vData;
	private AssignmentDestinationData<D> dData;

	public VehicleAssignmentProblem(Network network, TravelTime travelTime, TravelDisutility travelDisutility) {
		// we do not need router when there is no kNN filtering
		this(network, travelTime, travelDisutility, null, -1, -1);
	}

	public VehicleAssignmentProblem(Network network, TravelTime travelTime, TravelDisutility travelDisutility,
			LeastCostPathCalculator router, int nearestDestinationLimit, int nearestVehicleLimit) {
		this.travelTime = travelTime;
		this.router = router;

		IdMap<Node, Node> nodeMap = new IdMap<>(Node.class);
		nodeMap.putAll(network.getNodes());
		pathSearch = OneToManyPathSearch.createSearch(SpeedyGraphBuilder.build(network), nodeMap, travelTime, travelDisutility,
				false);

		// TODO this kNN is slow
		destinationFinder = nearestDestinationLimit < 0 ?
				null :
				new StraightLineKnnFinder<>(nearestDestinationLimit, veh -> veh.link.getToNode().getCoord(),
						dest -> dest.link.getFromNode().getCoord());
		vehicleFinder = nearestVehicleLimit < 0 ?
				null :
				new StraightLineKnnFinder<>(nearestVehicleLimit, dest -> dest.link.getFromNode().getCoord(),
						veh -> veh.link.getToNode().getCoord());
	}

	public List<Dispatch<D>> findAssignments(VehicleData vData, AssignmentDestinationData<D> dData,
			AssignmentCost<D> assignmentCost) {
		this.vData = vData;
		this.dData = dData;
		this.assignmentCost = assignmentCost;

		PathData[][] pathDataMatrix = createPathDataMatrix();
		double[][] costMatrix = createCostMatrix(pathDataMatrix);
		int[] assignments = new HungarianAlgorithm(costMatrix).execute();
		return createDispatches(assignments, pathDataMatrix, travelTime);
	}

	// private static int calcPathsForVehiclesCount = 0;
	// private static int calcPathsForDestinationsCount = 0;

	private PathData[][] createPathDataMatrix() {
		PathData[][] pathDataMatrix = new PathData[vData.getSize()][dData.getSize()];

		if (dData.getSize() > vData.getSize()) {
			calcPathsForVehicles(pathDataMatrix);
		} else {
			calcPathsForDestinations(pathDataMatrix);
		}

		return pathDataMatrix;
	}

	private void calcPathsForVehicles(PathData[][] pathDataMatrix) {
		for (int v = 0; v < vData.getSize(); v++) {
			VehicleData.Entry departure = vData.getEntry(v);

			List<DestEntry<D>> filteredDests = destinationFinder == null ?
					dData.getEntries() :
					destinationFinder.findNearest(departure, dData.getEntries().stream());
			List<Link> toLinks = filteredDests.stream().map(dest -> dest.link).toList();
			PathData[] paths = pathSearch.calcPathDataArray(departure.link, toLinks, departure.time, true);

			for (int i = 0; i < filteredDests.size(); i++) {
				int d = filteredDests.get(i).idx;
				pathDataMatrix[v][d] = paths[i];
			}
		}
	}

	// TODO does not support adv reqs
	private void calcPathsForDestinations(PathData[][] pathDataMatrix) {
		for (int d = 0; d < dData.getSize(); d++) {
			DestEntry<D> dest = dData.getEntry(d);

			List<VehicleData.Entry> filteredVehs = vehicleFinder == null ?
					vData.getEntries() :
					vehicleFinder.findNearest(dest, vData.getEntries().stream());
			List<Link> toLinks = filteredVehs.stream().map(veh -> veh.link).toList();
			PathData[] paths = pathSearch.calcPathDataArray(dest.link, toLinks, dest.time, false);

			for (int i = 0; i < filteredVehs.size(); i++) {
				int v = filteredVehs.get(i).idx;
				pathDataMatrix[v][d] = paths[i];
			}
		}
	}

	private double[][] createCostMatrix(PathData[][] pathDataMatrix) {

		double[][] costMatrix = new double[vData.getSize()][dData.getSize()];

		for (int v = 0; v < vData.getSize(); v++) {
			VehicleData.Entry departure = vData.getEntry(v);
			for (int r = 0; r < dData.getSize(); r++) {
				costMatrix[v][r] = assignmentCost.calc(departure, dData.getEntry(r), pathDataMatrix[v][r]);
			}
		}

		return costMatrix;
	}

	private List<Dispatch<D>> createDispatches(int[] assignments, PathData[][] pathDataMatrix, TravelTime travelTime) {
		List<Dispatch<D>> dispatches = new ArrayList<>(Math.min(vData.getSize(), dData.getSize()));
		for (int v = 0; v < assignments.length; v++) {
			int d = assignments[v];
			if (d == -1 || // no request assigned
					d >= dData.getSize()) {// non-existing (dummy) request assigned
				continue;
			}

			VehicleData.Entry departure = vData.getEntry(v);
			DestEntry<D> dest = dData.getEntry(d);
			PathData pathData = pathDataMatrix[v][d];

			// TODO if null is frequent we may be more efficient by increasing the neighbourhood
			VrpPathWithTravelData vrpPath = pathData == null ?
					VrpPaths.calcAndCreatePath(departure.link, dest.link, departure.time, router, travelTime) :
					VrpPaths.createPath(departure.link, dest.link, departure.time, pathData, travelTime);

			dispatches.add(new Dispatch<>(departure.vehicle, dest.destination, vrpPath));
		}

		return dispatches;
	}
}
