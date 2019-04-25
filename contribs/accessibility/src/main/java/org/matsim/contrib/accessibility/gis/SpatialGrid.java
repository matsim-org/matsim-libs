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
package org.matsim.contrib.accessibility.gis;

import java.io.BufferedReader;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox;
import org.matsim.core.utils.io.IOUtils;

/**
 * The spatial grid saves the data values of a study area in a matrix.
 * Intern the matrix is mirrored horizontal.
 * The methods getValue and setValue compensate the mirroring process (so altogether the class works consistent).
 * In contrast to the methods above the method getMirroredValue(int row, int col) returns the intern mirrored value.
 * 
 * 
 * @author illenberger
 * @author thomas
 * @author tthunig
 */
@Deprecated
public final class SpatialGrid{
	private static final Logger LOG = Logger.getLogger(SpatialGrid.class);
	
	private double[][] matrix;
	
	private final double minX;
	
	private final double minY;
	
	private final double maxX;
	
	private final double maxY;
	
	private final double resolution;
	
	private String label = "" ;
	public void setLabel( String str ) {
		label = str ;
	}
	public String getLabel() {
		return label ;
	}
	
	/**
	 * @param xmin
	 * @param ymin
	 * @param xmax
	 * @param ymax
	 * @param resolution cell size. E.g. (xmax-xmin)/resolution = number of cells in x direction
	 * @param initialValue TODO
	 */
	public SpatialGrid(double xmin, double ymin, double xmax, double ymax, double resolution, double initialValue) {
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
				matrix[y][x] = initialValue ;
	}
	
	/**
	 * @param boundingBox
	 * @param resolution cell size. E.g. (xmax-xmin)/resolution = number of cells in x direction
	 */
	public SpatialGrid(double [] boundingBox, double resolution) {
		this(boundingBox[0], boundingBox[1], boundingBox[2], boundingBox[3], resolution, Double.NaN);
	}
	
	public SpatialGrid( BoundingBox bbox, double resolution, double initialValue ) {
		this( bbox.getXMin(), bbox.getYMin(), bbox.getXMax(), bbox.getYMax(), resolution, initialValue ) ;
	}
	
	public SpatialGrid(SpatialGrid grid) {
		this(grid.getXmin(), grid.getYmin(), grid.getXmax(), grid.getYmax(), grid.getResolution(), Double.NaN);
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

	/**
	 * independent of the intern representation (mirrored) this method returns the initial value at the given point
	 *  
	 * @param point
	 * @return the initial value at the given point
	 */
	public double getValue(Point point) {
		if(isInBounds(point))
			return getValue(point.getX(), point.getY());
		
		// log.warn("This point lies outside the boundary!!!");
		// log.warn("Boundary: xmin:"+this.minX+", ymin:"+ this.minY+", xmax:"+this.maxX+", maxy:"+this.maxY);
		// log.warn("Point: x:"+point.getX()+ ", y:"+point.getY());
		return Double.NaN;
	}
	
	public double getValue( Coord coord ) {
		if ( isInBounds( coord ) ) {
			return getValue( coord.getX(), coord.getY() ) ;
		} else {
			return Double.NaN ;
		}
	}
	
	/**
	 * independent of the intern representation (mirrored) this method returns the initial value at the given point (x, y)
	 * 
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @return the initial value at the point (x, y)
	 */
	public double getValue(double x, double y){
		if(isInBounds(x, y))
			return matrix[getRow(y)][getColumn(x)];
		
		// log.warn("This point lies outside the boundary!!!");
		// log.warn("Boundary: xmin:"+this.minX+", ymin:"+ this.minY+", xmax:"+this.maxX+", maxy:"+this.maxY);
		// log.warn("Point: x:"+x+ ", y:"+y);
		return Double.NaN;
	}
	
	/**
	 * independent of the intern representation (mirrored) this method sets the given value at the initial point if it lies in the bounds of the study area
	 * 
	 * @param value
	 * @param point
	 * @return true if the point lies in the bounds of the study area, false otherwise
	 */
	public boolean setValue(double value, Point point) {
		if(isInBounds(point)) {
			setValue(value, point.getX(), point.getY());
			return true;
		} else
			return false;
	}
	
	public boolean addToValue( double value, Coord coord ) {
		if ( isInBounds(coord) ) {
			addToValue( value, coord.getX(), coord.getY() ) ;
			return true ;
		} else {
			return false ;
		}
	}
	
	public boolean isInBounds(Coord coord) {
		return isInBounds( coord.getX() , coord.getY() );
	}

	/**
	 * independent of the intern representation (mirrored) this method sets the given value at the initial point if it lies in the bounds of the study area
	 * 
	 * @param value
	 * @param x the x coordinate of the point to set
	 * @param y the y coordinate of the point to set
	 * @return true if the point lies in the bounds of the study area, false otherwise
	 */
	public boolean setValue(double value, double x, double y){
		if (isInBounds(x, y)){
			matrix[getRow(y)][getColumn(x)] = value;
			return true;
		} else
			return false;
	}
	
	public boolean addToValue( double value, double x, double y ) {
		if ( isInBounds(x,y)) {
			if ( Double.isNaN(matrix[getRow(y)][getColumn(x)])) {
				return setValue( value, x, y ) ;
			} else {
				matrix[getRow(y)][getColumn(x)] += value;
				return true ;
			}
		} else {
			return false ;
		}
	}
	
	public boolean isInBounds(Point point) {
		return isInBounds( point.getX(), point.getY() ) ;
	}
	
	public boolean isInBounds(double x, double y){
		return (x >= minX && x <= maxX && y >= minY && y <= maxY);
	}
	
	/**
	 * sets the value at matrix[row][col] so the value in the mirrored intern representation
	 * 
	 * @param row the row number
	 * @param col the column number
	 * @param value the value at matrix[row][col]
	 * @return true if the row and column numbers lie in the bounds, false otherwise
	 */
	@Deprecated
	private boolean setMirroredValue(int row, int col, double value) {
		if(row < matrix.length) {
			if(col < matrix[row].length) {
				matrix[row][col] = value;
				return true;
			} else
				return false;
		} else
			return false;
	}
	
	/**
	 * returns the row number of the y coordinate in the intern representation (mirrored)
	 * 
	 * @param yCoord the y coordinate
	 * @return the row number of the y coordinate 
	 */
	public int getRow(double yCoord) {
		return matrix.length - 1 - (int)Math.floor((yCoord - minY) / resolution);
	}
	
	/**
	 * returns the column number of the x coordinate in the intern representation
	 * 
	 * @param xCoord the x coordinate
	 * @return the column number of the x coordinate
	 */
	public int getColumn(double xCoord) {
		return (int)Math.floor((xCoord - minX) / resolution);
	}
	
	/**
	 * independent of the intern representation (mirrored) this method returns the initial data as a matrix (remirrored)
	 * for example the value at (xmin, ymin) will be at the bottom left corner of the returned matrix
	 * hence it fits with the coordinate system if you plot the matrix
	 * 
	 * @return the initial data as a matrix
	 */
	public double[][] getMatrix(){
//		return flip(matrix);
		return matrix;
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
						
						sg.setMirroredValue(sg.getRow(yCoord), sg.getColumn(xCoord), value);
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
			SpatialGrid sg = new SpatialGrid(xmin, ymin, xmax, ymax, res, Double.NaN);
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
