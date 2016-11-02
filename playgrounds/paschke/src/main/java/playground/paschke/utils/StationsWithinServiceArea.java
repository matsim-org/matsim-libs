package playground.paschke.utils;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.carsharing.config.FreefloatingAreasReader;
import org.matsim.contrib.carsharing.qsim.FreefloatingAreas;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;

public class StationsWithinServiceArea {
	public static void main(String[] args) {
		Map<String, FreefloatingAreas> freefloatingAreas;
		Map<String, Map<CSVehicle, Coord>> stations = new HashMap<String, Map<CSVehicle, Coord>>();

		FreefloatingAreasReader ffAreasReader = new FreefloatingAreasReader();
		ffAreasReader.readFile(args[0]);
		freefloatingAreas = ffAreasReader.getFreefloatingAreas();

		StationsReader stationsReader = new StationsReader();
		stationsReader.readFile(args[1]);
		stations = stationsReader.getStations();

		StationsWriter stationsWriter = new StationsWriter(stations);
		stationsWriter.writeFile(args[2]);
	}
}
