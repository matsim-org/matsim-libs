package playground.michalm.vrp.data.file;

import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.core.utils.io.*;
import org.xml.sax.*;

import pl.poznan.put.vrp.dynamic.data.*;
import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.network.*;
import playground.michalm.vrp.data.*;
import playground.michalm.vrp.data.network.*;


public class DepotReader
    extends MatsimXmlParser
{
    private final static String DEPOT = "depot";
    private final static String VEHICLE = "vehicle";

    private Scenario scenario;
    private MATSimVRPData data;
    private MATSimVRPGraph graph;

    private List<Depot> depots = new ArrayList<Depot>();
    private List<Vehicle> vehicles = new ArrayList<Vehicle>();

    private Depot currentDepot;


    public DepotReader(Scenario scenario, MATSimVRPData data)
    {
        this.scenario = scenario;
        this.data = data;

        graph = data.getVrpGraph();
    }


    public void readFile(String filename)
    {
        parse(filename);

        VRPData vrpData = data.getVrpData();
        vrpData.setDepots(depots);
        vrpData.setVehicles(vehicles);
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
    }


    private void startVehicle(Attributes atts)
    {
        int id = vehicles.size();

        String name = atts.getValue("name");
        if (name == null) {
            name = "D_" + id;
        }

        int capacity = getInt(atts, "id", 1);

        double cost = getDouble(atts, "cost", 0);

        int t0 = getInt(atts, "t0", 0);
        int t1 = getInt(atts, "t1", 86400); // default: 24 * 3600
        int tLimit = getInt(atts, "tLimit", 86400); // default: 24 * 3600

        vehicles.add(new VehicleImpl(id, name, currentDepot, capacity, cost, t0, t1, tLimit));
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
}
