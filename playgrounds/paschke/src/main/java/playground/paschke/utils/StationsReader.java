package playground.paschke.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;
import org.matsim.contrib.carsharing.vehicles.FFVehicleImpl;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

public class StationsReader extends MatsimXmlParser {

	private String companyName;
	private Map<String, Map<CSVehicle, Coord>> stations = new HashMap<String, Map<CSVehicle, Coord>>();

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if (name.equals("company")) {
			this.companyName = atts.getValue("name");

			Map<CSVehicle, Coord> companyStations = new HashMap<CSVehicle, Coord>();
			this.stations.put(this.companyName, companyStations);
		} else if (name.equals("freefloating")) {
			String x = atts.getValue("x");
			String y = atts.getValue("y");
			String id = atts.getValue("id");
			String type = atts.getValue("type");

			Coord stationCoord = new Coord(Double.parseDouble(x), Double.parseDouble(y));
			FFVehicleImpl vehicle = new FFVehicleImpl(type, id, this.companyName);
			this.stations.get(this.companyName).put(vehicle, stationCoord);
		}
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
		// TODO Auto-generated method stub
	}

	public Map<String, Map<CSVehicle, Coord>> getStations() {
		return this.stations;
	}
}
