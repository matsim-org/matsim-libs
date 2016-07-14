package playground.balac.utils.emissions;

import java.util.ArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public class RouteInfo {
	private ArrayList<Id<Link>> links;
	private ArrayList<Double> travelTime;
	public RouteInfo() {
		
		links = new ArrayList<Id<Link>>();
		travelTime = new ArrayList<Double>();
	}
	
	public void addNewLink(Id<Link> l, double travelTime) {
		
		links.add(l);
		this.travelTime.add(travelTime);
		
	}

	public ArrayList<Id<Link>> getLinks() {
		return links;
	}

	public ArrayList<Double> getTravelTime() {
		return travelTime;
	}
	
	
	

}
