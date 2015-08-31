package playground.balac.allcsmodestest.qsim;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;

import playground.balac.freefloating.config.FreeFloatingConfigGroup;
import playground.balac.freefloating.qsim.FreeFloatingStation;
import playground.balac.freefloating.qsim.FreeFloatingVehiclesLocation;
import playground.balac.onewaycarsharingredisgned.config.OneWayCarsharingRDConfigGroup;
import playground.balac.onewaycarsharingredisgned.qsimparking.OneWayCarsharingRDWithParkingStation;
import playground.balac.onewaycarsharingredisgned.qsimparking.OneWayCarsharingRDWithParkingVehicleLocation;
import playground.balac.twowaycarsharingredisigned.config.TwoWayCSConfigGroup;
import playground.balac.twowaycarsharingredisigned.qsim.TwoWayCSStation;
import playground.balac.twowaycarsharingredisigned.qsim.TwoWayCSVehicleLocation;

public class CarSharingVehicles {
	
	private Scenario scenario;
	private FreeFloatingVehiclesLocation ffvehiclesLocation;
	private OneWayCarsharingRDWithParkingVehicleLocation owvehiclesLocation;
	private TwoWayCSVehicleLocation twvehiclesLocation;
	
	public CarSharingVehicles(Scenario scenario) throws IOException {
		this.scenario = scenario;
		//readVehicleLocations();
	}
	
	public FreeFloatingVehiclesLocation getFreeFLoatingVehicles() {
		
		return this.ffvehiclesLocation;
	}
	
	public OneWayCarsharingRDWithParkingVehicleLocation getOneWayVehicles() {
		
		
		return this.owvehiclesLocation;
	}
	
	public TwoWayCSVehicleLocation getRoundTripVehicles() {
		
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
		    ArrayList<FreeFloatingStation> ffStations = new ArrayList<FreeFloatingStation>();
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
		    ArrayList<OneWayCarsharingRDWithParkingStation> owStations = new ArrayList<OneWayCarsharingRDWithParkingStation>();

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
		    	OneWayCarsharingRDWithParkingStation f = new OneWayCarsharingRDWithParkingStation(l, Integer.parseInt(arr[6]), vehIDs, Integer.parseInt(arr[7]));
		    	
		    	owStations.add(f);
		    	s = reader.readLine();
		    	
		    }	
		    this.owvehiclesLocation = new OneWayCarsharingRDWithParkingVehicleLocation(scenario, owStations);
		}
		if (configGrouptw.useTwoWayCarsharing()) {
		    reader = IOUtils.getBufferedReader(configGrouptw.getvehiclelocations());
		    s = reader.readLine();
		    s = reader.readLine();
		    int i = 1;
		    ArrayList<TwoWayCSStation> twStations = new ArrayList<TwoWayCSStation>();

		    while(s != null) {
		    	
		    	String[] arr = s.split("\t", -1);
		    
		    	Coord coordStart = new CoordImpl(arr[2], arr[3]);
		    	Link l = linkUtils.getClosestLink(coordStart);			    	
				ArrayList<String> vehIDs = new ArrayList<String>();
		    	
		    	for (int k = 0; k < Integer.parseInt(arr[6]); k++) {
		    		vehIDs.add(Integer.toString(i));
		    		i++;
		    	}
				TwoWayCSStation f = new TwoWayCSStation(l, coordStart, Integer.parseInt(arr[6]), vehIDs);
		    	
				twStations.add(f);
		    	s = reader.readLine();
		    	
		    }
		    
		    this.twvehiclesLocation = new TwoWayCSVehicleLocation(scenario, twStations);
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
