/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.droeder.Journal2MATSim2Journal;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.PersonEvent;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.ControlerConfigGroup.EventsFileFormat;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.run.Controler;
import org.xml.sax.SAXException;

import playground.droeder.DaFileReader;
import playground.droeder.DaPaths;


// TODO refactor cap of Network/Vehicles and vehicleEnterTimes

/**
 * @author droeder
 *
 */
public class Journal2Matsim2Journal {
	private static final Logger log = Logger
			.getLogger(Journal2Matsim2Journal.class);
	
	private static final String INPUTDIR = DaPaths.OUTPUT + "journals/";
	
	private String inFile = null;
	private String networkFile = null;
	private String plansUnrouted = null;
	private String schedule = null;
	private String vehicles = null;
	private String configFile = null;
	private String outDir = null;
	private String splitByExpr = null;
	
	private double startTime = Double.MAX_VALUE;
	
	private boolean inFileHasHeader;
	private String[] inFileHeader;
	private Set<String[]> inFileContent;
	
	
	private Map<String, Set<String[]>> person2waysOld;
	private Map<Id, Set<PersonEvent>> person2Event;
	private Map<String, Set<List<PersonEvent>>> carId2TripEvents;
	private Map<String, Set<List<PersonEvent>>> ptId2TripEvents;
	
	private final String ACT_0 = "home";
	private final String ACT_1 = "work";
	private final String ACT_2 = "other";
	
	public static void main(String[] args){
		Journal2Matsim2Journal j = new Journal2Matsim2Journal(INPUTDIR + "dummy.csv", ";", true);
		j.run(INPUTDIR + "multimodalnetwork.xml", INPUTDIR + "transitVehicles.xml", INPUTDIR + "transitSchedule.xml", INPUTDIR);
		
	}
	
	public Journal2Matsim2Journal(String journalFile, String splitByExpr, boolean hasHeader){
		this.inFile = journalFile;
		this.splitByExpr = splitByExpr;
		this.inFileHasHeader = hasHeader;
	}
	
	public void run(String networkFile, String vehicles, String schedule, String outputDirectory){
		this.outDir = outputDirectory;
		this.networkFile = networkFile;
		this.schedule = schedule;
		this.vehicles = vehicles;
		this.plansUnrouted = this.outDir + "plans_unrouted.xml";
		this.configFile = this.outDir + "config.xml";

//		this.readJournal();
//		this.sortWaysByPerson();
//		this.generatePlans();
//		
//		this.createAndWriteConfig();
//		
//		this.runQSim();
		
		this.readEventsFile();
		this.sortTripEventsByMode();
		this.analyzeEvents();
		
//		this.writeNewJournal();
	}


	private void readJournal() {
		this.inFileHeader = DaFileReader.readFileHeader(this.inFile, this.splitByExpr);
		this.inFileContent = DaFileReader.readFileContent(this.inFile, this.splitByExpr, this.inFileHasHeader);
	}
	
	private void sortWaysByPerson() {
		this.person2waysOld = new TreeMap<String, Set<String[]>>();
		String id;
		for(String[] way : this.inFileContent){
			id = way[2];
			
			if(this.person2waysOld.containsKey(id)&& !(way[23].equals("9999999")) && !(way[181].equals("9999999"))){
				this.person2waysOld.get(id).add(way);
			}else if(!(way[23].equals("9999999")) && !(way[181].equals("9999999"))){
				this.person2waysOld.put(id, new LinkedHashSet<String[]>());
				this.person2waysOld.get(id).add(way);
			}else{
				log.error("wayId " + way[1] + "not processed, because no coordinates are given for origin and/or destination!"); 
			}
		}
	}
	
	
	private void generatePlans(){
		Scenario sc = new ScenarioImpl();
		PopulationFactory fac = sc.getPopulation().getFactory();
		Person personCar;
		Person personPt;
		Plan planCar;
		Plan planPt;
		Coord startCoord;
		Coord endCoord;
		String type;
		
		double tripStartTime;
		double tripEndTime;

		for(Entry<String, Set<String[]>> e : this.person2waysOld.entrySet()){
			personCar = fac.createPerson(sc.createId(e.getKey() + "_" + TransportMode.car.toString()));
			planCar = fac.createPlan(); 
				
			personPt = fac.createPerson(sc.createId(e.getKey() + "_" + TransportMode.pt.toString()));
			planPt = fac.createPlan();
			
			String[] way;
			Iterator<String[]> it = e.getValue().iterator();
			boolean addPerson = true;
			do{
				way = it.next();
				
				tripStartTime = Double.valueOf(way[27]);
					if(this.startTime > tripStartTime) this.startTime = tripStartTime;
				tripEndTime = Double.valueOf(way[184]);
				
				
				// TODO remove randomizer, it's just for the test
				startCoord = new CoordImpl(Math.random()* 4000, Math.random() * 4000);
				endCoord = new CoordImpl(Math.random()* 4000, Math.random() * 4000);
//				startCoord = new CoordImpl(way[24], way[25]);
//				endCoord = new CoordImpl(way[182], way[183]);
				
				
				//add first planelement
				if(planCar.getPlanElements().size() == 0){		
					// TODO doesn't work
					if(way[17] == ("1")) {
						type = this.ACT_0;
					}else{
						type = this.ACT_2;
					}
					addActivity(planCar, startCoord, null, tripStartTime, fac, type);
					addActivity(planPt, startCoord, null, tripStartTime, fac, type);
				}
				// check last planelement, must be an activity
				else if(planCar.getPlanElements().get(planCar.getPlanElements().size()-1) instanceof Leg){
					addPerson = false;
					break;
				}
				// if not first activity and not leg add startTime of the current trip to the former activity
				else{
					((Activity) planCar.getPlanElements().get(planCar.getPlanElements().size() - 1)).setEndTime(tripStartTime);
					((Activity) planPt.getPlanElements().get(planPt.getPlanElements().size() - 1)).setEndTime(tripStartTime);
				}
				
				addLeg(planCar, tripStartTime, TransportMode.car, fac);
				addLeg(planPt, tripStartTime, TransportMode.pt, fac);
				
				// TODO doesn't work
				if(way[175] == ("1")){
					type = this.ACT_0;
				}else if(way[28] == ("1")){
					type = this.ACT_1;
				}else{
					type = this.ACT_2;
				}
				addActivity(planCar, endCoord, tripEndTime, null, fac, type);
				addActivity(planPt, endCoord, tripEndTime, null, fac, type);
				
			}while(it.hasNext());
			
			//handle Last Activity
			((Activity) planCar.getPlanElements().get(planCar.getPlanElements().size() - 1)).setEndTime(tripEndTime * 1.1);
			((Activity) planPt.getPlanElements().get(planPt.getPlanElements().size() - 1)).setEndTime(tripEndTime * 1.1);
			
			// add only if all ways have an start and endtime
			if(addPerson){
				personCar.addPlan(planCar);
				sc.getPopulation().addPerson(personCar);
				
				personPt.addPlan(planPt);
				sc.getPopulation().addPerson(personPt);
			}else{
				log.error("Person ID " + e.getKey() + " was not added, because of a missing start- or endtime!"); 
			}
		}
		
		new PopulationWriter(sc.getPopulation(),null).writeV4(plansUnrouted);
	}

	private void addActivity(Plan plan, Coord c, Double startTime, Double endTime, PopulationFactory fac, String type) {
		Activity a = fac.createActivityFromCoord(type, c);
		
		if(startTime == null){
			a.setStartTime(endTime * 0.9);
		}else{
			a.setStartTime(startTime);
		}
		
		if(!(endTime == null)){
			a.setEndTime(endTime);
		}
		
		plan.addActivity(a);
	}
	
	private void addLeg(Plan plan, double start, String mode, PopulationFactory fac) {
		Leg l = fac.createLeg(mode);
		l.setDepartureTime(start);
		plan.addLeg(l);
	}

	private void createAndWriteConfig() {
		Scenario s = new ScenarioImpl();
		Config c = s.getConfig();
		
		c.getModule(GlobalConfigGroup.GROUP_NAME).addParam("coordinateSystem", "Atlantis");
		c.getModule(GlobalConfigGroup.GROUP_NAME).addParam("randomSeed", "4711");
		
		c.getModule(NetworkConfigGroup.GROUP_NAME).addParam("inputNetworkFile", this.networkFile);
		
		c.getModule(PlansConfigGroup.GROUP_NAME).addParam("inputPlansFile", this.plansUnrouted);
		
		c.controler().setFirstIteration(0);
		c.controler().setLastIteration(0);
		c.controler().setOutputDirectory(this.outDir);
			Set<EventsFileFormat> eventsFormat = new TreeSet<EventsFileFormat>();
			eventsFormat.add(EventsFileFormat.xml);
			c.controler().setEventsFileFormats(eventsFormat);
		c.controler().setMobsim(QSimConfigGroup.GROUP_NAME);
		
		
		c.addQSimConfigGroup(new QSimConfigGroup());
		c.getQSimConfigGroup().setStartTime(this.startTime);
		c.getQSimConfigGroup().setEndTime(108000);
		c.getQSimConfigGroup().setSnapshotPeriod(0);
		c.getQSimConfigGroup().setSnapshotFormat("otfvis");
		
		c.getModule(StrategyConfigGroup.GROUP_NAME).addParam("maxAgentPlanMemorySize", "1");
		c.getModule(StrategyConfigGroup.GROUP_NAME).addParam("ModuleProbability_1", "1");
		c.getModule(StrategyConfigGroup.GROUP_NAME).addParam("Module_1", "BestScore");
		
		c.getModule(PlanCalcScoreConfigGroup.GROUP_NAME).addParam("activityType_0"  , this.ACT_0);
		c.getModule(PlanCalcScoreConfigGroup.GROUP_NAME).addParam("activityTypicalDuration_0"  , "12:00:00");
		
		c.getModule(PlanCalcScoreConfigGroup.GROUP_NAME).addParam("activityType_1"  , this.ACT_1);
		c.getModule(PlanCalcScoreConfigGroup.GROUP_NAME).addParam("activityTypicalDuration_1"  , "08:00:00");
		
		c.getModule(PlanCalcScoreConfigGroup.GROUP_NAME).addParam("activityType_2"  , this.ACT_2);
		c.getModule(PlanCalcScoreConfigGroup.GROUP_NAME).addParam("activityTypicalDuration_2"  , "01:00:00");
		
		
		c.getModule(TransitConfigGroup.GROUP_NAME).addParam("transitScheduleFile", this.schedule);
		c.getModule(TransitConfigGroup.GROUP_NAME).addParam("vehiclesFile", this.vehicles);
		c.getModule(TransitConfigGroup.GROUP_NAME).addParam("transitModes", "pt");
		
		c.scenario().setUseTransit(true);
		c.scenario().setUseVehicles(true);
		
		new ConfigWriter(c).write(this.configFile);
	}

	private void runQSim() {
		Controler c = new Controler(this.configFile);
		c.setOverwriteFiles(true);
		c.run();
	}

	
	private void readEventsFile(){
		EventsManager events = new EventsManagerImpl();
		TripDurationHandler handler = new TripDurationHandler();
		events.addHandler(handler);
		
		try {
			new EventsReaderXMLv1(events).parse(this.outDir + "ITERS/it.0/0.events.xml.gz");
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		this.person2Event = handler.getId2Event();
	}
	
	private void sortTripEventsByMode(){
		this.carId2TripEvents = new HashMap<String, Set<List<PersonEvent>>>();
		this.ptId2TripEvents = new HashMap<String, Set<List<PersonEvent>>>();
		
		for(Entry<Id, Set<PersonEvent>> entry : this.person2Event.entrySet()){
			boolean start = true;
			String[] id = entry.getKey().toString().split("_");
			
			if(id[1].equals("pt")){
				this.ptId2TripEvents.put(id[0], new LinkedHashSet<List<PersonEvent>>());
			}else{
				this.carId2TripEvents.put(id[0], new LinkedHashSet<List<PersonEvent>>());
			}
			
			List<PersonEvent> temp = null;
			for(PersonEvent e : entry.getValue()){
				if(start){
					temp = new LinkedList<PersonEvent>();
					start = false;
				}
				temp.add(e);
				
				if(e instanceof ActivityStartEvent && !((ActivityStartEvent)e).getActType().equals("pt interaction")){
					start = true;
					if(id[1].equals("pt")){
						this.ptId2TripEvents.get(id[0]).add(temp);
					}else{
						this.carId2TripEvents.get(id[0]).add(temp);
					}
				}
			}
		}
		
		
//		for(Entry<String, Set<List<PersonEvent>>> e : this.carId2TripEvents.entrySet()){
//			System.out.println(e.getKey());
//			for(List<PersonEvent> list : e.getValue()){
//				for(PersonEvent ev: list){
//					System.out.println(ev);
//				}
//				System.out.println();
//			}
//			System.out.println();
//			System.out.println();
//		}
//		
//		for(Entry<String, Set<List<PersonEvent>>> e : this.ptId2TripEvents.entrySet()){
//			System.out.println(e.getKey());
//			for(List<PersonEvent> list : e.getValue()){
//				for(PersonEvent ev: list){
//					System.out.println(ev);
//				}
//				System.out.println();
//			}
//			System.out.println();
//			System.out.println();
//		}
	}
	
	private void analyzeEvents(){
		double travelTime, transitTime, waitingTime;
		
		for(Entry<String, Set<List<PersonEvent>>> e :this.carId2TripEvents.entrySet()){
			
			
			
			// get TravelTime for car
			for(List<PersonEvent> list : e.getValue()){
				travelTime = 0;
				travelTime = ((LinkedList<PersonEvent>) list).getLast().getTime() - ((LinkedList<PersonEvent>) list).getFirst().getTime();
				System.out.print(travelTime + "\t");
			}
			
			
			// get times for pt
			for(List<PersonEvent> list : this.ptId2TripEvents.get(e.getKey())){
				travelTime = 0;
				transitTime = 0;
				waitingTime = 0;

				travelTime = ((LinkedList<PersonEvent>) list).getLast().getTime() - ((LinkedList<PersonEvent>) list).getFirst().getTime();
				
				for(PersonEvent pe : list){
					if(pe instanceof ActivityStartEvent){
						if(((ActivityStartEvent) pe).getActType().equals("pt interaction")){
							waitingTime = waitingTime - pe.getTime();
						}
					}else if (pe instanceof ActivityEndEvent){
						if(((ActivityEndEvent) pe).getActType().equals("pt interaction")){
							waitingTime = waitingTime + pe.getTime();
						}
					}else if (pe instanceof AgentDepartureEvent){
						if(((AgentDepartureEvent)pe).getLegMode().equals(TransportMode.transit_walk)){
							transitTime = transitTime - pe.getTime();
						}
					}else if (pe instanceof AgentArrivalEvent){
						if(((AgentArrivalEvent)pe).getLegMode().equals(TransportMode.transit_walk)){
							transitTime = transitTime + pe.getTime();
						}
					}
				}
				System.out.print(travelTime + "\t");
				System.out.print(waitingTime + "\t");
				System.out.print(transitTime + "\t");
			}
			System.out.println();
		}
	}
	
	private void writeNewJournal() {
		// TODO Auto-generated method stub
		
	}
}

class Trip{
	
	public Trip(String[] oldTrip){

	}
	
	public void setPtTime(double travelTime, double transitTime, double waitingTime){
		
	}
	
	public void setCarTime(double travelTime){
		
	}
}
