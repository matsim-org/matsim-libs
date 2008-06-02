/* *********************************************************************** *
 * project: org.matsim.*
 * PersonCreatePlanFromKnowledge.java
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

import java.util.ArrayList;

import org.matsim.facilities.Activity;
import org.matsim.gbl.Gbl;
import org.matsim.plans.Act;
import org.matsim.plans.Knowledge;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.algorithms.PersonAlgorithm;

import playground.balmermi.census2000v2.data.CAtts;
import playground.balmermi.census2000v2.data.Household;

public class PersonCreateFakePlanFromKnowledge extends PersonAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final String[] w_acts = { CAtts.ACT_W2, CAtts.ACT_W3 };
	private final String[] e_acts = { CAtts.ACT_EKIGA, CAtts.ACT_EPRIM, CAtts.ACT_ESECO, CAtts.ACT_EHIGH, CAtts.ACT_EOTHR };
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonCreateFakePlanFromKnowledge() {
		super();
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(final Person person) {
		Knowledge k = person.getKnowledge();

		Activity home = null;
		ArrayList<Activity> home_acts = k.getActivities(CAtts.ACT_HOME);
		if (home_acts.size()==0) { Gbl.errorMsg("pid="+person.getId()+", kdesc="+k.getDesc()+": no home activity defined"); }
		else if (home_acts.size()==1) { home = home_acts.get(0); }
		else if (home_acts.size()==2) {
			Household hh = (Household)person.getCustomAttributes().get(CAtts.HH_W);
			if (home_acts.get(0).getFacility().equals(hh.getFacility())) { home = home_acts.get(0); }
			else if (home_acts.get(1).getFacility().equals(hh.getFacility())) { home = home_acts.get(1); }
			else { Gbl.errorMsg("pid="+person.getId()+", kdesc="+k.getDesc()+", hhid_w="+hh.getId()+": given facilities do not match the household!"); }
		}
		else { Gbl.errorMsg("pid="+person.getId()+", kdesc="+k.getDesc()+": more than 2 home activities defined"); }

		Activity work = null;
		for (int i=0; i<this.w_acts.length;i++) {  // that only works if we know that there is at most one work activity
			ArrayList<Activity> work_acts = k.getActivities(this.w_acts[i]);
			if (!work_acts.isEmpty()) { work = work_acts.get(0); }
		}
		
		Activity educ = null;
		for (int i=0; i<this.e_acts.length;i++) {  // that only works if we know that there is at most one educ activity
			ArrayList<Activity> educ_acts = k.getActivities(this.e_acts[i]);
			if (!educ_acts.isEmpty()) { educ = educ_acts.get(0); }
		}
		
		Plan p = person.createPlan(true);
		try {
			if ((work==null)&&(educ==null)) {
				Act act = p.createAct(home.getType(),home.getFacility().getCenter().getX(),home.getFacility().getCenter().getY(),null,0,24*3600,24*3600,true);
				act.setFacility(home.getFacility());
			}
			else {
				// home act end time = [6am-8am]
				double start_time = 0.0;
				double end_time = 6*3600 + (Gbl.random.nextInt(2*3600));
				double sum_dur = end_time;
				int leg_cnt = 0;
				Act act = p.createAct(home.getType(),home.getFacility().getCenter().getX(),home.getFacility().getCenter().getY(),null,start_time,end_time,end_time,true);
				act.setFacility(home.getFacility());
				p.createLeg("car",end_time,0.0,end_time);
				leg_cnt++;
				
				if (work!=null) {
					// work act dur = [7h-8h]
					start_time = end_time;
					end_time = end_time + 7*3600 + (Gbl.random.nextInt(1*3600));
					sum_dur = sum_dur + (end_time-start_time);
					act = p.createAct(work.getType(),work.getFacility().getCenter().getX(),work.getFacility().getCenter().getY(),null,start_time,end_time,(end_time-start_time),true);
					act.setFacility(work.getFacility());
					p.createLeg("car",end_time,0.0,end_time);
					leg_cnt++;
				}
				
				if (educ!=null) {
					// educ act dur = [4h-6h]
					start_time = end_time;
					end_time = end_time + 4*3600 + (Gbl.random.nextInt(2*3600));
					sum_dur = sum_dur + (end_time-start_time);
					act = p.createAct(educ.getType(),educ.getFacility().getCenter().getX(),educ.getFacility().getCenter().getY(),null,start_time,end_time,(end_time-start_time),true);
					act.setFacility(educ.getFacility());
					p.createLeg("car",end_time,0.0,end_time);
					leg_cnt++;
				}
				start_time = end_time;
				end_time = 24*3600;
				sum_dur = sum_dur + (end_time-start_time);
				act = p.createAct(home.getType(),home.getFacility().getCenter().getX(),home.getFacility().getCenter().getY(),null,start_time,end_time,(end_time-start_time),true);
				act.setFacility(home.getFacility());
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}
