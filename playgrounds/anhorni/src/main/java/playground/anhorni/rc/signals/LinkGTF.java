package playground.anhorni.rc.signals;

import java.util.ArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public class LinkGTF {
	
	private Id<Link> linkId;
	private ArrayList<Double> hourlyGTFS = new ArrayList<Double>();
	
	public LinkGTF(Id<Link> linkId) {
		this.linkId = linkId;
	}
	
	public void addGTF(int h, double val) {
		this.hourlyGTFS.add(h, val);
	}

	public Id<Link> getLinkId() {
		return linkId;
	}

	public void setLinkId(Id<Link> linkId) {
		this.linkId = linkId;
	}

	public ArrayList<Double> getHourlyGTFS() {
		return hourlyGTFS;
	}

	public void setHourlyGTFS(ArrayList<Double> hourlyGTFS) {
		this.hourlyGTFS = hourlyGTFS;
	}

}
