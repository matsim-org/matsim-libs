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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV1;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

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
	private final String ptdir = "\\\\vsp-nas\\jbischoff\\WinHome\\Docs\\cottbus\\cottbus_feb_fix\\Cottbus-pt\\";
	
	
	
	public SfCottbusPtSchedule() {
		this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		this.config = this.scenario.getConfig();
		
		this.LINES = ptdir+"lines\\lines_congregated.csv";
		this.NETWORK = ptdir+"network_pt.xml";
		this.config.network().setInputFile(this.NETWORK);
		ScenarioUtils.loadScenario(this.scenario);
		this.network = this.scenario.getNetwork();
		this.schedulefactory = new TransitScheduleFactoryImpl();
		this.schedule = this.schedulefactory.createTransitSchedule();
		this.ptLinkList = new HashMap<Id, Id>(); //linkId und facilId
		this.ptFacList = new ArrayList<Id>();
		this.vehicles = this.createVehicles();
	}
	
	public static void main(String[] args) throws Exception {

		SfCottbusPtSchedule cottbus = new SfCottbusPtSchedule();
		Scenario scen = ScenarioUtils.createScenario(ConfigUtils.createConfig());	
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
		
		for(int nLines = 0; nLines<16; nLines++) {
			List<String> transitStopIdStrings = new ArrayList<String>();
			List<String> stopLinkString = new ArrayList<String>();
			List<Integer> offset = new ArrayList<Integer>();		
			List<Integer> stoptime = new ArrayList<Integer>();
			String lineName = lines[nLines];
			double firstDep = 0.;
			double freq = 0.;
			int counter = 0;
			String linesfile = cottbus.ptdir+"lines\\all_congregated\\"+lineName+".csv";
			BufferedReader br = new BufferedReader(new FileReader(linesfile));
			if (nLines <=3) cottbus.pt_mode="tram";
			else if (nLines>3 && nLines<=12) cottbus.pt_mode="pt";
			else cottbus.pt_mode="train";
			System.out.println("parsing line" + lineName + " as "+cottbus.pt_mode);
				
				while(br.ready()) {
					String[] lineEntries = br.readLine().split(";");
					transitStopIdStrings.add(lineEntries[1]);
					stopLinkString.add(lineEntries[6].replaceAll("\"",""));
					
					offset.add(Integer.parseInt(lineEntries[4]));
					stoptime.add(Integer.parseInt(lineEntries[5]));
					
						
							
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
				
			List<TransitRouteStop> stopList = cottbus.createTransitStopFacilities(transitStopIdStrings, stopLinkString, offset,stoptime);

			
			String lineroutes = cottbus.ptdir+"lines\\all_congregated\\"+lineName+"_links.csv";
			List<String> routeLinks = cottbus.createRouteLinks(lineroutes);
			//todo
			
			String[] routes = routeLinks.toArray(new String[routeLinks.size()]);
			
			NetworkRoute netRoute = cottbus.createNetworkRoute(routes);
			
			TransitRoute transRoute = cottbus.createTransitRoute(lineName, netRoute, stopList);
			
			
			cottbus.addDeparturesToRoute(transRoute, firstDep, freq, lineName);
			
			TransitLine transLine = cottbus.createTransitLine(transRoute, lineName);
			
			cottbus.schedule.addTransitLine(transLine);
		}
		
		TransitScheduleWriterV1 scheduleWriter = new TransitScheduleWriterV1(cottbus.schedule);
		String outputfile = cottbus.ptdir+"lines\\schedule.xml";
		scheduleWriter.write(outputfile);
		
		System.out.println("Schedule written to: "+outputfile);
		
		NetworkWriter networkwriter = new NetworkWriter(cottbus.scenario.getNetwork());
		networkwriter.write(cottbus.ptdir+"network_pt.xml");
		
		System.out.println("Network written to: "+cottbus.ptdir+"network_pt.xml");
		
		VehicleWriterV1 writer = new VehicleWriterV1(cottbus.vehicles);
		writer.writeFile(cottbus.ptdir+"transitVehicles.xml");
		
	}
	
	

	private List<String> createRouteLinks(String lineroutes) throws IOException {
		List<String> routeLinks = new ArrayList<String>();
		BufferedReader br2 = new BufferedReader(new FileReader(lineroutes));
		
		while(br2.ready()) {
			routeLinks.add(br2.readLine().replaceAll("\"", ""));
		}
		br2.close();
		
		for (String string : routeLinks) {
			Id id = new IdImpl(string);
			String[] allowedModesArray = this.scenario.getNetwork().getLinks().get(id).getAllowedModes().toArray(new String[this.scenario.getNetwork().getLinks().get(id).getAllowedModes().size()]);
			Set<String> allowedModes = new TreeSet<String>();
			for (int ii=0; ii<allowedModesArray.length; ii++) {
				allowedModes.add(allowedModesArray[ii]);
			}
			if (!allowedModes.contains(this.pt_mode)) {
				allowedModes.add(this.pt_mode);
				this.scenario.getNetwork().getLinks().get(id).setAllowedModes(allowedModes);
			} 
			else ;
		}
		
		return routeLinks;
	}

	private Vehicles createVehicles() {
		
		Vehicles veh = VehicleUtils.createVehiclesContainer();
		VehicleCapacity cap93 = veh.getFactory().createVehicleCapacity();
		VehicleCapacity cap90 = veh.getFactory().createVehicleCapacity();
		VehicleCapacity cap64 = veh.getFactory().createVehicleCapacity();
		VehicleCapacity cap72 = veh.getFactory().createVehicleCapacity();
		
		cap93.setSeats(52);
		cap93.setStandingRoom(93);
		cap90.setSeats(52);
		cap90.setStandingRoom(90);
		cap64.setSeats(35);
		cap64.setStandingRoom(64);
		
		cap72.setSeats(72);
		cap72.setStandingRoom(30);
		
		
		Id tram_93pax = new IdImpl("tram_93pax");
		Id bus_90pax = new IdImpl("bus_90pax");
		Id bus_64pax = new IdImpl("bus_64pax");
		Id train = new IdImpl("train");
		
		veh.addVehicleType(veh.getFactory().createVehicleType(tram_93pax));
		veh.addVehicleType( veh.getFactory().createVehicleType(bus_90pax));
		veh.addVehicleType( veh.getFactory().createVehicleType(bus_64pax));
		veh.addVehicleType( veh.getFactory().createVehicleType(train));
		veh.getVehicleTypes().get(tram_93pax).setCapacity(cap93);
		veh.getVehicleTypes().get(bus_90pax).setCapacity(cap90);
		veh.getVehicleTypes().get(bus_64pax).setCapacity(cap64);
		veh.getVehicleTypes().get(train).setCapacity(cap72);
		veh.getVehicleTypes().get(tram_93pax).setLength(28.0);
		veh.getVehicleTypes().get(bus_90pax).setLength(18.0);
		veh.getVehicleTypes().get(bus_64pax).setLength(12.0);
		veh.getVehicleTypes().get(train).setLength(25.5);
		
		
		return veh;
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
	
	private void addDeparturesToRoute (TransitRoute transitRoute, double firstDep, double frequency, String lineName) {
		double lastDep = 22.0 * 3600;
		double currentDep = firstDep;
		int vehid = 0;
		for (int i = 0; i<5;i++){
			Id vd = new IdImpl(lineName+"_"+i);
		if (this.pt_mode.equals("pt")) this.vehicles.addVehicle( this.vehicles.getFactory().createVehicle(vd, this.vehicles.getVehicleTypes().get(new IdImpl("bus_90pax"))));
		if (this.pt_mode.equals("tram")) this.vehicles.addVehicle( this.vehicles.getFactory().createVehicle(vd, this.vehicles.getVehicleTypes().get(new IdImpl("tram_93pax"))));
		if (this.pt_mode.equals("train")) this.vehicles.addVehicle( this.vehicles.getFactory().createVehicle(vd, this.vehicles.getVehicleTypes().get(new IdImpl("train"))));
		
		}
		while (currentDep <= lastDep) {
			if (vehid==5) vehid = 0;
			
			Departure dep = this.schedulefactory.createDeparture(new IdImpl(transitRoute.getId().toString()+"_"+currentDep), currentDep);
			dep.setVehicleId(new IdImpl(lineName+"_"+vehid));
			vehid++;
			
			transitRoute.addDeparture(dep);
			currentDep+=frequency;
			

		}
	}
	
	private TransitRoute createTransitRoute(String transitRouteId, NetworkRoute netRoute, List<TransitRouteStop> stopList) {
		TransitRoute transitRoute = this.schedulefactory.createTransitRoute(new IdImpl(transitRouteId), netRoute, stopList, this.pt_mode);
		return transitRoute;
	}
	
	private List<TransitRouteStop> createTransitStopFacilities(List<String> ids, List<String> links, List<Integer> offsets, List<Integer> stoptime) {
		
		List<Id> linkList = new ArrayList<Id>();
		List<TransitRouteStop> stopList = new ArrayList<TransitRouteStop>();
		List<TransitStopFacility> facilList = new ArrayList<TransitStopFacility>();
		TransitStopFacility trastofac = null;
		Integer offsetsum = 0;
		
		
		for(int index = 0; index<links.size(); index++)	{
						
//			eine facility mit mehreren stops erstellen
			Id facilId = new IdImpl(ids.get(index));
			Id linkId = new IdImpl(links.get(index));
			int currOffset = offsetsum + offsets.get(index);
			offsetsum =  currOffset + stoptime.get(index);
			
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
					trastofac = this.schedule.getFactory().createTransitStopFacility(facilId, this.network.getLinks().get(linkId).getCoord(), false);
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
					trastofac = this.schedule.getFactory().createTransitStopFacility(facilId, this.network.getLinks().get(linkId).getCoord(), false);
					facilList.add(trastofac);
					trastofac.setLinkId(linkId);
					this.schedule.addStopFacility(trastofac);
				}
			}
			
			
			else  trastofac = this.schedule.getFacilities().get(this.ptLinkList.get(linkId));
			
//			System.out.println("FAC LIST: "+this.ptFacList.);
			
			if (!linkList.contains(linkId))
				trastofac.setLinkId(linkId);
			
			    if  (stoptime.get(index)==0){
			    	int offset = 20;
			    	if (index == 0) offset = 0;
					TransitRouteStop transStop = this.schedule.getFactory().createTransitRouteStop(trastofac, currOffset*60, currOffset*60+offset);
					transStop.setAwaitDepartureTime(true);
					stopList.add(transStop);

			    }
			    else{
					TransitRouteStop transStop = this.schedule.getFactory().createTransitRouteStop(trastofac, currOffset*60, offsetsum*60);
					transStop.setAwaitDepartureTime(true);

					stopList.add(transStop);

			    }
			    
			    
				linkList.add(linkId);
		}
		
		return stopList;
	}	
	
	private NetworkRoute createNetworkRoute(String[] links) {
		int length=links.length;
		Id origin = new IdImpl(links[0]);
		Id destination = new IdImpl(links[length-1]);
		NetworkRoute netRoute = new LinkNetworkRouteImpl(origin, destination);
		List<Id<Link>> linkList = new ArrayList<Id<Link>>();
		for(int index = 1; index<length-1; index++) {
				linkList.add(new IdImpl(links[index]));
			}
		netRoute.setLinkIds(origin, linkList, destination);
		return netRoute;
	}

}

