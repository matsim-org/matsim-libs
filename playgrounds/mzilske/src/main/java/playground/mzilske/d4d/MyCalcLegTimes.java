/* *********************************************************************** *
 * project: org.matsim.*
 * CalcLegTimes.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.mzilske.d4d;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author mrieser
 *
 * Calculates the distribution of legs-durations, e.g. how many legs took at
 * most 5 minutes, how many between 5 and 10 minutes, and so on.
 * Also calculates the average trip duration.
 * Trips ended because of vehicles being stuck are not counted.
 * <p/>
 * yyyy This is a prototype, which might replace the original class since it is more useful for some practical things.  kai, jul'11 
 */
public class MyCalcLegTimes implements PersonDepartureEventHandler, PersonArrivalEventHandler {

	private Map<Id, Integer> agent2Time = new HashMap<Id, Integer>();
	private Map<Id, Integer> agent2departureTime = new HashMap<Id, Integer>();
	private String mode;


	public MyCalcLegTimes(Scenario scenario, String mode) {
		this.mode = mode;
	}

	public static void main(String[] args) {
		String path = "/Users/zilske/matsim-without-history/playgrounds/trunk/mzilske/output/0211-capital-only-05freespeed-beginning-disutility-travel/";
		EventsManager eventsManager = EventsUtils.createEventsManager();
		Scenario scenario = ScenarioUtils.createScenario(RunSimulation.createConfig("", 1.0));
		scenario.getConfig().planCalcScore().setWriteExperiencedPlans(true);
		new MatsimNetworkReader(scenario.getNetwork()).readFile("/Users/zilske/d4d/output/network.xml");
		MyCalcLegTimes events2ScoreCar = new MyCalcLegTimes(scenario, "car");
		eventsManager.addHandler(events2ScoreCar);
		MyCalcLegTimes events2ScoreOther = new MyCalcLegTimes(scenario, "other");
		eventsManager.addHandler(events2ScoreOther);
		MyCalcLegTimes events2ScoreBoth = new MyCalcLegTimes(scenario, ".both.");
		eventsManager.addHandler(events2ScoreBoth);
		new MatsimEventsReader(eventsManager).readFile(path+"/ITERS/it.180/180.events.xml.gz");
		// new MatsimEventsReader(eventsManager).readFile("/Users/zilske/matsim-without-history/playgrounds/trunk/mzilske/output2freespeed/ITERS/it.180/my_guy.xml");
		events2ScoreCar.writeStats(path+"stats_car.txt");
		events2ScoreOther.writeStats(path+"stats_other.txt");
		events2ScoreBoth.writeStats(path+"stats_both.txt");

	}

	private void writeStats(String filename) {
		BufferedWriter legStatsFile = IOUtils.getBufferedWriter(filename);
		for (Integer time : agent2Time.values()) {
			try {
				legStatsFile.write(time+"\n");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			legStatsFile.close();
		} catch (IOException e) {

		}
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (mode.equals(".both.") || mode.equals(event.getLegMode())) {
			Integer time = agent2Time.get(event.getPersonId());
			if (time == null) time = 0;
			time += (int) event.getTime() - agent2departureTime.get(event.getPersonId());
			agent2Time.put(event.getPersonId(), time);
			agent2departureTime.remove(event.getPersonId());
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (mode.equals(".both.") || mode.equals(event.getLegMode())) {
			agent2departureTime.put(event.getPersonId(), (int) event.getTime());
		}
	}


}
