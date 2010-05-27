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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.matsim.pt.router.TransitActsRemover;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitScheduleReader;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vehicles.Vehicles;
import org.xml.sax.SAXException;

public class RouterTester {
	public static void main(String[] args) {
		ScenarioImpl s = new ScenarioImpl();
		s.getConfig().scenario().setUseTransit(true);
		s.getConfig().scenario().setUseVehicles(true);

		Vehicles v = s.getVehicles();
		TransitSchedule ts = s.getTransitSchedule();
		PopulationImpl p = (PopulationImpl) s.getPopulation();

		new MatsimNetworkReader(s).readFile("/data/vis/zrh/output_network.xml.gz");
		new VehicleReaderV1(v).readFile("/data/vis/zrh/vehicles10pct.oevModellZH.xml");
		try {
			new TransitScheduleReader(s).readFile("/data/vis/zrh/transitSchedule.networkOevModellZH.xml");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}

		TransitRouter router = new TransitRouter(ts);

		PtRouter ptR = new PtRouter(router);

		p.setIsStreaming(true);
		p.addAlgorithm(ptR);

//		new MatsimPopulationReader(s).readFile("/data/vis/zrh/100.plans.xml.gz");
		new MatsimPopulationReader(s).readFile("/data/vis/zrh/plan-sample.xml");

		ptR.close();
	}

	private static class PtRouter implements PersonAlgorithm {

		private final TransitActsRemover transitLegsRemover = new TransitActsRemover();
		private final TransitRouter router;
		private final BufferedWriter out;

		public PtRouter(final TransitRouter router) {
			this.router = router;
			try {
				this.out = IOUtils.getBufferedWriter("/data/vis/routesLandmarks.txt");
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
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
								List<Leg> legs = router.calcRoute(prevAct.getCoord(), act.getCoord(), act.getStartTime());
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
