/* *********************************************************************** *
 * project: org.matsim.*
 * PtRate2QGIS.java
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
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.yu.analysis.PlanModeJudger;

/**
 * @author ychen
 * 
 */
public class PtRate2QGIS implements X2QGIS {

	/**
	 * 
	 */
	public static class LinkPtRate extends AbstractPersonAlgorithm implements
			PlanAlgorithm {
		private Map<Id, Integer> ptUsers;
		private Map<Id, Integer> agents;

		public LinkPtRate() {
			ptUsers = new HashMap<Id, Integer>();
			agents = new HashMap<Id, Integer>();
		}

		@Override
		public void run(Person person) {
			run(person.getSelectedPlan());
		}

		public void run(Plan plan) {
			ActivityImpl fa = ((PlanImpl) plan).getFirstActivity();
			Id linkId = fa.getLinkId();
			if (fa.getType().startsWith("h")) {
				Integer a = agents.get(linkId);
				if (a == null)
					a = Integer.valueOf(0);
				agents.put(linkId, Integer.valueOf(a.intValue() + 1));
				if (PlanModeJudger.usePt(plan)) {
					Integer p = ptUsers.get(linkId);
					if (p == null)
						p = Integer.valueOf(0);
					ptUsers.put(linkId, Integer.valueOf(p.intValue() + 1));
				}
			} else {
				System.out.println(fa.toString());
			}
		}

		public Map<Id, Double> getPtRate() {
			Map<Id, Double> ptRates = new TreeMap<Id, Double>();
			for (Id linkId : ptUsers.keySet()) {
				double a = agents.get(linkId).intValue();
				double ptRate = ((double) ptUsers.get(linkId).intValue()) / a;
				ptRates.put(linkId, Double.valueOf(ptRate));
			}
			return ptRates;
		}

		/**
		 * @return the ptUsers
		 */
		public Map<Id, Integer> getPtUsers() {
			return ptUsers;
		}

		/**
		 * @return the agents
		 */
		public Map<Id, Integer> getAgents() {
			return agents;
		}
	}

	/**
	 * @param args0
	 *            path of the netfile
	 * @param args1
	 *            path of the 1st plansfile
	 * @param args2
	 *            path of the 2nd plansfile
	 * @param args3
	 *            path of the Shapefile (.shp) to output
	 */
	public static void main(String[] args) {
		MATSimNet2QGIS mn2q = new MATSimNet2QGIS();
		/*
		 * ///////////////////////////////////////////////////////////////
		 * pt-rate and MATSim-network to Shp-file //
		 * ///////////////////////////////////////////////////////////////
		 */
		// mn2q.readNetwork("../data/ivtch/input/ivtch-osm-wu.xml");
		// mn2q.setCrs(ch1903);
		// LinkPtRate lpr = new LinkPtRate();
		//mn2q.readPlans("/net/ils/run466/output/ITERS/it.500/500.plans.xml.gz",
		// lpr);
		// mn2q.addParameter("PtRate", Double.class, lpr.getPtRate());
		// mn2q.addParameter("PtUsers", Integer.class, lpr.getPtUsers());
		// mn2q.addParameter("Persons", Integer.class, lpr.getAgents());
		// mn2q
		// .writeShapeFile(
		// "/net/ils/run466/output/ITERS/it.500/466.500.ptRate.shp");
		/*
		 * //
		 * ////////////////////////////////////////////////////////////////////
		 * Differenz-Network for pt-rate to Shp-file //
		 * /////////////////////////////////////////////////////////////////////
		 */
		mn2q.readNetwork(args[0]);
		// mn2q.readNetwork("../schweiz-ivtch/network/ivtch-osm-wu.xml");
		mn2q.setCrs(ch1903);
		LinkPtRate lprA = new LinkPtRate();
		mn2q.readPlans(args[1], lprA);
		// mn2q.readPlans("../runs/run465/500.plans.xml.gz", lprA);
		LinkPtRate lprB = new LinkPtRate();
		mn2q.readPlans(args[2], lprB);
		// mn2q.readPlans("../runs/run466/500.plans.xml.gz", lprB);
		Map<Id, Double> diff = new TreeMap<Id, Double>();
		for (Id linkId : lprA.getAgents().keySet()) {
			Double B = lprB.getPtRate().get(linkId);
			double b = (B != null) ? B.doubleValue() : 0.0;
			Double A = lprA.getPtRate().get(linkId);
			double a = (A != null) ? A.doubleValue() : 0.0;
			diff.put(linkId, Double.valueOf(b - a));
		}
		mn2q.addParameter("PtRate", Double.class, diff);
		mn2q.writeShapeFile(args[3]);
		// mn2q.writeShapeFile("../runs/run466/466.500-465.500.ptRate.shp");
	}
}