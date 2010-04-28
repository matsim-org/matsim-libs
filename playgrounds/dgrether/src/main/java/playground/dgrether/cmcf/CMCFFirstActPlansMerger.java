/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.dgrether.cmcf;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.PlanImpl;

import playground.dgrether.DgPaths;
import playground.dgrether.utils.MatsimIo;


/**
 * @author dgrether
 *
 */
public class CMCFFirstActPlansMerger {


	private static final Logger log = Logger.getLogger(CMCFFirstActPlansMerger.class);

  private static final String cmcfPlansFile = DgPaths.SCMWORKSPACE + "studies/schweiz-ivtch/cmcf/plans/plans_miv_zrh30km_10pct_simplified_acts_types_reduced_cmcf.xml";

  private static final String plansFile = DgPaths.SCMWORKSPACE + "studies/schweiz-ivtch/baseCase/plans/plans_miv_zrh30km_10pct.xml.gz";

  private static final String outPlansFile = DgPaths.SCMWORKSPACE + "studies/schweiz-ivtch/cmcf/plans/plans_miv_zrh30km_10pct_cmcf_first_act.xml.gz";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ScenarioImpl scenario = new ScenarioImpl();
		NetworkLayer net = scenario.getNetwork();
		MatsimIo.loadNetwork(DgPaths.IVTCHNET, scenario);
		Population plansCmcf = MatsimIo.loadPlans(cmcfPlansFile, net);
		Population plans = MatsimIo.loadPlans(plansFile, net);
		for (Person p : plans.getPersons().values()) {
			Plan pl = p.getSelectedPlan();
			Leg l = ((PlanImpl) pl).getNextLeg(((PlanImpl) pl).getFirstActivity());
			Plan plcmcf = plansCmcf.getPersons().get(p.getId()).getSelectedPlan();
			Leg lcmcf = ((PlanImpl) plcmcf).getNextLeg(((PlanImpl) plcmcf).getFirstActivity());
			l.setRoute(lcmcf.getRoute());
		}
		MatsimIo.writePlans(plans, net, outPlansFile);

		log.info("done");
	}

}
