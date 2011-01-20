/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeModalSplitTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.yu.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

import playground.yu.utils.io.SimpleWriter;

/**
 * summarize information of routes in different plans in choice set of AN agent
 * 
 * @author ychen
 */

public class DiverseRoutesSummary extends AbstractPersonAlgorithm {
	public static class LegRoute {
		// private Id personId;
		private int planIndex;
		private List<Id> routeLinkIds;

		public LegRoute(
		// Id personId,
				int planIndex, List<Id> routeLinkIds) {
			// this.personId = personId;
			this.planIndex = planIndex;
			// this.legIndex = legIndex;
			this.routeLinkIds = routeLinkIds;
		}

		// public Id getPersonId() {
		// return personId;
		// }

		public int getPlanIndex() {
			return planIndex;
		}

		// public int getLegIndex() {
		// return legIndex;
		// }

		public List<Id> getRouteLinkIds() {
			return routeLinkIds;
		}

		public String toString() {
			// StringBuffer sb = new StringBuffer(personId.toString());
			// sb.append("\t");
			StringBuffer sb = new StringBuffer();
			sb.append(planIndex);
			// sb.append("\t");
			// sb.append(legIndex);
			sb.append("\t[\t");
			for (Id linkId : routeLinkIds) {
				sb.append(linkId.toString());
				sb.append("\t");
			}
			sb.append("]");
			return sb.toString();
		}
	}

	private SimpleWriter writer;
	private double sample;
	/**
	 * @param routes
	 *            Map<List<Id routelinkId>,Integer routeFlows (car, total day)>
	 */
	private Map<String, List<LegRoute>> legRoutes = new HashMap<String, List<LegRoute>>();

	public Map<String, List<LegRoute>> getDailyRouteList() {
		return legRoutes;
	}

	public DiverseRoutesSummary(final double outputSample, final String filename) {
		writer = new SimpleWriter(filename);
		writer.writeln("sample:\t" + sample * 100d + "%");
		writer.writeln("person ID\tPlan index\tLeg Index\trouten");
		writer.flush();
		sample = outputSample;
	}

	public void write() {
		for (Entry<String, List<LegRoute>> personDailyRoutes : legRoutes
				.entrySet()) {
			String personLegId = personDailyRoutes.getKey();
			for (LegRoute dailyRoute : personDailyRoutes.getValue()) {
				writer.writeln(personLegId + "\t" + dailyRoute.toString());
			}
			writer.writeln("--------------------");
			writer.flush();
		}
		writer.close();
	}

	@Override
	public void run(final Person person) {
		Id personId = person.getId();
		if (MatsimRandom.getRandom().nextDouble() < sample) {
			int planIndex = 0;
			for (Plan plan : person.getPlans()) {
				if (plan != null) {
					int legIndex = 0;
					for (PlanElement pe : plan.getPlanElements()) {

						if (pe instanceof Leg) {

							Leg l = (Leg) pe;

							if (l.getMode().equals("car")) {

								// Id previousActLinkId = ((PlanImpl) plan)
								// .getPreviousActivity(l).getLinkId();
								// Id nextActLinkId = ((PlanImpl) plan)
								// .getNextActivity(l).getLinkId();

								List<Id> routeLinkList = new LinkedList<Id>();
								NetworkRoute r = (NetworkRoute) l.getRoute();

								if (!r.getStartLinkId()
										.equals(r.getEndLinkId())) {
									routeLinkList = new LinkedList<Id>();
									((LinkedList<Id>) routeLinkList).addFirst(r
											.getStartLinkId()
									// previousActLinkId
											);
									List<Id> origRouteLinkIds = r.getLinkIds();
									for (int i = 0; i < origRouteLinkIds.size(); i++) {
										routeLinkList.add(origRouteLinkIds
												.get(i));
									}
									((LinkedList<Id>) routeLinkList).addLast(r
											.getEndLinkId());
									// routeLinkIds = routeLinkList;
								} else if (r.getStartLinkId().equals(
										r.getEndLinkId())
								// && previousActLinkId
								// .equals(nextActLinkId)
								) {
									routeLinkList.add(r.getStartLinkId());
								}

								List<LegRoute> personDailyRoutes = legRoutes
										.get(personId);
								if (personDailyRoutes == null) {
									personDailyRoutes = new ArrayList<LegRoute>();
									legRoutes.put(personId + "_Leg" + legIndex,
											personDailyRoutes);
								}
								personDailyRoutes.add(new LegRoute(planIndex,
										routeLinkList));
							}

							legIndex++;
						}// if pe instance of Leg
					}

				}// if plan != null

				planIndex++;
			}// for

		}

	}

	public static void main(String[] args) {
		String networkFilename = "../matsimTests/ParamCalibration/network.xml"//
		, populationFilename = "../matsimTests/ParamCalibration/40.plans.xml.gz"//
		, outputFilename = "../matsimTests/dailyJourney_Route2QGIS/equilTest1of2.txt";

		Scenario scenario = new ScenarioImpl();
		new MatsimNetworkReader(scenario).readFile(networkFilename);
		new MatsimPopulationReader(scenario).readFile(populationFilename);

		DiverseRoutesSummary srs = new DiverseRoutesSummary(0.1, outputFilename);
		srs.run(scenario.getPopulation());
		srs.write();
	}

}
