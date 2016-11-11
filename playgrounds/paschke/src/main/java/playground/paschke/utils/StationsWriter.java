package playground.paschke.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;

public class StationsWriter extends MatsimXmlWriter {
	private Map<String, Map<CSVehicle, Coord>> stations = new HashMap<String, Map<CSVehicle, Coord>>();

	public StationsWriter(Map<String, Map<CSVehicle, Coord>> stations) {
		this.stations = stations;
	}

	public void writeFile(String filename) {
		this.openFile(filename);
		this.writeXmlHead();
		this.writeDoctype("companies", "src/main/resources/dtd/carsharing_stations.dtd");

		this.writeStartTag("companies", Collections.<Tuple<String, String>>emptyList());

		Iterator<Entry<String, Map<CSVehicle, Coord>>> iterator = stations.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, Map<CSVehicle, Coord>> companyStations = iterator.next();

			this.writeStartTag("company", Arrays.asList(createTuple("name", companyStations.getKey())));

			Iterator<Entry<CSVehicle, Coord>> companyIterator = companyStations.getValue().entrySet().iterator();
			while (companyIterator.hasNext()) {
				Entry<CSVehicle, Coord> station = companyIterator.next();
				this.writeStartTag("freefloating", Arrays.asList(
						createTuple("id", station.getKey().getVehicleId().toString()),
						createTuple("x", Math.floor(station.getValue().getX())),
						createTuple("y", Math.floor(station.getValue().getY())),
						createTuple("type", "car")
				), true);
			}

			this.writeEndTag("company");
		}

		this.writeEndTag("companies");
		this.close();
	}
}
