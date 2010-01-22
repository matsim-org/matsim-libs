/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeMatrix.java
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

/**
 * 
 */
package playground.johannes.socialnetworks.ivtsurveys;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.johannes.socialnetworks.graph.spatial.SpatialGrid;

/**
 * @author illenberger
 *
 */
public class TravelTimeMatrix {

//	private static final double xmin = 630000;
//	private static final double ymin = 200000;
//	private static final double xmax = 730000;
//	private static final double ymax = 300000;
	private static final double xmin = 668000;
	private static final double ymin = 232000;
	private static final double xmax = 698000;
	private static final double ymax = 262000;
	private static final double resolution = 1000;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String networkfile = args[0];
		String outputfile = args[1];
		ScenarioImpl scenario = new ScenarioImpl();
		NetworkLayer network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(networkfile);

		
		SpatialGrid<NodeImpl> grid = new SpatialGrid<NodeImpl>(xmin, ymin, xmax, ymax, resolution);
		
		for(int xcoord = (int)(xmin + resolution/2.0); xcoord <= xmax; xcoord += resolution) {
			for(int ycoord = (int)(ymin + resolution/2.0); ycoord <= ymax; ycoord += resolution) {
				Coord coord = new CoordImpl(xcoord, ycoord);
				NodeImpl node = network.getNearestNode(coord);
				grid.setValue(node, coord);
			}
		}
		
		FreespeedTravelTime freett = new FreespeedTravelTime();
		Dijkstra router = new Dijkstra(network, freett, freett);
		SpatialGrid<Integer> ttmatrix = new SpatialGrid<Integer>(0, 0, (xmax-xmin)/resolution, (ymax-ymin)/resolution, 1);
		int numRows = grid.getNumRows() - 1;
		int numCols = grid.getNumCols(0) - 1;
		for(int row = 0; row < numRows; row++) {
			for(int col = 0; col < numCols; col++) {
				NodeImpl source = grid.getValue(row, col);
				int souceCellIdx = row * col;
				for(int row2 = 0; row2 < numRows; row2++) {
					for(int col2 = 0; col2 < numCols; col2++) {
						NodeImpl target = grid.getValue(row2, col2);
						int targetCellIdx = row2 * col2;
						Path path = router.calcLeastCostPath(source, target, 0);
						ttmatrix.setValue(souceCellIdx, targetCellIdx, (int)path.travelTime);
					}
				}
				System.out.println(String.format("Processed %1$s of %2$s cells.", ((row+1) * col), (numRows * numCols)));
			}			
		}
		ttmatrix.toFile(outputfile);
		System.out.println("Done.");
	}
}
