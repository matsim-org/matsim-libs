package playground.ciarif.flexibletransports.router;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

public class CarSharingStation {
	private final Id id;
	private final Coord coord;

	/*package*/ CarSharingStation(Id id, Coord coord) {
		super();
		this.id = id;
		this.coord = coord;
	}

	public Id getId() {
		return id;
	}

	public Coord getCoord() {
		return coord;
	}

}
