package ft.utils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import analysis.experiencedTrips.DrtEventsReader;
import analysis.experiencedTrips.DrtPtTripEventHandler;
import analysis.experiencedTrips.ExperiencedTripsWriter;

public class ModalShiftAnalysis  {

public static void main(String[] args) {
	boolean createexpTripsFile=true;
	
	//Path to base Szenario (Ist-Fall)
	String baseCaseDirectory="D:\\Thiel\\Programme\\MatSim\\01_HannoverModel_2.0\\Simulation\\output\\vw234_nocad.0.1\\";
	String baseCaseRunId="vw234_nocad.0.1.";
	
	//Create ExperiencedTrips file for base Szenario
	if (createexpTripsFile==true) {
	RunExperiencedTripsAnalysisBase(baseCaseDirectory,baseCaseRunId);
	}
	
	//Path to plan Szenario (Plan-Fall)
	String PlanRunDirectory="D:\\Thiel\\Programme\\MatSim\\03_HannoverDRT\\Output\\Moia_1\\";
	String PlanRunId = "Moia_1.";
	
	//Create ExperiencedTrips file for plan Szenario
	if (createexpTripsFile==true) {
	RunExperiencedTripsAnalysisPlan(PlanRunDirectory,PlanRunId);
	}
		
}


public static void RunExperiencedTripsAnalysisBase(String runDirectory, String runId) {
	
//	String runDirectory = "D:\\Thiel\\Programme\\MatSim\\03_HannoverDRT\\Output\\Moia_1\\";
//	String runId = "Moia_1.";
	String runPrefix = runDirectory+"\\"+runId;
	
	Set<String> monitoredModes = new HashSet<>();
	monitoredModes.add("pt");
	monitoredModes.add("drt");
	monitoredModes.add("drt_walk");
	monitoredModes.add("transit_walk");
	monitoredModes.add("egress_walk");
	monitoredModes.add("access_walk");
	monitoredModes.add("car");
	monitoredModes.add("bike");
	monitoredModes.add("ride");
	monitoredModes.add("walk");
	
	
	Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	new MatsimNetworkReader(scenario.getNetwork()).readFile(runPrefix+"output_network.xml.gz");
	new TransitScheduleReader(scenario).readFile(runPrefix+"output_transitSchedule.xml.gz");
	
			
	// Analysis
	EventsManager events = EventsUtils.createEventsManager();
	
	
	Set<Id<Link>> monitoredStartAndEndLinks = new HashSet<>();
	
	DrtPtTripEventHandler eventHandler = new DrtPtTripEventHandler(scenario.getNetwork(), scenario.getTransitSchedule(), 
			monitoredModes, monitoredStartAndEndLinks);
	events.addHandler(eventHandler);
	new DrtEventsReader(events).readFile(runPrefix + "output_events.xml.gz");
	System.out.println("Start writing trips of " + eventHandler.getPerson2ExperiencedTrips().size() + " agents.");
	ExperiencedTripsWriter tripsWriter = new ExperiencedTripsWriter(runPrefix+
			"experiencedTrips.csv", 
			eventHandler.getPerson2ExperiencedTrips(), monitoredModes,scenario.getNetwork());
	tripsWriter.writeExperiencedTrips();
	ExperiencedTripsWriter legsWriter = new ExperiencedTripsWriter(runPrefix + 
			"experiencedLegs.csv", 
			eventHandler.getPerson2ExperiencedTrips(), monitoredModes,scenario.getNetwork());
	legsWriter.writeExperiencedLegs();
}
public static void RunExperiencedTripsAnalysisPlan(String runDirectory, String runId) {
	
//	String runDirectory = "D:\\Thiel\\Programme\\MatSim\\03_HannoverDRT\\Output\\Moia_1\\";
//	String runId = "Moia_1.";
	String runPrefix = runDirectory+"\\"+runId;
	
	Set<String> monitoredModes = new HashSet<>();
	monitoredModes.add("pt");
	monitoredModes.add("drt");
	monitoredModes.add("drt_walk");
	monitoredModes.add("transit_walk");
	monitoredModes.add("egress_walk");
	monitoredModes.add("access_walk");
	monitoredModes.add("car");
	monitoredModes.add("bike");
	monitoredModes.add("ride");
	monitoredModes.add("walk");
	
	
	Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	new MatsimNetworkReader(scenario.getNetwork()).readFile(runPrefix+"output_network.xml.gz");
	new TransitScheduleReader(scenario).readFile(runPrefix+"output_transitSchedule.xml.gz");
	
			
	// Analysis
	EventsManager events = EventsUtils.createEventsManager();
	
	
	Set<Id<Link>> monitoredStartAndEndLinks = new HashSet<>();
	
	DrtPtTripEventHandler eventHandler = new DrtPtTripEventHandler(scenario.getNetwork(), scenario.getTransitSchedule(), 
			monitoredModes, monitoredStartAndEndLinks);
	events.addHandler(eventHandler);
	new DrtEventsReader(events).readFile(runPrefix + "output_events.xml.gz");
	System.out.println("Start writing trips of " + eventHandler.getPerson2ExperiencedTrips().size() + " agents.");
	ExperiencedTripsWriter tripsWriter = new ExperiencedTripsWriter(runPrefix+
			"experiencedTrips.csv", 
			eventHandler.getPerson2ExperiencedTrips(), monitoredModes,scenario.getNetwork());
	tripsWriter.writeExperiencedTrips();
	ExperiencedTripsWriter legsWriter = new ExperiencedTripsWriter(runPrefix + 
			"experiencedLegs.csv", 
			eventHandler.getPerson2ExperiencedTrips(), monitoredModes,scenario.getNetwork());
	legsWriter.writeExperiencedLegs();
}
}