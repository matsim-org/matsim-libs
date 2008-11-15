package playground.kai.urbansim;

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;


public class HHIdBuilder implements IdBuilder {
	public HHId createId(String str) {
		return new HHId(str) ;
	}
	public HHId createId(long ii) {
		return new HHId(ii) ;
	}
}
