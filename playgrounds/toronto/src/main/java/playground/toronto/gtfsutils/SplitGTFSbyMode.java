package playground.toronto.gtfsutils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;

public class SplitGTFSbyMode {

	/**
	 * Splits the routes.txt, trips.txt, and stop_times.txt by mode. Also restricts by service level
	 * 
	 * @param args - Space-separated array of arguments:
	 * 		[0] - Folder location of the base files
	 * 		[1] - Folder location to store STREETCAR/TRAM output
	 * 		[2] - Folder location to store SUBWAY/METRO output
	 * 		[3] - Folder location to store TRAIN output
	 * 		[4] - Folder location to store BUS output
	 * 		[5] - Service Id to extract.
	 * 
	 */
	public static void main(final String[] args) throws IOException, ParseException{
		
		if (args.length != 6){
			System.out.println("Usage: splitFilesByMode baseFolder tramFolder metroFolder trainFolder busFolder serviceId\n" +
					"	baseFolder = Folder location of base files\n" +
					"	tramFolder = Folder location to save STREETCAR/TRAM output\n" +
					"	metroFolder = Folder location to save SUBWAY/METRO output\n" +
					"	trainFolder = Folder location to save TRAIN output\n" +
					"	busFolder = Folder location to save BUS output\n" + 
					"   serviceId = The service schedule to extract");
			System.exit(-1);
		}
		
		String routesFile = args[0].replace("\\", "/") + "/routes.txt";
		String tripsFiles = args[0].replace("\\", "/") + "/trips.txt";
		String stoptimesFile = args[0].replace("\\", "/") + "/stop_times.txt";
		String stopsFile = args[0].replace("\\", "/") + "/stops.txt";
		String service = args[5];
		
		HashMap<String, Integer> routeIdModeMap = new HashMap<String, Integer>();
		
		//Start with the routes.txt file
		BufferedReader reader = new BufferedReader(new FileReader(routesFile));
		String header = reader.readLine();
		int modeColumn = Arrays.asList(header.split(",")).indexOf("route_type");
		int idColumn = Arrays.asList(header.split(",")).indexOf("route_id");
		
		HashMap<Integer, BufferedWriter> writers = new HashMap<Integer, BufferedWriter>();
		writers.put(0, new BufferedWriter(new FileWriter(args[1].replace("\\", "/") + "/routes.txt"))); writers.get(0).write(header);
		writers.put(1, new BufferedWriter(new FileWriter(args[2].replace("\\", "/") + "/routes.txt"))); writers.get(1).write(header);
		writers.put(2, new BufferedWriter(new FileWriter(args[3].replace("\\", "/") + "/routes.txt"))); writers.get(2).write(header);
		writers.put(3, new BufferedWriter(new FileWriter(args[4].replace("\\", "/") + "/routes.txt"))); writers.get(3).write(header);
		
		String line;
		while ((line = reader.readLine()) != null){
			String[] cells = line.split(",");
			Integer currentMode = Integer.parseInt(cells[modeColumn]);
			String currentId = cells[idColumn];
			
			routeIdModeMap.put(currentId, currentMode);
			
			writers.get(currentMode).write("\n" + line);		
		}
		
		for (int i = 0; i < 4; i++) writers.get(i).close();
		
		//Move on to the trips.txt file
		reader = new BufferedReader(new FileReader(tripsFiles));
		header = reader.readLine();
		int rtCol = Arrays.asList(header.split(",")).indexOf("route_id");
		int tpCol = Arrays.asList(header.split(",")).indexOf("trip_id");
		int svCol = Arrays.asList(header.split(",")).indexOf("service_id");
		
		HashMap<String, Integer> tripIdModeMap = new HashMap<String, Integer>();
		HashMap<String, Boolean> skippedTrips = new HashMap<String, Boolean>(); 
		
		writers = new HashMap<Integer, BufferedWriter>();
		writers.put(0, new BufferedWriter(new FileWriter(args[1].replace("\\", "/") + "/trips.txt"))); writers.get(0).write(header);
		writers.put(1, new BufferedWriter(new FileWriter(args[2].replace("\\", "/") + "/trips.txt"))); writers.get(1).write(header);
		writers.put(2, new BufferedWriter(new FileWriter(args[3].replace("\\", "/") + "/trips.txt"))); writers.get(2).write(header);
		writers.put(3, new BufferedWriter(new FileWriter(args[4].replace("\\", "/") + "/trips.txt"))); writers.get(3).write(header);
		
		while ((line = reader.readLine()) != null){
			String[] cells = line.split(",");
			String sv = cells[svCol];
			String tpId = cells[tpCol];
			String rtId = cells[rtCol];
			if (!sv.equals(service)){
				skippedTrips.put(tpId, true);
				continue; //skips trips that aren't in the specified service schedule.
			}
			skippedTrips.put(tpId, false);

			Integer currentMode = routeIdModeMap.get(rtId);
			if(currentMode == null){
				System.err.println("Could not find mode for route " + rtId);
				continue;
			}
			
			writers.get(currentMode).write("\n" + line);
			
			tripIdModeMap.put(tpId, currentMode);
		}
		
		for (int i = 0; i < 4; i++) writers.get(i).close();
		
		//Finally, the stop_times.txt file
		reader = new BufferedReader(new FileReader(stoptimesFile));
		header = reader.readLine();
		tpCol = Arrays.asList(header.split(",")).indexOf("trip_id");
		long counter = 0;
		System.out.print("Reading " + stoptimesFile + "...");
		
		writers = new HashMap<Integer, BufferedWriter>();
		writers.put(0, new BufferedWriter(new FileWriter(args[1].replace("\\", "/") + "/stop_times.txt"))); writers.get(0).write(header);
		writers.put(1, new BufferedWriter(new FileWriter(args[2].replace("\\", "/") + "/stop_times.txt"))); writers.get(1).write(header);
		writers.put(2, new BufferedWriter(new FileWriter(args[3].replace("\\", "/") + "/stop_times.txt"))); writers.get(2).write(header);
		writers.put(3, new BufferedWriter(new FileWriter(args[4].replace("\\", "/") + "/stop_times.txt"))); writers.get(3).write(header);
		
		while ((line = reader.readLine()) != null){
			String[] cells = line.split(",");
			String tpId = cells[tpCol];
			if (++counter % 5000 == 0) System.out.print("\n" + counter + ": " + tpId);
			if (skippedTrips.get(tpId)) continue;			
			Integer currentMode = tripIdModeMap.get(tpId);
			if(currentMode == null){
				//if(!skippedTrips.contains(tpId)) 
					System.err.println("Could not find mode for trip " + tpId);
				continue;
			}
			
			writers.get(currentMode).write("\n" + line);
		}
		
		for (int i = 0; i < 4; i++) writers.get(i).close();
		
	}
	
	
}
