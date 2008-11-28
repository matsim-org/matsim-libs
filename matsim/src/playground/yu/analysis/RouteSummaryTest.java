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

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.BasicPlanImpl.LegIterator;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Leg;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.routes.CarRoute;
import org.matsim.utils.io.IOUtils;
import org.matsim.world.World;

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
		private Map<String, Set<List<Id>>> odRoutes = new HashMap<String, Set<List<Id>>>();
		/**
		 * @param routeCounters
		 *            Map<List<Id routelinkId>,Integer routeFlows (car, total
		 *            day)>
		 */
		private Map<List<Id>, Integer> routeCounters = new HashMap<List<Id>, Integer>();

		public Map<List<Id>, Integer> getRouteCounters() {
			return routeCounters;
		}

		/**
		 * @param numRoutesDistribution
		 *            Map<Integer routeFlows (car, total day), Integer number of
		 *            occurrences of "key">
		 */
		private Map<Integer, Integer> numRoutesDistribution = new HashMap<Integer, Integer>();

		public RouteSummary(final String filename) {
			try {
				writer = IOUtils.getBufferedWriter(filename);
				writer.write("odPair\trouteLinkIds\tnumber of routes\n");
				writer.flush();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void write() {
			try {
				for (String odPair : odRoutes.keySet()) {
					Set<List<Id>> routes = odRoutes.get(odPair);
					if (routes.size() > 0) {
						writer.write("odPair :\t" + odPair + "\n");
						for (List<Id> linkIds : routes) {
							Integer routeFlows = routeCounters.get(linkIds);
							Integer num_of_num_of_routes = numRoutesDistribution
									.get(routeFlows);
							numRoutesDistribution
									.put(
											routeFlows,
											(num_of_num_of_routes == null) ? new Integer(
													1)
													: new Integer(
															num_of_num_of_routes
																	.intValue() + 1));
							writer
									.write(linkIds.toString()
											+ "\tnum_of_routes :\t"
											+ routeFlows + "\n");
						}
						writer.write("-----------------------\n");
						writer.flush();
					}
				}
				writer.write("number_of_routes\tnumber_of_number_of_routes\n");
				for (Integer n_o_routes : numRoutesDistribution.keySet()) {
					writer.write(n_o_routes + "\t"
							+ numRoutesDistribution.get(n_o_routes) + "\n");
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void end() {
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run(final Person person) {
			Plan p = person.getSelectedPlan();
			if (p != null)
				if (PlanModeJudger.useCar(p))
					for (LegIterator li = p.getIteratorLeg(); li.hasNext();) {
						Leg l = (Leg) li.next();

						Id previousActLinkId = p.getPreviousActivity(l)
								.getLinkId();
						Id nextActLinkId = p.getNextActivity(l).getLinkId();

						String odPair = previousActLinkId.toString() + "->"
								+ nextActLinkId.toString();

						Set<List<Id>> aOdRouteSet = odRoutes.get(odPair);
						if (aOdRouteSet == null)
							aOdRouteSet = new HashSet<List<Id>>();

						List<Id> routeLinkIds = new LinkedList<Id>();
						CarRoute r = l.getRoute();
						// boolean illegalRoute = false;

						if (r.getNodes().size() > 0) {
							LinkedList<Id> tmpRouteLinkList = new LinkedList<Id>();
							tmpRouteLinkList.addFirst(previousActLinkId);
							List<Id> origRouteLinkIds = r.getLinkIds();
							for (int i = 0; i < origRouteLinkIds.size(); i++) {
								tmpRouteLinkList.add(origRouteLinkIds.get(i));
							}
							tmpRouteLinkList.addLast(nextActLinkId);
							routeLinkIds = tmpRouteLinkList;
						} else if (r.getNodes().size() == 0
								&& previousActLinkId.equals(nextActLinkId))
							routeLinkIds.add(previousActLinkId);
						// else
						// illegalRoute = true;

						if (!aOdRouteSet.contains(routeLinkIds))
							aOdRouteSet.add(routeLinkIds);
						// if (!illegalRoute) {
						Integer itg = routeCounters.get(routeLinkIds);
						routeCounters.put(routeLinkIds,
								(itg == null) ? new Integer(1) : new Integer(
										itg.intValue() + 1));
						// }
						odRoutes.put(odPair, aOdRouteSet);
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
		Gbl.createConfig(null);

		World world = Gbl.getWorld();

		NetworkLayer network = new NetworkLayer();
		System.out.println("-->reading networkfile: " + netFilename);
		new MatsimNetworkReader(network).readFile(netFilename);
		world.setNetworkLayer(network);

		Population population = new Population();

		RouteSummary rs = new RouteSummary(outFilename);
		population.addAlgorithm(rs);

		System.out.println("-->reading plansfile: " + plansFilename);
		new MatsimPopulationReader(population).readFile(plansFilename);

		population.runAlgorithms();
		rs.write();
		rs.end();

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}
}
