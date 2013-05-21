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

package playground.michalm.vrp.data.file;

import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.network.Vertex;
import playground.michalm.vrp.data.model.DynAgentVehicle;
import playground.michalm.vrp.data.network.MatsimVrpGraph;


public class DepotReader
    extends MatsimXmlParser
{
    private final static String DEPOT = "depot";
    private final static String VEHICLE = "vehicle";

    private Scenario scenario;
    private VrpData data;
    private MatsimVrpGraph graph;

    private List<Depot> depots = new ArrayList<Depot>();
    private List<Id> depotLinks = new ArrayList<Id>();
    private List<Vehicle> vehicles = new ArrayList<Vehicle>();

    private Depot currentDepot;


    public DepotReader(Scenario scenario, VrpData data)
    {
        this.scenario = scenario;
        this.data = data;

        graph = (MatsimVrpGraph)data.getVrpGraph();
    }


    public void readFile(String filename)
    {
        parse(filename);

        data.setDepots(depots);
        data.setVehicles(vehicles);
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
        int id = depots.size();

        String name = atts.getValue("name");
        if (name == null) {
            name = "D_" + id;
        }

        Id linkId = scenario.createId(atts.getValue("linkId"));
        Vertex vertex = graph.getVertex(linkId);

        currentDepot = new DepotImpl(id, name, vertex);
        depots.add(currentDepot);
        depotLinks.add(linkId);
    }


    private void startVehicle(Attributes atts)
    {
        int id = vehicles.size();

        String name = atts.getValue("name");
        if (name == null) {
            name = "V_" + id;
        }

        int capacity = getInt(atts, "id", 1);

        double cost = getDouble(atts, "cost", 0);

        int t0 = getInt(atts, "t0", 0);
        int t1 = getInt(atts, "t1", 24 * 60 * 60);
        int tLimit = getInt(atts, "tLimit", t1 - t0);

        vehicles.add(new DynAgentVehicle(id, name, currentDepot, capacity, cost, t0, t1, tLimit));
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


    private double getDouble(Attributes atts, String qName, double defaultValue)
    {
        String val = atts.getValue(qName);

        if (val != null) {
            return Double.parseDouble(val);
        }
        else {
            return defaultValue;
        }
    }


	public List<Id> getDepotLinks() {
		return depotLinks;
	}
    
}
