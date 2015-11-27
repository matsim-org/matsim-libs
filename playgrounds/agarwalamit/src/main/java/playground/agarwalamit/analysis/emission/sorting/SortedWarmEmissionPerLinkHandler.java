/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.agarwalamit.analysis.emission.sorting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import playground.agarwalamit.utils.GeometryUtils;
import playground.benjamin.scenarios.munich.analysis.nectar.EmissionsPerLinkWarmEventHandler;

/**
 * @author amit
 */

public class SortedWarmEmissionPerLinkHandler implements WarmEmissionEventHandler {
	private final EmissionsPerLinkWarmEventHandler delegate;
	private final Collection<SimpleFeature> features ;
	private Network network;
	private boolean isSorting;

	public SortedWarmEmissionPerLinkHandler (double simulationEndTime, int noOfTimeBins, String shapeFile, Network network){
		delegate = new EmissionsPerLinkWarmEventHandler(simulationEndTime,noOfTimeBins);
		features = new ShapeFileReader().readFileAndInitialize(shapeFile);
		this.network = network;
		isSorting = true;
	}
	
	public SortedWarmEmissionPerLinkHandler (double simulationEndTime, int noOfTimeBins){
		delegate = new EmissionsPerLinkWarmEventHandler(simulationEndTime,noOfTimeBins);
		features = new ArrayList<>();
		isSorting = false;
	}

	@Override
	public void handleEvent(WarmEmissionEvent event) {
		if(isSorting) {
			Link link = network.getLinks().get(event.getLinkId());
			if(GeometryUtils.isLinkInsideCity(features, link) ) delegate.handleEvent(event);
		} else {
			delegate.handleEvent(event);
		}
	}

	public Map<Double, Map<Id<Link>, Double>> getTime2linkIdLeaveCount() {
		return delegate.getTime2linkIdLeaveCount();
	}

	public Map<Double, Map<Id<Link>, Map<WarmPollutant, Double>>> getWarmEmissionsPerLinkAndTimeInterval() {
		return delegate.getWarmEmissionsPerLinkAndTimeInterval();
	}
	
	@Override
	public void reset(int iteration) {
		delegate.reset(iteration);
	}
}