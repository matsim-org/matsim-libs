package playground.kai.urbansim.ids;



public class LocationIdFactory implements IdFactory {

	public LocationId createId(String str) {
		return new LocationId( str ) ;
	}

	public LocationId createId(long ii) {
		return new LocationId( ii ) ;
	}

}
