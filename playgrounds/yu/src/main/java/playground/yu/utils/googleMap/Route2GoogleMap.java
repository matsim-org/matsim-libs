/* *********************************************************************** *
 * project: org.matsim.*
 * Route2GoogleMap.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.yu.utils.googleMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.routes.GenericRoute;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.ConfigUtils;

/**
 * shows routes in google maps with google map static API supporting latitude &
 * longitude (xx.xxxxxx) or WGS84 formats
 * 
 * @author yu
 * 
 */
public class Route2GoogleMap extends X2GoogleMap {

	private Route route;
	private Network network;
	private String pathColor = this.DEFAULT_PATH_COLOR;
	private int weight = this.DEFAULT_WEIGHT;
	protected static String NETWORK_ROUTE_TRANSPARENCY = "aa",
			GENERIC_ROUTE_TRANSPARENCY = "44";

	/**
	 * @param fromSystem
	 * @param network
	 * @param route
	 * @param pathColor
	 *            hex-color
	 */
	public Route2GoogleMap(String fromSystem, Network network, Route route) {
		super(fromSystem);
		this.network = network;
		this.route = route;
	}

	public void setPathColor(String pathColor) {
		this.pathColor = pathColor;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}

	public String getRoutePath4googleMap() {
		Map<Id, ? extends Link> links = this.network.getLinks();

		Id startLinkId = route.getStartLinkId();
		Link startLink = links.get(startLinkId);
		String startMarkers = createLinkCenterMarker(startLink, "O");

		Id endLinkId = route.getEndLinkId();
		Link endLink = links.get(endLinkId);
		String endMarkers = createLinkCenterMarker(endLink, "D");

		List<Coord> coords = new ArrayList<Coord>();
		coords.add(startLink.getFromNode().getCoord());
		coords.add(startLink.getToNode().getCoord());

		String transparency = "";

		if (route instanceof NetworkRoute) {
			List<Id> linkIds = ((NetworkRoute) route).getLinkIds();
			for (Id linkId : linkIds) {
				coords.add(links.get(linkId).getToNode().getCoord());
			}
			transparency = NETWORK_ROUTE_TRANSPARENCY;
		} else if (route instanceof GenericRoute) {
			transparency = GENERIC_ROUTE_TRANSPARENCY;
		}

		coords.add(endLink.getToNode().getCoord());

		String path = this.createPath(coords, this.pathColor + transparency,
				this.weight);

		StringBuffer strBuf = new StringBuffer(URL_HEADER);
		strBuf.append(SIZE);
		strBuf.append(DEFAULT_SIZE);
		strBuf.append(startMarkers);
		strBuf.append(endMarkers);
		strBuf.append(path);
		strBuf.append(SENSOR);
		strBuf.append(DEFAULT_SENSOR);

		return strBuf.toString();
	}

	// -------------CREATE FEATURES IN GOOGLE MAPS--------------

	protected String createLinkCenterMarker(Link link, String label) {
		return this.createLinkCenterMarker(link, label, DEFAULT_LABEL_COLOR);
	}

	protected String createLinkCenterMarker(Link link, String label,
			String color) {
		return this.createMarker(link.getCoord(), label, color);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final String netFilename = "../../matsim/examples/equil/network.xml";
		final String plansFilename = "../../matsim/examples/equil/plans100.xml";
		final String outputPlansFilenameBase = "../";

		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils
				.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(netFilename);
		new MatsimPopulationReader(scenario).readFile(plansFilename);

		Population population = scenario.getPopulation();
		Network network = scenario.getNetwork();

		for (Person person : population.getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				List<PlanElement> pes = plan.getPlanElements();
				for (PlanElement pe : pes) {
					if (pe instanceof Leg) {
						System.out
								.println(new Route2GoogleMap(
										TransformationFactory.ATLANTIS,
										network, ((Leg) pe).getRoute())
										.getRoutePath4googleMap());
					}
				}
			}
		}
	}
}
