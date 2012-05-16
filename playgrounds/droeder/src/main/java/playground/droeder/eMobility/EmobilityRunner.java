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

import java.util.GregorianCalendar;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFFileWriterFactory;

import playground.dgrether.energy.trafficstate.TrafficStateControlerListener;
import playground.dgrether.energy.validation.ObjectFactory;
import playground.dgrether.energy.validation.PoiInfo;
import playground.dgrether.energy.validation.PoiTimeInfo;
import playground.dgrether.energy.validation.ValidationInfoWriter;
import playground.dgrether.energy.validation.ValidationInformation;
import playground.droeder.DaFileReader;
import playground.droeder.eMobility.IO.AdditionalEDataReader;
import playground.droeder.eMobility.analysis.SoCEventHandler;
import playground.droeder.eMobility.energy.ChargingProfiles;
import playground.droeder.eMobility.energy.DisChargingProfiles;
import playground.droeder.eMobility.energy.EmobEnergyProfileReader;
import playground.droeder.eMobility.events.EFleetHandler;
import playground.droeder.eMobility.events.EPopulationHandler;
import playground.droeder.eMobility.poi.POI;


/**
 * @author droeder
 *
 */
public class EmobilityRunner {
	
	private static final String DIR = "D:/VSP/svn/shared/volkswagen_internal/";
//	private static final String DIR = "/home/dgrether/shared-svn/projects/volkswagen_internal/";

	private static final String CONFIGFILE = DIR + "scenario/config_congested_scenario.xml";
	private static final String ENERGYAGENTS = DIR + "scenario/input/testPlans.xml";
	private static final String APPOINTMENTS = DIR + "scenario/input/testAppointments.txt";
	private static final String CHARGINGFILE = DIR + "Dokumente_MATSim_AP1und2/ChargingLookupTable_2011-11-30.txt";
	private static final String DISCHARGINGFILE = DIR + "Dokumente_MATSim_AP1und2/DrivingLookupTable_2011-11-25.txt";
	private static final String POIFILE = DIR + "scenario/input/poiInfo.xml";

	private DisChargingProfiles dischargingProfiles;
	private ChargingProfiles chargingProfiles;

	private void loadDisChargingProfiles(String dischargingfile2) {
		this.dischargingProfiles = EmobEnergyProfileReader.readDisChargingProfiles(dischargingfile2);		
	}

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
		
		SoCEventHandler soc = new SoCEventHandler(scenario.getSc().getNetwork());
		TrafficStateControlerListener trafficState = new TrafficStateControlerListener();
		c.addControlerListener(trafficState);
		c.addControlerListener(new MyListener(fleetHandler, populationHandler, soc));
		
		c.setDumpDataAtEnd(true);
		c.setOverwriteFiles(true);
		
		c.run();
		
		soc.dumpData(scenario.getSc().getConfig().controler().getOutputDirectory() + Controler.DIRECTORY_ITERS + "/it.0/charts/");
		this.dumppoiData(scenario);
	}
	
	private void dumppoiData(EmobilityScenario eSc){
		ObjectFactory f = new ObjectFactory();
		ValidationInformation vInfo = f.createValidationInformationList();
		PoiInfo info;
		PoiTimeInfo tInfo;
		
		int start, end;
		
		for(POI p: eSc.getPoi().getPOIs()){
			info = f.createPoiInfo();
			info.setMaximalCapacity(p.getMaxSpace());
			info.setPoiID(p.getId().toString());
			for(int i = 0; i < p.getMaxLoad().length; i++){
				tInfo = new PoiTimeInfo();
				start = (int) (p.getTimeBinSize() * i);
				end = (int) (start + p.getTimeBinSize());
				tInfo.setStartTime(new GregorianCalendar(1979, 01, 01, 0, 0, start));
				tInfo.setEndTime(new GregorianCalendar(1979, 01, 01, 0, 0, end));
				tInfo.setUsedCapacity(p.getMaxLoad()[i]);
				info.getPoiTimeInfos().add(tInfo);
			}
			vInfo.add(info);
		}
		
		new ValidationInfoWriter(vInfo).writeFile(eSc.getSc().getConfig().controler().getOutputDirectory() + "poiInfo.xml");
	}
	
	//internal class
	private class MyListener implements StartupListener{
		private EFleetHandler fHandler;
		private EPopulationHandler pHandler;
		private SoCEventHandler socHandler;
		

		private MyListener(EFleetHandler fleetHandler, EPopulationHandler populationHandler, SoCEventHandler soc){
			this.fHandler = fleetHandler;
			this.pHandler = populationHandler;
			this.socHandler = soc;
		}

		@Override
		public void notifyStartup(StartupEvent event) {
			event.getControler().getEvents().addHandler(this.fHandler);
			event.getControler().getEvents().addHandler(this.pHandler);
			event.getControler().getEvents().addHandler(this.socHandler);
			this.fHandler.getFleet().registerEventsManager(event.getControler().getEvents());
			event.getControler().getQueueSimulationListener().add(this.fHandler.getFleet());
		}
	}
	
	public static void main(String[] args){
		if(args.length == 1){
			String[] inputFiles = DaFileReader.readFileContent(args[0], ";", false).iterator().next();
			Scenario sc = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(inputFiles[0]));
			EmobilityScenario eSc = new EmobilityScenario();
			eSc.setSc(sc);
			AdditionalEDataReader reader = new AdditionalEDataReader(eSc);
			reader.readAppointments(inputFiles[1], inputFiles[2]);
			reader.readPOI(inputFiles[3], 3600);
			
			EmobilityRunner runner = new EmobilityRunner();
			runner.loadChargingProfiles(inputFiles[4]);
			runner.loadDisChargingProfiles(inputFiles[5]);
			runner.run(eSc);
		}else{
			Scenario sc = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(CONFIGFILE));
			EmobilityScenario eSc = new EmobilityScenario();
			eSc.setSc(sc);
			AdditionalEDataReader reader = new AdditionalEDataReader(eSc);
			reader.readAppointments(ENERGYAGENTS, APPOINTMENTS);
			reader.readPOI(POIFILE, 3600);
			
			EmobilityRunner runner = new EmobilityRunner();
			runner.loadChargingProfiles(CHARGINGFILE);
			runner.loadDisChargingProfiles(DISCHARGINGFILE);
			runner.run(eSc);
		}
	}

}
