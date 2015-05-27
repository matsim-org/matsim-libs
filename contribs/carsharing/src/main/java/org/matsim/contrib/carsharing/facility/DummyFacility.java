package org.matsim.contrib.carsharing.facility;

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.facilities.Facility;

public class DummyFacility implements Facility<DummyFacility>{
	private final Coord coord;
	private final Id<Link> linkId;
	private final Map<String, Object> customAttributes = new LinkedHashMap<String, Object>();

	public DummyFacility(
			
		final Coord coord,
		final Id<Link> linkId) {
		
		this.coord = coord;
		this.linkId = linkId;
		
	}
		public Coord getCoord() {
		return coord;
	}

	

	public Map<String, Object> getCustomAttributes() {
		return customAttributes;
	}

	public Id<Link> getLinkId() {
		return linkId;
	}
	@Override
	public Id<DummyFacility> getId() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public String toString() {
		return "[coord=" + coord.toString() + "] [linkId=" + linkId + "]" ;
	}
}
