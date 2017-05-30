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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.gis.ShapeFileReader;
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
import playground.southafrica.freight.digicore.io.DigicoreVehicleReader_v2;
import playground.southafrica.freight.digicore.io.DigicoreVehiclesReader_v2;
import playground.southafrica.projects.complexNetworks.pathDependence.PathDependentNetwork;
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
	
	private static DirectedSparseGraph<Id<ActivityFacility>, Pair<Id<ActivityFacility>>> graph;
	private static Map<Id<Node>, Map<Id<Node>, Double>> weightMap;
	private static Map<Point, GridZone> pointMap;

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
		ShapeFileReader sfr = new ShapeFileReader();
		sfr.readFileAndInitialize(shapefile);
		Collection<SimpleFeature> features = sfr.getFeatureSet();
		Iterator<SimpleFeature> iterator = features.iterator();
		Object o = iterator.next().getDefaultGeometry();
		Geometry sa = null;
		if(o instanceof MultiPolygon){
			sa = (Geometry) o;
		}
		GeneralGrid grid = new GeneralGrid(GRID_WIDTH, GRID_TYPE);
		grid.generateGrid(sa);
		
		/* Parse the vehicles file */
		DigicoreVehicles vehicles = new DigicoreVehicles();
		DigicoreVehiclesReader_v2 dvr = new DigicoreVehiclesReader_v2(vehicles);
		dvr.readFile(vehiclesFile);
		
		/* Process the activity chains. */
		weightMap = new HashMap<>();
		pointMap = new HashMap<>();
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
						Point fromGrid = grid.getGrid().getClosest(fromCoord.getX(), fromCoord.getY());
						Geometry fromG = grid.getCellGeometry(fromGrid);
						boolean fromInside = fromG.covers(fromPoint);
						
						Coord toCoord = activities.get(i+1).getCoord();
						Point toPoint = gf.createPoint(new Coordinate(toCoord.getX(), toCoord.getY()));
						Point toGrid = grid.getGrid().getClosest(toCoord.getX(), toCoord.getY());
						Geometry toG = grid.getCellGeometry(toGrid);
						boolean toInside = toG.covers(toPoint);
						
						if(fromInside && toInside){
							/* The trip links two zones. Create an edge for it. */
							
							/* Check that both zones exist, or create if not. */
							Id<GridZone> fromZone = null;
							if(!pointMap.containsKey(fromGrid)){
								GridZone zone = new GridZone(
										Id.create(String.valueOf(pointMap.size()), GridZone.class), fromGrid);
								pointMap.put(fromGrid, zone);
								fromZone = zone.id;
							} else{
								fromZone = pointMap.get(fromGrid).id;
							}
							
							Id<GridZone> toZone = null;
							if(!pointMap.containsKey(toGrid)){
								GridZone zone = new GridZone(
										Id.create(String.valueOf(pointMap.size()), GridZone.class), toGrid);
								pointMap.put(toGrid, zone);
								toZone = zone.id;
							} else{
								toZone = pointMap.get(toGrid).id;
							}
							
							/* Add the edge to the weighted edgelist, or create
							 * if if it doesn't exist. */
							
							
						}
						
						
						
						
					}
				}
			}
			vehicleCounter.incCounter();
		}
		vehicleCounter.printCounter();
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
