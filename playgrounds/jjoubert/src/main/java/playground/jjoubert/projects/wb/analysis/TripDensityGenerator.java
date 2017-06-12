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
package playground.jjoubert.projects.wb.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

import playground.jjoubert.projects.wb.tiff.GeoTiffReader;
import playground.jjoubert.projects.wb.tiff.GtiLandCover;
import playground.southafrica.utilities.Header;
import playground.southafrica.utilities.grid.GeneralGrid;
import playground.southafrica.utilities.grid.GeneralGrid.GridType;
import playground.southafrica.utilities.grid.KernelDensityEstimator;
import playground.southafrica.utilities.grid.KernelDensityEstimator.KdeType;

/**
 * Class to convert a GeoTiff image to a trip density map.
 *   
 * @author jwjoubert
 */
public class TripDensityGenerator {
	final private static Logger LOG = Logger.getLogger(TripDensityGenerator.class);
	private GeoTiffReader reader;
	private KernelDensityEstimator kde_am_in;
	private KernelDensityEstimator kde_am_out;
	private KernelDensityEstimator kde_pm_in;
	private KernelDensityEstimator kde_pm_out;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(TripDensityGenerator.class.toString(), args);
		
		String shapefile = args[0];
		String crs = args[1];
		String geotiff = args[2];
		double width = Double.parseDouble(args[3]);
		GridType gridType = GridType.valueOf(args[4]);
		String outputFolder = args[5];
		outputFolder += outputFolder.endsWith("/") ? "" : "/";
		String area = args[6];
		
		String description = String.format("%s_%s_%04.0f", area, gridType.toString(), width);
		
		TripDensityGenerator tdg = new TripDensityGenerator(shapefile, geotiff, width, gridType);
		tdg.generateTrips(crs);
		tdg.writeDensityForR(outputFolder, description, crs);
		
		Header.printFooter();
	}
	
	public TripDensityGenerator(String geometry, String geotiff, 
			double width, GridType gridType) {
		LOG.info("Initialising the class...");
		
		LOG.info("Building kernel density grid from shapefile...");
		/* Parse the area shapefile. */
		ShapeFileReader sfr = new ShapeFileReader();
		sfr.readFileAndInitialize(geometry);
		Collection<SimpleFeature> features = sfr.getFeatureSet();
		if(features.size() > 1){
			LOG.warn("Multiple features in shapefile... only the first to be used!");
		}
		SimpleFeature firstFeature = features.iterator().next();
		Geometry area = null;
		if(firstFeature.getDefaultGeometry() instanceof MultiPolygon){
			area = (Geometry) firstFeature.getDefaultGeometry();
		}
		
		GeneralGrid grid = new GeneralGrid(width, gridType);
		grid.generateGrid(area);
		
		LOG.info("Parsing GeoTIFF image...");
		this.reader = new GeoTiffReader();
		reader.read(geotiff);
		reader.getNumberOfPixels();

		this.kde_am_in = new KernelDensityEstimator(grid, KdeType.CELL, 10.0);
		this.kde_am_out = new KernelDensityEstimator(grid, KdeType.CELL, 10.0);
		this.kde_pm_in = new KernelDensityEstimator(grid, KdeType.CELL, 10.0);
		this.kde_pm_out = new KernelDensityEstimator(grid, KdeType.CELL, 10.0);
		
		LOG.info("Done initializing the class.");
	}
	
	public void generateTrips(String crs){
		
		int cols = this.reader.getWidth();
		int rows = this.reader.getHeight();
		int pixels = rows*cols;
		
		LOG.info("Processing each pixel... (Total pixels: " + pixels + ")");
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(
				TransformationFactory.WGS84, crs);
		GeometryFactory gf = new GeometryFactory();
		
		Counter counter = new Counter("  pixel # ");
		
		for(int x = 0; x < cols; x++){
			for(int y = 0; y < rows; y++){
				/* Calculate the area of the pixel. */
				double area = this.reader.calculatePixelAreaSqm(x, y, ct);
				
				/* Get the number of trips for the pixel. This is a function of
				 * the pixel's land use value and the pixel's area. */
				GtiLandCover cover = this.reader.getLandCover(x, y);
				if(cover != null){
					/* Assign the trips to the grid's cells. */
					Coord centre = this.reader.getPixelCentre(x, y);
					Coord centreT = ct.transform(centre);
					Point centrePoint = gf.createPoint(new Coordinate(centreT.getX(), centreT.getY()));
					
					Point p = this.kde_am_in.getGrid().getGrid().getClosest(centreT.getX(), centreT.getY());
					
					/* Check that the closest cell in the grid actually covers
					 * the point representing the pixel. */
					Geometry cell = this.kde_am_in.getGrid().getCellGeometry(p);
					if(cell.covers(centrePoint)){
						kde_am_in.processPoint(p, cover.getAADT(area, true, true), true); 		/* AM peak in */
						kde_am_out.processPoint(p, cover.getAADT(area, true, false), true);		/* AM peak out */
						kde_pm_in.processPoint(p, cover.getAADT(area, false, true), true);		/* PM peak in */
						kde_pm_out.processPoint(p, cover.getAADT(area, false, false), true);	/* PM peak out */
					}
				}
				counter.incCounter();
			}
		}
		counter.printCounter();
		
		LOG.info("Done processing pixels.");
	}
	
	
	private void writeSingleDensityForR(KernelDensityEstimator kde, 
			String outputFile, CoordinateTransformation ct){
		
		BufferedWriter bw = IOUtils.getBufferedWriter(outputFile);
		int index = 1;
		try{
			bw.write("id,point,lon,lat,trips");
			bw.newLine();
			
			Iterator<Point> iterator = kde.getGrid().getGrid().values().iterator();
			while(iterator.hasNext()){
				Point p = iterator.next();
				Coordinate[] ca = kde.getGrid().getCellGeometry(p).getCoordinates();
				double weight = kde.getWeight(p);
				/* Only print those cells to file that have a weight. */
				if(weight > 0){
					for(int i = 0; i < ca.length; i++){
						Coordinate c = ca[i];
						Coord cWgs = ct.transform(CoordUtils.createCoord(c.x, c.y));
						bw.write(String.format("%d,%d,%.6f,%.6f,%.4f\n",
								index, i, 
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
		writeSingleDensityForR(kde_am_in, outputFolder + description + "_AM_IN.csv.gz", ct);
		writeSingleDensityForR(kde_am_out, outputFolder + description + "_AM_OUT.csv.gz", ct);
		writeSingleDensityForR(kde_pm_in, outputFolder + description + "_PM_IN.csv.gz", ct);
		writeSingleDensityForR(kde_pm_out, outputFolder + description + "_PM_OUT.csv.gz", ct);
	}

}
