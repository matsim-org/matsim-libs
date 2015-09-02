package playground.balac.twowaycarsharingredisigned.qsim;

import java.util.ArrayList;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;

import playground.balac.allcsmodestest.facilities.CarSharingStation;

public class TwoWayCSStation implements CarSharingStation {

	
	private Link link;
	private Coord coord;
	private int numberOfVehicles;
	private ArrayList<String> vehicleIDs = new ArrayList<String>();

	public TwoWayCSStation(Link link, Coord coord, int numberOfVehicles, ArrayList<String> vehicleIDs) {
		
		this.link = link;
		this.coord = coord;
		this.numberOfVehicles = numberOfVehicles;
		this.vehicleIDs = vehicleIDs;
	}
	
	public int getNumberOfVehicles() {
		
		return numberOfVehicles;
	}
	
	public Link getLink() {
		
		return link;
	}
	
	public ArrayList<String> getIDs() {
		
		return vehicleIDs;
	}

	public Coord getCoord() {
		return coord;
	}

	public void setCoord(Coord coord) {
		this.coord = coord;
	}
	
}