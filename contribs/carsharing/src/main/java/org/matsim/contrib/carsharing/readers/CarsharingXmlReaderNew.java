package org.matsim.contrib.carsharing.readers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.carsharing.manager.supply.CompanyContainer;
import org.matsim.contrib.carsharing.manager.supply.FreeFloatingVehiclesContainer;
import org.matsim.contrib.carsharing.manager.supply.OneWayContainer;
import org.matsim.contrib.carsharing.manager.supply.TwoWayContainer;
import org.matsim.contrib.carsharing.manager.supply.VehiclesContainer;
import org.matsim.contrib.carsharing.qsim.FreefloatingAreas;
import org.matsim.contrib.carsharing.stations.CarsharingStation;
import org.matsim.contrib.carsharing.stations.OneWayCarsharingStation;
import org.matsim.contrib.carsharing.stations.TwoWayCarsharingStation;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;
import org.matsim.contrib.carsharing.vehicles.FFVehicleImpl;
import org.matsim.contrib.carsharing.vehicles.StationBasedVehicle;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.SearchableNetwork;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;
/** 
 * 
 * @author balac
 */
public class CarsharingXmlReaderNew extends MatsimXmlParser {
	private Network network;
	private String id;	
	private int avaialbleParkingSpots;
	private ArrayList<StationBasedVehicle> vehicles;
	private Link link;	
	private String csType;
	private String companyName;
	private boolean hasFF = false;
	private boolean hasOW = false;
	private boolean hasTW = false;
	private Map<String, CompanyContainer> companies = new HashMap<String, CompanyContainer>();
	private Set<String> companyNames = new TreeSet<String>();
	private Map<String, FreefloatingAreas> freefloatingAreas;

	public void setFreefloatingAreas(Map<String, FreefloatingAreas> areas) {
		this.freefloatingAreas = areas;
	}

	public Set<String> getCompanyNames() {
		return this.companyNames;
	}

	public Map<String, CompanyContainer> getCompanies() {
		return companies;
	}

	private Map<String, CSVehicle> allVehicles = new HashMap<String, CSVehicle>();
	
	public Map<String, CSVehicle> getAllVehicles() {
		return allVehicles;
	}

	private Map<CSVehicle, Link> allVehicleLocations = new HashMap<CSVehicle, Link>();

	
	private ArrayList<CarsharingStation> twStations = new ArrayList<CarsharingStation>();
	private ArrayList<CarsharingStation> owStations = new ArrayList<CarsharingStation>();
	
	private QuadTree<CSVehicle> ffVehicleLocationQuadTree;	
	private QuadTree<CarsharingStation> owvehicleLocationQuadTree;	
	private QuadTree<CarsharingStation> twvehicleLocationQuadTree;

	private Map<String, CarsharingStation> twowaycarsharingstationsMap = new HashMap<String, CarsharingStation>();
	private Map<String, CarsharingStation> onewaycarsharingstationsMap = new HashMap<String, CarsharingStation>();
	
	private Map<CSVehicle, Link> ffvehiclesMap = new HashMap<CSVehicle, Link>();	
	private Map<String, CSVehicle> ffvehicleIdMap = new HashMap<String, CSVehicle>();
	
	private Map<String, CSVehicle> owvehicleIdMap = new HashMap<String, CSVehicle>();
	private Map<CSVehicle, Link> owvehiclesMap = new HashMap<CSVehicle, Link>();
	
	
	private Map<String, CSVehicle> twvehicleIdMap = new HashMap<String, CSVehicle>();
	private Map<CSVehicle, Link> twvehiclesMap = new HashMap<CSVehicle, Link>();

	private Map<CSVehicle, String> owvehicleToStationMap = new HashMap<CSVehicle, String>();
		
	public Map<String, CarsharingStation> getTwowaycarsharingstationsMap() {
		return twowaycarsharingstationsMap;
	}

	public Map<String, CarsharingStation> getOnewaycarsharingstationsMap() {
		return onewaycarsharingstationsMap;
	}

	
	
	public Map<String, CSVehicle> getOwvehicleIdMap() {
		return owvehicleIdMap;
	}

	public Map<String, CSVehicle> getFfvehicleIdMap() {
		return ffvehicleIdMap;
	}

	public CarsharingXmlReaderNew(Network network) {
		
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
		
		
		if (name.equals("company")) {
			
			companyName = atts.getValue("name");
			this.companyNames.add(companyName);
			createQuadTrees();

			twowaycarsharingstationsMap = new HashMap<String, CarsharingStation>();
			onewaycarsharingstationsMap = new HashMap<String, CarsharingStation>();

			ffvehiclesMap = new HashMap<CSVehicle, Link>();	
			ffvehicleIdMap = new HashMap<String, CSVehicle>();
			
			owvehicleIdMap = new HashMap<String, CSVehicle>();
			owvehiclesMap = new HashMap<CSVehicle, Link>();			
			
			twvehicleIdMap = new HashMap<String, CSVehicle>();
			twvehiclesMap = new HashMap<CSVehicle, Link>();
			
			owvehicleToStationMap = new HashMap<CSVehicle, String>();
			
			
			//allVehicles = new HashMap<String, CSVehicle>();
			//companies = new HashMap<String, CompanyContainer>();
		}
		
		else if (name.equals("twoway") || name.equals("oneway")) {
			csType = name;
			id = atts.getValue("id");
			String xCoord = atts.getValue("x");
			String yCoord = atts.getValue("y");
			Coord coordStation = new Coord(Double.parseDouble(xCoord), Double.parseDouble(yCoord));
				
			link = (Link)NetworkUtils.getNearestLinkExactly(network, coordStation);
			vehicles = new ArrayList<StationBasedVehicle>();
			if (name.equals("oneway")) {
				avaialbleParkingSpots = Integer.parseInt(atts.getValue("freeparking"));
				hasOW = true;
			}
			else
				hasTW = true;
			
		}
		else if (name.equals("freefloating")) {
			hasFF= true;
			String xCoord = atts.getValue("x");
			String yCoord = atts.getValue("y");
			String type = atts.getValue("type");
			Coord coordStation = new Coord(Double.parseDouble(xCoord), Double.parseDouble(yCoord));
				
			link = (Link)NetworkUtils.getNearestLinkExactly(network, coordStation);
			FFVehicleImpl ffcsvehicle = new FFVehicleImpl(type, atts.getValue("id"), companyName);
			ffVehicleLocationQuadTree.put(link.getCoord().getX(), link.getCoord().getY(), ffcsvehicle);
			ffvehiclesMap.put(ffcsvehicle, link);
			ffvehicleIdMap.put(atts.getValue("id"), ffcsvehicle);
			this.allVehicles.put(atts.getValue("id"), ffcsvehicle);
			allVehicleLocations.put(ffcsvehicle, link);
		}
		else if (name.equals("vehicle")) {
			
			StationBasedVehicle vehicle = new StationBasedVehicle(atts.getValue("type"), atts.getValue("vehicleID"), id, csType, companyName);
			vehicles.add(vehicle);
			this.allVehicles.put(atts.getValue("vehicleID"), vehicle);
			this.allVehicleLocations.put(vehicle, link);
			
		}
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
		if (name.equals("company")) {
			CompanyContainer companyContainer = new CompanyContainer(this.companyName);

			if (hasFF) {
				FreeFloatingVehiclesContainer ffvehiclesContainer = new FreeFloatingVehiclesContainer(ffVehicleLocationQuadTree,
					ffvehicleIdMap, ffvehiclesMap);

				if (this.freefloatingAreas != null) {
					ffvehiclesContainer.setNetwork((SearchableNetwork) this.network);
					ffvehiclesContainer.setFreefloatingAreas(this.freefloatingAreas.get(this.companyName));
				}

				companyContainer.addCarsharingType("freefloating", ffvehiclesContainer);
			hasFF = false;
			}
			if (hasTW) {
				VehiclesContainer twvehiclesContainer = new TwoWayContainer(twvehicleLocationQuadTree,
						twowaycarsharingstationsMap, twvehiclesMap);
					companyContainer.addCarsharingType("twoway", twvehiclesContainer);
				hasTW = false;
			}
			
			if (hasOW) {
				VehiclesContainer owvehiclesContainer = new OneWayContainer(owvehicleLocationQuadTree, onewaycarsharingstationsMap,
						owvehiclesMap, owvehicleToStationMap);
					companyContainer.addCarsharingType("oneway", owvehiclesContainer);
					hasOW = false;
				
			}
			
			companies.put(companyName, companyContainer);
			
		}
		else if (name.equals("twoway") || name.equals("oneway")) {
			Map<String, Integer> numberOfVehiclesPerType = new HashMap<String, Integer>();
			Map<String, ArrayList<CSVehicle>> vehiclesPerType = new HashMap<String, ArrayList<CSVehicle>>();
			
			for (CSVehicle vehicle : vehicles) {
				if (name.equals("oneway")) {
					this.owvehicleIdMap.put(vehicle.getVehicleId(), vehicle);
					this.owvehiclesMap.put(vehicle, link);
					this.owvehicleToStationMap.put(vehicle, id);
				}
				else if (name.equals("twoway")) {
					this.twvehicleIdMap.put(vehicle.getVehicleId(), vehicle);
					this.twvehiclesMap.put(vehicle, link);
				}
				if (numberOfVehiclesPerType.containsKey(vehicle.getType())) {
					
					int number = numberOfVehiclesPerType.get(vehicle.getType());
					ArrayList<CSVehicle> oldArray = vehiclesPerType.get(vehicle.getType());
					number++;
					oldArray.add(vehicle);
					numberOfVehiclesPerType.put(vehicle.getType(), number);
					vehiclesPerType.put(vehicle.getType(), oldArray);
					
				}
				else {
					
					numberOfVehiclesPerType.put(vehicle.getType(), 1);
					ArrayList<CSVehicle> newArray = new ArrayList<CSVehicle>();
					newArray.add(vehicle);
					vehiclesPerType.put(vehicle.getType(), newArray);
				}
			}
			if (name.equals("twoway")) {
				TwoWayCarsharingStation station = new TwoWayCarsharingStation(id, link, numberOfVehiclesPerType, vehiclesPerType);
				
				//TODO: check if the station already exists on the link
				
				/*if (twvehicleLocationQuadTree.getDisk(link.getCoord().getX(), link.getCoord().getY(), 0.0).size() != 0) {
					TwoWayCarsharingStation stationOld = (TwoWayCarsharingStation) twvehicleLocationQuadTree.getClosest(link.getCoord().getX(), link.getCoord().getY());
					for (String type : vehiclesPerType.keySet()) {
						
						if (stationOld.getVehiclesPerType().containsKey(type)) {
							
							
						}
					}
					stationOld.
				}*/
				
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

	public Map<CSVehicle, Link> getAllVehicleLocations() {
		return allVehicleLocations;
	}

	public void setAllVehicleLocations(Map<CSVehicle, Link> allVehicleLocations) {
		this.allVehicleLocations = allVehicleLocations;
	}
}
