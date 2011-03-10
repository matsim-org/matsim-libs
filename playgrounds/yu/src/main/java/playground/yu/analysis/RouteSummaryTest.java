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

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

/**
 * @author ychen
 */
public class RouteSummaryTest {
	public static class RouteSummary extends AbstractPersonAlgorithm {
		private BufferedWriter writer;
		/**
		 * @param odRoutes
		 *            Map<String odPair, Set<List<Id linkId>>>
		 */
		private final Map<String, Set<List<Id>>> odRoutes = new HashMap<String, Set<List<Id>>>();
		/**
		 * @param routes
		 *            Map<List<Id routelinkId>,Integer routeFlows (car, total
		 *            day)>
		 */
		private final Map<List<Id>, Integer> routeCounters = new HashMap<List<Id>, Integer>();

		public Map<List<Id>, Integer> getRouteCounters() {
			return this.routeCounters;
		}

		/**
		 * @param numRoutesDistribution
		 *            Map<Integer routeFlows (car, total day), Integer number of
		 *            occurrences of "key">
		 */
		private final Map<Integer, Integer> numRoutesDistribution = new HashMap<Integer, Integer>();

		public RouteSummary(final String filename) {
			try {
				this.writer = IOUtils.getBufferedWriter(filename);
				this.writer.write("odPair\trouteLinkIds\tnumber of routes\n");
				this.writer.flush();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void write() {
			try {
				for (String odPair : this.odRoutes.keySet()) {
					Set<List<Id>> routes = this.odRoutes.get(odPair);
					if (routes.size() > 0) {
						this.writer.write("odPair :\t" + odPair + "\n");
						for (List<Id> linkIds : routes) {
							Integer routeFlows = this.routeCounters
							.get(linkIds);
							Integer num_of_num_of_routes = this.numRoutesDistribution
							.get(routeFlows);
							this.numRoutesDistribution.put(routeFlows,
									(num_of_num_of_routes == null) ? 1
											: num_of_num_of_routes + 1);
							this.writer
							.write(linkIds.toString()
									+ "\tnum_of_routes :\t"
									+ routeFlows + "\n");
						}
						this.writer.write("-----------------------\n");
						this.writer.flush();
					}
				}
				this.writer
				.write("number_of_routes\tnumber_of_number_of_routes\n");
				for (Integer n_o_routes : this.numRoutesDistribution.keySet()) {
					this.writer
					.write(n_o_routes
							+ "\t"
							+ this.numRoutesDistribution
							.get(n_o_routes) + "\n");
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void end() {
			try {
				this.writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run(final Person person) {
			Plan p = person.getSelectedPlan();
			if (p != null)
				if (PlanModeJudger.useCar(p)) {
					for (PlanElement pe : p.getPlanElements()) {
						if (pe instanceof Leg) {

							Leg l = (Leg) pe;

							Id previousActLinkId = ((PlanImpl) p).getPreviousActivity(l).getLinkId();
							Id nextActLinkId = ((PlanImpl) p).getNextActivity(l).getLinkId();

							String odPair = previousActLinkId.toString() + "->" + nextActLinkId.toString();

							Set<List<Id>> aOdRouteSet = this.odRoutes.get(odPair);
							if (aOdRouteSet == null)
								aOdRouteSet = new HashSet<List<Id>>();

							List<Id> routeLinkIds = new LinkedList<Id>();
							NetworkRoute r = (NetworkRoute) l.getRoute();
							// boolean illegalRoute = false;

							if (!r.getStartLinkId().equals(r.getEndLinkId())) {
								LinkedList<Id> tmpRouteLinkList = new LinkedList<Id>();
								tmpRouteLinkList.addFirst(previousActLinkId);
								List<Id> origRouteLinkIds = r.getLinkIds();
								for (int i = 0; i < origRouteLinkIds.size(); i++) {
									tmpRouteLinkList.add(origRouteLinkIds.get(i));
								}
								tmpRouteLinkList.addLast(nextActLinkId);
								routeLinkIds = tmpRouteLinkList;
							} else if ((r.getStartLinkId().equals(r.getEndLinkId()))
									&& previousActLinkId.equals(nextActLinkId)) {
								routeLinkIds.add(previousActLinkId);
							}
							// else
							// illegalRoute = true;

							if (!aOdRouteSet.contains(routeLinkIds))
								aOdRouteSet.add(routeLinkIds);
							// if (!illegalRoute) {
							Integer itg = this.routeCounters.get(routeLinkIds);
							this.routeCounters.put(routeLinkIds,
									(itg == null) ? 1 : itg + 1);
							// }
							this.odRoutes.put(odPair, aOdRouteSet);
						}
					}
				}
		}
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		final String netFilename = args[0];
		// final String netFilename =
		// "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		// final String netFilename = "./test/yu/equil_test/equil_net.xml";
		final String plansFilename = args[1];
		// final String plansFilename =
		// "../data/ivtch/legCount/263.100.plans.xml.gz";
		// final String plansFilename =
		// "./test/yu/equil_test/output/100.plans.xml.gz";
		final String outFilename = args[2];
		// final String outFilename =
		// "../data/ivtch/legCount/263.legsCount.txt.gz";

		Gbl.startMeasurement();

		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		System.out.println("-->reading networkfile: " + netFilename);
		new MatsimNetworkReader(scenario).readFile(netFilename);

		Population population = scenario.getPopulation();
		System.out.println("-->reading plansfile: " + plansFilename);
		new MatsimPopulationReader(scenario).readFile(plansFilename);

		RouteSummary rs = new RouteSummary(outFilename);
		rs.run(population);
		rs.write();
		rs.end();

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}
}
