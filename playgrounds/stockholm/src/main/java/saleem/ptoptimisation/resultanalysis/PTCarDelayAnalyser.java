package saleem.ptoptimisation.resultanalysis;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.AgentWaitingForPtEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.handler.AgentWaitingForPtEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.utils.CreatePseudoNetwork;
import org.matsim.vehicles.Vehicle;

/**
 * This class is for result analysis, primarily PT traveler delay calculation (waiting times) at PT stops
 * 
 * @author Mohammad Saleem
 */

public class PTCarDelayAnalyser implements BasicEventHandler, AgentWaitingForPtEventHandler, TransitDriverStartsEventHandler, PersonEntersVehicleEventHandler,
PersonStuckEventHandler{ 

	private int totalStuck = 0; int hourlystopexits = 0;
	private double time=-3600, entrytimes=0, exittimes=0; //Required for calculation of delay and plotting

	private Set<Id<Person>> transitDrivers = new HashSet<>();
	private Set<Id<Vehicle>> transitVehicles = new HashSet<>();
	private ArrayList<Double> times = new ArrayList<Double>();//Time entries for plotting
	private ArrayList<Double> delays = new ArrayList<Double>();//Delays for plotting
	private Map<Id<Person>, Double> personStopEntryTimes = new HashMap<>();




	// ---------- IMPLEMENTATION OF *EventHandler INTERFACES ----------

	@Override
	public void reset(final int iteration) {
		if(this.transitDrivers==null) {
			this.transitDrivers = new HashSet<Id<Person>>();
		}else{
			this.transitDrivers.clear();
		}
		if(this.transitVehicles==null) {
			this.transitVehicles = new HashSet<Id<Vehicle>>();
		}else{
			this.transitVehicles.clear();
		}
		if(this.times==null) {
			this.times = new ArrayList<Double>();
		}else{
			this.times.clear();
		}
		if(this.delays==null) {
			this.delays = new ArrayList<Double>();
		}else{
			this.delays.clear();
		}
		if(this.personStopEntryTimes==null) {
			this.personStopEntryTimes = new HashMap<>();
		}else{
			this.personStopEntryTimes.clear();
		}
	}
	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		this.transitDrivers.add(event.getDriverId());
		this.transitVehicles.add(event.getVehicleId());
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (this.transitDrivers.contains(event.getPersonId()) || !this.transitVehicles.contains(event.getVehicleId())) {
			return; // ignore transit drivers or persons entering non-transit vehicles
		}
		this.registerEntry(personStopEntryTimes.get(event.getPersonId()));
		this.registerExit(event.getTime());//Extract arrival time at PT stop. 
		this.personStopEntryTimes.remove(event.getPersonId());//Register his exiting time from PT stop. Delay is: stop exit time - stop entry time
	}
	// TODO Auto-generated method stub
	//Register arrival time at PT stop
	@Override
	public void handleEvent(AgentWaitingForPtEvent event) {
		this.personStopEntryTimes.put(event.getPersonId(), event.getTime());// Register person against the stop
	}
	@Override
	public void handleEvent(PersonStuckEvent event) {
		// Just to check stuck people at the end
		totalStuck++;
	}
	@Override
	public void handleEvent(Event event) {
		if(event.getTime()-time>=3600) {//Average delay stats over hourly bins
			if(hourlystopexits==0)hourlystopexits=1;
			delays.add((exittimes-entrytimes)/hourlystopexits);
			times.add((double)Math.round(event.getTime()/3600));//Counting in hours
			exittimes=0;entrytimes=0;hourlystopexits=0;
			time=(double)Math.round(event.getTime()/3600)*3600;//Current time
		}
	}
	//A traveler entering a PT stop and starting waiting
	public void registerEntry(double time){
		entrytimes = entrytimes + time;
	}
	//A traveler exiting a PT stop (boarding a vehicle) and stopping to wait
	public void registerExit(double time){
		exittimes = exittimes + time;
		hourlystopexits+=1;
	}
	public int getTotalStuckOutsideStops() {
		return this.totalStuck;
	}
	//Load a scenario to analyse
	public PTCarDelayAnalyser loadScenario(String configfile, String eventspath){
		Config config = ConfigUtils.loadConfig(configfile);
	    final Scenario scenario = ScenarioUtils.loadScenario(config);
		new CreatePseudoNetwork(scenario.getTransitSchedule(), scenario.getNetwork(), "tr_").createNetwork();
		final EventsManager eventsmanager = EventsUtils.createEventsManager(scenario.getConfig());
		PTCarDelayAnalyser delayhandler = new PTCarDelayAnalyser();
		eventsmanager.addHandler(delayhandler);
		final MatsimEventsReader reader = new MatsimEventsReader(eventsmanager);
		reader.readFile(eventspath);
		return delayhandler;
	}
	//Write delay stats to path file
	public void writeStatistics(String stats, String path){
		try { 
			File file=new File(path);
		    FileOutputStream fileOutputStream=new FileOutputStream(file);
		    fileOutputStream.write(stats.getBytes());
		    fileOutputStream.close();
	       
	    } catch(Exception ex) {
	        //catch logic here
	    }
	}
	//Change ArrayList to String form
	public String getDelayStatsAsString(){
		String stats="";
		Iterator<Double> hoursiterator = this.times.iterator();
		Iterator<Double> delaysiterator = this.delays.iterator();

		while(hoursiterator.hasNext()){
			stats = stats + hoursiterator.next() + "\t" + delaysiterator.next() + "\n";
		}
		return stats;
	}
	public static void main(String[] args) {
		String configpath = "./ihop2/matsim-input/configoptimisationcarpt.xml";
		String eventspath = "./ihop2/matsim-input/500.events.xml.gz";
		String delaystatspath = "./ihop2/matsim-output/delaysstats.txt";;
		
		PTCarDelayAnalyser analyser = new PTCarDelayAnalyser();
		PTCarDelayAnalyser handler = analyser.loadScenario(configpath, eventspath);
		analyser.writeStatistics(handler.getDelayStatsAsString(), delaystatspath);
		// TODO Auto-generated method stub

	}

}
