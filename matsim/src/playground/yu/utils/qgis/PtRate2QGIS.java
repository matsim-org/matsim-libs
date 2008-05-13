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

import org.matsim.basic.v01.Id;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.algorithms.PersonAlgorithm;
import org.matsim.plans.algorithms.PlanAlgorithmI;

/**
 * @author yu
 * 
 */
public class PtRate2QGIS implements X2QGIS {

	/**
	 * 
	 */
	public static class LinkPtRate extends PersonAlgorithm implements
			PlanAlgorithmI {
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
			Id linkId = plan.getFirstActivity().getLinkId();
			Integer a = agents.get(linkId);
			if (a == null)
				a = new Integer(0);// TODO to inspect
			agents.put(linkId, new Integer(a.intValue() + 1));
			if (plan.getType().equals(Plan.Type.PT)) {
				Integer p = ptUsers.get(linkId);
				if (p == null)
					p = new Integer(0);// TODO to inspect
				ptUsers.put(linkId, new Integer(p.intValue() + 1));
			}
		}

		public Map<Id, Double> getPtRate() {
			Map<Id, Double> ptRates = new TreeMap<Id, Double>();
			for (Id linkId : ptUsers.keySet()) {
				double a = ((double) agents.get(linkId).intValue());
				double ptRate = ((double) ptUsers.get(linkId).intValue()) / a;
				ptRates
						.put(linkId,
								new Double((ptRate == 0.0) ? -1.0 : ptRate));
			}
			return ptRates;
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		MATSimNet2QGIS mn2q = new MATSimNet2QGIS();
		/*
		 * ///////////////////////////////////////////////////////////////
		 * pt-rate and MATSim-network to Shp-file // *
		 * ///////////////////////////////////////////////////////////////
		 */
		mn2q.readNetwork("../data/ivtch/input/ivtch-osm-wu.xml"); // //
		mn2q.setCrs(ch1903);
		LinkPtRate lpr = new LinkPtRate();
		mn2q.readPlans("/net/ils/run466/output/ITERS/it.400/400.plans.xml.gz",
				lpr);
		mn2q.addParameter("PtRate on link", Double.class, lpr.getPtRate());
		mn2q
				.writeShapeFile("/net/ils/run466/output/ITERS/it.400/466.400.ptRate.shp");
	}

}
