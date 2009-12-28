package playground.kai.urbansim.ids;

import org.matsim.api.core.v01.Id;


public class LocationIdFactory implements IdFactory {

	public LocationId createId(String str) {
		return new LocationId( str ) ;
	}

	public LocationId createId(long ii) {
		return new LocationId( ii ) ;
	}

}
