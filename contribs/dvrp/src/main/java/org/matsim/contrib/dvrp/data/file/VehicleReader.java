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

import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.*;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;


public class VehicleReader
    extends MatsimXmlParser
{
    private static final String VEHICLE = "vehicle";

    private static final double DEFAULT_CAPACITY = 1;
    private static final double DEFAULT_T_0 = 0;
    private static final double DEFAULT_T_1 = 24 * 60 * 60;

    private VrpData data;
    private Map<Id<Link>, ? extends Link> links;


    public VehicleReader(Network network, VrpData data)
    {
        this.data = data;
        links = network.getLinks();
    }


    @Override
    public void startTag(String name, Attributes atts, Stack<String> context)
    {
        if (VEHICLE.equals(name)) {
            data.addVehicle(createVehicle(atts));
        }
    }


    @Override
    public void endTag(String name, String content, Stack<String> context)
    {}


    protected Vehicle createVehicle(Attributes atts)
    {
        Id<Vehicle> id = Id.create(atts.getValue("id"), Vehicle.class);
        Link startLink = links.get(Id.createLinkId(atts.getValue("start_link")));
        double capacity = ReaderUtils.getDouble(atts, "capacity", DEFAULT_CAPACITY);
        double t0 = ReaderUtils.getDouble(atts, "t_0", DEFAULT_T_0);
        double t1 = ReaderUtils.getDouble(atts, "t_1", DEFAULT_T_1);
        return new VehicleImpl(id, startLink, capacity, t0, t1);
    }
}
