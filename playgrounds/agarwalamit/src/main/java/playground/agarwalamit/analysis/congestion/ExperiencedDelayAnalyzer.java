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
package playground.agarwalamit.analysis.congestion;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.utils.AreaFilter;
import playground.agarwalamit.utils.MapUtils;
import playground.agarwalamit.utils.PersonFilter;
import playground.vsp.congestion.handlers.CongestionHandlerImplV3;

/**
 * This analyzer calculates delay from link enter and link leave events and therefore provides only experienced delay.
 * <p> In order to get the caused delay for each person, see {@link CausedDelayAnalyzer}
 * 
 * @author amit
 */
public class ExperiencedDelayAnalyzer {
	
	private final static Logger LOGGER = Logger.getLogger(ExperiencedDelayAnalyzer.class);
	private final String eventsFile;
	private final FilteredExperienceDelayHandler congestionHandler;
	private final Scenario scenario;

	public ExperiencedDelayAnalyzer(final String eventFile, final Scenario scenario, final int noOfTimeBins, 
			final String userGroup, final PersonFilter personFilter, final AreaFilter areaFilter) {
		this.eventsFile = eventFile;
		this.scenario = scenario;
		this.congestionHandler = new FilteredExperienceDelayHandler(this.scenario, noOfTimeBins, userGroup, personFilter, areaFilter);
		LOGGER.info("Area and user group filtering is used, links fall inside the given shape and belongs to the given user group will be considered.");
	}
	
	public ExperiencedDelayAnalyzer(final String eventFile, final Scenario scenario, final int noOfTimeBins, 
			final String userGroup, final PersonFilter personFilter) {
		this(eventFile, scenario, noOfTimeBins, userGroup, personFilter, null);
		LOGGER.info("Usergroup filtering is used, result will include all links but persons from given user group only.");
	}
	
	public ExperiencedDelayAnalyzer(final String eventFile, final Scenario scenario, final int noOfTimeBins, 
			final AreaFilter areaFilter) {
		this(eventFile, scenario, noOfTimeBins, null, null, areaFilter);
		LOGGER.info("Area filtering is used, result will include links falls inside the given shape and persons from all user groups.");
	}
	
	public ExperiencedDelayAnalyzer(final String eventFile, final Scenario scenario, final int noOfTimeBins) {
		this(eventFile, scenario, noOfTimeBins, null, null, null);
		LOGGER.info("No filtering is used, result will include all links, persons from all user groups.");
	}
	
	public void run(){
		EventsManager eventsManager = EventsUtils.createEventsManager();
		MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);
		eventsManager.addHandler(this.congestionHandler);
		eventsReader.readFile(this.eventsFile);
	}
	
	public void writeResults(String outputFile) {
		SortedMap<Double, Double> data = getTimeBin2Delay();
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFile);
		try {
			writer.write("\timebin \t delayInMin \n");
			for(double d : data.keySet()){
				writer.write(d+"\t"+(data.get(d)/60)+"\n");
			}
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException("Data is not written to file. Reason "+ e);
		}
	}

	public double getTotalDelaysInHours (){
		return this.congestionHandler.getTotalDelayInHours();
	}
	
	public SortedMap<Double, Map<Id<Person>, Double>> getTimeBin2AffectedPersonId2Delay() {
		return this.congestionHandler.getDelayPerPersonAndTimeInterval();
	}
	
	public Map<Double, Map<Id<Link>, Double>> getTimeBin2LinkId2Delay() {
		return this.congestionHandler.getDelayPerLinkAndTimeInterval();
	}
	
	public Map<Double, Map<Id<Link>, Integer>> getTimeBin2LinkLeaveCount(){
		return this.congestionHandler.getTime2linkIdLeaveCount();
	}
	
	public SortedMap<Double, Double> getTimeBin2Delay(){
		SortedMap<Double, Double> outData = new TreeMap<>();
		for(double d : getTimeBin2LinkId2Delay().keySet()){
			double delay= MapUtils.doubleValueSum(getTimeBin2LinkId2Delay().get(d));
			outData.put(d,  delay);
		}
		return outData;
	}
	
	/**
	 * This method will return total delay without any filtering for area or user group thus not necessarily matches the outcome above.
	 */
	@Deprecated 
	public void checkTotalDelayUsingAlternativeMethod(){
		EventsManager em = EventsUtils.createEventsManager();
		CongestionHandlerImplV3 implV3 = new CongestionHandlerImplV3(em, this.scenario);
		MatsimEventsReader eventsReader = new MatsimEventsReader(em);
		em.addHandler(implV3);
		eventsReader.readFile(this.eventsFile);
		if(implV3.getTotalDelay()/3600. !=this.congestionHandler.getTotalDelayInHours())
			throw new RuntimeException("Total Delays are not equal using two methods; values are "+implV3.getTotalDelay()/3600+","+this.congestionHandler.getTotalDelayInHours());
	}
}
