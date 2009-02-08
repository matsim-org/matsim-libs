package playground.anhorni.locationchoice.cs.helper;

import org.matsim.basic.v01.Id;
import org.matsim.utils.geometry.Coord;

public class ZHFacility  {
	
	private Id id = null;
	private Coord mappedPosition = null;	
	private Coord exactPosition = null;
	private Id linkId;
	
	
	public ZHFacility(final Id id, Coord mappedPosition, Coord exactPosition, Id linkId) {
		this.id = id;
		this.mappedPosition = mappedPosition;
		this.exactPosition = exactPosition;
		this.linkId = linkId;
	}

	public Id getId() {
		return id;
	}

	public void setId(Id id) {
		this.id = id;
	}

	public Coord getMappedposition() {
		return mappedPosition;
	}

	public void setCenter(Coord mappedPosition) {
		this.mappedPosition = mappedPosition;
	}
	
	public double getCrowFlyDistance(Coord other) {
		return this.mappedPosition.calcDistance(other);
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
