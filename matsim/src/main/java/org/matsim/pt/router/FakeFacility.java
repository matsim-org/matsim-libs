package org.matsim.pt.router;

import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.facilities.Facility;

public final class FakeFacility implements Facility {
	private Coord coord;
	public FakeFacility( Coord coord ) { this.coord = coord ; }
	@Override public Coord getCoord() {
		return this.coord ;
	}

	@Override
	public Map<String, Object> getCustomAttributes() {
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public Id getLinkId() {
		throw new RuntimeException("not implemented") ;
	}
	
}
