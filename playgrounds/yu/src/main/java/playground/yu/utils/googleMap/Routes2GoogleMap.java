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
import java.util.Map.Entry;
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

import playground.yu.utils.io.SimpleWriter;

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
		if (routes.size() == 1) {
			return true;
		}
		if (routes.size() == 0) {
			throw new RuntimeException(
					"There is NOT routes in Collection<Route>");
		}
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
				Link tmpEndLink = links.get(tmpRoute.getEndLinkId());

				List<Coord> coords = new ArrayList<Coord>();
				String transparency = "";

				if (tmpRoute instanceof NetworkRoute) {
					coords.add(tmpStartLink.getFromNode().getCoord());
					if (!tmpStartLink.equals(tmpEndLink)) {
						coords.add(tmpStartLink.getToNode().getCoord());
					}

					List<Id> linkIds = ((NetworkRoute) tmpRoute).getLinkIds();
					for (Id linkId : linkIds) {
						coords.add(links.get(linkId).getToNode().getCoord());
					}

					coords.add(tmpEndLink.getToNode().getCoord());

					transparency = Route2GoogleMap.NETWORK_ROUTE_TRANSPARENCY;
				} else if (tmpRoute instanceof GenericRoute) {
					coords.add(startLink.getCoord());
					coords.add(endLink.getCoord());

					transparency = Route2GoogleMap.GENERIC_ROUTE_TRANSPARENCY;
				}

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
		final String fromSystem, netFilename, plansFilename, outputPlansFilename;
		final double outputSample;
		if (args.length == 4) {
			fromSystem = args[0];
			netFilename = args[1];
			plansFilename = args[2];
			outputPlansFilename = args[3];
			outputSample = Double.parseDouble(args[4]);
		} else {
			fromSystem = TransformationFactory.ATLANTIS;
			netFilename = "../../matsim/examples/equil/network.xml";
			plansFilename = "../../matsim/examples/equil/plans100.xml";
			outputPlansFilename = "./tmp.log";
			outputSample = 1d;
		}

		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils
				.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(netFilename);
		new MatsimPopulationReader(scenario).readFile(plansFilename);

		SimpleWriter writer = new SimpleWriter(outputPlansFilename);
		writer.writeln("person ID\tleg index\tgoogle maps url");
		writer.flush();

		Population population = scenario.getPopulation();
		Network network = scenario.getNetwork();

		Random random = MatsimRandom.getRandom();
		for (Person person : population.getPersons().values()) {
			if (random.nextDouble() < outputSample) {
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
						int cnt = 0;/* caution: position of cnt */
						for (PlanElement pe : pes) {
							if (pe instanceof Leg) {
								List<Route> routes = routeMap.get(cnt);
								if (routes == null) {
									routes = new ArrayList<Route>();
									routeMap.put(cnt, routes);
								}

								Route route = ((Leg) pe).getRoute();
								if (route != null) {
									routes.add(route);
								} else {
									System.err.println("person Id :\t"
											+ person.getId() + "leg index :\t"
											+ cnt + "Route==null");
								}
								cnt++;
							}
						}
						for (Entry<Integer, List<Route>> routesEntry : routeMap
								.entrySet()) {
							writer.writeln(person.getId()
									+ "\t"
									+ routesEntry.getKey()
									+ "\t"
									+ new Routes2GoogleMap(fromSystem, network,
											routesEntry.getValue())
											.getRoutesPath4googleMap());
							writer.flush();
						}
					}
				}
			}
		}
		writer.close();
	}
}
