package playground.balac.allcsmodestest.qsim;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;

import playground.balac.freefloating.config.FreeFloatingConfigGroup;
import playground.balac.freefloating.qsim.FreeFloatingStation;
import playground.balac.onewaycarsharingredisgned.config.OneWayCarsharingRDConfigGroup;
import playground.balac.onewaycarsharingredisgned.qsimparking.OneWayCarsharingRDWithParkingStation;
import playground.balac.twowaycarsharingredisigned.config.TwoWayCSConfigGroup;
import playground.balac.twowaycarsharingredisigned.qsim.TwoWayCSStation;

public class CarSharingVehicles {
	
	private Scenario scenario;
	
	private final ArrayList<FreeFloatingStation> ffvehiclesLocation;
	private final ArrayList<OneWayCarsharingRDWithParkingStation> owvehiclesLocation;
	private final ArrayList<TwoWayCSStation> twvehiclesLocation;
	
	public CarSharingVehicles(Scenario scenario) {
		
		this.scenario = scenario;
		ffvehiclesLocation = new ArrayList<FreeFloatingStation>();
		owvehiclesLocation = new ArrayList<OneWayCarsharingRDWithParkingStation>();
		twvehiclesLocation = new ArrayList<TwoWayCSStation>();
	}
	
	public ArrayList<FreeFloatingStation> getFreeFLoatingVehicles() {
		
		return this.ffvehiclesLocation;
	}
	
	public ArrayList<OneWayCarsharingRDWithParkingStation> getOneWayVehicles() {
		
		
		return this.owvehiclesLocation;
	}
	
	public ArrayList<TwoWayCSStation> getRoundTripVehicles() {
		
		return this.twvehiclesLocation;
	}
	
	public void readVehicleLocations() throws IOException {
		final FreeFloatingConfigGroup configGroupff = (FreeFloatingConfigGroup)
				scenario.getConfig().getModule( FreeFloatingConfigGroup.GROUP_NAME );
		
		final OneWayCarsharingRDConfigGroup configGroupow = (OneWayCarsharingRDConfigGroup)
				scenario.getConfig().getModule( OneWayCarsharingRDConfigGroup.GROUP_NAME );
		
		final TwoWayCSConfigGroup configGrouptw = (TwoWayCSConfigGroup)
				scenario.getConfig().getModule( TwoWayCSConfigGroup.GROUP_NAME );
		BufferedReader reader;
		String s;
		
		LinkUtils linkUtils = new LinkUtils(this.scenario.getNetwork());
		
		if (configGroupff.useFeeFreeFloating()) {
		 reader = IOUtils.getBufferedReader(configGroupff.getvehiclelocations());
		    s = reader.readLine();
		    s = reader.readLine();
		    int i = 1;
		    while(s != null) {
		    	
		    	String[] arr = s.split("\t", -1);
			    
		    	CoordImpl coordStart = new CoordImpl(arr[2], arr[3]);
				Link l = linkUtils.getClosestLink(coordStart);		    	
				ArrayList<String> vehIDs = new ArrayList<String>();
		    	
		    	for (int k = 0; k < Integer.parseInt(arr[6]); k++) {
		    		vehIDs.add(Integer.toString(i));
		    		i++;
		    	}
		    	
		    	FreeFloatingStation f = new FreeFloatingStation(l, Integer.parseInt(arr[6]), vehIDs);
		    	
		    	ffvehiclesLocation.add(f);
		    	s = reader.readLine();
		    	
		    }	  
		}
		if (configGroupow.useOneWayCarsharing()) {
		reader = IOUtils.getBufferedReader(configGroupow.getvehiclelocations());
		s = reader.readLine();
		    s = reader.readLine();
		    int i = 1;
		    while(s != null) {
		    	
		    	String[] arr = s.split("\t", -1);
		    
		    	CoordImpl coordStart = new CoordImpl(arr[2], arr[3]);
				Link l = linkUtils.getClosestLink(coordStart);		    	
				ArrayList<String> vehIDs = new ArrayList<String>();
		    	
		    	for (int k = 0; k < Integer.parseInt(arr[6]); k++) {
		    		vehIDs.add(Integer.toString(i));
		    		i++;
		    	}
		    	//add parking spaces
		    	OneWayCarsharingRDWithParkingStation f = new OneWayCarsharingRDWithParkingStation(l, Integer.parseInt(arr[6]), vehIDs, Integer.parseInt(arr[6]) * 2);
		    	
		    	owvehiclesLocation.add(f);
		    	s = reader.readLine();
		    	
		    }	 
		}
		if (configGrouptw.useTwoWayCarsharing()) {
		    reader = IOUtils.getBufferedReader(configGrouptw.getvehiclelocations());
		    s = reader.readLine();
		    s = reader.readLine();
		    int i = 1;
		    while(s != null) {
		    	
		    	String[] arr = s.split("\t", -1);
		    
		    	CoordImpl coordStart = new CoordImpl(arr[2], arr[3]);
		    	Link l = linkUtils.getClosestLink(coordStart);			    	
				ArrayList<String> vehIDs = new ArrayList<String>();
		    	
		    	for (int k = 0; k < Integer.parseInt(arr[6]); k++) {
		    		vehIDs.add(Integer.toString(i));
		    		i++;
		    	}
				TwoWayCSStation f = new TwoWayCSStation(l, Integer.parseInt(arr[6]), vehIDs);
		    	
				twvehiclesLocation.add(f);
		    	s = reader.readLine();
		    	
		    }	
		}
	}
	
		
	private class LinkUtils {
		
		Network network;
		public LinkUtils(Network network) {
			
			this.network = network;		}
		
		public LinkImpl getClosestLink(Coord coord) {
			
			double distance = (1.0D / 0.0D);
		    Id<Link> closestLinkId = Id.create(0L, Link.class);
		    for (Link link : network.getLinks().values()) {
		      LinkImpl mylink = (LinkImpl)link;
		      Double newDistance = Double.valueOf(mylink.calcDistance(coord));
		      if (newDistance.doubleValue() < distance) {
		        distance = newDistance.doubleValue();
		        closestLinkId = link.getId();
		      }

		    }

		    return (LinkImpl)network.getLinks().get(closestLinkId);
			
			
		}
	}
	

}
