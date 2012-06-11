/* *********************************************************************** *
 * project: org.matsim.*
 * SpatialGrid.java
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
package playground.tnicolai.matsim4opus.gis;

import java.io.BufferedReader;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

import playground.tnicolai.matsim4opus.utils.io.writer.SpatialGridTableWriter;

import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 * @author thomas
 */
public class SpatialGrid{
	
	private static final Logger log = Logger.getLogger(SpatialGrid.class);

	private double[][] matrix;
	
	private final double minX;
	
	private final double minY;
	
	private final double maxX;
	
	private final double maxY;
	
	private final double resolution;
	
	public SpatialGrid(double xmin, double ymin, double xmax, double ymax, double resolution) {
		minX = xmin;
		minY = ymin;
		maxX = xmax;
		maxY = ymax;
		this.resolution = resolution;
		int numXBins = (int)Math.ceil((maxX - minX) / resolution) + 1;
		int numYBins = (int)Math.ceil((maxY - minY) / resolution) + 1;
		matrix = new double[numYBins][numXBins];
		
		// init matrix
		for(int x = 0; x < numXBins; x++)
			for(int y = 0; y < numYBins; y++)
				matrix[y][x] = Double.NaN;
	}
	
	public SpatialGrid(double [] boundingBox, double resolution) {
		this(boundingBox[0], boundingBox[1], boundingBox[2], boundingBox[3], resolution);
	}
	
	public SpatialGrid(SpatialGrid grid) {
		this(grid.getXmin(), grid.getYmin(), grid.getXmax(), grid.getYmax(), grid.getResolution());
	}
	
	public double getXmin() {
		return minX;
	}
	
	public double getYmin() {
		return minY;
	}
	
	public double getXmax() {
		return maxX;
	}
	
	public double getYmax() {
		return maxY;
	}
	
	public double getResolution() {
		return resolution;
	}
	
	public int getNumRows() {
		return matrix.length;
	}
	
	public int getNumCols(int row) {
		return matrix[row].length;
	}

	public double getValue(Point point) {
		if(isInBounds(point))
			return getValue(point.getX(), point.getY());
//			return matrix[getRow(point.getY())][getColumn(point.getX())];
//		
//		log.warn("This point lies outside the boundary!!!");
//		log.warn("Boundary: xmin:"+this.minX+", ymin:"+ this.minY+", xmax:"+this.maxX+", maxy:"+this.maxY);
//		log.warn("Point: x:"+point.getX()+ ", y:"+point.getY());
		return Double.NaN;
	}
	
	public double getValue(double x, double y){
		if(isInBounds(x, y))
			return matrix[getRow(y)][getColumn(x)];
		
		log.warn("This point lies outside the boundary!!!");
		log.warn("Boundary: xmin:"+this.minX+", ymin:"+ this.minY+", xmax:"+this.maxX+", maxy:"+this.maxY);
		log.warn("Point: x:"+x+ ", y:"+y);
		return Double.NaN;
	}
	
	public boolean setValue(double value, Point point) {
		if(isInBounds(point)) {
			matrix[getRow(point.getY())][getColumn(point.getX())] = value;
			return true;
		} else
			return false;
	}
	
	public boolean isInBounds(Point point) {
		return point.getX() >= minX && point.getX() <= maxX &&
			   point.getY() >= minY && point.getY() <= maxY;
	}
	
	public boolean isInBounds(double x, double y){
		return (x >= minX && x <= maxX && y >= minY && y <= maxY);
	}
	
	public double getValue(int row, int col) {
		return matrix[row][col];
	}
	
	public boolean setValue(int row, int col, double value) {
		if(row < matrix.length) {
			if(col < matrix[row].length) {
				matrix[row][col] = value;
				return true;
			} else
				return false;
		} else
			return false;
	}
	
	public int getRow(double yCoord) {
		return matrix.length - 1 - (int)Math.floor((yCoord - minY) / resolution);
	}
	
	public int getColumn(double xCoord) {
		return (int)Math.floor((xCoord - minX) / resolution);
	}
	
	public double[][] getMatrix(){
		return flip(matrix);
	}
	
	/**
	 * flips the given matrix horizontal
	 * 
	 * @param matrix
	 * @return the horizontal mirrored matrix
	 */
	private static double[][] flip(double[][] matrix) {
		double[][] flip= new double[matrix.length][matrix[0].length];
		for (int i=0; i<flip.length; i++){
			for (int j=0; j<flip[0].length; j++){
				flip[i][j]= matrix[matrix.length-1-i][j];
			}
		}
		return flip;
	}
	
	/**
	 * creates a Spatial Grid and initializes it with the values from a given
	 * file
	 * 
	 * @param filename
	 * @return SpatialGrid
	 */
	public static SpatialGrid readFromFile(String filename){
		
		BufferedReader reader = IOUtils.getBufferedReader(filename);
		String line;
		SpatialGrid sg = null;
		try{
			sg = initSpatialGrid(filename);
			// fill spatial grid
			String[] header = reader.readLine().split(SpatialGridTableWriter.separator);
			String[] parts;
			double yCoord, xCoord, value;
			
			while((line = reader.readLine()) != null){
				parts = line.split(SpatialGridTableWriter.separator);
				
				if( parts.length == header.length ){
				
					yCoord = Double.parseDouble(parts[0]);

					for(int i = 1; i < header.length; i++){
						
						xCoord = Double.parseDouble(header[i]);
						value = Double.parseDouble(parts[i]);
						
						sg.setValue(sg.getRow(yCoord), sg.getColumn(xCoord), value);
					}
				}
			}
		} catch(IOException ioe){
			ioe.printStackTrace();
		}
		return sg;
	}
	
	/**
	 * determines the boundary and resolution of the study area 
	 * than this initializes and returns a Spatial Grid with these settings
	 * @param filename
	 * @return SpatialGrid
	 * @throws IOException
	 */
	private static SpatialGrid initSpatialGrid(String filename) throws IOException {
		BufferedReader reader = IOUtils.getBufferedReader(filename);
		String line;
		double xmin= 0.; 
		double ymin= 0.; 
		double xmax= 0.; 
		double ymax= 0.;
		double ytmp= 0.;
		double res = 0.;

		line = reader.readLine();
		String[] header = line.split(SpatialGridTableWriter.separator);

		if (header.length > 1) {
			xmin = Double.parseDouble(header[1]);
			xmax = Double.parseDouble(header[header.length - 1]);
			res = (xmax - xmin) / (header.length - 2);

			boolean firstLine = true;
			while ((line = reader.readLine()) != null) {
				ytmp = Double.parseDouble(line
						.split(SpatialGridTableWriter.separator)[0]);
				if (firstLine) {
					ymin = ytmp;
					firstLine = false;
				}
			}
			ymax = ytmp;

			System.out.println(xmin + "," + ymin + "," + xmax + "," + ymax
					+ "," + res);
			SpatialGrid sg = new SpatialGrid(xmin, ymin, xmax, ymax, res);
			return sg;
		}
		return null;
	}
	
	/**
	 * just for debugging convenience 
	 * @param filename
	 */
	public void writeToFile(String filename){
		GridUtils.writeSpatialGridTable(this, filename);
	}
	
	/**
	 * for testing
	 * @param args
	 */
	public static void main(String args[]){
		
		SpatialGrid test = SpatialGrid.readFromFile("/Users/thomas/Development/opus_home/data/seattle_parcel/results/interpolationQuickTest/results/400.0travel_time_accessibility.txt");// SpatialGrid.readFromFile("/Users/thomas/Development/opus_home/matsim4opus/tmp/carAccessibility_cellsize_100.0SF.txt");
		test.writeToFile("/Users/thomas/Development/opus_home/data/seattle_parcel/results/interpolationQuickTest/results/400.0travel_time_accessibilityTEST.txt");
	}

}
