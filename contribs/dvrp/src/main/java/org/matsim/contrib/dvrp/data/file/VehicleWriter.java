package org.matsim.contrib.dvrp.data.file;

import java.util.*;

import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;


public class VehicleWriter
    extends MatsimXmlWriter
{
    private List<Vehicle> vehicles;


    public VehicleWriter(List<Vehicle> vehicles)
    {
        this.vehicles = vehicles;
    }


    public void write(String fileName)
    {
        this.openFile(fileName);
        this.writeDoctype("vehicles", "http://matsim.org/files/dtd/vehicles_v1.dtd");
        this.writeStartTag("vehicles", Collections.<Tuple<String, String>>emptyList());
        this.writeVehicles();
        this.writeEndTag("vehicles");
    }


    private void writeVehicles()
    {
        for (Vehicle veh : vehicles) {
            List<Tuple<String, String>> atts = new ArrayList<Tuple<String, String>>();
            atts.add(new Tuple<String, String>("id", veh.getId().toString()));
            atts.add(new Tuple<String, String>("start_link", veh.getStartLink().getId().toString()));
            atts.add(new Tuple<String, String>("t_0", veh.getT0() + ""));
            atts.add(new Tuple<String, String>("t_1", veh.getT1() + ""));
            this.writeStartTag("vehicle", atts, true);
        }
    }
}
