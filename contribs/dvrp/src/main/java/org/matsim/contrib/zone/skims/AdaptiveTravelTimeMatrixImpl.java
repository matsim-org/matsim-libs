/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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

package org.matsim.contrib.zone.skims;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.common.util.DistanceUtils;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.contrib.common.zones.ZoneSystem;
import org.matsim.contrib.common.zones.ZoneSystemUtils;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

/**
 * @author steffenaxer
 */
public class AdaptiveTravelTimeMatrixImpl implements AdaptiveTravelTimeMatrix {
	private final double TIME_INTERVAL = 3600.;
	private final List<Matrix> timeDependentMatrix;
	private final ZoneSystem gridSystem;
	private final double alpha;
	private final Map<Zone, Node> centralNodes;
	private final int numberOfBins;
	private final DvrpTravelTimeMatrixParams params;
	private final Map<SparseTravelTimeKey, Double> sparseTravelTimeCache = new ConcurrentHashMap<>();

	public AdaptiveTravelTimeMatrixImpl(double maxTime, Network dvrpNetwork, ZoneSystem zoneSystem, DvrpTravelTimeMatrixParams params,
										TravelTimeMatrix freeSpeedMatrix, double alpha) {
		this.alpha = alpha;
		this.numberOfBins = numberOfBins(maxTime);
		this.gridSystem = zoneSystem;
		this.centralNodes = ZoneSystemUtils.computeMostCentralNodes(dvrpNetwork.getNodes().values(), this.gridSystem);
		this.timeDependentMatrix = IntStream.range(0, numberOfBins).mapToObj(i -> new Matrix(centralNodes.keySet()))
				.toList();
		this.params = params;
		this.initializeRegularMatrix(freeSpeedMatrix);
		this.initializeSparseTravelTimeCache(freeSpeedMatrix);
	}

	record SparseTravelTimeKey(Node fromNode, Node toNode, double timeBin) {
	}

	int numberOfBins(double maxTime) {
		return (int) (maxTime / TIME_INTERVAL);
	}

	// SparseTravelTimeCache is a poor man's version of the SparseMatrix
	// Much less code, but probably not so memory efficient?
	void initializeSparseTravelTimeCache(TravelTimeMatrix freeSpeedMatrix) {
		for (int i = 0; i < numberOfBins; i++) {
			int bin = i;
			centralNodes.entrySet().stream().parallel().forEach(originZoneEntry -> {
				for (Entry<Zone, Node> destinationZoneEntry : centralNodes.entrySet()) {
					Node originNode = originZoneEntry.getValue();
					Node destinationNode = destinationZoneEntry.getValue();
					if (DistanceUtils.calculateSquaredDistance(originNode.getCoord(),
							destinationNode.getCoord()) < (params.maxNeighborDistance * params.maxNeighborDistance)) {
						SparseTravelTimeKey key = getSparseTravelTimeKey(originNode, destinationNode, bin);
						double freeSpeedTravelTime = freeSpeedMatrix.getTravelTime(originNode, destinationNode, Double.NaN);
						this.sparseTravelTimeCache.computeIfAbsent(key, k -> freeSpeedTravelTime);
					}
				}
			});
		}
	}

	SparseTravelTimeKey getSparseTravelTimeKey(Node originNode, Node destinationNode, int bin) {
		return new SparseTravelTimeKey(originNode, destinationNode, bin);
	}

	// Matrix needs to be filled otherwise we have -1 exceptions
	// We fill the matrix with already calculated free speed travel times
	void initializeRegularMatrix(TravelTimeMatrix freeSpeedMatrix) {
		for (int i = 0; i < numberOfBins; i++) {
			int bin = i;
			centralNodes.entrySet().stream().parallel().forEach(originZoneEntry -> {
				for (Entry<Zone, Node> destinationZoneEntry : centralNodes.entrySet()) {
					Node originNode = originZoneEntry.getValue();
					Node destinationNode = destinationZoneEntry.getValue();
					double freeSpeedTravelTime = freeSpeedMatrix.getTravelTime(originNode, destinationNode, Double.NaN);
					this.timeDependentMatrix.get(bin).set(originZoneEntry.getKey(), destinationZoneEntry.getKey(),
							freeSpeedTravelTime);
				}
			});
		}
	}

	@Override
	public double getTravelTime(Node fromNode, Node toNode, double departureTime) {
		int bin = this.getBin(departureTime);
		Double sparseValue = this.sparseTravelTimeCache.get(this.getSparseTravelTimeKey(fromNode, toNode, bin));
		if (sparseValue != null) {
			return sparseValue;
		}
		return this.timeDependentMatrix.get(bin).get(this.gridSystem.getZoneForNodeId(fromNode.getId()).orElseThrow(), this.gridSystem.getZoneForNodeId(toNode.getId()).orElseThrow());
	}

	int getBin(double departureTime) {
		return Math.min((int) (departureTime / TIME_INTERVAL), this.numberOfBins-1);
	}

	@Override
	public void setTravelTime(Node fromNode, Node toNode, double routeEstimate, double departureTime) {
		int bin = this.getBin(departureTime);
		SparseTravelTimeKey key = this.getSparseTravelTimeKey(fromNode, toNode, bin);
		Double sparseValue = this.sparseTravelTimeCache.get(this.getSparseTravelTimeKey(fromNode, toNode, bin));

		// Update sparseTravelTimeCache
		if (sparseValue != null) {
			double value = getUpdatedValue(sparseValue, routeEstimate, this.alpha);
			this.sparseTravelTimeCache.put(key, value);

			// Update regular matrix for long distances
		} else {
			double currentTravelTimeEstimate = this.getTravelTime(fromNode, toNode, departureTime);
			double value = getUpdatedValue(currentTravelTimeEstimate, routeEstimate, this.alpha);
			this.timeDependentMatrix.get(bin).set(this.gridSystem.getZoneForNodeId(fromNode.getId()).orElseThrow(), this.gridSystem.getZoneForNodeId(toNode.getId()).orElseThrow(),
					value);
		}

	}

	static double getUpdatedValue(double currentValue, double newValue, double alpha) {
		return currentValue * (1 - alpha) + alpha * newValue;
	}

}
