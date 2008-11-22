package playground.kai.urbansim.ids;

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;

public interface IdFactory {
	public Id createId(String str) ;
	
	public Id createId(long ii) ;
	
}
