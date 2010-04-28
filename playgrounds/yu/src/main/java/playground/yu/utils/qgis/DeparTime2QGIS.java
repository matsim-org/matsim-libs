/* *********************************************************************** *
 * project: org.matsim.*
 * DeparTime2QGIS.java
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
package playground.yu.utils.qgis;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PlanImpl;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * @author yu
 *
 */
public class DeparTime2QGIS implements X2QGIS {
	public static class LinkDeparTime extends AbstractPersonAlgorithm implements
			PlanAlgorithm {
		/**
		 * @param arg0
		 *            a linkId
		 * @param arg1
		 *            sum of departuretimes
		 */
		private Map<Id, Double> dpTimes;
		/**
		 * @param arg0
		 *            a linkId
		 * @param arg1
		 *            counter of departure
		 */
		private Map<Id, Integer> dpCnt;

		public LinkDeparTime() {
			dpCnt = new HashMap<Id, Integer>();
			dpTimes = new HashMap<Id, Double>();
		}

		@Override
		public void run(Person person) {
			run(person.getSelectedPlan());
		}

		public void run(Plan p) {
			PlanImpl plan = (PlanImpl) p;
			Activity fa = plan.getFirstActivity();
			if (fa.getType().startsWith("h")) {
				Id linkId = fa.getLinkId();
				Integer c = dpCnt.get(linkId);
				if (c == null)
					c = Integer.valueOf(0);
				dpCnt.put(linkId, Integer.valueOf(c.intValue() + 1));
				Double times = dpTimes.get(linkId);
				if (times == null)
					times = Double.valueOf(0.0);
				dpTimes.put(linkId, Double.valueOf(times.doubleValue()
						+ fa.getEndTime()));
			}
		}

		public Map<Id, Double> getAvgDeparTime() {
			Map<Id, Double> avgDpTimes = new TreeMap<Id, Double>();
			for (Id linkId : dpCnt.keySet()) {
				avgDpTimes.put(linkId, Double.valueOf(dpTimes.get(linkId)
						.doubleValue()
						/ dpCnt.get(linkId).doubleValue()));
			}
			return avgDpTimes;
		}
	}

	/**
	 * @param args0
	 *            netfilename
	 * @param args1
	 *            1st plansfilename
	 * @param args2
	 *            2nd plansfilename
	 * @param args3
	 *            3nd Shape-filename (.shp-file)
	 */
	public static void main(String[] args) {
		MATSimNet2QGIS mn2q = new MATSimNet2QGIS(args[0], ch1903);
		/*
		 * ///////////////////////////////////////////////////////////////
		 * DepartureTime and MATSim-network to Shp-file // *
		 * ///////////////////////////////////////////////////////////////
		 */
		// mn2q.readNetwork("../data/ivtch/input/ivtch-osm-wu.xml"); // //
		// mn2q.setCrs(ch1903);
		// LinkDeparTime ldt = new LinkDeparTime();
		// mn2q.readPlans("/net/ils/run466/output/ITERS/it.500/500.plans.xml.gz",
		// ldt);
		// mn2q.addParameter("DeparTime", Double.class, ldt.getAvgDeparTime());
		// mn2q
		// .writeShapeFile("/net/ils/run466/output/ITERS/it.500/466.500.deparTime.shp");
		LinkDeparTime lprA = new LinkDeparTime();
		mn2q.readPlans(args[1], lprA);
		LinkDeparTime lprB = new LinkDeparTime();
		mn2q.readPlans(args[2], lprB);
		Map<Id, Double> diff = new TreeMap<Id, Double>();
		for (Id linkId : lprB.getAvgDeparTime().keySet()) {
			Double B = lprB.getAvgDeparTime().get(linkId);
			double b = (B != null) ? B.doubleValue() : 0.0;
			Double A = lprA.getAvgDeparTime().get(linkId);
			double a = (A != null) ? A.doubleValue() : 0.0;
			diff.put(linkId, Double.valueOf(b - a));
		}
		mn2q.addParameter("deparTime", Double.class, diff);
		mn2q.writeShapeFile(args[3]);
	}

}
