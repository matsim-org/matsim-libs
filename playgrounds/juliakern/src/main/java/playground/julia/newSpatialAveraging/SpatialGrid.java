/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.julia.newSpatialAveraging;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.CoordImpl;


public class SpatialGrid {

	private Cell [][] grid;
	private Double gridMinX;
	private Double gridMaxX;
	private Double gridMinY;
	private Double gridMaxY;
	private int numberOfCellsX;
	private int numberOfCellsY;
	
	public SpatialGrid(int numberOfCellsX, int numberOfCellsY, Double gridMinX, Double gridMaxX, Double gridMinY, Double gridMaxY){
		this.gridMinX= gridMinX;
		this.gridMaxX= gridMaxX;
		this.gridMinY= gridMinY;
		this.gridMaxY= gridMaxY;
		this.numberOfCellsX = numberOfCellsX;
		this.numberOfCellsY = numberOfCellsY;
		this.grid = new Cell [numberOfCellsX][numberOfCellsY];
		// initialize grid with 0.0

		for(int i=0; i<numberOfCellsX; i++){
			for(int j=0; j<numberOfCellsY; j++){
				Coord cellCentroid = new CoordImpl(gridMinX + i*(gridMaxX-gridMinX)/numberOfCellsY,
													gridMinY + j*(gridMaxY-gridMinY)/numberOfCellsY);
				grid[i][j]= new Cell(i,j,cellCentroid);
			}
		}
	}
	
	public void addLinkValue(Link link, Double value, LinkWeightUtil linkWeightUtil){
		for(int i=0; i<numberOfCellsX; i++){
			for(int j=0; j<numberOfCellsY; j++){
				Cell currentCell = grid[i][j];
				Double weight = linkWeightUtil.getWeightFromLink(link, currentCell.getCentroid());
				Double weightedValue = value * weight;
				currentCell.addWeightedValue(weightedValue);
				currentCell.addWeight(weight);
			}
		}
	}
	
	public Double[][] getWeightedValuesOfGrid(){
		Double [][] table = new Double[numberOfCellsX][numberOfCellsY];
		for(int i=0; i<numberOfCellsX; i++){
			for(int j=0; j<numberOfCellsY; j++){
				table[i][j]= grid[i][j].getWeightedValue();
			}
		}
		return table;
	}
	
	public Double[][] getWeightsOfGrid(){
		Double [][] table = new Double[numberOfCellsX][numberOfCellsY];
		for(int i=0; i<numberOfCellsX; i++){
			for(int j=0; j<numberOfCellsY; j++){
				table[i][j] = grid[i][j].getWeight();
			}
		}
		return table;
	}
	
	public Double[][] getAverageValuesOfGrid(){
		Double [][] table = new Double[numberOfCellsX][numberOfCellsY];
		for(int i=0; i<numberOfCellsX; i++){
			for(int j=0; j<numberOfCellsY; j++){
				table[i][j] = grid[i][j].getAverageValue();
			}
		}
		return table;
	}

	public void multiplyAllCells(Double normalizationFactor) {
		for(int i=0; i<numberOfCellsX; i++){
			for(int j=0; j<numberOfCellsY; j++){
				grid[i][j].multiplyAllValues(normalizationFactor);
			}
		}	
	}
}
