package playground.toronto.demand;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.toronto.demand.util.TableReader;

/**
 * A new class for creating populations plans from TTS trip records, which 
 * handles the new implementation of transit trips. Cleans up some of the
 * earlier code, which requires the file to be formatted in an ordered
 * way. The two input files can be ordered in any way both, row-wise and
 * column-wise (ie, "hhid,pid,..." is as valid as "pid,hhid,..." and 
 * trips can be in any order). 
 * 
 * @author pkucirek
 *
 */
public class CreatePlansFromTrips {

	private static final Logger log = Logger.getLogger(CreatePlansFromTrips.class);
	private ScenarioImpl scenario;
	private HashMap<Id, Coord> zones;
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
	
	public CreatePlansFromTrips(){
		this.scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
	}
	
	private void readZones(String filename) throws IOException{
		TableReader tr = new TableReader(filename);
		tr.open();
		if (!tr.checkHeaders(zonesFieldNames)) throw new IOException("Zones file not correctly formatted!");
		
		this.zones = new HashMap<Id, Coord>();
		
		while (tr.next()){
			this.zones.put(new IdImpl(tr.current().get("zone_id")), 
					new CoordImpl(tr.current().get("x"), 
							tr.current().get("y")));
		}
		tr.close();
	}
	
	private void readTrips(String filename) throws IOException{
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
					tr.current().get("act_d"), 
					tr.current().get("start_time"), 
					tr.current().get("type"));
			trips.put(tid, T);
			personTripsMap.get(pid).add(tid);

		}

		tr.close();
	}
	
	private void createPlans(){
		
		for (Person P : scenario.getPopulation().getPersons().values()){
			
		}
		
	}
	
	public static void main(String[] args){
		
		CreatePlansFromTrips converter = new CreatePlansFromTrips();
		
		String file = "";
		JFileChooser fc = new JFileChooser();
		fc.setDialogTitle("Please select a zones file");
		fc.setFileFilter(new FileFilter() {
			@Override
			public String getDescription() {return "Table of zones in *.txt format.";}
			@Override
			public boolean accept(File f) {
				// TODO Auto-generated method stub
				return f.isDirectory() || f.getName().toLowerCase(Locale.ROOT).endsWith( ".txt" );
			}
		});
		int state = fc.showOpenDialog(null);
		if (state == JFileChooser.APPROVE_OPTION){
			file = fc.getSelectedFile().getAbsolutePath();
		}else if (state == JFileChooser.CANCEL_OPTION) return;
		
		if (file == "" || file == null) return;
		
		log.info("Opening " + file + " as a zones file.");
		try {
			converter.readZones(file);
			log.info("File opened successfully.");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		
		fc = new JFileChooser();
		fc.setDialogTitle("Please select a trips file");
		fc.setFileFilter(new FileFilter() {
			@Override
			public String getDescription() {return "Table of trip records in *.txt format.";}
			@Override
			public boolean accept(File f) {
				// TODO Auto-generated method stub
				return f.isDirectory() || f.getName().toLowerCase(Locale.ROOT).endsWith( ".txt" );
			}
		});
		state = fc.showOpenDialog(null);
		if (state == JFileChooser.APPROVE_OPTION){
			file = fc.getSelectedFile().getAbsolutePath();
		}else if (state == JFileChooser.CANCEL_OPTION) return;
		
		log.info("Opening " + file + " as a trips file.");
		try {
			converter.readTrips(file);
			log.info("File opened successfully.");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		
	}
	
	private static class Trip{
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
	}
	
}
