/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.mixedTraffic.patnaIndia.policies.analysis;

import java.io.BufferedWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import jdk.nashorn.internal.scripts.JO;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.congestion.FilteredExperienceDelayHandler;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaPersonFilter;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaPersonFilter.PatnaUserGroup;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.agarwalamit.utils.LoadMyScenarios;
import playground.agarwalamit.utils.NumberUtils;

/**
 * @author amit
 */

public class PatnaExperienceDelayAnalyzer {

	public static void main(String[] args) {
		String outputDir = "../../repos/runs-svn/patnaIndia/run108/jointDemand/policies/"+ PatnaUtils.PCU_2W+"pcu/";
		String runCases [] = {"baseCaseCtd","bikeTrack","trafficRestrain","both","BT-mb"};
		for (String runCase : runCases){
			String eventsFile = outputDir+runCase+"/output_events.xml.gz";
			Scenario sc = LoadMyScenarios.loadScenarioFromOutputDir(outputDir+runCase);
			String outFile = outputDir+runCase+"/analysis/mode2delay.txt";
			new PatnaExperienceDelayAnalyzer().run(eventsFile, sc, outFile);
		}
	}

	private void run (final String eventsFile, final Scenario scenario, final String outFile) {
		PatnaPersonFilter personFilter = new PatnaPersonFilter();
		int noOfTimeBins = 1;
		double simEndTime = scenario.getConfig().qsim().getEndTime();
		BufferedWriter writer = IOUtils.getBufferedWriter(outFile);
		try {
			writer.write("userGroup\tmode\tdelayInHr\n");
		} catch (Exception e) {
			throw new RuntimeException("Data is not written. Reason :"+ e);
		}
		for ( PatnaUserGroup pug : PatnaUserGroup.values()) {
			EventsManager events = EventsUtils.createEventsManager();
			PatnaExperienceDelayHandler person2modeHandler = new PatnaExperienceDelayHandler();
			FilteredExperienceDelayHandler congestionHandler = new FilteredExperienceDelayHandler(scenario, noOfTimeBins, pug.toString(), personFilter);

			events.addHandler(person2modeHandler);
			events.addHandler(congestionHandler);

			new MatsimEventsReader(events).readFile(eventsFile);

			SortedMap<String, Double> mode2delay = getMode2Delay(person2modeHandler.personId2Mode, congestionHandler.getDelayPerPersonAndTimeInterval().get(simEndTime));

			for (String mode : mode2delay.keySet()) {
				double delay = NumberUtils.round(mode2delay.get(mode)/3600., 2);
				try {
					writer.write(pug.toString()+"\t"+mode+"\t"+delay+"\n");
				} catch (Exception e) {
					throw new RuntimeException("Data is not written. Reason :"+ e);
				}
			}
		}
		try {
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written. Reason :"+ e);
		}

	}

	private SortedMap<String, Double> getMode2Delay(Map<Id<Person>, String> personId2Mode, Map<Id<Person>, Double> map) {
		SortedMap<String, Double> mode2delay = new TreeMap<>();

		for(Id<Person> personId : map.keySet()) {
			String mode  = personId2Mode.get(personId);

			if(mode2delay.containsKey(mode)) {
				mode2delay.put( mode, mode2delay.get(mode) + map.get(personId) );
			} else {
				mode2delay.put(mode, map.get(personId));
			}
		}
		return mode2delay;
	}

	//=== handler

	public class PatnaExperienceDelayHandler implements PersonDepartureEventHandler {

		private final Map<Id<Person>, String> personId2Mode = new HashMap<>();

		@Override
		public void reset(int iteration) {
			personId2Mode.clear();
		}

		@Override
		public void handleEvent(PersonDepartureEvent event) {
			String mode = personId2Mode.get(event.getPersonId());
			if(mode == null ) personId2Mode.put(event.getPersonId(), event.getLegMode());
			else if(! mode.equals(event.getLegMode()) ) throw new RuntimeException("Person "+event.getPersonId()+ " have different modes in the legs."
					+ " This situation is not incorporated yet.");
		}
	}
}