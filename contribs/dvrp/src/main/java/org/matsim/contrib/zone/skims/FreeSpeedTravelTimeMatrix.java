/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.zone.skims;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.contrib.common.zones.ZoneSystem;
import org.matsim.contrib.common.zones.ZoneSystemUtils;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.trafficmonitoring.QSimFreeSpeedTravelTime;
import org.matsim.contrib.zone.skims.SparseMatrix.NodeAndTime;
import org.matsim.contrib.zone.skims.SparseMatrix.SparseRow;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.router.util.TravelTime;

import com.google.common.base.Verify;
import com.google.common.collect.Sets;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

/**
 * @author Michal Maciejewski (michalm)
 */
public class FreeSpeedTravelTimeMatrix implements TravelTimeMatrix {

	private final static Logger log = LogManager.getLogger(FreeSpeedTravelTimeMatrix.class);

	public static FreeSpeedTravelTimeMatrix createFreeSpeedMatrix(Network dvrpNetwork, ZoneSystem zoneSystem, DvrpTravelTimeMatrixParams params, int numberOfThreads,
		double qSimTimeStepSize) {
		return new FreeSpeedTravelTimeMatrix(dvrpNetwork, zoneSystem, params, numberOfThreads, new QSimFreeSpeedTravelTime(qSimTimeStepSize));
	}

	private final ZoneSystem zoneSystem;
	private final Matrix freeSpeedTravelTimeMatrix;
	private final SparseMatrix freeSpeedTravelTimeSparseMatrix;

	public FreeSpeedTravelTimeMatrix(Network dvrpNetwork, ZoneSystem zoneSystem, DvrpTravelTimeMatrixParams params, int numberOfThreads, TravelTime travelTime) {
		this.zoneSystem = zoneSystem;
		var centralNodes = ZoneSystemUtils.computeMostCentralNodes(dvrpNetwork.getNodes().values(), zoneSystem);
		var travelDisutility = new TimeAsTravelDisutility(travelTime);
		var routingParams = new TravelTimeMatrices.RoutingParams(dvrpNetwork, travelTime, travelDisutility, numberOfThreads);
		freeSpeedTravelTimeMatrix = TravelTimeMatrices.calculateTravelTimeMatrix(routingParams, centralNodes, 0);
		freeSpeedTravelTimeSparseMatrix = TravelTimeMatrices.calculateTravelTimeSparseMatrix(routingParams, params.maxNeighborDistance,
			params.maxNeighborTravelTime, 0).orElse(null);
	}

	@Override
	public int getTravelTime(Node fromNode, Node toNode, double departureTime) {
		if (fromNode == toNode) {
			return 0;
		}
		if (freeSpeedTravelTimeSparseMatrix != null) {
			int time = freeSpeedTravelTimeSparseMatrix.get(fromNode, toNode);
			if (time >= 0) {// value is present
				return time;
			}
		}
		return freeSpeedTravelTimeMatrix.get(zoneSystem.getZoneForNodeId(fromNode.getId()).orElseThrow(), zoneSystem.getZoneForNodeId(toNode.getId()).orElseThrow());
	}

	public int getZonalTravelTime(Node fromNode, Node toNode, double departureTime) {
		return freeSpeedTravelTimeMatrix.get(zoneSystem.getZoneForNodeId(fromNode.getId()).orElseThrow(), zoneSystem.getZoneForNodeId(toNode.getId()).orElseThrow());
	}

	public static FreeSpeedTravelTimeMatrix createFreeSpeedMatrixFromCache(Network dvrpNetwork, ZoneSystem zoneSystem, DvrpTravelTimeMatrixParams params, int numberOfThreads, double qSimTimeStepSize, URL cachePath) {

		FreeSpeedTravelTimeMatrix matrix;
		if (cachePath != null) {
            try {
                return new FreeSpeedTravelTimeMatrix(dvrpNetwork, zoneSystem, cachePath);
            } catch (FileNotFoundException e) {
				log.warn("Freespeed matrix cache file not found, will use as output path for on-the-fly creation.");
            }
        }
		matrix = createFreeSpeedMatrix(dvrpNetwork, zoneSystem, params, numberOfThreads, qSimTimeStepSize);
		matrix.write(cachePath, dvrpNetwork);
		return matrix;
	}

	public FreeSpeedTravelTimeMatrix(Network dvrpNetwork, ZoneSystem zoneSystem, URL cachePath) throws FileNotFoundException {
		this.zoneSystem = zoneSystem;

		try (DataInputStream inputStream = new DataInputStream(IOUtils.getInputStream(cachePath))) {
			// number of zones
			int numberOfZones = inputStream.readInt();
			Verify.verify(numberOfZones == zoneSystem.getZones().size());

			// read zone list
			List<Id<Zone>> zoneIds = new ArrayList<>(numberOfZones);
			for (int i = 0; i < numberOfZones; i++) {
				zoneIds.add(Id.create(inputStream.readUTF(), Zone.class));
			}

			IdSet<Zone> systemZones = new IdSet<>(Zone.class);
			systemZones.addAll(zoneSystem.getZones().keySet());

			IdSet<Zone> dataZones = new IdSet<>(Zone.class);
			dataZones.addAll(zoneIds);

			Verify.verify(Sets.difference(systemZones, dataZones).size() == 0);
			Verify.verify(Sets.difference(dataZones, systemZones).size() == 0);

			// fill matrix
			freeSpeedTravelTimeMatrix = new Matrix(new HashSet<>(zoneSystem.getZones().values()));

			Zone[] zoneArray = new Zone[numberOfZones];
			for (int i = 0; i < numberOfZones; i++) {
				zoneArray[i] = zoneSystem.getZones().get(zoneIds.get(i));
			}

			Counter counter = new Counter("Zone ");
			for (int i = 0; i < numberOfZones; i++) {
				counter.incCounter();
				Zone fromZone = zoneArray[i];
				for (int j = 0; j < numberOfZones; j++) {
					Zone toZone = zoneArray[j];
					freeSpeedTravelTimeMatrix.set(fromZone, toZone, inputStream.readInt());
				}
			}

			// sparse matrix available?
			boolean hasSparseMatrix = inputStream.readBoolean();

			if (!hasSparseMatrix) {
				freeSpeedTravelTimeSparseMatrix = null;
			} else {
				freeSpeedTravelTimeSparseMatrix = new SparseMatrix();

				// read nodes
				int numberOfNodes = inputStream.readInt();
				Verify.verify(numberOfNodes == dvrpNetwork.getNodes().size());

				List<Node> nodes = new ArrayList<>(numberOfNodes);
				for (int i = 0; i < numberOfNodes; i++) {
					Id<Node> nodeId = Id.createNodeId(inputStream.readUTF());
					nodes.add(Objects.requireNonNull(dvrpNetwork.getNodes().get(nodeId)));
				}

				// read rows
				for (int i = 0; i < numberOfNodes; i++) {
					Node from = nodes.get(i);
					int numberOfElements = inputStream.readInt();

					if (numberOfElements > 0) {
						List<NodeAndTime> nodeTimeList = new ArrayList<>(numberOfElements);

						for (int j = 0; j < numberOfElements; j++) {
							Node to = nodes.get(inputStream.readInt());
							int value = inputStream.readInt();

							nodeTimeList.add(new NodeAndTime(to.getId().index(), value));
						}

						SparseRow row = new SparseRow(nodeTimeList);
						freeSpeedTravelTimeSparseMatrix.setRow(from, row);
					}
				}
			}
		} catch (FileNotFoundException e) {
			throw e;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public void write(URL outputPath, Network dvrpNetwork) {
		try (DataOutputStream outputStream = new DataOutputStream(IOUtils.getOutputStream(outputPath, false))) {
			// obtain fixed order of zones
			List<Zone> zones = new ArrayList<>(zoneSystem.getZones().values());
			outputStream.writeInt(zones.size());
			for (Zone zone : zones) {
				outputStream.writeUTF(zone.getId().toString());
			}

			// write matrix
			for (var from : zones) {
				for (var to : zones) {
					int value = freeSpeedTravelTimeMatrix.get(from, to);
					outputStream.writeInt(value);
				}
			}

			// write if sparse exists
			outputStream.writeBoolean(freeSpeedTravelTimeSparseMatrix != null);

			if (freeSpeedTravelTimeSparseMatrix != null) {
				// obtain fixed order of nodes
				List<Node> nodes = new ArrayList<>(dvrpNetwork.getNodes().values());
				outputStream.writeInt(nodes.size());
				for (Node node : nodes) {
					outputStream.writeUTF(node.getId().toString());
				}

				for (Node from : nodes) {
					// write size of the matrix row
					int rowSize = 0;

					for (Node to : nodes) {
						int value = freeSpeedTravelTimeSparseMatrix.get(from, to);
						if (value >= 0) {
							rowSize++;
						}
					}

					outputStream.writeInt(rowSize);
					
					// write matrix row
					for (Node to : nodes) {
						int value = freeSpeedTravelTimeSparseMatrix.get(from, to);
						if (value >= 0) {
							outputStream.writeInt(nodes.indexOf(to));
							outputStream.writeInt(value);
						}
					}
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
