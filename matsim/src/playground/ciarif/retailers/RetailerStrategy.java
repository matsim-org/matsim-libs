package playground.ciarif.retailers;

import java.util.Map;

import org.matsim.facilities.Facility;
import org.matsim.interfaces.basic.v01.Id;

public interface RetailerStrategy {
	public void moveFacilities(Map<Id, Facility> facilities);
}
