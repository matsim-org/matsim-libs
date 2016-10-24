/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.analysis.activity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.utils.collections.Tuple;

import playground.agarwalamit.utils.LoadMyScenarios;
import playground.vsp.analysis.modules.AbstractAnalysisModule;

/**
 * A class to analyze activity type to activity durations, activity start times
 * and activity end times considering repetition of 
 * same activity as different leg. 
 * @author amit
 */
public class ActivityType2ActDurationsAnalyzer extends AbstractAnalysisModule {
	private final ActivityType2DurationHandler actDurHandler;
	private final String eventsFile;
	
	public ActivityType2ActDurationsAnalyzer(final String outputDir) {
		super(ActivityType2ActDurationsAnalyzer.class.getSimpleName());
		String configFile = outputDir+"/output_config.xml";
		int lastIt = LoadMyScenarios.getLastIteration(configFile);
		this.eventsFile = outputDir+"/ITERS/it."+lastIt+"/"+lastIt+".events.xml.gz";
		this.actDurHandler = new ActivityType2DurationHandler(24*3600);
	}
	
	@Override
	public void preProcessData() {
		EventsManager manager = EventsUtils.createEventsManager();
		MatsimEventsReader reader = new MatsimEventsReader(manager);
		manager.addHandler(actDurHandler);
		reader.readFile(eventsFile);
	}
	
	
	@Override
	public List<EventHandler> getEventHandler() {
		return new ArrayList<>();
	}

	@Override
	public void postProcessData() {
	}
	
	@Override
	public void writeResults(String outputFolder) {
		
	}
	public Map<Id<Person>, Map<String, List<Double>>> getPersonId2ActivityType2ActivityDurations(){
		return actDurHandler.getPersonId2ActDurations();
	}
	
	public Set<String> getActivityTypes(){
		return actDurHandler.getActivityTypes();
	}
	
	public Map<Id<Person>, List<Tuple<String, Double>>> getPersonId2ActEndTimes(){
		return this.actDurHandler.getPersonId2ActEndTimes();
	}
	
	public Map<Id<Person>, List<Tuple<String, Double>>> getPersonId2ActStartTimes(){
		return this.actDurHandler.getPersonId2ActStartTimes();
	}
}