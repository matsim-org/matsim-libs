package playground.kai;

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;

public interface IdBuilder {
	public Id createId(String str) ;
	
	public Id createId(long ii) ;
	
}
