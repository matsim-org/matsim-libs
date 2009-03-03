package playground.ciarif.retailers;

import java.util.Map;

import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Facility;

public interface RetailerStrategy {
	public void moveFacilities(Map<Id, Facility> facilities);
}
