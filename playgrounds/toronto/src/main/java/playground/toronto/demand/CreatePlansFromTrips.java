package playground.toronto.demand;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;

import playground.balmermi.world.WorldUtils;
import playground.balmermi.world.Zone;
import playground.balmermi.world.ZoneLayer;
import playground.toronto.demand.util.TableReader;

/**
 * A new class for creating MATSim plans from a record of trips,
 * generally an output of the Transportation Tomorrow Survey (TTS).
 * It could be adapted for use elsewhere, as long as the fields
 * are formatted the same (the only hard-coded values are 'H' as
 * the flag for 'home activity').
 * 
 * Uses the TableReader class for a more relaxed file format.
 * 
 * A bunch of added functionality, including a consistency checker,
 * and some simple GUIs for opening/saving files.
 * 
 * @author pkucirek
 *
 */
public class CreatePlansFromTrips {

	// ////////////////////////////////////////////////////////////////////
	// member variables
	// ////////////////////////////////////////////////////////////////////
	
	private static final Logger log = Logger.getLogger(CreatePlansFromTrips.class);
	
	private ScenarioImpl scenario;
	private ZoneLayer zones = null;
	private final List<String> tripsFieldNames = Arrays.asList(new String[]
				{"hhid", //Household ID
				"pid", //Person ID
				"start_time", //Start time of trip
				"act_o", //Activity purpose of origin
				"act_d", //Activity purpose of destination
				"zone_o", //Zone of origin
				"zone_d", //Zone of destination
				"type", //Trip type: (0 = car, 1 = pure transit, 2 = drive-access-transit)
				"weight"}); //Household weight
	private final List<String> zonesFieldNames = Arrays.asList(new String[]
			{"zone_id",
			"x",
			"y"});
	
	private HashMap<Id, String> personHouseholdMap; //person id, hhid
	private HashMap<Id, Trip> trips; //trip_id, attributes
	private HashMap<Id, List<Id>> personTripsMap; //person id, list of trip ids.
	private HashMap<String, Coord> householdCoords;
	private HashSet<Id> unusableTripChains;
	private HashSet<Id> nonHouseholdTripChains;
	private HashSet<Id> chainsWithNoZones;
	
	// ////////////////////////////////////////////////////////////////////
	// constructor
	// ////////////////////////////////////////////////////////////////////
	
	public CreatePlansFromTrips(){
		this.scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
	}
	
	// ////////////////////////////////////////////////////////////////////
	// IO functions
	// ////////////////////////////////////////////////////////////////////
	
	private void readZones(String filename) throws IOException{
		
		log.info("Opening " + filename + " as a zones file.");
		
		TableReader tr = new TableReader(filename);
		tr.open();
		if (!tr.checkHeaders(zonesFieldNames)) throw new IOException("Zones file not correctly formatted!");
	
		this.zones = new ZoneLayer();
		
		while (tr.next()){
			this.zones.createZone(new IdImpl(tr.current().get("zone_id")), 
					tr.current().get("x"), 
					tr.current().get("y"), 
					null, null, null, null);
		}
		tr.close();
		
		log.info("Zones file opened successfully.");
	}
	
	private void readTrips(String filename) throws IOException{
		log.info("Opening " + filename + " as a trips file.");
		
		TableReader tr = new TableReader(filename);
		tr.open();
		if (!tr.checkHeaders(tripsFieldNames)) throw new IOException("Trips file not correctly formatted!");
		
		this.personHouseholdMap = new HashMap<Id, String>();
		this.trips = new HashMap<Id, CreatePlansFromTrips.Trip>();
		this.personTripsMap = new HashMap<Id, List<Id>>();
		
		long tripNum = 0;
		while (tr.next()){
			//Get the current person, create if necessary.
			IdImpl pid = new IdImpl(tr.current().get("hhid") + "-" + tr.current().get("pid"));
			PersonImpl P;
			if (!scenario.getPopulation().getPersons().containsKey(pid)) {
				P = new PersonImpl(pid);
				scenario.getPopulation().addPerson(P);
				personHouseholdMap.put(pid, tr.current().get("hhid"));
				personTripsMap.put(pid, new ArrayList<Id>());
			}
			else{
				P = (PersonImpl) scenario.getPopulation().getPersons().get(pid);
			}
			
			//Map trips to persons
			IdImpl tid = new IdImpl(tripNum++);
			Trip T = new Trip(tr.current().get("zone_o"), 
					tr.current().get("zone_d"), 
					tr.current().get("act_o"), 
					tr.current().get("act_d").replace("C", "S").replace("R", "W"), //Replaces 'subsequent work' and 'subsequent school' (R and C) with (W and S) 
					tr.current().get("start_time"), 
					tr.current().get("type"));
			trips.put(tid, T);
			personTripsMap.get(pid).add(tid);

		}

		tr.close();
		
		log.info("Trips file opened successfully. " + scenario.getPopulation().getPersons().size() + " persons were recognized.");
	}
	
	private void writePlans(String filename){
		PopulationWriter writer = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
		writer.writeStartPlans(filename);
		writer.writePersons();
		writer.writeEndPlans();
	}
		
	// ////////////////////////////////////////////////////////////////////
	// primary functions
	// ////////////////////////////////////////////////////////////////////
	
	/**
	 * Checks that all trips start and end in zones represented in the zones file.
	 */
	private boolean checkTripZoneConsistency(){
		
		log.info("Checking that zones and trips are consistent.");
		
		HashSet<String> missingZones = new HashSet<String>();
		
		for (Trip T : trips.values()){
			BasicLocation zo = zones.getLocation(new IdImpl(T.zone_o));
			BasicLocation zd = zones.getLocation(new IdImpl(T.zone_d));
			
			if (zo == null){
				missingZones.add(T.zone_o);
			}
			if (zd == null){
				missingZones.add(T.zone_d);
			}
		}
		
		if (missingZones.size() > 0){
			String msg = missingZones.size() + " zones are missing! Full list below: ";
			for (String s : missingZones) msg += "\n\tZone \"" + s.toString() + "\"";
			log.error(msg);
			return false;
		}else log.info("All zones OK!");
		
		return true;
	}
	
	/**
	 * Checks which persons have inconsistent (which do not start and end in the same location)
	 * or non-household trip chains.
	 */
	private boolean checkPopConsistency(){
		
		log.info("Checking consistency of trip chains.");

		unusableTripChains = new HashSet<Id>();
		nonHouseholdTripChains = new HashSet<Id>();
		chainsWithNoZones = new HashSet<Id>();
		
		for (Person P : scenario.getPopulation().getPersons().values()){
			ArrayList<Trip> pTrips = new ArrayList<CreatePlansFromTrips.Trip>();
			for (Id i : personTripsMap.get(P.getId())) pTrips.add(trips.get(i));
			
			//Sort trips in order of start time
			bubbleSortTrips(pTrips);

			//Check the consistency of the trip chain
			int state = checkTripChain(pTrips);
			if(state == 5){
				//Trip chain has unusable zones
				chainsWithNoZones.add(P.getId());
			} else if (state == 4 || state == 3 || state == 2){
				//Trip chain is inconsistent, or has only one trip
				unusableTripChains.add(P.getId());
				continue;
			}
			else if (state == 1){
				//Trip chain is consistent, but does not contain a home episode
				nonHouseholdTripChains.add(P.getId());
				
			}else if (state == 0){
				//Trip chain is consistent AND contains at least one home episode
			}
			else{
				log.error("CheckTripConsistency returned an unrecognized value!");
			}
		}
		

		
		int total = this.scenario.getPopulation().getPersons().size();
		int badChains = unusableTripChains.size();
		int nonHHChains = nonHouseholdTripChains.size();
		int noZoneChains = chainsWithNoZones.size();
		int goodChains = total - badChains - nonHHChains - noZoneChains;
		int pctBC = (badChains * 100 / total );
		int pctNHH = (nonHHChains * 100 / total);
		int pctNZC = (noZoneChains* 100 / total );
		int pctGC = (goodChains* 100 / total );
		
		String summary = "POPULATION CONSISTENCY SUMMARY\n" +
				 "*********************************************\n\n" +
				 "Good chains: " + goodChains + " (" + pctGC + "%)\n" +
				 "Chains with no home: " + nonHHChains + " (" + pctNHH + "%)\n" +
				 "Chains with an unsuable zone: " + noZoneChains + " (" + pctNZC + "%)\n" +
				 "Inconsistent chains:"  + badChains + " (" + pctBC + "%)\n" +
				 "TOTAL: " + total + "\n\n" +
				 "Inconsistent trip chains and chains with\n" +
				 "an unusable zone will be omitted from\n" +
				 "the plan creation procedure.\n\n" +
				 "Proceed?";
		
		int state = new JOptionPane().showOptionDialog(null, summary, "Summary", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
		if (state == JOptionPane.YES_OPTION) return true;
		else return false;
	}
	
	/**
	 * This is the primary logic of this class. Converts an ordered list of trips into a person's activity plan.
	 * 
	 */
	private void createPlans(){
		
		log.info("Creating plans from trips...");
		log.warn("This program does not currenlty handle auto-access/egress transit trips. Please contact author for details.");
		
		householdCoords = new HashMap<String, Coord>();
		
		int completedPlans = 0;
		int currentPct = 0;
		for (Person P : scenario.getPopulation().getPersons().values()){
			
			boolean skipPerson = false;
			boolean isEmployed = false;
			
			if (unusableTripChains.contains(P.getId())) continue; //skips persons with incomplete trips.
			if (chainsWithNoZones.contains(P.getId())) continue;
			
			ArrayList<Trip> pTrips = new ArrayList<CreatePlansFromTrips.Trip>();
			for (Id i : personTripsMap.get(P.getId())) pTrips.add(trips.get(i));
			bubbleSortTrips(pTrips);
			
			PlanImpl p = new PlanImpl();
			Trip T = null;
			
			for (int i = 0; i<pTrips.size(); i++){ //Decided against using a for each loop because ordering is important
				T = pTrips.get(i); //The current trip
				if ((zones.getLocation(new IdImpl(T.zone_o)) == null) || (zones.getLocation(new IdImpl(T.zone_d)) == null)) {
					skipPerson = true;
					continue;
				}
				
				String act_o = T.act_o;
				if (act_o.equals("W")) isEmployed = true;
				Coord c;
				
				if(act_o.equals("H")){ //Home activity
					if(householdCoords.containsKey(personHouseholdMap.get(P.getId()))){ //Household coord already set
						c = householdCoords.get(personHouseholdMap.get(P.getId()));
					}else{ //Household coordinate not set.
						c = getRandomCoordInZone(new IdImpl(T.zone_o));
						householdCoords.put(personHouseholdMap.get(P.getId()), c);
					}
				}else{
					//TODO: Do I want to restrict subsequent work/school activities occurring in the same zone to having the same coordinates? Default is create a new coordinate for each activity.
					c = getRandomCoordInZone(new IdImpl(T.zone_o));
				}
				
				ActivityImpl act = p.createAndAddActivity(act_o, c);
				act.setEndTime(convertTime(T.start_time)); //We only know when an activity episode ends! We don't know when it starts (or how long it is) because we have no information about travel time.
				
				int type;
				LegImpl leg = null;
				try {
					type = Integer.parseInt(T.type);
				} catch (NumberFormatException e) {
					log.error("'Type' field incorrectly formatted for person \"" + P.getId().toString() + "\"! Type must be an integer.");
					skipPerson = true;
					continue;
				}
				if(type == 0){
					//pure-auto trips
					leg = new LegImpl(TransportMode.car);
					
				}else if (type == 1){
					//pure-transit trips
					leg = new LegImpl(TransportMode.pt);
					
				}else if (type == 2){
					//auto-access/auto-egress transit (ie, to GO/subway)
					//TODO handle auto access/egress trips
					
					
					
					
					continue;
				}else{
					log.error("Type \"" + type + "\" not recognized!");
					continue;
				}
				if (leg != null) p.addLeg(leg);
			}
			//Add final activity. Trip T should still be set to the final trip.
			if (T == null){
				log.error("Trip is null!!! Why is this?!");
			}
			String act_d = T.act_d;
			Coord c;
			
			if(act_d.equals("H")){ //Home activity
				if(householdCoords.containsKey(personHouseholdMap.get(P.getId()))){ //Household coord already set
					c = householdCoords.get(personHouseholdMap.get(P.getId()));
				}else{ //Household coordinate not set.
					c = getRandomCoordInZone(new IdImpl(T.zone_o));
					householdCoords.put(personHouseholdMap.get(P.getId()), c);
				}
			}else{
				//TODO: Do I want to restrict subsequent work/school activities occurring in the same zone to having the same coordinates? Default is create a new coordinate for each activity.
				c = getRandomCoordInZone(new IdImpl(T.zone_o));
			}
			
			p.addActivity(new ActivityImpl(act_d, c));
			
			if (!skipPerson) {
				PersonImpl q = (PersonImpl) P;
				q.setEmployed(isEmployed);
				P.addPlan(p);
				completedPlans++;
				int pct = completedPlans * 100 / (scenario.getPopulation().getPersons().size() - chainsWithNoZones.size() - unusableTripChains.size());
				if ((pct % 5) == 0 && pct != currentPct) {
					log.info("..." + completedPlans + " plans completed (" + pct  + "%)...");
					currentPct = pct;
				}
			}
			
		}
		
		log.info(completedPlans + " plans successfully completed.");
		
		List<Id> personsToRemove = new ArrayList<Id>();
		for(Person P : scenario.getPopulation().getPersons().values()) if (P.getSelectedPlan() == null) personsToRemove.add(P.getId());
		for(Id i : personsToRemove) scenario.getPopulation().getPersons().remove(i);
		log.info(personsToRemove.size() + " blank persons removed from population.");

	}

	// ////////////////////////////////////////////////////////////////////
	// utility functions
	// ////////////////////////////////////////////////////////////////////
	
	private Coord getRandomCoordInZone(final Id zoneId) {
		return WorldUtils.getRandomCoordInZone(
				(Zone) this.zones.getLocation(zoneId), this.zones);
	}
	
	private static double convertTime(final String time) throws NumberFormatException {
		int timeAsInt = Integer.parseInt(time);
		int hours = timeAsInt / 100;
		int minutes = timeAsInt % 100;
		return hours * 3600 + minutes * 60;
	}
	
	private void bubbleSortTrips(List<Trip> a){
		Collections.sort(a);
	}	
	
	private int checkTripChain(List<Trip> a){
		
		for (Trip T : a) if ((zones.getLocation(new IdImpl(T.zone_o)) == null) 
				|| zones.getLocation(new IdImpl(T.zone_d)) == null) return 5; //could not find zone.
		
		if (a.size() == 1) return 4; //only one trip in trip chain
		
		String firstActivity = a.get(0).act_o;
		String nextActivity = a.get(0).act_d;
		boolean hasHomeEpisode = false;
		
		int i = 1;
		for (; i < a.size(); i++){
			if (!a.get(i).act_o.equals(nextActivity)) return 3; //Mismatch between two subsequent trip-chains
			if (a.get(i).act_o.equals("H") || a.get(i).act_d.equals("H")) hasHomeEpisode = true;
			nextActivity = a.get(i).act_d;
		}
		//Check that last activity == first activity
		if (!a.get(i - 1).act_d.equals(firstActivity)) return 2; //first activity != last activity
		
		if (!hasHomeEpisode) return 1; //Does not have home activity.
		
		return 0;
	}
	
	// ////////////////////////////////////////////////////////////////////
	// main method
	// ////////////////////////////////////////////////////////////////////
	
	public static void main(String[] args){
		
		String basefolder = "";
		if (args.length == 1) basefolder= args[0];
		
		CreatePlansFromTrips converter = new CreatePlansFromTrips();
		
		String file = "";
		JFileChooser fc;
		if (basefolder != ""){
			fc = new JFileChooser(basefolder);
		}else fc = new JFileChooser();
		fc.setDialogTitle("Please select a zones file");
		fc.setFileFilter(new FileFilter() {
			@Override
			public String getDescription() {return "Table of zones in *.txt format.";}
			@Override
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().toLowerCase(Locale.ROOT).endsWith( ".txt" );
			}
		});		
		int state = fc.showOpenDialog(null);
		if (state == JFileChooser.APPROVE_OPTION){
			file = fc.getSelectedFile().getAbsolutePath();
		}else if (state == JFileChooser.CANCEL_OPTION) return;
		
		if (file == "" || file == null) return;
		
		try {
			converter.readZones(file);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		if (basefolder != ""){
			fc = new JFileChooser(basefolder);
		}else fc = new JFileChooser();
		fc.setDialogTitle("Please select a trips file");
		fc.setFileFilter(new FileFilter() {
			@Override
			public String getDescription() {return "Table of trip records in *.txt format.";}
			@Override
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().toLowerCase(Locale.ROOT).endsWith( ".txt" );
			}
		});
		state = fc.showOpenDialog(null);
		if (state == JFileChooser.APPROVE_OPTION){
			file = fc.getSelectedFile().getAbsolutePath();
		}else if (state == JFileChooser.CANCEL_OPTION) return;
		try {
			converter.readTrips(file);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		//converter.checkTripZoneConsistency();
		
		if(!converter.checkPopConsistency()) return;
				
		converter.createPlans();
		
		if (basefolder != ""){
			fc = new JFileChooser(basefolder);
		}else fc = new JFileChooser();
		fc.setDialogTitle("Save plans file");
		fc.setFileFilter(new FileFilter() {
			@Override
			public String getDescription() {return "Plans in *.xml format";}
			@Override
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().toLowerCase(Locale.ROOT).endsWith( ".xml" );
			}
		});	
		state = fc.showSaveDialog(null);
		if (state == JFileChooser.APPROVE_OPTION){
			file = fc.getSelectedFile().getAbsolutePath();
			if (file != "" && file != null) converter.writePlans(file);
		}else if (state == JFileChooser.CANCEL_OPTION) return;

		
	}
	
	// ////////////////////////////////////////////////////////////////////
	// private Trip class
	// ////////////////////////////////////////////////////////////////////
	
	private static class Trip implements Comparable<Trip>{
		public String zone_o;
		public String zone_d;
		public String act_o;
		public String act_d;
		public String start_time;
		public String type;
		
		public Trip(String zo, String zd, String ao, String ad, String st, String t){
			this.zone_o = zo;
			this.zone_d = zd;
			this.act_o = ao;
			this.act_d = ad;
			this.start_time = st;
			this.type = t;
		}

		@Override
		public int compareTo(Trip o) {
			if (Time.parseTime(o.start_time) < Time.parseTime(this.start_time)) return 1;
			else if (Time.parseTime(o.start_time) > Time.parseTime(this.start_time)) return -1;
			else return 0;
		}
	}
	
}
