package org.matsim.contrib.carsharing.readers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.carsharing.stations.CarsharingStation;
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
	private String csType;
	
	private ArrayList<CarsharingStation> twStations = new ArrayList<CarsharingStation>();
	private ArrayList<CarsharingStation> owStations = new ArrayList<CarsharingStation>();
	private ArrayList<CSVehicle> ffVehicles = new ArrayList<CSVehicle>();    
	
	private QuadTree<CSVehicle> ffVehicleLocationQuadTree;	
	private QuadTree<CarsharingStation> owvehicleLocationQuadTree;	
	private QuadTree<CarsharingStation> twvehicleLocationQuadTree;

	private Map<String, CarsharingStation> twowaycarsharingstationsMap = new HashMap<String, CarsharingStation>();
	private Map<String, CarsharingStation> onewaycarsharingstationsMap = new HashMap<String, CarsharingStation>();

	public Map<String, CarsharingStation> getTwowaycarsharingstationsMap() {
		return twowaycarsharingstationsMap;
	}

	public Map<String, CarsharingStation> getOnewaycarsharingstationsMap() {
		return onewaycarsharingstationsMap;
	}

	private Map<CSVehicle, Link> ffvehiclesMap = new HashMap<CSVehicle, Link>();	
	private Map<String, CSVehicle> ffvehicleIdMap = new HashMap<String, CSVehicle>();
	
	private Map<String, CSVehicle> owvehicleIdMap = new HashMap<String, CSVehicle>();
	private Map<CSVehicle, Link> owvehiclesMap = new HashMap<CSVehicle, Link>();
	
	private Map<String, CSVehicle> csvehicleIdMap = new HashMap<String, CSVehicle>();
	
	private Map<String, CSVehicle> twvehicleIdMap = new HashMap<String, CSVehicle>();
	private Map<CSVehicle, Link> twvehiclesMap = new HashMap<CSVehicle, Link>();
	
	public Map<String, CSVehicle> getOwvehicleIdMap() {
		return owvehicleIdMap;
	}

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
	    owvehicleLocationQuadTree = new QuadTree<CarsharingStation>(minx, miny, maxx, maxy);
	    twvehicleLocationQuadTree = new QuadTree<CarsharingStation>(minx, miny, maxx, maxy);		
	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		
		if (name.equals("twoway") || name.equals("oneway")) {
			csType = name;
			id = atts.getValue("id");
			String lat = atts.getValue("lat");
			String lon = atts.getValue("lon");
			Coord coordStation = new Coord(Double.parseDouble(lat), Double.parseDouble(lon));
				
			link = (Link)NetworkUtils.getNearestLinkExactly(network, coordStation);
			vehicles = new ArrayList<StationBasedVehicle>();
			if (name.equals("oneway"))
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
			this.csvehicleIdMap.put(atts.getValue("id"), ffcsvehicle);
		}
		else if (name.equals("vehicle")) {
			
			StationBasedVehicle vehicle = new StationBasedVehicle(atts.getValue("type"), atts.getValue("vehicleID"), id, csType);
			vehicles.add(vehicle);
			this.csvehicleIdMap.put(atts.getValue("vehicleID"), vehicle);

			
		}
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
		
		if (name.equals("twoway") || name.equals("oneway")) {
			Map<String, Integer> numberOfVehiclesPerType = new HashMap<String, Integer>();
			Map<String, ArrayList<CSVehicle>> vehiclesPerType = new HashMap<String, ArrayList<CSVehicle>>();
			
			for (StationBasedVehicle vehicle : vehicles) {
				if (name.equals("oneway")) {
					this.owvehicleIdMap.put(vehicle.getVehicleId(), vehicle);
					this.owvehiclesMap.put(vehicle, link);
				}
				else if (name.equals("twoway")) {
					this.twvehicleIdMap.put(vehicle.getVehicleId(), vehicle);
					this.twvehiclesMap.put(vehicle, link);
				}
				if (numberOfVehiclesPerType.containsKey(vehicle.getVehicleType())) {
					
					int number = numberOfVehiclesPerType.get(vehicle.getVehicleType());
					ArrayList<CSVehicle> oldArray = vehiclesPerType.get(vehicle.getVehicleType());
					number++;
					oldArray.add(vehicle);
					numberOfVehiclesPerType.put(vehicle.getVehicleType(), number);
					vehiclesPerType.put(vehicle.getVehicleType(), oldArray);
					
				}
				else {
					
					numberOfVehiclesPerType.put(vehicle.getVehicleType(), 1);
					ArrayList<CSVehicle> newArray = new ArrayList<CSVehicle>();
					newArray.add(vehicle);
					vehiclesPerType.put(vehicle.getVehicleType(), newArray);
				}
			}
			if (name.equals("twoway")) {
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

	public QuadTree<CarsharingStation> getOwvehicleLocationQuadTree() {
		return owvehicleLocationQuadTree;
	}

	public QuadTree<CarsharingStation> getTwvehicleLocationQuadTree() {
		return twvehicleLocationQuadTree;
	}

	public ArrayList<CarsharingStation> getTwStations() {
		return twStations;
	}

	public ArrayList<CarsharingStation> getOwStations() {
		return this.owStations;
	}

	public ArrayList<CSVehicle> getFFVehicles() {
		return this.ffVehicles;
	}	
	
	public Map<CSVehicle, Link> getFfvehiclesMap() {
		return ffvehiclesMap;
	}

	public Map<CSVehicle, Link> getOwvehiclesMap() {
		// TODO Auto-generated method stub
		return owvehiclesMap;
	}

	public Map<String, CSVehicle> getCsehiclesMap() {
		return csvehicleIdMap;
	}

	public Map<CSVehicle, Link> getTwvehiclesMap() {
		// TODO Auto-generated method stub
		return twvehiclesMap;
	}	
}
