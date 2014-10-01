/* *********************************************************************** *
 * project: org.matsim.*
 * DigicoreActivityDensityEstimator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.southafrica.freight.digicore.algorithms.kernelDensity;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.freight.digicore.containers.DigicoreVehicle;
import playground.southafrica.freight.digicore.io.DigicoreVehicleReader_v1;
import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;
import playground.southafrica.utilities.containers.MyZone;
import playground.southafrica.utilities.gis.MyMultiFeatureReader;

import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;


public class DigicoreActivityDensityEstimator {
	private final static Logger LOG = Logger.getLogger(DigicoreActivityDensityEstimator.class);
	private final MultiPolygon area;
	private Raster raster;
	private static double stride;
	private static double radius;
	private static int kdeType; 
	private static Color color;
	private final File outputFolder;

	/**
	 * @param args the following arguments are required, and in the following order:
	 * <ol>
	 * 	<li> number of threads;
	 * 	<li> absolute path of the file containing the vehicle {@link Id}s;
	 * 	<li> absolute path of the folder containing the xml vehicle files;
	 * 	<li> absolute path of the shapefile containing the study area;
	 * 	<li> parameters for the kernel density estimation:
	 * 	<ol>
	 * 		<li> the stride, i.e. the resolution;
	 * 		<li> radius of the impact area; and
	 * 		<li> the type of estimate. See {@link Raster}
	 * 	<ol>
	 * </ol>
	 */
	public static void main(String[] args) {
		Header.printHeader(DigicoreActivityDensityEstimator.class.toString(), args);

		String inputFolder = args[2];
		
		/* Read the vehicle list. */
		List<File> files = null;
		if(args[1].equalsIgnoreCase("null")){
			files = FileUtils.sampleFiles(new File(inputFolder), Integer.MAX_VALUE, FileUtils.getFileFilter(".xml.gz"));
		} else{
			//FIXME: I just commented this out to get playground to compile.
//			MyVehicleIdentifier mvi = new MyVehicleIdentifier();
//			try {
//				files = mvi.readListFromFile(args[1], args[2]);
//			} catch (IOException e) {
//				throw new RuntimeException("Could not read vehicle file list.");
//			}
		}
		
		/* Read the study area. */
		MyMultiFeatureReader mmfr = new MyMultiFeatureReader();
		try {
			mmfr.readMultizoneShapefile(args[3], 1);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} //FIXME I hard coded the id field.
		List<MyZone> list = mmfr.getAllZones(); 
		MultiPolygon mp = list.get(0); //FIXME Hard coded single polygon.
		
		DigicoreActivityDensityEstimator dade = new DigicoreActivityDensityEstimator(mp);
		
		/* Kernel density estimate parameters. */
		stride = Double.parseDouble(args[4]);
		radius = Double.parseDouble(args[5]);
		kdeType = Integer.parseInt(args[6]);
		color = new Color(0, 0, 0); /* Black. */
		try {
			dade.assembleRaster(dade.parseVehicleFiles(files, Integer.parseInt(args[0])), stride, radius, kdeType, color);
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		dade.raster.writeRasterForR(args[7], mp);
		
		Header.printFooter();
	}
	
	
	/**
	 * Constructs an instance.
	 * @param studyarea only points inside the study area will be used for 
	 * 		the kernel density estimate.
	 */
	public DigicoreActivityDensityEstimator(MultiPolygon studyarea) {
		this.area = studyarea;
		
		this.outputFolder = new File("tmp/");
		this.outputFolder.deleteOnExit();
		if(!this.outputFolder.mkdirs()){
			throw new RuntimeException("Could not create temporary folder " + this.outputFolder.getAbsolutePath());
		}
	}
	
	
	/**
	 * Processes a list of vehicles over one or more threads. The vehicle files 
	 * are assigned in a multi-threaded way.
	 * @param files a List of {@link File}s, each representing a {@link DigicoreVehicle}.
	 * @param threads the number of threads to use for processing vehicle chains.
	 * @return a list of {@link Point}s, each representing an activity inside the study
	 * 		area.
	 */
	public List<String> parseVehicleFiles(List<File> files, int threads){
		LOG.info("Parsing activity coordinates for " + files.size() + " vehicle files.");
		final Counter counter = new Counter("  #: ");
		
		ExecutorService threadExecutor = Executors.newFixedThreadPool(threads);
		List<Future<String>> listOfJobs = new ArrayList<Future<String>>();
		for(File file : files){
			DigicoreVehicleReader_v1 dvr = new DigicoreVehicleReader_v1();
			try {
				dvr.parse(file.getAbsolutePath());
				DigicoreVehicle vehicle = dvr.getVehicle(); 
				Callable<String> job = new DigicoreActivityDensityCallable(area, vehicle, counter, stride, radius, kdeType, color, this.outputFolder.getAbsolutePath());
				Future<String> submit = threadExecutor.submit(job);
				listOfJobs.add(submit);
			} catch (IOException e) {
				LOG.error("Could not read " + file.getAbsolutePath() + " -- file ignored");
			}
		}
		threadExecutor.shutdown();
		while(!threadExecutor.isTerminated()){
		}
		counter.printCounter();
		
		LOG.info("Done parsing. Collecting all activity points.");
		List<String> list = new ArrayList<String>();
		for(Future<String> future : listOfJobs){
			try {
				list.add(future.get());
			} catch (InterruptedException e) {
				throw new RuntimeException("InterruptedException !!"); 
			} catch (ExecutionException e) {
				throw new RuntimeException("ExecutionException !!"); 
			}
		}	
		
		return list;
	}
	
	
	/**
	 * Creates and populates a {@link Raster} with the activities inside the 
	 * study area.
	 * @param list of rasters created from individual vehicle files.
	 * @param stride the size, width or breadth, of each raster pixel.
	 * @param radius the kernel density impact area each point will have.
	 * @param kdeType the type of estimate required. See {@link Raster#processPoint(Point)}.
	 * @param color the base color of the raster. 
	 * @throws IOException 
	 * @throws NumberFormatException 
	 */
	public void assembleRaster(List<String> list, double stride, double radius, int kdeType, Color color) throws NumberFormatException, IOException{
		LOG.info("Assembling the final raster from " + list.size() + " seperate instances");
		Counter counter = new Counter("  # completed: ");
		Polygon polygon = (Polygon) this.area.getEnvelope();
		this.raster = new Raster(polygon, stride, radius, kdeType, color);
		
		BufferedReader br = null;
		for(String s : list){
			br = IOUtils.getBufferedReader(s);
			try{
				String line = null;
				while((line = br.readLine()) != null){
					String[] ls = line.split(",");
					int row = Integer.parseInt(ls[0]);
					int col = Integer.parseInt(ls[1]);
					double value = Double.parseDouble(ls[2]);
					raster.increaseImageMatrixValue(row, col, value);
				}
				counter.incCounter();				
			} finally{
				br.close();
			}
		}		
		counter.printCounter();
	}	

}

