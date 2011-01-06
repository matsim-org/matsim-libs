package playground.jjoubert.digicoreNew.containers;

import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

public class DigicoreLocation implements BasicLocation {
		
	private Id id;
	private Coord coord;

	public DigicoreLocation(Id id) {
		this.id = id;
	}

	@Override
	public Id getId() {
		return this.id;
	}

	@Override
	public Coord getCoord() {
		return this.coord;
	}
}
