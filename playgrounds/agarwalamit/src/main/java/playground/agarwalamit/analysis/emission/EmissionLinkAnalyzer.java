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
package playground.agarwalamit.analysis.emission;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.emissions.events.EmissionEventsReader;
import org.matsim.contrib.emissions.types.ColdPollutant;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.contrib.emissions.utils.EmissionUtils;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.EventHandler;

import playground.agarwalamit.analysis.emission.sorting.SortedColdEmissionPerLinkHandler;
import playground.agarwalamit.analysis.emission.sorting.SortedWarmEmissionPerLinkHandler;
import playground.vsp.analysis.modules.AbstractAnalysisModule;

/**
 * @author amit
 *
 */
public class EmissionLinkAnalyzer extends AbstractAnalysisModule {
	private final Logger logger = Logger.getLogger(EmissionLinkAnalyzer.class);
	private final String emissionEventsFile;
	private EmissionUtils emissionUtils;
	private SortedWarmEmissionPerLinkHandler warmHandler;
	private SortedColdEmissionPerLinkHandler coldHandler;
	private Map<Double, Map<Id<Link>, Map<WarmPollutant, Double>>> link2WarmEmissions;
	private Map<Double, Map<Id<Link>, Map<ColdPollutant, Double>>> link2ColdEmissions;
	private SortedMap<Double, Map<Id<Link>, SortedMap<String, Double>>> link2TotalEmissions;

	public EmissionLinkAnalyzer(double simulationEndTime, String emissionEventFile, int noOfTimeBins, String shapeFile, Network network ) {
		super(EmissionLinkAnalyzer.class.getSimpleName());
		this.emissionEventsFile = emissionEventFile;
		this.logger.info("Aggregating emissions for each "+simulationEndTime/noOfTimeBins+" sec time bin.");
		this.emissionUtils = new EmissionUtils();
		this.warmHandler = new SortedWarmEmissionPerLinkHandler(simulationEndTime, noOfTimeBins, shapeFile, network);
		this.coldHandler = new SortedColdEmissionPerLinkHandler(simulationEndTime, noOfTimeBins, shapeFile, network);
	}
	
	
	public EmissionLinkAnalyzer(double simulationEndTime, String emissionEventFile, int noOfTimeBins) {
		super(EmissionLinkAnalyzer.class.getSimpleName());
		this.emissionEventsFile = emissionEventFile;
		this.logger.info("Aggregating emissions for each "+simulationEndTime/noOfTimeBins+" sec time bin.");
		this.emissionUtils = new EmissionUtils();
		this.warmHandler = new SortedWarmEmissionPerLinkHandler(simulationEndTime, noOfTimeBins);
		this.coldHandler = new SortedColdEmissionPerLinkHandler(simulationEndTime, noOfTimeBins);
	}

	@Override
	public List<EventHandler> getEventHandler() {
		List<EventHandler> handler = new LinkedList<EventHandler>();
		return handler;
	}

	@Override
	public void preProcessData() {
		EventsManager eventsManager = EventsUtils.createEventsManager();
		EmissionEventsReader emissionReader = new EmissionEventsReader(eventsManager);

		eventsManager.addHandler(this.warmHandler);
		eventsManager.addHandler(this.coldHandler);
		eventsManager.addHandler(new SortedColdEmissionPerLinkHandler(30., 9));

		emissionReader.parse(this.emissionEventsFile);
	}

	@Override
	public void postProcessData() {
		this.link2WarmEmissions = this.warmHandler.getWarmEmissionsPerLinkAndTimeInterval();
		this.link2ColdEmissions = this.coldHandler.getColdEmissionsPerLinkAndTimeInterval();
		this.link2TotalEmissions = sumUpEmissionsPerTimeInterval(this.link2WarmEmissions, this.link2ColdEmissions);
	}

	@Override
	public void writeResults(String outputFolder) {

	}

	private SortedMap<Double, Map<Id<Link>, SortedMap<String, Double>>> sumUpEmissionsPerTimeInterval(
			Map<Double, Map<Id<Link>, Map<WarmPollutant, Double>>> time2warmEmissionsTotal,
			Map<Double, Map<Id<Link>, Map<ColdPollutant, Double>>> time2coldEmissionsTotal) {

		SortedMap<Double, Map<Id<Link>, SortedMap<String, Double>>> time2totalEmissions = new TreeMap<>();

		for(double endOfTimeInterval: time2warmEmissionsTotal.keySet()){
			Map<Id<Link>, Map<WarmPollutant, Double>> warmEmissions = time2warmEmissionsTotal.get(endOfTimeInterval);

			Map<Id<Link>, SortedMap<String, Double>> totalEmissions = new HashMap<>();
			if(time2coldEmissionsTotal.get(endOfTimeInterval) == null){
				for(Id<Link> id : warmEmissions.keySet()){
					SortedMap<String, Double> warmEmissionsOfLink = this.emissionUtils.convertWarmPollutantMap2String(warmEmissions.get(id));
					totalEmissions.put(id, warmEmissionsOfLink);
				}
			} else {
				Map<Id<Link>, Map<ColdPollutant, Double>> coldEmissions = time2coldEmissionsTotal.get(endOfTimeInterval);
				totalEmissions = this.emissionUtils.sumUpEmissionsPerId(warmEmissions, coldEmissions);
			}
			time2totalEmissions.put(endOfTimeInterval, totalEmissions);
		}
		return time2totalEmissions;
	}

	public SortedMap<Double, Map<Id<Link>, SortedMap<String, Double>>> getLink2TotalEmissions() {
		return this.link2TotalEmissions;
	}

	public Map<Double, Map<Id<Link>, Map<WarmPollutant, Double>>> getLink2WarmEmissions() {
		return link2WarmEmissions;
	}

	public Map<Double, Map<Id<Link>, Map<ColdPollutant, Double>>> getLink2ColdEmissions() {
		return link2ColdEmissions;
	}
}
