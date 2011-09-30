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

import java.io.IOException;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.xml.sax.Attributes;

import com.vividsolutions.jts.geom.Point;


/**
 * @author illenberger
 *
 */
public class SpatialGrid<T> {
	
	private static final Logger logger = Logger.getLogger(SpatialGrid.class);

	private Object[][] matrix;
	
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
		matrix = new Object[numYBins][numXBins];
	}
	
	public SpatialGrid(SpatialGrid<?> grid) {
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
	
	@SuppressWarnings("unchecked")
	public T getValue(Point point) {
		if(isInBounds(point))
			return (T)matrix[getRow(point.getY())][getColumn(point.getX())];
		else
			return null;
	}
	
	public boolean setValue(T value, Point point) {
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
	
	@SuppressWarnings("unchecked")
	public T getValue(int row, int col) {
		return (T)matrix[row][col];
	}
	
	public boolean setValue(int row, int col, T value) {
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
	
	@SuppressWarnings("unchecked")
	public void toFile(String filename) {
		toFile(filename, (ObjectSerializer<T>) new DoubleSerializer());
	}
	
	public void toFile(String filename, ObjectSerializer<T> serializer) {
		GridXMLWriter<T> writer = new GridXMLWriter<T>(serializer);
		try {
			writer.write(this, filename);
		} catch (IOException e) {
			logger.fatal("IOException occured!", e);
		}
	}
	
	public static SpatialGrid<Double> readFromFile(String filename) {
		return readFromFile(filename, new DoubleSerializer());
	}
	
	public static <V> SpatialGrid<V> readFromFile(String filename, ObjectSerializer<V> serializer) {
		GridXMLParser<V> parser = new GridXMLParser<V>(serializer);
		parser.setValidating(false);
		try {
			parser.parse(filename);
			return parser.grid;
		} catch (Exception e) {
			logger.fatal("Exception during parsing occured!", e);
			e.printStackTrace();
			return null;
		}
	}
	
	private static class GridXMLParser<V> extends MatsimXmlParser {

		public static final String GRID_TAG = "grid";
		
		public static final String CELL_TAG = "cell";
		
		public static final String XMIN_TAG = "xmin";
		
		public static final String XMAX_TAG = "xmax";
		
		public static final String YMIN_TAG = "ymin";
		
		public static final String YMAX_TAG = "ymax";
		
		public static final String RES_TAG = "resolution";
		
		public static final String ROW_TAG = "row";
		
		public static final String COL_TAG = "col";
		
		public static final String VALUE_TAG = "value";
		
		private SpatialGrid<V> grid;
		
		private ObjectSerializer<V> serializer;

		public GridXMLParser(ObjectSerializer<V> serializer) {
			super();
			this.serializer = serializer;
		}
		
		@Override
		public void endTag(String name, String content, Stack<String> context) {
		}

		@Override
		public void startTag(String name, Attributes atts, Stack<String> context) {
			if(CELL_TAG.equalsIgnoreCase(name)) {
				int row = (int)getDouble(atts, ROW_TAG);
				int col = (int)getDouble(atts, COL_TAG);
				String data = atts.getValue(VALUE_TAG);
				if(data == null)
					throw new IllegalArgumentException(VALUE_TAG + " must be specified");
				else {
					if(!grid.setValue(row, col, serializer.decode(data)))
						logger.warn(String.format("Out of bounds! (row=%1$s, col=%2$s)",row, col));
				}
			} else if(GRID_TAG.equalsIgnoreCase(name)) {
				if(grid != null) {
					logger.fatal("Only one grid per file allowed!");
					throw new UnsupportedOperationException("Only one grid per file allowed!");
				} else {
					double xmin = getDouble(atts, XMIN_TAG);
					double xmax = getDouble(atts, XMAX_TAG);
					double ymin = getDouble(atts, YMIN_TAG);
					double ymax = getDouble(atts, YMAX_TAG);
					double resolution = getDouble(atts, RES_TAG);
					grid = new SpatialGrid<V>(xmin, ymin, xmax, ymax, resolution);
				}
			}
		}
		
		private double getDouble(Attributes atts, String qName) {
			String val = atts.getValue(qName);
			if(val == null)
				throw new IllegalArgumentException(qName + " must be specified!");
			else
				return Double.parseDouble(val);
		}
	}
	
	private static class GridXMLWriter<V> extends MatsimXmlWriter {
		
		private ObjectSerializer<V> serializer;
		
		public GridXMLWriter(ObjectSerializer<V> serializer) {
			this.serializer = serializer;
		}
		
		@SuppressWarnings("unchecked")
		public void write(SpatialGrid<V> grid, String filename) throws IOException {
			openFile(filename);
			writeXmlHead();
			
			writer.write("<");
			writer.write(GridXMLParser.GRID_TAG);
			writer.write(" ");
			writer.write(makeAttribute(GridXMLParser.XMIN_TAG, String.valueOf(grid.minX)));
			writer.write(makeAttribute(GridXMLParser.YMIN_TAG, String.valueOf(grid.minY)));
			writer.write(makeAttribute(GridXMLParser.XMAX_TAG, String.valueOf(grid.maxX)));
			writer.write(makeAttribute(GridXMLParser.YMAX_TAG, String.valueOf(grid.maxY)));
			writer.write(makeAttribute(GridXMLParser.RES_TAG, String.valueOf(grid.resolution)));
			writer.write(">");
			writer.write(NL);
			
			for(int row = 0; row < grid.matrix.length; row++) {
				for(int col = 0; col < grid.matrix[row].length; col++) {
					writer.write("\t<");
					writer.write(GridXMLParser.CELL_TAG);
					writer.write(" ");
					writer.write(makeAttribute(GridXMLParser.ROW_TAG, String.valueOf(row)));
					writer.write(makeAttribute(GridXMLParser.COL_TAG, String.valueOf(col)));
					writer.write(makeAttribute(GridXMLParser.VALUE_TAG, serializer.encode((V) grid.matrix[row][col])));
					writer.write("/>");
					writer.write(NL);
				}
			}
			
			writer.write("</");
			writer.write(GridXMLParser.GRID_TAG);
			writer.write(">");
			writer.close();
		}
		
		private String makeAttribute(String qName, String value) {
			StringBuilder builder = new StringBuilder(25);
			builder.append(qName);
			builder.append("=\"");
			builder.append(value);
			builder.append("\" ");
			return builder.toString();
		}
	}
	
	public static interface ObjectSerializer<T> {

		public String encode(T object);
		
		public T decode(String data);
	}
	
	public static class DoubleSerializer implements ObjectSerializer<Double> {

		public String encode(Double object) {
			if(object == null)
				return "0";
			else
				return String.valueOf(object);
		}

		public Double decode(String data) {
			return new Double(data);
		}

	}
	
	public static class IntegerSerializer implements ObjectSerializer<Integer> {

		public String encode(Integer object) {
			if(object == null)
				return "0";
			else
				return String.valueOf(object);
		}

		public Integer decode(String data) {		
			return Integer.parseInt(data);
		}

	}

}
