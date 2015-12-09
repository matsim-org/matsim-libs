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
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.types.ColdPollutant;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import playground.agarwalamit.utils.GeometryUtils;
import playground.benjamin.scenarios.munich.analysis.nectar.EmissionsPerLinkColdEventHandler;

/**
 * @author amit
 */

public class SortedColdEmissionPerLinkHandler implements ColdEmissionEventHandler{
	private EmissionsPerLinkColdEventHandler delegate;
	private Collection<SimpleFeature> features ;
	private Network network;
	private boolean isSorting;

	public SortedColdEmissionPerLinkHandler(double simulationEndTime, int noOfTimeBins) {
		delegate = new EmissionsPerLinkColdEventHandler(simulationEndTime, noOfTimeBins);
		features = new ArrayList<>();
		isSorting = false;
	}

	public SortedColdEmissionPerLinkHandler(double simulationEndTime, int noOfTimeBins,String shapeFile, Network network) {
		delegate = new EmissionsPerLinkColdEventHandler(simulationEndTime, noOfTimeBins);
		features = new ShapeFileReader().readFileAndInitialize(shapeFile);
		this.network = network;
		isSorting = true;
	}	

	@Override
	public void handleEvent(ColdEmissionEvent event) {
		if(isSorting) {
			Link link = network.getLinks().get(event.getLinkId());
			if(GeometryUtils.isLinkInsideCity(features, link) ) delegate.handleEvent(event);
		} else {
			delegate.handleEvent(event);
		}
	}

	public Map<Double, Map<Id<Link>, Map<ColdPollutant, Double>>> getColdEmissionsPerLinkAndTimeInterval() {
		return delegate.getColdEmissionsPerLinkAndTimeInterval();
	}

	@Override
	public void reset(int iteration) {
		delegate.reset(iteration);
	}
}