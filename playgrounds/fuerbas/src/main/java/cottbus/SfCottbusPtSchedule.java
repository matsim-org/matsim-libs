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
import java.io.File;
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
 * @author jbischoff
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
			String linesfile = lines[nLines]+".csv";
			BufferedReader br = new BufferedReader(new FileReader(linesfile));
			while(br.ready()) {
				String[] lineEntries = br.readLine().split(";");
				transitStopIdStrings.add(lineEntries[1]);
				stopLinkString.add(lineEntries[6]);
			}
			cottbus.createTransitStopFacilities(cottbus.network, cottbus.schedule, transitStopIdStrings, stopLinkString);
			
		}
		
		
//		cottbus.schedule.addTransitLine(cottbus.createTransitLine());
		
		
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
	
	private TransitLine createTransitLine(TransitScheduleFactory stf, TransitRoute transitRoute, String lineId) {
		TransitLine transitLine = stf.createTransitLine(new IdImpl(lineId));
		transitLine.addRoute(transitRoute);
		return transitLine;
	}
	
	private void addDeparturesToRoute (TransitRoute transitRoute, double firstDep, double lastDep, double frequency) {
		
	}
	
	private TransitRoute createTransitRoute(String transitRouteId, NetworkRoute netRoute, List<TransitRouteStop> stopList, TransitScheduleFactory tsf, String pt_mode) {
		TransitRoute transitRoute = tsf.createTransitRoute(new IdImpl(transitRouteId), netRoute, stopList, pt_mode);
		return transitRoute;
	}
	
	private List<TransitRouteStop> createTransitStopFacilities(Network network, TransitSchedule tsf, List<String> ids, List<String> links) {
		List<TransitRouteStop> stopList = new ArrayList<TransitRouteStop>();
		for(int index = 0; index<ids.size(); index++)	{
			
//			eine facility mit mehreren stops erstellen
			
			Id facilId = new IdImpl(ids.get(index));
			Id linkId = new IdImpl(links.get(index));
			TransitStopFacility trastofac = tsf.getFactory().createTransitStopFacility(facilId, network.getLinks().get(linkId).getCoord(), true);
			trastofac.setLinkId(linkId);
			TransitRouteStop transStop = tsf.getFactory().createTransitRouteStop(trastofac, 0, 0);
			tsf.addStopFacility(trastofac);
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

