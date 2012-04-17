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
package playground.droeder.eMobility.v3;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.GenericEvent;
import org.matsim.core.api.experimental.events.handler.GenericEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.vis.otfvis.OTFFileWriterFactory;

import playground.droeder.eMobility.energy.ChargingProfiles;
import playground.droeder.eMobility.energy.DisChargingProfiles;
import playground.droeder.eMobility.energy.EmobEnergyProfileReader;
import playground.droeder.eMobility.v3.events.EFleetHandler;
import playground.droeder.eMobility.v3.events.EPopulationHandler;
import playground.droeder.eMobility.v3.events.SoCChangeEvent;
import playground.droeder.eMobility.v3.fleet.EFleet;


/**
 * @author droeder
 *
 */
public class EmobilityRunner {
	
	private static final String DIR = "D:/VSP/svn/shared/volkswagen_internal/";

	private static final String CONFIGFILE = DIR + "scenario/config_base_scenario.xml";
	private static final String CHARGINGFILE = DIR + "Dokumente_MATSim_AP1und2/ChargingLookupTable_2011-11-30.txt";
	private static final String DISCHARGINGFILE = DIR + "Dokumente_MATSim_AP1und2/DrivingLookupTable_2011-11-25.txt";

	public static void main(String[] args){
		CreateTestScenario test = new CreateTestScenario();
		
		EmobilityRunner runner = new EmobilityRunner();
		runner.loadChargingProfiles(CHARGINGFILE);
		runner.loadDisChargingProfiles(DISCHARGINGFILE);
		
		runner.run(test.run(CONFIGFILE));
	}

	private DisChargingProfiles dischargingProfiles;
	private ChargingProfiles chargingProfiles;
	private EFleet fleet;

	/**
	 * @param dischargingfile2
	 */
	private void loadDisChargingProfiles(String dischargingfile2) {
		this.dischargingProfiles = EmobEnergyProfileReader.readDisChargingProfiles(dischargingfile2);		
	}

	/**
	 * @param chargingfile2
	 */
	private void loadChargingProfiles(String chargingfile2) {
		this.chargingProfiles = EmobEnergyProfileReader.readChargingProfiles(chargingfile2);		
	}

	public void run(EmobilityScenario scenario) {
		Controler c = new Controler(scenario.getSc());
		c.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());
		EFleetHandler fleetHandler = new EFleetHandler(scenario.getFleet());
		EPopulationHandler populationHandler = new EPopulationHandler(scenario.getPopulation());

		fleetHandler.getFleet().init(this.chargingProfiles, this.dischargingProfiles, scenario.getSc().getNetwork(), scenario.getPoi());
		scenario.getPopulation().init(fleetHandler.getFleet());
		
		c.addControlerListener(new MyListener(fleetHandler, populationHandler));
		
		c.setDumpDataAtEnd(true);
		c.setOverwriteFiles(true);
		
		c.run();
		
		
	}
	//internal class
	private class MyListener implements StartupListener{
		
		

		private EFleetHandler fHandler;
		private EPopulationHandler pHandler;

		private MyListener(EFleetHandler fleetHandler, EPopulationHandler populationHandler){
			this.fHandler = fleetHandler;
			this.pHandler = populationHandler;
		}

		/* (non-Javadoc)
		 * @see org.matsim.core.controler.listener.StartupListener#notifyStartup(org.matsim.core.controler.events.StartupEvent)
		 */
		@Override
		public void notifyStartup(StartupEvent event) {
			event.getControler().getEvents().addHandler(this.fHandler);
			event.getControler().getEvents().addHandler(this.pHandler);
			this.fHandler.getFleet().registerEventsManager(event.getControler().getEvents());
			event.getControler().getQueueSimulationListener().add(this.fHandler.getFleet());
		}

		
	}

}
