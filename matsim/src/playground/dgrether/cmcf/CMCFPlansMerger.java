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
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;

import playground.dgrether.DgPaths;
import playground.dgrether.utils.MatsimIo;


/**
 * @author dgrether
 *
 */
public class CMCFPlansMerger {

	private static final Logger log = Logger.getLogger(CMCFPlansMerger.class);
	
	private static final String plansFile = DgPaths.VSPSVNBASE + "studies/schweiz-ivtch/baseCase/plans/plans_miv_zrh30km_10pct.xml.gz";
	
//	private static final String cmcfPlansFile = DgPaths.VSPSVNBASE + "studies/schweiz-ivtch/cmcf/plans/plans_miv_zrh30km_10pct_one_act_cmcf.xml";

//	private static final String outPlansFile = DgPaths.VSPSVNBASE + "studies/schweiz-ivtch/cmcf/plans/plans_miv_zrh30km_10pct_all_acts_cmcf.xml.gz";
	
  //new paths nov 08
	
	private static final String cmcfPlansFile = DgPaths.VSPSVNBASE + "studies/schweiz-ivtch/cmcf/plans/plans_miv_zrh30km_10pct_one_act_newcmcf.xml.gz";
	
	private static final String outPlansFile = DgPaths.VSPSVNBASE + "studies/schweiz-ivtch/cmcf/plans/plans_miv_zrh30km_10pct_all_acts_newcmcf.xml.gz";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		NetworkLayer net = MatsimIo.loadNetwork(DgPaths.IVTCHNET);
		Population plansCmcf = MatsimIo.loadPlans(cmcfPlansFile);
		Population plans = MatsimIo.loadPlans(plansFile);
		
		for (Person person : plansCmcf.getPersons().values()) {
			String idstring = person.getId().toString();
			String[] idLegNumber = idstring.split("leg");
			Id id = new IdImpl(idLegNumber[0]);
			Person p = plans.getPerson(id);
			Plan plan = p.getSelectedPlan();
			int legNumber = Integer.parseInt(idLegNumber[1]);
			legNumber = (legNumber * 2) + 1;
			Leg leg = (Leg) plan.getActsLegs().get(legNumber);
			leg.setRoute(person.getSelectedPlan().getNextLeg(person.getSelectedPlan().getFirstActivity()).getRoute());
		}
		
		MatsimIo.writePlans(plans, outPlansFile);
		log.info("done");
		
	}

}
