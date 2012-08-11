package playground.toronto.analysis;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.toronto.analysis.handlers.VehicleBoardingHandler;



public class AnalyzeTransitRouteBoardings {

	public static void main(String[] args) throws IOException {
		
		String eventsFile = args[0];
		String scheduleFile = args[1];
		//String vehiclesFile = args[2];
		String exportFolder = args[2];
		
		EventsManager em = EventsUtils.createEventsManager();
		
		VehicleBoardingHandler vbh = new VehicleBoardingHandler();
		em.addHandler(vbh);
		
		MatsimEventsReader reader = new MatsimEventsReader(em);
		reader.readFile(eventsFile);
		
		Config config = ConfigUtils.createConfig();
		config.setParam("scenario", "useTransit", "true");
		config.setParam("transit", "transitScheduleFile", scheduleFile);
		//config.setParam("transit", "vehiclesFile", vehiclesFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		TransitSchedule schedule = scenario.getTransitSchedule();
		
		//BufferedWriter bw = new BufferedWriter(new FileWriter(exportFolder));
		//bw.write("lineId;routeId;[arrival1,arrival2,arrival3...]");
		
		//Assumes each vehicle is referenced to only one departure
		for (TransitLine line : schedule.getTransitLines().values()){
			
			BufferedWriter bw = new BufferedWriter(new FileWriter(exportFolder + "/" + line.getId() + ".txt"));
			ArrayList<ArrayList<Double>> arrivalTable = new ArrayList<ArrayList<Double>>();
			
			int maxArrivals = 0;
			for (TransitRoute route : line.getRoutes().values()){
				bw.write(route.getId() + "\t");
				
				ArrayList<Double> arrivals = new ArrayList<Double>();
				for (Departure dep : route.getDepartures().values()){
					Id vehicleId = dep.getVehicleId();
					for (Double d : vbh.getBaordingsForVehicle(vehicleId)) arrivals.add(d);
				}
				Collections.sort(arrivals); 
				if (arrivals.size() > maxArrivals) maxArrivals = arrivals.size();
				
				arrivalTable.add(arrivals);
			}
			
			for (int row = 0; row < maxArrivals; row++){
				bw.newLine();
				for (int col = 0; col < arrivalTable.size(); col++){
					ArrayList<Double> arrivals = arrivalTable.get(col);
					if (row < arrivals.size()) bw.write(Time.writeTime(arrivals.get(row)));
					bw.write("\t");
				}
			}
			bw.close();
			
			System.out.println("Exported line " + line.getId());
		}
	}

}
