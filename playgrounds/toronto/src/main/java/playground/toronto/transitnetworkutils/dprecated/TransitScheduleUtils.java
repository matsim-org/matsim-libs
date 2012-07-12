package playground.toronto.transitnetworkutils.dprecated;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.TransitLineImpl;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.TransitScheduleImpl;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV1;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.pt.utils.CreateVehiclesForSchedule;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

public abstract class TransitScheduleUtils {

	public static void main(String[] args) throws IOException{
		
		if (args.length != 3) return;
		
		PostProcessGTFS(args[0], args[1], args[2]);
		
	}
	
	/**
	 * A method which takes the GTFS2MATSim output and post-processes the result. Aggregates 
	 * Departures across several trips into one route. Also create fake vehicles and exports
	 * them to the output folder.
	 * 
	 * It is assumed (but not asserted) that the link sequence for a given line.route stop
	 * sequence is that same for all line.routes with the same stop sequence. The stop offsets
	 * for a given stop sequence (branch) are taken from the first occurrence of this branch.
	 * 
	 * @param scheduleInFile
	 * @param GTFSfolder
	 * @param outFolder
	 * @throws IOException
	 * 
	 * @author pkucirek
	 */
	public static void PostProcessGTFS(String scheduleInFile, String GTFSfolder, String outFolder) throws IOException{
		
		TransitScheduleFactoryImpl builder = new TransitScheduleFactoryImpl();
		TransitScheduleImpl oldSchedule = (TransitScheduleImpl) builder.createTransitSchedule();
		
		
		//Read in the schedule .xml file
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		//public TransitScheduleReaderV1(final TransitSchedule schedule, final ModeRouteFactory routeFactory, final IdFactory idf)
		//new TransitScheduleReaderV1(scenario).readFile(scheduleInFile);
		//(TransitScheduleImpl) scenario.getTransitSchedule(); //=(TransitScheduleImpl) scenario.getTransitSchedule()
		
		TransitScheduleReaderV1 tsreader = new TransitScheduleReaderV1(oldSchedule, new ModeRouteFactory(), scenario);
		tsreader.readFile(scheduleInFile);
		
		//Read routes.txt
		HashMap<String, String> routeNameMap = new HashMap<String, String>();
		HashMap<String, String> routeDescrMap = new HashMap<String, String>();
		BufferedReader reader = new BufferedReader(new FileReader(GTFSfolder + "/routes.txt"));
		String header = reader.readLine();
		int rtCol = Arrays.asList(header.split(",")).indexOf("route_id");
		int nmCol = Arrays.asList(header.split(",")).indexOf("route_short_name");
		int dscCol = Arrays.asList(header.split(",")).indexOf("route_long_name");
		
		String line;
		while ((line = reader.readLine()) != null){
			String[] cells = line.split(",");
			String id = cells[rtCol];
			String name = cells[nmCol];
			String descr = cells[dscCol];
			
			routeNameMap.put(id, name);
			routeDescrMap.put(id, descr);
		}
		reader.close();
		
		//Read trips.txt
		HashMap<String, String> tripDirectionMap = new HashMap<String, String>();
		reader = new BufferedReader(new FileReader(GTFSfolder + "/trips.txt"));
		header = reader.readLine();
		int tpCol = Arrays.asList(header.split(",")).indexOf("trip_id");
		int dirCol = Arrays.asList(header.split(",")).indexOf("direction_id");
		while ((line = reader.readLine()) != null){
			String[] cells = line.split(",");
			String trip = cells[tpCol];
			String direction = cells[dirCol];
			
			tripDirectionMap.put(trip, direction);
		}
		reader.close();
		
		if (oldSchedule.getTransitLines().size() != routeNameMap.size()) {
			System.err.println("This function only works on GTFS file!");
			return;
		}
		
		TransitScheduleImpl newSchedule = (TransitScheduleImpl) builder.createTransitSchedule();
		for (TransitStopFacility S : oldSchedule.getFacilities().values()) newSchedule.addStopFacility(S);
		
		for (TransitLine L : oldSchedule.getTransitLines().values()){
			String oldId = L.getId().toString();
			IdImpl newId = new IdImpl(routeNameMap.get(oldId));
			
			HashMap<String, Id> stopSequences = new HashMap<String, Id>(); //StopSequence, branchId
			TransitLineImpl currentLine = (TransitLineImpl) builder.createTransitLine(newId);
			int currentBranch = 0;
			String currentDirection = "";
			
			
			//iterate through current trips
			for (TransitRoute R : L.getRoutes().values()){
				String tripId = R.getId().toString();
				if (!tripDirectionMap.containsKey(tripId)){
					System.err.println("WARN: Route " + tripId + " in line " + oldId + " was not recognized as a valid trip! Skipping.");
					continue;
				}
				
				String S = "";
				for (TransitRouteStop F : R.getStops()) S += F.getStopFacility().getId().toString() + ",";
				IdImpl branch;
				
				if (stopSequences.containsKey(S)){
					branch = (IdImpl) stopSequences.get(S);
				}
				else{
					//Create new route
					if (!tripDirectionMap.get(tripId).equals(currentDirection)){
						currentDirection = tripDirectionMap.get(tripId);
						currentBranch = 0;
					}
					branch = new IdImpl("Direction " + currentDirection + ", Branch " + currentBranch++);
					stopSequences.put(S, branch);
					currentLine.addRoute(builder.createTransitRoute(branch, R.getRoute(), R.getStops(), R.getTransportMode()));
					
				}
				
				if (R.getDepartures().size() > 1){
					System.err.println("Found multiple departures for route " + R.getId().toString() + " in line " + L.getId().toString() + ". Only the first departure will be retained.");
					
				}
				if (!R.getDepartures().containsKey(new IdImpl("1"))){
					System.err.println("Could not find departure 1 for route " + R.getId().toString() + " in line " + L.getId().toString() + "!");
				}
				
				for (Departure D : R.getDepartures().values()) {
					Departure d = builder.createDeparture(new IdImpl(currentLine.getRoutes().get(branch).getDepartures().size() + 1), D.getDepartureTime());
					currentLine.getRoutes().get(branch).addDeparture(d);
				}
				
		
			}
			
			newSchedule.addTransitLine(currentLine);
			System.out.println("Added line \"" + newId.toString() + "\" to schedule.");
		}
		
		Vehicles veh = VehicleUtils.createVehiclesContainer();
		CreateVehiclesForSchedule cvfs = new CreateVehiclesForSchedule(newSchedule, veh);
		cvfs.run();
		
		VehicleWriterV1 vw = new VehicleWriterV1(veh);
		vw.writeFile(outFolder + "/synthetic_vehicles.xml");
		
		TransitScheduleWriterV1 writer = new TransitScheduleWriterV1(newSchedule);
		writer.write(outFolder + "/new_schedule.xml");
		
	}
	
	
	
}
