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
import java.util.Map.Entry;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
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
@SuppressWarnings("deprecation")
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
	private final List<String> additonalTripAttributeNames = Arrays.asList(new String[]
			{"access_mode",
			"egress_mode",
			"access_zone",
			"egress_zone"});
	
	private HashMap<Id, String> personHouseholdMap; //person id, hhid
	private HashMap<Id, Trip> trips; //trip_id, attributes
	private HashMap<Id, HashSet<Id>> personTripsMap; //person id, list of trip ids.
	private HashMap<Id, Coord> stations; //station id, location
	private HashMap<String, Double> householdWeights; //hhid, expansionfactor
	
	
	private HashMap<String, Coord> householdCoords;
	private HashSet<Id> unusableTripChains;
	private HashSet<Id> nonHouseholdTripChains;
	private HashSet<Id> chainsWithNoZones;
	//TODO remove this once mixed mode trips are implemented.
	private HashSet<Id> personsWithMixedModeTrips;
	
	private boolean isUsingMixedModeTrips = false;
	
	// ////////////////////////////////////////////////////////////////////
	// constructor
	// ////////////////////////////////////////////////////////////////////
	
	public CreatePlansFromTrips(){
		this.scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
	}
	
	// ////////////////////////////////////////////////////////////////////
	// get / set methods
	// ////////////////////////////////////////////////////////////////////
	
	public boolean isUsingMixedModeTrips() {
		return isUsingMixedModeTrips;
	}

	public void setUsingMixedModeTrips(boolean isUsingMixedModeTrips) {
		this.isUsingMixedModeTrips = isUsingMixedModeTrips;
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
		if (tr.checkHeaders(additonalTripAttributeNames)) this.isUsingMixedModeTrips = true;
		
		this.personHouseholdMap = new HashMap<Id, String>();
		this.trips = new HashMap<Id, CreatePlansFromTrips.Trip>();
		this.personTripsMap = new HashMap<Id, HashSet<Id>>();
		this.householdWeights = new HashMap<String, Double>();
		
		long tripNum = 0;
		while (tr.next()){
			//Puts the household factor into the map.
			householdWeights.put(tr.current().get("hhid"), 
					new Double( tr.current().get("weight")));
			
			//Get the current person, create if necessary.
			IdImpl pid = new IdImpl(tr.current().get("hhid") + "-" + tr.current().get("pid"));
			PersonImpl P;

			if (!scenario.getPopulation().getPersons().containsKey(pid)) {
				P = new PersonImpl(pid);
				scenario.getPopulation().addPerson(P);
				personHouseholdMap.put(pid, tr.current().get("hhid"));
								
				personTripsMap.put(pid, new HashSet<Id>());
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
		
		//TODO remove this once mixed mode trips are implemented.
		personsWithMixedModeTrips = new HashSet<Id>();		
		
		for (Person P : scenario.getPopulation().getPersons().values()){
			ArrayList<Trip> pTrips = new ArrayList<CreatePlansFromTrips.Trip>();
			for (Id i : personTripsMap.get(P.getId())) pTrips.add(trips.get(i));
			
			//Sort trips in order of start time
			sortTrips(pTrips);

			//TODO Currently skips all trips of type '2'
			//Check for persons with a 'type 2 trip'
			for (Trip T : pTrips) {
				if (T.type.equals("2")){
					personsWithMixedModeTrips.add(P.getId());
					continue;
				}
			}
			
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
	 * A function for expanding the population by a given factor. USE BEFORE CREATING TRIPS.
	 */
	private void expandPopulation(){
		
		log.info("Starting the population expansion procedure...");
		
		double weightFactor = 0;
		String message = "Please enter the weight factor. Household weights (ie, expansion factors)\n" +
				"will be multiplied by this value. For example, to generate a full population,\n" +
				"enter '1.0'. To generate a population with a 1:1 matching of the sample data\n" +
				"enter '0', or leave this field blank.\n" +
				"Default value = 0.0";
		String s = JOptionPane.showInputDialog(message);
		try {
			weightFactor = Double.parseDouble(s);
			
		} catch (NumberFormatException e) {
			log.info("Expansion factor factor set to 0.");
		}		
		
		if (weightFactor > 0.2){
			message = "WARNING: Using the expansion factor will\n" +
					"significantly increase the size of the\n" +
					"population. The time required to create\n" +
					"plans for all agents will increase\n" +
					"accordingly.";
			JOptionPane.showMessageDialog(null, message, "WARNING", JOptionPane.WARNING_MESSAGE);
		}
		
		int originalPopulation = 0;
		int personsAdded = 0;
				
		//Build original households by inverting the personHouseholdMap
		HashMap<String, HashSet<Id>> oldHouseholds = new HashMap<String, HashSet<Id>>();
		for (Entry<Id, String> e : personHouseholdMap.entrySet()){
			String hhid = e.getValue();
			Id pid = e.getKey();
			if (!oldHouseholds.containsKey(hhid)) oldHouseholds.put(hhid, new HashSet<Id>());
			
			oldHouseholds.get(hhid).add(pid);
			originalPopulation++;
		}
		
		//Iterate through oldHouseholds
		for (Entry<String, HashSet<Id>> e : oldHouseholds.entrySet()){
			
			double weight = householdWeights.get(e.getKey()) * weightFactor;
			
			// iterate over the household weight.
			for (int i = 1; i < weight; i++){
				String newHhId = e.getKey() + "(" + i + ")";
				for (Id pid : e.getValue()){
					
					//Skip non-usable people. There is no need to copy them and waste memory.
					if (unusableTripChains.contains(pid) || 
							personsWithMixedModeTrips.contains(pid) || //TODO: remove this once mixed mode trips are implemented.
							chainsWithNoZones.contains(pid)) continue;
					
					//Create the new person
					IdImpl newPid = new IdImpl(newHhId + "-" + pid.toString().split("-")[1]); //Assumes that person Ids are formatted as "[hhid]-[person#]"
					personHouseholdMap.put(newPid, newHhId); //Map the new person to the new household
					Person P = new PersonImpl(newPid);
					scenario.getPopulation().addPerson(P);
					personsAdded++;
					
					//Copy original's set of trips
					HashSet<Id> newTrips = new HashSet<Id>();
					for (Id j : personTripsMap.get(pid)) newTrips.add(j);
					personTripsMap.put(newPid, newTrips);
				}
			}
		}
		
		log.info(personsAdded + " persons created, increasing the final population to " + (personsAdded + originalPopulation) + " from " + originalPopulation);	
	}
	

	private void assignActsToNearestLink(){
		
		log.info("Assigning activities to nearest link...");
		
		//Open network file.
		String networkFileName = "";
		JFileChooser fc = new JFileChooser();
		fc.setFileFilter(new FileFilter() {
			@Override
			public String getDescription() {return "MATSim network in *.xml format.";}
			@Override
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().toLowerCase(Locale.ROOT).endsWith( ".xml" );
			}
		});	
		fc.setDialogTitle("Please select a network file");
		int state = fc.showOpenDialog(null);
		if (state == JFileChooser.APPROVE_OPTION){
			networkFileName = fc.getSelectedFile().getAbsolutePath();
		}else if (state == JFileChooser.CANCEL_OPTION) return;
		if (networkFileName == "" || networkFileName == null) return;
		
		//Load the network
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(networkFileName);
		
		
		//Remove highway links and non-car links from the network. DON'T EXPORT!!!
		HashSet<Id> linksToRemove = new HashSet<Id>();
		for (Link l : network.getLinks().values()){
			LinkImpl L = (LinkImpl) l;
			
			if (L.getType() == null) continue; //Assumes that links without a type are OK.
			
			//Highway links (& on/off ramps)
			if (L.getType().equals("Highway") || L.getType().equals("Toll Highway") 
					|| L.getType().equals("On/Off Ramp") || L.getType().equals("Turn") ||
					L.getType().equals("LOOP")) linksToRemove.add(new IdImpl(L.getId().toString()));
			
			//Transit EX-ROW links
			if (!L.getAllowedModes().contains("Car") || !L.getAllowedModes().contains("car")) linksToRemove.add(L.getId());
			
		}
		for (Id i : linksToRemove) network.removeLink(i);
		
		//Remove nodes with degree 0;
		HashSet<Id> nodestoRemove = new HashSet<Id>();
		for (Node N : network.getNodes().values()){
			if (N.getInLinks().size() == 0 && N.getOutLinks().size() == 0) nodestoRemove.add(N.getId());
		}
		for (Id i : nodestoRemove) network.removeNode(i);
		
		/*
		MultimodalNetworkCleaner mmnc = new MultimodalNetworkCleaner(network);
		HashSet<String> modes = new HashSet<String>();
		modes.add("Subway"); modes.add("Car"); modes.add("Train"); modes.add("Streetcar"); modes.add("Bus");
		modes.add("Walk");
		mmnc.run(modes);
		*/
		
		//Assign activities to nearest node.
		for (Person P : scenario.getPopulation().getPersons().values()){
			PlanImpl plan = (PlanImpl) P.getSelectedPlan();
			
			ActivityImpl a = (ActivityImpl) plan.getFirstActivity();
			Link nearestLink = network.getNearestLink(a.getCoord());
			
			a.setLinkId(nearestLink.getId());
		}
	}
	
	/**
	 * This is the primary logic of this class. Converts an ordered list of trips into a person's activity plan.
	 * 
	 */
	private void run(){
		
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
			
			//TODO remove this once mixed mode trips are implemented.
			if (personsWithMixedModeTrips.contains(P.getId())) continue;
			
			
			ArrayList<Trip> pTrips = new ArrayList<CreatePlansFromTrips.Trip>();
			for (Id i : personTripsMap.get(P.getId())) pTrips.add(trips.get(i));
			sortTrips(pTrips);
			
			PlanImpl p = new PlanImpl();
			Trip T = null;
			
			HashMap<String, Coord> workplaceZoneMap = new HashMap<String, Coord>();
			HashMap<String, Coord> schoolZoneMap = new HashMap<String, Coord>();
			
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
				}else if (act_o.equals("W")){ //Work activity
					//Allows activity episodes occurring in the same zone to be assigned the same coordinate. Only applies to work or school activities.
					String workZone = T.zone_o;
					if (!workplaceZoneMap.containsKey(workZone)) workplaceZoneMap.put(workZone, getRandomCoordInZone(new IdImpl(T.zone_o)));
					c = workplaceZoneMap.get(workZone);
					
				}else if (act_o.equals("S")){ //School activity
					String schoolZone = T.zone_o;
					if (!schoolZoneMap.containsKey(schoolZone)) schoolZoneMap.put(schoolZone, getRandomCoordInZone(new IdImpl(T.zone_o)));
					c = schoolZoneMap.get(schoolZone);
					
				}else{
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
					c = getRandomCoordInZone(new IdImpl(T.zone_d));
					householdCoords.put(personHouseholdMap.get(P.getId()), c);
				}
			}else if (act_d.equals("W")){ //Work activity
				//Allows activity episodes occurring in the same zone to be assigned the same coordinate. Only applies to work or school activities.
				String workZone = T.zone_d;
				if (!workplaceZoneMap.containsKey(workZone)) workplaceZoneMap.put(workZone, getRandomCoordInZone(new IdImpl(T.zone_d)));
				c = workplaceZoneMap.get(workZone);
				
			}else if (act_d.equals("S")){ //School activity
				String schoolZone = T.zone_d;
				if (!schoolZoneMap.containsKey(schoolZone)) schoolZoneMap.put(schoolZone, getRandomCoordInZone(new IdImpl(T.zone_d)));
				c = schoolZoneMap.get(schoolZone);
				
			}else{
				c = getRandomCoordInZone(new IdImpl(T.zone_d));
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
	
	/**
	 * "Pulls" a random coordinate within a circle around a zone's centroid. 
	 * @param zoneId
	 * @return
	 */
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
	
	private void sortTrips(List<Trip> a){
		Collections.sort(a);
	}	
	
	/**
	 * Categorizes a trip-chain. Currently, the categories are as follows:
	 *  5 - could not find zone
	 *  4 - only one trip in the trip-chain
	 *  3 - activity location/type mismatch between two subsequent trips
	 *  2 - (not used) final activity != first activity
	 *  1 - does not have a home activity
	 *  0 - none of the above (ie, trip chain is OK) 
	 * 
	 * @param a
	 * @return
	 */
	private int checkTripChain(List<Trip> a){
		
		for (Trip T : a) if ((zones.getLocation(new IdImpl(T.zone_o)) == null) 
				|| zones.getLocation(new IdImpl(T.zone_d)) == null) return 5; //could not find zone.
		
		if (a.size() == 1) return 4; //only one trip in trip chain
		
		//String firstActivity = a.get(0).act_o;
		String nextActivity = a.get(0).act_d;
		boolean hasHomeEpisode = false;
		
		int i = 1;
		for (; i < a.size(); i++){
			if (!a.get(i).act_o.equals(nextActivity)) return 3; //Mismatch between two subsequent trip-chains
			if (a.get(i).act_o.equals("H") || a.get(i).act_d.equals("H")) hasHomeEpisode = true;
			nextActivity = a.get(i).act_d;
		}
		
		//@deprecated: Check that last activity == first activity. This should not be an issue
		//if (!a.get(i - 1).act_d.equals(firstActivity)) return 2; //first activity != last activity
		
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
				
		////////////////////////
		converter.expandPopulation();
		////////////////////////
		converter.run();
		////////////////////////
		//converter.assignActsToNearestLink();
		//////////////////////
		
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
		
		//Additional attributes for mixed-mode trips?
		public String zone_a; //access zone to transit
		public String zone_e; //egress zone from transit
		public String mode_a; //access mode to transit
		public String mode_e; //egress mode from transit
		
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
