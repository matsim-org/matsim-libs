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
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.types.ColdPollutant;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import playground.agarwalamit.utils.GeometryUtils;
import playground.vsp.analysis.modules.emissionsAnalyzer.EmissionsPerPersonColdEventHandler;

/**
 * @author amit
 */

public class SortedColdEmissionHandler extends EmissionsPerPersonColdEventHandler{
	private final EmissionsPerPersonColdEventHandler delegate;
	private final Collection<SimpleFeature> features ;
	private Network network;
	private boolean isSorting;

	public SortedColdEmissionHandler() {
		delegate = new EmissionsPerPersonColdEventHandler();
		features = new ArrayList<>();
		isSorting = false;
	}

	public SortedColdEmissionHandler(String shapeFile, Network network) {
		delegate = new EmissionsPerPersonColdEventHandler();
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

	@Override
	public Map<Id<Person>, Map<ColdPollutant, Double>> getColdEmissionsPerPerson() {
		return delegate.getColdEmissionsPerPerson();
	}

	@Override
	public void reset(int iteration) {
		delegate.reset(iteration);
	}


}