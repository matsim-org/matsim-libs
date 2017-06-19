/* *********************************************************************** *
 * project: org.matsim.*
 * TripDensityGenerator.java
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

/**
 * 
 */
package playground.jjoubert.projects.wb.freight;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import playground.southafrica.utilities.Header;
import playground.southafrica.utilities.grid.GeneralGrid;
import playground.southafrica.utilities.grid.GeneralGrid.GridType;
import playground.southafrica.utilities.grid.KernelDensityEstimator;
import playground.southafrica.utilities.grid.KernelDensityEstimator.KdeType;

/**
 * Class to aggregate the commercial vehicle activities to the existing grid 
 * that was generated on National level.
 *   
 * @author jwjoubert
 */
public class ActivityDensityGeneratorOnStandardGrid {
	final private static Logger LOG = Logger.getLogger(ActivityDensityGeneratorOnStandardGrid.class);
	private final GeneralGrid grid;
	private KernelDensityEstimator kde;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(ActivityDensityGeneratorOnStandardGrid.class.toString(), args);
		
		String gridfile = args[0];
		/* The national grid file SHOULD be in the Hartebeesthoek Lo29 NE 
		 * projected coordinate reference system. */
		String crs = TransformationFactory.HARTEBEESTHOEK94_LO29;
		String facilitiesFile = args[1];
		double width = Double.parseDouble(args[2]);
		GridType gridType = GridType.valueOf(args[3]);
		String outputFolder = args[4];
		outputFolder += outputFolder.endsWith("/") ? "" : "/";
		
		String description = String.format("%s_%04.0f", gridType.toString(), width);
		
		ActivityDensityGeneratorOnStandardGrid tdg = new ActivityDensityGeneratorOnStandardGrid(gridfile, width, gridType);
		tdg.processFacilities(facilitiesFile);
		tdg.writeDensityForR(outputFolder, description, crs);
		
		Header.printFooter();
	}
	
	public ActivityDensityGeneratorOnStandardGrid(String geometry, 
			double width, GridType gridType) {
		LOG.info("Initialising the class...");
		
		LOG.info("Building grid from file...");
		grid = GeneralGrid.readGrid(geometry, width, gridType);
		this.kde = new KernelDensityEstimator(grid, KdeType.CELL, 1.0);

		LOG.info("Done initializing the class.");
	}
	
	public void processFacilities(String facilitiesFile){
		LOG.info("Parsing facilities file...");
		GeometryFactory gf = new GeometryFactory();
		BufferedReader br = IOUtils.getBufferedReader(facilitiesFile);
		try{
			String line = br.readLine();
			while((line=br.readLine()) != null){
				String[] sa = line.split(",");
				double x = Double.parseDouble(sa[1]);
				double y = Double.parseDouble(sa[2]);
				double weight = Double.parseDouble(sa[3]);
				Point p = gf.createPoint(new Coordinate(x, y));
				Point closest = grid.getGrid().getClosest(x, y);
				Geometry cell = grid.getCellGeometry(closest);
				if(cell.covers(p)){
					kde.processPoint(closest, weight);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read from " + facilitiesFile);
		} finally{
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + facilitiesFile);
			}
		}
		LOG.info("Done parsing facilities file...");
	}
	
	private void writeSingleDensityForR(KernelDensityEstimator kde, 
			String outputFile, CoordinateTransformation ct){
		
		BufferedWriter bw = IOUtils.getBufferedWriter(outputFile);
		int index = 1;
		try{
			bw.write("id,point,centroidLon,centroidLat,lon,lat,weight");
			bw.newLine();
			
			Iterator<Point> iterator = kde.getGrid().getGrid().values().iterator();
			while(iterator.hasNext()){
				Point p = iterator.next();
				Coord centroid = ct.transform(CoordUtils.createCoord(p.getX(), p.getY()));
				
				Coordinate[] ca = kde.getGrid().getCellGeometry(p).getCoordinates();
				double weight = kde.getWeight(p);
				/* Only print those cells to file that have a weight. */
				if(weight > 0){
					for(int i = 0; i < ca.length; i++){
						Coordinate c = ca[i];
						Coord cWgs = ct.transform(CoordUtils.createCoord(c.x, c.y));
						bw.write(String.format("%d,%d,%.6f,%.6f,%.6f,%.6f,%.4f\n",
								index, i, 
								centroid.getX(), centroid.getY(),
								cWgs.getX(), cWgs.getY(),
								weight));
					}
					index++;					
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + bw.toString());
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + bw.toString());
			}
		}
	}
	
	public void writeDensityForR(String outputFolder, String description, String crs){
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(
				crs, TransformationFactory.WGS84);
		writeSingleDensityForR(kde, outputFolder + description + "_facilityActivities.csv.gz", ct);
	}

}
