package playground.toronto.gtfsutils;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Time;

import playground.toronto.demand.util.TableReader;

/**
 * This class reads the GTFS file for Toronto, and creates a frequencies.txt
 * file for use in the converter. This was necessary because the TTC files
 * have one 'trip' per departure (which slowed down the process of converting
 * the files considerably).
 * 
 * It also creates a new trips.txt file and stop_times.txt file. It groups the 
 * trips by common stop-sequence into groups; with each group becoming the 
 * basis for a new trip in the updated file. A separate file, trip_groups.txt,
 * is created which lists the stop sequence and list of trips contained in a
 * trip group.
 * 
 * @author pkucirek
 *
 */
public class CreateGTFSFrequencies {

	private static final Logger log = Logger.getLogger(CreateGTFSFrequencies.class);
	
	private static HashMap<IdImpl, Trip> trips;
	private static HashMap<IdImpl, TripGroup> groups;
	private static HashMap<IdImpl, Period> frequencies;
	
	private static boolean filesLoaded = false;
	private static boolean tripsGrouped = false;
	private static boolean frequenciesCreated = false;
	
	private static HashMap<String, IdImpl> stopSequenceTripGroupMap;
	private static HashMap<IdImpl, String> tripGroupStopSequenceMap; //The above map in reverse.
	private static HashMap<IdImpl, String> routeIdNameMap;
	private static HashSet<IdImpl> tripGroupsWithOneTrip;
	
	private static void loadFiles(String folder, String service) throws FileNotFoundException, IOException{
		
		routeIdNameMap = new HashMap<IdImpl, String>();
		trips = new HashMap<IdImpl, CreateGTFSFrequencies.Trip>();
		TableReader tr;
		
		log.info("Opening GTFS files in folder " + folder);		
		//Load routes.txt
		tr = new TableReader(folder + "/routes.txt");
		tr.open();
		tr.ignoreTrailingBlanks(true);
		while (tr.next()) routeIdNameMap.put(new IdImpl(tr.current().get("route_id")), tr.current().get("route_short_name"));
		tr.close();
		log.info("routes.txt loaded.");
		
		//Load trips.txt
		tr = new TableReader(folder + "/trips.txt");
		tr.open();
		tr.ignoreTrailingBlanks(true);
		while (tr.next()){
			String svc = tr.current().get("service_id");
			if (!service.equals(svc)) continue;
			
			Trip T = new Trip(new IdImpl(tr.current().get("trip_id")));
			T.dir = tr.current().get("direction_id");
			T.shape = tr.current().get("shape_id");
			T.routeId = new IdImpl(tr.current().get("route_id"));
			T.headsign = tr.current().get("trip_headsign");
			T.serviceId = "" + svc;
			trips.put(T.id, T);
		}
		tr.close();
		log.info("trips.txt loaded.");
		
		//Load stop_times.txt
		tr = new TableReader(folder + "/stop_times.txt");
		tr.open();
		tr.ignoreTrailingBlanks(true);
		while(tr.next()){
			IdImpl tpId = new IdImpl(tr.current().get("trip_id"));
			if (!trips.containsKey(tpId)) continue;//throw new IOException("Trip \"" + tpId.toString() + "\" could not be found! This is a problem with the data.");
			Trip T = trips.get(tpId);

			double dep = Time.parseTime(tr.current().get("departure_time"));
			double arr = Time.parseTime(tr.current().get("arrival_time"));
			IdImpl stopId = new IdImpl(tr.current().get("stop_id"));
			Integer stopIndex = new Integer(tr.current().get("stop_sequence"));
			Tuple<Double, Double> times = new Tuple<Double, Double>(dep, arr);
			Tuple<String,String> stopCodes = new Tuple<String, String>(tr.current().get("pickup_type"), tr.current().get("drop_off_type"));
			
			T.checkAndOrAddDeparture(dep);
			T.stopOrder.put(stopIndex, stopId);
			T.stopTimes.put(stopId, times);
			T.stopCodes.put(stopId, stopCodes);
		}
		tr.close();
		
		filesLoaded = true;
		log.info("stop_times.txt file loaded. All GTFS files have been loaded.");
	}
	
	private static void buildGroups() throws IOException{
		if(!filesLoaded) throw new IOException("Files have not been loaded!");
		
		groups = new HashMap<IdImpl, CreateGTFSFrequencies.TripGroup>();
		stopSequenceTripGroupMap = new HashMap<String, IdImpl>();
		tripGroupStopSequenceMap = new HashMap<IdImpl, String>();
		
		//Create a simple map to store the number of branches per direction per route. 
		HashMap<IdImpl, HashMap<String, Integer>> routeBranchNumbersMap;routeBranchNumbersMap = new HashMap<IdImpl, HashMap<String,Integer>>();
		for (IdImpl r : routeIdNameMap.keySet()) routeBranchNumbersMap.put(r, new HashMap<String, Integer>());
		
		//Iterate through the trips
		for (Trip T : trips.values()){
			String seq = T.getSequence();
			if (!stopSequenceTripGroupMap.containsKey(seq)){
				//New TripGroup, new entry to the sequence map.
				
				String routeName = routeIdNameMap.get(T.routeId);
				Integer branchNum;
				if (!routeBranchNumbersMap.get(T.routeId).containsKey(T.dir)){
					//New direction
					branchNum = new Integer(0);
				}else branchNum = routeBranchNumbersMap.get(T.routeId).get(T.dir) + 1;
				routeBranchNumbersMap.get(T.routeId).put(T.dir, branchNum);
				
				IdImpl groupId = new IdImpl(routeName + "-D" + T.dir + "_B" + branchNum.toString());
				TripGroup g = new TripGroup(groupId);
				
				//I've assumed that the following properties are the same for all trips in a trip-group.
				g.dir = T.dir;
				g.routeId = T.routeId;
				g.shape = T.shape;
				g.headsign = T.headsign;
				g.serviceId = T.serviceId;
				
				groups.put(groupId, g);
				stopSequenceTripGroupMap.put(seq, groupId);
				tripGroupStopSequenceMap.put(groupId, seq);
			}
			
			TripGroup G = groups.get(stopSequenceTripGroupMap.get(seq));
			G.trips.add(T.id);
		}
		
		tripsGrouped = true;
		log.info(groups.size() + " trip groups built.");
	}
	
	private static void buildFrequencies() throws Exception{
		if (!tripsGrouped) throw new Exception("Trips have not been grouped!");
		
		frequencies = new HashMap<IdImpl, CreateGTFSFrequencies.Period>();
		tripGroupsWithOneTrip = new HashSet<IdImpl>();
		int currentPeriod = 1;
		
		for (TripGroup group : groups.values()){
			if (group.trips.size() < 2) {
				tripGroupsWithOneTrip.add(group.id);
				continue; //Do not create a frequencies entry for this trip group. Stop_times for these groups will be left 'as-is'
			}
			
			ArrayList<Trip> tps = new ArrayList<CreateGTFSFrequencies.Trip>();
			for (IdImpl t : group.trips) tps.add(trips.get(t));
			Collections.sort(tps);
			
			Period f = new Period(new IdImpl(currentPeriod++));
			f.groupId = group.id;
			f.startTime = tps.get(0).departure;
			double prevDep = tps.get(1).departure;
			int prevHdwy = (int) (prevDep - f.startTime);
			
			for (int i = 2; i < tps.size(); i++){
				Trip T = tps.get(i);
				double dep = T.departure;
				int hdwy = (int) (dep - prevDep);			
				
				if (hdwy != prevHdwy){
					//Headway has changed. Complete the current frequency period, and add it to the list.
					f.endTime = dep;
					f.headway = prevHdwy;
					
					frequencies.put(f.id, f);
					
					f = new Period(new IdImpl(currentPeriod++));
					f.groupId = group.id;
					f.startTime = T.departure;
					prevHdwy = hdwy;
				}
				prevDep = T.departure;
			}
			//End of trips.
			f.headway = prevHdwy;
			f.endTime = prevDep;
			frequencies.put(f.id, f);
		}
		
		frequenciesCreated = true;
		log.info(frequencies.size() + " frequency periods created for " + groups.size() + " trip groups.");
	}
	
	private static void writeTripGroups(String folder) throws IOException{
		if (!tripsGrouped) throw new IOException("Trip groups have not been created!");
		
		BufferedWriter writer1 = new BufferedWriter(new FileWriter(folder + "\\trip_groups.txt"));
		BufferedWriter writer2 = new BufferedWriter(new FileWriter(folder + "\\trips_updated.txt"));
		writer1.write("group_id;[stop1,stop2,...];[trip1,trip2,...]");
		writer2.write("route_id,service_id,trip_id,trip_headsign,direction_id,shape_id");
		
		for (TripGroup G : groups.values()){
			String str = "\n" + G.id.toString() + ";[" + tripGroupStopSequenceMap.get(G.id) + "];[";
			for (IdImpl T : G.trips) str += trips.get(T).id.toString() + ",";
			str += "]";
			
			writer1.write(str);
			
			writer2.write("\n" + G.printAsTrip());
		}
		
		writer1.close();
		writer2.close();
		log.info("Trip groups successfully exported to " + folder + "\\trip_groups.txt");
		log.info("Trips file successfully updated as " + folder + "\\trips_updated.txt");
	}
	
	private static void writeStopTimes(String folder) throws IOException{
		if (!tripsGrouped) throw new IOException("Trip groups have not been created!");
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(folder + "\\stop_times_updated.txt"));
		writer.write("trip_id,arrival_time,departure_time,stop_id,stop_sequence,stop_headsign,pickup_type,drop_off_type");
		
		for (TripGroup G : groups.values()){
			
			if (tripGroupsWithOneTrip.contains(G.id)){
				//Print stop-times 'as-is'.
				Trip T = trips.get(G.trips.toArray()[0]);
				for (int i = 1; i <= T.stopOrder.size(); i++){
					IdImpl stop = T.stopOrder.get(i);
					
					writer.write("\n" + G.id.toString() + "," //tip_id
							+ Time.writeTime(T.stopTimes.get(stop).getFirst()) + "," //arrival
							+ Time.writeTime(T.stopTimes.get(stop).getSecond()) + "," //departure
							+ stop.toString() + "," + i + ",," //stop_id,stop_sequence,headsign = null
							+ T.stopCodes.get(stop).getFirst() + "," //pickup_type
							+ T.stopCodes.get(stop).getSecond()); //dropoff_type
				}
			}else{
				//Print average of stop offsets over all trips in the period.
				ArrayList<Trip> tps = new ArrayList<CreateGTFSFrequencies.Trip>();
				for (IdImpl t : G.trips) tps.add(trips.get(t));
				HashMap<IdImpl, Tuple<Double, Double>> averageStopTimes = generateAverageStopTimes(tps);
				
				//Assuming that all trips in the group have similar properties, at least in terms of stop times.
				Trip T = tps.get(0);
				for (int i = 1; i <= T.stopOrder.size(); i++){
					IdImpl stop = T.stopOrder.get(i);
					//Double arr = averageStopTimes.get(stop).getFirst();
					//Double dep = averageStopTimes.get(stop).getSecond();
					
					writer.write("\n" + G.id.toString() + "," //tip_id
							+ Time.writeTime(averageStopTimes.get(stop).getFirst()) + "," //arrival
							+ Time.writeTime(averageStopTimes.get(stop).getSecond()) + "," //departure
							+ stop.toString() + "," + i + ",," //stop_id,stop_sequence,headsign = null
							+ T.stopCodes.get(stop).getFirst() + "," //pickup_type
							+ T.stopCodes.get(stop).getSecond()); //dropoff_type
				}
			}	
		}
		
		writer.close();
		log.info("Stop times successfully updated as " + folder + "\\stop_times_updated.txt");
	}
	
	private static void writeFrequencies(String folder) throws IOException{
		if (!frequenciesCreated) throw new IOException("Frequency periods have not been created!");
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(folder + "\\frequencies.txt"));
		writer.write("trip_id,start_time,end_time,headway_secs,exact_times");
		for (Period f : frequencies.values()) writer.write("\n" + f.print());
		
		writer.close();
		log.info("Frequency periods successfully exported as " + folder + "\\frequencies.txt");
	}
	
	public static void main(String[] args){

		String folder = args[0];
		String serviceId = args[1];
		
		//Don't want to overwrite an existing frequencies file.
		if (new File(folder + "/frequencies.txt").exists()) {
			log.error("Frequencies file already exists in folder \"" + folder + "\"! Exiting.");
			return;
		}
		
		try {
			
			///////////////////////////////////////////
			loadFiles(folder, serviceId);
			///////////////////////////////////////////
			
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			return;
		} catch (IOException e1) {
			e1.printStackTrace();
			return;
		}

		///////////////////////////////////////////
		try {
			buildGroups();
		} catch (IOException e1) {
			e1.printStackTrace();
			return;
		}
		///////////////////////////////////////////
		
		///////////////////////////////////////////
		try {
			buildFrequencies();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		///////////////////////////////////////////

		try {
			writeTripGroups(folder);
			writeFrequencies(folder);
			writeStopTimes(folder);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		System.exit(0);
	}
	
	private static HashMap<IdImpl, Tuple<Double,Double>> generateAverageStopTimes(ArrayList<Trip> a){
		HashMap<IdImpl, Tuple<Double, Double>> result = new HashMap<IdImpl, Tuple<Double,Double>>();
		HashMap<IdImpl, Double> arrivalSums = new HashMap<IdImpl, Double>();
		HashMap<IdImpl, Double> depSums = new HashMap<IdImpl, Double>();
		
		for (Trip T : a){
			for (Entry<IdImpl, Tuple<Double, Double>> st : T.stopTimes.entrySet()){
				IdImpl s = st.getKey();
				Double arr = st.getValue().getFirst();
				Double dep = st.getValue().getSecond();
				if (arrivalSums.containsKey(s)) arr += arrivalSums.get(s);
				if (depSums.containsKey(s)) dep += depSums.get(s);
				arrivalSums.put(s, arr);
				depSums.put(s, dep);
			}
		}
		
		for (IdImpl s : arrivalSums.keySet()){
			result.put(s, new Tuple<Double, Double>(arrivalSums.get(s) / a.size(), depSums.get(s) / a.size()));
		}
		
		return result;
	}
	
	private static class Period {
		private IdImpl id;
		protected IdImpl groupId;
		protected double startTime;
		protected double endTime;
		protected int headway;
		protected boolean exactTimes;

		protected Period(IdImpl i){
			this.id = i;
		}
		
		/**
		 * @return trip_id,start_time,end_time,heaway_secs,exact_times
		 */
		protected String print(){
			return this.groupId.toString() + "," + Time.writeTime(this.startTime) +
					"," + Time.writeTime(this.endTime) + "," + this.headway + "," + 
					(this.exactTimes ? 1 : 0);
		}
	}
	
	private static class TripGroup {
		private IdImpl id;
		protected IdImpl routeId; //Generally the same properties as a trip
		protected String dir;
		protected String shape;
		protected String headsign;
		protected String serviceId;
		
		protected HashSet<IdImpl> trips;
		
		protected TripGroup(IdImpl i){
			this.id = i;
			this.trips = new HashSet<IdImpl>();
		}
		
		/**
		 * @return route_id,service_id,trip_id,trip_headsign,direction_id,shape_id
		 */
		protected String printAsTrip(){
			return this.routeId.toString() + "," +  this.serviceId + "," +
					this.id.toString() + "," + this.headsign + "," +
					this.dir + "," + this.shape;
		}
	}
	
	private static class Trip implements Comparable<Trip> {
		private IdImpl id;
		protected IdImpl routeId;
		protected String dir;
		protected String shape;
		protected double departure;
		protected String headsign;
		protected String serviceId;
		
		protected HashMap<IdImpl, Tuple<Double, Double>> stopTimes;
		protected HashMap<Integer, IdImpl> stopOrder;
		protected HashMap<IdImpl, Tuple<String, String>> stopCodes; //For pickup/dropoff types;
		
		protected Trip(IdImpl i){
			this.id = i;
			this.stopOrder = new HashMap<Integer, IdImpl>();
			this.stopTimes = new HashMap<IdImpl, Tuple<Double,Double>>();
			this.stopCodes = new HashMap<IdImpl, Tuple<String,String>>();
			this.departure = Double.MAX_VALUE;
		}

		protected void checkAndOrAddDeparture(Double dep){
			if (dep < this.departure) this.departure = dep;
		}
		
		protected String getSequence(){
			String s = this.stopOrder.get(1).toString();
			for (int i = 2; i <= this.stopOrder.size(); i++) s += "," + this.stopOrder.get(i);
			return s;
		}

		@Override
		public int compareTo(Trip o) {
			if (o.departure < this.departure) return 1;
			else if (o.departure > this.departure) return -1;
			else return 0;
		}
		
	}
}
