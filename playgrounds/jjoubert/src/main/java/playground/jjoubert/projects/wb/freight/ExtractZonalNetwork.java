/* *********************************************************************** *
 * project: org.matsim.*
 * ExtractZonalNetwork.java
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

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.ActivityFacility;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.util.Pair;
import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicoreChain;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;
import playground.southafrica.freight.digicore.containers.DigicoreVehicles;
import playground.southafrica.freight.digicore.io.DigicoreVehiclesReader_v2;
import playground.southafrica.utilities.Header;
import playground.southafrica.utilities.grid.GeneralGrid;
import playground.southafrica.utilities.grid.GeneralGrid.GridType;

/**
 * @author jwjoubert
 *
 */
public class ExtractZonalNetwork {
	final private static Logger LOG = Logger.getLogger(ExtractZonalNetwork.class);
	final private static GridType GRID_TYPE = GridType.HEX;
	final private static Double GRID_WIDTH = 2000.0;
	final private static String VEHICLES_CRS = TransformationFactory.HARTEBEESTHOEK94_LO29;
	
	private static DirectedSparseGraph<Id<ActivityFacility>, Pair<Id<ActivityFacility>>> graph;
	private static Map<Id<GridZone>, Map<Id<GridZone>, Double>> weightMap;
	private static Map<Point, GridZone> zoneMap;
	private static Map<Id<GridZone>, Point> pointMap;

	/**
	 * Class to build a complex network of connectivity, not between clustered
	 * facilities, but rather zones. For this implementation for the World Bank,
	 * the zones will be a hexagonal grid with width 2km.
	 *  
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(ExtractZonalNetwork.class.toString(), args);
		run(args);
		Header.printFooter();
	}
	
	public static void run(String[] args){
		String vehiclesFile = args[0];
		String shapefile = args[1];
		String networkFile = args[2];
		
		GeometryFactory gf = new GeometryFactory();
		
		/* Build the grid from the shapefile */
		LOG.info("Parsing the area shapefile...");
		ShapeFileReader sfr = new ShapeFileReader();
		sfr.readFileAndInitialize(shapefile);
		Collection<SimpleFeature> features = sfr.getFeatureSet();
		Iterator<SimpleFeature> iterator = features.iterator();
		Object o = iterator.next().getDefaultGeometry();
		Geometry sa = null;
		if(o instanceof MultiPolygon){
			sa = (Geometry) o;
		}
		LOG.info("Generating the grid...");
		GeneralGrid grid = new GeneralGrid(GRID_WIDTH, GRID_TYPE);
		grid.generateGrid(sa);
		
		/* Parse the vehicles file */
		LOG.info("Parsing the digicore vehicles container...");
		DigicoreVehicles vehicles = new DigicoreVehicles();
		DigicoreVehiclesReader_v2 dvr = new DigicoreVehiclesReader_v2(vehicles);
		dvr.readFile(vehiclesFile);
		
		/* Process the activity chains. */
		LOG.info("Processing the activity chains...");
		weightMap = new TreeMap<Id<GridZone>, Map<Id<GridZone>, Double>>();
		zoneMap = new HashMap<>();
		Counter vehicleCounter = new Counter("  vehicle # ");
		for(DigicoreVehicle vehicle : vehicles.getVehicles().values()){
			for(DigicoreChain chain : vehicle.getChains()){
				List<DigicoreActivity> activities = chain.getAllActivities();
				for(int i = 0; i < activities.size()-1; i++){
					Id<ActivityFacility> fromId = activities.get(i).getFacilityId();
					Id<ActivityFacility> toId = activities.get(i+1).getFacilityId();
					if(fromId != null && toId != null){
						/* It is a connected pair. Now get their zonal IDs. 
						 * Check that the point is actually inside the cell. */
						Coord fromCoord = activities.get(i).getCoord();
						Point fromPoint = gf.createPoint(new Coordinate(fromCoord.getX(), fromCoord.getY()));
						Point fromGridPoint = grid.getGrid().getClosest(fromCoord.getX(), fromCoord.getY());
						Geometry fromG = grid.getCellGeometry(fromGridPoint);
						boolean fromInside = fromG.covers(fromPoint);
						
						Coord toCoord = activities.get(i+1).getCoord();
						Point toPoint = gf.createPoint(new Coordinate(toCoord.getX(), toCoord.getY()));
						Point toGridPoint = grid.getGrid().getClosest(toCoord.getX(), toCoord.getY());
						Geometry toG = grid.getCellGeometry(toGridPoint);
						boolean toInside = toG.covers(toPoint);
						
						if(fromInside && toInside){
							/* The trip links two zones. Create an edge for it. */
							
							/* Check that both zones exist, or create if not. */
							Id<GridZone> fromZone = null;
							if(!zoneMap.containsKey(fromGridPoint)){
								GridZone zone = new GridZone(
										Id.create(String.valueOf(zoneMap.size()), GridZone.class), fromGridPoint);
								zoneMap.put(fromGridPoint, zone);
								pointMap.put(zone.id, fromGridPoint);
								fromZone = zone.id;
							} else{
								fromZone = zoneMap.get(fromGridPoint).id;
							}
							
							Id<GridZone> toZone = null;
							if(!zoneMap.containsKey(toGridPoint)){
								GridZone zone = new GridZone(
										Id.create(String.valueOf(zoneMap.size()), GridZone.class), toGridPoint);
								zoneMap.put(toGridPoint, zone);
								pointMap.put(zone.id, toGridPoint);
								toZone = zone.id;
							} else{
								toZone = zoneMap.get(toGridPoint).id;
							}
							
							/* Add the edge to the weighted edgelist, or create
							 * if if it doesn't exist. */
							if(!weightMap.containsKey(fromZone)){
								weightMap.put(fromZone, new TreeMap<>());
							}
							Map<Id<GridZone>, Double> thisMap = weightMap.get(fromZone);
							if(!thisMap.containsKey(toZone)){
								thisMap.put(toZone, 1.0);
							} else{
								double oldValue = thisMap.get(toZone);
								thisMap.put(toZone, oldValue += 1.0);
							}
						}
					}
				}
			}
			vehicleCounter.incCounter();
		}
		vehicleCounter.printCounter();
		
		/* Writing the edgelist to file. The coordinate transformation assumes
		 * that the vehicles were in the 'standard' Hartebeesthoek 94 Lo 29 NE
		 * projection. */
		LOG.info("Writing the edgelist to file...");
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(
				VEHICLES_CRS, TransformationFactory.WGS84);
		BufferedWriter bw = IOUtils.getBufferedWriter(networkFile);
		try{
			bw.write("fId,fX,fY,fLon,fLat,tId,tX,tY,tLon,tLat,weight");
			bw.newLine();
			for(Id<GridZone> fId : weightMap.keySet()){
				for(Id<GridZone> tId : weightMap.keySet()){
					double weight = weightMap.get(fId).get(tId);
					
					Point fPoint = pointMap.get(fId);
					Coord fCoord = CoordUtils.createCoord(fPoint.getX(), fPoint.getY());
					Coord fCoordWGS = ct.transform(fCoord);
					
					Point tPoint = pointMap.get(tId);
					Coord tCoord = CoordUtils.createCoord(tPoint.getX(), tPoint.getY());
					Coord tCoordWGS = ct.transform(tCoord);
					
					String s = String.format("%s,%.0f,%.0f,%.6f,%.6f,%s,%.0f,%.0f,%.6f,%.6f,%f\n", 
							fId.toString(),
							fCoord.getX(), fCoord.getY(),
							fCoordWGS.getX(), fCoordWGS.getY(),
							tId.toString(),
							tCoord.getX(), tCoord.getY(),
							tCoordWGS.getX(), tCoordWGS.getY(),
							weight );
					bw.write(s);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + networkFile);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + networkFile);
			}
		}
	}
	
	
	
	private static class GridZone{
		private final Id<GridZone> id;
		private final Point point;
		
		public GridZone(Id<GridZone> id, Point p) {
			this.id = id;
			this.point = p;
		}
		
		public String toString(){
			return this.id + " (" + this.point.getX() + "; " + this.point.getY() + ")";
		}
		
	}
	
	
	
	
	

}
