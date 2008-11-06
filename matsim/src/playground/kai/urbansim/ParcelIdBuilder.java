package playground.kai.urbansim;

import org.matsim.basic.v01.Id;

import playground.kai.IdBuilder;

public class ParcelIdBuilder implements IdBuilder {

	public ParcelIdImpl createId(String str) {
		return new ParcelIdImpl( str ) ;
	}

	public ParcelIdImpl createId(long ii) {
		return new ParcelIdImpl( ii ) ;
	}

}
