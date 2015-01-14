package playground.anhorni.rc.signals;

import java.util.ArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public class SignalLink {
	
	private Id<Link> id;	
	private ArrayList<Double> gtfs = new ArrayList<Double>();
	
	public void setGtfsPerHour(int houer, double value) {
		this.gtfs.add(houer, value);
	}

	public Id<Link> getId() {
		return id;
	}

	public void setId(Id<Link> id) {
		this.id = id;
	}

	public ArrayList<Double> getGtfs() {
		return gtfs;
	}
}
