package playground.toronto.transitnetworkutils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
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


public class ConvertGOScheduleToGTFS {

	private static final Logger log = Logger.getLogger(ConvertGOScheduleToGTFS.class);
	
	private static HashSet<RouteTable> routeTables;
	
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
		
		GOtoGTFS(gtfsFolder);
		

	}

	private static void GOtoGTFS(String foldername) throws IOException{
		
		//Get all the routes
		HashMap<String, Boolean> routeHasTrainTrips = new HashMap<String, Boolean>();
		for (RouteTable rt : routeTables) 
			routeHasTrainTrips.put(rt.routeName, rt.hasMixedModeTrips());
			
		
		//Write routes.txt
		BufferedWriter bw1 = new BufferedWriter(new FileWriter(foldername + "/routes.txt"));
		bw1.write("route_id,agency_id,route_short_name,route_long_name,route_desc,route_type,route_url,route_color,route_text_color");
		for (String route : routeHasTrainTrips.keySet()){
			
			String sName = route.substring(0, 2);
			String lName = route.substring(3);
			
			if (routeHasTrainTrips.get(route)){
				//Write entry for bus route
				bw1.write("\n" + "GO" + sName + "b,2," + sName + "," + lName + ",,3,,,");
				//write entry for train route
				bw1.write("\n" + "GO" + sName + "t,2," + sName + "," + lName + ",,2,,,");
			}else{
				//Assume bus-only route
				bw1.write("\n" + "GO" + sName + "b,2," + sName + "," + lName + ",,3,,,");
			}
		}
		bw1.close();
		
		//Write trips and stop_times simultaneously...each departure will be given its own trip & a random id
		bw1 = new BufferedWriter(new FileWriter(foldername + "/trips.txt"));
		bw1.write("route_id,service_id,trip_id,trip_headsign,direction_id,shape_id");
		BufferedWriter bw2 = new BufferedWriter(new FileWriter(foldername + "/stop_times.txt"));
		bw2.write("trip_id,arrival_time,departure_time,stop_id,stop_sequence,stop_headsign,pickup_type,drop_off_type");

		HashMap<String, HashSet<String>> routeDirectionMap = new HashMap<String, HashSet<String>>();
		HashMap<String, Integer> routeCurrentDirection = new HashMap<String, Integer>();
		
		int currentTrip = 2000;
		
		for (RouteTable route : routeTables){
			String sName = route.routeName.substring(0, 2);
			
			//Get current direction #
			if (!routeDirectionMap.containsKey(route.routeName)){
				routeDirectionMap.put(route.routeName, new HashSet<String>());
				routeCurrentDirection.put(route.routeName, 0);
			}
			if (!routeDirectionMap.get(route.routeName).contains(route.direction)){
				routeDirectionMap.get(route.routeName).add(route.direction);
				routeCurrentDirection.put(route.routeName, routeCurrentDirection.get(route.routeName) + 1);
			}
			int directionNum = routeCurrentDirection.get(route.routeName);
			
			for (int currentDep = 0; currentDep < route.times.size(); currentDep++){
				List<Tuple<Double, Double>> departure = route.times.get(currentDep);
				String route_id = "GO" + sName + (route.trainTrips.get(currentDep) ? "t" : "b");
				String trip_id = "" + ++currentTrip;
				
				//Write to trips file.
				bw1.write("\n" + route_id + ",1," + trip_id + ",," + directionNum + ",");
				
				for (int i = 0, j = 1; i < departure.size(); i++){
					String stop = route.stopsHeader.get(i);
					if (stop == null || stop.equals("")) continue;
					Tuple<Double, Double> tup = departure.get(i);
					if (tup == null) continue; // VIA stops will NOT be exported!
					
					double arr = tup.getFirst();
					double dep = tup.getSecond();
					bw2.write("\n" + trip_id + "," + Time.writeTime(arr) + "," + Time.writeTime(dep)
							+ ",\"" + stop + "\"," + (j++) + ",,0,0");
				}
			}
			log.info("Exported " + route.routeName + " (" + route.direction + ")");
			
		}
		
		bw1.close();
		bw2.close();
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
			int busCount = 0;
			int trainCount = 0;
			for (Boolean b : this.trainTrips) {
				if (b){
					trainCount++;
				}else{
					busCount++;
				}
			}
			
			return ((busCount > 0) && (trainCount > 0));
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
