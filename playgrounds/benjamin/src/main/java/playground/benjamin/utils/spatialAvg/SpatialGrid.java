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

package playground.benjamin.utils.spatialAvg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;


public class SpatialGrid {

	private Cell [][] grid;
	private Double gridMinX;
	private Double gridMaxX;
	private Double gridMinY;
	private Double gridMaxY;
	private int numberOfCellsX;
	private int numberOfCellsY;
	private double gridWidth;
	private double gridHeight;
	private double cellWidth;
	private double cellHeight;
	
	public SpatialGrid(int numberOfCellsX, int numberOfCellsY, Double gridMinX, Double gridMaxX, Double gridMinY, Double gridMaxY){
		this.gridMinX= gridMinX;
		this.gridMaxX= gridMaxX;
		this.gridMinY= gridMinY;
		this.gridMaxY= gridMaxY;
		this.numberOfCellsX = numberOfCellsX;
		this.numberOfCellsY = numberOfCellsY;
		this.gridWidth = gridMaxX-gridMinX;
		this.cellWidth = gridWidth/numberOfCellsX;
		this.gridHeight = gridMaxY-gridMinY;
		this.cellHeight = gridHeight/numberOfCellsY;
		this.grid = new Cell [numberOfCellsX][numberOfCellsY];
		// initialize grid with 0.0

		for(int i=0; i<numberOfCellsX; i++){
			for(int j=0; j<numberOfCellsY; j++){
				Coord cellCentroid = new Coord(gridMinX + (i + 0.5) * cellWidth, gridMinY + (j + 0.5) * cellHeight);
				grid[i][j]= new Cell(i,j,cellCentroid);
			}
		}
	}
	
	public SpatialGrid(SpatialAveragingInputData inputData, int noOfXbins,int noOfYbins) {
		this(noOfXbins, noOfYbins, inputData.getMinX(), inputData.getMaxX(), inputData.getMinY(), inputData.getMaxY());
	}

	public void addLinkValue(Link link, EmissionsAndVehicleKm emissionsAndVehicleKm, LinkWeightUtil linkWeightUtil){
		for(int i=0; i<numberOfCellsX; i++){
			for(int j=0; j<numberOfCellsY; j++){
				Cell currentCell = grid[i][j];
				Double weight = linkWeightUtil.getWeightFromLink(link, currentCell.getCentroid());
				Double weightedEmissionValue = emissionsAndVehicleKm.getEmissionValue() * weight;
				Double weightedVehicleKm = emissionsAndVehicleKm.getLinkLenghtKm() * weight;
				currentCell.addWeightedValue(weightedEmissionValue);
				currentCell.addWeight(weightedVehicleKm);
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
	
	public void addWeightedValueToCell (int xCell, int yCell, double value){
		this.grid[xCell][yCell].addWeightedValue(value);
	}

	public void addWeightedValueToCell(Cell cell, double d) {
		this.addWeightedValueToCell(cell.getXNumber(), cell.getYNumber(), d);
		
	}
	
	public Cell[][] getGrid(){
		return grid;
	}

	public List<Cell> getCells() {
		List<Cell> listOfAllCells = new ArrayList<Cell>();
		
		for(int i=0; i<numberOfCellsX; i++){
			for(int j=0; j<numberOfCellsY; j++){
				listOfAllCells.add(grid[i][j]);
			}
		}
		
		return listOfAllCells;
	}

	public void distributeAndAddWelfare(Coord homeCoord, Double utilityValue, LinkWeightUtil linkWeightUtil, Double scalingFactor) {
		for(int i=0; i<numberOfCellsX; i++){
			for(int j=0; j<numberOfCellsY; j++){
				Cell currentCell = grid[i][j];
				Double weight = linkWeightUtil.getWeightFromCoord(homeCoord, currentCell.getCentroid());
				Double weightedUtilityValue = utilityValue * weight * scalingFactor;
				Double weightedCount = 1.0 * weight * scalingFactor;
				currentCell.addWeightedValue(weightedUtilityValue);
				currentCell.addWeight(weightedCount);
			}
		}
		
	}

	public SpatialGrid getDifferencesAAverages(SpatialGrid baseCaseGrid) {
		SpatialGrid differences = new SpatialGrid(numberOfCellsX, numberOfCellsY, gridMinX, gridMaxX, gridMinY, gridMaxY);
		for(int i=0; i<numberOfCellsX; i++){
			for(int j=0; j<numberOfCellsY; j++){
				double weightedValueDifference = this.grid[i][j].getWeightedValue()-baseCaseGrid.grid[i][j].getWeightedValue();
				double averageDifference = this.grid[i][j].getAverageValue() - baseCaseGrid.grid[i][j].getAverageValue();
				double weightDifference = weightedValueDifference/averageDifference;
				differences.grid[i][j].addWeightedValue(weightedValueDifference);
				differences.grid[i][j].addWeight(weightDifference);
			}
		}
		return differences;
	}

	public Double getAverageWeightedValuePerCell() {
			Double sum = 0.0;
			for(int i=0; i<numberOfCellsX; i++){
				for(int j=0; j<numberOfCellsY; j++){
					sum +=grid[i][j].getWeightedValue();
				}
			}
			return (sum/numberOfCellsX/numberOfCellsY);
	}

	public Map<Link, Cell> getLinks2GridCells(
			Collection<? extends Link> links) {
			HashMap<Id<Link>, Cell> linkIds2cells = new HashMap<Id<Link>, Cell>();
			HashMap<Link, Cell> links2cells = new HashMap<Link, Cell>();
			
			for(Link link: links){
				Cell cCell = this.getCellForCoordinate(link.getCoord());
				if(cCell!=null){
					links2cells.put(link, cCell);
					linkIds2cells.put(link.getId(), cCell);
				}
			}		
		
	return links2cells;
	}
}
