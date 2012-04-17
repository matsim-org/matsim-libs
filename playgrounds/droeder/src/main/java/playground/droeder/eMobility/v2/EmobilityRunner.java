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
package playground.droeder.eMobility.v2;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.ScenarioUtils;

import playground.droeder.eMobility.energy.ChargingProfiles;
import playground.droeder.eMobility.energy.DisChargingProfiles;
import playground.droeder.eMobility.energy.EmobEnergyProfileReader;
import playground.droeder.eMobility.v2.handler.EmobPersonHandler;
import playground.droeder.eMobility.v2.fleet.EmobFleet;
import playground.droeder.eMobility.v2.handler.EmobVehicleDrivingHandler;
import playground.droeder.eMobility.v2.population.EmobPerson;
import playground.droeder.eMobility.v2.population.EmobPopulation;

/**
 * @author droeder
 *
 */
public class EmobilityRunner {
	
	private Scenario scenario;
	private EmobPopulation population;
	private EmobFleet fleet;
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
		this.handler = new ArrayList<EventHandler>();
		this.population =  new EmobPopulation();
		this.fleet = new EmobFleet(null);
	}
	
	public void createAndAddPerson(String eMobPlan){
		//TODO 
		
	}
	
	public void loadDisChargingProfiles(String file){
		this.discharging = EmobEnergyProfileReader.readDisChargingProfiles(file);
	}
	
	public void loadChargingProfiles(String file){
		this.charging = EmobEnergyProfileReader.readChargingProfiles(file);
	}
	
	public void run(){
		Controler c = new Controler(this.scenario);
		EventsManager events = c.getEvents() ;
		
		this.fleet.setEventsmanager(events);
		
		EmobVehicleDrivingHandler h1 = new EmobVehicleDrivingHandler(c.getNetwork(), this.discharging);
		EmobPersonHandler h2 = new EmobPersonHandler(h1, population, fleet, this.charging);

		
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
	
//	/**
//	 * @param outdir2
//	 */
//	public void dumpResults(String outdir) {
//		System.out.println("test");
//		BufferedWriter writer = IOUtils.getBufferedWriter(outdir + "distance.csv");
//		try {
//			writer.write("id;distance;charge;\n");
//			for(Entry<Id, EmobilityPerson> e: this.persons.entrySet()){
//				for(Tuple<Double, Double> ee: e.getValue().getVehicle().getDist2Charge()){
//					writer.write(e.getKey() + ";" + ee.getFirst() +";" + ee.getSecond() + ";");
//					writer.newLine();
//				}
//			}
//			writer.flush();
//			writer.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		
//		writer = IOUtils.getBufferedWriter(outdir + "time.csv");
//		try {
//			writer.write("id;time;charge;\n");
//			for(Entry<Id, EmobilityPerson> e: this.persons.entrySet()){
//				for(Tuple<Double, Double> ee: e.getValue().getVehicle().getTime2Charge()){
//					writer.write(e.getKey() + ";" + ee.getFirst() +";" + ee.getSecond() + ";");
//					writer.newLine();
//				}
//			}
//			writer.flush();
//			writer.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}

	private final static String VWDIR = "D:/VSP/svn/shared/volkswagen_internal/";
	private final static String SCENARIO = VWDIR + "scenario/";
	
	private static final String CONFIG = SCENARIO + "config_base_scenario.xml";
	private static final String EMOBPLANS = SCENARIO + "input/testplan.txt";
	private static final String OUTDIR = SCENARIO + "matsimOutput/2/";
	
	private static final String CHARGINGPROFILE = "C:/Users/Daniel/Desktop/Dokumente_MATSim_AP1und2/ChargingLookupTable_2011-11-30.txt";
	private static final String DISCHARGINGPROFILE = "C:/Users/Daniel/Desktop/Dokumente_MATSim_AP1und2/DrivingLookupTable_2011-11-25.txt";

	public static void main(String[] args) {
		EmobilityRunner runner = new EmobilityRunner(CONFIG);
		
		runner.createAndAddPerson(EMOBPLANS);
		runner.loadChargingProfiles(CHARGINGPROFILE);
		runner.loadDisChargingProfiles(DISCHARGINGPROFILE);
		
		runner.run();
		
//		runner.dumpResults(OUTDIR);
	}
}
