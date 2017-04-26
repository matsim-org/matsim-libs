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

package playground.agarwalamit.analysis.spatial;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Class to generate either a square or hexagonal grid from a given shapefile, 
 * and apply (optional) smoothing when processing points or lines over the
 * grid surface. 
 *
 * @author jwjoubert
 */
public class GeneralGrid{
	
	private final static Logger LOG = Logger.getLogger(GeneralGrid.class);

	private Geometry geometry;
	private QuadTree<Point> qt;
	private final GeometryFactory gf = new GeometryFactory();

	private final GridType type;
	private final double width;

	/* For caching purposes, create the grid cell geometry once, and return the 
	 * cached geometry subsequently. */
	private final Map<Point, Geometry> geometryCache = new HashMap<>();


	/**
	 * Instantiates a grid.
	 * @param width the horizontal distance at the widest portion of the cell.
	 * 		  For a square that is obviously the width; for a (flat-topped) 
	 * 		  hexagonal cell it is the distance from the left-most point to the
	 * 	      right-most point. 
	 * @param type an indicator specifying the {@link GridType}. 
	 */
	public GeneralGrid(final double width, final GridType type) {
		this.width = width;
		this.type = type;
	}

	/**
	 * Converts a given (single) shapefile into a grid, each cell having a 
	 * centroid within the shapefile.
	 * @param g the (preferably single) shapefile to convert into a grid. If the
	 * 		  given {@link Geometry} has multiple geometries... I don't know 
	 * 		  what happens.
	 */
	public void generateGrid(final Geometry g){
		LOG.warn("Did you check that the width given is in the same unit-of-measure as the shapefile?");
		LOG.info("Generating " + this.type.toString() + " grid. This may take some time...");
		this.geometry = g;

//		grid = new Matrix("grid", null);
		Polygon envelope = (Polygon)g.getEnvelope();
		qt = new QuadTree<>(
				envelope.getCoordinates()[0].x - width,
				envelope.getCoordinates()[0].y - width,
				envelope.getCoordinates()[2].x + width,
				envelope.getCoordinates()[2].y + width);

		Counter counter = new Counter("   cells # ");

		double startX = envelope.getCoordinates()[0].x; // + 0.5*width;
		double startY = envelope.getCoordinates()[0].y; // + 0.5*width;

		/* Determine the step size, given the GridType. 
		 * TODO: Update this for every new grid type. */ 
		double yStep;
		double xStep;
		switch (this.type) {
		case SQUARE:
			xStep = width;
			yStep = width;
			break;
		case HEX:
			xStep = 0.75*width;
			yStep = ( Math.sqrt(3.0) / 2 ) * width;
			break;
		case UNKNOWN:
			throw new RuntimeException("Don't know how to generate grid for type " + GridType.UNKNOWN);
		default:
			throw new RuntimeException("Don't know how to generate grid for type " + GridType.UNKNOWN);
		}

		double y = startY;
		int row = 0;
		while(y <= envelope.getCoordinates()[2].y){
//			Id fromId = new IdImpl(row);
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
				Geometry cell = this.getIndividualGeometry(p);
				
				/* This is the new implementation. The original implementation
				 * only checked if the centroid was within the study area. */
				if(g.intersects(cell)){

				/*FIXME Remove after reproducing maps for Joubert & Meintjes paper. */
//				if(g.contains(p)){
					qt.put(thisX, thisY, p);
					geometryCache.put(p, cell);					
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
	
	public Geometry getCellGeometry(final Point p){
		return this.geometryCache.get(p);
	}


	/**
	 * Writes the grid to a file that can be visualised using R, for example. 
	 * The format of the file, irrespective of the {@link GridType}, contains 
	 * the following columns:<br>
	 * <blockquote><code> From,To,Long,Lat,X,Y,Width </code></blockquote>
	 * where
	 * <ul>
	 * 	<b>From</b> is the row index of the grid cell;<br>
	 * 	<b>To</b> is the column index of the grid cell;<br>
	 * 	<b>Long</b> is the longitude of the centroid of the grid cell's centroid.
	 *     If the original geometry's coordinate reference system (CRS) is 
	 *     provided, then the output longitude will be in decimal degrees 
	 *     following the WGS84 coordinate reference system). If not, the 
	 *     x-value of the centroid will be in the same CRS as the original
	 *     geometry;<br>
	 * 	<b>Lat</b> is, similar to the longitude, the latitude of the grid cell's 
	 *     centroid (in decimal degrees following the WGS84 CRS if the original 
	 *     CRS is provided);<br>
	 *  <b>X</b> is the x-value of the grid cell's centroid in the original CRS;
	 *  <b>Y</b> is the y-value of the grid cell's centroid in the original CRS; and
	 * 	<b>Width</b> is the width of the grid cell.
	 * </ul>
	 * 
	 * @param folder where the output will be written. The final output filename
	 * 		  will be dependent of the {@link GridType}, the grid's width, 
	 * 		  followed by the <code>.csv</code> extension. 
	 * @param originalCRS a string, allowably <code>null</code> describing the 
	 * 		  coordinate reference system (CRS) of the original geometry 
	 * 		  provided. If <code>null</code> then no transformation will be done 
	 * 		  on the coordinate points of the centroids.  
	 */
	public void writeGrid(final String folder, final String originalCRS){
		//ZZ_TODO : more arguments are passed than than actually needed (wants 4 but 5 are passed)
		String filename = String.format("%s%s%s_%.0f.csv", folder, (folder.endsWith("/") ? "" : "/"), this.type, this.width, ".csv");
		LOG.info("Writing grid to file: " + filename);

		CoordinateTransformation ct = null;
		if(originalCRS != null){
			ct = TransformationFactory.getCoordinateTransformation(originalCRS, "WGS84");
		}

		BufferedWriter bw = IOUtils.getBufferedWriter(filename);

		double sum = 0.0;
		int count = 0;

		try{
			bw.write("Long,Lat,X,Y,Width");
			bw.newLine();
			Collection<Point> list = qt.getRectangle(qt.getMinEasting(), qt.getMinNorthing(), qt.getMaxEasting(), qt.getMaxNorthing(), new ArrayList<>());
			for(Point p : list){
				Coord original = new Coord(new Double(p.getX()), new Double(p.getY()));
				Coord wgs84 = null;
				if(ct != null){
					wgs84 = ct.transform(original);
				} else{
					wgs84 = original;
				}

				bw.write(String.format("%.6f,%.6f,%.4f,%.4f,%.4f\n", wgs84.getX(), wgs84.getY(), p.getX(), p.getY(), width));

				count++;
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
		LOG.info(String.format("Avg length (in decimal degrees) of 1000m: %.8f (%d observations)", sum / (count), count));
	}


	/**
	 * @return the {@link GridType} of the grid.
	 */
	public GridType getGridType(){
		return this.type;
	}

	public QuadTree<Point> getGrid(){
		return this.qt;
	}


//	public static void main(String[] args){
//		Header.printHeader(GeneralGrid.class.toString(), args);
//
//		String shapefile = args[0];
//		String outputFolder = args[1];
//		String type = args[2];
//		Double width = Double.parseDouble(args[3]);
//
//		MyMultiFeatureReader mmfr = new MyMultiFeatureReader();
//		try {
//			mmfr.readMultizoneShapefile(shapefile, 1);
//		} catch (IOException e) {
//			e.printStackTrace();
//			throw new RuntimeException("Cannot read shapefile from " + shapefile);
//		}
//
//		LOG.info("Read " + mmfr.getAllZones().size() + " zone(s).");
//		GeneralGrid grid = new GeneralGrid(width, GridType.valueOf(type));
//
//		Geometry zone = mmfr.getAllZones().get(0);
//		Geometry dummy = grid.buildDummyPolygon();
//
//		grid.generateGrid(zone);
//		grid.writeGrid(outputFolder, "WGS84_SA_Albers");
//
//		Header.printFooter();
//	}



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
	public enum GridType{
		SQUARE, 
		HEX,
		UNKNOWN
    }



	/**
	 * Creates a geometry, typical polygon, around the centroid of a grid cell.
	 * The class uses cache, so it first checks if the geometry has already been
	 * created.
	 * 
	 * @param centroid
	 * @return
	 */
	private Geometry getIndividualGeometry(final Point centroid){
		Geometry g = null;

		switch (this.type) {
		case SQUARE:
			double l = centroid.getX() - 0.5*this.width;
			double r = centroid.getX() + 0.5*this.width;
			double b = centroid.getY() - 0.5*this.width;
			double t = centroid.getY() + 0.5*this.width;
			Coordinate c1 = new Coordinate(l, b);
			Coordinate c2 = new Coordinate(l, t);
			Coordinate c3 = new Coordinate(r, t);
			Coordinate c4 = new Coordinate(r, b);
			Coordinate[] ca = {c1,c2,c3,c4,c1};
			g = gf.createPolygon(ca);
			break;
			
		case HEX:
			double w = 0.5*this.width;
			double h = Math.sqrt(3.0)/2.0 * w;
			double x = centroid.getX();
			double y = centroid.getY();
			Coordinate cc1 = new Coordinate(x-w, y);
			Coordinate cc2 = new Coordinate(x-0.5*w, y+h);
			Coordinate cc3 = new Coordinate(x+0.5*w, y+h);
			Coordinate cc4 = new Coordinate(x+w, y);
			Coordinate cc5 = new Coordinate(x+0.5*w, y-h);
			Coordinate cc6 = new Coordinate(x-0.5*w, y-h);
			Coordinate[] cca = {cc1, cc2, cc3, cc4, cc5, cc6, cc1};
			g = gf.createPolygon(cca);
			break;
			
		default:
			break;
		}
		return g;
	}
	
	public double getCellWidth(){
		return this.width;
	}
	
	public Geometry getGeometry(){
		return this.geometry;
	}
}

