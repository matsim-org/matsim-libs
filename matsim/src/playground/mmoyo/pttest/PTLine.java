package playground.mmoyo.pttest;

import java.util.ArrayList;
import java.util.List;

import org.matsim.basic.v01.IdImpl;

public class PTLine {
	private IdImpl id;
	private String type;
	private boolean withDedicatedTracks;

	private String strLinksRoute = null; // ja
	public List<String> strLinksRoute2 = new ArrayList<String>(); // ja
	private List<PTLink> ptLinksRoute = new ArrayList<PTLink>();

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

	public List<PTLink> getLinksRoute() {
		return ptLinksRoute;
	}

	public void setLinksRoute(List<PTLink> linksRoute) {
		this.ptLinksRoute = linksRoute;
	}

	private void FillRouteLinksList() {
		// TODO Optimize with code from matsim/plans/route.java
		String idLink = "";
		for (int i = 0; i < this.strLinksRoute.length(); i++) {
			// TODO: Improve in order to validate fewer times.
			if (this.strLinksRoute.charAt(i) == ' ') {
				strLinksRoute2.add(idLink);
				idLink = "";
			} else if (i == ((this.strLinksRoute.length()) - 1)) {
				idLink = idLink + this.strLinksRoute.charAt(i);
				strLinksRoute2.add(idLink);
			} else {
				idLink = idLink + this.strLinksRoute.charAt(i);
			}
		}// for
	}// CreateLinksRoute

}// class
