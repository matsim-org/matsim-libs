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
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.VrpData;
import org.matsim.contrib.dvrp.data.model.Depot;
import org.matsim.contrib.dvrp.data.model.impl.DepotImpl;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentVehicleImpl;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;


public class DepotReader
    extends MatsimXmlParser
{
    private final static String DEPOT = "depot";
    private final static String VEHICLE = "vehicle";

    private Scenario scenario;
    private VrpData data;
    private Map<Id, ? extends Link> links;

    private Depot currentDepot;


    public DepotReader(Scenario scenario, VrpData data)
    {
        this.scenario = scenario;
        this.data = data;
        this.links = scenario.getNetwork().getLinks();
    }


    public void readFile(String filename)
    {
        parse(filename);
    }


    @Override
    public void startTag(String name, Attributes atts, Stack<String> context)
    {
        if (DEPOT.equals(name)) {
            startDepot(atts);
        }
        else if (VEHICLE.equals(name)) {
            startVehicle(atts);
        }
    }


    @Override
    public void endTag(String name, String content, Stack<String> context)
    {}


    private void startDepot(Attributes atts)
    {
        int id = data.getDepots().size();
        Id depotId = scenario.createId(id + "");

        String name = atts.getValue("name");
        if (name == null) {
            name = "D_" + id;
        }

        Id linkId = scenario.createId(atts.getValue("linkId"));
        Link link = links.get(linkId);

        currentDepot = new DepotImpl(depotId, name, link);
        data.addDepot(currentDepot);
    }


    private void startVehicle(Attributes atts)
    {
        int id = data.getVehicles().size();
        Id vehicleId = scenario.createId(id + "");

        String name = atts.getValue("name");
        if (name == null) {
            name = "V_" + id;
        }

        int capacity = getInt(atts, "id", 1);

        int t0 = getInt(atts, "t0", 0);
        int t1 = getInt(atts, "t1", 24 * 60 * 60);
        int tLimit = getInt(atts, "tLimit", t1 - t0);

        data.addVehicle(new VrpAgentVehicleImpl(vehicleId, name, currentDepot, capacity, t0, t1,
                tLimit));
    }


    private int getInt(Attributes atts, String qName, int defaultValue)
    {
        String val = atts.getValue(qName);

        if (val != null) {
            return Integer.parseInt(val);
        }
        else {
            return defaultValue;
        }
    }
}
