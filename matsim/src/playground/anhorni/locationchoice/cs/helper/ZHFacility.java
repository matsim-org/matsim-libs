package playground.anhorni.locationchoice.cs.helper;

import org.matsim.basic.v01.Id;
import org.matsim.utils.geometry.Coord;

public class ZHFacility  {
	
	private Id id = null;
	private Coord center = null;	
	private Coord exactPosition = null;
	private Id linkId;
	
	
	public ZHFacility(final Id id, Coord coord, Coord exactPosition, Id linkId) {
		this.id = id;
		this.center = coord;
		this.exactPosition = exactPosition;
		this.linkId = linkId;
	}

	public Id getId() {
		return id;
	}

	public void setId(Id id) {
		this.id = id;
	}

	public Coord getCenter() {
		return center;
	}

	public void setCenter(Coord center) {
		this.center = center;
	}
	
	public double getCrowFlyDistance(Coord other) {
		return this.center.calcDistance(other);
	}

	public Coord getExactPosition() {
		return exactPosition;
	}

	public void setExactPosition(Coord exactPosition) {
		this.exactPosition = exactPosition;
	}

	public Id getLinkId() {
		return linkId;
	}

	public void setLinkId(Id linkId) {
		this.linkId = linkId;
	}
}
