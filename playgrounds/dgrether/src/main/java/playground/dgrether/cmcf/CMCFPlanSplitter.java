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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

import playground.dgrether.DgPaths;
import playground.dgrether.utils.MatsimIo;


/**
 * @author dgrether
 *
 */
public class CMCFPlanSplitter {
	private static final Logger log = Logger.getLogger(CMCFFirstActPlansMerger.class);

	private static final String plansFile = DgPaths.REPOS + "studies/schweiz-ivtch/baseCase/plans/plans_miv_zrh30km_10pct.xml.gz";

//  private static final String cmcfPlansFile = DgPaths.VSPSVNBASE + "studies/schweiz-ivtch/cmcf/plans/plans_miv_zrh30km_10pct_simplified_acts_types_reduced_cmcf.xml";

  private static final String outPlansFile = DgPaths.REPOS + "studies/schweiz-ivtch/cmcf/plans/plans_miv_zrh30km_10pct_one_act.xml.gz";

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network net = scenario.getNetwork();
		MatsimIo.loadNetwork(DgPaths.IVTCHNET, scenario);
//		Plans plansCmcf = MatsimIo.loadPlans(cmcfPlansFile);
		Population plans = MatsimIo.loadPlans(plansFile, net);
		Population plansOne = ((MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig())).getPopulation();
		for (Person p : plans.getPersons().values()) {
			Plan pl = p.getSelectedPlan();
		  int i = 0;
		  for (PlanElement pe : pl.getPlanElements()) {
		  	if (pe instanceof Leg) {
		  		StringBuffer idStringBuffer = new StringBuffer(p.getId().toString());
		  		idStringBuffer.append("leg");
		  		idStringBuffer.append(Integer.toString(i));

		  		Person pNew = PopulationUtils.getFactory().createPerson(Id.create(idStringBuffer.toString(), Person.class));
		  		Plan planNew = PopulationUtils.createPlan(pNew);
		  		Leg leg = (Leg) pe;
				final Leg leg2 = leg;

		  		planNew.addActivity(PopulationUtils.getPreviousActivity(((Plan) pl), leg2));
		  		planNew.addLeg(leg);
				final Leg leg1 = leg;
		  		planNew.addActivity(PopulationUtils.getNextActivity(((Plan) pl), leg1));

		  		pNew.addPlan(planNew);

		  		try {
		  			plansOne.addPerson(pNew);
		  		} catch (Exception e) {
		  			e.printStackTrace();
		  		}
		  	}
		  }

//			Leg l = pl.getNextLeg(pl.getFirstActivity());
//			Plan plcmcf = plansCmcf.getPerson(p.getId()).getSelectedPlan();
//			Leg lcmcf = plcmcf.getNextLeg(plcmcf.getFirstActivity());
//			l.setRoute(lcmcf.getRoute());
		}
		MatsimIo.writePlans(plansOne, net, outPlansFile);

		log.info("done");
	}


}
