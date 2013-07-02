package playground.southafrica.freight.digicore.containers;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.facilities.Facility;

import com.vividsolutions.jts.geom.Geometry;

public class DigicoreFacility implements Facility {
		
	private Id id;
	private Id linkId;
	private Coord coord;
	private Map<String, Object> attributes = new HashMap<String, Object>();

	public DigicoreFacility(Id id) {
		this.id = id;
	}

	public Id getId() {
		return this.id;
	}

	public Coord getCoord() {
		return this.coord;
	}

	public void setCoord(Coord coord) {
		this.coord = coord;
	}
	
	@Override
	public Id getLinkId() {
		return this.linkId;
	}

	@Override
	public Map<String, Object> getCustomAttributes() {
		return this.attributes;
	}

}
