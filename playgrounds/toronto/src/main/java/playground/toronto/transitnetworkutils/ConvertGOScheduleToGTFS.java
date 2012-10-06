package playground.toronto.transitnetworkutils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.swing.JOptionPane;

import org.apache.log4j.Logger;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Time;

import playground.toronto.demand.util.TableReader;


public class ConvertGOScheduleToGTFS {

	private static final Logger log = Logger.getLogger(ConvertGOScheduleToGTFS.class);
	
	private static HashSet<RouteTable> routeTables;
	private static HashMap<String, String> stops = null;
	
	private static void loadGOSchedule(String filename) throws IOException{
		//factory = new TransitScheduleFactoryImpl();
		//schedule = (TransitScheduleImpl) factory.createTransitSchedule();
		routeTables = new HashSet<ConvertGOScheduleToGTFS.RouteTable>();
		
		log.info("Loading GO Schedules from file " + filename);
		
		BufferedReader br = new BufferedReader(new FileReader(filename));
		String line;
		String routeName = "";
		String dirName = "";
		List<String> stops = null;
		List<String[]> lines = null;
		while ((line = br.readLine()) != null){
			String[] cells = line.split(",");
			if (cells.length < 1) continue;
			if (cells[0].equals("ROUTE")){
				//Finish old stop table
				if (!routeName.equals("")){
					RouteTable rt = new RouteTable(routeName, dirName, stops, lines);
					routeTables.add(rt);
					log.info("Route \"" + routeName + "(" + dirName + ")\" loaded.");
				}
				
				//New stop table
				routeName = cells[1];
				dirName = br.readLine().split(",")[1];
				cells = br.readLine().split(",");
				stops = Arrays.asList(cells);
				lines = new ArrayList<String[]>();
			}else{
				lines.add(cells);
			}
		}
		
		log.info("Done. " + routeTables.size() + " routes loaded.");		
	}
	
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		String inFileName = args[0];
		String tempFileName = args[1];
		String gtfsFolder = args[2];
						
		loadGOSchedule(inFileName);

		/*
		log.info("Check table consistency...");
		
		for (RouteTable rt : routeTables){
			if (!rt.checkTableConsistency(tempFileName)) System.exit(0);
			log.info(rt.routeName + " (" + rt.direction + ") is OK.");
		}
		*/
		
		String stopMap = null;
		if (args.length == 4){
			stopMap = args[3];
			
			RenameStops(stopMap);
		}
		
		GO2GTFS(gtfsFolder);
		

	}

	private static void RenameStops(String mapName) throws FileNotFoundException, IOException{
		int stopNumber = 100;
		
		HashMap<String, String> converter = new HashMap<String, String>();
		stops = new HashMap<String, String>();
		
		TableReader tr = new TableReader(mapName);
		tr.ignoreTrailingBlanks(true);
		tr.open();
		while(tr.next()){
			String name = tr.current().get("stop_name");
			String desc = tr.current().get("stop_desc");
			String lon = tr.current().get("stop_lon");
			String lat = tr.current().get("stop_lat");
			
			String oldId = desc + ": " + name;
			//stop_id,stop_code,stop_name,stop_desc,stop_lat,stop_lon,zone_id,stop_url,location_type,parent_station
			String data = stopNumber + ",," + name + "," + desc + "," + lat + "," + lon; 
			converter.put(oldId, "" + stopNumber);
			stops.put("" + stopNumber++, data);
		}
		tr.close();
		
		HashSet<String> missingStops = new HashSet<String>();
		
		for (RouteTable table : routeTables){
			ArrayList<String> newHeader = new ArrayList<String>();
			for (String oldName : table.stopsHeader){
				String newName = converter.get(oldName);
				if (newName == null){
					missingStops.add(oldName);
					newHeader.add(oldName);
				}else{
					newHeader.add(newName);
				}
			}
			table.stopsHeader = newHeader;
		}
		
		for (String str : missingStops) log.warn("Could not find stop id for \"" + str + "\"!");
	}
	
	private static void GO2GTFS(String foldername) throws IOException{
		BufferedWriter routeFile = new BufferedWriter(new FileWriter(foldername + "/routes.txt"));
		routeFile.write("route_id,agency_id,route_short_name,route_long_name,route_desc,route_type,route_url,route_color,route_text_color");
		BufferedWriter tripsFile = new BufferedWriter(new FileWriter(foldername + "/trips.txt"));
		tripsFile.write("route_id,service_id,trip_id,direction_id,shape_id");
		BufferedWriter stopTimesFile = new BufferedWriter(new FileWriter(foldername + "/stop_times.txt"));
		stopTimesFile.write("trip_id,arrival_time,departure_time,stop_id,stop_sequence,stop_headsign,pickup_type,drop_off_type");
		
		if (stops != null){
			BufferedWriter bw = new BufferedWriter(new FileWriter(foldername + "/stops.txt"));
			bw.write("stop_id,stop_code,stop_name,stop_desc,stop_lat,stop_lon,zone_id,stop_url,location_type,parent_station");
			for (String s : stops.values()){
				bw.newLine();
				bw.write(s);
			}
			bw.close();
		}
		
		//Default is that there is one RT for each direction, so these need to be grouped when exporting.
		//Additionally, there needs to be one route for each mode
		//One trip entry is created for each row in the RouteTable (grouping will be done later)
		//Stop times will be created this way as well
		
		HashSet<String> routes = new HashSet<String>();
		int tripNumber = 2000;
		
		for (RouteTable table : routeTables){
			String sName = table.routeName.substring(0, 2);
			String lName = table.routeName.substring(3);
				
			String busRoute = "GO" + sName + "b,2," + sName + "," + lName + ",,3,,,";
			String trainRoute = "GO" + sName + "t,2," + sName + "," + lName + ",,2,,,";
			
			int direction = 0; //Assuming that each route has only two directions.
			if (routes.contains(trainRoute) || routes.contains(busRoute)) direction = 1;
			
			if (table.trainTrips.contains(false)){
				routes.add(busRoute);
			}
			if (table.trainTrips.contains(true)){
				routes.add(trainRoute);
			}
			if (table.trainTrips.size() == 0){
				log.info("No trips recorded for table " + table.routeName);
				continue;
			}
			
			for (int i = 0; i < table.times.size(); i++){
				//Write trip entry.
				int tripId = tripNumber++;
				tripsFile.newLine();
				if (table.trainTrips.get(i)){
					tripsFile.write(trainRoute.split(",")[0] + ",1," + tripId + "," + direction); //train trip
				}else{
					tripsFile.write(busRoute.split(",")[0] + ",1," + tripId + "," + direction); //bus trip
				}
				
				//Write stop times entries
				List<Tuple<Double, Double>> trip = table.times.get(i);
				List<String> headers = table.stopsHeader; 
				//trip_id,arrival_time,departure_time,stop_id,stop_sequence,stop_headsign,pickup_type,drop_off_type
				int seq = 1;
				for (int j = 0; j < trip.size(); j++){
					Tuple<Double, Double> stopTimes = trip.get(j);
					if (stopTimes == null) continue;
					
					String stop = headers.get(j);
					stopTimesFile.newLine();
					stopTimesFile.write(tripId + "," + Time.writeTime(stopTimes.getFirst()) + "," + Time.writeTime(stopTimes.getSecond())
							+ "," + stop + "," + seq++ + ",0,0");
				}
			}
		}
		
		for (String rt : routes){
			routeFile.newLine();
			routeFile.write(rt);
		}
		
		routeFile.close();
		tripsFile.close();
		stopTimesFile.close();
	}
	
	private static class RouteTable {
		private String routeName;
		private String direction;
		private List<String> stopsHeader;
		private List<List<Tuple<Double, Double>>> times;
		private List<Boolean> trainTrips;
		
		private RouteTable(String name, String direction, List<String> header, List<String[]> lines){
			this.routeName = name;
			this.direction = direction;
			this.stopsHeader = header;
			this.times = new ArrayList<List<Tuple<Double,Double>>>();
			this.trainTrips = new ArrayList<Boolean>();
			
			for (String[] cells : lines){
				ArrayList<Tuple<Double,Double>> trip = new ArrayList<Tuple<Double,Double>>();
				{
					boolean isTrain = false;
					for (String s : cells)  if (s.contains("t")) isTrain = true;
					trainTrips.add(isTrain);
				}				
				
				for (int i = 0; i < cells.length; i++){
					String S = cells[i];
					
					//nullifies VIA stops
					if (S.equals("") || S == null || S.equals("v")) {
						trip.add(null);
					}else{
						if (S.contains("t")) S = S.replaceAll("t", "");
						
						if (S.contains(".")){
							String[] c = S.split("\\.");
							Double first = parseTime(c[0]);
							Double second = parseTime(c[1]);
							trip.add(new Tuple<Double, Double>(first, second));
						}else{
							Double value = parseTime(S);
							trip.add(new Tuple<Double, Double>(value, value));
						}
					}
				}
				
				this.times.add(trip);
			}
		}
		
		private void changeTableEntry(int i, int j, Tuple<Double,Double> entry){
			this.times.get(i).set(j, entry);
			
		}
		
		public boolean hasMixedModeTrips(){
			return ((this.trainTrips.contains(true)) && (this.trainTrips.contains(false)));
		}
		
		private boolean checkTableConsistency(String filename) throws IOException{
			
			File x = new File(filename);
			if (x.exists()){
				BufferedReader br = new BufferedReader(new FileReader(filename));
				String line;
				while ((line = br.readLine()) != null){
					String[] cells = line.split(",");
					if (cells[0].equals("ROUTE")){
						String s = cells[1];
						String d = br.readLine().split(",")[1];
						if (this.routeName.equals(s) && this.direction.equals(d)) return true;
					}
				}
				br.close();
			}

			String output = "ROUTE," + this.routeName + "\n" +
					"DIRECTION," + this.direction + "\n" + this.stopsHeader.get(0);
			for (int i = 1; i < this.stopsHeader.size(); i++) output += "," + this.stopsHeader.get(i);
			
			

			
			for (int i = 0; i < this.times.size(); i++){
				output += "\n";
				int prevStopIndex = 0;
				double prevDep = 0;
				List<Tuple<Double, Double>> trip = this.times.get(i);
				for (int j = 0; j < trip.size(); j++){
					Tuple<Double, Double> tup = trip.get(j);
					if (tup == null){
						if (j != 0) output += ",";
						continue;
					}
					
					double dep = tup.getSecond();
					
					if (dep <= prevDep){
						//initiate the GUI checking procedure.
						
						//Previous stop.
						String message = "Route " + this.routeName + " (" + this.direction + ") has a congruency error.\n\n" +
								"Please enter a new stop time for previous stop \"" + this.stopsHeader.get(prevStopIndex) + "\":\n";
						for (int k = 0; k < trip.size(); k++){
							if (trip.get(k) == null) continue; 
							message += "\n" + this.stopsHeader.get(k) + " (" + writeTime(trip.get(k).getSecond()) + ")";
						}
						message += "\n\nTime format: hhmm";
						boolean isParsing = true;
						while (isParsing){
							String s = JOptionPane.showInputDialog(message);
							if (s == null || s.equals("")){
								int state = JOptionPane.showConfirmDialog(null, "Do you wish to cancel? Data for this route table may not be saved.", "Exit?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
								if (state == JOptionPane.YES_OPTION) return false;
							}else{
								try {
									Double newtime = parseTime(s);
									Tuple<Double, Double> T = new Tuple<Double, Double>(newtime, newtime);
									this.changeTableEntry(i, prevStopIndex, T);
									isParsing = false;
								} catch (NumberFormatException e) {
									int state = JOptionPane.showConfirmDialog(null, "Do you wish to cancel? Data for this route table may not be saved.", "Exit?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
									if (state == JOptionPane.YES_OPTION) return false;
								}
							}
						}
						
						//Current stop
						message = "Please enter a new stop time for current stop \"" + this.stopsHeader.get(j) + "\":\n";
						for (int k = 0; k < trip.size(); k++){
							if (trip.get(k) == null) continue; 
							message += "\n" + this.stopsHeader.get(k) + " (" + writeTime(trip.get(k).getSecond()) + ")";
						}
						message += "\n\nTime format: hhmm";
						isParsing = true;
						while (isParsing){
							String s = JOptionPane.showInputDialog(message);
							if (s == null || s.equals("")){
								int state = JOptionPane.showConfirmDialog(null, "Do you wish to cancel? Data for this route table may not be saved.", "Exit?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
								if (state == JOptionPane.YES_OPTION) return false;
							}else{
								try {
									Double newtime = parseTime(s);
									Tuple<Double, Double> T = new Tuple<Double, Double>(newtime, newtime);
									this.changeTableEntry(i, j, T);
									isParsing = false;
								} catch (NumberFormatException e) {
									int state = JOptionPane.showConfirmDialog(null, "Do you wish to cancel? Data for this route table may not be saved.", "Exit?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
									if (state == JOptionPane.YES_OPTION) return false;
								}
							}
						}
					}
					
					//write time to file.
					if (j != 0) output += ",";
					if (tup.getFirst().equals(tup.getSecond())){
						output += writeTime(tup.getFirst());
						if (this.trainTrips.get(i)) output += "t";
					}else output += writeTime(tup.getFirst()) + "." + writeTime(tup.getSecond());
					prevDep = dep;
					prevStopIndex = j;
				}
			}
			
			BufferedWriter bw = new BufferedWriter(new FileWriter(filename, true));
			bw.write(output + "\n");
			bw.close();
			
			return true;
		}
		
		
	}
	
	/**
	 * Parses the 4-character time representation hhmm, padding with leading zeroes
	 * 
	 * @param s - the time string to parse
	 * @return
	 * @throws IOException
	 */
	private static double parseTime(String s) throws NumberFormatException{
		if (s.length() > 4) throw new NumberFormatException("Could not parse time: \"" + s + "\"!");
		
		double result = 0;
		
		char[] c = s.toCharArray();
		char[] x = new char[]{'0','0','0','0'};
		for (int i = 0; i < c.length; i++) x[i + 4 - c.length] = c[i];
		
		try {
			int hours = Integer.parseInt("" + x[0] + x[1]);
			int minutes = Integer.parseInt("" + x[2] + x[3]);
			result = hours * 3600 + minutes * 60;
		} catch (NumberFormatException e) {
			throw new NumberFormatException("Could not parse time: \"" + s + "\"!");
		}
		
		//Carry time over to next day for any time earlier tahn 0300h
		if (result < (3*3600)) result += (24*3600);
		
		return result;
	}
	
	private static String writeTime(double t){
		int h = (int) (t / 3600);
		int m = (int) ((t - h * 3600) / 60);
		
		if (m < 10 ) return "" + h + "0" + m;
		else return "" + h + "" + m;
		
	}
}
