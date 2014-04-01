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

package playground.jjoubert.Utilities.KernelDensityEstimation;

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
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class GeneralGrid {
	private final static Logger LOG = Logger.getLogger(GeneralGrid.class);
	
	private Matrix grid = null;
	private QuadTree<Tuple<String, Point>> qt;
	private final GeometryFactory gf = new GeometryFactory();
	
	private final GridType type;
	private final double width;

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
	
	private void generateGrid(Geometry g){
		LOG.info("Generating " + this.type.toString() + " grid. This may take some time...");

		grid = new Matrix("grid", null);
		Polygon envelope = (Polygon)g.getEnvelope();
		qt = new QuadTree<Tuple<String,Point>>(
				envelope.getCoordinates()[0].x-width, 
				envelope.getCoordinates()[0].y-width, 
				envelope.getCoordinates()[2].x+width, 
				envelope.getCoordinates()[2].y+width);				

		double xDim = qt.getMaxEasting() - qt.getMinEasting();
		double yDim = qt.getMaxNorthing() - qt.getMinNorthing();
		
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
	
	private void writeGrid(String folder){
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
		Geometry zone = mmfr.getAllZones().get(0);
		
		GeneralGrid grid = new GeneralGrid(width, type);
//		grid.generateGrid(grid.buildDummyPolygon());
		grid.generateGrid(zone);
		grid.writeGrid(outputFolder);
		
		Header.printFooter();
	}
	
	private enum GridType{
		SQUARE("Square"), 
		HEX("Hex"),
		UNKNOWN("Unknown");
		
		private final String type;
		
		private GridType(final String type){this.type = type;}
	}
	
	private Polygon buildDummyPolygon(){
		Coordinate c1 = new Coordinate(0.0, 0.0);
		Coordinate c2 = new Coordinate(0.0, 20*width);
		Coordinate c3 = new Coordinate(20*width, 20*width);
		Coordinate c4 = new Coordinate(20*width, 0.0);
		Coordinate[] ca = {c1,c2,c3,c4,c1};
		return gf.createPolygon(ca);
	}

}

