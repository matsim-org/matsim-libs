/* *********************************************************************** *
 * project: org.matsim.*
 * LandmarkerPieSlices.java
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordUtils;

/*package*/ class LandmarkerPieSlices {

	private Node[] landmarks;

	private Coord center = null;

	private final Rectangle2D.Double travelZone;

	private static final Logger log = Logger.getLogger(LandmarkerPieSlices.class);

	private static final double ZONE_EXPANSION = 0.1;

	LandmarkerPieSlices(final int landmarkCount, final Rectangle2D.Double travelZone) {
		this.landmarks = new Node[landmarkCount];
		this.travelZone = travelZone;
	}

	public void run(final Network network) {
		Collection<? extends Node> nodes;
		if (this.travelZone.getHeight() == 0 || this.travelZone.getWidth() == 0) {
			nodes = network.getNodes().values();
		} else {
			nodes = getNodesInTravelZone(network);
		}
		run(nodes);
	}

	private Set<Node> getNodesInTravelZone(final Network network) {
		double minX = travelZone.getX();
		double maxX = travelZone.getWidth() + minX;
		double minY = travelZone.getY();
		double maxY = travelZone.getHeight() + minY;

		// largen the zone...
		maxX += (maxX - minX) * ZONE_EXPANSION;
		minX -= (maxX - minX) * ZONE_EXPANSION;
		maxY += (maxY - minY) * ZONE_EXPANSION;
		minY -= (maxY - minY) * ZONE_EXPANSION;
		Set<Node> resultNodes = new TreeSet<Node>();
		for (Node n : network.getNodes().values()) {
			if (n.getCoord().getX() <= maxX && n.getCoord().getX() >= minX
					&& n.getCoord().getY() <= maxY && n.getCoord().getY() >= minY) {
				resultNodes.add(n);
			}
		}

		return resultNodes;
	}

	public void run(final Collection<? extends Node> nodes) {
		this.center = getCenter(nodes);
		putLandmarks(nodes, this.landmarks.length);
	}

	private void putLandmarks(final Collection<? extends Node> nodes, final int landmarkCount) {

		ArrayList<ArrayList<Node>> sectors = new ArrayList<ArrayList<Node>>();

		log.info("Filling sectors...");
		double[][] angles = fillSectors(sectors, nodes);

		if (angles.length < landmarkCount) {
			log.info("Reducing number of landmarks from " + landmarkCount + " to " + angles.length + "...");
			this.landmarks = new Node[angles.length];
		}
		for (int i = 0; i < this.landmarks.length; i++) {
			this.landmarks[i] = getLandmark(sectors.get(i), angles[i]);
		}

		log.info("Refining landmarks...");
		refineLandmarks(sectors, angles);
		log.info("done");
	}

	private double[][] fillSectors(final ArrayList<ArrayList<Node>> sectors, final Collection<? extends Node> nodes) {
		ArrayList<double[]> angles = new ArrayList<double[]>();
		// Sort nodes according to angle
		TreeMap<Double, Node[]> sortedNodes = new TreeMap<Double, Node[]>();
		Node[] nodeList;
		for (Node node : nodes) {
			double x = node.getCoord().getX() - this.center.getX();
			double y = node.getCoord().getY() - this.center.getY();
			double angle = Math.atan2(y, x) + Math.PI;
			nodeList = sortedNodes.get(angle);
			if (nodeList == null) {
				nodeList = new Node[1];
				nodeList[0] = node;
			} else {
				Node[] nodeList2 = new Node[nodeList.length + 1];
                System.arraycopy(nodeList, 0, nodeList2, 0, nodeList.length);
				nodeList2[nodeList.length] = node;
				nodeList = nodeList2;
			}
			sortedNodes.put(angle, nodeList);
		}
		double lastAngle = 0;
		Iterator<Node[]> it = sortedNodes.values().iterator();
		if (it.hasNext()) {
			// Fill sectors such that each sector contains on average the same number of nodes
			Node[] tmpNodes = it.next();
			int k = 0;
			for (int i = 0; i < this.landmarks.length; i++) {

				sectors.add(new ArrayList<Node>());
				Node node = null;
				for (int j = 0; j < nodes.size() / this.landmarks.length; j++) {
					if (k == tmpNodes.length) {
						tmpNodes = it.next();
						k = 0;
					}
					node = tmpNodes[k++];
					sectors.get(angles.size()).add(node);
				}
				// Add the remaining nodes to the last sector
				if (i == this.landmarks.length - 1) {
					while (it.hasNext() || k < tmpNodes.length) {
						if (k == tmpNodes.length) {
							tmpNodes = it.next();
							k = 0;
						}
						node = tmpNodes[k++];
						sectors.get(angles.size()).add(node);
					}
				}
				if (sectors.get(angles.size()).isEmpty()) {
					log.info("There is no node in sector " + i + "!");
					sectors.remove(angles.size());
				} else {
					// Get the angle of the "rightmost" node
					double x = node.getCoord().getX() - this.center.getX();
					double y = node.getCoord().getY() - this.center.getY();
					double angle = Math.atan2(y, x) + Math.PI;
					double[] tmp = new double[2];
					tmp[0] = lastAngle;
					tmp[1] = angle;
					angles.add(tmp);
					lastAngle = angle;
				}
			}
		}
		return angles.toArray(new double[0][2]);
	}

	private Node getLandmark(final ArrayList<Node> nodes, final double[] angles) {
		double maxDist = Double.NEGATIVE_INFINITY;
		Node landmark = null;
		for (Node node : nodes) {
			if ((node.getOutLinks().size() > 1 && node.getInLinks().size() > 1)
					|| landmark == null) {
				double x = node.getCoord().getX() - this.center.getX();
				double y = node.getCoord().getY() - this.center.getY();
				double angle = Math.atan2(y, x) + Math.PI;
				double minAngelToBorder = 0;
				if (angle - angles[0] < angles[1] - angle) {
					minAngelToBorder = angle - angles[0];
				} else {
					minAngelToBorder = angles[1] - angle;
				}
				double distApprox = Math.sqrt(x * x + y * y) * (1 + minAngelToBorder / (2 * Math.PI));
				// Set the node that is farthest away from the center to be the landmark in the current sector
				if (distApprox > maxDist) {
					landmark = node;
					maxDist = distApprox;
				}
			}
		}

		return landmark;
	}

	private void refineLandmarks(final ArrayList<ArrayList<Node>> sectors, final double[][] sectorAngles) {
		boolean doRefine = true;
		double[] landmarkAngels = new double[this.landmarks.length];
		for (int i = 0; i < this.landmarks.length; i++) {
			double x = this.landmarks[i].getCoord().getX() - this.center.getX();
			double y = this.landmarks[i].getCoord().getY() - this.center.getY();
			landmarkAngels[i] = Math.atan2(y, x) + Math.PI;
		}
		double minAngelFactor = 0.5;
		while (doRefine) {
			doRefine = false;
			for (int i = 0; i < this.landmarks.length && !sectors.get(i).isEmpty(); i++) {
				int preInd = i - 1;
				double angelDiff;
				if (preInd == -1) {
					preInd = this.landmarks.length - 1;
					angelDiff = 2 * Math.PI + landmarkAngels[i] - landmarkAngels[preInd];
				} else {
					angelDiff = landmarkAngels[i] - landmarkAngels[preInd];
				}
				// Get the lower size angle of the two sectors
				double minSectorSize = sectorAngles[i][1] - sectorAngles[i][0];
				if (sectorAngles[preInd][1] - sectorAngles[preInd][0] < minSectorSize) {
					minSectorSize = sectorAngles[preInd][1] - sectorAngles[preInd][0];
				}
				if (angelDiff < minSectorSize * minAngelFactor) {
					// Change landmark that is nearer to the center
					int indexToChange = 0;
					if (CoordUtils.calcEuclideanDistance(this.center, this.landmarks[preInd].getCoord()) < CoordUtils.calcEuclideanDistance(this.center, this.landmarks[i].getCoord())) {
						// Narrow the sector
						sectorAngles[preInd][1] -= minSectorSize * minAngelFactor;
						indexToChange = preInd;
					} else {
						// Narrow the sector
						sectorAngles[i][0] += minSectorSize * minAngelFactor;
						indexToChange = i;
					}
					removeNodesFromSector(sectors.get(indexToChange), sectorAngles[indexToChange]);
					if (sectors.get(indexToChange).isEmpty()) {
						log.info("There is no node in sector " + indexToChange + " after narrowing it!");
					} else {
						this.landmarks[indexToChange] = getLandmark(sectors.get(indexToChange), sectorAngles[indexToChange]);
					}
					double x = this.landmarks[indexToChange].getCoord().getX() - this.center.getX();
					double y = this.landmarks[indexToChange].getCoord().getY() - this.center.getY();
					landmarkAngels[indexToChange] = Math.atan2(y, x) + Math.PI;
					doRefine = true;
					break;
				}
			}
		}
	}

	private void removeNodesFromSector(final ArrayList<Node> sector,
			final double[] sectorAngles) {
		int i = 0;
		while (i < sector.size()) {
			Node node = sector.get(i);
			double x = node.getCoord().getX() - this.center.getX();
			double y = node.getCoord().getY() - this.center.getY();
			double angle = Math.atan2(y, x) + Math.PI;
			if (angle < sectorAngles[0] || angle > sectorAngles[1]) {
				sector.remove(i);
			} else {
				i++;
			}
		}
	}

	private Coord getCenter(final Collection<? extends Node> nodes) {
		double[] bBox = NetworkUtils.getBoundingBox(nodes);
		double minX = bBox[0];
		double minY = bBox[1];
		double maxX = bBox[2];
		double maxY = bBox[3];

		double centerX = (maxX - minX) / 2 + minX;
		double centerY = (maxY - minY) / 2 + minY;

		return new Coord(centerX, centerY);
	}

	public Node[] getLandmarks() {
		return this.landmarks.clone();
	}

}
