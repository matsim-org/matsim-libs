/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.pieter.singapore.utils.events;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Queue;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import others.sergioo.util.dataBase.DataBaseAdmin;

public class EZLinkToEvents {
	
	
	public static void main(String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, SQLException{
		DataBaseAdmin dba = new DataBaseAdmin(new File("f:/data/matsim2.properties"));
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader nwr = new MatsimNetworkReader(scenario);
		nwr.readFile("F:\\data\\sing2.2\\input\\network\\network100.xml.gz");
		TransitScheduleReader tsr = new TransitScheduleReader(scenario);
		tsr.readFile("F:\\data\\sing2.2\\input\\transit\\transitSchedule.xml");
		EventsManager em = EventsUtils.createEventsManager();
		EventWriterXML ewx = new EventWriterXML("F:\\data\\sing2.2\\ezlinkevents.xml");
		em.addHandler(ewx);
		Queue<Event> eventQueue = new LinkedList<Event>();
		for (Event event : eventQueue) {
			em.processEvent(event);
		}
	}
}
