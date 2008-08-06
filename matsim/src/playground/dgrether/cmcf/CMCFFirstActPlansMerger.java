/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
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
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;

import playground.dgrether.DgPaths;
import playground.dgrether.utils.MatsimIo;


/**
 * @author dgrether
 *
 */
public class CMCFFirstActPlansMerger {

	
	private static final Logger log = Logger.getLogger(CMCFFirstActPlansMerger.class);
	
  private static final String cmcfPlansFile = DgPaths.VSPSVNBASE + "studies/schweiz-ivtch/cmcf/plans/plans_miv_zrh30km_10pct_simplified_acts_types_reduced_cmcf.xml";
	
  private static final String plansFile = DgPaths.VSPSVNBASE + "studies/schweiz-ivtch/baseCase/plans/plans_miv_zrh30km_10pct.xml.gz";
  
  private static final String outPlansFile = DgPaths.VSPSVNBASE + "studies/schweiz-ivtch/cmcf/plans/plans_miv_zrh30km_10pct_cmcf_first_act.xml.gz";
  
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		NetworkLayer net = MatsimIo.loadNetwork(DgPaths.IVTCHNET);
		Plans plansCmcf = MatsimIo.loadPlans(cmcfPlansFile);
		Plans plans = MatsimIo.loadPlans(plansFile);
		for (Person p : plans.getPersons().values()) {
			Plan pl = p.getSelectedPlan();
			Leg l = pl.getNextLeg(pl.getFirstActivity());
			Plan plcmcf = plansCmcf.getPerson(p.getId()).getSelectedPlan();
			Leg lcmcf = plcmcf.getNextLeg(plcmcf.getFirstActivity());
			l.setRoute(lcmcf.getRoute());
		}
		MatsimIo.writePlans(plans, outPlansFile);
		
		log.info("done");
	}

}
