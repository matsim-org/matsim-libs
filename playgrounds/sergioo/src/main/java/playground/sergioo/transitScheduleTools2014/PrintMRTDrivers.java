package playground.sergioo.transitScheduleTools2014;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.vehicles.VehicleReaderV1;

public class PrintMRTDrivers {
	public static void main(String[] args) throws FileNotFoundException {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().scenario().setUseTransit(true);
		scenario.getConfig().scenario().setUseVehicles(true);
		new TransitScheduleReader(scenario).readFile(args[0]);
		new VehicleReaderV1(scenario.getTransitVehicles()).readFile(args[1]);
		PrintWriter printWriter = new PrintWriter(args[2]);
		for(TransitLine transitLine:scenario.getTransitSchedule().getTransitLines().values())
			for(TransitRoute transitRoute:transitLine.getRoutes().values())
				if(transitRoute.getTransportMode().contains(args[3]))
					for(Departure departure:transitRoute.getDepartures().values())
						printWriter.println("pt_"+departure.getVehicleId()+"_"+scenario.getTransitVehicles().getVehicles().get(departure.getVehicleId()).getType().getId());
		printWriter.close();
	}
}
