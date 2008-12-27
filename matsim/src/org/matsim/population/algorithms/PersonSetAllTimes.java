/* *********************************************************************** *
 * project: org.matsim.*
 * PersonSetAllTimes.java
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

package org.matsim.population.algorithms;

import org.matsim.gbl.MatsimRandom;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.utils.misc.Time;

public class PersonSetAllTimes extends AbstractPersonAlgorithm {

	private static final int EARLIEST_ENDTIME = 6*3600;
	private static final int LATEST_ENDTIME = 11*3600;
	private static final int W_DUR = 8*3600;
	private static final int E_DUR = 8*3600;
	private static final int L_DUR_SHORT = 1*3600;
	private static final int L_DUR_LONG = 8*3600;
	private static final int S_DUR_SHORT = 1*3600;
	private static final int S_DUR_LONG = 8*3600;

	public PersonSetAllTimes() {
	}

	@Override
	public void run(final Person person) {
		for (int i=0; i<person.getPlans().size(); i++) {
			Plan plan = person.getPlans().get(i);

			int w_cnt = 0;
			int e_cnt = 0;
			int l_cnt = 0;
			int s_cnt = 0;
			double prev_endtime = 0;

			for (int j=0; j<plan.getActsLegs().size(); j=j+2) {
				Act act = (Act)plan.getActsLegs().get(j);

				if (j == 0) {
					int endtime = EARLIEST_ENDTIME +
												MatsimRandom.random.nextInt(LATEST_ENDTIME -
																					 EARLIEST_ENDTIME + 1);
					act.setEndTime(endtime);
					act.setStartTime(Time.UNDEFINED_TIME);
					act.setDuration(Time.UNDEFINED_TIME);

					prev_endtime = act.getEndTime();
				}
				else if (j == plan.getActsLegs().size()-1) {
					act.setStartTime(Time.UNDEFINED_TIME);
					act.setDuration(Time.UNDEFINED_TIME);
					act.setEndTime(Time.UNDEFINED_TIME);
				}
				else {
					if (act.getType().equals("w")) { w_cnt++; }
					if (act.getType().equals("e")) { e_cnt++; }
					if (act.getType().equals("l")) { l_cnt++; }
					if (act.getType().equals("s")) { s_cnt++; }
					act.setStartTime(Time.UNDEFINED_TIME);
					act.setEndTime(Time.UNDEFINED_TIME);
				}
			}

			for (int j=0; j<plan.getActsLegs().size(); j=j+2) {
				Act act = (Act)plan.getActsLegs().get(j);

				if (act.getType().equals("w")) {
					if (w_cnt == 0) { throw new RuntimeException("HAE?: w_cnt=0"); }
					else if (w_cnt == 1) { act.setDuration(W_DUR); act.setType("w1"); }
					else if (w_cnt == 2) { act.setDuration(W_DUR/2); act.setType("w2");}
					else { throw new RuntimeException("HAE?: w_cnt>2"); }
				}
				else if (act.getType().equals("e")) {
					if (e_cnt == 0) { throw new RuntimeException("HAE?: e_cnt=0"); }
					else if (e_cnt == 1) { act.setDuration(E_DUR); act.setType("e1");}
					else if (e_cnt == 2) { act.setDuration(E_DUR/2); act.setType("e2");}
					else { throw new RuntimeException("HAE?: e_cnt>2"); }
				}
				else if (act.getType().equals("l")) {
					if ((w_cnt > 0) || (e_cnt > 0)) {
						act.setDuration(L_DUR_SHORT); act.setType("l");
					}
					else {
						if (l_cnt == 0) { throw new RuntimeException("HAE?: l_cnt=0"); }
						else if (l_cnt == 1) { act.setDuration(L_DUR_LONG); act.setType("l1"); }
						else if (l_cnt == 2) { act.setDuration(L_DUR_LONG/2); act.setType("l2"); }
						else { throw new RuntimeException("HAE?: l_cnt>2"); }
					}
				}
				else if (act.getType().equals("s")) {
					if ((w_cnt > 0) || (e_cnt > 0)) {
						act.setDuration(L_DUR_SHORT); act.setType("s");
					}
					else if (l_cnt > 0) {
						act.setDuration(S_DUR_SHORT);
					}
					else {
						if (s_cnt == 0) { throw new RuntimeException("HAE?: s_cnt=0"); }
						else if (s_cnt == 1) { act.setDuration(S_DUR_LONG); act.setType("s1"); }
						else if (s_cnt == 2) { act.setDuration(S_DUR_LONG/2); act.setType("s2"); }
						else { throw new RuntimeException("HAE?: s_cnt>2"); }
					}
				}
				else if (!act.getType().equals("h")) {
					throw new RuntimeException("ERROR: in " + this.getClass().getName() +
														 " in run(Person person):" +
														 " does not know act type = " + act.getType() + ".");
				}

				if (j == 0) {
					act.setStartTime(0);
					act.setDuration(act.getEndTime());
				}
				else if (j == plan.getActsLegs().size()-1) {
					act.setStartTime(prev_endtime);
				}
				else {
					act.setStartTime(prev_endtime);
					act.setEndTime(act.getStartTime()+act.getDuration());
					prev_endtime = act.getEndTime();
				}
			}

			double act_end_time = 0;
			for (int j=0; j<plan.getActsLegs().size(); j++) {
				if (j % 2 == 0) {
					Act act = (Act)plan.getActsLegs().get(j);
					act_end_time = act.getEndTime();
				}
				else {
					Leg leg = (Leg)plan.getActsLegs().get(j);
					leg.setDepartureTime(act_end_time);
					leg.setTravelTime(0);
					leg.setArrivalTime(act_end_time);
				}
			}
		}
	}
}
