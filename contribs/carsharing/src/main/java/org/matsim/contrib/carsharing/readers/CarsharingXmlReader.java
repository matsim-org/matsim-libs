package org.matsim.contrib.carsharing.readers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.carsharing.stations.FreeFloatingStation;
import org.matsim.contrib.carsharing.stations.OneWayCarsharingStation;
import org.matsim.contrib.carsharing.stations.TwoWayCarsharingStation;
import org.matsim.contrib.carsharing.vehicles.FFCSVehicle;
import org.matsim.contrib.carsharing.vehicles.StationBasedVehicle;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

public class CarsharingXmlReader extends MatsimXmlParser {

	private Network network;
	private String id;	
	private int avaialbleParkingSpots;
	private ArrayList<StationBasedVehicle> vehicles;
	private Link link;	
	
	private ArrayList<TwoWayCarsharingStation> twStations = new ArrayList<TwoWayCarsharingStation>();
	private ArrayList<OneWayCarsharingStation> owStations = new ArrayList<OneWayCarsharingStation>();
	private ArrayList<FFCSVehicle> ffVehicles = new ArrayList<FFCSVehicle>();    
	public CarsharingXmlReader(Network network) {
		
		this.network = network;
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
			ffVehicles.add(new FFCSVehicle(type, atts.getValue("id"), link));
			
			
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
				twStations.add(station);
			}
			else {
				OneWayCarsharingStation station = new OneWayCarsharingStation(id, link, numberOfVehiclesPerType,
						vehiclesPerType, avaialbleParkingSpots);
				owStations.add(station);
				
			}
		}
		
		
	}
	
	public ArrayList<TwoWayCarsharingStation> getTwStations() {
		return twStations;
	}

	public ArrayList<OneWayCarsharingStation> getOwStations() {
		return this.owStations;
	}

	public ArrayList<FFCSVehicle> getFFVehicles() {
		return this.ffVehicles;
	}	
}
