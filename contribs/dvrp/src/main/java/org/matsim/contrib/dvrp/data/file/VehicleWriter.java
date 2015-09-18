package org.matsim.contrib.dvrp.data.file;

import java.util.*;

import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;


public class VehicleWriter
    extends MatsimXmlWriter
{
    private Iterable<Vehicle> vehicles;


    public VehicleWriter(Iterable<Vehicle> vehicles)
    {
        this.vehicles = vehicles;
    }


    public void write(String file)
    {
        openFile(file);
        writeDoctype("vehicles", "http://matsim.org/files/dtd/dvrp_vehicles_v1.dtd");
        writeStartTag("vehicles", Collections.<Tuple<String, String>>emptyList());
        writeVehicles();
        writeEndTag("vehicles");
        close();
    }


    private void writeVehicles()
    {
        for (Vehicle veh : vehicles) {
            List<Tuple<String, String>> atts = new ArrayList<>();
            atts.add(new Tuple<String, String>("id", veh.getId().toString()));
            atts.add(
                    new Tuple<String, String>("start_link", veh.getStartLink().getId().toString()));
            atts.add(new Tuple<String, String>("t_0", veh.getT0() + ""));
            atts.add(new Tuple<String, String>("t_1", veh.getT1() + ""));
            writeStartTag("vehicle", atts, true);
        }
    }
}
