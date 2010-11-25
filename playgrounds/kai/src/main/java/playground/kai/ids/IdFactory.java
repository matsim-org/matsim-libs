package playground.kai.ids;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;


public class IdFactory {
	
	public static Id createPersonId(long id) {
		return new IdImpl(id) ;
	}
	public static Id createPersonId(String str) {
		return new IdImpl(str) ;
	}
	
	

}
