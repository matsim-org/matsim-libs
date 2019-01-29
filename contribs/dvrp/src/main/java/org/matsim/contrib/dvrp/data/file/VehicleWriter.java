package org.matsim.contrib.dvrp.data.file;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;

/**
 * @author michalm
 */
public class VehicleWriter extends MatsimXmlWriter {
	private Iterable<? extends Vehicle> vehicles;

	public VehicleWriter(Iterable<? extends Vehicle> vehicles) {
		this.vehicles = vehicles;
	}

	public void write(String file) {
		openFile(file);
		writeDoctype("vehicles", "http://matsim.org/files/dtd/dvrp_vehicles_v1.dtd");
		writeStartTag("vehicles", Collections.<Tuple<String, String>> emptyList());
		writeVehicles();
		writeEndTag("vehicles");
		close();
	}

	private void writeVehicles() {
		for (Vehicle veh : vehicles) {
			List<Tuple<String, String>> atts = Arrays.asList(
					new Tuple<>("id", veh.getId().toString()),
					new Tuple<>("start_link", veh.getStartLink().getId().toString()),
					new Tuple<>("t_0", veh.getServiceBeginTime() + ""),
					new Tuple<>("t_1", veh.getServiceEndTime() + ""),
					new Tuple<>("capacity", veh.getCapacity() + ""));
			writeStartTag("vehicle", atts, true);
		}
	}
}
