package playground.ciarif.retailers;

import java.util.Map;

import org.matsim.basic.v01.Id;
import org.matsim.facilities.Facility;

public interface RetailerStrategy {
	public void moveFacilities(Map<Id, Facility> facilities);
}
