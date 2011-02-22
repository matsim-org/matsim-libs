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

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.droeder.DaFileReader;
import playground.droeder.DaPaths;

/**
 * @author droeder
 *
 */
public class Journal2Matsim2Journal {
	private static final Logger log = Logger
			.getLogger(Journal2Matsim2Journal.class);
	
	private static String homeDir = DaPaths.OUTPUT + "journals/";
	
	private String inFile = null;
	private String networkFile = null;
	private String plansUnrouted = null;
	private String schedule = null;
	private String vehicles = null;
	private String configFile = null;
	private String dir = null;
	private String splitByExpr = null;
	
	private boolean hasHeader;
	
	private Set<String[]> inFileContent;
	private Map<String, Set<String[]>> person2ways;
	
	public static void main(String[] args){
		Journal2Matsim2Journal j = new Journal2Matsim2Journal(homeDir + "dummy.csv", ";", true);
		j.run(homeDir + "multimodalnetwork.xml", homeDir + "transitVehicles.xml", homeDir + "transitSchedule", DaPaths.OUTPUT + "journals/");
		
	}
	
	public Journal2Matsim2Journal(String journalFile, String splitByExpr, boolean hasHeader){
		this.inFile = journalFile;
		this.splitByExpr = splitByExpr;
		this.hasHeader = hasHeader;
	}
	
	public void run(String networkFile, String vehicles, String schedule, String outputDirectory){
		this.dir = outputDirectory;
		this.networkFile = networkFile;
		this.schedule = schedule;
		this.vehicles = vehicles;
		this.plansUnrouted = this.dir + "plans_unrouted.xml";
		this.configFile = this.dir + "config.xml";
		
		this.readJournal();
		this.sortWays();
		this.generatePlans();
		
		createScenario();
//		this.runMobSim();
//		this.writeNewJournal();
	}


	private void readJournal() {
		this.inFileContent = DaFileReader.readFileContent(this.inFile, this.splitByExpr, this.hasHeader);
	}
	
	private void sortWays() {
		this.person2ways = new HashMap<String, Set<String[]>>();
		String id;
		for(String[] way : this.inFileContent){
			id = way[2];
			
			if(this.person2ways.containsKey(id)&& !(way[23].equals("9999999")) && !(way[181].equals("9999999"))){
				this.person2ways.get(id).add(way);
			}else if(!(way[23].equals("9999999")) && !(way[181].equals("9999999"))){
				this.person2ways.put(id, new LinkedHashSet<String[]>());
				this.person2ways.get(id).add(way);
			}else{
				log.error("wayId " + way[1] + "not processed, because no coordinates are given!"); 
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
		
		double startTime;
		double endTime;

		for(Entry<String, Set<String[]>> e : this.person2ways.entrySet()){
			personCar = fac.createPerson(sc.createId(e.getKey() + "_" + TransportMode.car.toString()));
			planCar = fac.createPlan(); 
				
			personPt = fac.createPerson(sc.createId(e.getKey() + "_" + TransportMode.pt.toString()));
			planPt = fac.createPlan();
			
			String[] way;
			Iterator<String[]> it = e.getValue().iterator();
			boolean addPerson = true;
			do{
				way = it.next();
				
				startTime = Double.valueOf(way[27]);
				endTime = Double.valueOf(way[184]);
				
				startCoord = new CoordImpl(way[24], way[25]);
				endCoord = new CoordImpl(way[182], way[183]);
				
				
				//add first 
				if(planCar.getPlanElements().size() == 0){				
					// TODO get Type from file
					type = "test";
					addActivity(planCar, startCoord, startTime, fac, type);
					addActivity(planPt, startCoord, startTime, fac, type);
				}
				// check last activity, must be an activity
				else if(planCar.getPlanElements().get(planCar.getPlanElements().size()-1) instanceof Leg){
					addPerson = false;
					log.error("Person ID " + e.getKey() + " was not added, because of a missing planElement!"); 
					break;
				}
				
				addLeg(planCar, startTime, TransportMode.car, fac);
				addLeg(planPt, startTime, TransportMode.pt, fac);
				
				// TODO get Type from file
				type = "test";
				addActivity(planCar, endCoord, endTime, fac, type);
				addActivity(planPt, endCoord, endTime, fac, type);
				
			}while(it.hasNext());
			
			if(addPerson){
				personCar.addPlan(planCar);
				sc.getPopulation().addPerson(personCar);
				
				personPt.addPlan(planPt);
				sc.getPopulation().addPerson(personPt);
			}
		}
		
		new PopulationWriter(sc.getPopulation(), null).writeV4(plansUnrouted);
	}

	private void addActivity(Plan plan, Coord c, double time, PopulationFactory fac, String type) {
		Activity a = fac.createActivityFromCoord(type, c);
		a.setStartTime(time);
//		a.setEndTime(time);
		plan.addActivity(a);
	}
	
	private void addLeg(Plan plan, double start, String mode, PopulationFactory fac) {
		Leg l = fac.createLeg(mode);
		l.setDepartureTime(start);
		plan.addLeg(l);
	}

	
	/**
	 * 
	 */
	private void createScenario() {
//		Scenario sc = new ScenarioLoaderImpl(this.homeDir + "config_0.xml").loadScenario();
//		
//		sc.getConfig().controler().setFirstIteration(0);
//		sc.getConfig().controler().setLastIteration(0);
//		sc.getConfig().controler().setOutputDirectory(this.dir);
//
//		sc.getConfig().network().setInputFile(this.networkFile);
//		new ScenarioLoaderImpl(sc).loadNetwork();
//		
//		sc.getConfig().plans().setInputFile(this.plansUnrouted);
//		new ScenarioLoaderImpl(sc).loadPopulation();
//		
//		
//		new ConfigWriter(sc.getConfig()).write(this.configFile);
		
		
//		Scenario s = new ScenarioLoaderImpl("./src/main/java/playground/droeder/Journal2MATSim2Journal/config.xml");
//		Config c = s.getConfig();
		
		
//		c.addModule(GlobalConfigGroup.GROUP_NAME, new GlobalConfigGroup());
//		c.getModule(GlobalConfigGroup.GROUP_NAME).addParam("coordinateSystem", "Atlantis");
//		c.getModule(GlobalConfigGroup.GROUP_NAME).addParam("randomSeed", "4711");
//		
//		c.addModule(NetworkConfigGroup.GROUP_NAME, new NetworkConfigGroup());
//		c.getModule(NetworkConfigGroup.GROUP_NAME).addParam("inputNetworkFile", this.networkFile);
//		
//		c.addModule(PlansConfigGroup.GROUP_NAME, new PlansConfigGroup());
//		c.getModule(PlansConfigGroup.GROUP_NAME).addParam("inputPlansFile", this.plansUnrouted);

//		c.controler().setFirstIteration(0);
//		c.controler().setLastIteration(0);
//		c.controler().setOutputDirectory(this.dir);
//			Set<EventsFileFormat> eventsFormat = new TreeSet<EventsFileFormat>();
//			eventsFormat.add(EventsFileFormat.xml);
//		c.controler().setEventsFileFormats(eventsFormat);
		
//		c.addModule(QSimConfigGroup.GROUP_NAME, new QSimConfigGroup());
//		c.getModule(QSimConfigGroup.GROUP_NAME).addParam("startTime", "00:00:00");
//		c.getModule(QSimConfigGroup.GROUP_NAME).addParam("endTime", "30:00:00");
//		c.getModule(QSimConfigGroup.GROUP_NAME).addParam("snapshotperiod", "00:00:00");
//		c.getModule(QSimConfigGroup.GROUP_NAME).addParam("snapshotFormat", "otfvis");
		
//		c.strategy().setMaxAgentPlanMemorySize(1);
//		c.strategy().addParam("ModuleProbability_1", "1");
//		c.strategy().addParam("Module_1", "Best_Score");
//		
//		c.getModule("transit").addParam("transitScheduleFile", this.schedule);
//		c.getModule("transit").addParam("vehiclesFile", this.vehicles);
//		c.getModule("transit").addParam("transitModes", "pt");
		
		
//		new ConfigWriter(c).write(this.configFile);
	}
	
	/**
	 * 
	 */
	private void runMobSim() {
		// TODO Auto-generated method stub
		
	}

	/**
	 * 
	 */
	private void writeNewJournal() {
		// TODO Auto-generated method stub
		
	}
	
	

}
