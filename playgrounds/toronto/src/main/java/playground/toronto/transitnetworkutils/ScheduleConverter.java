package playground.toronto.transitnetworkutils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.matsim.core.utils.collections.Tuple;


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

public class ScheduleConverter {
	
	private ArrayList<ScheduledRoute> routes;
	
	public ScheduleConverter(){
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
		final String PERIOD = ".";
		
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
		
						int currentStopIndex = 0;
						for (int i=0; i<cells.length; i++){
							
							if(!(emptyCells[i])) {
								String[] parsedArrivalTime = {cells[i],""};
								if (parsedArrivalTime[0].contains("t")){
									newRoute.mode = "train";
									parsedArrivalTime[0] = cells[i].replace("t", "");
								}
								if (parsedArrivalTime[0].contains("a") && parsedArrivalTime[0].contains("d")){
									parsedArrivalTime = cells[i].split(PERIOD);
									parsedArrivalTime[0].replace("a", "");
									parsedArrivalTime[1].replace("d", "");
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
							String parsedArrivalTime = cells[i];
							if (parsedArrivalTime.contains("t")){
								parsedArrivalTime = cells[i].replace("t", "");
							}
							
							if(!(emptyCells[i])) {
								this.routes.get(currentRouteIndex).getStop(currentStopIndex++)
								.AddTime(new Tuple<String,String>(parsedArrivalTime, ""));
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
	

	//==============================================================================
	/**
	 * Master function for exporting a formatted transitschedule.xml to MATSim. A lot of stuff
	 * going on in this function. Most information is encoded in the schedule (already imported
	 * to ScheduleConverter), but two additional mapping files are required to establish the
	 * network: 
	 * 1. Stop-Link Map: A four-column file - StopName, X, Y, LinkRefId. StopName should be
	 * 		indexed to the list of stops present in the schedule file (use ExportStopList).
	 * 2. Route Link Profile: TODO determine the format of this file
	 * 
	 * @param stopLinkMapName
	 * @param routeLinkProfileMapName
	 * @param outfileName
	 * @throws IOException
	 */
	public void ExportToMatsim(String stopLinkMapName, String routeLinkProfileMapName, String outfileName) throws IOException{
		
		//1. EXPORT LIST OF STOPS
		
	}
}
