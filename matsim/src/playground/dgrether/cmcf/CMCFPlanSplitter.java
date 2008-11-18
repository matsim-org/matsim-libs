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
import org.matsim.basic.v01.IdImpl;
import org.matsim.basic.v01.BasicPlanImpl.LegIterator;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.PersonImpl;
import org.matsim.population.Plan;
import org.matsim.population.Population;

import playground.dgrether.DgPaths;
import playground.dgrether.utils.MatsimIo;


/**
 * @author dgrether
 *
 */
public class CMCFPlanSplitter {
	private static final Logger log = Logger.getLogger(CMCFFirstActPlansMerger.class);
	
	private static final String plansFile = DgPaths.VSPSVNBASE + "studies/schweiz-ivtch/baseCase/plans/plans_miv_zrh30km_10pct.xml.gz";
	
//  private static final String cmcfPlansFile = DgPaths.VSPSVNBASE + "studies/schweiz-ivtch/cmcf/plans/plans_miv_zrh30km_10pct_simplified_acts_types_reduced_cmcf.xml";
  
  private static final String outPlansFile = DgPaths.VSPSVNBASE + "studies/schweiz-ivtch/cmcf/plans/plans_miv_zrh30km_10pct_one_act.xml.gz";
  
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		NetworkLayer net = MatsimIo.loadNetwork(DgPaths.IVTCHNET);
//		Plans plansCmcf = MatsimIo.loadPlans(cmcfPlansFile);
		Population plans = MatsimIo.loadPlans(plansFile);
		Population plansOne = new Population(Population.NO_STREAMING);
		for (Person p : plans.getPersons().values()) {
			Plan pl = p.getSelectedPlan();
		  int i = 0;
			for (LegIterator legIter = pl.getIteratorLeg(); legIter.hasNext(); i++) {
				StringBuffer idStringBuffer = new StringBuffer(p.getId().toString());
				idStringBuffer.append("leg");
				idStringBuffer.append(Integer.toString(i));
				
				Person pNew = new PersonImpl(new IdImpl(idStringBuffer.toString()));
				Plan planNew = new Plan(pNew);
				Leg leg = (Leg) legIter.next();
				
				planNew.addAct(pl.getPreviousActivity(leg));
				planNew.addLeg(leg);
				planNew.addAct(pl.getNextActivity(leg));
				
				pNew.addPlan(planNew);
				
				try {
					plansOne.addPerson(pNew);
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				
			}
			
//			Leg l = pl.getNextLeg(pl.getFirstActivity());
//			Plan plcmcf = plansCmcf.getPerson(p.getId()).getSelectedPlan();
//			Leg lcmcf = plcmcf.getNextLeg(plcmcf.getFirstActivity());
//			l.setRoute(lcmcf.getRoute());
		}
		MatsimIo.writePlans(plansOne, outPlansFile);
		
		log.info("done");
	}


}
