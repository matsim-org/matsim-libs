package playground.sergioo.vehiclesGenerator;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.utils.CreateVehiclesForSchedule;
import org.matsim.vehicles.VehicleWriterV1;
import org.xml.sax.SAXException;

public class VehiclesGenerator {

	/**
	 * @param args
	 * @throws ParserConfigurationException 
	 * @throws SAXException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().scenario().setUseTransit(true);
		scenario.getConfig().scenario().setUseVehicles(true);
		TransitScheduleReader reader = new TransitScheduleReader(scenario);
		reader.readFile("./data/gtfs/test.xml");
		CreateVehiclesForSchedule cvfs = new CreateVehiclesForSchedule(((ScenarioImpl)scenario).getTransitSchedule(), ((ScenarioImpl)scenario).getVehicles());
		cvfs.run();
		TransitScheduleWriter writer= new TransitScheduleWriter(((ScenarioImpl)scenario).getTransitSchedule());
		writer.writeFile("./data/ezLinkDataSimulation/transitScheduleWV.xml");
		VehicleWriterV1 writer2 = new VehicleWriterV1(((ScenarioImpl)scenario).getVehicles());
		writer2.writeFile("./data/ezLinkDataSimulation/vehicles.xml");
		
	}

}
