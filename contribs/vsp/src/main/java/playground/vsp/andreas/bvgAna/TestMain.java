/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.vsp.andreas.bvgAna;

import java.util.TreeMap;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.VehicleReaderV1;

import playground.vsp.andreas.bvgAna.level1.AgentId2EnterLeaveVehicleEventHandler;
import playground.vsp.andreas.bvgAna.level1.AgentId2PtTripTravelTimeMap;
import playground.vsp.andreas.bvgAna.level1.PersonEnterLeaveVehicle2ActivityHandler;
import playground.vsp.andreas.bvgAna.level1.StopId2LineId2Pulk;
import playground.vsp.andreas.bvgAna.level1.StopId2PersonEnterLeaveVehicleHandler;
import playground.vsp.andreas.bvgAna.level1.VehDelayAtStopHistogram;
import playground.vsp.andreas.bvgAna.level1.VehId2DelayAtStopMap;
import playground.vsp.andreas.bvgAna.level1.VehId2OccupancyHandler;
import playground.vsp.andreas.bvgAna.level1.VehId2PersonEnterLeaveVehicleMap;
import playground.vsp.andreas.bvgAna.level2.StopId2DelayOfLine24hMap;
import playground.vsp.andreas.bvgAna.level2.StopId2RemainSeatedDataMap;
import playground.vsp.andreas.bvgAna.level2.VehId2AgentIds;
import playground.vsp.andreas.bvgAna.level2.VehId2LoadMap;
import playground.vsp.andreas.bvgAna.level3.AgentId2StopDifferenceMap;
import playground.vsp.andreas.bvgAna.level4.StopId2MissedVehMap;
import playground.vsp.andreas.bvgAna.level4.StopId2MissedVehMapData;

/**
 * Simple test class
 *
 * @author aneumann
 */
public class TestMain {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

//		String eventsFile = "f:\\ils4\\data\\bvg\\runs\\bvg.run128.25pct\\it.100\\bvg.run128.25pct.100.events.xml.gz";
//		String plansFile = "f:\\ils4\\data\\bvg\\runs\\bvg.run128.25pct\\it.100\\bvg.run128.25pct.100.plans.selected.xml.gz";
//		String netFile = "E:\\temp\\network.final.xml.gz";
//		String transitScheduleFile = "E:\\temp\\transitSchedule.xml.gz";
//		String vehDefinitionFile = "E:\\temp\\transitVehicles.final.xml.gz";		
//		String outFile = "E:\\temp\\out.txt";
		
		String eventsFile = "./bvg.run128.25pct.100.events.xml.gz";
		String plansFile = "./bvg.run128.25pct.100.plans.selected.xml.gz";
		String netFile = "./network.final.xml.gz";
		String transitScheduleFile = "./transitSchedule.xml.gz";
		String vehDefinitionFile = "./transitVehicles.final.xml.gz";
		String outFile = "./out.txt";

		EventsManager eventsManager = EventsUtils.createEventsManager();
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(eventsManager);

		MutableScenario sc = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		sc.getConfig().transit().setUseTransit(true);

		new MatsimNetworkReader(sc.getNetwork()).readFile(netFile);
		final Population plans = sc.getPopulation();
		PopulationReader popReader = new PopulationReader(sc);
		popReader.readFile(plansFile);

		new TransitScheduleReader(sc).readFile(transitScheduleFile);

		new VehicleReaderV1(sc.getTransitVehicles()).readFile(vehDefinitionFile);

		TreeSet<Id<Person>> agentIds = new TreeSet<>();
		agentIds.add(Id.create("1000", Person.class));
		agentIds.add(Id.create("10001", Person.class));
		agentIds.add(Id.create("10002", Person.class));
		agentIds.add(Id.create("2176", Person.class));
		agentIds.add(Id.create("182", Person.class));

		AgentId2StopDifferenceMap comp = new AgentId2StopDifferenceMap(plans, agentIds);
		eventsManager.addHandler(comp);

		TreeSet<Id<TransitStopFacility>> stopIds = new TreeSet<>();
		stopIds.add(Id.create("812013.1", TransitStopFacility.class));
		stopIds.add(Id.create("792200.4", TransitStopFacility.class));
		stopIds.add(Id.create("792050.2", TransitStopFacility.class));
		stopIds.add(Id.create("801040.1", TransitStopFacility.class));
		stopIds.add(Id.create("804070.2", TransitStopFacility.class));

		StopId2PersonEnterLeaveVehicleHandler stophandler = new StopId2PersonEnterLeaveVehicleHandler(stopIds);
//		eventsManager.addHandler(stophandler);
		PersonEnterLeaveVehicle2ActivityHandler enterLeaveHandler = new PersonEnterLeaveVehicle2ActivityHandler(agentIds);
//		eventsManager.addHandler(enterLeaveHandler);
		AgentId2EnterLeaveVehicleEventHandler aid2elhandler = new AgentId2EnterLeaveVehicleEventHandler(agentIds);
//		eventsManager.addHandler(aid2elhandler);

		VehId2OccupancyHandler veh2occu = new VehId2OccupancyHandler();
//		eventsManager.addHandler(veh2occu);

		VehId2LoadMap veh2load = new VehId2LoadMap(sc.getTransitVehicles());
//		eventsManager.addHandler(veh2load);

		StopId2RemainSeatedDataMap remainSeated = new StopId2RemainSeatedDataMap();
//		eventsManager.addHandler(remainSeated);

		AgentId2PtTripTravelTimeMap a2ptleg = new AgentId2PtTripTravelTimeMap(agentIds);
//		eventsManager.addHandler(a2ptleg);

		VehId2DelayAtStopMap v2delay = new VehId2DelayAtStopMap();
//		eventsManager.addHandler(v2delay);

		StopId2DelayOfLine24hMap s224h = new StopId2DelayOfLine24hMap();
//		eventsManager.addHandler(s224h);

		VehId2PersonEnterLeaveVehicleMap v2ELM = new VehId2PersonEnterLeaveVehicleMap();
//		eventsManager.addHandler(v2ELM);

		VehId2AgentIds v2agid = new VehId2AgentIds();
//		eventsManager.addHandler(v2agid);

		StopId2LineId2Pulk s2pulk = new StopId2LineId2Pulk();
//		eventsManager.addHandler(s2pulk);

		StopId2MissedVehMap s2mv = new StopId2MissedVehMap(plans);
		eventsManager.addHandler(s2mv);
		
		VehDelayAtStopHistogram dH = new VehDelayAtStopHistogram(24 * 60);
//		eventsManager.addHandler(dH);

		reader.readFile(eventsFile);

//		Map<Id, List<Tuple<Id, Double>>> testMap = comp.getAgentId2StopDifferenceMap();
//
//		Map<Id, List<Tuple<Id, Integer>>> testMap2 = comp.getNumberOfMissedVehiclesMap();
//
//		TreeMap<Id, Id> map3 = new TransitScheduleDataProvider(sc.getTransitSchedule()).getRouteId2lineIdMap();
//
//		Map<Id, List<PersonEntersVehicleEvent>> map4 = stophandler.getStopId2PersonEnterEventMap();
//		Map<Id, List<PersonLeavesVehicleEvent>> map5 = stophandler.getStopId2PersonLeaveEventMap();
//
//		Map<PersonEntersVehicleEvent, ActivityEndEvent> map6 = enterLeaveHandler.getPersonEntersVehicleEvent2ActivityEndEvent();
//		Map<PersonLeavesVehicleEvent, ActivityStartEvent> map7 = enterLeaveHandler.getPersonLeavesVehicleEvent2ActivityStartEvent();
//		TreeMap<Id, ArrayList<PersonEntersVehicleEvent>> map8 = aid2elhandler.getAgentId2EnterEventMap();
//		TreeMap<Id, ArrayList<PersonLeavesVehicleEvent>> map9 = aid2elhandler.getAgentId2LeaveEventMap();
//
//		String name = new TransitScheduleDataProvider(sc.getTransitSchedule()).getStopName(stopIds.first());
//
//		TreeMap<Id, PersonImpl> map10 = AgentId2PersonMap.getAgentId2PersonMap(plans, agentIds);
//
//		int occu = veh2occu.getVehicleLoad(Id.create("veh_8"), 46127.0);
//		double load = veh2load.getVehLoadByTime(Id.create("veh_8"), 46127.0);
//
//		Map<Id, List<StopId2RemainSeatedDataMapData>> remSeat = remainSeated.getStopId2RemainSeatedDataMap();
//		int rS = remSeat.get(Id.create("812013.1")).get(68).getNumberOfAgentsRemainedSeated();
//		double fS = remSeat.get(Id.create("812013.1")).get(68).getFractionRemainedSeated();
//
//		TreeMap<Id, LinkedList<VehId2DelayAtStopMapData>> vdelay = v2delay.getVehId2DelayAtStopMap();
//
//		TreeMap<Id, StopId2DelayOfLine24hMapData> s22 = s224h.getStopId2DelayOfLine24hMap();
//
//		TreeMap<Id, ArrayList<PersonEntersVehicleEvent>> v2em = v2ELM.getVehId2PersonEnterEventMap();
//		TreeMap<Id, ArrayList<PersonLeavesVehicleEvent>> v2lm = v2ELM.getVehId2PersonLeaveEventMap();
//
//		@SuppressWarnings("unused")
//		Set<Id> v2agidr = v2agid.getAgentIdsInVehicle(Id.create("veh_8"), 46127.0);
//
//		TreeMap<Id, TreeMap<Id, List<StopId2LineId2PulkData>>> s2pulkR = s2pulk.getStopId2LineId2PulkDataList();

		TreeMap<Id, StopId2MissedVehMapData> s2mvR = s2mv.getStopId2StopId2MissedVehMapDataMap();
		s2mv.writeResultsToFile(outFile);
		
//		dH.dumpToConsole();
//		dH.write("F:/temp/delayHistogram.txt");

		System.out.println("Waiting");

	}

}
