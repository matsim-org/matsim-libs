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

package playground.andreas.P.replan;

import java.io.IOException;
import java.util.TreeMap;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.xml.sax.SAXException;

import playground.andreas.P.replan.ScorePlansHandler.ScoreContainer;

public class ScorePlans {
	
	private final static Logger log = Logger.getLogger(ScorePlans.class);
	
	public static TreeMap<Id, Double> scorePlans(String eventsFile, Network net){
		
		EventsManager eventsManager = (EventsManager) EventsUtils.createEventsManager();
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(eventsManager);
		
		ScorePlansHandler scorePlansHandler = new ScorePlansHandler(net);
		eventsManager.addHandler(scorePlansHandler);
		
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
		
		TreeMap<Id, Double> scores = new TreeMap<Id, Double>();
		TreeMap<Id, ScoreContainer> personId2ScoreMap = scorePlansHandler.getPersonId2ScoreMap();
		
		for (ScoreContainer score : personId2ScoreMap.values()) {
			if(score.passengersCurrentlyInVeh != 0){
				log.warn("Driver " + score.driverId + " has passengers in vehicle. Should be empty now");
			}
			
			scores.put(score.driverId, new Double(score.earnings - score.costs));
		}		
		
		return scores;
	}

}
