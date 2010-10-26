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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEvent;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReaderMatsimV4;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.routes.ExperimentalTransitRouteFactory;
import org.matsim.transitSchedule.api.TransitScheduleReader;
import org.xml.sax.SAXException;

import playground.andreas.bvgAna.agentDelayAtStopComparator.AgentDelayAtStopComparator;
import playground.andreas.bvgAna.agentId2EnterLeaveEvent.AgentId2EnterLeaveEventHandler;
import playground.andreas.bvgAna.personEnterLeave2Act.PersonEnterLeave2ActHandler;
import playground.andreas.bvgAna.stopId2VehEnterLeave.StopId2PersonEnterLeaveHandler;
import playground.andreas.bvgAna.transitSchedule.TransitScheduleMapProvider;

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
		// TODO Auto-generated method stub
		
		String eventsFile = "F:/bvgAna/output/ITERS/it.0/bvgAna.0.events.xml.gz";
		String plansFile = "F:/bvgAna/output/ITERS/it.0/bvgAna.0.plans.xml.gz";
		String netFile = "F:/bvgAna/input/network.xml";
		String transitScheduleFile = "F:/bvgAna/input/transitSchedule.xml";
		
		EventsManagerImpl eventsManager = new EventsManagerImpl();		
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(eventsManager);		
		
		ScenarioImpl sc = new ScenarioImpl();
		sc.getConfig().getModule("scenario").addParam("useTransit", "true");
		sc.getConfig().getModule("scenario").addParam("useVehicles", "true");
		sc.getNetwork().getFactory().setRouteFactory(TransportMode.pt, new ExperimentalTransitRouteFactory());
		
		new MatsimNetworkReader(sc).readFile(netFile);		
		final PopulationImpl plans = (PopulationImpl) sc.getPopulation();		
		PopulationReaderMatsimV4 popReader = new PopulationReaderMatsimV4(sc);
		popReader.readFile(plansFile);
		
		try {
			new TransitScheduleReader(sc).readFile(transitScheduleFile);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (SAXException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (ParserConfigurationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		TreeSet<Id> agentIds = new TreeSet<Id>();
		agentIds.add(new IdImpl("1000"));
		agentIds.add(new IdImpl("10001"));	
		agentIds.add(new IdImpl("10002"));
		agentIds.add(new IdImpl("2176"));
		agentIds.add(new IdImpl("182"));
		
		AgentDelayAtStopComparator comp = new AgentDelayAtStopComparator(plans, agentIds);
		eventsManager.addHandler(comp);
		
		TreeSet<Id> stopIds = new TreeSet<Id>();
		stopIds.add(new IdImpl("812013.1"));
		stopIds.add(new IdImpl("792200.4"));	
		stopIds.add(new IdImpl("792050.2"));
		stopIds.add(new IdImpl("801040.1"));
		stopIds.add(new IdImpl("804070.2"));
		
		StopId2PersonEnterLeaveHandler stophandler = new StopId2PersonEnterLeaveHandler(stopIds);
		eventsManager.addHandler(stophandler);
		PersonEnterLeave2ActHandler enterLeaveHandler = new PersonEnterLeave2ActHandler(agentIds);
		eventsManager.addHandler(enterLeaveHandler);
		AgentId2EnterLeaveEventHandler aid2elhandler = new AgentId2EnterLeaveEventHandler(agentIds);
		eventsManager.addHandler(aid2elhandler);
		
		try {
			reader.parse(eventsFile);
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		TreeMap<Id, ArrayList<Tuple<Id, Double>>> testMap = comp.getDifferenceMap();
		
		TreeMap<Id, ArrayList<Tuple<Id, Integer>>> testMap2 = comp.getNumberOfMissedVehiclesMap();
		
		TreeMap<Id, Id> map3 = new TransitScheduleMapProvider(sc.getTransitSchedule()).getRouteId2lineIdMap();
		
		TreeMap<Id, ArrayList<PersonEntersVehicleEvent>> map4 = stophandler.getStopId2PersonEnterEventMap();
		TreeMap<Id, ArrayList<PersonLeavesVehicleEvent>> map5 = stophandler.getStopId2PersonLeaveEventMap();
		
		HashMap<PersonEntersVehicleEvent, ActivityEndEvent> map6 = enterLeaveHandler.getPersonsEntersVehicleEvent2ActivityEndEvent();
		HashMap<PersonLeavesVehicleEvent, ActivityStartEvent> map7 = enterLeaveHandler.getPersonLeavesVehicleEvent2ActivityStartEvent();
		TreeMap<Id, ArrayList<PersonEntersVehicleEvent>> map8 = aid2elhandler.getAgentId2EnterEventMap();
		TreeMap<Id, ArrayList<PersonLeavesVehicleEvent>> map9 = aid2elhandler.getAgentId2LeaveEventMap();

		System.out.println("Waiting");

	}

}
