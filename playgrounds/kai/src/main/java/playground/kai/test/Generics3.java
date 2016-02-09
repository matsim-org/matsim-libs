package playground.kai.test;

import org.matsim.api.core.v01.Id;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.Facility;

public class Generics3 {

	void run(){
		
		Id<ActivityFacility> actFacId = Id.create("actFac", ActivityFacility.class ) ;
		
		Id<? extends Facility<?>> facId = actFacId ;
		
	}
	
}
