package org.matsim.world;

import java.util.TreeMap;

import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.identifiers.IdI;

public interface Location {

	/**
	 * Calculates the distance from a given coordinate to that location.
	 * The interpretation of <em>distance</em> differ from the actual type of location.
	 * @param coord The coordinate from which the distance to that location should be calculated.
	 * @return the distance to that location
	 */
	public abstract double calcDistance(final CoordI coord);

	/**
	 * Connects two location of two different layers. The layers have to be 'neighbors' which
	 * means that the layers are connected via a MappingRule. The other layer have to be 'above'
	 * the one this location belongs to.
	 * @param other
	 */
	public abstract void addUpMapping(final Location other);

	/**
	 * Connects two location of two different layers. The layers have to be 'neighbors' which
	 * means that the layers are connected via a MappingRule. The other layer have to be 'below'
	 * the one this location belongs to.
	 * @param other
	 */
	public abstract void addDownMapping(final Location other);

	public abstract boolean removeAllUpMappings();

	public abstract boolean removeAllDownMappings();

	// TODO [balmermi] I do not like that (see above why)
	public abstract void setId(IdI id);

	public abstract IdI getId();

	public abstract Layer getLayer();

	public abstract CoordI getCenter();

	public abstract Location getUpLocation(IdI id);

	public abstract Location downLocation(IdI id);

	public abstract TreeMap<IdI, Location> getUpMapping();

	public abstract TreeMap<IdI, Location> getDownMapping();

	public abstract String toString();

}