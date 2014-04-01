/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,     *
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

package playground.southafrica.utilities.grid;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.matrices.Matrix;

import playground.southafrica.utilities.Header;
import playground.southafrica.utilities.gis.MyMultiFeatureReader;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Class to generate either a square or hexagonal grid from a given shapefile.
 *
 * @author jwjoubert
 */
public class GeneralGrid {
	private final static Logger LOG = Logger.getLogger(GeneralGrid.class);
	
	private Matrix grid = null;
	private QuadTree<Tuple<String, Point>> qt;
	private final GeometryFactory gf = new GeometryFactory();
	
	private final GridType type;
	private final double width;

	
	/**
	 * Instantiates a grid.
	 * @param width the horizontal distance at the widest portion of the cell.
	 * 		  For a square that is obviously the width; for a (flat-topped) 
	 * 		  hexagonal cell it is the distance from the left-most point to the
	 * 	      right-most point. 
	 * @param type an indicator specifying the type of grid. Currently supported
	 * 		  grid-type values are:
	 * 		  <ol>
	 * 			<li> square;
	 * 			<li> hexagonal.
	 * 		  </ol>
	 */
	public GeneralGrid(double width, int type) {
		this.width = width;
		switch (type) {
		case 1:
			this.type = GridType.SQUARE;
			break;
		case 2:
			this.type = GridType.HEX;
			break;
		default:
			this.type = GridType.UNKNOWN;
		};
	}
	
	/**
	 * Converts a given (single) shapefile into a grid, each cell having a 
	 * centroid within the shapefile.
	 * @param g the (preferably single) shapefile to convert into a grid. If the
	 * 		  given {@link Geometry} has multiple geometries... I don't know 
	 * 		  what happens.
	 */
	public void generateGrid(Geometry g){
		LOG.warn("Did you check that the width given is in the same unit-of-measure as the shapefile?");
		LOG.info("Generating " + this.type.toString() + " grid. This may take some time...");

		grid = new Matrix("grid", null);
		Polygon envelope = (Polygon)g.getEnvelope();
		qt = new QuadTree<Tuple<String,Point>>(
				envelope.getCoordinates()[0].x-width, 
				envelope.getCoordinates()[0].y-width, 
				envelope.getCoordinates()[2].x+width, 
				envelope.getCoordinates()[2].y+width);				

		Counter counter = new Counter("   cells # ");
		
		double startX = envelope.getCoordinates()[0].x + 0.5*width;
		double startY = envelope.getCoordinates()[0].y + 0.5*width;
		
		/* Determine the step size, given the GridType. */ 
		double yStep;
		double xStep;
		if(this.type == GridType.SQUARE){
			xStep = width;
			yStep = width;
		} else if(this.type == GridType.HEX){
			xStep = 0.75*width;
			yStep = ( Math.sqrt(3.0) / 2 ) * width;
		} else{
			throw new RuntimeException("Don't know how to generate grid for type " + GridType.UNKNOWN);
		}
		
		double y = startY;
		int row = 0;
		while(y <= envelope.getCoordinates()[2].y){
			Id fromId = new IdImpl(row);
			double x = startX;
			int col = 0;
			while(x <= envelope.getCoordinates()[2].x){
				double thisX = 0.0;
				double thisY = 0.0;
				if(this.type == GridType.SQUARE){
					thisX = x;
					thisY = y;
				} else if(this.type == GridType.HEX){
					thisX = x;
					if(col%2 == 0){
						thisY = y;
					} else{
						thisY = y-0.5*yStep;
					}
				} 
								
				Point p = gf.createPoint(new Coordinate(thisX, thisY));
				
				/* Add the point if it is within the original geometry. */
				if(g.contains(p)){
					Id toId = new IdImpl(col);
					qt.put(thisX, thisY, new Tuple<String, Point>(fromId.toString() + "_" + toId.toString(), p));
					grid.createEntry(fromId, toId, 0.0);
				}
				x += xStep;
				col++;
				counter.incCounter();
			}
			y += yStep;
			row++;
		}
		counter.printCounter();
	}
	
	
	/**
	 * Writes the grid to a file that can be visualised using R. The format of
	 * the file, irrespective of the {@link GridType}, contains the following
	 * columns:<br>
	 * <blockquote><code> From,To,Long,Lat,Width </code></blockquote>
	 * where
	 * <ul>
	 * 	<b>From</b> is the row index of the grid cell;<br>
	 * 	<b>To</b> is the column index of the grid cell;<br>
	 * 	<b>Long</b> is the longitude of the centroid of the grid cell;<br>
	 * 	<b>Lat</b> is the latitude of the grid cell; and<br>
	 * 	<b>Width</b> is the width of the grid cell.
	 * </ul>
	 * 
	 * @param folder where the output will be written. The final output filename
	 * 		  will be dependent of the {@link GridType}, followed by the 
	 * 		  <code>.csv</code> extension. 
	 */
	public void writeGrid(String folder){
		String filename = folder + (folder.endsWith("/") ? "" : "/") + this.type + ".csv";
		LOG.info("Writing grid to file: " + filename);
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		
		try{
			bw.write("From,To,Long,Lat,Width");
			bw.newLine();
			Collection<Tuple<String, Point>> list = qt.get(qt.getMinEasting(), qt.getMinNorthing(), qt.getMaxEasting(), qt.getMaxNorthing(), new ArrayList<Tuple<String,Point>>());
			for(Tuple<String,Point> tuple : list){
				String[] sa = tuple.getFirst().split("_");
				bw.write(sa[0]);
				bw.write(",");
				bw.write(sa[1]);
				bw.write(",");
				Coordinate c = tuple.getSecond().getCoordinate();
				bw.write(String.format("%.4f,%.4f,%.4f\n", c.x, c.y, width));
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + filename);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + filename);
			}
		}
		LOG.info("Done writing file.");
	}
	
	
	/**
	 * @return the {@link GridType} of the grid.
	 */
	public GridType getGridType(){
		return this.type;
	}
	
	public QuadTree<Tuple<String,Point>> getGrid(){
		return this.qt;
	}
	
	
	public static void main(String[] args){
		Header.printHeader(GeneralGrid.class.toString(), args);
		
		String shapefile = args[0];
		String outputFolder = args[1];
		int type = Integer.parseInt(args[2]);
		Double width = Double.parseDouble(args[3]);
		
		MyMultiFeatureReader mmfr = new MyMultiFeatureReader();
		try {
			mmfr.readMultizoneShapefile(shapefile, 1);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read shapefile from " + shapefile);
		}
		
		LOG.info("Read " + mmfr.getAllZones().size() + " zone(s).");
		GeneralGrid grid = new GeneralGrid(width, type);

		Geometry zone = mmfr.getAllZones().get(0);
		Geometry dummy = grid.buildDummyPolygon();
		
		grid.generateGrid(dummy);
		grid.writeGrid(outputFolder);
		
		Header.printFooter();
	}
	
	
	/**
	 * Currently supported grid types are:
	 * <ul>
	 * 	<b>SQUARE</b> where each cell is a square;<br>
	 * 	<b>HEX</b> where each cell is a hexagon, resulting in a honeycomb grid;
	 * 	<b>UNKNOWN</b> if no (proper) grid type is passed. This will ultimately 
	 * 	lead to a {@link RuntimeException} being thrown.
	 *
	 * @author jwjoubert
	 */
	public static enum GridType{
		SQUARE, 
		HEX,
		UNKNOWN;		
	}
	
	/**
	 * Builds a geometry that is 20 x 20 grids cells in size.
	 * @return a geometry of type {@link Polygon} 
	 */
	private Polygon buildDummyPolygon(){
		Coordinate c1 = new Coordinate(0.0, 0.0);
		Coordinate c2 = new Coordinate(0.0, 10*width);
		Coordinate c3 = new Coordinate(10*width, 10*width);
		Coordinate c4 = new Coordinate(10*width, 0.0);
		Coordinate[] ca = {c1,c2,c3,c4,c1};
		return gf.createPolygon(ca);
	}
	
	

}

