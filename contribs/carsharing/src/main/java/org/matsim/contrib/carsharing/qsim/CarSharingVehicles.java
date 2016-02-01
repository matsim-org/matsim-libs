package org.matsim.contrib.carsharing.qsim;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.carsharing.config.FreeFloatingConfigGroup;
import org.matsim.contrib.carsharing.config.OneWayCarsharingConfigGroup;
import org.matsim.contrib.carsharing.config.TwoWayCarsharingConfigGroup;
import org.matsim.contrib.carsharing.stations.FreeFloatingStation;
import org.matsim.contrib.carsharing.stations.OneWayCarsharingStation;
import org.matsim.contrib.carsharing.stations.TwoWayCarsharingStation;
import org.matsim.contrib.carsharing.vehicles.FreeFloatingVehiclesLocation;
import org.matsim.contrib.carsharing.vehicles.OneWayCarsharingVehicleLocation;
import org.matsim.contrib.carsharing.vehicles.TwoWayCarsharingVehicleLocation;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.utils.io.IOUtils;


public class CarSharingVehicles {
	private static final Logger log = Logger.getLogger(CarSharingVehicles.class);

	private Scenario scenario;
	private FreeFloatingVehiclesLocation ffvehiclesLocation;
	private OneWayCarsharingVehicleLocation owvehiclesLocation;
	private TwoWayCarsharingVehicleLocation twvehiclesLocation;
	
	public CarSharingVehicles(Scenario scenario) throws IOException {
		this.scenario = scenario;
		//readVehicleLocations();
	}
	
	public FreeFloatingVehiclesLocation getFreeFLoatingVehicles() {
		
		return this.ffvehiclesLocation;
	}
	
	public OneWayCarsharingVehicleLocation getOneWayVehicles() {
		
		
		return this.owvehiclesLocation;
	}
	
	public TwoWayCarsharingVehicleLocation getTwoWayVehicles() {
		
		return this.twvehiclesLocation;
	}
	
	public void readVehicleLocations() throws IOException {
		final FreeFloatingConfigGroup configGroupff = (FreeFloatingConfigGroup)
				scenario.getConfig().getModule( FreeFloatingConfigGroup.GROUP_NAME );
		
		final OneWayCarsharingConfigGroup configGroupow = (OneWayCarsharingConfigGroup)
				scenario.getConfig().getModule( OneWayCarsharingConfigGroup.GROUP_NAME );
		
		final TwoWayCarsharingConfigGroup configGrouptw = (TwoWayCarsharingConfigGroup)
				scenario.getConfig().getModule( TwoWayCarsharingConfigGroup.GROUP_NAME );
		BufferedReader reader;
		String s;
		
		LinkUtils linkUtils = new LinkUtils(this.scenario.getNetwork());
		
		if (configGroupff.useFeeFreeFloating()) {
			 reader = IOUtils.getBufferedReader(configGroupff.getvehiclelocations());
			    s = reader.readLine();
			    s = reader.readLine();
			    int i = 1;
			    ArrayList<FreeFloatingStation> ffStations = new ArrayList<FreeFloatingStation>();
			    HashMap<Link, FreeFloatingStation> stationToLinkMap = new HashMap<Link, FreeFloatingStation>();

			    while(s != null) {
			    	
			    	String[] arr = s.split("\t", -1);
				    
			    	Coord coordStart = new Coord(Double.parseDouble(arr[2]), Double.parseDouble(arr[3]));
					Link l = linkUtils.getClosestLink(coordStart);		    	
					ArrayList<String> vehIDs = new ArrayList<String>();
			    	
			    	for (int k = 0; k < Integer.parseInt(arr[6]); k++) {
			    		vehIDs.add(Integer.toString(i));
			    		i++;
			    	}
			    	
			    	if (stationToLinkMap.containsKey(l)) {
			    		
			    		FreeFloatingStation oldStation = stationToLinkMap.get(l);
			    		
			    		log.warn("Merging freefloating carsharing stations that are mapped to the same link with id: " + oldStation.getLink().getId().toString() + " .");

			    		ArrayList<String> oldVehIDs = oldStation.getIDs();
			    		ArrayList<String> newvehIDs = new ArrayList<String>();
						for (String id : oldVehIDs) {
							newvehIDs.add(id);
						}
						newvehIDs.addAll(vehIDs);
						FreeFloatingStation newStation = new FreeFloatingStation(l, Integer.parseInt(arr[6]) + oldStation.getNumberOfVehicles(),
				    			newvehIDs);
				    	
						ffStations.remove(oldStation);
				    	stationToLinkMap.put(l, newStation);
				    	ffStations.add(newStation);
						
			    	}
			    	else {
			    		
			    		FreeFloatingStation newStation = new FreeFloatingStation(l, Integer.parseInt(arr[6]) ,
			    				vehIDs);
				    	
				    	stationToLinkMap.put(l, newStation);
				    	ffStations.add(newStation);
			    		
			    		
			    	}
			    	s = reader.readLine();
			    	
			    }	
			    
			    ffvehiclesLocation = new FreeFloatingVehiclesLocation(scenario, ffStations);
			}
			
			if (configGroupow.useOneWayCarsharing()) {
				reader = IOUtils.getBufferedReader(configGroupow.getvehiclelocations());
				s = reader.readLine();
			    s = reader.readLine();
			    int i = 1;
			    ArrayList<OneWayCarsharingStation> owStations = new ArrayList<OneWayCarsharingStation>();
			    HashMap<Link, OneWayCarsharingStation> stationToLinkMap = new HashMap<Link, OneWayCarsharingStation>();

			    while(s != null) {
			    	
			    	String[] arr = s.split("\t", -1);
			    
			    	Coord coordStart = new Coord(Double.parseDouble(arr[2]), Double.parseDouble(arr[3]));
					Link l = linkUtils.getClosestLink(coordStart);		    	
					ArrayList<String> vehIDs = new ArrayList<String>();
			    	
			    	for (int k = 0; k < Integer.parseInt(arr[6]); k++) {
			    		vehIDs.add(Integer.toString(i));
			    		i++;
			    	}

			    	if (stationToLinkMap.containsKey(l)) {
			    		
			    		OneWayCarsharingStation oldStation = stationToLinkMap.get(l);
			    		
			    		log.warn("Merging oneway carsharing stations that are mapped to the same link with id: " + oldStation.getLink().getId().toString() + " .");

			    		ArrayList<String> oldVehIDs = oldStation.getIDs();
			    		ArrayList<String> newvehIDs = new ArrayList<String>();
						for (String id : oldVehIDs) {
							newvehIDs.add(id);
						}
						newvehIDs.addAll(vehIDs);
						OneWayCarsharingStation newStation = new OneWayCarsharingStation(l, Integer.parseInt(arr[6]) + oldStation.getNumberOfVehicles(),
				    			newvehIDs, oldStation.getNumberOfAvailableParkingSpaces() + Integer.parseInt(arr[7]));
				    	
						owStations.remove(oldStation);
				    	stationToLinkMap.put(l, newStation);
				    	owStations.add(newStation);
						
			    	}
			    	else {
			    		
			    		OneWayCarsharingStation newStation = new OneWayCarsharingStation(l, Integer.parseInt(arr[6]) ,
			    				vehIDs, Integer.parseInt(arr[7]));
				    	
				    	stationToLinkMap.put(l, newStation);
				    	owStations.add(newStation);
			    		
			    		
			    	}
			    	s = reader.readLine();
			    	
			    }	
			    this.owvehiclesLocation = new OneWayCarsharingVehicleLocation(scenario, owStations);
			}
			if (configGrouptw.useTwoWayCarsharing()) {
			    reader = IOUtils.getBufferedReader(configGrouptw.getvehiclelocations());
			    s = reader.readLine();
			    s = reader.readLine();
			    int i = 1;
			    ArrayList<TwoWayCarsharingStation> twStations = new ArrayList<TwoWayCarsharingStation>();
			    HashMap<Link, TwoWayCarsharingStation> stationToLinkMap = new HashMap<Link, TwoWayCarsharingStation>();
			    while(s != null) {
			    	
			    	String[] arr = s.split("\t", -1);
			    
			    	Coord coordStart = new Coord(Double.parseDouble(arr[2]), Double.parseDouble(arr[3]));
			    	Link l = linkUtils.getClosestLink(coordStart);			    	
					ArrayList<String> vehIDs = new ArrayList<String>();
			    	
			    	for (int k = 0; k < Integer.parseInt(arr[6]); k++) {
			    		vehIDs.add(Integer.toString(i));
			    		i++;
			    	}
			    	
			    	if (stationToLinkMap.containsKey(l)) {
			    		
			    		TwoWayCarsharingStation oldStation = stationToLinkMap.get(l);
			    		
			    		log.warn("Merging twoway carsharing stations that are mapped to the same link with id: " + oldStation.getLink().getId().toString() + " .");

			    		ArrayList<String> oldVehIDs = oldStation.getIDs();
			    		ArrayList<String> newvehIDs = new ArrayList<String>();
						for (String id : oldVehIDs) {
							newvehIDs.add(id);
						}
						newvehIDs.addAll(vehIDs);
				    	TwoWayCarsharingStation newStation = new TwoWayCarsharingStation(l, Integer.parseInt(arr[6]) + oldStation.getNumberOfVehicles(),
				    			newvehIDs);
				    	
				    	twStations.remove(oldStation);
				    	stationToLinkMap.put(l, newStation);
				    	twStations.add(newStation);
						
			    	}
			    	else {
			    		
			    		TwoWayCarsharingStation newStation = new TwoWayCarsharingStation(l, Integer.parseInt(arr[6]) ,
			    				vehIDs);
				    	
				    	stationToLinkMap.put(l, newStation);
				    	twStations.add(newStation);
			    		
			    		
			    	}
			    	
			    	s = reader.readLine();
			    	
			    }
			    
			    this.twvehiclesLocation = new TwoWayCarsharingVehicleLocation(scenario, twStations);
			}
		}
		
	private class LinkUtils {
		
		NetworkImpl network;
		
		public LinkUtils(Network network) {
			
			this.network = NetworkImpl.createNetwork();
			Set<String> restrictions = new HashSet<>();
			restrictions = new HashSet<>();
			restrictions.add("car");
			TransportModeNetworkFilter networkFilter = new TransportModeNetworkFilter(scenario.getNetwork());

			networkFilter = new TransportModeNetworkFilter(scenario.getNetwork());
			networkFilter.filter(this.network, restrictions);
			
		}
		
		public LinkImpl getClosestLink(Coord coord) {			

		    return (LinkImpl)network.getNearestLinkExactly(coord);			
			
		}
	}
	

}
