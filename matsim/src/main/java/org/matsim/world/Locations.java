package org.matsim.world;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.BasicLocations;



public interface Locations extends BasicLocations {
	
	public abstract MappedLocation getLocation(final Id location_id);

	public abstract Map<Id, MappedLocation> getLocations();

}