/* *********************************************************************** *
 * project: org.matsim.*
 * Routes2GoogleMap.java
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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

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
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.routes.GenericRoute;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.ConfigUtils;

/**
 * @author yu
 * 
 */
public class Routes2GoogleMap extends X2GoogleMap {
	private Collection<Route> routes = new ArrayList<Route>();
	private Network network;
	private String fromSystem;

	public Routes2GoogleMap(String fromSystem, Network network,
			List<Route> routes) {
		super(fromSystem);
		this.fromSystem = fromSystem;
		this.network = network;
		this.routes = routes;
	}

	/**
	 * @return if routes in List have the same OD pair
	 */
	protected boolean haveSameODPair() {
		Iterator<Route> it = routes.iterator();
		Route route = it.next();
		Id startLinkId = route.getStartLinkId(), endLinkId = route
				.getEndLinkId();
		for (route = it.next(); it.hasNext();) {
			if (!route.getStartLinkId().equals(startLinkId)
					|| !route.getEndLinkId().equals(endLinkId)) {
				return false;
			}
		}
		return true;
	}

	public String getRoutesPath4googleMap() {
		if (this.haveSameODPair()) {
			Map<Id, ? extends Link> links = this.network.getLinks();

			Iterator<Route> it = routes.iterator();
			Route route = it.next();
			Id startLinkId = route.getStartLinkId(), endLinkId = route
					.getEndLinkId();

			Route2GoogleMap r2gm = new Route2GoogleMap(this.fromSystem,
					network, route);

			Link startLink = links.get(startLinkId);
			String startMarkers = r2gm.createLinkCenterMarker(startLink, "O");

			Link endLink = links.get(endLinkId);
			String endMarkers = r2gm.createLinkCenterMarker(endLink, "D");

			StringBuffer strBuf = new StringBuffer(URL_HEADER);
			strBuf.append(SIZE);
			strBuf.append(DEFAULT_SIZE);
			strBuf.append(startMarkers);
			strBuf.append(endMarkers);

			for (Iterator<Route> routeIt = routes.iterator(); routeIt.hasNext();) {
				Route tmpRoute = routeIt.next();

				Link tmpStartLink = links.get(tmpRoute.getStartLinkId());

				List<Coord> coords = new ArrayList<Coord>();
				coords.add(tmpStartLink.getFromNode().getCoord());
				coords.add(tmpStartLink.getToNode().getCoord());

				String transparency = "";

				if (tmpRoute instanceof NetworkRoute) {
					List<Id> linkIds = ((NetworkRoute) tmpRoute).getLinkIds();
					for (Id linkId : linkIds) {
						coords.add(links.get(linkId).getToNode().getCoord());
					}
					transparency = Route2GoogleMap.NETWORK_ROUTE_TRANSPARENCY;
				} else if (tmpRoute instanceof GenericRoute) {
					transparency = Route2GoogleMap.GENERIC_ROUTE_TRANSPARENCY;
				}

				Link tmpEndLink = links.get(tmpRoute.getEndLinkId());
				coords.add(tmpEndLink.getToNode().getCoord());

				Random random = MatsimRandom.getRandom();
				Integer.toHexString(random.nextInt(256));
				String path = createPath(coords,
						"0x" + Integer.toHexString(random.nextInt(256))
								+ Integer.toHexString(random.nextInt(256))
								+ Integer.toHexString(random.nextInt(256))
								+ transparency, DEFAULT_WEIGHT);
				strBuf.append(path);
			}

			strBuf.append(SENSOR);
			strBuf.append(DEFAULT_SENSOR);

			return strBuf.toString();
		}
		return "These routes have different OD pair";
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
			List<? extends Plan> plans = person.getPlans();
			int size = plans.get(0).getPlanElements().size();// rough
																// temporarily
			boolean sameSize = true;
			for (int i = 1; i < plans.size(); i++) {
				if (size != plans.get(i).getPlanElements().size()) {
					sameSize = false;
					break;
				}
			}

			if (sameSize) {
				Map<Integer, List<Route>> routeMap = new HashMap<Integer, List<Route>>();
				for (Plan plan : plans) {

					List<PlanElement> pes = plan.getPlanElements();
					for (PlanElement pe : pes) {
						int cnt = 0;
						if (pe instanceof Leg) {
							List<Route> routes = routeMap.get(cnt);
							if (routes == null) {
								routes = new ArrayList<Route>();
								routeMap.put(cnt, routes);
							}
							routes.add(((Leg) pe).getRoute());
							cnt++;
						}
					}
				}
				for (List<Route> routeList : routeMap.values()) {
					System.out.println(new Routes2GoogleMap(
							TransformationFactory.ATLANTIS, network, routeList)
							.getRoutesPath4googleMap());
				}
			}
		}
	}
}
