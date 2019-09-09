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

package playground.vsp.airPollution.exposure;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

public class Cell {

	private final int x;
	private final int y;
	
	public Cell(int x, int y){
		this.x= x;
		this.y= y;
	}
	
	public int getX(){
		return this.x;
	}
	
	public int getY(){
		return this.y;
	}
	
	public int getDistanceBetweenCells(Cell cell){
		int distance = Math.abs(this.x-cell.getX());
		distance += Math.abs(this.y-cell.getY());
		return distance;
	}
	
	public List<Cell> getNearbyCells(int noOfXCells, int noOfYCells, int distance){
		if(distance<0){
			Logger.getLogger(Cell.class).warn("Negative distance - will not return any nearby neighbors.");
			return null;
		}
		List<Cell> nearbyCells = new ArrayList<Cell>();
		for(int i= this.x -distance; i<=this.x+distance; i++){
			if (i>=0 && i<= noOfXCells) {
				for (int j = this.y - distance; j <= this.y + distance; j++) {
					if (j>=0 && j<= noOfYCells) {
						Cell nearby = new Cell(i, j);
						if (this.getDistanceBetweenCells(nearby) < distance) {
							nearbyCells.add(nearby);
						}
					}
				}
			}
		}
		return nearbyCells;
	}

	public List<Cell> getCellsWithExactDistance(int noOfXCells, int noOfYCells, int distance) {
		if(distance<0){
			Logger.getLogger(Cell.class).warn("Negative distance - will not return any nearby neighbors.");
			return null;
		}
		List<Cell> cells = new ArrayList<Cell>();
		
		for(int i = this.x-distance; i< this.x+ distance+1; i++){
			if(i>=0 && i<= noOfXCells){
				int xDistance = Math.abs(this.x-i);
				int y1 = -distance + xDistance + this.y;
				int y2 = distance - xDistance + this.y;
				if(y1>=0 && y1<= noOfYCells){
					cells.add(new Cell(i, y1));
				}
				if(y2>=0 && y2 <= noOfYCells && y2-y1!=0){
					cells.add(new Cell(i, y2));
				}
			}
		}
		
		return cells;
	}
	
	public String toString(){
		return "x = " + this.x + " y = " + this.y;
	}
}
