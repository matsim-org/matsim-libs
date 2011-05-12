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

package playground.andreas.bvgAna;

import java.util.TreeMap;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.pt.routes.ExperimentalTransitRouteFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.vehicles.VehicleReaderV1;

import playground.andreas.bvgAna.level1.AgentId2EnterLeaveVehicleEventHandler;
import playground.andreas.bvgAna.level1.AgentId2PtTripTravelTimeMap;
import playground.andreas.bvgAna.level1.PersonEnterLeaveVehicle2ActivityHandler;
import playground.andreas.bvgAna.level1.StopId2LineId2Pulk;
import playground.andreas.bvgAna.level1.StopId2PersonEnterLeaveVehicleHandler;
import playground.andreas.bvgAna.level1.VehDelayAtStopHistogram;
import playground.andreas.bvgAna.level1.VehId2DelayAtStopMap;
import playground.andreas.bvgAna.level1.VehId2OccupancyHandler;
import playground.andreas.bvgAna.level1.VehId2PersonEnterLeaveVehicleMap;
import playground.andreas.bvgAna.level2.StopId2DelayOfLine24hMap;
import playground.andreas.bvgAna.level2.StopId2RemainSeatedDataMap;
import playground.andreas.bvgAna.level2.VehId2AgentIds;
import playground.andreas.bvgAna.level2.VehId2LoadMap;
import playground.andreas.bvgAna.level3.AgentId2StopDifferenceMap;
import playground.andreas.bvgAna.level4.StopId2MissedVehMap;
import playground.andreas.bvgAna.level4.StopId2MissedVehMapData;

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

		ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		sc.getConfig().getModule("scenario").addParam("useTransit", "true");
		sc.getConfig().getModule("scenario").addParam("useVehicles", "true");
		sc.getNetwork().getFactory().setRouteFactory(TransportMode.pt, new ExperimentalTransitRouteFactory());

		new MatsimNetworkReader(sc).readFile(netFile);
		final PopulationImpl plans = (PopulationImpl) sc.getPopulation();
		MatsimPopulationReader popReader = new MatsimPopulationReader(sc);
		popReader.readFile(plansFile);

		new TransitScheduleReader(sc).readFile(transitScheduleFile);

		new VehicleReaderV1(sc.getVehicles()).readFile(vehDefinitionFile);

		TreeSet<Id> agentIds = new TreeSet<Id>();
		agentIds.add(new IdImpl("1000"));
		agentIds.add(new IdImpl("10001"));
		agentIds.add(new IdImpl("10002"));
		agentIds.add(new IdImpl("2176"));
		agentIds.add(new IdImpl("182"));

		AgentId2StopDifferenceMap comp = new AgentId2StopDifferenceMap(plans, agentIds);
		eventsManager.addHandler(comp);

		TreeSet<Id> stopIds = new TreeSet<Id>();
		stopIds.add(new IdImpl("812013.1"));
		stopIds.add(new IdImpl("792200.4"));
		stopIds.add(new IdImpl("792050.2"));
		stopIds.add(new IdImpl("801040.1"));
		stopIds.add(new IdImpl("804070.2"));

		StopId2PersonEnterLeaveVehicleHandler stophandler = new StopId2PersonEnterLeaveVehicleHandler(stopIds);
//		eventsManager.addHandler(stophandler);
		PersonEnterLeaveVehicle2ActivityHandler enterLeaveHandler = new PersonEnterLeaveVehicle2ActivityHandler(agentIds);
//		eventsManager.addHandler(enterLeaveHandler);
		AgentId2EnterLeaveVehicleEventHandler aid2elhandler = new AgentId2EnterLeaveVehicleEventHandler(agentIds);
//		eventsManager.addHandler(aid2elhandler);

		VehId2OccupancyHandler veh2occu = new VehId2OccupancyHandler();
//		eventsManager.addHandler(veh2occu);

		VehId2LoadMap veh2load = new VehId2LoadMap(sc.getVehicles());
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

		reader.parse(eventsFile);

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
//		int occu = veh2occu.getVehicleLoad(new IdImpl("veh_8"), 46127.0);
//		double load = veh2load.getVehLoadByTime(new IdImpl("veh_8"), 46127.0);
//
//		Map<Id, List<StopId2RemainSeatedDataMapData>> remSeat = remainSeated.getStopId2RemainSeatedDataMap();
//		int rS = remSeat.get(new IdImpl("812013.1")).get(68).getNumberOfAgentsRemainedSeated();
//		double fS = remSeat.get(new IdImpl("812013.1")).get(68).getFractionRemainedSeated();
//
//		TreeMap<Id, LinkedList<VehId2DelayAtStopMapData>> vdelay = v2delay.getVehId2DelayAtStopMap();
//
//		TreeMap<Id, StopId2DelayOfLine24hMapData> s22 = s224h.getStopId2DelayOfLine24hMap();
//
//		TreeMap<Id, ArrayList<PersonEntersVehicleEvent>> v2em = v2ELM.getVehId2PersonEnterEventMap();
//		TreeMap<Id, ArrayList<PersonLeavesVehicleEvent>> v2lm = v2ELM.getVehId2PersonLeaveEventMap();
//
//		@SuppressWarnings("unused")
//		Set<Id> v2agidr = v2agid.getAgentIdsInVehicle(new IdImpl("veh_8"), 46127.0);
//
//		TreeMap<Id, TreeMap<Id, List<StopId2LineId2PulkData>>> s2pulkR = s2pulk.getStopId2LineId2PulkDataList();

		TreeMap<Id, StopId2MissedVehMapData> s2mvR = s2mv.getStopId2StopId2MissedVehMapDataMap();
		s2mv.writeResultsToFile(outFile);
		
//		dH.dumpToConsole();
//		dH.write("F:/temp/delayHistogram.txt");

		System.out.println("Waiting");

	}

}
