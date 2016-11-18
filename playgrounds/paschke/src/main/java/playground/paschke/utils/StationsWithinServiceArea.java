package playground.paschke.utils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.carsharing.config.FreefloatingAreasReader;
import org.matsim.contrib.carsharing.qsim.FreefloatingAreas;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;

public class StationsWithinServiceArea {
	public static void main(String[] args) {
		Map<String, FreefloatingAreas> freefloatingAreas;
		Map<String, Map<CSVehicle, Coord>> stations;
		Map<String, Map<CSVehicle, Coord>> stationsWithinAreas = new HashMap<String, Map<CSVehicle, Coord>>();

		FreefloatingAreasReader ffAreasReader = new FreefloatingAreasReader();
		ffAreasReader.readFile(args[0]);
		freefloatingAreas = ffAreasReader.getFreefloatingAreas();

		StationsReader stationsReader = new StationsReader();
		stationsReader.readFile(args[1]);
		stations = stationsReader.getStations();

		Iterator<Entry<String, Map<CSVehicle, Coord>>> iterator = stations.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, Map<CSVehicle, Coord>> companyStations = iterator.next();

			String companyName = companyStations.getKey();
			FreefloatingAreas companyAreas = freefloatingAreas.get(companyName);

			Map<CSVehicle, Coord> companyStationsWithinAreas = new HashMap<CSVehicle, Coord>();
			stationsWithinAreas.put(companyName, companyStationsWithinAreas);

			Iterator<Entry<CSVehicle, Coord>> companyIterator = companyStations.getValue().entrySet().iterator();
			while (companyIterator.hasNext()) {
				Entry<CSVehicle, Coord> station = companyIterator.next();

				if (companyAreas.contains(station.getValue())) {
					stationsWithinAreas.get(companyName).put(station.getKey(), station.getValue());
				}
			}
		}

		StationsWriter stationsWriter = new StationsWriter(stationsWithinAreas);
		stationsWriter.writeFile(args[2]);
	}
}
