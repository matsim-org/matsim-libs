package playground.kai.urbansim;

import org.matsim.basic.v01.Id;


public class ParcelIdBuilder implements IdBuilder {

	public ParcelId createId(String str) {
		return new ParcelId( str ) ;
	}

	public ParcelId createId(long ii) {
		return new ParcelId( ii ) ;
	}

}
