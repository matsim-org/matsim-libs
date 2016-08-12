package org.matsim.contrib.carsharing.readers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.carsharing.stations.OneWayCarsharingStation;
import org.matsim.contrib.carsharing.stations.TwoWayCarsharingStation;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;
import org.matsim.contrib.carsharing.vehicles.FFVehicleImpl;
import org.matsim.contrib.carsharing.vehicles.StationBasedVehicle;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;
/** 
 * 
 * @author balac
 */
public class CarsharingXmlReader extends MatsimXmlParser {

	private Network network;
	private String id;	
	private int avaialbleParkingSpots;
	private ArrayList<StationBasedVehicle> vehicles;
	private Link link;	
	
	private ArrayList<TwoWayCarsharingStation> twStations = new ArrayList<TwoWayCarsharingStation>();
	private ArrayList<OneWayCarsharingStation> owStations = new ArrayList<OneWayCarsharingStation>();
	private ArrayList<CSVehicle> ffVehicles = new ArrayList<CSVehicle>();    
	
	private QuadTree<CSVehicle> ffVehicleLocationQuadTree;	
	private QuadTree<OneWayCarsharingStation> owvehicleLocationQuadTree;	
	private QuadTree<TwoWayCarsharingStation> twvehicleLocationQuadTree;

	private Map<String, TwoWayCarsharingStation> twowaycarsharingstationsMap = new HashMap<String, TwoWayCarsharingStation>();
	private Map<String, OneWayCarsharingStation> onewaycarsharingstationsMap = new HashMap<String, OneWayCarsharingStation>();

	public Map<String, TwoWayCarsharingStation> getTwowaycarsharingstationsMap() {
		return twowaycarsharingstationsMap;
	}

	public Map<String, OneWayCarsharingStation> getOnewaycarsharingstationsMap() {
		return onewaycarsharingstationsMap;
	}

	private Map<CSVehicle, Link> ffvehiclesMap = new HashMap<CSVehicle, Link>();	
	private Map<String, CSVehicle> ffvehicleIdMap = new HashMap<String, CSVehicle>();

	public Map<String, CSVehicle> getFfvehicleIdMap() {
		return ffvehicleIdMap;
	}

	public CarsharingXmlReader(Network network) {
		
		this.network = network;		
		createQuadTrees();
	}
	
	private void createQuadTrees() {

	    double minx = (1.0D / 0.0D);
	    double miny = (1.0D / 0.0D);
	    double maxx = (-1.0D / 0.0D);
	    double maxy = (-1.0D / 0.0D);

        for (Link l : this.network.getLinks().values()) {
	      if (l.getCoord().getX() < minx) minx = l.getCoord().getX();
	      if (l.getCoord().getY() < miny) miny = l.getCoord().getY();
	      if (l.getCoord().getX() > maxx) maxx = l.getCoord().getX();
	      if (l.getCoord().getY() <= maxy) continue; maxy = l.getCoord().getY();
	    }
	    minx -= 1.0D; miny -= 1.0D; maxx += 1.0D; maxy += 1.0D;

	    ffVehicleLocationQuadTree = new QuadTree<CSVehicle>(minx, miny, maxx, maxy);
	    owvehicleLocationQuadTree = new QuadTree<OneWayCarsharingStation>(minx, miny, maxx, maxy);
	    twvehicleLocationQuadTree = new QuadTree<TwoWayCarsharingStation>(minx, miny, maxx, maxy);		
	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		
		if (name.equals("twowaycarsharing") || name.equals("onewaycarsharing")) {
			
			id = atts.getValue("id");
			String lat = atts.getValue("lat");
			String lon = atts.getValue("lon");
			Coord coordStation = new Coord(Double.parseDouble(lat), Double.parseDouble(lon));
				
			link = (Link)NetworkUtils.getNearestLinkExactly(network, coordStation);
			vehicles = new ArrayList<StationBasedVehicle>();
			if (name.equals("onewaycarsharing"))
				avaialbleParkingSpots = Integer.parseInt(atts.getValue("freeparking"));
			
		}
		else if (name.equals("freefloating")) {
			String lat = atts.getValue("lat");
			String lon = atts.getValue("lon");
			String type = atts.getValue("type");
			Coord coordStation = new Coord(Double.parseDouble(lat), Double.parseDouble(lon));
				
			link = (Link)NetworkUtils.getNearestLinkExactly(network, coordStation);
			FFVehicleImpl ffcsvehicle = new FFVehicleImpl(type, atts.getValue("id"));
			ffVehicleLocationQuadTree.put(link.getCoord().getX(), link.getCoord().getY(), ffcsvehicle);
			ffvehiclesMap.put(ffcsvehicle, link);
			ffvehicleIdMap.put(atts.getValue("id"), ffcsvehicle);
			
		}
		else if (name.equals("vehicle")) {
			
			StationBasedVehicle vehicle = new StationBasedVehicle(atts.getValue("type"), atts.getValue("vehicleID"), id);
			vehicles.add(vehicle);
			
		}
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
		
		if (name.equals("twowaycarsharing") || name.equals("onewaycarsharing")) {
			Map<String, Integer> numberOfVehiclesPerType = new HashMap<String, Integer>();
			Map<String, ArrayList<StationBasedVehicle>> vehiclesPerType = new HashMap<String, ArrayList<StationBasedVehicle>>();
			
			for (StationBasedVehicle vehicle : vehicles) {
				
				if (numberOfVehiclesPerType.containsKey(vehicle.getType())) {
					
					int number = numberOfVehiclesPerType.get(vehicle.getType());
					ArrayList<StationBasedVehicle> oldArray = vehiclesPerType.get(vehicle.getType());
					number++;
					oldArray.add(vehicle);
					numberOfVehiclesPerType.put(vehicle.getType(), number);
					vehiclesPerType.put(vehicle.getType(), oldArray);
					
				}
				else {
					
					numberOfVehiclesPerType.put(vehicle.getType(), 1);
					ArrayList<StationBasedVehicle> newArray = new ArrayList<StationBasedVehicle>();
					newArray.add(vehicle);
					vehiclesPerType.put(vehicle.getType(), newArray);
				}
			}
			if (name.equals("twowaycarsharing")) {
				TwoWayCarsharingStation station = new TwoWayCarsharingStation(id, link, numberOfVehiclesPerType, vehiclesPerType);
				
				twvehicleLocationQuadTree.put(link.getCoord().getX(), link.getCoord().getY(), station);
				this.twowaycarsharingstationsMap.put(id, station);
				twStations.add(station);
			}
			else {
				OneWayCarsharingStation station = new OneWayCarsharingStation(id, link, numberOfVehiclesPerType,
						vehiclesPerType, avaialbleParkingSpots);
				
				owvehicleLocationQuadTree.put(link.getCoord().getX(), link.getCoord().getY(), station);
				this.onewaycarsharingstationsMap.put(id, station);
				owStations.add(station);
				
			}
		}
		
		
	}
	
	public QuadTree<CSVehicle> getFfVehicleLocationQuadTree() {
		return ffVehicleLocationQuadTree;
	}

	public QuadTree<OneWayCarsharingStation> getOwvehicleLocationQuadTree() {
		return owvehicleLocationQuadTree;
	}

	public QuadTree<TwoWayCarsharingStation> getTwvehicleLocationQuadTree() {
		return twvehicleLocationQuadTree;
	}

	public ArrayList<TwoWayCarsharingStation> getTwStations() {
		return twStations;
	}

	public ArrayList<OneWayCarsharingStation> getOwStations() {
		return this.owStations;
	}

	public ArrayList<CSVehicle> getFFVehicles() {
		return this.ffVehicles;
	}	
	
	public Map<CSVehicle, Link> getFfvehiclesMap() {
		return ffvehiclesMap;
	}
}
