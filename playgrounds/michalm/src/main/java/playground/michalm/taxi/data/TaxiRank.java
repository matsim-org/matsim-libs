/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.michalm.taxi.data;

import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.facilities.Facility;

public class TaxiRank implements Facility<TaxiRank> {
	private final Id<TaxiRank> id;
	private final String name;
	private final Link link;
	private final int capacity;

	private final Map<Id<Vehicle>, Vehicle> taxis = new HashMap<>();

	public TaxiRank(Id<TaxiRank> id, String name, Link link, int capacity) {
		this.id = id;
		this.name = name;
		this.link = link;
		this.capacity = capacity;
	}

	@Override
	public Id<TaxiRank> getId() {
		return id;
	}

	@Override
	public Coord getCoord() {
		return link.getCoord();
	}

	public String getName() {
		return name;
	}

	public Link getLink() {
		return link;
	}

	public boolean addTaxi(Vehicle veh) {
		if (taxis.size() == this.capacity) {
			throw new IllegalStateException();
		}

		taxis.put(veh.getId(), veh);
		return true;
	}

	public void removeTaxi(Vehicle veh) {
		taxis.remove(veh.getId());
	}

	public boolean hasCapacity() {
		return taxis.size() < this.capacity;
	}

	@Override
	public Map<String, Object> getCustomAttributes() {
		return null;
	}

	@Override
	public Id<Link> getLinkId() {
		return link.getId();
	}
}
