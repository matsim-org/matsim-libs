package playground.sergioo.vehiclesGenerator2012;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.utils.CreateVehiclesForSchedule;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vehicles.VehicleWriterV1;
import org.xml.sax.SAXException;

public class VehiclesGenerator {

	/**
	 * @param args
	 * @throws ParserConfigurationException 
	 * @throws SAXException 
	 * @throws IOException 
	 */
	/*public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().scenario().setUseTransit(true);
		scenario.getConfig().scenario().setUseVehicles(true);
		new TransitScheduleReader(scenario).readFile(args[0]);
		new CreateVehiclesForSchedule(((ScenarioImpl)scenario).getTransitSchedule(), ((ScenarioImpl)scenario).getVehicles()).run();
		new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(args[0]);
		new VehicleWriterV1(((ScenarioImpl)scenario).getVehicles()).writeFile(args[1]);
	}*/
	
	public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().scenario().setUseTransit(true);
		scenario.getConfig().scenario().setUseVehicles(true);
		new TransitScheduleReader(scenario).readFile(args[0]);
		new VehicleReaderV1(scenario.getTransitVehicles()).readFile(args[1]);
		List<Vehicle> vehicles = new ArrayList<Vehicle>();
		for(Vehicle vehicle:scenario.getTransitVehicles().getVehicles().values())
			if(new Integer(vehicle.getType().getId().toString())<20)
				vehicles.add(vehicle);
		int i=0;
		for(TransitLine line:scenario.getTransitSchedule().getTransitLines().values())
			for(TransitRoute route:line.getRoutes().values())
				if(route.getTransportMode().equals("bus"))
					for(Departure departure:route.getDepartures().values())
						departure.setVehicleId(vehicles.get(i++).getId());
		new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(args[2]);
	}

}
