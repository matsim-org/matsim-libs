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

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.run.OTFVis;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OnTheFlyServer;

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
	
	/**
	 * 
	 */
	public EmobilityRunner(String configFile) {
		Config c = ConfigUtils.loadConfig(configFile);
		this.scenario = ScenarioUtils.loadScenario(c);
		this.manager = EventsUtils.createEventsManager();
	}
	
	public void createAndAddElectroVehicle(String eMobPlans){
		this.vehicles = new HashMap<Id, ElectroVehicle>();
		
		//TODO load Data from FILE First try with any Agent
		ElectroVehicle veh =  new ElectroVehicle(new IdImpl(""), 26.0, 26.0);
		this.vehicles.put(veh.getId(), veh);
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
		ElectroVehicleHandler eHandler = new ElectroVehicleHandler(this.vehicles, this.charging, this.discharging, this.scenario.getNetwork());
		this.manager.addHandler(eHandler);
		QSim otfVisQSim = QSim.createQSimAndAddAgentSource(this.scenario, this.manager);
		
		OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(this.scenario.getConfig(), this.scenario, this.manager, otfVisQSim);
		OTFClientLive.run(this.scenario.getConfig(), server);
		otfVisQSim.run();
	}
	
	/**
	 * @param outdir2
	 */
	private void dumpResults(String outdir) {
		// TODO Auto-generated method stub
		
	}

	private static final String CONFIG = "";
	private static final String EMOBPLANS = "";
	private static final String CHARGINGPROFILE = "";
	private static final String DISCHARGINGPROFILE = "";
	private static final String OUTDIR = "";
	
	public static void main(String[] args) {
		EmobilityRunner runner = new EmobilityRunner(CONFIG);
		
		runner.createAndAddElectroVehicle(EMOBPLANS);
		runner.loadChargingProfiles(CHARGINGPROFILE);
		runner.loadChargingProfiles(DISCHARGINGPROFILE);
		
		runner.run();
		
		runner.dumpResults(OUTDIR);
	}
}
