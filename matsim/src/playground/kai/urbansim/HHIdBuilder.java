package playground.kai.urbansim;

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;

import playground.kai.IdBuilder;

public class HHIdBuilder implements IdBuilder {
	public HHIdImpl createId(String str) {
		return new HHIdImpl(str) ;
	}
	public HHIdImpl createId(long ii) {
		return new HHIdImpl(ii) ;
	}
}
