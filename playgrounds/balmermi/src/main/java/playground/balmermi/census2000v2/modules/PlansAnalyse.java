/* *********************************************************************** *
 * project: org.matsim.*
 * PlansFilterArea.java
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

package playground.balmermi.census2000v2.modules;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;

public class PlansAnalyse {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(PlansAnalyse.class);

	private static final String ALWAYS = "always";
	private static final String SOMETIMES = "sometimes";
	private static final String NEVER = "never";

	private static final String H = "h";
	private static final String W = "w";
	private static final String E = "e";
	private static final String S = "s";
	private static final String L = "l";
	
	private static final String CAR = "car";
	private static final String PT = "pt";
	private static final String BIKE = "bike";
	private static final String WALK = "walk";

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PlansAnalyse() {
		log.info("    init " + this.getClass().getName() + " module...");
		log.info("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// run method
	//////////////////////////////////////////////////////////////////////

	public void run(final PopulationImpl plans) {
		log.info("    running " + this.getClass().getName() + " module...");
		
		double[] lic_cnt = new double[2]; // yes,no
		for (int i=0; i<lic_cnt.length; i++) { lic_cnt[i]=0; }
		double[] mt_cnt = new double[6]; // nev_no,som_no,alw_no,nev_yes,som_yes,alw_yes,
		for (int i=0; i<mt_cnt.length; i++) { mt_cnt[i]=0; }
		double a_cnt = 0; // all acts
		double[] at_cnt = new double[5]; // h,w,e,s,l
		for (int i=0; i<at_cnt.length; i++) { at_cnt[i]=0; }
		double leg_cnt = 0; // all modes
		double[] mtype_cnt = new double[4]; // car,pt,bike,walk
		for (int i=0; i<mtype_cnt.length; i++) { mtype_cnt[i]=0; }
		
		double[] trip_dist = new double[30];
		for (int i=0; i<trip_dist.length; i++) { trip_dist[i]=0; }
		
		for (Person pp : plans.getPersons().values()) {
			PersonImpl p = (PersonImpl) pp;
			// license
			if (p.hasLicense()) { lic_cnt[0]++; } else { lic_cnt[1]++; }
			// mob tools
			int idx = -1;
			if (p.getTravelcards() == null) { idx = 0; } else { idx = 3; }
			if (p.getCarAvail() == null) { ; }
			else if (p.getCarAvail().equals(NEVER)) { idx += 0; }
			else if (p.getCarAvail().equals(SOMETIMES)) { idx += 1; }
			else if (p.getCarAvail().equals(ALWAYS)) { idx += 2; }
			else { Gbl.errorMsg("pid="+p.getId()+": Haeh?"); }
			mt_cnt[idx]++;
			// act types
			if (p.getPlans().size() != 1) { Gbl.errorMsg("pid="+p.getId()+": There must be exactly one plan per person!"); }
			Plan plan = p.getPlans().get(0);
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof ActivityImpl) {
					ActivityImpl a = (ActivityImpl) pe;
					if (a.getType().substring(0,1).equals(H))      { at_cnt[0]++; }
					else if (a.getType().substring(0,1).equals(W)) { at_cnt[1]++; }
					else if (a.getType().substring(0,1).equals(E)) { at_cnt[2]++; }
					else if (a.getType().substring(0,1).equals(S)) { at_cnt[3]++; }
					else if (a.getType().substring(0,1).equals(L)) { at_cnt[4]++; }
					else { Gbl.errorMsg("pid="+p.getId()+": Haeh?"); }
					a_cnt++;
				}
			}
			// mode types
			int cnt = 0;
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof LegImpl) {
					LegImpl l = (LegImpl) pe;
					cnt++;
					if (l.getMode().equals(TransportMode.car))       { mtype_cnt[0]++; }
					else if (l.getMode().equals(TransportMode.pt))   { mtype_cnt[1]++; }
					else if (l.getMode().equals(TransportMode.bike)) { mtype_cnt[2]++; }
					else if (l.getMode().equals(TransportMode.walk)) { mtype_cnt[3]++; }
					else { Gbl.errorMsg("pid="+p.getId()+": Haeh?"); }
					leg_cnt++;
				}
			}
			trip_dist[cnt]++;
		}

		double nump = plans.getPersons().size();

		log.info("Population:                 " + plans.getName());
		log.info("--------------------------------------------------------");
		log.info("person count:               " + nump);
		log.info("license yes:                " + lic_cnt[0] + "\t" + ((int)(100*lic_cnt[0]/nump)) + " %");
		log.info("license no:                 " + lic_cnt[1] + "\t" + ((int)(100*lic_cnt[1]/nump)) + " %");
		log.info("car(never),ticket(no):      " + mt_cnt[0] + "\t" + ((int)(100*mt_cnt[0]/nump)) + " %");
		log.info("car(sometimes),ticket(no):  " + mt_cnt[1] + "\t" + ((int)(100*mt_cnt[1]/nump)) + " %");
		log.info("car(always),ticket(no):     " + mt_cnt[2] + "\t" + ((int)(100*mt_cnt[2]/nump)) + " %");
		log.info("car(never),ticket(yes):     " + mt_cnt[3] + "\t" + ((int)(100*mt_cnt[3]/nump)) + " %");
		log.info("car(sometimes),ticket(yes): " + mt_cnt[4] + "\t" + ((int)(100*mt_cnt[4]/nump)) + " %");
		log.info("car(always),ticket(yes):    " + mt_cnt[5] + "\t" + ((int)(100*mt_cnt[5]/nump)) + " %");
		log.info("act count:                  " + a_cnt + "\t" + (a_cnt/nump) + " acts/p");
		log.info("acttype h:                  " + at_cnt[0] + "\t" + (at_cnt[0]/nump) + " h_act/p");
		log.info("acttype w:                  " + at_cnt[1] + "\t" + (at_cnt[1]/nump) + " w_act/p");
		log.info("acttype e:                  " + at_cnt[2] + "\t" + (at_cnt[2]/nump) + " e_act/p");
		log.info("acttype s:                  " + at_cnt[3] + "\t" + (at_cnt[3]/nump) + " s_act/p");
		log.info("acttype l:                  " + at_cnt[4] + "\t" + (at_cnt[4]/nump) + " l_act/p");
		log.info("leg count:                  " + leg_cnt + "\t" + (leg_cnt/nump) + " legs/p");
		log.info("legmode car:                " + mtype_cnt[0] + "\t" + (mtype_cnt[0]/nump) + " car_leg/p");
		log.info("legmode pt:                 " + mtype_cnt[1] + "\t" + (mtype_cnt[1]/nump) + " pt_leg/p");
		log.info("legmode bike:               " + mtype_cnt[2] + "\t" + (mtype_cnt[2]/nump) + " bike_leg/p");
		log.info("legmode walk:               " + mtype_cnt[3] + "\t" + (mtype_cnt[3]/nump) + " walk_leg/p");
		log.info("#t/p" + ":\t" + "count" + "\t" + "percent");
		for (int i=0; i<trip_dist.length; i++) {
			log.info(i + ":\t" + trip_dist[i] + "\t" + ((int)(100*trip_dist[i]/nump)));
		}
		
		log.info("    done.");
	}
}
