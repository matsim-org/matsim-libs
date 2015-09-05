/* *********************************************************************** *
 * project: org.matsim.*
 * PersonLicenseModel.java
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

import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

public class PersonAssignAndNormalizeTimes extends AbstractPersonAlgorithm implements PlanAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(PersonAssignAndNormalizeTimes.class);

	private static final double HOME_MIN = 4*3600;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonAssignAndNormalizeTimes() {
		log.info("    init " + this.getClass().getName() + " module...");
		log.info("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private final void moveTravTimeToNextAct(final Plan p) {
		double prev_ttime = Time.UNDEFINED_TIME;
		double tod = ((Activity)p.getPlanElements().get(0)).getEndTime();
		for (int i=1; i<p.getPlanElements().size(); i++) {
			PlanElement pe = p.getPlanElements().get(i);
			if (i == p.getPlanElements().size()-1) { // last act
				Activity a = (Activity) p.getPlanElements().get(i);
				if (prev_ttime == Time.UNDEFINED_TIME) {
					throw new RuntimeException("That must not happen!");
				}
				double dur = prev_ttime;
				a.setStartTime(tod);
				a.setMaximumDuration(dur);
				a.setEndTime(tod+dur);
				prev_ttime = Time.UNDEFINED_TIME;
			}
			else if (pe instanceof Activity) { // in between acts
				Activity a = (Activity)p.getPlanElements().get(i);
				double dur = a.getMaximumDuration();
				if (prev_ttime == Time.UNDEFINED_TIME) {
					throw new RuntimeException("That must not happen!");
				}
				dur += prev_ttime;
				if (dur < 5*60.0) { dur = 5*60.0; } // NOTE: Sometimes the mz act duration is 0 sec.
				a.setStartTime(tod);
				a.setMaximumDuration(dur);
				a.setEndTime(tod+dur);
				tod = a.getEndTime();
				prev_ttime = Time.UNDEFINED_TIME;
			}
			else if (pe instanceof Leg) {
				Leg l = (Leg) p.getPlanElements().get(i);
				prev_ttime = l.getTravelTime();
				l.setDepartureTime(tod);
				l.setTravelTime(0.0);
				if (l instanceof LegImpl) {
					((LegImpl) l).setArrivalTime(tod);
				}
			}
		}
	}

	//////////////////////////////////////////////////////////////////////

	private final void normalizeTimes(final Plan p) {
		if (p.getPlanElements().size() == 1) {
			ActivityImpl a = (ActivityImpl)p.getPlanElements().get(0);
			a.setStartTime(0.0);
			a.setMaximumDuration(Time.MIDNIGHT);
			a.setEndTime(Time.MIDNIGHT);
			return;
		}
		double home_dur = ((ActivityImpl)p.getPlanElements().get(0)).getEndTime();
		double othr_dur = 0.0;
		for (int i=1; i<p.getPlanElements().size()-1; i=i++) {
			PlanElement pe = p.getPlanElements().get(i);
			if (pe instanceof Activity) {
				othr_dur += ((Activity)p.getPlanElements().get(i)).getMaximumDuration();
			}
		}
		if (othr_dur <= (Time.MIDNIGHT - HOME_MIN)) {
			ActivityImpl a = (ActivityImpl)p.getPlanElements().get(p.getPlanElements().size()-1);
			a.setMaximumDuration(Time.UNDEFINED_TIME);
			a.setEndTime(Time.UNDEFINED_TIME);
			return;
		}

		// normalize the other activity durations
		log.info("pid="+p.getPerson().getId()+": normalizing times (othr_dur="+Time.writeTime(othr_dur)+").");
		double tod = home_dur;
		for (int i=1; i<p.getPlanElements().size(); i++) {
			PlanElement pe = p.getPlanElements().get(i);
			if (i == p.getPlanElements().size()-1) {
				Activity a = (Activity)p.getPlanElements().get(i);
				a.setStartTime(tod);
				a.setMaximumDuration(Time.UNDEFINED_TIME);
				a.setEndTime(Time.UNDEFINED_TIME);
			}
			else if (pe instanceof Activity) {
				Activity a = (Activity)p.getPlanElements().get(i);
				a.setStartTime(tod);
				a.setMaximumDuration((Time.MIDNIGHT - HOME_MIN)*a.getMaximumDuration()/othr_dur);
				a.setEndTime(tod+a.getMaximumDuration());
				tod = a.getEndTime();
			}
			else if (pe instanceof Leg) {
				Leg l = (Leg)p.getPlanElements().get(i);
				l.setDepartureTime(tod);
				l.setTravelTime(0.0);
				if (l instanceof LegImpl) {
					((LegImpl) l).setArrivalTime(tod);
				}
			}
		}
	}

	//////////////////////////////////////////////////////////////////////

	private final void assignDesires(final Plan p) {
		throw new UnsupportedOperationException( "desires are gone" );
		//Desires d = ((PersonImpl) p.getPerson()).createDesires(null);
		//double othr_dur = 0.0;
		//Activity prevAct = null;
		//boolean prevIsFirst = true;
		//for (PlanElement pe : p.getPlanElements()) {
		//	// handle all activities except the first and the last one
		//	if (pe instanceof Activity) {
		//		if (!prevIsFirst) {
		//			if (prevAct.getMaximumDuration() <= 0.0) {
		//				log.fatal("pid="+p.getPerson().getId()+": That must not happen!");
		//			}
		//			d.accumulateActivityDuration(prevAct.getType(), prevAct.getMaximumDuration());
		//			othr_dur += prevAct.getMaximumDuration();
		//		}
		//		prevIsFirst = (prevAct == null);
		//		prevAct = (Activity) pe;
		//	}
		//}
		//double home_dur = Time.MIDNIGHT - othr_dur;
		//if (home_dur <= 0.0) {
		//	throw new RuntimeException("pid="+p.getPerson().getId()+": That must not happen!");
		//}
		//d.accumulateActivityDuration(((ActivityImpl)p.getPlanElements().get(0)).getType(),home_dur);
	}

	//////////////////////////////////////////////////////////////////////

	private final void varyTimes(final Plan p) {
		List<? extends PlanElement> acts_legs = p.getPlanElements();

		// draw a new random number until the new end time >= 0.0
		double bias = MatsimRandom.getRandom().nextInt(3600)-1800.0; // [-1800,1800[
		double first_end_time = ((ActivityImpl)acts_legs.get(0)).getEndTime();
		while (first_end_time+bias < 0.0) { bias = MatsimRandom.getRandom().nextInt(3600)-1800.0; }

		for (int i=0; i<acts_legs.size(); i++) {
			PlanElement pe = acts_legs.get(i);
			if (pe instanceof Activity) {
				Activity act = (Activity) pe;
				if (i == 0) { // first act
					act.setStartTime(0.0);
					act.setMaximumDuration(act.getMaximumDuration()+bias);
					act.setEndTime(act.getEndTime()+bias);
				}
				else if (i == acts_legs.size()-1) { // last act
					act.setStartTime(act.getStartTime()+bias);
					act.setMaximumDuration(Time.UNDEFINED_TIME);
					act.setEndTime(Time.UNDEFINED_TIME);
				}
				else { // in between acts
					act.setStartTime(act.getStartTime()+bias);
					act.setEndTime(act.getEndTime()+bias);
				}
			}
			if (pe instanceof Leg) {
				Leg leg = (Leg) pe;
				leg.setDepartureTime(leg.getDepartureTime()+bias);
				((LegImpl) leg).setArrivalTime(((LegImpl) leg).getArrivalTime()+bias);
			}
		}
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(final Person person) {
		this.run(person.getSelectedPlan());
	}

	@Override
	public void run(final Plan plan) {
		this.moveTravTimeToNextAct(plan);
		this.normalizeTimes(plan);
		this.assignDesires(plan);
		this.varyTimes(plan);
	}
}
