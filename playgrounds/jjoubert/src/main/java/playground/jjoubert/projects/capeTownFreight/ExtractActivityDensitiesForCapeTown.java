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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import playground.jjoubert.Utilities.MyShapefileReader;
import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicoreChain;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;
import playground.southafrica.freight.digicore.io.DigicoreVehicleReader;
import playground.southafrica.freight.digicore.utils.DigicoreUtils;
import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;
import playground.southafrica.utilities.grid.GeneralGrid;

public class ExtractActivityDensitiesForCapeTown {

	public static void main(String[] args) {
		Header.printHeader(ExtractActivityDensitiesForCapeTown.class.toString(), args);
		
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
		GeneralGrid gg = new GeneralGrid(500, 2);
		gg.generateGrid(area);
		gg.writeGrid(outputFolder);
		QuadTree<Tuple<String, Point>> grid = gg.getGrid();
		
		/* Perform the analysis */
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84", "WGS84_SA_Albers");
		Counter counter = new Counter("   vehicles # ");
		
		Map<String, Integer> zoneCounts = new HashMap<String, Integer>();
		Map<String, Polygon> hexPolygons = new HashMap<String, Polygon>();
		
		GeometryFactory gf = new GeometryFactory();
		for(File f : vehicleFiles){
			Id id = new IdImpl(f.getName().substring(0, f.getName().indexOf(".")));
			/* Read the vehicle file. */
			DigicoreVehicle vehicle = new DigicoreVehicle(id);
			DigicoreVehicleReader dvr = new DigicoreVehicleReader(vehicle);
			dvr.parse(f.getAbsolutePath());

			/* Process the activities. */
			for(DigicoreChain chain : vehicle.getChains()){
				for(DigicoreActivity act : chain.getAllActivities()){
					Coord coord = act.getCoord();
					if(checkInQt(grid, coord)){
						Point point = gf.createPoint(new Coordinate(coord.getX(), coord.getY()));
						
						Tuple<String, Point> tuple = grid.get(coord.getX(), coord.getY());
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
							if(!zoneCounts.containsKey(tuple.getFirst())){
								zoneCounts.put(zone, 1);
							} else{
								int oldValue = zoneCounts.get(zone);
								zoneCounts.put(zone, oldValue+1);
							}
						}
					}
				}
			}
			counter.incCounter();
		}
		counter.printCounter();
		
		writeCountsToFile(outputFolder + "zoneCounts.csv", zoneCounts);
		
		Header.printFooter();
	}
	
	private static boolean checkInQt(QuadTree<Tuple<String, Point>> qt, Coord coord){
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
	
	
	private static void writeCountsToFile(String filename, Map<String, Integer> map){
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		try{
			bw.write("Id,Count");
			bw.newLine();
			
			for(String s : map.keySet()){
				bw.write(String.format("%s,%d\n", s, map.get(s) ));
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
	
	
	private class VehicleActivityDensityAnalyser implements Callable<Map<String,Integer>>{
		private final QuadTree<Tuple<String, Point>> qt;
		private Map<String, Integer> map;
		private File file;
		
		public VehicleActivityDensityAnalyser(final QuadTree<Tuple<String,Point>> qt, File file) {
			this.qt = qt;
			this.map = new TreeMap<String, Integer>();
			this.file = file;
		}

		@Override
		public Map<String, Integer> call() throws Exception {
			
			
			
			
			return this.map;
		}
		
	}
	
	

}
