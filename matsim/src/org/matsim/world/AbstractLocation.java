/* *********************************************************************** *
 * project: org.matsim.*
 * Location.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.world;

import java.util.ArrayList;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.LinkImpl;

/**
 * Basic geographical class in MATSim.
 * @see LinkImpl
 * @see ActivityFacilityImpl
 * @see Zone
 * @author Michael Balmer
 */
public abstract class AbstractLocation implements MappedLocation {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	// TODO [balmermi] The id should be unchangeable ('final'), but there
	// are modules which actually want to change ids of locations (see NetworkCleaner).
	// I'm not that happy the Ids can change (otherwise it would not be an id)!
	protected Id id;
	protected final Layer layer;
	protected final Coord center;

	// points to the zones of the lower resolution layer
	protected final TreeMap<Id,MappedLocation> up_mapping = new TreeMap<Id,MappedLocation>();

	// points to the zones of the higher resolution layer
	protected final TreeMap<Id,MappedLocation> down_mapping = new TreeMap<Id,MappedLocation>();

	//////////////////////////////////////////////////////////////////////
	// constructor
	//////////////////////////////////////////////////////////////////////

	/**
	 * A unique location for a given layer.
	 * @param layer The layer the location belongs to.
	 * @param id The unique id of that location.
	 * @param center The center of that location. Does not have to be the middle of the location object.
	 */
	protected AbstractLocation(final Layer layer, final Id id, final Coord center) {
		this.layer = layer;
		this.id = id;
		this.center = center;
		if (this.center == null) {
			Gbl.errorMsg("Location id=" + id + " instanciate without coordinate!");
		}
		if (this.layer == null) {
			Gbl.errorMsg("Location id=" + id + " instanciate without layer!");
		}
	}

	//////////////////////////////////////////////////////////////////////
	// calc methods
	//////////////////////////////////////////////////////////////////////

	public abstract double calcDistance(final Coord coord);

	//////////////////////////////////////////////////////////////////////
	// add methods
	//////////////////////////////////////////////////////////////////////

	@Deprecated
	public final void addUpMapping(final MappedLocation other) {
		if (this.layer.getUpRule() == null) {
			Gbl.errorMsg(this.toString() + "[other=" + other + " has no up_rule]");
		}
		if (!this.layer.getUpRule().getUpLayer().equals(other.getLayer())) {
			Gbl.errorMsg(this.toString() + "[other=" + other + " has wrong layer]");
		}
		Id other_id = other.getId();
		if (!this.up_mapping.containsKey(other_id)) {
			this.up_mapping.put(other_id,other);
		}
	}

	/* (non-Javadoc)
	 * @see org.matsim.world.Location#addDownMapping(org.matsim.world.Location)
	 */
	@Deprecated
	public final void addDownMapping(final MappedLocation other) {
		if (this.layer.getDownRule() == null) {
			Gbl.errorMsg(this.toString() + "[other=" + other + " has no down_rule]");
		}
		if (!this.layer.getDownRule().getDownLayer().equals(other.getLayer())) {
			Gbl.errorMsg(this.toString() + "[other=" + other + " has wrong layer]");
		}
		Id other_id = other.getId();
		if (!this.down_mapping.containsKey(other_id)) {
			this.down_mapping.put(other_id,other);
		}
	}

	//////////////////////////////////////////////////////////////////////
	// remove methods
	//////////////////////////////////////////////////////////////////////

	@Deprecated
	protected final boolean removeUpMapping(final Id other_id) {
		if (this.up_mapping.get(other_id) == null) { return true; }
		AbstractLocation other = (AbstractLocation) this.up_mapping.get(other_id);
		if (other.down_mapping.remove(this.getId()) == null) { Gbl.errorMsg("This should never happen!"); }
		if (this.up_mapping.remove(other_id) == null) { Gbl.errorMsg("This should never happen!"); }
		return true;
	}

	@Deprecated
	protected final boolean removeDownMapping(final Id other_id) {
		if (this.down_mapping.get(other_id) == null) { return true; }
		AbstractLocation other = (AbstractLocation) this.down_mapping.get(other_id);
		if (other.up_mapping.remove(this.getId()) == null) { Gbl.errorMsg("This should never happen!"); }
		if (this.down_mapping.remove(other_id) == null) { Gbl.errorMsg("This should never happen!"); }
		return true;
	}

	/* (non-Javadoc)
	 * @see org.matsim.world.Location#removeAllUpMappings()
	 */
	@Deprecated
	public final boolean removeAllUpMappings() {
		ArrayList<Id> other_ids = new ArrayList<Id>(this.up_mapping.keySet());
		for (int i=0; i<other_ids.size(); i++) { this.removeUpMapping(other_ids.get(i)); }
		return true;
	}

	/* (non-Javadoc)
	 * @see org.matsim.world.Location#removeAllDownMappings()
	 */
	@Deprecated
	public final boolean removeAllDownMappings() {
		ArrayList<Id> other_ids = new ArrayList<Id>(this.down_mapping.keySet());
		for (int i=0; i<other_ids.size(); i++) { this.removeDownMapping(other_ids.get(i)); }
		return true;
	}

	//////////////////////////////////////////////////////////////////////
	// set methods
	//////////////////////////////////////////////////////////////////////

	// TODO [balmermi] I do not like that (see above why)
	/* (non-Javadoc)
	 * @see org.matsim.world.Location#setId(org.matsim.utils.identifiers.IdI)
	 */
	public final void setId(final Id id) {
		this.id = id;
	}

	public void setCoord(Coord coord) {
		throw new UnsupportedOperationException( " derived classes need to implement their own setCoord methods if desired " ) ;
	}

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	/* (non-Javadoc)
	 * @see org.matsim.world.Location#getId()
	 */
	public final Id getId() {
		return this.id;
	}

	/* (non-Javadoc)
	 * @see org.matsim.world.Location#getLayer()
	 */
	public Layer getLayer() {
		return this.layer;
	}

	public final Coord getCoord() {
		return this.center;
	}
	

	/* (non-Javadoc)
	 * @see org.matsim.world.Location#getUpLocation(org.matsim.utils.identifiers.IdI)
	 */
	@Deprecated
	public final MappedLocation getUpLocation(final Id id) {
		return this.up_mapping.get(id);
	}

	/* (non-Javadoc)
	 * @see org.matsim.world.Location#downLocation(org.matsim.utils.identifiers.IdI)
	 */
	@Deprecated
	public final MappedLocation downLocation(final Id id) {
		return this.down_mapping.get(id);
	}

	/* (non-Javadoc)
	 * @see org.matsim.world.Location#getUpMapping()
	 */
	@Deprecated
	public final TreeMap<Id,MappedLocation> getUpMapping() {
		return this.up_mapping;
	}

	/* (non-Javadoc)
	 * @see org.matsim.world.Location#getDownMapping()
	 */
	@Deprecated
	public final TreeMap<Id, MappedLocation> getDownMapping() {
		return this.down_mapping;
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	/* (non-Javadoc)
	 * @see org.matsim.world.Location#toString()
	 */
	@Override
	public String toString() {
		return "[id=" + this.getId() + "]" +
		       "[layer=" + this.layer + "]" +
		       "[center=" + this.center + "]" +
		       "[nof_up_mapping=" + this.up_mapping.size() + "]" +
		       "[nof_down_mapping=" + this.down_mapping.size() + "]";
	}
}
