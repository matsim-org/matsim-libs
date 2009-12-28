/* *********************************************************************** *
 * project: org.matsim.*
 * MyGrid.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.jjoubert.Utilities.KernelDensityEstimation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Collection;
import java.util.Scanner;

import org.apache.log4j.Logger;
import org.matsim.core.utils.collections.QuadTree;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;


public class MyGrid {
	
	private final static Logger log = Logger.getLogger(MyGrid.class);
	private QuadTree<MyGridCell> grid;
	private Envelope gridEnvelope;
	private Geometry studyArea;
	private double gridLongitude;
	private double gridLatitude;
	private Integer numberOfTimeBins;
	
	public MyGrid(Geometry studyArea, double gridWidth, double gridHeight){
		this.studyArea = studyArea;
		this.gridLongitude = gridWidth;
		this.gridLatitude = gridHeight;
		Geometry envelope = this.studyArea.getEnvelope();
		if(envelope instanceof Polygon){
			Coordinate[] corners = envelope.getCoordinates();
			if(corners.length == 5){
				this.gridEnvelope = new Envelope(corners[0].x, corners[2].x, corners[0].y, corners[2].y);
				// Create empty QuadTree
				this.grid = new QuadTree<MyGridCell>(corners[0].x, corners[0].y, corners[2].x, corners[2].y);
			} else{
				log.error("Envelope of the study area provided in constructor is not rectangular!!");
			}
		} else{
			log.error("Envelope of the study area provided in constructor is not a polygon!!");
		}
		this.numberOfTimeBins = null;
		this.buildGrid();
	}
	
	public MyGrid(Geometry studyArea, double gridWidth, double gridHeight, int numberOfTimeBins){
		this.studyArea = studyArea;
		this.gridLongitude = gridWidth;
		this.gridLatitude = gridHeight;
		Geometry envelope = this.studyArea.getEnvelope();
		if(envelope instanceof Polygon){
			Coordinate[] corners = envelope.getCoordinates();
			if(corners.length == 5){
				this.gridEnvelope = new Envelope(corners[0].x, corners[2].x, corners[0].y, corners[2].y);
				// Create empty QuadTree
				this.grid = new QuadTree<MyGridCell>(corners[0].x, corners[0].y, corners[2].x, corners[2].y);
			} else{
				log.error("Envelope of the study area provided in constructor is not rectangular!!");
			}
		} else{
			log.error("Envelope of the study area provided in constructor is not a polygon!!");
		}
		this.numberOfTimeBins = numberOfTimeBins;
		this.buildGrid();
	}

	
	private void buildGrid(){
		//TODO Build a grid
		double minX = this.gridEnvelope.getMinX();
		double minY = this.gridEnvelope.getMinY();
		int numberOfLongitudeCells = (int) Math.ceil(this.gridEnvelope.getWidth() / gridLongitude);
		int numberOfLatitudeCells = (int) Math.ceil(this.gridEnvelope.getHeight() / gridLatitude);
		long totalCells = numberOfLongitudeCells * numberOfLatitudeCells;
		log.info("Total number of cells to create: " + String.valueOf(totalCells));
		log.info("Building QuadTree of cells... this may take a while.");
		long cellMultiplier = 1;
		int cellId = 0;
		for(int x = 0; x < numberOfLongitudeCells; x++){
			for(int y = 0; y < numberOfLatitudeCells; y++){
				if(cellId == cellMultiplier){
					log.info("Cells added: " + String.valueOf(cellId));
					cellMultiplier *= 2;
				}
				MyGridCell newCell = new MyGridCell(cellId,
													minX + ((x)  *gridLongitude),
													minX + ((x+1)*gridLongitude),
													minY + ((y)  *gridLatitude),
													minY + ((y+1)*gridLatitude),
													numberOfTimeBins);
				/*
				 * TODO For now I am going to just create all the grid cells within the 
				 * envelope. At a later stage I can, IF IT IS EFFICIENT, only add
				 * grid cells that are WITHIN the study area. The final grid can then be
				 * 'clipped' in ArcGIS, if you want.
				 */
				this.grid.put(newCell.centre().x, newCell.centre().y, newCell);
				
				cellId++;
			}
		}
		log.info("Cells added: " + String.valueOf(cellId) + " (Complete)");
	}
	
	/**
	 * Assumes a comma-separated file format.
	 * @param filename
	 */
	public void estimateActivities(String filename, double radius){
		int lineCounter = 0;
		int lineMultiplier = 1;
		try {
			Scanner input = new Scanner(new BufferedReader(new FileReader(new File(filename))));
			input.nextLine();
			
			while(input.hasNextLine()){
				if(lineCounter == lineMultiplier){
					log.info("Number of activities processed: " + lineCounter);
					lineMultiplier *= 2;
				}
				String[] line = input.nextLine().split(",");
				double x = Double.parseDouble(line[1]);
				double y = Double.parseDouble(line[2]);
				int position = line[3].indexOf("H");
				int hour = Integer.parseInt(line[3].substring(position-2, position));
				
				Collection<MyGridCell> cells = grid.get(x, y, radius);
				float value = ((float) 1) / ((float) cells.size());
				for (MyGridCell cell : cells) {
					cell.addToTotalCount(value);
					cell.addToHourCount(hour, value);
				}
				lineCounter++;
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		log.info("Number of activities processed: " + lineCounter + " (Completed)");		
	}
	
	public QuadTree<MyGridCell> getGrid() {
		return grid;
	}

	public Envelope getGridEnvelope() {
		return gridEnvelope;
	}

	public Geometry getStudyArea() {
		return studyArea;
	}

}
