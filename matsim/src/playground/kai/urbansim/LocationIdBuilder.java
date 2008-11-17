package playground.kai.urbansim;

import org.matsim.basic.v01.Id;


public class LocationIdBuilder implements IdBuilder {

	public LocationId createId(String str) {
		return new LocationId( str ) ;
	}

	public LocationId createId(long ii) {
		return new LocationId( ii ) ;
	}

}
