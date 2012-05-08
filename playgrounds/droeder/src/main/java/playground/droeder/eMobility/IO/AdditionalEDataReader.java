/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.droeder.eMobility.IO;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import playground.dgrether.energy.validation.PoiInfo;
import playground.dgrether.energy.validation.ValidationInfoReader;
import playground.dgrether.energy.validation.ValidationInformation;
import playground.droeder.DaFileReader;
import playground.droeder.eMobility.EmobilityScenario;
import playground.droeder.eMobility.fleet.EActivity;
import playground.droeder.eMobility.fleet.EFleet;
import playground.droeder.eMobility.fleet.EVehicle;
import playground.droeder.eMobility.poi.POI;
import playground.droeder.eMobility.poi.PoiList;
import playground.droeder.eMobility.population.EPerson;
import playground.droeder.eMobility.population.EPopulation;

/**
 * @author droeder
 *
 */
public class AdditionalEDataReader {
	private static final Logger log = Logger
			.getLogger(AdditionalEDataReader.class);
	
	 private EmobilityScenario eSc;

	private EPopulation ePop;

	private EFleet fleet;

	public AdditionalEDataReader(EmobilityScenario sc){
		 this.eSc = sc;
		 if(this.eSc.getSc() == null){
			 throw new RuntimeException("need MatsimScenario...");
		 }
		this.ePop = new EPopulation();
		this.fleet = new EFleet();

	 }
	
	public void readAppointments(String matsimPlan, String appointmentsFile){
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(sc).readFile(this.eSc.getSc().getConfig().getParam(NetworkConfigGroup.GROUP_NAME, "inputNetworkFile"));
		new MatsimPopulationReader(sc).readFile(matsimPlan);
		
		Set<String[]> appointments = DaFileReader.readFileContent(appointmentsFile, "\t", true);
		
		List<String[]> temp = null;
		String[] s;
		Iterator<String[]> it = appointments.iterator();
		String currId = null;
		while(it.hasNext()){
			s = it.next();
			if(temp == null){
				temp = new ArrayList<String[]>();
				temp.add(s);
				currId = s[0];
			}else if (currId.equals(s[0])){
				temp.add(s);
			}else{
				createEPopulationData(currId, temp, sc);
				temp = new ArrayList<String[]>();
				temp.add(s);
				currId = s[0];
			}
		}
		
		this.eSc.setFleet(this.fleet);
		this.eSc.setPopulation(this.ePop);
	}

	/**
	 * @param currId 
	 * @param temp
	 * @param sc 
	 */
	private void createEPopulationData(String currId, List<String[]> temp, Scenario sc) {
		Person p;
		EPerson ep;
		EVehicle veh;
		ArrayList<EActivity> activities;
		EActivity ea;
		Link start = null;
		Double soc = null;
		double startTime, endTime;
		String [] time;
		if(sc.getPopulation().getPersons().containsKey(new IdImpl(currId))){
			p = this.eSc.getSc().getPopulation().getFactory().createPerson(this.eSc.getSc().createId(EPopulation.IDENTIFIER + currId));
			p.addPlan(sc.getPopulation().getPersons().get(new IdImpl(currId)).getSelectedPlan());
			this.eSc.getSc().getPopulation().addPerson(p);
			activities = new ArrayList<EActivity>();
			for(String[] s : temp){
				if(start == null){
					start = sc.getNetwork().getLinks().get(new IdImpl(s[2]));
					soc = Double.parseDouble(s[3]);
				}
				if(s[5].equals("NULL")){
					startTime = Double.MAX_VALUE;
				}else{
					time = s[5].split(":");
					startTime = 3600 * Double.parseDouble(time[0]) + 60 * Double.parseDouble(time[1]) + Double.parseDouble(time[2]);
				}
				if(s[6].equals("NULL")){
					endTime = Double.MAX_VALUE;
				}else{
					time = s[6].split(":");
					endTime = 3600 * Double.parseDouble(time[0]) + 60 * Double.parseDouble(time[1]) + Double.parseDouble(time[2]);
				}
				ea = new EActivity(new IdImpl(s[4]), startTime, endTime - startTime, new IdImpl(s[8]), new IdImpl(s[7]));
				activities.add(ea);
			}
			veh = new EVehicle(p.getId(), soc, start, activities);
			this.fleet.addVehicle(veh);
			veh.getPoiId();
			ep = new EPerson(p.getId(), veh);
			this.ePop.add(ep);
			
		}else{
			log.error("can not create an Agent for Emobility-Simulation, because no matsim-plan exists for this agent...");
		}
		
		
		
	}

	/**
	 * @param poifile
	 */
	public void readPOI(String poifile, double timeBinSize) {
		ValidationInformation info = new ValidationInfoReader().readFile(poifile);
		
		PoiList list = new PoiList();
		POI p;
		for(PoiInfo pi : info.getValidationInformationList()){
			p = new POI(new IdImpl(pi.getPoiID()), pi.getMaximalCapacity(), timeBinSize);
			list.add(p);
		}
		this.eSc.setPoi(list);
	}

}
