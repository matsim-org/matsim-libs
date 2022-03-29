package org.matsim.contrib.shared_mobility.io;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;

public class SharingServiceWriter extends MatsimXmlWriter {
	private final SharingServiceSpecification specification;

	public SharingServiceWriter(SharingServiceSpecification specification) {
		this.specification = specification;
	}

	public void write(String file) {
		openFile(file);
		writeXmlHead();
		writeDoctype("service", "src/main/resources/dtd/sharing_service_v1.dtd");

		writeStartTag("service", Collections.emptyList());

		writeStartTag("stations", Collections.emptyList());
		specification.getStations().forEach(this::writeStation);
		writeEndTag("stations");

		writeStartTag("vehicles", Collections.emptyList());
		specification.getVehicles().forEach(this::writeVehicle);
		writeEndTag("vehicles");

		writeEndTag("service");

		close();
	}

	private synchronized void writeStation(SharingStationSpecification station) {
		List<Tuple<String, String>> attributes = Arrays.asList( //
				Tuple.of("id", station.getId().toString()), //
				Tuple.of("link", station.getLinkId().toString()), //
				Tuple.of("capacity", String.valueOf(station.getCapacity())) //
		);
		writeStartTag("station", attributes, true);
	}

	private synchronized void writeVehicle(SharingVehicleSpecification vehicle) {
		List<Tuple<String, String>> attributes = new ArrayList<>(Arrays.asList( //
				Tuple.of("id", vehicle.getId().toString())));

		if (vehicle.getStartLinkId().isPresent()) {
			attributes.add(Tuple.of("startLink", vehicle.getStartLinkId().get().toString()));
		}

		if (vehicle.getStartStationId().isPresent()) {
			attributes.add(Tuple.of("startStation", vehicle.getStartStationId().get().toString()));
		}

		writeStartTag("vehicle", attributes, true);
	}
}
