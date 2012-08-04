package playground.toronto.transitnetworkutils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import javax.swing.JOptionPane;

import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Time;


/**
 * This class contains code for reading and parsing specific transit schedules from GTHA transit operators (ie, GO, TTC, YRT, etc.); each
 * of which are in slightly different formats. The aim is to convert them to a common format which is stop-agnostic (ie, the stops are 
 * simply string identifiers) since geocoding stops is largely a manual activity. Each transit route is defined by a set order of stops,
 * each stop has an associated list of arrival/departure times. Each importer function will store a list of routes, then a common
 * export function will convert the routes to a common MATSim-XML format.
 * 
 * Unfortunately these functions will be ugly (ie, not elegant) as they are intended to be (mostly) one-offs. It appears that Brampton
 * and Mississauga use the same software (HASTUS) for scheduling so I hope that this work will not be overlooked.
 *  
 * @author pkucirek
 */

public class ScheduleParser {
	
	private ArrayList<ScheduledRoute> routes;
	
	public ScheduleParser(){
		this.routes = new ArrayList<ScheduledRoute>();
	}
			
	//==============================================================================
	
	/** 
	 * Imports GO Transit's schedule. I've converted it to an xlsx file, which has been exported to a csv file.
	 * The algorithm automatically creates numbered branches based on which stops/stations a line includes
	 * times for. 
	 * 
	 * Update Jan 19 2012: This algorithm should work for Durham, as well as Hamilton! 
	 * 
	 * FORMAT (.csv)
	 * 
	 * ROUTE, [routename]
	 * DIRECTION, [direction]
	 * [stopA], [stopB],...
	 * [timeA1], [timeB1],...
	 * [timeA2], [timeB2]...
	 * ...
	 * 
	 * Stops which have schedule departure/arrivals should be formatted thusly:
	 * ...,[prevtime],[arrivaltime]a.[departuretime]d,[nexttime],...
	 *   
	 * @param filename -- The name & location of the file to be read.
	 * @throws FileNotFoundException 
	 */
	public void ImportGOSchedule(String filename) throws IOException{
		
		final String COMMA = ",";
		final String ROUTE = "ROUTE";
		
		Integer currentBranchNumber = 0;
		String currentRouteName = "";
		String currentDirection = "";
		
		String[] currentStops = null;		
		
		BufferedReader reader = new BufferedReader( new FileReader(filename));
		
		String fileLine = reader.readLine();	
		
		do {
			
			String[] cells = fileLine.split(COMMA);
			
			if (!IsLineEmpty(cells)){
				
				if (cells[0].equals(ROUTE)) {
					//This piece of code reads in three lines: route, direction, and the stops header.
					currentRouteName = cells[1];
					currentDirection = reader.readLine().split(COMMA)[1];
					currentStops = reader.readLine().split(COMMA);
					currentBranchNumber = 0;
				}
				else {
					
					boolean[] emptyCells = new boolean[cells.length];
					for(int i=0; i<cells.length; i++) emptyCells[i] = cells[i].isEmpty(); 
					
					
					if (cells.length > currentStops.length) {
						System.err.println("ERROR: Stop header length doesn't match timetable length!");
					}
					
					ArrayList<String> tempStops = new ArrayList<String>();
					for (int i=0; i<cells.length; i++) if(!(emptyCells[i])) tempStops.add(currentStops[i]);
					
					if (tempStops.size() == 1) {
						System.out.println("check here!");
					}
	
					//--------------------------------------
					if (!(SearchForStopSequence(tempStops))){
						// No match. Create new 'route' object (with new branch #)						
						ScheduledRoute newRoute = new ScheduledRoute(currentRouteName, currentDirection, "" + currentBranchNumber++, tempStops);
		
						ArrayList<String> checkstops = newRoute.getStopSequence();
						
						int currentStopIndex = 0;
						for (int i=0; i<cells.length; i++){
							
							if(!(emptyCells[i])) {
								String[] parsedArrivalTime = {cells[i],""};
								if (parsedArrivalTime[0].contains("t")){
									newRoute.mode = "train";
									parsedArrivalTime[0] = cells[i].replace("t", "");
								}
								if (parsedArrivalTime[0].contains(".")){
									parsedArrivalTime = cells[i].split("\\.");
								}
								if (parsedArrivalTime[0].contains("v")){
									newRoute.getStop(currentStopIndex++).isVIA = true;
									continue;
								}
								
								newRoute.getStop(currentStopIndex++).AddTime(new Tuple<String,String>(parsedArrivalTime[0], parsedArrivalTime[1]));
							}
						}
						
						this.routes.add(newRoute);
						
						
					}
					else {
						// Matches existing route. Append stop times to route.
						int currentRouteIndex = MatchStopSequence(tempStops);
						
						int currentStopIndex = 0;
						for (int i=0; i<cells.length; i++){
							
							//if (parsedArrivalTime[0].contains("v")) newRoute.getStop(currentStopIndex).isVIA = true;
							
							String[] parsedArrivalTime = {cells[i],""};
							if (parsedArrivalTime[0].contains("t")){
								parsedArrivalTime[0] = cells[i].replace("t", "");
							}
							if (parsedArrivalTime[0].contains(".")){
								parsedArrivalTime = cells[i].split("\\.");
							}
							if (parsedArrivalTime[0].contains("v")) continue;
							
							if(!(emptyCells[i])) {
								this.routes.get(currentRouteIndex).getStop(currentStopIndex++)
								.AddTime(new Tuple<String,String>(parsedArrivalTime[0],parsedArrivalTime[1]));
							}
						}
					}
						
				}
			}
			
			fileLine = reader.readLine();
			
		} while (fileLine != null);
		
		reader.close();
	}
	
	
	//==============================================================================
	
	/**
	 * Imports a pre-processed HASTUS schedule, pre-processed in Excel to be similar to the GO schedule
	 * format. RESTRICTION: Data values cannot include the ":" character
	 * 
	 * FORMAT (.txt)
	 * 
	 * Route: [routename]
	 * Direction: [direction]
	 * [columnheader, a series of space-separated lines which denote fixed column-width (like ____ _____ ___ __ ____ ....)]
	 * [stops names]
	 * [stop times 1]
	 * [stop times 2]
	 * ...
	 * 
	 * @param filename
	 * @throws IOException
	 */
	public void ImportHastusSchedule(String filename) throws IOException{
		
		final String[] SKIPPABLE = {"Block", "Note", "From", "To"};
		final String COLON = ":";
		
		// Converts SKIPABLE to an ArrayList, to allow for ease of add/removing skippable column headers
		ArrayList<String> SkippedHeaders = new ArrayList<String>();
		for (String s:SKIPPABLE) SkippedHeaders.add(s);
		
		String currentRouteName = "";
		String currentDirectionName = "";
		Integer currentBranchNumber = 0;
		String currentColumnWidths = "";
		
		ArrayList<String> currentStops = new ArrayList<String>();
		ArrayList<String> currentCells = null;
		
		BufferedReader reader = new BufferedReader( new FileReader(filename));
		
		String fileLine = reader.readLine();
		
		do{
			
			if (fileLine.split(COLON).length > 1){
				
				// Identifies a header record, and parses the next route/direction combo.
				
				currentRouteName = fileLine.split(COLON)[1];
				
				currentDirectionName = reader.readLine().split(COLON)[1];
				
				// Determine the column widths for this section.
				currentColumnWidths = reader.readLine();
				currentStops = SplitFixedWidth(reader.readLine(), getFixedColumnWidths(currentColumnWidths));
								
				currentBranchNumber = 0;
	
			}
			else {
				
				currentCells = SplitFixedWidth(fileLine, getFixedColumnWidths(currentColumnWidths));
				
				boolean[] emptyCells = new boolean[currentCells.size()];
				for(int i=0; i<currentCells.size(); i++) emptyCells[i] = (currentCells.get(i).isEmpty() ||
						SkippedHeaders.contains(currentCells.get(i))); 
				
				
				if (currentCells.size() > currentStops.size()) {
					System.err.println("ERROR: Stop header length doesn't match timetable length!");
				}
				
				ArrayList<String> tempStops = new ArrayList<String>();
				for (int i=0; i < currentCells.size(); i++) if(!(emptyCells[i])) tempStops.add(currentStops.get(i));
				
				if (tempStops.size() == 1) {
					System.err.println("check here!");
				}
	
				//--------------------------------------
				if (!(SearchForStopSequence(tempStops))){
					// No match. Create new 'route' object (with new branch #)
					ScheduledRoute newRoute = new ScheduledRoute(currentRouteName, currentDirectionName, "" + currentBranchNumber++, tempStops);
	
					int currentStopIndex = 0;
					for (int i=0; i < currentCells.size(); i++){
						
						if(!(emptyCells[i])) {
							String parsedArrivalTime = currentCells.get(i);
							
							newRoute.getStop(currentStopIndex++).AddTime(new Tuple<String,String>(parsedArrivalTime, ""));
						}
					}
					
					this.routes.add(newRoute);
					
				}
				else {
					// Matches existing route. Append stop times to route.
					int currentRouteIndex = MatchStopSequence(tempStops);
					
					int currentStopIndex = 0;
					for (int i=0; i < currentCells.size(); i++){
						String parsedArrivalTime = currentCells.get(i);
						
						if(!(emptyCells[i])) {
							this.routes.get(currentRouteIndex).getStop(currentStopIndex++)
							.AddTime(new Tuple<String,String>(parsedArrivalTime, ""));
						}
					}
				}
			}
			
			fileLine = reader.readLine();
		
		} while (fileLine!= null);
		
		for (ScheduledRoute R:this.routes){
			System.out.println(R.id + "(" + R.direction + "): " + R.getStopSequence().toString());
		}
		System.out.println("...done");

		
	}
	
	//==============================================================================
	
	private boolean SearchForStopSequence(ArrayList<String> stops){
		
		for (ScheduledRoute route:this.routes){
			if (stops.equals(route.getStopSequence())) return true;
		}
		
		return false;
	}
	
	// Returns the location of a particular sequence of stops within the routes array. Returns -1 if no match 
	private int MatchStopSequence(ArrayList<String> stops){
		
		int i = 0;
		
		for (; i < this.routes.size(); i++){
			if (stops.equals(this.routes.get(i).getStopSequence())) return i;
		}
		
		return -1;
		
	}
	
	private boolean IsLineEmpty(String[] cells){
			
		for(String s:cells){
			if (!(s.isEmpty())) return false;
		}
		
		return true;
	}
	
	private ArrayList<Tuple<Integer,Integer>> getFixedColumnWidths(String line){
		ArrayList<Tuple<Integer,Integer>> result = new ArrayList<Tuple<Integer,Integer>>();
			
		char[] charArray = line.toCharArray();
		for(int start=0, end= 0; end < charArray.length; end++){
			char c = charArray[end];

			if (c == ' '){
				result.add(new Tuple<Integer, Integer>(start, end));
				start = end + 1;
			}
			if (end == (charArray.length - 1)){
				result.add(new Tuple<Integer, Integer>(start, charArray.length - 1));
			}
		}
		
		return result;
	}
	
	// A simple function for returning the cells of a fixed-width column array, given defined column widths
	private ArrayList<String> SplitFixedWidth(String line, ArrayList<Tuple<Integer,Integer>> columnWidths){
		
		ArrayList<String> result = new ArrayList<String>();
		char[] cArray = line.toCharArray();
		
		for (Tuple<Integer,Integer> T:columnWidths){
			
			String s = "";
			
			for (int i = T.getFirst(); (i < T.getSecond()) && (i < cArray.length); i++){
				s += cArray[i];
			}
			
			result.add(s);
		}
		
		return result;
	}
	
	//==============================================================================

	// A simple getter in case other functions need to access the routes data.
	public ArrayList<ScheduledRoute> getRoutes(){
		return this.routes;
	}
	public void PrintRoute(){
		for (ScheduledRoute r:this.routes) System.out.println(r.id + " " + r.getStopSequence().toString());
		System.out.println("...done.");
	}
	
	/**
	 * A function for exporting a master list of stops for all routes. Also identifies stops with literal
	 * duplicate names (ie, will recognize that "Bramalea" and "Bramalea" are the same, but not "BRAMALEA")
	 * 
	 * @param filename
	 */
	public void ExportStopList(String filename, String systemName) throws IOException{
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(filename));		
		writer.write(systemName + " LIST OF STOPS");
		writer.newLine();
		writer.newLine();
		
		ArrayList<String> stops = new ArrayList<String>();
		
		for (ScheduledRoute r:this.routes){
			for (String s:r.getStopSequence()){
				if (!(stops.contains(s))) stops.add(s);
			}
		}
		
		for (String s:stops){
			writer.write(s);
			writer.newLine();
		}
		writer.close();
	}
	
	/**
	 * Converts the schedule files into GTFS files. Does not handle stops (as geographic data is not present).
	 * 
	 * @param foldername: The folder to export to.
	 * @throws IOException 
	 */
	public void ExportGtfsFiles(String foldername) throws IOException{
		
				
		BufferedWriter wRoutes = new BufferedWriter(new FileWriter(foldername + "/routes.txt"));
		BufferedWriter wStopTimes = new BufferedWriter(new FileWriter(foldername + "/stop_times.txt"));
		BufferedWriter wTrips = new BufferedWriter(new FileWriter(foldername + "/trips.txt"));
		BufferedWriter wFrequencies = new BufferedWriter(new FileWriter(foldername + "/frequencies.txt"));
	
		wRoutes.write("route_id,agency_id,route_short_name,route_long_name,route_desc,route_type,route_url,route_color,route_text_color");
		wStopTimes.write("trip_id,arrival_time,departure_time,stop_id,stop_sequence,stop_headsign,pickup_type,drop_off_type");
		wTrips.write("route_id,service_id,trip_id,trip_headsign,direction_id,shape_id");
		wFrequencies.write("trip_id,start_time,end_time,headway_secs,exact_times");
		
		int currentRoute = 0;
		int currentTrip = 0;
		HashSet<String> routesExported = new HashSet<String>();
		HashMap<String, HashSet<String>> routeDirectionsMap = new HashMap<String, HashSet<String>>();
		HashMap<String, Integer> routeCurrentDirectionMap = new HashMap<String, Integer>();
		for (ScheduledRoute R : this.routes){
			
			String routeId = (currentRoute < 100) ? (currentRoute < 10) ? ("00" + currentRoute) : ("0" + currentRoute) : (currentRoute + "");
			String sname = R.routename.substring(0, 2);
			
			//Write to routes.txt file
			if (!routesExported.contains(R.routename)){
				routesExported.add(R.routename);
				currentRoute++;
				
				String lname = R.routename.substring(2);
				int type = R.mode.equals("train") ? 2 : 3; //TODO: Need to migrate train trips as separate routes.
				
				//route_id,agency_id,route_short_name,route_long_name,route_desc,route_type,route_url,route_color,route_text_color
				wRoutes.write("\n" + routeId + ",," + sname + "," + lname + ",," + type + ",,");
							
				//Initialize the upper-level maps which keep track of current direction and current branch
				routeDirectionsMap.put(R.routename, new HashSet<String>());
				routeDirectionsMap.get(R.routename).add(R.direction);
				routeCurrentDirectionMap.put(R.routename, new Integer(0));
			}
				
			//Write to trips file. There is a 1:1 relationship between 'routes' as defined in this class, and 'trips' as used in our GTFS files
			{
				String tripId = sname + R.direction + "_" + R.branch;
				
				if (!routeDirectionsMap.get(R.routename).contains(R.direction)){
					//New direction
					routeDirectionsMap.get(R.routename).add(R.direction);
					routeCurrentDirectionMap.put(R.routename, routeCurrentDirectionMap.get(R.routename) + 1);
				}
				
				//route_id,service_id,trip_id,trip_headsign,direction_id,shape_id
				wTrips.write("\n" + routeId + ",1," + tripId + ",," + routeCurrentDirectionMap.get(R.routename) + ",");
				
				//Write to stop_times and frequencies files.
				{
					int numberOfDepartures = R.stops.get(0).getTimes().size();
					if (numberOfDepartures == 1){
						//Only one departure. No frequency entry will be created for this trip; also, stop_times will be exactly the offset
						for (int i = 0; i < R.stops.size(); i++){
							ScheduledStop S = R.stops.get(i);
							if (S.isVIA) continue;
							
							//trip_id,arrival_time,departure_time,stop_id,stop_sequence,stop_headsign,pickup_type,drop_off_type
							wStopTimes.write("\n" + tripId + S.getTimes().get(0).getFirst() + "," + S.getTimes().get(0).getSecond()
									+ "," + S.getId() + "," + (i + 1) + ",0,0,");
						}
					}else{
						//Write the average stop times
						for (int i = 0; i < R.stops.size(); i++){
							ScheduledStop S = R.stops.get(i);
							if (S.isVIA) continue;
							
							wStopTimes.write("\n" + tripId + S.getAvgArrTime() + "," + S.getAvgDepTime()
									+ "," + S.getId() + "," + (i + 1) + ",0,0,");
						}
						
						//Create headway frequencies
						ArrayList<Tuple<Double, Double>> departures = R.stops.get(0).getTimes();
						double startTime = departures.get(0).getSecond();
						double prevDep = departures.get(1).getSecond();
						int prevHdwy = (int) (prevDep - startTime);
						double dep = 0;
						for (int i = 2; i < departures.size(); i++){
							dep = departures.get(i).getSecond();
							int hdwy = (int) (dep - prevDep);
							if (hdwy != prevHdwy){
								//trip_id,start_time,end_time,headway_secs,exact_times
								wFrequencies.write("\n" + tripId + "," + startTime + "," + dep + "," + prevHdwy+ ",0");
								startTime = dep;
								prevHdwy = hdwy;
							}
							prevDep = dep;
						}
						wFrequencies.write("\n" + tripId + "," + startTime + "," + dep + "," + prevHdwy+ ",0");
					}	
				}
				
			}
			
			System.out.println("Completed " + R.id);
		}
		
		//All routes done.
		wRoutes.close();
		wTrips.close();
		wStopTimes.close();
		wFrequencies.close();
		
		
		
	}
	
	/**
	 * Exports a summary of branches and directions per route. Each summary lincludes a list of
	 * all stops serviced, and a list of route-branches which fully ennumerate all stops.
	 * 
	 * @param filename
	 * @param systemName
	 * @throws IOException
	 */
	public void ExportRouteSummary(String filename, String systemName) throws IOException{

		//ArrayList<RouteGroup> groups = new ArrayList<RouteGroup>();
		HashMap<String, RouteGroup> groups = new HashMap<String, RouteGroup>();
		
		for(ScheduledRoute R : this.routes){
			if( ! groups.containsKey(R.routename)){
				groups.put(R.routename, new RouteGroup(R.routename));
			}
			groups.get(R.routename).addRoute(R);
		}
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
		writer.write(systemName + " ROUTE SUMMARY\n");
		
		for(RouteGroup G : groups.values()){
			
			writer.write("\n\nROUTE: \"" + G.getName() + "\""
					+ "\n\tMODES: " + G.getModes().toString()
					+ "\n\tDIRECTIONS:");
			for(String S: G.getBranches().keySet()){
				writer.write("\n\t\t" + S + " - " + G.getBranches().get(S) + " branch(es).");
			}
			writer.write("\n\tSTOPS:");
			for(String S : G.getStops()){
				writer.write("\n\t\t" + S);
			}
			writer.write("\n\tENUMERATED SET:");
			for(ScheduledRoute R : G.getEnumeratedSet()){
				writer.write("\n\t\t" + R.direction + " " + R.branch);
			}
		}
		
		writer.close();
	}
	
	public void ExportDetailedRoutes(String systemName, String folderName) throws IOException{
		
		BufferedWriter writer;
		String fileStream = "";
		String prevRoute = "";
				
		for (ScheduledRoute r:this.routes){			
			
			if (prevRoute == ""){
				prevRoute = r.routename;
			}
			
			if (!r.routename.equals(prevRoute)){
				
				fileStream = prevRoute + "\r\r" + fileStream;
				
				writer = new BufferedWriter(new FileWriter(folderName + "\\" + systemName + " - " + prevRoute + ".txt"));
				writer.write(fileStream);
				writer.close();
				
				prevRoute = r.routename;
				fileStream = "";
			}
			else{
							
				fileStream += "Bracnh " + r.branch + "," + r.mode + " (" + r.direction + "):\r";
				ArrayList<String> stops = r.getStopSequence();
				for (String s:stops){
					fileStream += s + "\r";
				}
			}
		}
		
		prevRoute = this.routes.get(this.routes.size() - 1).routename;
		
		fileStream = prevRoute + "\r\r" + fileStream;
		
		writer = new BufferedWriter(new FileWriter(folderName + "\\" + systemName + " - " + prevRoute + ".txt"));
		writer.write(fileStream);
		writer.close();
		
	}
	
	/**
	 * Checks that all routes have ordered times.
	 */
	public void ValidateSchedule(String newScheduleFile){
		for (ScheduledRoute R : this.routes){
			
			//Iterate through all departures.
			double prevDep = 0;
			for (int currentDepIndex = 0; currentDepIndex < R.getStop(0).getTimes().size(); currentDepIndex++){
				double currentDep = R.getStop(0).getTimes().get(currentDepIndex).getSecond();
				double prevStopDep = 0;
				for (int i = 0; i < R.getStopSequence().size(); i++){
					double stopDep = R.getStop(i).getTimes().get(currentDepIndex).getSecond();
					if (stopDep <= prevStopDep){
						
						String msg = "Route \"" + R.id + "\" has a stop congruency error.\n\n" +
								"Please enter a new time for the previous stop (" + R.getStop(i - 1).getId() + "):\n";
						for (int j = 0; j < R.getStopLength(); j++) msg += "\n" + R.getStop(j).getId() + "     " + 
								Time.writeTime(R.getStop(j).getTimes().get(currentDepIndex).getSecond());
						msg += "\n\nFormat: hhmm";
						String s = JOptionPane.showInputDialog(msg);
						Double first = new Double(ScheduledStop.parseTime(s));
						
						R.getStop(i - 1).setTime(currentDepIndex, new Tuple<Double,Double>(first, first));
						
						msg = "Please enter a new time for the current stop (" + R.getStop(i).getId() + "):\n";
						for (int j = 0; j < R.getStopLength(); j++) msg += "\n" + R.getStop(j).getId() + "     " + 
								Time.writeTime(R.getStop(j).getTimes().get(currentDepIndex).getSecond());
						msg += "\n\nFormat: hhmm";
						
						s = JOptionPane.showInputDialog(msg);
						Double second = new Double(ScheduledStop.parseTime(s));
						
						R.getStop(i).setTime(currentDepIndex, new Tuple<Double,Double>(second, second));

					}	
					prevStopDep = stopDep;
				}
				
				if (currentDep <= prevDep){
					System.out.println("Departures for route \"" + R.id + "\" are out of order!");
				}
				
			}
			
			
		}
	}
	
	/*
	private void readOverrides(String filename){
		
	}
	*/
	
}
