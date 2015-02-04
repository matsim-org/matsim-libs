package playground.dhosse.prt.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.accessibility.CSVWriter;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.data.VrpData;
import org.matsim.contrib.dvrp.data.VrpDataImpl;
import org.matsim.contrib.dvrp.data.file.VehicleReader;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.vehicles.VehicleReaderV1;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;

import playground.michalm.taxi.data.file.ElectricVehicleReader;
import playground.vsp.analysis.modules.ptDriverPrefix.PtDriverIdAnalyzer;
import playground.vsp.analysis.modules.waitingTimes.WaitingTimeHandler;

public class PrtAnalyzer {
	
	public static void main(String args[]){
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().scenario().setUseTransit(true);
		scenario.getConfig().scenario().setUseVehicles(true);
		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario);
		netReader.readFile("C:/Users/Daniel/Desktop/dvrp/cottbus_scenario/network_prt.xml");
		PopulationReader popReader = new MatsimPopulationReader(scenario);
		popReader.readFile("C:/Users/Daniel/Desktop/dvrp/cottbus_scenario/population_miv_pt2.xml");
		TransitScheduleReader scheduleReader = new TransitScheduleReader(scenario);
		scheduleReader.readFile("C:/Users/Daniel/Desktop/dvrp/cottbus_scenario/schedule.xml");
		VehicleReaderV1 vReader = new VehicleReaderV1(scenario.getVehicles());
		vReader.readFile("C:/Users/Daniel/Desktop/dvrp/cottbus_scenario/transitVehicles.xml");
		VrpData data = new VrpDataImpl();
		ElectricVehicleReader vehReader = new ElectricVehicleReader(scenario, data);
		vehReader.parse("C:/Users/Daniel/Desktop/dvrp/cottbus_scenario/vehicles.xml");
		
		List<Id<Vehicle>> vehicleIds = new ArrayList<Id<Vehicle>>();
		for(Vehicle veh : data.getVehicles()){
			vehicleIds.add(veh.getId());
		}
		
		for(org.matsim.vehicles.Vehicle veh : scenario.getVehicles().getVehicles().values()){
			Id<Vehicle> vehicleId = Id.create(veh.getId().toString(), Vehicle.class);
			vehicleIds.add(vehicleId);
		}
		
		ShapeFileReader shpReader = new ShapeFileReader();
		Collection<SimpleFeature> features = shpReader.readFileAndInitialize("C:/Users/Daniel/Documents/Masterarbeit/"
				+ "inputFiles/admin_level_6/admin_level_6.shp");
		Geometry cottbus = null;
		for(SimpleFeature feature : features){
			if(feature.getAttribute("name").equals("Cottbus, Stadt")){
				cottbus = (Geometry)feature.getDefaultGeometry();
			}
		}
		
		EventsManager events = EventsUtils.createEventsManager();
		PrtEventsHandler handler = new PrtEventsHandler(vehicleIds, cottbus);
		events.addHandler(handler);
		
		String sc = "base";
//		String sc = "NOS";
//		String sc = "4persons";
//		String sc = "15persons";
		
		MatsimEventsReader reader = new MatsimEventsReader(events);
//		reader.readFile("C:/Users/Daniel/Desktop/dvrp/results/base/pt.0.events.xml.gz");
		reader.readFile("C:/Users/Daniel/Desktop/dvrp/results/NOS/events.xml");
//		reader.readFile("C:/Users/Daniel/Desktop/dvrp/results/4persons/events.xml");
//		reader.readFile("C:/Users/Daniel/Desktop/dvrp/results/15persons/events.xml");
		
		BufferedWriter writer = IOUtils.getBufferedWriter("C:/Users/Daniel/Desktop/dvrp/results/" + sc + "/stats.csv");
		
		try {
			
			writer.write("ttime_iv\n");
			
			for(Id<Person> d : handler.iv.keySet()){
				writer.write(d.toString() + ";" + (handler.iv.get(d)[1] - handler.iv.get(d)[0]) + "\n");
				writer.write(d.toString() + ";" + (handler.iv.get(d)[3] - handler.iv.get(d)[2]) + "\n");
			}
			
			writer.flush();
			writer.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		writer = IOUtils.getBufferedWriter("C:/Users/Daniel/Desktop/dvrp/results/" + sc + "/stats_pt.csv");
		
		try {
			
			writer.write("ttime_pt\n");
			
			for(Id<Person> d : handler.pt.keySet()){
				writer.write(d.toString() + ";" + (handler.pt.get(d)[1] - handler.pt.get(d)[0]) + "\n");
				writer.write(d.toString() + ";" + (handler.pt.get(d)[3] - handler.pt.get(d)[2]) + "\n");
			}
			
			writer.flush();
			writer.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		writer = IOUtils.getBufferedWriter("C:/Users/Daniel/Desktop/dvrp/results/" + sc + "/wait_pt.csv");
		
		try {
			
			writer.write("wtime_pt\n");
			
			for(Double d : handler.waitingTimesPt){
				writer.write(d + "\n");
			}
			
			writer.flush();
			writer.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		double ttimesIV = 0;
		double ttimesPT = 0;
		double minIV = Double.MAX_VALUE;
		double maxIV = 0;
		double minPT = Double.MAX_VALUE;
		double maxPT = 0;
		
		for(Id<Person> personId : handler.iv.keySet()){
			double t0 = handler.iv.get(personId)[1] - handler.iv.get(personId)[0];
			double t1 = handler.iv.get(personId)[3] -handler.iv.get(personId)[2];
			if(t0 < minIV) minIV = t0;
			if(t0 > maxIV) maxIV = t0;
			if(t1 < minIV) minIV = t1;
			if(t1 > maxIV) maxIV = t1;
			ttimesIV += t1 + t0;
		}
		
		for(Id<Person> personId : handler.pt.keySet()){
			double t0 = handler.pt.get(personId)[1] - handler.pt.get(personId)[0];
			double t1 = handler.pt.get(personId)[3] -handler.pt.get(personId)[2];
			if(t0 < minPT) minPT = t0;
			if(t0 > maxPT) maxPT = t0;
			if(t1 < minPT) minPT = t1;
			if(t1 > maxPT) maxPT = t1;
			ttimesPT += t1 + t0;
		}
		
		System.out.println("mean duration iv: " + Time.writeTime((0.5*ttimesIV/handler.iv.size()),Time.TIMEFORMAT_HHMMSS) + " [s]");
		System.out.println("max duration iv: " + Time.writeTime((maxIV),Time.TIMEFORMAT_HHMMSS) + " [s]");
		System.out.println("min duration iv: " + Time.writeTime((minIV),Time.TIMEFORMAT_HHMMSS) + " [s]");
		System.out.println("mean duration pt: " + Time.writeTime((0.5*ttimesPT/handler.pt.size()),Time.TIMEFORMAT_HHMMSS) + " [s]");
		System.out.println("max duration pt: " + Time.writeTime((maxPT),Time.TIMEFORMAT_HHMMSS) + " [s]");
		System.out.println("min duration pt: " + Time.writeTime((minPT),Time.TIMEFORMAT_HHMMSS) + " [s]");
		
	}

}
