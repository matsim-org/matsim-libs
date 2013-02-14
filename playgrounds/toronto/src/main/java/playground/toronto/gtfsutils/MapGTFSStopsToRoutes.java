package playground.toronto.gtfsutils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap; 
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MapGTFSStopsToRoutes {

	/**
	 * Produces, for each stop, a list of routes which use it.
	 * 
	 * @param args	[0] = folder location of GTFS files
	 * 				[1] = folder location to export.
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException
	{
		if(args.length != 2) {
			System.out.println("Incorrect arguments!");
			return;
		}
		
		//Build the route-mode mapping
		HashMap<String, String> routeModeMap = new HashMap<String, String>();
		BufferedReader reader = new BufferedReader(new FileReader(args[0] + "/routes.txt"));
		String header = reader.readLine();
		int rtCol = Arrays.asList(header.split(",")).indexOf("route_id");
		int modCol = Arrays.asList(header.split(",")).indexOf("route_type");
		
		String line;
		while ((line = reader.readLine()) != null){
			String[] cells = line.split(",");
			String routeId = cells[rtCol];
			int mode = Integer.parseInt(cells[modCol]);
			String newMode = "";
			
			switch (mode) {
			case 0: newMode = "Streetcar"; break;
			case 1: newMode = "Subway"; break;
			case 2: newMode = "Train"; break;
			case 3: newMode = "Bus"; break;
			default: 
				System.err.println("Error: cannot find a match for mode " + mode); break;
			}
			
			routeModeMap.put(routeId, newMode);
		}
		
		//Build the trip-route mapping
		HashMap<String,String> tripRouteMap = new HashMap<String, String>();
		reader = new BufferedReader(new FileReader(args[0] + "/trips.txt"));
		header = reader.readLine();
		int tpCol = Arrays.asList(header.split(",")).indexOf("trip_id");
		rtCol = Arrays.asList(header.split(",")).indexOf("route_id");
		
		while ((line = reader.readLine()) != null){
			String[] cells = line.split(",");
			String tripId = cells[tpCol];
			String routeId = cells[rtCol];
			if(tripRouteMap.containsKey(tripId)){
				System.err.println("Warning: Trip " + tripId + " found more than once! Skipping.");
				continue;
			}
			
			tripRouteMap.put(tripId, routeId);
		}
		
		reader.close();
		
		
		//Key: stop_id, Object: List of routes.
		HashMap<String,ArrayList<String>> stopRoutesMap = new HashMap<String, ArrayList<String>>();	
		reader = new BufferedReader(new FileReader(args[0] + "/stop_times.txt"));
		header = reader.readLine();
		tpCol = Arrays.asList(header.split(",")).indexOf("trip_id");
		int stpCol = Arrays.asList(header.split(",")).indexOf("stop_id");
		while((line = reader.readLine()) != null){
			String[] cells = line.split(",");
			String tripId = cells[tpCol];
			String stopId = cells[stpCol];
			
			if(stopRoutesMap.containsKey(stopId)){
				ArrayList<String> a = stopRoutesMap.get(stopId);
				String routeId = tripRouteMap.get(tripId);
				if(!a.contains(routeId)){
					a.add(routeId);
					stopRoutesMap.put(stopId, a);
				}
			}
			else{
				ArrayList<String> a = new ArrayList<String>();
				a.add(tripRouteMap.get(tripId));
				stopRoutesMap.put(stopId, a);
			}
		}
		
		reader.close();
		

		
		//Reproduces the stops.txt file and appends a list of the modes served.
		reader = new BufferedReader(new FileReader(args[0] + "/stops.txt"));
		header = reader.readLine();
		stpCol = Arrays.asList(header.split(",")).indexOf("stop_id");
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(args[1] + "/newstops.txt"));
		writer.write(header + ",stop_modes");
		while((line = reader.readLine()) != null){
			List<String> cells = Arrays.asList(line.split(","));
			String id = cells.get(stpCol);
			
			HashSet<String> modesServed = new HashSet<String>();
			
			ArrayList<String> routes = stopRoutesMap.get(id);
			if (routes == null){
				System.err.println("Could not find data for stop id " + id);
				continue;
			}
			for (String r : routes) {
				String x = routeModeMap.get(r);
				if (x == null){
					System.err.println("Could not find mode data for route " + r);
				}
				modesServed.add(x);
			}
				
			
			
			writer.write("\n" + line);
			writer.write("," + modesServed.toString().replace(",",";"));
			/*
			writer.write(",\"" + modesServed.);
			for(int j = 1; j < modesServed.size(); j++) writer.write("," + modesServed.get(j));
			writer.write("\"");*/
		}
		writer.close();
		
		writer = new BufferedWriter(new FileWriter(args[1] + "/stop_route_mapping.txt"));
		Iterator i = stopRoutesMap.entrySet().iterator();
		while(i.hasNext()){
			Map.Entry e = (Map.Entry)i.next();
			writer.write("\n" + e.getKey() + ":" + e.getValue().toString());
			i.remove();
		}
		writer.close();
		
		
	}
}
