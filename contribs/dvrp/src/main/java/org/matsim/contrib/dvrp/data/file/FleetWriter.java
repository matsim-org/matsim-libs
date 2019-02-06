package org.matsim.contrib.dvrp.data.file;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;

/**
 * @author michalm
 */
public class FleetWriter extends MatsimXmlWriter {
	private Stream<? extends DvrpVehicleSpecification> vehicleSpecifications;

	public FleetWriter(Stream<? extends DvrpVehicleSpecification> vehicleSpecifications) {
		this.vehicleSpecifications = vehicleSpecifications;
	}

	public void write(String file) {
		openFile(file);
		writeDoctype("vehicles", "http://matsim.org/files/dtd/dvrp_vehicles_v1.dtd");
		writeStartTag("vehicles", Collections.emptyList());
		writeVehicles();
		writeEndTag("vehicles");
		close();
	}

	private void writeVehicles() {
		vehicleSpecifications.forEach(veh -> {
			List<Tuple<String, String>> atts = Arrays.asList(new Tuple<>("id", veh.getId().toString()),
					new Tuple<>("start_link", veh.getStartLinkId() + ""),
					new Tuple<>("t_0", veh.getServiceBeginTime() + ""),
					new Tuple<>("t_1", veh.getServiceEndTime() + ""), new Tuple<>("capacity", veh.getCapacity() + ""));
			writeStartTag("vehicle", atts, true);
		});
	}
}
