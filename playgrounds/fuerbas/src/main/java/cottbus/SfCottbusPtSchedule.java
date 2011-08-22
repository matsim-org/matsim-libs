/* *********************************************************************** *
 * project: org.matsim.*
 * SfCottbusPtSchedule.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package cottbus;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * @author fuerbas
 *
 */

public class SfCottbusPtSchedule {

	/**
	 * @param args
	 */
	
	private Scenario scenario;
	private Config config;
	private Network network;
	private TransitSchedule schedule;
	private TransitScheduleFactory schedulefactory;
	private String pt_mode;
	private String NETWORK = "INPUTFILE_NETWORK";
	private String LINES = "E:\\Cottbus\\Cottbus_pt\\lines\\lines.csv";
	
	
	public SfCottbusPtSchedule() {
		this.scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		this.config = this.scenario.getConfig();;
		this.config.network().setInputFile(NETWORK);
		ScenarioUtils.loadScenario(this.scenario);
		this.network = this.scenario.getNetwork();
		this.schedulefactory = new TransitScheduleFactoryImpl();
		this.schedule = this.schedulefactory.createTransitSchedule();
	}
	
	public static void main(String[] args) throws Exception {

		SfCottbusPtSchedule cottbus = new SfCottbusPtSchedule();
		
//		Linienliste einlesen, Linien als Strings speichern, über alle Linien gleiches Schema ausführen...
		
		String[] lines = cottbus.getLineNumbers(cottbus.LINES);
		
		for(int nLines = 0; nLines<24; nLines++) {
			List<String> transitStopIdStrings = new ArrayList<String>();
			List<String> stopLinkString = new ArrayList<String>();
			String lineName = lines[nLines];
			String linesfile = lineName+".csv";
			BufferedReader br = new BufferedReader(new FileReader(linesfile));
			if (nLines <=7) cottbus.pt_mode="tram";
			else if (nLines>7 && nLines<=20) cottbus.pt_mode="pt";
			else cottbus.pt_mode="train";
			
			while(br.ready()) {
				String[] lineEntries = br.readLine().split(";");
				transitStopIdStrings.add(lineEntries[1]);
				stopLinkString.add(lineEntries[6]);
			}
			List<TransitRouteStop> stopList = cottbus.createTransitStopFacilities(transitStopIdStrings, stopLinkString);
			
			String[] routes = null;
			NetworkRoute netRoute = cottbus.createNetworkRoute(routes);
			
			TransitRoute transRoute = cottbus.createTransitRoute(lineName, netRoute, stopList);
//			cottbus.addDeparturesToRoute(transRoute, firstDep, lastDep, frequency);
			
			TransitLine transLine = cottbus.createTransitLine(transRoute, lineName);
			
			cottbus.schedule.addTransitLine(transLine);
		}
		
			
		
	}
	
	private String[] getLineNumbers(String inputfile) throws IOException {
		BufferedReader linesReader = new BufferedReader(new FileReader(inputfile));
		int ii=0;
		String[] lines = new String[24];
		while(linesReader.ready()) {
			String oneLine = linesReader.readLine();
			String[] lineEntries = oneLine.split(";");
			lines[ii] = lineEntries[0];
			ii++;
		}
		linesReader.close();
		return lines;
	}
	
	private TransitLine createTransitLine(TransitRoute transitRoute, String lineId) {
		TransitLine transitLine = this.schedulefactory.createTransitLine(new IdImpl(lineId));
		transitLine.addRoute(transitRoute);
		return transitLine;
	}
	
	private void addDeparturesToRoute (TransitRoute transitRoute, double firstDep, double lastDep, double frequency) {
		
	}
	
	private TransitRoute createTransitRoute(String transitRouteId, NetworkRoute netRoute, List<TransitRouteStop> stopList) {
		TransitRoute transitRoute = this.schedulefactory.createTransitRoute(new IdImpl(transitRouteId), netRoute, stopList, this.pt_mode);
		return transitRoute;
	}
	
	private List<TransitRouteStop> createTransitStopFacilities(List<String> ids, List<String> links) {
		
		List<Id> linkList = new ArrayList<Id>();
		List<TransitRouteStop> stopList = new ArrayList<TransitRouteStop>();
		List<TransitStopFacility> facilList = new ArrayList<TransitStopFacility>();
		TransitStopFacility trastofac = null;
		
		for(int index = 0; index<ids.size(); index++)	{
						
//			eine facility mit mehreren stops erstellen
			Id facilId = new IdImpl(ids.get(index));
			Id linkId = new IdImpl(links.get(index));
			if (!this.schedule.getFacilities().containsKey(facilId)) {
				trastofac = this.schedule.getFactory().createTransitStopFacility(facilId, this.network.getLinks().get(linkId).getCoord(), true);
				facilList.add(trastofac);
				trastofac.setLinkId(linkId);
				this.schedule.addStopFacility(trastofac);
			} 
			else  trastofac = this.schedule.getFacilities().get(facilId);
			
			if (!linkList.contains(linkId))
				trastofac.setLinkId(linkId);
				TransitRouteStop transStop = this.schedule.getFactory().createTransitRouteStop(trastofac, 0, 0);
				linkList.add(linkId);
				stopList.add(transStop);
		}
		
		return stopList;
	}	
	
	private NetworkRoute createNetworkRoute(String[] links) {
		int length=links.length;
		Id origin = new IdImpl(links[0]);
		Id destination = new IdImpl(links[length-1]);
		NetworkRoute netRoute = new LinkNetworkRouteImpl(origin, destination);
		List<Id> linkList = new ArrayList<Id>();
		for(int index = 1; index<length-1; index++) {
				linkList.add(new IdImpl(links[index]));
			}
		netRoute.setLinkIds(origin, linkList, destination);
		return netRoute;
	}

}

