package playground.kai.urbansim.ids;

import org.matsim.basic.v01.IdImpl;
import org.matsim.interfaces.basic.v01.Id;


public class HHIdFactory implements IdFactory {
	public HHId createId(String str) {
		return new HHId(str) ;
	}
	public HHId createId(long ii) {
		return new HHId(ii) ;
	}
}
