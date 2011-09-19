/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.mrieser.pt;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.matsim.pt.router.TransitActsRemover;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterImpl;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vehicles.Vehicles;

public class RouterTester {

	private static final String NETWORK = "/Volumes/Data/vis/zrh/output_network.xml.gz";
	private static final String VEHICLES = "/Volumes/Data/vis/zrh/vehicles10pct.oevModellZH.xml";
	private static final String SCHEDULE = "/Volumes/Data/vis/zrh/transitSchedule.networkOevModellZH.xml";
	private static final String PLANS = "/Volumes/Data/vis/zrh/100.plans.xml.gz";

//	private static final String NETWORK = "/Volumes/Data/projects/speedupTransit/bvg1pct/network.cleaned.xml";
//	private static final String SCHEDULE = "/Volumes/Data/projects/speedupTransit/bvg1pct/transitSchedule.xml";
//	private static final String VEHICLES = "/Volumes/Data/projects/speedupTransit/bvg1pct/transitVehicles.xml";
//	private static final String PLANS = "/Volumes/Data/projects/speedupTransit/bvg1pct/plans.xml";

	private final static Logger log = Logger.getLogger(RouterTester.class);

	public static void main(String[] args) {
		ScenarioImpl s = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		s.getConfig().scenario().setUseTransit(true);
		s.getConfig().scenario().setUseVehicles(true);

		Vehicles v = s.getVehicles();
		TransitSchedule ts = s.getTransitSchedule();
		PopulationImpl p = (PopulationImpl) s.getPopulation();

		new MatsimNetworkReader(s).readFile(NETWORK);
		new VehicleReaderV1(v).readFile(VEHICLES);
		new TransitScheduleReader(s).readFile(SCHEDULE);
		log.info("build transit router...");
		TransitRouterConfig tRConfig = new TransitRouterConfig(s.getConfig().planCalcScore(), 
				s.getConfig().plansCalcRoute(), s.getConfig().transitRouter(),
				s.getConfig().vspExperimental());

		TransitRouterImpl router = new TransitRouterImpl(ts, tRConfig);

		PtRouter ptR = new PtRouter(router);

		p.setIsStreaming(true);
		p.addAlgorithm(ptR);

		log.info("start processing persons...");
		new MatsimPopulationReader(s).readFile(PLANS);

		ptR.close();
	}

	private static class PtRouter implements PersonAlgorithm {

		private final TransitActsRemover transitLegsRemover = new TransitActsRemover();
		private final TransitRouter router;
		private final BufferedWriter out;

		public PtRouter(final TransitRouter router) {
			this.router = router;
			this.out = IOUtils.getBufferedWriter("/Volumes/Data/vis/routesDijkstra1.txt");
		}

		@Override
		public void run(Person person) {
			try {
				Plan plan = person.getSelectedPlan();
				this.transitLegsRemover.run(plan);
//				for (Plan plan : person.getPlans()) {
					Activity prevAct = null;
					for (PlanElement pe : plan.getPlanElements()) {
						if (pe instanceof Activity) {
							Activity act = (Activity) pe;
							if (prevAct != null) {
								List<Leg> legs = router.calcRoute(prevAct.getCoord(), act.getCoord(), act.getStartTime(), person);
								out.write(person.getId() + " " + prevAct.getCoord() + " -> " + act.getCoord() + " @ " + Time.writeTime(act.getStartTime()) + " :\n");
								if (legs != null) {
									for (Leg l : legs) {
										out.write("  " + l.getMode());
										if (l.getRoute() instanceof ExperimentalTransitRoute) {
											ExperimentalTransitRoute r = (ExperimentalTransitRoute) l.getRoute();
											out.write(" " + r.getRouteDescription());
										}
										out.write("\n");
									}
								}
							}
							prevAct = act;
						}
					}
//				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		private void close() {
			try {
				this.out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
