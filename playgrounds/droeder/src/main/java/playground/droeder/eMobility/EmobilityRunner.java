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
package playground.droeder.eMobility;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;

import playground.droeder.DRPaths;
import playground.droeder.eMobility.energy.ChargingProfiles;
import playground.droeder.eMobility.energy.DisChargingProfiles;
import playground.droeder.eMobility.handler.EmobPersonHandler;
import playground.droeder.eMobility.handler.EmobVehicleDrivingHandler;
import playground.droeder.eMobility.io.EmobEnergyProfileReader;
import playground.droeder.eMobility.subjects.EmobilityPerson;

/**
 * @author droeder
 *
 */
public class EmobilityRunner {
	
	private Scenario scenario;
//	private EventsManager manager;
	private Map<Id, EmobilityPerson> persons;
	private ChargingProfiles charging;
	private DisChargingProfiles discharging;
	private List<EventHandler> handler;
	
	private Integer cnt = 0;
	/**
	 * 
	 */
	public EmobilityRunner(String configFile) {
		Config c = ConfigUtils.loadConfig(configFile);
		this.scenario = ScenarioUtils.loadScenario(c);
//		this.manager = EventsUtils.createEventsManager();
		this.handler = new ArrayList<EventHandler>();
		this.persons = new HashMap<Id, EmobilityPerson>();
	}
	
	public void createAndAddPerson(String eMobPlan){
		//TODO 
		EmobilityPerson p = new EmobilityPerson(new IdImpl("emob" + String.valueOf(this.cnt)), new CoordImpl(4588309,5820079), scenario.getNetwork().getNodes().get(new IdImpl("26736131")), 26.0, scenario.getNetwork());
		p.readDataFromFile(eMobPlan);
		this.scenario.getPopulation().addPerson(p.createMatsimPerson(this.scenario.getPopulation().getFactory()));
		this.persons.put(p.getId(), p);
		this.cnt++;
		
//		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
//		new MatsimNetworkRe	ader(sc).readFile(this.scenario.getConfig().getParam(NetworkConfigGroup.GROUP_NAME, "inputNetworkFile"));
//		new MatsimPopulationReader(sc).readFile(eMobPlans);
//		for(Person p: sc.getPopulation().getPersons().values()){
	}
	
	public void loadDisChargingProfiles(String file){
		this.discharging = EmobEnergyProfileReader.readDisChargingProfiles(file);
	}
	
	public void loadChargingProfiles(String file){
		this.charging = EmobEnergyProfileReader.readChargingProfiles(file);
	}
	
	public void run(){
//		this.eHandler = new EmobiltyHandler(this.vehicles, this.charging, this.discharging, this.scenario.getNetwork());
//		this.handler.add(this.eHandler);
		EmobVehicleDrivingHandler h1 =  new EmobVehicleDrivingHandler(discharging, this.scenario.getNetwork());
		EmobPersonHandler h2 = new EmobPersonHandler(persons, h1, charging);
		this.handler.add(h1);
		this.handler.add(h2);
		
		Controler c = new Controler(this.scenario);
		c.addControlerListener(new MyListener(this.handler));
		
		c.setDumpDataAtEnd(true);
		c.setOverwriteFiles(true);
		
		c.run();
	}
	
	//internal class
	private class MyListener implements StartupListener{
		
		
		private List<EventHandler> handler;

		private MyListener(List<EventHandler> handler){
			this.handler = handler;
		}

		/* (non-Javadoc)
		 * @see org.matsim.core.controler.listener.StartupListener#notifyStartup(org.matsim.core.controler.events.StartupEvent)
		 */
		@Override
		public void notifyStartup(StartupEvent event) {
			for(EventHandler h: this.handler){
				event.getControler().getEvents().addHandler(h);
			}
		}
		
	}
	
	/**
	 * @param outdir2
	 */
	public void dumpResults(String outdir) {
		System.out.println("test");
		BufferedWriter writer = IOUtils.getBufferedWriter(outdir + "distance.csv");
		try {
			writer.write("id;distance;charge;\n");
			for(Entry<Id, EmobilityPerson> e: this.persons.entrySet()){
				for(Tuple<Double, Double> ee: e.getValue().getVehicle().getDist2Charge()){
					writer.write(e.getKey() + ";" + ee.getFirst() +";" + ee.getSecond() + ";");
					writer.newLine();
				}
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		writer = IOUtils.getBufferedWriter(outdir + "time.csv");
		try {
			writer.write("id;time;charge;\n");
			for(Entry<Id, EmobilityPerson> e: this.persons.entrySet()){
				for(Tuple<Double, Double> ee: e.getValue().getVehicle().getTime2Charge()){
					writer.write(e.getKey() + ";" + ee.getFirst() +";" + ee.getSecond() + ";");
					writer.newLine();
				}
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private final static String VWDIR = "D:/VSP/svn/shared/volkswagen/";
	private final static String SCENARIO = VWDIR + "80testdaten/1/";
	
	private static final String CONFIG = SCENARIO + "config.xml";
	private static final String EMOBPLANS = SCENARIO + "testplan.txt";
	private static final String OUTDIR = SCENARIO + "output/";
	
	private static final String CHARGINGPROFILE = "C:/Users/Daniel/Desktop/Dokumente_MATSim_AP1und2/ChargingLookupTable_2011-11-30.txt";
	private static final String DISCHARGINGPROFILE = "C:/Users/Daniel/Desktop/Dokumente_MATSim_AP1und2/DrivingLookupTable_2011-11-25.txt";

	public static void main(String[] args) {
		EmobilityRunner runner = new EmobilityRunner(CONFIG);
		
		runner.createAndAddPerson(EMOBPLANS);
		runner.loadChargingProfiles(CHARGINGPROFILE);
		runner.loadDisChargingProfiles(DISCHARGINGPROFILE);
		
		runner.run();
		
		runner.dumpResults(OUTDIR);
	}
}
