package playground.kai.urbansim.ids;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;


public class HHIdFactory implements IdFactory {
	public HHId createId(String str) {
		return new HHId(str) ;
	}
	public HHId createId(long ii) {
		return new HHId(ii) ;
	}
}
