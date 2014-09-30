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
	private double cellWidth;
	private double cellHeight;
	
	public SpatialGrid(int numberOfCellsX, int numberOfCellsY, Double gridMinX, Double gridMaxX, Double gridMinY, Double gridMaxY){
		this.gridMinX= gridMinX;
		this.gridMaxX= gridMaxX;
		this.gridMinY= gridMinY;
		this.gridMaxY= gridMaxY;
		this.numberOfCellsX = numberOfCellsX;
		this.numberOfCellsY = numberOfCellsY;
		this.cellWidth = gridMaxX-gridMinX;
		this.cellHeight = gridMaxY-gridMinY;
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
	
	public SpatialGrid(SpatialAveragingInputData inputData, int noOfXbins,int noOfYbins) {
		this(noOfXbins, noOfYbins, inputData.getMinX(), inputData.getMaxX(), inputData.getMinY(), inputData.getMaxY());
	}

	public void addLinkValue(Link link, Double value, LinkWeightUtil linkWeightUtil){
		for(int i=0; i<numberOfCellsX; i++){
			for(int j=0; j<numberOfCellsY; j++){
				Cell currentCell = grid[i][j];
				Double weight = linkWeightUtil.getWeightFromLink(link, currentCell.getCentroid());
				Double weightedValue = value * weight;
				currentCell.addWeightedValue(weightedValue);
				currentCell.addWeight(weight*link.getLength()/1000.);
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

	public SpatialGrid getDifferences(SpatialGrid spatialGrid) {
		SpatialGrid differences = new SpatialGrid(numberOfCellsX, numberOfCellsY, gridMinX, gridMaxX, gridMinY, gridMaxY);
		for(int i=0; i<numberOfCellsX; i++){
			for(int j=0; j<numberOfCellsY; j++){
				double weightDifference = this.grid[i][j].getWeight()-spatialGrid.grid[i][j].getWeight();
				double weightedValueDifference = this.grid[i][j].getWeightedValue()-spatialGrid.grid[i][j].getWeightedValue();
				differences.grid[i][j].addWeightedValue(weightedValueDifference);
				differences.grid[i][j].addWeight(weightDifference);
			}
		}
		return differences;
	}

	public Cell getCellForCoordinate(Coord coord) {
		int i = mapXCoordToGrid(coord.getX());
		int j = mapYCoordToGrid(coord.getY());
		if(i>=0 && j>=0) return this.grid[i][j];
		return null;
	}

	private int mapYCoordToGrid(double y) {
		int yGrid = (int) Math.floor((y-gridMinY)/cellHeight);
		if(yGrid>=0 && yGrid<numberOfCellsY) return yGrid;
		return -1;
	}

	private int mapXCoordToGrid(double x) {
		int xGrid = (int) Math.floor((x-gridMinX)/cellWidth);
		if(xGrid>=0 && xGrid<numberOfCellsX) return xGrid;
		return -1;
	}
}
