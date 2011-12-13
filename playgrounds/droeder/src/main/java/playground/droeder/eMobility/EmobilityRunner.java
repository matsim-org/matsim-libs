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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;

import playground.droeder.eMobility.energy.ChargingProfiles;
import playground.droeder.eMobility.energy.DisChargingProfiles;

/**
 * @author droeder
 *
 */
public class EmobilityRunner {
	
	private Scenario scenario;
	private EventsManager manager;
	private Map<Id, ElectroVehicle> vehicles;
	private ChargingProfiles charging;
	private DisChargingProfiles discharging;
	private ElectroVehicleHandler eHandler;
	private List<EventHandler> handler;
	/**
	 * 
	 */
	public EmobilityRunner(String configFile) {
		Config c = ConfigUtils.loadConfig(configFile);
		this.scenario = ScenarioUtils.loadScenario(c);
		this.manager = EventsUtils.createEventsManager();
		this.handler = new ArrayList<EventHandler>();
	}
	
	public void createAndAddElectroVehicle(String eMobPlans){
		//TODO
		this.vehicles = new HashMap<Id, ElectroVehicle>();
		
		ElectroVehicle veh = new ElectroVehicle(new IdImpl("emob1"), 26.0, 26.0);
		veh.setChargingType(new IdImpl("FAST"));
		veh.setDischargingType(new IdImpl("HIGH"));
		this.vehicles.put(new IdImpl("emob1"), veh);

		
//		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
//		new MatsimNetworkReader(sc).readFile(this.scenario.getConfig().getParam(NetworkConfigGroup.GROUP_NAME, "inputNetworkFile"));
//		new MatsimPopulationReader(sc).readFile(eMobPlans);
//		for(Person p: sc.getPopulation().getPersons().values()){
	}
	
	public void loadDisChargingProfiles(String file){
		this.discharging = new DisChargingProfiles();
		this.discharging.readAndAddDataFromFile(file);
	}
	
	public void loadChargingProfiles(String file){
		this.charging = new ChargingProfiles();
		this.charging.readAndAddDataFromFile(file);
	}
	
	public void run(){
		this.eHandler = new ElectroVehicleHandler(this.vehicles, this.charging, this.discharging, this.scenario.getNetwork());
		this.handler.add(eHandler);
		
		Controler c = new Controler(this.scenario);
		c.addControlerListener(new MyListener(this.handler));
		
		c.setDumpDataAtEnd(true);
		c.setOverwriteFiles(true);
		
		c.run();
//		this.manager.addHandler(eHandler);
//		QSim otfVisQSim = QSim.createQSimAndAddAgentSource(this.scenario, this.manager);
//		
//		OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(this.scenario.getConfig(), this.scenario, this.manager, otfVisQSim);
//		OTFClientLive.run(this.scenario.getConfig(), server);
//		otfVisQSim.run();
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
			writer.write("distance;charge;\n");
			for(Tuple<Double, Double> v: this.vehicles.get(new IdImpl("emob1")).getDist2Charge()){
				writer.write(v.getFirst() +";" + v.getSecond() + ";");
				writer.newLine();
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		writer = IOUtils.getBufferedWriter(outdir + "time.csv");
		try {
			writer.write("time;charge;\n");
			for(Tuple<Double, Double> v: this.vehicles.get(new IdImpl("emob1")).getTime2Charge()){
				writer.write(v.getFirst() +";" + v.getSecond() + ";");
				writer.newLine();
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static final String CONFIG = "C:/Users/Daniel/Desktop/Dokumente_MATSim_AP1und2/config.xml";
	private static final String EMOBPLANS = "C:/Users/Daniel/Desktop/Dokumente_MATSim_AP1und2/testpopulation_new.xml";
	private static final String CHARGINGPROFILE = "C:/Users/Daniel/Desktop/Dokumente_MATSim_AP1und2/ChargingLookupTable_2011-11-30.txt";
	private static final String DISCHARGINGPROFILE = "C:/Users/Daniel/Desktop/Dokumente_MATSim_AP1und2/DrivingLookupTable_2011-11-25.txt";
	private static final String OUTDIR = "C:/Users/Daniel/Desktop/Dokumente_MATSim_AP1und2/";
	
	public static void main(String[] args) {
		EmobilityRunner runner = new EmobilityRunner(CONFIG);
		
		runner.createAndAddElectroVehicle(EMOBPLANS);
		runner.loadChargingProfiles(CHARGINGPROFILE);
		runner.loadDisChargingProfiles(DISCHARGINGPROFILE);
		
		runner.run();
		
		runner.dumpResults(OUTDIR);
	}
}
