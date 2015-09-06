package org.matsim.contrib.carsharing.qsim;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

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
import org.matsim.core.utils.io.IOUtils;


public class CarSharingVehicles {
	
	private Scenario scenario;
	private FreeFloatingVehiclesLocation ffvehiclesLocation;
	private OneWayCarsharingVehicleLocation owvehiclesLocation;
	private TwoWayCarsharingVehicleLocation twvehiclesLocation;
	
	public CarSharingVehicles(Scenario scenario) throws IOException {
		this.scenario = scenario;
		readVehicleLocations();
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
		    while(s != null) {
		    	
		    	String[] arr = s.split("\t", -1);

				Coord coordStart = new Coord(Double.parseDouble(arr[2]), Double.parseDouble(arr[3]));
				Link l = linkUtils.getClosestLink(coordStart);		    	
				ArrayList<String> vehIDs = new ArrayList<String>();
		    	
		    	for (int k = 0; k < Integer.parseInt(arr[6]); k++) {
		    		vehIDs.add(Integer.toString(i));
		    		i++;
		    	}
		    	
		    	FreeFloatingStation f = new FreeFloatingStation(l, Integer.parseInt(arr[6]), vehIDs);
		    	
		    	ffStations.add(f);
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

		    while(s != null) {
		    	
		    	String[] arr = s.split("\t", -1);

				Coord coordStart = new Coord(Double.parseDouble(arr[2]), Double.parseDouble(arr[3]));
				Link l = linkUtils.getClosestLink(coordStart);		    	
				ArrayList<String> vehIDs = new ArrayList<String>();
		    	
		    	for (int k = 0; k < Integer.parseInt(arr[6]); k++) {
		    		vehIDs.add(Integer.toString(i));
		    		i++;
		    	}
		    	//add parking spaces
		    	OneWayCarsharingStation f = new OneWayCarsharingStation(l, Integer.parseInt(arr[6]), vehIDs, Integer.parseInt(arr[6]) * 2);
		    	
		    	owStations.add(f);
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

		    while(s != null) {
		    	
		    	String[] arr = s.split("\t", -1);

				Coord coordStart = new Coord(Double.parseDouble(arr[2]), Double.parseDouble(arr[3]));
		    	Link l = linkUtils.getClosestLink(coordStart);			    	
				ArrayList<String> vehIDs = new ArrayList<String>();
		    	
		    	for (int k = 0; k < Integer.parseInt(arr[6]); k++) {
		    		vehIDs.add(Integer.toString(i));
		    		i++;
		    	}
		    	TwoWayCarsharingStation f = new TwoWayCarsharingStation(l, Integer.parseInt(arr[6]), vehIDs);
		    	
				twStations.add(f);
		    	s = reader.readLine();
		    	
		    }
		    
		    this.twvehiclesLocation = new TwoWayCarsharingVehicleLocation(scenario, twStations);
		}
	}
		
	private class LinkUtils {
		
		NetworkImpl network;
		
		public LinkUtils(Network network) {
			
			this.network = (NetworkImpl) network;		}
		
		public LinkImpl getClosestLink(Coord coord) {
			
			

		    return (LinkImpl)network.getNearestLinkExactly(coord);
			
			
		}
	}
	

}
