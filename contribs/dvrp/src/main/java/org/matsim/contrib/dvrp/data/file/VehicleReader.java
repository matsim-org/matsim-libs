/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.data.file;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.FleetImpl;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.data.VehicleImpl;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import java.util.Map;
import java.util.Objects;
import java.util.Stack;

/**
 * @author michalm
 */
public class VehicleReader extends MatsimXmlParser {
	private static final String VEHICLE = "vehicle";

	private static final int DEFAULT_CAPACITY = 1;
	private static final double DEFAULT_T_0 = 0;
	private static final double DEFAULT_T_1 = 24 * 60 * 60;

	private final FleetImpl fleet;
	private final Map<Id<Link>, ? extends Link> links;

	public VehicleReader(Network network, FleetImpl fleet) {
		this.fleet = fleet;
		links = network.getLinks();
	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if (VEHICLE.equals(name)) {
			fleet.addVehicle(createVehicle(atts));
		}
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
	}

	private Vehicle createVehicle(Attributes atts) {
		Id<Vehicle> id = Id.create(atts.getValue("id"), Vehicle.class);
		Link startLink = Objects.requireNonNull(links.get(Id.createLinkId(atts.getValue("start_link"))));
        double cap = ReaderUtils.getDouble(atts, "capacity", DEFAULT_CAPACITY);
        int capacity = (int) cap;
        if (capacity != cap) {
            throw new IllegalArgumentException("capacity must be an Integer value");
        }
		//for backwards compatibility when reading files. capacity used be double
		double t0 = ReaderUtils.getDouble(atts, "t_0", DEFAULT_T_0);
		double t1 = ReaderUtils.getDouble(atts, "t_1", DEFAULT_T_1);
		return createVehicle(id, startLink, capacity, t0, t1, atts);
	}

	protected Vehicle createVehicle(Id<Vehicle> id, Link startLink, int capacity, double t0, double t1,
			Attributes atts) {
		return new VehicleImpl(id, startLink, capacity, t0, t1);
	}
}
