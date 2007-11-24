/* *********************************************************************** *
 * project: org.matsim.*
 * RandomPlansGenerator.java
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

package playground.lnicolas.network.algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.network.algorithms.NetworkAlgorithm;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansWriter;
import org.matsim.utils.geometry.shared.Coord;

public class RandomPlansGenerator extends NetworkAlgorithm {

	int cellSize = 5000;
	int distanceTolerance = 500;

	private double minX;
	private double minY;
	private double givenFromToDistance;
	private int tripCount;
	private Plans plans;

	public RandomPlansGenerator(double avgFromToDistance,
			int tripCount) {
		this.givenFromToDistance = avgFromToDistance;
		this.tripCount = tripCount;
	}

	private ArrayList<Node>[][] initCells(NetworkLayer network) {
		this.minX = Double.MAX_VALUE;
		this.minY = Double.MAX_VALUE;
		double maxX = Double.MIN_VALUE;
		double maxY = Double.MIN_VALUE;

		for (Node n : network.getNodes().values()) {
			if (n.getCoord().getX() > maxX) {
				maxX = n.getCoord().getX();
			}
			if (n.getCoord().getX() < minX) {
				minX = n.getCoord().getX();
			}
			if (n.getCoord().getY() > maxY) {
				maxY = n.getCoord().getY();
			}
			if (n.getCoord().getY() < minY) {
				minY = n.getCoord().getY();
			}
		}

		int cellColumnCount = (int) Math.ceil((maxX - minX) / cellSize);
		int cellRowCount = (int) Math.ceil((maxY - minY) / cellSize);
		ArrayList<Node>[][] cells
			= new ArrayList[cellRowCount][cellColumnCount];
		for (int i = 0; i < cellRowCount; i++) {
			for (int j = 0; j < cellColumnCount; j++) {
				cells[i][j] = new ArrayList<Node>();
			}
		}

		// Put each node in the appropriate cell
		for (Node n : network.getNodes().values()) {
			int row = (int)(n.getCoord().getY() - minY) / cellSize;
			int column = (int)(n.getCoord().getX() - minX) / cellSize;
			cells[row][column].add(n);
		}

		System.out.println("Rows: " + cellRowCount + ", columns: "
				+ cellColumnCount);

		int emptyCount = 0;
		int cellCount = 0;
		for (int i = 0; i < cellRowCount; i++) {
			for (int j = 0; j < cellColumnCount; j++) {
				if (cells[i][j].isEmpty()) {
					emptyCount++;
//					System.out.println("Cell " + i + ", " + j + " is empty");
				}
				cellCount++;
			}
		}
		System.out.println(emptyCount + " of " + cellCount + " are empty");

		return cells;
	}

	private void generateTrips(NetworkLayer network, ArrayList<Node>[][] cells) {

		Gbl.random.nextDouble(); // draw one because of strange "not-randomness" in the first draw...

		// Take a random cell and determine the cells that are givenFromToDistance
		// away from it
		Plans plans = new Plans();
		for (int i = 0; i < tripCount; i++) {
			if (addPlan(cells, plans, i) == false) {
				Gbl.errorMsg("No from-to node pairs found for distance "
					+ givenFromToDistance);
			}
		}

		PlansWriter plans_writer = new PlansWriter(plans);
		plans_writer.write();
		System.out.println("Wrote plans to "
				+ Gbl.getConfig().plans().getOutputFile());

		this.plans = plans;
	}

	private void generateTrips(NetworkLayer network) {
		List<Node> fromNodes = new ArrayList<Node>(network.getNodes().values());
		List<Node> toNodes = new ArrayList<Node>(network.getNodes().values());
		Iterator<Node> fromIt = fromNodes.iterator();
		int i = 0;
//		String statusString = "|----------+-----------|";
//		System.out.println(statusString);
		Plans plans = new Plans();

		while (i < this.tripCount) {
			if (fromIt.hasNext() == false) {
				fromIt = fromNodes.iterator();
			}
			Node fromNode = fromIt.next();
			Collections.shuffle(toNodes, Gbl.random);
			Iterator<Node> toIt = toNodes.iterator();
			Node toNode = null;
			while (toNode == null) {
				if (toIt.hasNext() == false) {
					fromIt.remove();
					break;
				}
				Node toNode2 = toIt.next();
				double dist = toNode2.getCoord().calcDistance(fromNode.getCoord());
				if (dist > (givenFromToDistance - distanceTolerance)
						&& dist < (givenFromToDistance + distanceTolerance)) {
					toNode = toNode2;
					break;
				}
			}
			if (toNode != null) {
				addPlan(plans, i, toNode, fromNode);
				i++;
			}

//			if (i % (this.tripCount / statusString.length()) == 0) {
//				System.out.print(".");
//				System.out.flush();
//			}
		}

		PlansWriter plans_writer = new PlansWriter(plans);
		plans_writer.write();
		System.out.println("Wrote plans to "
				+ Gbl.getConfig().plans().getOutputFile());

		this.plans = plans;
	}

	private boolean addPlan(ArrayList<Node>[][] cells, Plans plans, int id) {
		Node toNode = null;
		Node fromNode = null;
		int cnt = 0;
		while (toNode == null && cnt < 1000) {
			ArrayList<Node> fromNodes = new ArrayList<Node>();
			int fromCellRow = 0;
			int fromCellColumn = 0;
			while (fromNodes.isEmpty() == true) {
				fromCellRow = (int) (Gbl.random.nextDouble() * (cells.length));
				fromCellColumn = (int) (Gbl.random.nextDouble() * (cells[0].length));
				fromNodes = cells[fromCellRow][fromCellColumn];
			}
			int fromNodeIndex = (int) (Gbl.random.nextDouble() * (fromNodes.size()));
			fromNode = fromNodes.get(fromNodeIndex);
			ArrayList<Node> toNodes = getToNodes(fromCellRow, fromCellColumn,
					cells);

			for (Node n : toNodes) {
				double dist = n.getCoord().calcDistance(fromNode.getCoord());
				if (dist > (givenFromToDistance - distanceTolerance)
						&& dist < (givenFromToDistance + distanceTolerance)) {
					toNode = n;
					break;
				}
			}
			cnt++;
		}
		if (toNode == null) {
			System.out.println("no from-to node pairs found for distance "
					+ givenFromToDistance);
			return false;
		}

		return addPlan(plans, id, toNode, fromNode);
	}

	private boolean addPlan(Plans plans, int id, Node toNode, Node fromNode) {
		Person person = new Person("" + id, "f", "26", "yes", "always", "yes");
		Plan plan = person.createPlan(null, null, "yes");
		try {
			plans.addPerson(person);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		Link fromLink = fromNode.getInLinks().values().iterator().next();
		int startTime = (int) (Gbl.random.nextDouble() * 60*60*24);
		try {
			plan.createAct("w", -1, -1, fromLink, startTime, 0, 0, false);
			plan.createLeg("0", "car", null, null, null);
			Link toLink = toNode.getInLinks().values().iterator().next();
			int endTime = (int) (Gbl.random.nextDouble() * 60*60*2);
			plan.createAct("w", -1, -1, toLink, startTime + endTime, 0, 0, false);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	private ArrayList<Node> getToNodes(int fromCellRow, int fromCellColumn, ArrayList<Node>[][] cells) {
		ArrayList<Node> toNodes = new ArrayList<Node>();
		Coord fromCellCenter = getCellCenter(fromCellRow, fromCellColumn);
		if (2*cellSize >= this.givenFromToDistance) {
			for (int i = Math.max(fromCellRow-1, 0); i < Math.min(fromCellRow+2, cells.length); i++) {
				for (int j = Math.max(fromCellColumn-1, 0);
					j < Math.min(fromCellColumn+2, cells[0].length); j++) {
					if (!(i == fromCellRow && j == fromCellColumn)) {
						toNodes.addAll(cells[i][j]);
					}
				}
			}
		} else {
			for (int i = 0; i < cells.length; i++) {
				for (int j = 0; j < cells[0].length; j++) {
					Coord c = getCellCenter(j, i);
					double dist = c.calcDistance(fromCellCenter);
					if (dist > (givenFromToDistance - cellSize)
							&& dist < (givenFromToDistance + cellSize)) {
						toNodes.addAll(cells[i][j]);
					}
				}
			}
		}
		return toNodes;
	}

	private Coord getCellCenter(int fromCellRow, int fromCellColumn) {
		return new Coord(minX + cellSize*(fromCellColumn+0.5),
				minY + (fromCellRow+0.5));
	}

	@Override
	public void run(NetworkLayer network) {
		ArrayList<Node>[][] cells = initCells(network);
		// Now generate the trips
		generateTrips(network, cells);
	}

	public void runDumb(NetworkLayer network) {
		generateTrips(network);
	}

	public Plans getPlans() {
		return this.plans;
	}

}
