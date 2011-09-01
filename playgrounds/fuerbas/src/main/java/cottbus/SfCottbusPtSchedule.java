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
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV1;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.VehicleCapacityImpl;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;
import org.matsim.vehicles.VehiclesFactoryImpl;
import org.matsim.vehicles.VehiclesImpl;

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
	private String NETWORK;
	private String LINES;
	private HashMap<Id, Id> ptLinkList;
	private List<Id> ptFacList;
	private Vehicles vehicles;
	
	
	public SfCottbusPtSchedule() {
		this.scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		this.config = this.scenario.getConfig();
		this.LINES = "E:\\Cottbus\\Cottbus_pt\\lines\\lines.csv";
		this.NETWORK = "E:\\Cottbus\\Cottbus_pt\\Cottbus-pt\\network_pt.xml";
		this.config.network().setInputFile(this.NETWORK);
		ScenarioUtils.loadScenario(this.scenario);
		this.network = this.scenario.getNetwork();
		this.schedulefactory = new TransitScheduleFactoryImpl();
		this.schedule = this.schedulefactory.createTransitSchedule();
		this.ptLinkList = new HashMap<Id, Id>(); //linkId und facilId
		this.ptFacList = new ArrayList<Id>();
		this.vehicles = new VehiclesImpl();
		this.vehicles.getVehicleTypes().put(new IdImpl("tram_93pax"), this.vehicles.getFactory().createVehicleType(new IdImpl("tram_93pax")));
		this.vehicles.getVehicleTypes().put(new IdImpl("bus_90pax"), this.vehicles.getFactory().createVehicleType(new IdImpl("bus_90pax")));
		this.vehicles.getVehicleTypes().put(new IdImpl("bus_64pax"), this.vehicles.getFactory().createVehicleType(new IdImpl("bus_64pax")));
		this.vehicles.getVehicleTypes().get(new IdImpl("bus_90pax")).setCapacity(new VehicleCapacityImpl());
	}
	
	public static void main(String[] args) throws Exception {

		SfCottbusPtSchedule cottbus = new SfCottbusPtSchedule();
		
		Scenario scen = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());	
		Config config = scen.getConfig();
		config.network().setInputFile(cottbus.NETWORK);
		ScenarioUtils.loadScenario(scen);		
		cottbus.network = scen.getNetwork();
		scen.getConfig().scenario().setUseTransit(true);
		scen.getConfig().scenario().setUseVehicles(true);
				
		cottbus.schedulefactory = new TransitScheduleFactoryImpl();
		cottbus.schedule = cottbus.schedulefactory.createTransitSchedule();

		
		
//		Linienliste einlesen, Linien als Strings speichern, über alle Linien gleiches Schema ausführen...
		
		String[] lines = cottbus.getLineNumbers(cottbus.LINES);
		
		for(int nLines = 0; nLines<24; nLines++) {
			List<String> transitStopIdStrings = new ArrayList<String>();
			List<String> stopLinkString = new ArrayList<String>();
			String lineName = lines[nLines];
			double firstDep = 0.;
			double freq = 0.;
			int counter = 0;
			String linesfile = "E:\\Cottbus\\Cottbus_pt\\lines\\all\\"+lineName+".csv";
			BufferedReader br = new BufferedReader(new FileReader(linesfile));
			if (nLines <=7) cottbus.pt_mode="tram";
			else if (nLines>7 && nLines<=20) cottbus.pt_mode="pt";
			else cottbus.pt_mode="train";
				
				while(br.ready()) {
					String[] lineEntries = br.readLine().split(";");
					transitStopIdStrings.add(lineEntries[1]);
					stopLinkString.add(lineEntries[6].replaceAll("\"",""));
					if (counter==0) {
						lineEntries[7] = lineEntries[7].replaceAll("\"", "");
						String[] time = lineEntries[7].split(":");
						System.out.println("ZEIT: "+lineEntries[7]);
						firstDep = Double.parseDouble(time[0])*3600 + Double.parseDouble(time[1])*60 + Double.parseDouble(time[2]);
						freq = Double.parseDouble(lineEntries[8])*60;
					}
					counter++;
				}
				br.close();
				
			List<TransitRouteStop> stopList = cottbus.createTransitStopFacilities(transitStopIdStrings, stopLinkString);
			
			

			String lineroutes = "E:\\Cottbus\\Cottbus_pt\\lines\\all\\"+lineName+"_links.csv";
			BufferedReader br2 = new BufferedReader(new FileReader(lineroutes));
			
			List<String> routeLinks = new ArrayList<String>();
			while(br2.ready()) {
				routeLinks.add(br2.readLine().replaceAll("\"", ""));
			}
			br2.close();
			
			String[] routes = routeLinks.toArray(new String[routeLinks.size()]);
			
			for (String string : routeLinks) {
				Id id = new IdImpl(string);
				String[] allowedModesArray = cottbus.scenario.getNetwork().getLinks().get(id).getAllowedModes().toArray(new String[cottbus.scenario.getNetwork().getLinks().get(id).getAllowedModes().size()]);
				Set<String> allowedModes = new TreeSet<String>();
				for (int ii=0; ii<allowedModesArray.length; ii++) {
					allowedModes.add(allowedModesArray[ii]);
				}
				if (!allowedModes.contains(cottbus.pt_mode)) {
					allowedModes.add(cottbus.pt_mode);
					cottbus.scenario.getNetwork().getLinks().get(id).setAllowedModes(allowedModes);
				} 
				else ;
			}

			NetworkRoute netRoute = cottbus.createNetworkRoute(routes);
			
			TransitRoute transRoute = cottbus.createTransitRoute(lineName, netRoute, stopList);
			
			cottbus.addDeparturesToRoute(transRoute, firstDep, freq);
			
			TransitLine transLine = cottbus.createTransitLine(transRoute, lineName);
			
			cottbus.schedule.addTransitLine(transLine);
		}
		
		TransitScheduleWriterV1 scheduleWriter = new TransitScheduleWriterV1(cottbus.schedule);
		String outputfile = "E:\\Cottbus\\Cottbus_pt\\lines\\schedule.xml";
		scheduleWriter.write(outputfile);
		
		System.out.println("Schedule written to: "+outputfile);
		
		NetworkWriter networkwriter = new NetworkWriter(cottbus.scenario.getNetwork());
		networkwriter.write("E:\\Cottbus\\Cottbus_pt\\Cottbus-pt\\network_pt.xml");
		
		System.out.println("Network written to: "+"E:\\Cottbus\\Cottbus_pt\\Cottbus-pt\\network_pt.xml");
		
		VehicleWriterV1 writer = new VehicleWriterV1(cottbus.vehicles);
		writer.writeFile("E:\\Cottbus\\Cottbus_pt\\Cottbus-pt\\transitVehicles.xml");
		
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
	
	private void addDeparturesToRoute (TransitRoute transitRoute, double firstDep, double frequency) {
		double lastDep = 22.0 * 3600;
		double currentDep = firstDep;
		while (currentDep <= lastDep) {
			Departure dep = this.schedulefactory.createDeparture(new IdImpl(transitRoute.getId().toString()+"_"+currentDep), currentDep);
			transitRoute.addDeparture(dep);
			currentDep+=frequency;
			if (this.pt_mode.equals("pt")) this.vehicles.getVehicles().put(dep.getId(), this.vehicles.getFactory().createVehicle(dep.getId(), this.vehicles.getVehicleTypes().get("1")));
		}
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
		
		
		for(int index = 0; index<links.size(); index++)	{
						
//			eine facility mit mehreren stops erstellen
			Id facilId = new IdImpl(ids.get(index));
			Id linkId = new IdImpl(links.get(index));
			
			if (!this.ptLinkList.containsKey(linkId)) {
				linkList.add(linkId);
				if (this.ptFacList.contains(facilId)) {
					System.out.println("FACIL ID vorher: "+facilId);
					facilId = new IdImpl(facilId+"_b");
						if (this.ptLinkList.containsValue(facilId)) facilId = new IdImpl(ids.get(index)+"_c");
						if (this.ptLinkList.containsValue(facilId)) facilId = new IdImpl(ids.get(index)+"_d");
						if (this.ptLinkList.containsValue(facilId)) facilId = new IdImpl(ids.get(index)+"_e");
						if (this.ptLinkList.containsValue(facilId)) facilId = new IdImpl(ids.get(index)+"_f");
					System.out.println("FACIL ID: "+facilId);
					System.out.println("LINK ID: "+linkId);
					this.ptLinkList.put(linkId, facilId);
					trastofac = this.schedule.getFactory().createTransitStopFacility(facilId, this.network.getLinks().get(linkId).getCoord(), true);
					facilList.add(trastofac);
					trastofac.setLinkId(linkId);
					this.schedule.addStopFacility(trastofac);
				}
				else {
					this.ptFacList.add(facilId);
					System.out.println("FACIL ID vorher: "+facilId);
					facilId = new IdImpl(facilId+"_a");
					System.out.println("FACIL ID: "+facilId);
					System.out.println("LINK ID: "+linkId);
					this.ptLinkList.put(linkId, facilId);
					trastofac = this.schedule.getFactory().createTransitStopFacility(facilId, this.network.getLinks().get(linkId).getCoord(), true);
					facilList.add(trastofac);
					trastofac.setLinkId(linkId);
					this.schedule.addStopFacility(trastofac);
				}
			}
			
			
			else  trastofac = this.schedule.getFacilities().get(this.ptLinkList.get(linkId));
			
//			System.out.println("FAC LIST: "+this.ptFacList.);
			
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

