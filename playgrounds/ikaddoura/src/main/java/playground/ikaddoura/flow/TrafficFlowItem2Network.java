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

package playground.ikaddoura.flow;

import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import com.vividsolutions.jts.geom.Coordinate;

/**
* @author ikaddoura
*/

public class TrafficFlowItem2Network {

	private final List<TrafficItem> trafficItems;
	private final Scenario scenario;
	
	private final CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.DHDN_GK4);
	
	public TrafficFlowItem2Network(List<TrafficItem> trafficItems, String networkFile) {
		this.trafficItems = trafficItems;
		this.scenario = loadScenario(networkFile);
	}

	private Scenario loadScenario(String networkFile) {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(networkFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		return scenario;
	}

	public void run() {
		for (TrafficItem item : trafficItems) {
			
			Coordinate coordinate = item.getCoordinates()[0];
			Coord coord = MGC.coordinate2Coord(coordinate);
			Coord transformedCoord = ct.transform(coord);
			
			Link nearestLinkFrom = NetworkUtils.getNearestLink(scenario.getNetwork(), transformedCoord);
			System.out.println(item.getId() + " Coordinate: " + coordinate + " / Coord: " + coord + " / transformed Coord: " + transformedCoord + " --> " + nearestLinkFrom.getId());
		}
	}

}

