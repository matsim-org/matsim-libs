package org.matsim.world;

import java.util.ArrayList;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

public interface Layer {

	public void setName(final String name);

	@Deprecated
	// a "type" that returns an "Id" ???
	public Id getType();

	public String getName();

	@Deprecated
	// use of mapping rules is discouraged
	public MappingRule getUpRule();

	@Deprecated
	// use of mapping rules is discouraged
	public MappingRule getDownRule();

	public MappedLocation getLocation(final Id location_id);

	@Deprecated
	// string-based methods are discouraged
	public Location getLocation(final String location_id);

	/**
	 * Note: this is method is, I think, <em> not </em> quad-tree based, and therefore is rather slow in 
	 * most cases.
	 *
	 * @param coord A coordinate to which the nearest location should be returned.
	 *
	 * @return the Location with the smallest distance to the given coordinate. If multiple locations have
	 * the same minimal distance, all of them are returned.
	 */
	public ArrayList<MappedLocation> getNearestLocations(final Coord coord);

	/**
	 * Note: this is method is, I think, <em> not </em> quad-tree based, and therefore is rather slow in 
	 * most cases.
	 * 
	 * @param coord A coordinate to which the nearest location should be returned.
	 * @param excludeLocation A location that should be ignored when finding the nearest location. Useful to
	 * find the nearest neighbor of the excluded location.
	 *
	 * @return the Location with the smallest distance to the given coordinate. If multiple locations have
	 * the same minimal distance, all of them are returned.
	 * 
	 */
	public ArrayList<MappedLocation> getNearestLocations(final Coord coord, final Location excludeLocation);

	public TreeMap<Id, ? extends MappedLocation> getLocations();

	public String toString();

	@Deprecated
	public boolean removeUpRule();

	@Deprecated
	public boolean removeDownRule();

	@Deprecated
	public void setUpRule(final MappingRule up_rule);

	@Deprecated
	public void setDownRule(final MappingRule down_rule);

	@Deprecated
	public void forceDownRuleToNull();

	@Deprecated
	public void forceUpRuleToNull();

}