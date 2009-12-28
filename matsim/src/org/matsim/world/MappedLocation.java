package org.matsim.world;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

public interface MappedLocation extends Location, Mappings {

	/**
	 * Calculates the distance from a given coordinate to that location.
	 * The interpretation of <em>distance</em> differ from the actual type of location.
	 * @param coord The coordinate from which the distance to that location should be calculated.
	 * @return the distance to that location
	 */
	public abstract double calcDistance(final Coord coord);
	// yyyy kn I think this can be more easily implemented as a utility that compares two coordinates. jun09

	// TODO [balmermi] I do not like that (see above why)
	@Deprecated // does not really make much sense to set id's outside the creational method
	public abstract void setId(Id id);

}