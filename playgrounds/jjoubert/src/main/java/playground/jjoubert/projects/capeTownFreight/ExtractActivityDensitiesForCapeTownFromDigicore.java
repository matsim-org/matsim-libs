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

package playground.jjoubert.projects.capeTownFreight;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import playground.jjoubert.Utilities.MyShapefileReader;
import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicoreChain;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;
import playground.southafrica.freight.digicore.io.DigicoreVehicleReader_v1;
import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;
import playground.southafrica.utilities.grid.GeneralGrid;
import playground.southafrica.utilities.grid.GeneralGrid.GridType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class ExtractActivityDensitiesForCapeTownFromDigicore {
	private final static Logger LOG = Logger.getLogger(ExtractActivityDensitiesForCapeTownFromDigicore.class);

	public static void main(String[] args) {
		Header.printHeader(ExtractActivityDensitiesForCapeTownFromDigicore.class.toString(), args);
		
		String vehicleFolder = args[0];
		String shapefile = args[1];	
		String outputFolder = args[2];
		int numberOfThreads = Integer.parseInt(args[3]);
		double zoneWidth = Double.parseDouble(args[4]);
		
		/* Read list of vehicle files. */
		List<File> vehicleFiles = FileUtils.sampleFiles(new File(vehicleFolder), Integer.MAX_VALUE, FileUtils.getFileFilter(".xml.gz"));
		
		/* Read shapefile. */
		MyShapefileReader msr = new MyShapefileReader(shapefile);
		MultiPolygon area = msr.readMultiPolygon();
		
		/* Convert shapefile to hexagonal grid shapes. */
		GeneralGrid gg = new GeneralGrid(zoneWidth, GridType.HEX);
		gg.generateGrid(area);
		gg.writeGrid(outputFolder, "WGS84_SA_Albers");
		QuadTree<Point> grid = gg.getGrid();
		
		/* Perform the analysis */
		Counter counter = new Counter("   vehicles # ");
		
		/* Set up the multi-threaded infrastructure. */
		LOG.info("Setting up multi-threaded infrastructure for analyses");
		ExecutorService threadExecutor = Executors.newFixedThreadPool(numberOfThreads);
		List<Future<Map<String, Integer>>> jobs = new ArrayList<Future<Map<String,Integer>>>();
		
		/*FIXME Must redo this with the new GeneralGrid code... */
//		LOG.info("Processing vehicle files...");
//		for(File f : vehicleFiles){
//			Callable<Map<String, Integer>> job = new VehicleActivityDensityAnalyser(grid, f, counter, zoneWidth);
//			Future<Map<String, Integer>> result = threadExecutor.submit(job);
//			jobs.add(result);
//		}
//		counter.printCounter();
//		LOG.info("Done processing vehicle files...");
//		
//		threadExecutor.shutdown();
//		while(!threadExecutor.isTerminated()){
//		}
//
//		/* Consolidate the output */
//		LOG.info("Consolidating output...");
//		Map<String, Integer> zoneCounts = new HashMap<String, Integer>();
//		for(Future<Map<String, Integer>> future : jobs){
//			/* Get the job's results. */
//			Map<String, Integer> map = null;
//			try {
//				map = future.get();
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			} catch (ExecutionException e) {
//				e.printStackTrace();
//			}
//			
//			for(String zone : map.keySet()){
//				if(!zoneCounts.containsKey(zone)){
//					zoneCounts.put(zone, map.get(zone));
//				} else{
//					int oldValue = zoneCounts.get(zone);
//					zoneCounts.put(zone, oldValue + map.get(zone));
//				}
//			}
//		}
//		LOG.info("Done consolidating output...");
//
//		writeCountsToFile(outputFolder + "zoneCounts.csv", zoneCounts);
		
		Header.printFooter();
	}
	
	
	private static void writeCountsToFile(String filename, Map<String, Integer> map){
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		try{
			bw.write("From,To,Count");
			bw.newLine();
			
			for(String s : map.keySet()){
				String[] sa = s.split("_");
				bw.write(String.format("%s,%s,%d\n", sa[0], sa[1], map.get(s) ));
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + filename);
		}finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + filename);
			}
		}
	}
	
	
	private static class VehicleActivityDensityAnalyser implements Callable<Map<String,Integer>>{
		private final QuadTree<Tuple<String, Point>> qt;
		private Map<String, Integer> map;
		private File file;
		private final Counter counter;
		private final double zoneWidth;
		
		public VehicleActivityDensityAnalyser(final QuadTree<Tuple<String,Point>> qt, File file, final Counter counter, final double width) {
			this.qt = qt;
			this.map = new HashMap<String, Integer>();
			this.file = file;
			this.counter = counter;
			this.zoneWidth = width;
		}

		@Override
		public Map<String, Integer> call() throws Exception {
			Map<String, Polygon> hexPolygons = new HashMap<String, Polygon>();
			
			GeometryFactory gf = new GeometryFactory();
			
			/* Read the vehicle file. */
			DigicoreVehicleReader_v1 dvr = new DigicoreVehicleReader_v1();
			dvr.parse(this.file.getAbsolutePath());
			DigicoreVehicle vehicle = dvr.getVehicle();

			/* Process the activities. */
			for(DigicoreChain chain : vehicle.getChains()){
				for(DigicoreActivity act : chain.getAllActivities()){
					Coord coord = act.getCoord();
					if(checkInQt(this.qt, coord)){
						Point point = gf.createPoint(new Coordinate(coord.getX(), coord.getY()));
						
						Tuple<String, Point> tuple = this.qt.get(coord.getX(), coord.getY());
						String zone = tuple.getFirst();
						
						if(!hexPolygons.containsKey(zone)){
							/* Create a hexagonal polygon. */
							double w = 0.5*zoneWidth;
							double h = Math.sqrt(3.0)/2 * w;
							double x = tuple.getSecond().getX();
							double y = tuple.getSecond().getY();
							Coordinate c1 = new Coordinate(x-w, y);
							Coordinate c2 = new Coordinate(x-0.5*w, y+h);
							Coordinate c3 = new Coordinate(x+0.5*w, y+h);
							Coordinate c4 = new Coordinate(x+w, y);
							Coordinate c5 = new Coordinate(x+0.5*w, y-h);
							Coordinate c6 = new Coordinate(x-0.5*w, y-h);
							Coordinate[] ca = {c1, c2, c3, c4, c5, c6, c1};
							
							Polygon hex = gf.createPolygon(ca);
							hexPolygons.put(zone, hex);
						}
						Polygon zonePoly = hexPolygons.get(zone);
						
						if(zonePoly.contains(point)){
							if(!map.containsKey(tuple.getFirst())){
								map.put(zone, 1);
							} else{
								int oldValue = map.get(zone);
								map.put(zone, oldValue+1);
							}
						}
					}
				}
			}
			counter.incCounter();
			return this.map;
		}
		
		private boolean checkInQt(QuadTree<Tuple<String, Point>> qt, Coord coord){
			boolean result = false;
			double x = coord.getX();
			double y = coord.getY();
			if(x >= qt.getMinEasting() &&
					x <= qt.getMaxEasting() &&
					y >= qt.getMinNorthing() &&
					y <= qt.getMaxNorthing()){
				result = true;
			}
			return result;
		}
	}
	
	

}
