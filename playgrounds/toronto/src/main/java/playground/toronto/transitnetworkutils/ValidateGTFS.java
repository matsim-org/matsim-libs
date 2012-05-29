package playground.toronto.transitnetworkutils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap; 
import java.util.Iterator;
import java.util.Map;

public class ValidateGTFS {

	/**
	 * Produces, for each stop, a list of routes which use it.
	 * 
	 * @param args	[0] = file location of trips.txt
	 * 				[1] = file location of stop_times.txt
	 * 				[2] = file location to export.
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException
	{
		if(args.length != 3) {
			System.out.println("Incorrect arguments!");
			return;
		}
		
		
		//Build the trip-route mapping
		HashMap<String,String> tripRouteMap = new HashMap<String, String>();
		BufferedReader reader = new BufferedReader(new FileReader(args[0]));
		String header = reader.readLine();
		int tpCol = Arrays.asList(header.split(",")).indexOf("trip_id");
		int rtCol = Arrays.asList(header.split(",")).indexOf("route_id");
		
		String line;
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
		reader = new BufferedReader(new FileReader(args[1]));
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
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(args[2]));
		Iterator i = stopRoutesMap.entrySet().iterator();
		while(i.hasNext()){
			Map.Entry e = (Map.Entry)i.next();
			writer.write("\n" + e.getKey() + ":" + e.getValue().toString());
			i.remove();
		}
		writer.close();
		
	}
}
