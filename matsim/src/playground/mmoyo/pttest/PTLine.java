package playground.mmoyo.pttest;

import java.util.ArrayList;
import java.util.List;

import org.matsim.basic.v01.IdImpl;

public class PTLine {
	private IdImpl id;
	private String type;
	private boolean withDedicatedTracks;

	private String strLinksRoute = null;
	public List<String> strLinksRoute2 = new ArrayList<String>();

	public PTLine(IdImpl id, String type, boolean withDedicatedTracks, String strlinksRoute) {
		this.id = id;
		this.type = type;
		this.withDedicatedTracks = withDedicatedTracks;
		this.strLinksRoute = strlinksRoute;
		FillRouteLinksList();
	}

	public IdImpl getId() {
		return id;
	}

	public void setId(IdImpl id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public boolean isWithDedicatedTracks() {
		return this.withDedicatedTracks;
	}

	public void setWithDedicatedTracks(boolean withDedicatedTracks) {
		this.withDedicatedTracks = withDedicatedTracks;
	}

	public String getStrLinksRoute() {
		return strLinksRoute;
	}

	public void setStrLinksRoute(String strLinksRoute) {
		this.strLinksRoute = strLinksRoute;
	}

	private void FillRouteLinksList() {
		// According to code from matsim/plans/route.java
		String [] tempRoute = strLinksRoute.split("[ \t\n]+");
		int ini = 0;
		if ((tempRoute.length > 0) && (tempRoute[0].equals(""))) { ini = 1; }
		for (int i = ini; i < tempRoute.length; i++) {
			this.strLinksRoute2.add(tempRoute[i]);
		}
	}
		 
}// class
