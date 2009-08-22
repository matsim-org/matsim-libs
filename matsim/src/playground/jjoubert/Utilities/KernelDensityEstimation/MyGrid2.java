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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Scanner;

import org.apache.log4j.Logger;
import org.matsim.core.utils.collections.QuadTree;


import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;


public class MyGrid2 {
	
	private final static Logger log = Logger.getLogger(MyGrid2.class);
	private QuadTree<MyGridCell> grid;
	private Envelope gridEnvelope;
	private Geometry studyArea;
	private float gridLongitude;
	private float gridLatitude;
	private Integer numberOfTimeBins;
	private String inputFilename;
	private QuadTree<ActivityPoint> pointTree;
		
	public MyGrid2(Geometry studyArea, float gridWidth, float gridHeight, int numberOfTimeBins, String inputFilename){
		this.studyArea = studyArea;
		this.gridLongitude = gridWidth;
		this.gridLatitude = gridHeight;
		this.numberOfTimeBins = numberOfTimeBins;
		this.inputFilename = inputFilename;
	}
	
	public QuadTree<ActivityPoint> getPointTree() {
		return pointTree;
	}

	public void readPoints(){
		// Build empty quad tree
		Geometry envelope = this.studyArea.getEnvelope();
		if(envelope instanceof Polygon){
			Coordinate[] corners = envelope.getCoordinates();
			if(corners.length == 5){
				this.gridEnvelope = new Envelope(corners[0].x, corners[2].x, corners[0].y, corners[2].y);
				// Create empty QuadTree
				this.pointTree = new QuadTree<ActivityPoint>(corners[0].x, corners[0].y, corners[2].x, corners[2].y);
			} else{
				log.error("Envelope of the study area provided in constructor is not rectangular!!");
			}
		} else{
			log.error("Envelope of the study area provided in constructor is not a polygon!!");
		}
		
		// Populate the quad tree
		GeometryFactory gf = new GeometryFactory();
		try {
			Scanner input = new Scanner(new BufferedReader(new FileReader(new File(this.inputFilename))));
			int lineCounter = 0;
			int lineMultiplier = 1;
			
			// Read header
			String[] line;
			input.nextLine();
			
			double x;
			double y;
			Integer hour;
			while(input.hasNextLine()){
				if(lineCounter == lineMultiplier){
					log.info("Number of activities processed: " + lineCounter);
					lineMultiplier *= 2;
				}
				line = input.nextLine().split(",");
				if(line.length == 5){
					x = Double.parseDouble(line[1]);
					y = Double.parseDouble(line[2]);
					int position = line[3].indexOf("H");
					hour = Integer.parseInt(line[3].substring(position-2, position));
					if(this.gridEnvelope.contains(x, y)){
						Point p = gf.createPoint(new Coordinate(x, y));
						ActivityPoint ap = new ActivityPoint(p, Integer.valueOf(hour));
						this.pointTree.put(x, y, ap);
					} 
//					else{
//						/*
//						 * This point is outside the envelope... drop it ;-)
//						 */
//					}
					lineCounter++;
				}
			}
			log.info("Number of activities processed: " + lineCounter + " (Completed)");
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
	}
	
	public void processCells(String outputFilename, float radius){
		
		GeometryFactory gf = new GeometryFactory();
		
		ArrayList<BufferedWriter> bwList = new ArrayList<BufferedWriter>(numberOfTimeBins);
		int position = outputFilename.indexOf(".");
		String firstPart = outputFilename.substring(0, position);
		String secondPart = outputFilename.substring(position, outputFilename.length());
		try {
			for(int i = 0; i < numberOfTimeBins; i++){
				String addString = i < 10 ? "0" : "";
				BufferedWriter bw = new BufferedWriter(new FileWriter(new File(firstPart + "_H" + addString + String.valueOf(i) + secondPart)));
				bw.write("ID,XMIN,YMIN,XMAX,YMAX,ACTIVITY_COUNT");
				bw.newLine();
				bwList.add(bw);
			}
			try{
				double minX = this.gridEnvelope.getMinX();
				double minY = this.gridEnvelope.getMinY();
				int numberOfLongitudeCells = (int) Math.ceil(this.gridEnvelope.getWidth() / gridLongitude);
				int numberOfLatitudeCells = (int) Math.ceil(this.gridEnvelope.getHeight() / gridLatitude);
				long totalCells = numberOfLongitudeCells * numberOfLatitudeCells;
				log.info("Total number of cells to process: " + String.valueOf(totalCells));
				log.info("Start processing cells. this may take a while.");
				long cellMultiplier = 1;
				int cellId = 0;
				for(int x = 0; x < numberOfLongitudeCells; x++){
					for(int y = 0; y < numberOfLatitudeCells; y++){
						if(cellId == cellMultiplier){
							log.info("Cells processed: " + cellId);
							cellMultiplier *= 2;
						}
						MyGridCell cell = new MyGridCell(cellId,
								minX + ((x)  *gridLongitude),
								minX + ((x+1)*gridLongitude),
								minY + ((y)  *gridLatitude),
								minY + ((y+1)*gridLatitude),
								24);
						double xCentre = cell.centre().x;
						double yCentre = cell.centre().y;
						Point pCentre = gf.createPoint(cell.centre());
						for (ActivityPoint ap : pointTree.get(xCentre, yCentre, radius)) {
							double distance = ap.getPoint().distance(pCentre);
//							double impact = this.triangularKernel(distance, radius);
							double impact = this.exponentialKernel(distance, radius);
							cell.addToHourCount(ap.getHour(), impact);
						}
						this.writeKdeCell(bwList, cell);
						cellId++;
					}
				}
				log.info("Cells processed: " + cellId + " (Completed)");

				
				
				
			}finally{
				for (BufferedWriter bw : bwList) {
					bw.write("END");
					bw.newLine();
					bw.close();
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}

		
	}

	
	private void writeKdeCell(ArrayList<BufferedWriter> bwList, MyGridCell cell) throws IOException {

		for(int i = 0; i < bwList.size(); i++){
			BufferedWriter output = bwList.get(i);
			output.write(String.valueOf(cell.getId()));
			output.write(",");
			output.write(String.valueOf(cell.getMinX()));
			output.write(",");
			output.write(String.valueOf(cell.getMinY()));
			output.write(",");
			output.write(String.valueOf(cell.getMaxX()));
			output.write(",");
			output.write(String.valueOf(cell.getMaxY()));
			output.write(",");
			output.write(String.valueOf(cell.getHourCount().get(i)));
			output.newLine();
		}

		
	}

//	private double triangularKernel(double distance, double radius) {
//		double result = ((-1 / radius) * distance) + 1;
//		return result;
//	}

	private double exponentialKernel(double distance, double radius) {
		double A = 1;
		double k = 2;
		double result = A*Math.exp(-k*(distance) / (radius / 2));
		return result;
	}
	
//	private void buildGrid(){
//		//TODO Build a grid
//		double minX = this.gridEnvelope.getMinX();
//		double minY = this.gridEnvelope.getMinY();
//		int numberOfLongitudeCells = (int) Math.ceil(this.gridEnvelope.getWidth() / gridLongitude);
//		int numberOfLatitudeCells = (int) Math.ceil(this.gridEnvelope.getHeight() / gridLatitude);
//		long totalCells = numberOfLongitudeCells * numberOfLatitudeCells;
//		log.info("Total number of cells to create: " + String.valueOf(totalCells));
//		log.info("Building QuadTree of cells... this may take a while.");
//		long cellMultiplier = 1;
//		int cellId = 0;
//		for(int x = 0; x < numberOfLongitudeCells; x++){
//			for(int y = 0; y < numberOfLatitudeCells; y++){
//				if(cellId == cellMultiplier){
//					log.info("Cells added: " + String.valueOf(cellId));
//					cellMultiplier *= 2;
//				}
//				MyGridCell newCell = new MyGridCell(cellId,
//													minX + ((x)  *gridLongitude),
//													minX + ((x+1)*gridLongitude),
//													minY + ((y)  *gridLatitude),
//													minY + ((y+1)*gridLatitude),
//													numberOfTimeBins);
//				/*
//				 * TODO For now I am going to just create all the grid cells within the 
//				 * envelope. At a later stage I can, IF IT IS EFFICIENT, only add
//				 * grid cells that are WITHIN the study area. The final grid can then be
//				 * 'clipped' in ArcGIS, if you want.
//				 */
//				this.grid.put(newCell.centre().x, newCell.centre().y, newCell);
//				
//				cellId++;
//			}
//		}
//		log.info("Cells added: " + String.valueOf(cellId) + " (Complete)");
//	}
	
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
				if(line.length == 5){
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
